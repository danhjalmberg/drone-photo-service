package io.github.danhjalmberg.dronephotoservice.models.drones;

import io.github.danhjalmberg.dronephotoservice.models.Model;
import io.github.danhjalmberg.dronephotoservice.models.components.Battery;
import io.github.danhjalmberg.dronephotoservice.models.components.Motor;
import io.github.danhjalmberg.dronephotoservice.models.events.SimulationEventType;
import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;
import io.github.danhjalmberg.dronephotoservice.models.photo_agencies.PhotoAgency;
import io.github.danhjalmberg.dronephotoservice.models.components.Camera;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.BatterySnapshot;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.CameraSnapshot;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.DroneSnapshot;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.MotorSnapshot;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.TaskSnapshot;
import io.github.danhjalmberg.dronephotoservice.models.tasks.PhotoTask;
import io.github.danhjalmberg.dronephotoservice.models.tasks.Task;
import io.github.danhjalmberg.dronephotoservice.models.tasks.TaskQueue;
import io.github.danhjalmberg.dronephotoservice.models.tasks.VideoTask;
import io.github.danhjalmberg.dronephotoservice.models.tasks.ZoomTask;
import io.github.danhjalmberg.dronephotoservice.settings.ModelSettings;
import io.github.danhjalmberg.dronephotoservice.support.ImageUtils;
import io.github.danhjalmberg.dronephotoservice.support.TimeUtils;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Mobile simulation actor that acquires and performs capture tasks.
 *
 * <p>The physics caller advances movement, battery charge and consumption,
 * hover delays, and operational state through {@link #updatePhysics(double,
 * Duration)}. The dedicated worker started through {@link #run()} executes
 * image processing so expensive capture work does not block physics updates.</p>
 *
 * <p>Essential task-result jobs have priority over optional in-flight video
 * frames. Optional work uses a bounded rolling queue that discards its oldest
 * pending frame when full. Task completion waits for both the worker result and
 * the configured minimum hover duration.</p>
 *
 * <p>A drone must be assembled and configured with a name, model, task queue,
 * and base position before simulation updates begin.</p>
 *
 * @author Dan Hjälmberg
 */
public final class Drone implements Runnable {

    private static final int VIDEO_FRAME_WORK_QUEUE_CAPACITY =
            ModelSettings.VIDEO_TASK_MAX_FRAMES;

    private String name;
    private String thread;
    private final Battery battery;
    private final Camera camera;
    private final Motor motor;

    private Vector2D basePositionMeters;
    private Vector2D currentPositionMeters;
    private Vector2D taskPositionMeters;

    private volatile DroneState state = DroneState.IDLE;

    private TaskQueue taskQueue;
    private Task task;
    // Retain lightweight map markers rather than completed tasks and their images.
    private final ArrayList<TaskSnapshot> completedTasks;

    private Model model;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private String log = "";

    private double videoFrameAccumulatorSeconds = 0.0;

    // Essential task-result work must never compete with optional video frames
    // for queue capacity. The worker always checks this queue first.
    private final BlockingQueue<Runnable> essentialDroneWorkQueue =
            new LinkedBlockingQueue<>();

    // Set an upper bound on optional work so video frames do not pile up indefinitely.
    private final BlockingQueue<Runnable> videoFrameWorkQueue =
            new LinkedBlockingQueue<>(VIDEO_FRAME_WORK_QUEUE_CAPACITY);

    private volatile boolean taskResultWorkInProgress = false;
    private volatile boolean taskResultWorkCompleted = false;

    private boolean taskProcessingStarted = false;
    private double minimumHoverTimeRemainingSeconds = 0.0;

    /**
     * Creates an idle drone with its required components installed.
     *
     * <p>The constructor has package visibility so supported drones are created
     * through {@link DroneFactory}.</p>
     *
     * @param battery battery used by the drone
     * @param camera camera used to capture task imagery
     * @param motor motor used for drone movement
     * @throws NullPointerException if any component is {@code null}
     */
    Drone(
            Battery battery,
            Camera camera,
            Motor motor) {

        this.battery = Objects.requireNonNull(
                battery,
                "Battery must not be null.");

        this.camera = Objects.requireNonNull(
                camera,
                "Camera must not be null.");

        this.motor = Objects.requireNonNull(
                motor,
                "Motor must not be null.");

        this.task = null;
        this.completedTasks = new ArrayList<>();
    }

    /**
     * Assigns a generated name from a model-local identifier.
     *
     * @param id drone identifier.
     */
    public void setName(int id) {
        this.name = "drone_" + id;
    }

    /**
     * Returns the generated drone name.
     *
     * @return name in the form {@code drone_<id>}, or {@code null} before assignment.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the installed battery component.
     *
     * @return installed battery.
     */
    public Battery getBattery() {
        return battery;
    }

    /**
     * Returns the installed camera component.
     *
     * @return installed camera.
     */
    public Camera getCamera() {
        return camera;
    }

    /**
     * Returns the installed motor component.
     *
     * @return installed motor.
     */
    public Motor getMotor() {
        return motor;
    }

    /**
     * Sets both the base and current positions to the supplied world position.
     *
     * @param basePositionMeters base position in world meters.
     */
    public void setBasePositionMeters(Vector2D basePositionMeters) {

        this.basePositionMeters = basePositionMeters;
        this.currentPositionMeters = basePositionMeters;
    }

    /**
     * Returns the base position in world meters.
     *
     * @return base position, or {@code null} before initialization.
     */
    public Vector2D getBasePositionMeters() {
        return basePositionMeters;
    }

    /**
     * Returns the current position in world meters.
     *
     * @return current position, or {@code null} before base initialization.
     */
    public Vector2D getCurrentPositionMeters() {
        return currentPositionMeters;
    }

    /**
     * Returns the current operational state.
     *
     * @return drone state.
     */
    public DroneState getState() {
        return state;
    }

    /**
     * Connects this drone to the shared task queue.
     *
     * @param taskQueue the queue of tasks.
     */
    public void setTaskQueue(TaskQueue taskQueue) {
        this.taskQueue = taskQueue;
    }

    /**
     * Assigns a task directly and caches its target position.
     *
     * <p>This setup method does not set the task start timestamp or emit an
     * assignment event; normal simulation assignment uses
     * {@link #assignTask(Duration)}.</p>
     *
     * @param task task to assign.
     * @throws NullPointerException if {@code task} is {@code null}.
     */
    public void setTask(Task task) {

        this.task = task;
        this.taskPositionMeters = task.getTargetPositionMeters();
    }

    /**
     * Returns the currently assigned task.
     *
     * @return assigned task, or {@code null} when none is assigned.
     */
    public Task getTask() {
        return task;
    }

    /**
     * Returns a snapshot of this drone's bounded completed-task marker history.
     *
     * <p>The returned list may be modified without affecting the drone. Its
     * {@link TaskSnapshot} elements are immutable and contain no task images.</p>
     *
     * @return completed task snapshots.
     */
    public List<TaskSnapshot> getCompletedTasks() {
        return new ArrayList<>(completedTasks);
    }

    /**
     * Captures a consistent presentation snapshot of this drone.
     *
     * <p>All drone-owned mutable state is read while holding the same lock used
     * by simulation updates, preventing values from different simulation instants
     * from being combined in one snapshot.</p>
     *
     * @return a consistent snapshot of the current drone state.
     */
    public synchronized DroneSnapshot createSnapshot() {

        TaskSnapshot assignedTaskSnapshot = task == null
                ? null
                : new TaskSnapshot(
                        task.getName(),
                        task.getType(),
                        task.getTargetPositionMeters());

        return new DroneSnapshot(
                name,
                new BatterySnapshot(
                        battery.getType(),
                        battery.getCapacitySeconds(),
                        battery.getCurrentChargeSeconds()),
                new CameraSnapshot(camera.getType()),
                new MotorSnapshot(
                        motor.getType(),
                        motor.getMaxSpeed(),
                        motor.getCurrentSpeed()),
                basePositionMeters,
                currentPositionMeters,
                state,
                assignedTaskSnapshot,
                new ArrayList<>(completedTasks));
    }

    /**
     * Captures the live camera view at the current world position.
     *
     * <p>No image is produced before position initialization or while the drone
     * is at base. Otherwise the active camera filter is applied.</p>
     *
     * @return captured image, or {@code null} at base or before initialization.
     */
    public BufferedImage createLiveCameraImage() {

        if (currentPositionMeters == null || isAtBase()) {
            return null;
        }

        return takePhoto(currentPositionMeters);
    }

    /**
     * Resets time accumulation used for fixed-rate video capture.
     */
    private void resetTaskCounters() {
        videoFrameAccumulatorSeconds = 0.0;
    }

    /**
     * Connects this drone to the main model.
     *
     * @param model model used for map capture and event publication.
     */
    public void setModel(Model model) {
        this.model = model;
    }

    // ########################################################################
    // Simulation
    // ########################################################################

    /**
     * Runs queued capture work on the calling thread until stopped or interrupted.
     *
     * <p>Essential result work is always polled before optional video frames.
     * Pausing delays execution of dequeued work without pausing physics itself.
     * An interrupt restores the interrupt status and ends the worker.</p>
     */
    @Override
    public void run() {

        thread = Thread.currentThread().getName();
        log("running in " + Thread.currentThread().getName());

        isRunning.set(true);

        while (isRunning.get()) {
            try {
                // Essential completion work has priority over optional video frames.
                Runnable work = essentialDroneWorkQueue.poll();

                if (work == null) {
                    work = videoFrameWorkQueue.poll(
                            ModelSettings.ACTOR_IDLE_PAUSE_MS,
                            TimeUnit.MILLISECONDS);
                }

                if (work == null) {
                    continue;
                }

                while (isPaused.get() && isRunning.get()) {
                    Thread.sleep(ModelSettings.ACTOR_IDLE_PAUSE_MS);
                }

                if (!isRunning.get()) {
                    return;
                }

                work.run();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Requests cooperative suspension of queued worker jobs.
     */
    public void pause() {
        isPaused.set(true);
    }

    /**
     * Allows queued worker jobs to resume.
     */
    public void resume() {
        isPaused.set(false);
    }

    /**
     * Requests cooperative termination of the worker loop.
     */
    public void stop() {
        isRunning.set(false);
    }

    /**
     * Submits essential task-result work to the unbounded priority queue.
     *
     * @param work the work to be executed.
     */
    private void submitTaskResultWork(Runnable work) {
        essentialDroneWorkQueue.offer(work);
    }

    /**
     * Submits an optional video-frame job. Dropping a frame is preferable to
     * delaying or rejecting the essential task-result job.
     *
     * @param work video-frame work to execute
     */
    private void submitVideoFrameWork(Runnable work) {
        if (!videoFrameWorkQueue.offer(work)) {
            // Preserve the newest portion of the flight. When the rolling
            // window is full, discard its oldest pending frame and append the
            // newest position instead.
            videoFrameWorkQueue.poll();

            if (!videoFrameWorkQueue.offer(work)) {
                log("Video frame work queue full. Dropping frame.");
            }
        }
    }

    /**
     * Processes all pending frames in the rolling video window before the
     * video task is finalized. This method runs in the drone worker thread.
     */
    private void processPendingVideoFrames() {
        Runnable pendingFrameWork;

        while ((pendingFrameWork = videoFrameWorkQueue.poll()) != null) {
            pendingFrameWork.run();
        }
    }

    /**
     * Discards queued work belonging to the task that is being aborted.
     * A drone owns only one assigned task at a time, so no queued work can
     * belong to a later task while the synchronized abort is in progress.
     * Work already executing is protected by its task identity check.
     */
    private void discardQueuedTaskWork() {
        int discardedVideoFrames = videoFrameWorkQueue.size();
        int discardedTaskResults = essentialDroneWorkQueue.size();

        videoFrameWorkQueue.clear();
        essentialDroneWorkQueue.clear();

        if (discardedVideoFrames > 0 || discardedTaskResults > 0) {
            log("Discarded queued work for aborted task: "
                    + discardedVideoFrames
                    + " video frame(s), "
                    + discardedTaskResults
                    + " task result(s).");
        }
    }

    /**
     * Resets the task processing state.
     */
    private void resetTaskProcessingState() {

        taskProcessingStarted = false;
        taskResultWorkInProgress = false;
        taskResultWorkCompleted = false;
        minimumHoverTimeRemainingSeconds = 0.0;
    }

    /**
     * Starts processing the current task after the drone reaches the task position.
     *
     * @param minimumHoverMs minimum hover time in milliseconds
     * @param simulationTime current simulation time
     */
    private void startTaskProcessing(int minimumHoverMs, Duration simulationTime) {

        if (transitionStateTo(DroneState.PROCESSING_TASK)) {
            addSimulationEvent(
                    simulationTime,
                    SimulationEventType.TASK_STARTED,
                    task.getName() + " processing started");
        }

        motor.setSpeed(0);

        taskProcessingStarted = true;
        taskResultWorkInProgress = true;
        taskResultWorkCompleted = false;

        minimumHoverTimeRemainingSeconds = minimumHoverMs / 1000.0;
    }

    /**
     * Lets the drone hover for the remaining minimum hover time, if any.
     * This method is called in the physics timer loop while the drone is processing a task.
     *
     * @param dt the time delta in seconds.
     */
    private void hoverAtTaskPosition(double dt) {

        motor.setSpeed(0);

        if (minimumHoverTimeRemainingSeconds > 0.0) {
            minimumHoverTimeRemainingSeconds =
                    Math.max(0.0, minimumHoverTimeRemainingSeconds - dt);
        }

        battery.consume(dt);
    }

    /**
     * Reports whether worker output and the minimum hover delay are complete.
     *
     * @return {@code true} if the current task can finish.
     */
    private boolean taskCanFinish() {

        return taskProcessingStarted
                && taskResultWorkCompleted
                && minimumHoverTimeRemainingSeconds <= 0.0;
    }

    /**
     * Advances this drone by one physics tick.
     *
     * @param dt             simulated seconds advanced by one physics tick.
     * @param simulationTime current simulation time.
     */
    public void updatePhysics(double dt, Duration simulationTime) {

        performSimulation(dt, simulationTime);
    }

    /**
     * Applies state-dependent simulation behavior for one physics tick.
     *
     * @param dt             simulated seconds advanced by one physics tick.
     * @param simulationTime current simulation time.
     */
    public synchronized void performSimulation(double dt, Duration simulationTime) {

        if (isAtBase()) {
            basePositionActions(dt, simulationTime);
        } else if (isAtTask()) {
            taskPositionActions(dt, simulationTime);
        } else {
            defaultPositionActions(dt, simulationTime);
        }
    }

    /**
     * Reports whether current and base positions are equal.
     *
     * @return {@code true} if the drone is at base.
     */
    private boolean isAtBase() {
        return currentPositionMeters.equals(basePositionMeters);
    }

    /**
     * Reports whether the drone has reached its cached task target.
     *
     * @return {@code true} if a target exists and the drone is at that target.
     */
    private boolean isAtTask() {
        return taskPositionMeters != null && currentPositionMeters.equals(taskPositionMeters);
    }

    /**
     * Applies charging, assignment, or departure behavior at base.
     *
     * <p>The drone remains at base until fully charged. Once charged, it polls
     * for a task when idle and begins moving immediately if one is assigned.</p>
     *
     * @param dt             simulated seconds advanced by one physics tick.
     * @param simulationTime current simulation time.
     */
    public void basePositionActions(double dt, Duration simulationTime) {

        motor.setSpeed(0);

        // Do not move from base until battery fully charged
        if (battery.getCurrentChargeSeconds() < battery.getCapacitySeconds()) {

            if (transitionStateTo(DroneState.CHARGING)) {
                addSimulationEvent(
                        simulationTime,
                        SimulationEventType.DRONE_CHARGING,
                        "Started charging");
            }

            battery.charge(dt);

            log("Battery level: " + battery.getCurrentChargeSeconds()
                    + " of " + battery.getCapacitySeconds());
            return;
        }

        // Battery is fully charged
        if (task == null) {

            transitionStateTo(DroneState.IDLE);

            // Try to assign a task
            if (assignTask(simulationTime)) {
                moveToTask(dt);
            }
            // If no task is assigned wait at base position
        } else {
            // The battery is fully charged and there is a task assigned
            moveToTask(dt);
        }
    }

    /**
     * Processes or aborts the assigned task at its target position.
     *
     * <p>Sufficient battery dispatches to behavior for the concrete task type.
     * Otherwise the task is returned to its agency and the drone starts toward
     * base.</p>
     *
     * @param dt             simulated seconds advanced by one physics tick.
     * @param simulationTime current simulation time.
     */
    public void taskPositionActions(double dt, Duration simulationTime) {

        if (sufficientBattery()) {

            if (task instanceof PhotoTask) {
                processPhotoTask(dt, simulationTime);

            } else if (task instanceof VideoTask) {
                processVideoTask(dt, simulationTime);

            } else if (task instanceof ZoomTask) {
                processZoomTask(dt, simulationTime);

            } else {
                Logger.getGlobal().info("Task type is not defined.");
            }

        } else {
            // Battery is not sufficient for continuing, return to base
            if (task != null) {
                discardTask(simulationTime);
            }
            returnToBase(dt, simulationTime);
        }
    }

    /**
     * Applies travel behavior away from both base and the task target.
     *
     * <p>The drone continues toward assigned work while battery is sufficient,
     * recording fixed-rate video positions when applicable. Without work it
     * returns to base. Insufficient battery aborts assigned work first.</p>
     *
     * @param dt             simulated seconds advanced by one physics tick
     * @param simulationTime current simulation time
     */
    public void defaultPositionActions(double dt, Duration simulationTime) {

        if (sufficientBattery()) {
            if (task == null) {
                if (assignTask(simulationTime)) {
                    // A new task is assigned
                    moveToTask(dt);
                } else {
                    returnToBase(dt, simulationTime);
                }
            } else {
                // Battery is sufficient and there is a task assigned
                Vector2D previousPosition = currentPositionMeters;

                moveToTask(dt);

                if (task instanceof VideoTask) {
                    recordVideoFramesBetween(previousPosition, currentPositionMeters, dt);
                }
            }
        } else {
            // The drone cannot complete the task so it has to discard it
            if (task != null) {
                discardTask(simulationTime);
            }
            // Battery is not sufficient for continuing, return to base
            returnToBase(dt, simulationTime);
        }
    }

    // ########################################################################
    // Task processing
    // ########################################################################

    /**
     * Advances photo-task processing at the target.
     *
     * <p>On the first tick, the drone enters the processing state and queues one
     * capture on its worker. Later physics ticks consume hover battery and
     * finish the task only after both capture and the minimum delay complete.</p>
     *
     * @param dt             simulated seconds advanced by one physics tick.
     * @param simulationTime current simulation time.
     */
    public void processPhotoTask(double dt, Duration simulationTime) {

        // Check state
        if (!taskProcessingStarted) {

            startTaskProcessing(ModelSettings.PHOTO_TASK_TARGET_DELAY_MS, simulationTime);

            final Task taskToComplete = task;
            final Vector2D capturePos = taskPositionMeters;

            submitTaskResultWork(() -> {
                try {
                    // Take an aerial photo at the task position
                    BufferedImage aerialPhoto = takePhoto(capturePos);

                    synchronized (this) {
                        if (task == taskToComplete) {
                            // Set time stamp and add image to task
                            task.setImageSimulationTime(simulationTime);
                            task.addImage(aerialPhoto);
                            task.addImagePosition(capturePos);
                            taskResultWorkCompleted = true;
                        }
                    }

                } finally {
                    taskResultWorkInProgress = false;
                }
            });
        }

        hoverAtTaskPosition(dt);

        if (taskCanFinish()) {
            finishTask(simulationTime);
        }
    }

    /**
     * Advances video-task finalization at the target.
     *
     * <p>The worker drains the retained pending frames from the flight and then
     * captures the target as the final frame. Physics ticks continue the hover
     * delay until both worker output and the delay are complete.</p>
     *
     * @param dt             simulated seconds advanced by one physics tick.
     * @param simulationTime current simulation time.
     */
    public void processVideoTask(double dt, Duration simulationTime) {

        if (!taskProcessingStarted) {

            startTaskProcessing(ModelSettings.VIDEO_TASK_TARGET_DELAY_MS, simulationTime);

            final Task taskToComplete = task;
            final Vector2D capturePos = taskPositionMeters;

            submitTaskResultWork(() -> {
                try {
                    // The essential result job has priority over optional
                    // frames. Drain the retained trailing window here so the
                    // completed video contains the last part of the flight.
                    processPendingVideoFrames();

                    BufferedImage finalFrame = takePhoto(capturePos);

                    synchronized (this) {
                        if (task == taskToComplete) {
                            // Always preserve the actual destination as the
                            // final video frame. VideoTask keeps a rolling
                            // window and removes its oldest frame if needed.
                            task.addImage(finalFrame);
                            task.addImagePosition(capturePos);

                            // Set time stamp
                            task.setImageSimulationTime(simulationTime);
                            taskResultWorkCompleted = true;
                        }
                    }

                } finally {
                    taskResultWorkInProgress = false;
                }
            });
        }

        hoverAtTaskPosition(dt);

        if (taskCanFinish()) {
            finishTask(simulationTime);
        }
    }

    /**
     * Queues fixed-rate video frames along one physics movement segment.
     *
     * <p>Capture positions are interpolated between the previous and next
     * world positions, making registration independent of physics tick size.
     * The worker later creates each image at its registered position.</p>
     *
     * @param previousPos movement-segment start in world meters.
     * @param nextPos     movement-segment end in world meters.
     * @param dt          simulated movement duration in seconds.
     */
    private void recordVideoFramesBetween(Vector2D previousPos, Vector2D nextPos, double dt) {

        double frameIntervalSeconds = 1.0 / ModelSettings.VIDEO_TASK_FPS;

        videoFrameAccumulatorSeconds += dt;

        while (videoFrameAccumulatorSeconds >= frameIntervalSeconds) {

            double timeIntoStep = dt - videoFrameAccumulatorSeconds + frameIntervalSeconds;
            double fraction = dt <= 0.0 ? 1.0 : timeIntoStep / dt;

            fraction = Math.max(0.0, Math.min(1.0, fraction));

            Vector2D framePos = interpolate(previousPos, nextPos, fraction);
            Task videoTask = task;

            submitVideoFrameWork(() -> {
                BufferedImage frame = takePhoto(framePos);

                synchronized (this) {
                    if (task == videoTask) {
                        task.addImage(frame);
                        task.addImagePosition(framePos);
                    }
                }
            });

            videoFrameAccumulatorSeconds -= frameIntervalSeconds;
        }
    }

    /**
     * Returns a linear interpolation between two positions.
     *
     * @param from     starting position.
     * @param to       ending position.
     * @param fraction interpolation factor, normally between {@code 0.0} and {@code 1.0}.
     * @return interpolated position.
     */
    private Vector2D interpolate(Vector2D from, Vector2D to, double fraction) {

        double x = from.getX() + (to.getX() - from.getX()) * fraction;
        double y = from.getY() + (to.getY() - from.getY()) * fraction;

        return new Vector2D(x, y);
    }

    /**
     * Advances zoom-task processing at the target.
     *
     * <p>The worker captures one source image, generates the configured number
     * of progressively tighter crops at the source resolution, and records one
     * shared capture position for the resulting sequence.</p>
     *
     * @param dt             simulated seconds advanced by one physics tick.
     * @param simulationTime current simulation time.
     */
    private void processZoomTask(double dt, Duration simulationTime) {

        if (!taskProcessingStarted) {

            startTaskProcessing(ModelSettings.ZOOM_TASK_TARGET_DELAY_MS, simulationTime);

            final Task taskToComplete = task;
            final Vector2D capturePos = taskPositionMeters;

            submitTaskResultWork(() -> {
                try {
                    BufferedImage originalImage = takePhoto(capturePos);
                    List<BufferedImage> zoomFrames = new ArrayList<>();

                    for (int i = 0; i < ModelSettings.ZOOM_TASK_FRAME_COUNT; i++) {

                        double t = (double) i / (ModelSettings.ZOOM_TASK_FRAME_COUNT - 1);

                        double scale = ModelSettings.ZOOM_TASK_START_SCALE
                                + (ModelSettings.ZOOM_TASK_END_SCALE - ModelSettings.ZOOM_TASK_START_SCALE) * t;

                        int cropWidth = Math.max(1, (int) Math.round(originalImage.getWidth() * scale));
                        int cropHeight = Math.max(1, (int) Math.round(originalImage.getHeight() * scale));

                        // Use subpixel precision in both image cropping and resampling
                        Vector2D centerPoint = new Vector2D(
                                originalImage.getWidth() * 0.5,
                                originalImage.getHeight() * 0.5);

                        BufferedImage croppedImage = ImageUtils.cropImageSubpixel(
                                originalImage,
                                centerPoint,
                                new Dimension(cropWidth, cropHeight),
                                ModelSettings.CAMERA_INTERPOLATION);

                        BufferedImage zoomFrame = ImageUtils.resampleImage(
                                croppedImage,
                                originalImage.getWidth(),
                                originalImage.getHeight(),
                                ModelSettings.CAMERA_INTERPOLATION);

                        zoomFrames.add(zoomFrame);
                    }

                    synchronized (this) {
                        if (task == taskToComplete) {
                            for (BufferedImage frame : zoomFrames) {
                                task.addImage(frame);
                            }

                            // Every generated frame shares this capture position.
                            task.addImagePosition(capturePos);

                            task.setImageSimulationTime(simulationTime);
                            taskResultWorkCompleted = true;
                        }
                    }

                } finally {
                    taskResultWorkInProgress = false;
                }
            });
        }

        hoverAtTaskPosition(dt);

        if (taskCanFinish()) {
            finishTask(simulationTime);
        }
    }

    /**
     * Finalizes the assigned task and makes the drone idle.
     *
     * <p>A lightweight marker is retained by the drone, while the full task is
     * returned to its agency for completion timestamping and archival. Task
     * references, counters, and processing flags are then cleared.</p>
     *
     * @param simulationTime current simulation time
     */
    public synchronized void finishTask(Duration simulationTime) {

        recordCompletedTaskMarker(simulationTime);

        transmitTask(simulationTime);

        task = null;
        taskPositionMeters = null;

        resetTaskCounters();
        resetTaskProcessingState();

        transitionStateTo(DroneState.IDLE);
    }

    /**
     * Polls and assigns the oldest task from the shared queue.
     *
     * <p>Successful assignment records the elapsed start time, caches the
     * target, resets video timing when applicable, and emits an assignment
     * event. The operation does not wait for work to become available.</p>
     *
     * @param simulationTime current simulation time.
     * @return {@code true} if a task was assigned; {@code false} if the queue was empty.
     */
    public boolean assignTask(Duration simulationTime) {

        this.task = taskQueue.getTask();

        if (this.task == null) {
            return false;
        }

        this.task.setStartSimulationTime(simulationTime);
        taskPositionMeters = this.task.getTargetPositionMeters();

        if (this.task instanceof VideoTask) {
            resetTaskCounters();
        }

        log(this.name + " is assigned " + this.task.getName());

        log(this.task.getName()
                + " started at "
                + TimeUtils.formatSimulationTime(task.getStartSimulationTime()));

        addSimulationEvent(
                simulationTime,
                SimulationEventType.TASK_ASSIGNED,
                this.task.getName() + " assigned");

        return true;
    }

    /**
     * Accelerates or decelerates the motor for the remaining travel distance.
     *
     * @param distanceToTargetMeters distance to target in meters.
     * @param dt                     simulated time step in seconds.
     */
    private void updateMotorSpeedForTarget(
            double distanceToTargetMeters,
            double dt) {

        double currentSpeedMetersPerSecond = motor.getCurrentSpeed();
        double maxSpeedMetersPerSecond = motor.getMaxSpeed();

        double accelerationMetersPerSecondSquared =
                ModelSettings.DRONE_ACCELERATION_METERS_PER_SECOND_SQUARED;

        double decelerationMetersPerSecondSquared =
                ModelSettings.DRONE_DECELERATION_METERS_PER_SECOND_SQUARED;

        double stoppingDistanceMeters =
                currentSpeedMetersPerSecond
                        * currentSpeedMetersPerSecond
                        / (2.0 * decelerationMetersPerSecondSquared);

        if (distanceToTargetMeters <= stoppingDistanceMeters) {
            currentSpeedMetersPerSecond = Math.max(
                    0.0,
                    currentSpeedMetersPerSecond - decelerationMetersPerSecondSquared * dt);
        } else {
            currentSpeedMetersPerSecond = Math.min(
                    maxSpeedMetersPerSecond,
                    currentSpeedMetersPerSecond + accelerationMetersPerSecondSquared * dt);
        }

        motor.setSpeed(currentSpeedMetersPerSecond);
    }

    /**
     * Calculates one accelerated movement step toward a world position.
     *
     * <p>The returned position snaps to the target rather than overshooting it,
     * and motor speed becomes zero on arrival.</p>
     *
     * @param targetMeters target position in world meters.
     * @param dt           simulated time step in seconds.
     * @return new drone position in world meters.
     */
    private Vector2D moveTowardTarget(Vector2D targetMeters, double dt) {

        double distanceToTargetMeters = currentPositionMeters.distanceTo(targetMeters);

        updateMotorSpeedForTarget(distanceToTargetMeters, dt);

        double movementDistanceMeters = motor.getCurrentSpeed() * dt;

        if (distanceToTargetMeters <= movementDistanceMeters) {
            motor.setSpeed(0);
            return targetMeters;
        }

        return currentPositionMeters.moveToward(
                targetMeters,
                movementDistanceMeters);
    }

    /**
     * Advances the drone toward its assigned target for one physics tick.
     *
     * <p>The state becomes {@link DroneState#MOVING_TO_TASK}, and battery
     * operating time is consumed by {@code dt}.</p>
     *
     * @param dt simulated time step in seconds
     */
    public void moveToTask(double dt) {

        transitionStateTo(DroneState.MOVING_TO_TASK);

        currentPositionMeters = moveTowardTarget(taskPositionMeters, dt);

        battery.consume(dt);
    }

    /**
     * Advances the drone toward base for one physics tick.
     *
     * <p>The transition event is emitted only when entering
     * {@link DroneState#RETURNING_TO_BASE}. Battery operating time is consumed
     * by {@code dt}.</p>
     *
     * @param dt             simulated time step in seconds
     * @param simulationTime current simulation time
     */
    public void returnToBase(double dt, Duration simulationTime) {

        if (transitionStateTo(DroneState.RETURNING_TO_BASE)) {

            log("Started returning to base");

            addSimulationEvent(
                    simulationTime,
                    SimulationEventType.DRONE_RETURNING_TO_BASE,
                    "Started returning to base");
        }

        currentPositionMeters = moveTowardTarget(basePositionMeters, dt);

        battery.consume(dt);
    }

    /**
     * Changes the drone state if it differs from the current state.
     *
     * @param newState the new drone state
     * @return true if the state changed, false if it was already the same
     */
    private boolean transitionStateTo(DroneState newState) {

        if (state == newState) {
            return false;
        }

        state = newState;
        return true;
    }

    /**
     * Reports whether current charge exceeds estimated return time plus the
     * configured safety margin.
     *
     * <p>Return time is estimated from straight-line world-meter distance and
     * maximum motor speed; acceleration and deceleration are not included. The
     * comparison is strict, so charge exactly equal to the requirement is not
     * sufficient.</p>
     *
     * @return {@code true} if remaining operating seconds exceed the estimate and margin.
     */
    public boolean sufficientBattery() {

        double distanceToBaseMeters = basePositionMeters.distanceTo(currentPositionMeters);

        double travelTimeToBaseSeconds = distanceToBaseMeters / motor.getMaxSpeed();

        double requiredSeconds = travelTimeToBaseSeconds + ModelSettings.BATTERY_DURATION_SAFETY_MARGIN_SECONDS;

        return battery.getCurrentChargeSeconds() > requiredSeconds;
    }

    /**
     * Aborts the assigned task and returns it to its originating agency.
     *
     * <p>Queued work for the task is discarded, local task-processing state is
     * cleared, and the agency resets the task for a later retry. A call without
     * an assigned task has no effect.</p>
     *
     * @param simulationTime current simulation time
     */
    public synchronized void discardTask(Duration simulationTime) {

        if (task == null) {
            return;
        }

        Task abortedTask = task;

        log(abortedTask.getName() + " aborted due to insufficient battery.");

        addSimulationEvent(
                simulationTime,
                SimulationEventType.TASK_ABORTED,
                abortedTask.getName() + " aborted due to insufficient battery");

        PhotoAgency photoAgency = abortedTask.getPhotoAgency();

        discardQueuedTaskWork();

        task = null;
        taskPositionMeters = null;

        resetTaskCounters();
        resetTaskProcessingState();

        if (photoAgency != null) {

            photoAgency.receiveAbortedTask(
                    abortedTask,
                    getName());

            log(abortedTask.getName() + " returned to " + photoAgency.getName());
        }
    }

    /**
     * Records a bounded lightweight marker for the completed task.
     *
     * <p>The marker preserves target and capture positions for map display but
     * does not retain task images. Oldest markers are removed when the
     * configured history limit is exceeded.</p>
     *
     * @param simulationTime current simulation time.
     */
    private void recordCompletedTaskMarker(Duration simulationTime) {

        if (task == null) {
            return;
        }

        log(task.getName() + " is completed.");

        addSimulationEvent(
                simulationTime,
                SimulationEventType.TASK_COMPLETED,
                task.getName() + " completed");

        completedTasks.add(new TaskSnapshot(
                task.getName(),
                task.getType(),
                task.getTargetPositionMeters(),
                task.getImagePositionsMeters()));

        while (completedTasks.size() > ModelSettings.COMPLETED_TASK_MARKER_HISTORY_MAX_SIZE) {
            completedTasks.remove(0);
        }
    }

    /**
     * Captures a camera-resolution image centered on a world position.
     *
     * <p>The position is converted to world-image pixels, a subpixel crop is
     * extracted from the active cropped map image, and the installed camera
     * filter is applied.</p>
     *
     * @param worldPointMeters world position in meters.
     * @return processed camera image.
     */
    public BufferedImage takePhoto(Vector2D worldPointMeters) {

        BufferedImage photo = model.getMapImageCropped();

        Vector2D worldPointPixels = model.worldMetersToPixels(worldPointMeters);

        Dimension cameraResolution = new Dimension(
                ModelSettings.CAMERA_RESOLUTION_WIDTH,
                ModelSettings.CAMERA_RESOLUTION_HEIGHT);

        BufferedImage photoDetail = ImageUtils.cropImageSubpixel(
                photo,
                worldPointPixels,
                cameraResolution,
                ModelSettings.CAMERA_INTERPOLATION);

        return camera.applyFilter(photoDetail);
    }

    /**
     * Returns the assigned completed task to its originating agency.
     *
     * <p>The agency records completion time and archives the task.</p>
     *
     * @param simulationTime current simulation time
     */
    public void transmitTask(Duration simulationTime) {

        PhotoAgency photoAgency = task.getPhotoAgency();

        photoAgency.receiveCompletedTask(task, simulationTime);
    }

    /**
     * Appends a line to the drone's in-memory diagnostic log.
     *
     * @param message message to append.
     */
    public void log(String message) {
        log += (message + "\n");
    }

    /**
     * Returns the accumulated diagnostic log.
     *
     * @return diagnostic log text.
     */
    public String getLog() {
        return log;
    }

    /**
     * Adds a simulation event.
     *
     * @param simulationTime current simulation time
     * @param type           event type
     * @param message        event description
     */
    private void addSimulationEvent(
            Duration simulationTime,
            SimulationEventType type,
            String message) {

        if (model != null) {
            model.addSimulationEvent(
                    simulationTime,
                    type,
                    name,
                    message);
        }
    }

    /**
     * Formats a world meter position for log output.
     *
     * @param positionMeters position in world meters.
     * @return formatted meter position.
     */
    private String formatMeterPosition(Vector2D positionMeters) {

        if (positionMeters == null) {
            return "";
        }

        return "("
                + String.format("%.0f", positionMeters.getX())
                + ", "
                + String.format("%.0f", positionMeters.getY())
                + ")";
    }

    /**
     * Returns multiline diagnostic information about the drone's current state.
     *
     * @return formatted drone information.
     */
    @Override
    public String toString() {

        StringBuilder info = new StringBuilder();
        info.append(name).append("\n");
        info.append("Running in ").append(thread).append("\n");
        info.append("State: ").append(state).append("\n");
        info.append(battery.toString());
        info.append(camera.toString());
        info.append(motor.toString());

        info.append("Current position m: ")
                .append(formatMeterPosition(currentPositionMeters))
                .append("\n");

        if (task == null) {
            info.append("Processing: No task is assigned.").append("\n");

            info.append("Target position m: ").append("\n");

        } else {
            info.append("Processing: ").append(task.getName()).append("\n");

            info.append("Target position m: ")
                    .append(formatMeterPosition(task.getTargetPositionMeters()))
                    .append("\n");
        }
        return info.toString();
    }
}
