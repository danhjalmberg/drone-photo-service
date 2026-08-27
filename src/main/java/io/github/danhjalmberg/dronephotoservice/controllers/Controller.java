package io.github.danhjalmberg.dronephotoservice.controllers;

import io.github.danhjalmberg.dronephotoservice.models.Model;
import io.github.danhjalmberg.dronephotoservice.views.View;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Composition root connecting the domain model, Swing view, and workflow
 * controllers.
 *
 * <p>Construction creates the specialized controllers, wires all view
 * listeners, attempts to load the bundled demo map, and applies initial control
 * state. Application shutdown is guarded so duplicate close requests start
 * only one cleanup sequence.</p>
 *
 * @author Dan Hjälmberg
 */
public class Controller {

    private final Model model;
    private final View view;

    private final SimulationController simulationController;
    private final TaskPlaybackController taskPlaybackController;
    private final SelectionController selectionController;
    private final ImageExportController imageExportController;

    private final AtomicBoolean shutdownStarted = new AtomicBoolean(false);

    /**
     * Constructs and wires the application controller graph.
     *
     * @param model application domain model.
     * @param view application Swing view.
     */
    public Controller(Model model, View view) {

        this.model = model;
        this.view = view;

        this.taskPlaybackController = new TaskPlaybackController(
                model,
                view);

        this.selectionController = new SelectionController(
                model,
                view,
                taskPlaybackController);

        ViewRefreshController viewRefreshController = new ViewRefreshController(
                model,
                view,
                selectionController);

        selectionController.setUpdateTaskThumbnails(
                viewRefreshController::updateTaskThumbnails);

        this.simulationController = new SimulationController(
                model,
                view,
                viewRefreshController::updateGUI,
                viewRefreshController::updateMapView,
                selectionController::clearSelection,
                viewRefreshController::resetEventLog);

        ControlStateController controlStateController = new ControlStateController(
                view,
                simulationController);

        this.imageExportController = new ImageExportController(
                model,
                view,
                controlStateController);

        MapLoadController mapLoadController = new MapLoadController(
                model,
                view,
                viewRefreshController::updateMapView,
                simulationController,
                controlStateController);

        CommandDispatcher commandDispatcher = new CommandDispatcher(
                view,
                simulationController,
                imageExportController,
                mapLoadController,
                controlStateController,
                this::shutdownApplication);

        view.addCommandListener(commandDispatcher);

        mapLoadController.loadDemoMap();

        controlStateController.updateControls();

        view.addApplyMapScaleListener(
                event -> applyMapScale());

        view.addSimulationTickSliderListener(event -> {
            model.setSimulationTickMs(view.getSimulationTickMs());
            commandDispatcher.setPhysicsTimerInterval();
            simulationController.refreshSimulationHeader();
        });

        view.addSimulationSpeedSliderListener(event -> {
            model.setSimulationSpeedMultiplier(view.getSimulationSpeedMultiplier());
            commandDispatcher.setPhysicsTimerInterval();
            simulationController.refreshSimulationHeader();
        });

        view.addGuiRefreshSliderListener(event ->
                commandDispatcher.setGuiRefreshInterval(view.getGuiRefreshIntervalMs()));

        view.addDroneTableSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                selectionController.selectDrone(view.getSelectedDroneName());
            }
        });

        view.addMapSelectionListener(selectionController::selectMapEntity);

        view.addMapMousePositionListener(viewRefreshController::updateMapMousePosition);

        view.addTaskThumbnailSelectionListener(selectionController::selectTask);

        view.addTaskResultPlayListener(event ->
                taskPlaybackController.playSelectedTaskResult(
                        selectionController.getSelectedTaskName()));

        view.addTaskResultStopListener(event ->
                taskPlaybackController.stopSelectedTaskResult(
                        selectionController.getSelectedTaskName()));

        view.addClearDroneSelectionAction(event ->
                selectionController.clearSelection());


        view.addApplicationCloseListener(this::shutdownApplication);
    }

    /**
     * Reads and applies the manually entered map scale.
     * Displays an error dialog if the input cannot be parsed or does not satisfy
     * the model's map-scale validation rules.
     */
    private void applyMapScale() {

        if (simulationController.getSimulationState() != SimulationState.READY) {
            return;
        }

        try {
            double metersPerPixel = view.getMapMetersPerPixelInput();
            model.setMapMetersPerPixel(metersPerPixel);
            view.displayMapMetadata(model.getMapMetadata());

        } catch (IllegalArgumentException exception) {
            view.displayErrorMessage(
                    "Invalid map scale",
                    "Meters per pixel must be a positive finite number.");
        }
    }

    /**
     * Starts the one-time asynchronous application shutdown sequence.
     *
     * <p>Playback stops first, active export is canceled, and simulation timers
     * and actors are shut down. The view is disposed only after executor
     * termination succeeds.</p>
     */
    private void shutdownApplication() {

        if (!shutdownStarted.compareAndSet(false, true)) {
            return;
        }

        // Stop EDT-driven activities first so they cannot initiate more work
        taskPlaybackController.shutdown();

        // Cancel any active export before shutting down simulation resources.
        // Files belonging to tasks completed before cancellation remain in the export directory.
        imageExportController.shutdown();

        // Stop simulation scheduling, actors and executors. Executor waiting runs
        // outside the EDT. The window is disposed after termination is confirmed.
        simulationController.shutdown(view::dispose);
    }
}
