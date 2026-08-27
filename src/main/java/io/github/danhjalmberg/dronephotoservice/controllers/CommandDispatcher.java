package io.github.danhjalmberg.dronephotoservice.controllers;

import io.github.danhjalmberg.dronephotoservice.views.View;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;

/**
 * Routes Swing action commands to the controller that owns each workflow.
 *
 * <p>Lifecycle commands also synchronize enabled control state. Stop applies
 * the temporary {@link SimulationState#STOPPING} controls immediately and
 * applies final controls through the asynchronous completion callback.</p>
 *
 * @author Dan Hjälmberg
 */
public class CommandDispatcher implements ActionListener {

    private final View view;
    private final SimulationController simulationController;
    private final ImageExportController imageExportController;
    private final MapLoadController mapLoadController;
    private final ControlStateController controlStateController;
    private final Runnable shutdownApplication;

    /**
     * Creates a dispatcher for application commands.
     *
     * @param view the view to be used by the command dispatcher
     * @param simulationController the simulation controller to control the simulation state and timers
     * @param imageExportController the image export controller to control the image export
     * @param mapLoadController the map load controller to control the map loading
     * @param controlStateController the control state controller to manage enabled/disabled control state
     * @param shutdownApplication operation that performs orderly application shutdown
     */
    public CommandDispatcher(
            View view,
            SimulationController simulationController,
            ImageExportController imageExportController,
            MapLoadController mapLoadController,
            ControlStateController controlStateController,
            Runnable shutdownApplication) {

        this.view = view;
        this.simulationController = simulationController;
        this.imageExportController = imageExportController;
        this.mapLoadController = mapLoadController;
        this.controlStateController = controlStateController;
        this.shutdownApplication = Objects.requireNonNull(
                        shutdownApplication,
                        "Application shutdown action must not be null.");
    }

    /**
     * Applies a new delay to an existing GUI refresh timer.
     *
     * @param intervalMs the refresh interval in milliseconds.
     */
    public void setGuiRefreshInterval(int intervalMs) {
        simulationController.setGuiRefreshInterval(intervalMs);
    }

    /**
     * Reconfigures an existing physics scheduler from current model timing.
     */
    public void setPhysicsTimerInterval() {
        simulationController.setPhysicsTimerInterval();
    }

    /**
     * Dispatches a recognized action command; unknown commands are ignored.
     *
     * @param event Swing action event.
     */
    @Override
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        switch (command) {
            case Commands.LOAD_MAP -> mapLoadController.loadMap();
            case Commands.EXIT -> shutdownApplication.run();
            case Commands.NEW -> newSimulationCmd();
            case Commands.START -> startSimulationCmd();
            case Commands.PAUSE -> pauseSimulationCmd();
            case Commands.RESUME -> resumeSimulationCmd();
            case Commands.STOP -> stopSimulationCmd();
            case Commands.SAVE_IMAGES -> imageExportController.saveImages();
            case Commands.HELP -> view.displayHelpDialog();
            case Commands.ABOUT -> view.displayAboutDialog();
            default -> { }
        }
    }

    /**
     * Prepares a new run and refreshes lifecycle controls.
     */
    private void newSimulationCmd() {
        simulationController.newSimulation();
        controlStateController.updateControls();
    }

    /**
     * Starts a prepared run with control updates for success or rollback.
     */
    private void startSimulationCmd() {

        simulationController.startSimulation(controlStateController::updateControls);
    }

    /**
     * Pauses the simulation and refreshes lifecycle controls.
     */
    private void pauseSimulationCmd() {
        simulationController.pauseSimulation();
        controlStateController.updateControls();
    }

    /**
     * Resumes the simulation and refreshes lifecycle controls.
     */
    private void resumeSimulationCmd() {
        simulationController.resumeSimulation();
        controlStateController.updateControls();
    }

    /**
     * Begins asynchronous simulation shutdown and refreshes lifecycle controls.
     */
    private void stopSimulationCmd() {

        simulationController.stopSimulation(controlStateController::updateControls);

        // Apply the STOPPING controls immediately. The completion callback applies
        // the STOPPED controls after executor termination has been confirmed.
        controlStateController.updateControls();
    }
}
