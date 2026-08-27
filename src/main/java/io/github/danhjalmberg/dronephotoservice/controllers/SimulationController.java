package io.github.danhjalmberg.dronephotoservice.controllers;

import io.github.danhjalmberg.dronephotoservice.models.Model;
import io.github.danhjalmberg.dronephotoservice.models.events.SimulationEventType;
import io.github.danhjalmberg.dronephotoservice.settings.AppSettings;
import io.github.danhjalmberg.dronephotoservice.views.View;
import io.github.danhjalmberg.dronephotoservice.views.viewdata.SimulationHeaderViewData;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns simulation lifecycle state and periodic physics and GUI scheduling.
 *
 * <p>A single scheduled executor advances model physics away from the EDT,
 * while a Swing timer performs GUI refreshes on the EDT. Startup is
 * transactional at the controller level: partial actor or timer creation is
 * rolled back asynchronously. Stop and shutdown publish {@link
 * SimulationState#STOPPING} while blocking executor waits run in a
 * {@link SwingWorker}.</p>
 *
 * @author Dan Hjälmberg
 */
public class SimulationController {

    // Physics updates are expected to be short operations. The physics executor
    // has already received shutdownNow() before this timeout is used.
    private static final long PHYSICS_EXECUTOR_SHUTDOWN_TIMEOUT_MS = 500L;

    private static final Logger LOGGER = Logger.getLogger(SimulationController.class.getName());

    private static final int GUI_REFRESH_INITIAL_DELAY_MS = 0;

    private final Model model;
    private final View view;

    private final Runnable updateGUI;
    private final Runnable updateMapView;
    private final Runnable clearSelection;
    private final Runnable resetEventLog;

    private SimulationState simulationState = SimulationState.NO_MAP_LOADED;

    // Physics stays off the EDT; actor workers perform heavier queued result work.
    private ScheduledExecutorService physicsExecutor;

    private Timer guiRefreshTimer;

    /**
     * Creates a lifecycle controller in {@link SimulationState#NO_MAP_LOADED}.
     *
     * @param model          the model to be used by the simulation controller
     * @param view           the view to be used by the simulation controller
     * @param updateGUI      EDT callback that refreshes periodic view state.
     * @param updateMapView  callback that redraws the map projection.
     * @param clearSelection callback that clears view selection.
     * @param resetEventLog  callback that resets the displayed-event cursor.
     */
    public SimulationController(
            Model model,
            View view,
            Runnable updateGUI,
            Runnable updateMapView,
            Runnable clearSelection,
            Runnable resetEventLog) {

        this.model = model;
        this.view = view;
        this.updateGUI = updateGUI;
        this.updateMapView = updateMapView;
        this.clearSelection = clearSelection;
        this.resetEventLog = resetEventLog;
    }

    /**
     * Returns the current controller lifecycle state.
     *
     * @return the current simulation state
     */
    public SimulationState getSimulationState() {
        return simulationState;
    }

    /**
     * Publishes the no-map lifecycle state and refreshes the header.
     */
    public void setNoMapLoaded() {
        simulationState = SimulationState.NO_MAP_LOADED;
        updateSimulationHeader();
    }

    /**
     * Clears run-specific state after a map has loaded successfully.
     *
     * <p>Timers are stopped, model and presentation state are reset, and the
     * lifecycle enters {@link SimulationState#READY}. The loaded map survives
     * the model reset.</p>
     */
    public void prepareForLoadedMap() {

        stopSimulationTimers();

        model.resetSimulation();
        view.clearSimulationDisplay();
        resetEventLog.run();
        clearSelection.run();

        simulationState = SimulationState.READY;

        updateSimulationHeader();
    }

    /**
     * Changes the delay of an existing GUI refresh timer.
     *
     * <p>A call before timer creation has no effect.</p>
     *
     * @param intervalMs the refresh interval in milliseconds.
     */
    public void setGuiRefreshInterval(int intervalMs) {
        if (guiRefreshTimer != null) {
            guiRefreshTimer.setDelay(intervalMs);
        }
    }

    /**
     * Replaces an existing physics scheduler using the current model interval.
     *
     * <p>A call before scheduler creation has no effect. The previous executor
     * receives immediate shutdown, but this reconfiguration path does not wait
     * for its termination.</p>
     */
    public void setPhysicsTimerInterval() {
        if (physicsExecutor != null) {
            startPhysicsTimer();
        }
    }

    /**
     * Requests a compact header refresh from current lifecycle and model state.
     */
    public void refreshSimulationHeader() {

        updateSimulationHeader();
    }

    /**
     * Posts a compact simulation-header update to the EDT.
     */
    private void updateSimulationHeader() {

        SimulationHeaderViewData data = new SimulationHeaderViewData(
                AppSettings.APPLICATION_NAME,
                simulationState,
                model.getSimulationTime(),
                model.getSimulationSpeedMultiplier(),
                model.getMapFilePath());

        SwingUtilities.invokeLater(() -> view.displaySimulationHeader(data));
    }

    /**
     * Discards the completed simulation and prepares a new simulation using
     * the currently loaded map.
     *
     * <p>The completed run remains available for inspection and export until
     * this operation is invoked. Creating a new simulation clears run-specific
     * model data, displayed simulation data, event-log presentation state, and
     * selection state. It then returns the lifecycle to {@link
     * SimulationState#READY}, where setup controls and Start become available.</p>
     *
     * <p>This operation is permitted only in {@link SimulationState#STOPPED}.
     * Calls made in any other lifecycle state have no effect.</p>
     */
    public void newSimulation() {

        if (simulationState != SimulationState.STOPPED) {
            return;
        }

        stopSimulationTimers();
        model.resetSimulation();
        view.clearSimulationDisplay();
        resetEventLog.run();
        clearSelection.run();
        updateMapView.run();

        simulationState = SimulationState.READY;
        updateSimulationHeader();
    }

    /**
     * Starts a prepared simulation when lifecycle state is {@link
     * SimulationState#READY}.
     *
     * <p>Run data and settings are prepared, actors are created, and timers start
     * before {@link SimulationState#RUNNING} is published. Any runtime failure
     * starts asynchronous rollback: timers stop, actors receive stop requests,
     * partial executors terminate, and the model is reset before returning to
     * READY. Calls in other states have no effect.</p>
     *
     * @param stateUpdateAction optional EDT control-refresh callback invoked for
     *                          published startup or rollback states.
     */
    public void startSimulation(Runnable stateUpdateAction) {

        if (simulationState != SimulationState.READY) {
            return;
        }

        try {
            prepareSimulationData();
            createSimulationActors();
            startSimulationTimers();

            // Do not expose RUNNING until every required runtime resource exists.
            simulationState = SimulationState.RUNNING;

            model.addSimulationEvent(
                    model.getSimulationTime(),
                    SimulationEventType.SIMULATION_STARTED,
                    "simulation",
                    "Simulation started");

            updateSimulationHeader();
            runStateUpdateAction(stateUpdateAction);

        } catch (RuntimeException exception) {
            rollbackFailedStartup(exception, stateUpdateAction);
        }
    }

    /**
     * Clears earlier run state and applies queue and speed settings from the view.
     *
     * <p>No background runtime resource is created here.</p>
     */
    private void prepareSimulationData() {

        model.resetSimulation();
        view.clearSimulationDisplay();
        resetEventLog.run();
        clearSelection.run();
        updateMapView.run();

        model.setTaskQueueCapacity(view.getTaskQueueSize());

        model.setSimulationSpeedMultiplier(view.getSimulationSpeedMultiplier());
    }

    /**
     * Creates actor executors, actors, and a shared random drone base.
     *
     * <p>The caller owns rollback if this operation fails partway through.</p>
     */
    private void createSimulationActors() {

        int photoAgencyPoolSize = view.getPhotoAgencyPoolSize();

        model.createPhotoAgencyPool(photoAgencyPoolSize);

        for (int i = 0; i < photoAgencyPoolSize; i++) {
            model.addPhotoAgency();
        }

        model.createRandomBasePosition();

        int dronePoolSize = view.getDronePoolSize();

        model.createDronePool(dronePoolSize);

        for (int i = 0; i < dronePoolSize; i++) {
            model.addDrone();
        }
    }

    /**
     * Starts background physics scheduling and the EDT GUI refresh timer after
     * actor creation.
     */
    private void startSimulationTimers() {

        startPhysicsTimer();

        guiRefreshTimer = new Timer(view.getGuiRefreshIntervalMs(), event -> updateGuiRefreshTick());

        guiRefreshTimer.setRepeats(true);
        guiRefreshTimer.setInitialDelay(GUI_REFRESH_INITIAL_DELAY_MS);

        guiRefreshTimer.start();
    }

    /**
     * Rolls back a simulation startup that failed after initialization began.
     * Timer cancellation and actor stop requests happen immediately on the EDT.
     * Blocking executor termination is delegated to the existing background
     * shutdown worker. Model state is reset only after executor termination has
     * been confirmed.
     *
     * @param startupFailure    exception that caused startup to fail
     * @param stateUpdateAction operation that refreshes enabled/disabled controls
     */
    private void rollbackFailedStartup(
            RuntimeException startupFailure,
            Runnable stateUpdateAction) {

        LOGGER.log(
                Level.SEVERE,
                "Simulation startup failed. Rolling back partially initialized resources.",
                startupFailure);

        // Stop all possible sources of further work. These methods are safe when
        // startup failed before some or all resources were created
        ScheduledExecutorService stoppedPhysicsExecutor = stopSimulationTimers();

        model.stopSimulationActors();

        // STOPPING prevents the user from starting, resetting or loading another
        // simulation while partially created executors are being terminated
        simulationState = SimulationState.STOPPING;

        updateSimulationHeader();
        runStateUpdateAction(stateUpdateAction);

        // Reset shared model state only after every partially created executor has terminated
        // This prevents old actor work from accessing reset state
        shutdownExecutorsAsync(stoppedPhysicsExecutor, () -> completeStartupRollback(stateUpdateAction));

        view.displayErrorMessage(
                "Could not start simulation",
                "The simulation could not be started because an unexpected error occurred.\n\n"
                        + "Partially initialized resources are being cleaned up.");
    }

    /**
     * Completes startup rollback after all partially created executors have terminated.
     * This method runs on Swing's Event Dispatch Thread through SwingWorker.done().
     *
     * @param stateUpdateAction operation that refreshes enabled/disabled controls
     */
    private void completeStartupRollback(Runnable stateUpdateAction) {

        // Executor termination has now been confirmed
        // Clearing actor collections and task state is safe
        model.resetSimulation();

        view.clearSimulationDisplay();
        resetEventLog.run();
        clearSelection.run();
        updateMapView.run();

        // The loaded map and user-selected setup values remain available
        // The user may correct the cause where possible and try starting again
        simulationState = SimulationState.READY;

        updateSimulationHeader();
        runStateUpdateAction(stateUpdateAction);
    }

    /**
     * Pauses a running simulation.
     *
     * <p>Actor work is cooperatively suspended, physics scheduling is stopped,
     * {@link SimulationState#PAUSED} and its event are published, and one final
     * GUI refresh occurs before periodic refresh stops. Calls outside RUNNING
     * have no effect.</p>
     */
    public void pauseSimulation() {

        if (simulationState != SimulationState.RUNNING) {
            return;
        }

        model.pauseSimulationActors();

        stopPhysicsTimer();

        simulationState = SimulationState.PAUSED;

        model.addSimulationEvent(
                model.getSimulationTime(),
                SimulationEventType.SIMULATION_PAUSED,
                "simulation",
                "Simulation paused");

        // Display the final events before periodic GUI refresh is stopped
        updateGUI.run();

        if (guiRefreshTimer != null) {
            guiRefreshTimer.stop();
        }

        updateSimulationHeader();
    }

    /**
     * Resumes a paused simulation.
     *
     * <p>Actor work resumes, RUNNING and its event are published and displayed,
     * and physics and periodic GUI scheduling restart. Calls outside PAUSED have
     * no effect.</p>
     */
    public void resumeSimulation() {

        if (simulationState != SimulationState.PAUSED) {
            return;
        }

        model.resumeSimulationActors();

        simulationState = SimulationState.RUNNING;

        model.addSimulationEvent(
                model.getSimulationTime(),
                SimulationEventType.SIMULATION_RESUMED,
                "simulation",
                "Simulation resumed");

        // Display the resume event immediately
        updateGUI.run();

        startPhysicsTimer();

        if (guiRefreshTimer != null) {
            guiRefreshTimer.start();
        }

        updateSimulationHeader();
    }

    /**
     * Stops a running or paused simulation asynchronously.
     *
     * <p>The controller enters {@link SimulationState#STOPPING} immediately and
     * prevents further lifecycle commands while physics and actor executors
     * terminate off the EDT. After successful termination, the state changes to
     * {@link SimulationState#STOPPED}, the final event and view state are
     * published, and the completion action runs on the EDT. If termination fails,
     * the controller remains in {@code STOPPING} because resetting shared model
     * state would be unsafe. Calls in other states have no effect.</p>
     *
     * @param completionAction optional operation to run after successful shutdown
     */
    public void stopSimulation(Runnable completionAction) {

        if (simulationState != SimulationState.RUNNING && simulationState != SimulationState.PAUSED) {
            return;
        }

        // Stop sources of new actor work before requesting actor and executor
        // shutdown. Keep the detached physics executor so its termination can be
        // confirmed by the background shutdown worker.
        ScheduledExecutorService stoppedPhysicsExecutor = stopSimulationTimers();

        model.stopSimulationActors();

        simulationState = SimulationState.STOPPING;
        updateSimulationHeader();

        shutdownExecutorsAsync(stoppedPhysicsExecutor, () -> {

            simulationState = SimulationState.STOPPED;

            model.addSimulationEvent(
                    model.getSimulationTime(),
                    SimulationEventType.SIMULATION_STOPPED,
                    "simulation",
                    "Simulation stopped");

            // Periodic refresh has stopped, so publish the final event explicitly.
            updateGUI.run();
            updateSimulationHeader();

            if (completionAction != null) {
                completionAction.run();
            }
        });
    }

    /**
     * Stops both periodic update mechanisms and returns the detached physics
     * executor.
     *
     * <p>The returned executor has received {@link java.util.concurrent.ExecutorService#shutdownNow()}
     * but may still be completing an update. Full shutdown paths must await its
     * termination off the EDT; pause and timer reconfiguration may ignore it.</p>
     *
     * @return the physics executor that was stopped, or null if none existed
     */
    private ScheduledExecutorService stopSimulationTimers() {

        ScheduledExecutorService stoppedPhysicsExecutor = stopPhysicsTimer();

        stopGuiRefreshTimer();

        return stoppedPhysicsExecutor;
    }

    /**
     * Stops and discards the EDT GUI refresh timer, if one exists.
     */
    private void stopGuiRefreshTimer() {
        if (guiRefreshTimer != null) {
            guiRefreshTimer.stop();
            guiRefreshTimer = null;
        }
    }

    /**
     * Waits asynchronously for all simulation executors to terminate.
     *
     * <p>Actor pools must already have received their cooperative stop request,
     * and the supplied physics executor must already have received
     * {@code shutdownNow()}. Blocking waits run in a {@link SwingWorker}; its
     * completion action and any error dialog run on the EDT. The completion action
     * is not invoked if an executor fails to terminate.</p>
     *
     * @param stoppedPhysicsExecutor physics executor previously stopped by
     *                               stopSimulationTimers(), or null
     * @param completionAction       operation to run after all executors terminate
     */
    private void shutdownExecutorsAsync(
            ScheduledExecutorService stoppedPhysicsExecutor,
            Runnable completionAction) {

        SwingWorker<Boolean, Void> shutdownWorker =
                new SwingWorker<>() {

                    @Override
                    protected Boolean doInBackground()
                            throws InterruptedException {

                        boolean actorExecutorsTerminated = model.shutdownActorPools();

                        boolean physicsExecutorTerminated = awaitPhysicsExecutorTermination(stoppedPhysicsExecutor);

                        return actorExecutorsTerminated && physicsExecutorTerminated;
                    }

                    @Override
                    protected void done() {

                        try {
                            boolean terminated = get();

                            if (!terminated) {
                                view.displayErrorMessage(
                                        "Could not stop simulation",
                                        "One or more simulation executors did not "
                                                + "stop within the shutdown timeout.\n\n"
                                                + "The simulation cannot be reset safely.");

                                return;
                            }

                            if (completionAction != null) {
                                completionAction.run();
                            }

                        } catch (InterruptedException exception) {
                            // Preserve the EDT's interruption status. This exception is
                            // unlikely because doInBackground() failures normally arrive
                            // through ExecutionException.
                            Thread.currentThread().interrupt();

                            view.displayErrorMessage(
                                    "Shutdown interrupted",
                                    "Waiting for the simulation executors was interrupted.\n\n"
                                            + "The simulation cannot be reset safely.");

                        } catch (ExecutionException exception) {
                            view.displayErrorMessage(
                                    "Could not stop simulation",
                                    "An unexpected error occurred while stopping the simulation executors.\n\n"
                                            + "The simulation cannot be reset safely.");
                        }
                    }
                };

        shutdownWorker.execute();
    }

    /**
     * Replaces the physics scheduler with a single-threaded fixed-rate scheduler.
     * Physics updates begin immediately and run off the EDT at the model's current
     * actor interval.
     */
    private void startPhysicsTimer() {

        stopPhysicsTimer();

        physicsExecutor = Executors.newSingleThreadScheduledExecutor();

        physicsExecutor.scheduleAtFixedRate(
                this::updateSimulationPhysics,
                0,
                model.getActorSleepMs(),
                TimeUnit.MILLISECONDS);
    }

    /**
     * Advances the model clock and physics by one scheduled update.
     */
    private void updateSimulationPhysics() {

        model.updatePhysics();
    }

    /**
     * Stops the current physics executor and detaches it from the controller.
     *
     * <p>{@code shutdownNow()} prevents additional scheduled updates and
     * interrupts a running update. Because interruption is cooperative, the
     * returned executor may not yet be fully terminated.</p>
     *
     * @return the stopped executor, or null if no physics executor existed
     */
    private ScheduledExecutorService stopPhysicsTimer() {

        ScheduledExecutorService stoppedExecutor = physicsExecutor;

        physicsExecutor = null;

        if (stoppedExecutor != null) {
            stoppedExecutor.shutdownNow();
        }

        return stoppedExecutor;
    }

    /**
     * Waits for a previously stopped physics executor to terminate.
     * This bounded wait must run off the EDT.
     *
     * @param executor previously stopped physics executor, or null
     * @return true if the executor terminated or no executor existed
     * @throws InterruptedException if the waiting thread is interrupted
     */
    private static boolean awaitPhysicsExecutorTermination(
            ScheduledExecutorService executor)
            throws InterruptedException {

        if (executor == null) {
            return true;
        }

        return executor.awaitTermination(
                PHYSICS_EXECUTOR_SHUTDOWN_TIMEOUT_MS,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Refreshes the simulation view and lifecycle header for one EDT timer tick.
     */
    private void updateGuiRefreshTick() {

        updateGUI.run();
        updateSimulationHeader();
    }

    /**
     * Runs an optional callback after a simulation lifecycle state change.
     *
     * @param stateUpdateAction callback that updates application controls
     */
    private static void runStateUpdateAction(Runnable stateUpdateAction) {

        if (stateUpdateAction != null) {
            stateUpdateAction.run();
        }
    }

    /**
     * Shuts down all simulation resources before application disposal.
     *
     * <p>Timers and actor work receive immediate stop requests, the lifecycle
     * enters {@link SimulationState#STOPPING}, and executor termination is awaited
     * off the EDT. Unlike {@link #stopSimulation(Runnable)}, application shutdown
     * does not publish {@link SimulationState#STOPPED} or a stopped event; after
     * successful termination it invokes the completion action on the EDT. If
     * termination fails, the action is not invoked and the state remains
     * {@code STOPPING}.</p>
     *
     * @param completionAction operation to run after successful shutdown
     */
    public void shutdown(Runnable completionAction) {
        ScheduledExecutorService stoppedPhysicsExecutor = stopSimulationTimers();

        model.stopSimulationActors();

        simulationState = SimulationState.STOPPING;
        updateSimulationHeader();

        shutdownExecutorsAsync(
                stoppedPhysicsExecutor,
                completionAction);
    }
}
