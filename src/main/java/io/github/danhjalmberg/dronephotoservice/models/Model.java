package io.github.danhjalmberg.dronephotoservice.models;

import io.github.danhjalmberg.dronephotoservice.models.drones.Drone;
import io.github.danhjalmberg.dronephotoservice.models.drones.DroneFactory;
import io.github.danhjalmberg.dronephotoservice.models.drones.DroneType;
import io.github.danhjalmberg.dronephotoservice.models.events.SimulationEvent;
import io.github.danhjalmberg.dronephotoservice.models.events.SimulationEventLog;
import io.github.danhjalmberg.dronephotoservice.models.events.SimulationEventType;
import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;
import io.github.danhjalmberg.dronephotoservice.models.map.MapLoadException;
import io.github.danhjalmberg.dronephotoservice.models.map.MapMetadata;
import io.github.danhjalmberg.dronephotoservice.models.map.MapModel;
import io.github.danhjalmberg.dronephotoservice.models.photo_agencies.PhotoAgency;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.DroneSnapshot;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.PhotoAgencySnapshot;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.TaskDetailsSnapshot;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.TaskExportData;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.TaskSnapshot;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.TaskThumbnailSnapshot;
import io.github.danhjalmberg.dronephotoservice.models.tasks.Task;
import io.github.danhjalmberg.dronephotoservice.models.tasks.TaskArchive;
import io.github.danhjalmberg.dronephotoservice.models.tasks.TaskQueue;
import io.github.danhjalmberg.dronephotoservice.models.tasks.TaskType;
import io.github.danhjalmberg.dronephotoservice.settings.ModelSettings;
import io.github.danhjalmberg.dronephotoservice.support.TaskImageExporter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

/**
 * Application facade and lifecycle owner for the simulation domain.
 *
 * <p>The model coordinates the active map, simulation clock, photo-agency and
 * drone actors, the shared task queue, completed-task archive, event history,
 * snapshots, and image export. Controllers interact with these subsystems
 * through this facade instead of coordinating domain actors directly.</p>
 *
 * <p>The physics step defines how much simulated time one update advances. The
 * speed multiplier changes the wall-clock interval at which those fixed steps
 * are scheduled; it does not change the physics delta itself. Actor workers
 * execute asynchronously and use bounded or prioritized work queues where
 * backpressure is required.</p>
 *
 * <p>Runtime reads and collection snapshots use targeted synchronization, but
 * lifecycle and configuration operations are expected to be serialized by the
 * controller.</p>
 *
 * @author Dan Hjälmberg
 */
public class Model {

    // Executor shutdown timeouts.
    // The cooperative timeout is longer than SIMULATION_PAUSE_MS so actor
    // threads normally have time to observe their running flag and exit
    private static final long ACTOR_EXECUTOR_SHUTDOWN_TIMEOUT_MS = 1500L;
    private static final long ACTOR_EXECUTOR_FORCE_SHUTDOWN_TIMEOUT_MS = 1000L;

    private final Random random = new Random();

    private final MapModel mapModel;

    // The queue is application-wide; TaskArchive belongs to this model instance.
    private final TaskQueue taskQueue;
    private final TaskArchive taskArchive;

    private ExecutorService photoAgencyPool;
    private int photoAgencyId = 0;
    private final Deque<PhotoAgency> photoAgencies;

    private ExecutorService dronePool;
    private final DroneFactory droneFactory;
    private int droneId = 0;
    private Vector2D basePositionMeters;
    private final Deque<Drone> drones;

    private volatile int simulationTickMs = ModelSettings.SIMULATION_TICK_MS;
    private volatile double simulationSpeedMultiplier = ModelSettings.SIMULATION_SPEED_MULTIPLIER;
    private final AtomicLong elapsedSimulationMillis = new AtomicLong();

    private final SimulationEventLog simulationEventLog;

    /**
     * Creates an empty simulation model with default timing configuration.
     *
     * <p>The model receives the application-wide task queue singleton but owns
     * its map, task archive, event log, actor collections, and executor
     * references.</p>
     */
    public Model() {

        this.mapModel = new MapModel();
        this.taskQueue = TaskQueue.INSTANCE;
        this.taskArchive = new TaskArchive();
        this.photoAgencies = new LinkedList<>();
        this.droneFactory = new DroneFactory();
        this.drones = new LinkedList<>();
        this.simulationEventLog = new SimulationEventLog();
    }

    // ########################################################################
    // Map image file management
    // ########################################################################

    /**
     * Loads a classpath map image and its optional JSON sidecar metadata.
     *
     * <p>The caller retains ownership of {@code inputStream}; this operation
     * does not close it. Loading is transactional, so failure preserves the
     * previously active map.</p>
     *
     * @param inputStream       image resource input stream
     * @param mapFileName       name of the map file
     * @param imageResourcePath classpath path of the image resource
     * @throws MapLoadException if the map image or its metadata cannot be
     *                          read, validated, or processed
     */
    public void loadMap(
            InputStream inputStream,
            String mapFileName,
            String imageResourcePath) throws MapLoadException {

        mapModel.loadMap(inputStream, mapFileName, imageResourcePath);
    }

    /**
     * Loads a map image from a caller-owned stream without sidecar metadata.
     *
     * <p>This operation does not close the stream. Loading is transactional,
     * so failure preserves the previously active map.</p>
     *
     * @param inputStream map image input stream
     * @param mapFileName name of the map file
     * @throws MapLoadException if the map image cannot be read or processed
     */
    public void loadMap(
            InputStream inputStream,
            String mapFileName) throws MapLoadException {

        mapModel.loadMap(inputStream, mapFileName);
    }

    /**
     * Loads a map image from disk together with optional sidecar metadata.
     *
     * <p>Loading is transactional, so failure preserves the previously active
     * map.</p>
     *
     * @param mapFile map image file
     * @throws MapLoadException if the map image or its metadata cannot be
     *                          read, validated, or processed
     */
    public void loadMap(File mapFile) throws MapLoadException {
        mapModel.loadMap(mapFile);
    }

    /**
     * Returns the active map path or classpath resource identifier.
     *
     * @return map identifier, or {@code "No map loaded"} if unavailable.
     */
    public String getMapFilePath() {

        return mapModel.getMapFilePath();
    }

    /**
     * Returns the cropped image used as the simulation world.
     *
     * @return mutable world image, or {@code null} if no map is loaded.
     */
    public BufferedImage getMapImageCropped() {

        return mapModel.getMapImageCropped();
    }

    /**
     * Returns the resampled image used for display.
     *
     * @return mutable display image, or {@code null} if no map is loaded.
     */
    public BufferedImage getMapImageResampled() {

        return mapModel.getMapImageResampled();
    }

    /**
     * Converts display pixels to world meters through the active map scale.
     *
     * @param displayPoint point in display pixels.
     * @return point in world meters.
     */
    public Vector2D displayToWorldMeters(Vector2D displayPoint) {

        return mapModel.displayToWorldMeters(displayPoint);
    }

    /**
     * Returns the active map's mutable metadata object.
     *
     * <p>This is the model-owned instance rather than a defensive copy.</p>
     *
     * @return map metadata, or null if no map is loaded.
     */
    public MapMetadata getMapMetadata() {
        return mapModel.getMetadata();
    }

    /**
     * Returns attribution text for the active map.
     *
     * @return attribution text, or {@code null} if the loaded map has none.
     * @throws IllegalStateException if no map is loaded.
     */
    public String getMapAttribution() {
        return mapModel.getMapAttribution();
    }

    /**
     * Replaces the active map scale with a manually supplied value.
     *
     * @param metersPerPixel real-world meters represented by one world-image pixel
     * @throws IllegalStateException    if no map is loaded
     * @throws IllegalArgumentException if the value is non-positive or non-finite
     */
    public void setMapMetersPerPixel(double metersPerPixel) {

        mapModel.setMapMetersPerPixel(metersPerPixel);
    }

    /**
     * Converts world meters to pixels in the cropped world image.
     *
     * @param worldPointMeters point in world meters.
     * @return point in world-image pixels.
     */
    public Vector2D worldMetersToPixels(Vector2D worldPointMeters) {

        return mapModel.worldMetersToPixels(worldPointMeters);
    }

    /**
     * Converts world meters to display pixels through the active map scale.
     *
     * @param worldPointMeters point in world meters.
     * @return point in display pixels.
     */
    public Vector2D worldMetersToDisplay(Vector2D worldPointMeters) {

        return mapModel.worldMetersToDisplay(worldPointMeters);
    }

    // ########################################################################
    // Simulation controls
    // ########################################################################

    /**
     * Resets elapsed simulation time to zero.
     */
    public void resetSimulationClock() {
        elapsedSimulationMillis.set(0L);
    }

    /**
     * Advances the internal simulation clock by one simulation step.
     */
    private void advanceSimulationClock() {
        elapsedSimulationMillis.addAndGet(getPhysicsStepMs());
    }

    /**
     * Returns elapsed simulation time.
     *
     * @return elapsed simulation time.
     */
    public Duration getSimulationTime() {
        return Duration.ofMillis(elapsedSimulationMillis.get());
    }

    /**
     * Clears state belonging to the current simulation run.
     *
     * <p>Queued and archived tasks, events, actor collections, generated actor
     * identifiers, the shared base position, and elapsed simulation time are
     * reset. The loaded map, map configuration, physics step, speed multiplier,
     * and configured task-queue capacity are preserved.</p>
     *
     * <p>Reset is permitted only when both actor executor pools are absent or
     * fully terminated. This prevents workers from an earlier run from
     * accessing cleared or reused state.</p>
     *
     * @throws IllegalStateException if an actor executor is still active or is still terminating
     */
    public synchronized void resetSimulation() {

        if (!actorPoolsAreTerminated()) {
            throw new IllegalStateException(
                    "Simulation cannot be reset while actor executors are active or still terminating."
            );
        }

        taskQueue.clear();
        taskArchive.clear();
        simulationEventLog.clear();

        photoAgencies.clear();
        drones.clear();

        photoAgencyId = 0;
        droneId = 0;
        basePositionMeters = null;

        resetSimulationClock();
    }

    /**
     * Checks whether both actor executor pools are safe for model reset.
     * This method is called while holding the model monitor through resetSimulation().
     * A null executor means that its pool has not been created or was cleared after successful termination.
     *
     * @return true if both actor executors are absent or terminated
     */
    private boolean actorPoolsAreTerminated() {

        return executorIsAbsentOrTerminated(photoAgencyPool) && executorIsAbsentOrTerminated(dronePool);
    }

    /**
     * Checks whether one executor no longer owns active worker threads.
     *
     * @param executor executor to inspect, or null
     * @return true if the executor is absent or fully terminated
     */
    private static boolean executorIsAbsentOrTerminated(ExecutorService executor) {

        return executor == null || executor.isTerminated();
    }

    /**
     * Verifies that an executor slot can safely receive a new pool.
     *
     * <p>An executor that is shutting down but has not yet terminated still
     * owns worker threads and must not be replaced.</p>
     *
     * @param executor         existing executor, or {@code null}
     * @param actorDescription actor category used in the exception message
     * @throws IllegalStateException if the existing executor has not terminated
     */
    private static void requireReplaceableExecutor(
            ExecutorService executor,
            String actorDescription) {

        if (!executorIsAbsentOrTerminated(executor)) {
            throw new IllegalStateException(
                    actorDescription
                            + " pool cannot be replaced while it is active or terminating.");
        }
    }

    /**
     * Sets the fixed amount of simulated time advanced by each physics update.
     *
     * @param tickMs simulation tick duration in milliseconds.
     * @throws IllegalArgumentException if the tick duration is not positive.
     */
    public void setSimulationTickMs(int tickMs) {

        if (tickMs <= 0) {
            throw new IllegalArgumentException(
                    "Simulation tick duration must be greater than zero.");
        }

        this.simulationTickMs = tickMs;
    }

    /**
     * Returns the fixed amount of simulated time advanced by each physics update.
     *
     * @return simulation tick duration in milliseconds.
     */
    public int getPhysicsStepMs() {

        return simulationTickMs;
    }

    /**
     * Sets the ratio of simulated-time progression to wall-clock scheduling.
     *
     * <p>The physics step remains unchanged; the multiplier is used when
     * calculating the scheduled update interval.</p>
     *
     * @param multiplier simulation speed multiplier.
     * @throws IllegalArgumentException if the multiplier is not positive or finite.
     */
    public void setSimulationSpeedMultiplier(double multiplier) {

        if (multiplier <= 0.0 || !Double.isFinite(multiplier)) {
            throw new IllegalArgumentException(
                    "Simulation speed multiplier must be positive and finite.");
        }

        this.simulationSpeedMultiplier = multiplier;
    }

    /**
     * Returns the current simulation speed multiplier.
     *
     * @return current simulation speed multiplier.
     */
    public double getSimulationSpeedMultiplier() {

        return simulationSpeedMultiplier;
    }

    /**
     * Calculates the wall-clock interval for scheduled physics updates.
     *
     * <p>The fixed physics step is divided by the speed multiplier, rounded to
     * the nearest millisecond, and clamped to at least one millisecond.</p>
     *
     * @return scheduled physics-update interval in milliseconds.
     */
    public int getActorSleepMs() {

        return Math.max(1, (int) Math.round(simulationTickMs / simulationSpeedMultiplier));
    }

    /**
     * Converts the fixed physics step to simulated seconds.
     *
     * @return physics delta time in simulated seconds.
     */
    public double getPhysicsDeltaTimeSeconds() {

        return simulationTickMs * 0.001;
    }

    /**
     * Replaces the shared task queue with an empty queue of the given capacity.
     *
     * <p>Any currently queued tasks are discarded. This operation is intended
     * for configuration before simulation actors start.</p>
     *
     * @param taskQueueCapacity maximum number of queued tasks.
     * @throws IllegalArgumentException if the capacity is not positive.
     */
    public void setTaskQueueCapacity(int taskQueueCapacity) {

        taskQueue.resetCapacity(taskQueueCapacity);
    }

    /**
     * Advances the clock and all current actors by one physics step.
     *
     * <p>The clock advances before actors receive the resulting elapsed time.
     * Actor collections are copied under the model lock and then updated
     * without holding that lock.</p>
     */
    public void updatePhysics() {

        advanceSimulationClock();

        double dt = getPhysicsDeltaTimeSeconds();
        Duration simulationTime = getSimulationTime();

        List<PhotoAgency> photoAgencySnapshot;
        List<Drone> droneSnapshot;

        synchronized (this) {
            photoAgencySnapshot = new ArrayList<>(photoAgencies);
            droneSnapshot = new ArrayList<>(drones);
        }

        for (PhotoAgency photoAgency : photoAgencySnapshot) {
            photoAgency.updateProduction(dt, simulationTime);
        }

        for (Drone drone : droneSnapshot) {
            drone.updatePhysics(dt, simulationTime);
        }
    }

    // ########################################################################
    // Simulation events
    // ########################################################################

    /**
     * Appends a validated simulation event to the bounded event history.
     *
     * @param simulationTime elapsed simulation time
     * @param type           event type
     * @param sourceName     name of the actor or system that produced the event
     * @param message        human-readable event message
     * @throws NullPointerException if any argument is null
     */
    public void addSimulationEvent(
            Duration simulationTime,
            SimulationEventType type,
            String sourceName,
            String message) {

        simulationEventLog.add(
                Objects.requireNonNull(
                        simulationTime,
                        "Simulation time must not be null."),
                Objects.requireNonNull(
                        type,
                        "Event type must not be null."),
                Objects.requireNonNull(
                        sourceName,
                        "Event source name must not be null."),
                Objects.requireNonNull(
                        message,
                        "Event message must not be null."));
    }

    /**
     * Returns retained events whose insertion sequence exceeds the supplied
     * cursor.
     *
     * <p>The returned list is an independent copy ordered from oldest to
     * newest. Events evicted from the bounded history cannot be recovered.</p>
     *
     * @param sequenceNumber last sequence number already processed by the caller
     * @return copied list of newer simulation events in insertion order
     */
    public List<SimulationEvent> getSimulationEventsSince(long sequenceNumber) {

        return simulationEventLog.getEventsSince(sequenceNumber);
    }

    // ########################################################################
    // PhotoAgency
    // ########################################################################

    /**
     * Creates a fixed-size executor for photo-agency actors.
     *
     * <p>A previously created photo-agency executor must be fully terminated
     * before a new one can replace it. This method does not initiate shutdown
     * of an existing executor.</p>
     *
     * @param photoAgencyPoolSize number of actor threads.
     * @throws IllegalArgumentException if the pool size is not positive.
     * @throws IllegalStateException    if the existing photo-agency executor is
     *                                  active or still terminating.
     */
    public synchronized void createPhotoAgencyPool(int photoAgencyPoolSize) {

        if (photoAgencyPoolSize <= 0) {
            throw new IllegalArgumentException(
                    "Photo agency pool size must be greater than zero.");
        }

        requireReplaceableExecutor(photoAgencyPool, "Photo agency");

        photoAgencyPool = Executors.newFixedThreadPool(photoAgencyPoolSize);
    }

    /**
     * Creates, registers, and submits one photo-agency actor.
     *
     * <p>The executor is validated before actor creation. Registration and
     * submission occur while holding the model monitor. If submission is
     * rejected, registration is rolled back and the actor identifier is not
     * consumed.</p>
     *
     * @throws IllegalStateException      if the photo-agency executor is not
     *                                    accepting work
     * @throws RejectedExecutionException if submission is rejected after
     *                                    executor validation
     */
    public synchronized void addPhotoAgency() {

        ExecutorService executor = requireActiveExecutor(
                photoAgencyPool,
                "Photo agency");

        int candidateId = photoAgencyId + 1;
        PhotoAgency photoAgency = new PhotoAgency(candidateId, taskQueue, this);
        photoAgencies.addLast(photoAgency);

        try {
            executor.execute(photoAgency);
            photoAgencyId = candidateId;
        } catch (RejectedExecutionException exception) {
            photoAgencies.removeLastOccurrence(photoAgency);
            throw exception;
        }
    }

    /**
     * Returns the number of stored photo-agency actors.
     *
     * @return stored agency count.
     */
    public synchronized int getPhotoAgencyCount() {

        return photoAgencies.size();
    }

    /**
     * Returns concatenated diagnostic logs in agency insertion order.
     *
     * @return concatenated diagnostic logs.
     */
    public synchronized String getPhotoAgencyDiagnosticText() {

        StringBuilder sb = new StringBuilder();
        for (PhotoAgency photoAgency : photoAgencies) {
            sb.append(photoAgency.getLog()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Creates immutable summaries of all agencies in insertion order.
     *
     * @return list of photo agency snapshots.
     */
    public synchronized List<PhotoAgencySnapshot> getPhotoAgencySnapshots() {

        return photoAgencies.stream()
                .map(pa -> new PhotoAgencySnapshot(
                        pa.getId(),
                        pa.getName(),
                        pa.getCreatedTaskCount(),
                        pa.getPendingTaskName(),
                        pa.hasPendingTask()
                ))
                .collect(Collectors.toList());
    }

    // ########################################################################
    // Drones
    // ########################################################################

    /**
     * Selects a random safe world-meter base position for subsequently added
     * drones.
     *
     * @throws IllegalStateException if no map is loaded.
     */
    public void createRandomBasePosition() {
        basePositionMeters = mapModel.createRandomSafeWorldMeterPosition(random);
    }

    /**
     * Sets the world-meter base position for subsequently added drones.
     *
     * <p>Existing drones retain their current base positions.</p>
     *
     * @param basePositionMeters base position in world meters
     * @throws NullPointerException if the base position is null
     */
    public void setBasePositionMeters(Vector2D basePositionMeters) {

        this.basePositionMeters = Objects.requireNonNull(
                basePositionMeters, "Base position must not be null.");
    }

    /**
     * Creates a fixed-size executor for drone actors.
     *
     * <p>A previously created drone executor must be fully terminated before a
     * new one can replace it. This method does not initiate shutdown of an
     * existing executor.</p>
     *
     * @param dronePoolSize number of actor threads.
     * @throws IllegalArgumentException if the pool size is not positive.
     * @throws IllegalStateException    if the existing drone executor is active
     *                                  or still terminating.
     */
    public synchronized void createDronePool(int dronePoolSize) {

        if (dronePoolSize <= 0) {
            throw new IllegalArgumentException("Drone pool size must be greater than zero.");
        }

        requireReplaceableExecutor(dronePool, "Drone");

        dronePool = Executors.newFixedThreadPool(dronePoolSize);
    }

    /**
     * Creates, configures, stores, and submits one randomly typed drone actor.
     *
     * <p>The drone receives the currently configured base position, shared task
     * queue, and this model. The executor is validated before construction.
     * Registration and submission occur while holding the model monitor. If
     * submission is rejected, registration is rolled back and the actor
     * identifier is not consumed.</p>
     *
     * @throws IllegalStateException      if the drone executor is not accepting work
     * @throws RejectedExecutionException if submission is rejected after
     *                                    executor validation
     */
    public synchronized void addDrone() {

        ExecutorService executor = requireActiveExecutor(
                dronePool,
                "Drone");

        DroneType[] droneTypes = DroneType.values();
        DroneType droneType = droneTypes[random.nextInt(droneTypes.length)];

        Drone drone = droneFactory.createDrone(droneType);

        int candidateId = droneId + 1;
        drone.setName(candidateId);
        drone.setBasePositionMeters(basePositionMeters);
        drone.setTaskQueue(taskQueue);
        drone.setModel(this);

        drones.addLast(drone);

        try {
            executor.execute(drone);
            droneId = candidateId;
        } catch (RejectedExecutionException exception) {
            drones.removeLastOccurrence(drone);
            throw exception;
        }
    }

    /**
     * Returns an executor that is available to accept actor work.
     *
     * @param executor         executor to validate
     * @param actorDescription actor category used in the exception message
     * @return validated executor
     * @throws IllegalStateException if the executor is absent or shutting down
     */
    private static ExecutorService requireActiveExecutor(
            ExecutorService executor,
            String actorDescription) {

        if (executor == null || executor.isShutdown()) {
            throw new IllegalStateException(
                    actorDescription + " pool is not accepting work.");
        }

        return executor;
    }

    /**
     * Returns the number of stored drone actors.
     *
     * @return stored drone count.
     */
    public synchronized int getDroneCount() {
        return drones.size();
    }

    /**
     * Returns formatted diagnostic information in drone insertion order.
     *
     * @return concatenated drone diagnostic information.
     */
    public synchronized String getDroneDiagnosticText() {

        StringBuilder sb = new StringBuilder();

        for (Drone drone : drones) {
            sb.append(drone.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Creates presentation snapshots for all drones in insertion order.
     *
     * <p>Drone, task, and component values are captured at creation time. No
     * live drone component is exposed through the returned snapshots.</p>
     *
     * @return drone snapshots.
     */
    public synchronized List<DroneSnapshot> getDroneSnapshots() {

        return drones.stream()
                .map(Drone::createSnapshot)
                .collect(Collectors.toList());
    }

    /**
     * Captures a live camera image from the named drone.
     *
     * @param droneName the name of the drone.
     * @return camera image, or {@code null} if the name is {@code null}, no drone
     * matches, or the matching drone is at base.
     */
    public synchronized BufferedImage getDroneLiveCameraImage(String droneName) {

        if (droneName == null) {
            return null;
        }

        return drones.stream()
                .filter(drone -> droneName.equals(drone.getName()))
                .findFirst()
                .map(Drone::createLiveCameraImage)
                .orElse(null);
    }

    // ########################################################################
    // Shared thread control
    // ########################################################################

    /**
     * Requests cooperative suspension of all currently stored actors.
     *
     * <p>This does not pause the physics scheduler; the controller coordinates
     * that timer separately.</p>
     */
    public void pauseSimulationActors() {

        List<PhotoAgency> photoAgencySnapshot;
        List<Drone> droneSnapshot;

        synchronized (this) {
            photoAgencySnapshot = new ArrayList<>(photoAgencies);
            droneSnapshot = new ArrayList<>(drones);
        }

        photoAgencySnapshot.forEach(PhotoAgency::pause);
        droneSnapshot.forEach(Drone::pause);
    }

    /**
     * Allows all currently stored actors to resume queued work.
     */
    public void resumeSimulationActors() {

        List<PhotoAgency> photoAgencySnapshot;
        List<Drone> droneSnapshot;

        synchronized (this) {
            photoAgencySnapshot = new ArrayList<>(photoAgencies);
            droneSnapshot = new ArrayList<>(drones);
        }

        photoAgencySnapshot.forEach(PhotoAgency::resume);
        droneSnapshot.forEach(Drone::resume);
    }

    /**
     * Requests cooperative termination of all currently stored actor loops.
     *
     * <p>This does not wait for executor termination; call
     * {@link #shutdownActorPools()} outside the Swing event-dispatch thread to
     * perform the blocking shutdown sequence.</p>
     */
    public void stopSimulationActors() {

        List<PhotoAgency> photoAgencySnapshot;
        List<Drone> droneSnapshot;

        synchronized (this) {
            photoAgencySnapshot = new ArrayList<>(photoAgencies);
            droneSnapshot = new ArrayList<>(drones);
        }

        photoAgencySnapshot.forEach(PhotoAgency::stop);
        droneSnapshot.forEach(Drone::stop);
    }

    /**
     * Shuts down the photo-agency and drone executor pools and waits for them.
     *
     * <p>Both executors first stop accepting work. Callers should previously
     * request actor termination with {@link #stopSimulationActors()}; otherwise
     * the long-running actor jobs normally require forced interruption after
     * the cooperative timeout. Both pools receive shutdown requests before
     * either is awaited.</p>
     *
     * <p>If a pool misses the first timeout, its remaining tasks are interrupted
     * and it receives one final termination wait. References to successfully
     * terminated executors are cleared; a live executor remains referenced so
     * its ownership is not lost. This blocking method must not run on Swing's
     * event-dispatch thread.</p>
     *
     * @return {@code true} if both executors terminated or were absent;
     * otherwise {@code false}.
     * @throws InterruptedException if waiting for termination is interrupted
     */
    public boolean shutdownActorPools() throws InterruptedException {

        ExecutorService photoAgencyExecutor;
        ExecutorService droneExecutor;

        // Stable local references identify exactly which executors this call owns.
        synchronized (this) {
            photoAgencyExecutor = photoAgencyPool;
            droneExecutor = dronePool;
        }

        // Let both pools begin termination before waiting for either one.
        requestExecutorShutdown(photoAgencyExecutor);
        requestExecutorShutdown(droneExecutor);

        boolean photoAgenciesTerminated;
        boolean dronesTerminated;

        try {
            photoAgenciesTerminated = awaitExecutorTermination(photoAgencyExecutor);

            dronesTerminated = awaitExecutorTermination(droneExecutor);

        } catch (InterruptedException exception) {

            // Do not leave either pool running when the shutdown coordinator is interrupted.
            forceExecutorShutdown(photoAgencyExecutor);
            forceExecutorShutdown(droneExecutor);

            throw exception;
        }

        // Retain ownership of executors that failed to terminate or were replaced.
        synchronized (this) {

            if (photoAgenciesTerminated && photoAgencyPool == photoAgencyExecutor) {
                photoAgencyPool = null;
            }

            if (dronesTerminated && dronePool == droneExecutor) {
                dronePool = null;
            }
        }

        return photoAgenciesTerminated && dronesTerminated;
    }

    /**
     * Prevents an executor from accepting new work while allowing running tasks
     * to finish cooperatively.
     *
     * @param executor executor to request shutdown for
     */
    private static void requestExecutorShutdown(ExecutorService executor) {

        if (executor != null) {
            executor.shutdown();
        }
    }

    /**
     * Interrupts remaining executor tasks.
     *
     * @param executor executor to forcefully shutdown
     */
    private static void forceExecutorShutdown(ExecutorService executor) {

        if (executor != null && !executor.isTerminated()) {
            executor.shutdownNow();
        }
    }

    /**
     * Waits for an executor to terminate and escalates to shutdownNow() if its
     * cooperative shutdown timeout expires.
     *
     * @param executor executor to wait for termination
     * @return true if the executor terminated
     * @throws InterruptedException if the waiting thread is interrupted
     */
    private static boolean awaitExecutorTermination(ExecutorService executor)
            throws InterruptedException {

        if (executor == null) {
            return true;
        }

        if (executor.awaitTermination(
                ACTOR_EXECUTOR_SHUTDOWN_TIMEOUT_MS,
                TimeUnit.MILLISECONDS)) {

            return true;
        }

        executor.shutdownNow();

        return executor.awaitTermination(
                ACTOR_EXECUTOR_FORCE_SHUTDOWN_TIMEOUT_MS,
                TimeUnit.MILLISECONDS);
    }

    // ########################################################################
    // Tasks
    // ########################################################################

    /**
     * Creates a random camera-safe task target in world meters.
     *
     * @return random safe task position in world meters.
     * @throws IllegalStateException if no map is loaded.
     */
    public Vector2D createRandomTaskPositionMeters() {

        return mapModel.createRandomSafeWorldMeterPosition(random);
    }

    /**
     * Creates positional summaries of tasks observed in the shared queue.
     *
     * <p>The backing {@code LinkedBlockingQueue} provides weakly consistent
     * traversal, so concurrent producers or drones may change the queue while
     * this result is being assembled.</p>
     *
     * @return queued task snapshots
     */
    public List<TaskSnapshot> getQueuedTaskSnapshots() {

        return taskQueue.getTasks()
                .stream()
                .map(task -> new TaskSnapshot(
                        task.getName(),
                        task.getType(),
                        task.getTargetPositionMeters()))
                .collect(Collectors.toList());
    }

    /**
     * Returns the current shared task-queue size.
     *
     * @return number of queued tasks
     */
    public int getQueuedTaskCount() {

        return taskQueue.getTasks().size();
    }

    /**
     * Adds a completed task to this model's bounded archive.
     *
     * <p>A {@code null} task is ignored. If capacity is exceeded, the oldest
     * task is evicted and its image references are cleared.</p>
     *
     * @param task completed task to archive.
     */
    public void addTaskToArchive(Task task) {
        taskArchive.add(task);
    }

    /**
     * Returns the number of archived tasks.
     *
     * @return archive size.
     */
    public int getTaskArchiveSize() {
        return taskArchive.size();
    }

    /**
     * Creates export records for archived tasks in insertion order.
     *
     * <p>Each record contains an unmodifiable image list, but the mutable images
     * themselves remain shared with the archived task.</p>
     *
     * @return task export data snapshots.
     */
    public List<TaskExportData> getTaskExportData() {
        return taskArchive.getTaskExportData();
    }

    /**
     * Returns formatted diagnostic text for all archived tasks in insertion
     * order.
     *
     * @return archived-task diagnostic text.
     */
    public String getArchivedTaskDiagnosticText() {
        return taskArchive.getDiagnosticText();
    }

    /**
     * Creates a detailed presentation view of an archived task.
     *
     * <p>The task target is already stored in world meters; no coordinate
     * conversion occurs here. The image list structure is copied from the task,
     * but its mutable images are shared. Video tasks use the last retained frame
     * as their preview; other task types use the first.</p>
     *
     * @param taskName name of the archived task.
     * @return task details, or {@code null} if the name is {@code null} or absent.
     */
    public TaskDetailsSnapshot getArchivedTaskDetails(String taskName) {

        Task task = taskArchive.getTaskByName(taskName);

        if (task == null) {
            return null;
        }

        List<BufferedImage> images = new ArrayList<>(task.getImages());
        BufferedImage previewImage = getPreviewImageForTask(task, images);

        String photoAgencyName = task.getPhotoAgency() == null
                ? ""
                : task.getPhotoAgency().getName();

        return new TaskDetailsSnapshot(
                task.getName(),
                task.getType(),
                photoAgencyName,
                task.getCreationSimulationTime(),
                task.getStartSimulationTime(),
                task.getImageSimulationTime(),
                task.getCompletionSimulationTime(),
                task.getTargetPositionMeters(),
                task.getImageCount(),
                previewImage,
                images);
    }

    /**
     * Selects the image used to preview a task.
     *
     * @param task   task whose preview image should be found.
     * @param images task image list.
     * @return last image for video, first image for other types, or {@code null}
     * if no image is available.
     */
    private BufferedImage getPreviewImageForTask(Task task, List<BufferedImage> images) {

        if (images.isEmpty()) {
            return null;
        }

        if (task.getType() == TaskType.VIDEO) {
            return images.get(images.size() - 1);
        }

        return images.get(0);
    }

    /**
     * Creates thumbnail records for up to the latest archived tasks.
     *
     * <p>The selected records remain ordered from oldest to newest and contain
     * shared mutable thumbnail images.</p>
     *
     * @param maxCount maximum number of thumbnails to return.
     * @return thumbnail records, or an empty list if {@code maxCount} is not positive.
     */
    public List<TaskThumbnailSnapshot> getLatestTaskThumbnails(int maxCount) {
        return taskArchive.getLatestTaskThumbnails(maxCount);
    }

    // ########################################################################
    // Task image saving
    // ########################################################################

    /**
     * Saves completed-task images to a new timestamped export directory.
     *
     * <p>The optional callback receives the number of fully processed tasks.
     * Thread interruption is checked between tasks so a canceled export does
     * not begin another task file.</p>
     *
     * @param tasks             completed task export snapshots
     * @param saveRootDirectory selected root export directory
     * @param progressCallback  optional progress callback
     * @return created simulation export directory
     * @throws NullPointerException if the task list or a contained record is null
     * @throws IOException          if the output directory cannot be used or writing fails
     */
    public File saveImagesToDisk(
            List<TaskExportData> tasks,
            File saveRootDirectory,
            IntConsumer progressCallback) throws IOException {

        return TaskImageExporter.saveImagesToDisk(
                tasks,
                saveRootDirectory,
                progressCallback);
    }
}
