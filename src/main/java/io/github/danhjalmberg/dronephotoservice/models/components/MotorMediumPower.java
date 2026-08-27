package io.github.danhjalmberg.dronephotoservice.models.components;

import io.github.danhjalmberg.dronephotoservice.settings.ModelSettings;

/**
 * Provides the medium-power motor configuration with an intermediate maximum
 * horizontal speed.
 *
 * @author Dan Hjälmberg
 */
public class MotorMediumPower implements Motor {

    private final String type = "Medium Power";
    private final double maxSpeedMetersPerSecond = ModelSettings.DRONE_MEDIUM_SPEED_METERS_PER_SECOND;

    private double currentSpeedMetersPerSecond;

    /**
     * Creates a stationary medium-power motor.
     */
    public MotorMediumPower() {
        this.currentSpeedMetersPerSecond = 0.0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getMaxSpeed() {
        return maxSpeedMetersPerSecond;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCurrentSpeed() {
        return currentSpeedMetersPerSecond;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSpeed(double speedMetersPerSecond) {
        this.currentSpeedMetersPerSecond = Math.max(0.0, speedMetersPerSecond);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Motor type: Medium Power\n"
                + "Current speed: "
                + String.format("%.1f", currentSpeedMetersPerSecond)
                + " of max "
                + String.format("%.1f", maxSpeedMetersPerSecond)
                + " m/s\n";
    }
}
