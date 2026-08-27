package io.github.danhjalmberg.dronephotoservice.views.viewdata;

import io.github.danhjalmberg.dronephotoservice.controllers.SimulationState;

import java.time.Duration;

/**
 * Supplies the values rendered by the compact simulation header.
 *
 * <p>The controller prepares lifecycle and model values; the north-panel view
 * remains responsible for formatting state labels, duration, speed, and the map
 * path for display.</p>
 *
 * @author Dan Hjälmberg
 */
public class SimulationHeaderViewData {

    private final String applicationName;
    private final SimulationState simulationState;
    private final Duration simulationTime;
    private final double speedMultiplier;
    private final String mapFilePath;

    /**
     * Creates a simulation-header data value.
     *
     * @param applicationName the name of the application
     * @param simulationState the current state of the simulation
     * @param simulationTime the elapsed time of the simulation
     * @param speedMultiplier the speed multiplier of the simulation
     * @param mapFilePath loaded map path, or {@code null} when no map is loaded
     */
    public SimulationHeaderViewData(
            String applicationName,
            SimulationState simulationState,
            Duration simulationTime,
            double speedMultiplier,
            String mapFilePath) {

        this.applicationName = applicationName;
        this.simulationState = simulationState;
        this.simulationTime = simulationTime;
        this.speedMultiplier = speedMultiplier;
        this.mapFilePath = mapFilePath;
    }

    /**
     * Returns the application name to display in the simulation header.
     *
     * @return name of the application
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Returns the lifecycle state to display in the simulation header.
     *
     * @return current state of the simulation
     */
    public SimulationState getSimulationState() {
        return simulationState;
    }

    /**
     * Returns the elapsed simulation time to display.
     *
     * @return elapsed time of the simulation
     */
    public Duration getSimulationTime() {
        return simulationTime;
    }

    /**
     * Returns the simulation-speed multiplier to display.
     *
     * @return speed multiplier of the simulation
     */
    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    /**
     * Returns the path of the map represented by this header data.
     *
     * @return loaded map path, or {@code null} when no map is loaded
     */
    public String getMapFilePath() {

        return mapFilePath;
    }
}
