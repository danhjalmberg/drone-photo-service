package io.github.danhjalmberg.dronephotoservice.models.components;

import io.github.danhjalmberg.dronephotoservice.settings.ModelSettings;

/**
 * Provides the high-power motor configuration with the highest maximum
 * horizontal speed.
 *
 * @author Dan Hjälmberg
 */
public class MotorHighPower implements Motor {

    private final String type = "High Power";
    private final double maxSpeedMetersPerSecond = ModelSettings.DRONE_HIGH_SPEED_METERS_PER_SECOND;

    private double currentSpeedMetersPerSecond;

    /**
     * Creates a stationary high-power motor.
     */
    public MotorHighPower() {
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
        return "Motor type: High Power\n"
                + "Current speed: "
                + String.format("%.1f", currentSpeedMetersPerSecond)
                + " of max "
                + String.format("%.1f", maxSpeedMetersPerSecond)
                + " m/s\n";
    }
}
