package io.github.danhjalmberg.dronephotoservice.controllers;

import io.github.danhjalmberg.dronephotoservice.views.View;

/**
 * Derives enabled Swing controls from simulation lifecycle and export state.
 *
 * <p>An active export temporarily disables map and simulation setup controls
 * regardless of lifecycle. Otherwise each {@link SimulationState} maps to one
 * complete control configuration.</p>
 *
 * @author Dan Hjälmberg
 */
public class ControlStateController {

    private final View view;
    private final SimulationController simulationController;

    private boolean saving = false;

    /**
     * Creates a control-state coordinator.
     *
     * @param view the view to be controlled
     * @param simulationController the simulation controller
     */
    public ControlStateController(
            View view,
            SimulationController simulationController) {

        this.view = view;
        this.simulationController = simulationController;
    }

    /**
     * Reports whether an image-export workflow may start.
     *
     * <p>Export is allowed only after simulation shutdown has completed and no
     * other export is active. Archive emptiness is handled by the export
     * controller after this check.</p>
     *
     * @return {@code true} if export may start.
     */
    public boolean canSaveImages() {

        return !saving
                && simulationController.getSimulationState() == SimulationState.STOPPED;
    }

    /**
     * Records export activity and immediately reapplies all controls.
     *
     * @param saving whether an export is active.
     */
    public void setSaving(boolean saving) {
        this.saving = saving;
        updateControls();
    }

    /**
     * Updates all application controls according to the current simulation
     * lifecycle state and any temporary image-saving restriction.
     *
     * <p>New is available only after a simulation has stopped. Invoking it
     * discards the completed run and returns the application to the ready state.
     * Start is available only in the ready state.</p>
     *
     * @throws IllegalStateException if the simulation state is unrecognized.
     */
    public void updateControls() {

        if (saving) {
            view.setSavingControls(true);

            view.setMapLoadControlsEnabled(false);
            view.setMapScaleControlsEnabled(false);
            view.setSimulationSetupControlsEnabled(false);
            return;
        }

         // Remove temporary saving restrictions. The state-specific
         // calls below then establish the correct enabled state.
        view.setSavingControls(false);

        SimulationState state =
                simulationController.getSimulationState();

        switch (state) {

            case NO_MAP_LOADED -> {
                view.setSimulationControls(
                        false,  // New
                        false,  // Start
                        false,  // Pause
                        false,  // Resume
                        false,  // Stop
                        false); // Save images

                view.setMapLoadControlsEnabled(true);
                view.setMapScaleControlsEnabled(false);
                view.setSimulationSetupControlsEnabled(false);
            }

            case READY -> {
                view.setSimulationControls(
                        false,   // New
                        true,   // Start
                        false,  // Pause
                        false,  // Resume
                        false,  // Stop
                        false); // Save images

                view.setMapLoadControlsEnabled(true);
                view.setMapScaleControlsEnabled(true);
                view.setSimulationSetupControlsEnabled(true);
            }

            case RUNNING -> {
                view.setSimulationControls(
                        false,  // New
                        false,  // Start
                        true,   // Pause
                        false,  // Resume
                        true,   // Stop
                        false); // Save images

                view.setMapLoadControlsEnabled(false);
                view.setMapScaleControlsEnabled(false);
                view.setSimulationSetupControlsEnabled(false);
            }

            case PAUSED -> {
                view.setSimulationControls(
                        false,  // New
                        false,  // Start
                        false,  // Pause
                        true,   // Resume
                        true,   // Stop
                        false); // Save images

                view.setMapLoadControlsEnabled(false);
                view.setMapScaleControlsEnabled(false);
                view.setSimulationSetupControlsEnabled(false);
            }

            case STOPPING -> {
                view.setSimulationControls(
                        false,  // New
                        false,  // Start
                        false,  // Pause
                        false,  // Resume
                        false,  // Stop
                        false); // Save images

                view.setMapLoadControlsEnabled(false);
                view.setMapScaleControlsEnabled(false);
                view.setSimulationSetupControlsEnabled(false);
            }

            case STOPPED -> {
                view.setSimulationControls(
                        true,  // New
                        false,  // Start
                        false,  // Pause
                        false,  // Resume
                        false,  // Stop
                        true);  // Save images

                // Preserve the current map and completed results until the user
                // explicitly creates a new simulation.
                view.setMapLoadControlsEnabled(false);
                view.setMapScaleControlsEnabled(false);
                view.setSimulationSetupControlsEnabled(false);
            }

            default -> throw new IllegalStateException(
                    "Unhandled simulation state: " + state);
        }
    }
}
