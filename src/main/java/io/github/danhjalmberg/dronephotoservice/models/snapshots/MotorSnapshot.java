package io.github.danhjalmberg.dronephotoservice.models.snapshots;

/**
 * Immutable motor state captured for presentation outside the simulation
 * model.
 *
 * <p>The type and speed values describe the motor when the containing drone
 * snapshot was created. This object does not retain or expose the live motor
 * component.</p>
 *
 * @author Dan Hjälmberg
 */
public final class MotorSnapshot {

    private final String type;
    private final double maxSpeed;
    private final double currentSpeed;

    /**
     * Creates an immutable motor summary.
     *
     * @param type         motor type name.
     * @param maxSpeed     maximum horizontal speed in meters per second.
     * @param currentSpeed current horizontal speed in meters per second.
     */
    public MotorSnapshot(
            String type,
            double maxSpeed,
            double currentSpeed) {

        this.type = type;
        this.maxSpeed = maxSpeed;
        this.currentSpeed = currentSpeed;
    }

    /**
     * Returns the captured motor type name.
     *
     * @return motor type name.
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the captured maximum horizontal speed.
     *
     * @return maximum speed in meters per second.
     */
    public double getMaxSpeed() {
        return maxSpeed;
    }

    /**
     * Returns the captured current horizontal speed.
     *
     * @return current speed in meters per second.
     */
    public double getCurrentSpeed() {
        return currentSpeed;
    }
}
