package io.github.danhjalmberg.dronephotoservice.models.components;

/**
 * Defines horizontal drone motor speed behavior.
 *
 * <p>Motor speeds are expressed in meters per second. Conversion from meters
 * to map coordinates is handled by the map subsystem using the active map
 * scale.</p>
 *
 * @author Dan Hjälmberg
 */
public interface Motor {

    /**
     * Returns the motor type name.
     *
     * @return motor type name
     */
    String getType();

    /**
     * Returns the maximum horizontal speed associated with this motor type.
     *
     * @return maximum speed in meters per second
     */
    double getMaxSpeed();

    /**
     * Returns the current horizontal speed.
     *
     * @return current speed in meters per second
     */
    double getCurrentSpeed();

    /**
     * Sets the current horizontal speed, clamping negative values to zero.
     *
     * @param speedMetersPerSecond requested speed in meters per second
     */
    void setSpeed(double speedMetersPerSecond);

    /**
     * Returns formatted motor information.
     *
     * @return formatted motor information
     */
    @Override
    String toString();
}
