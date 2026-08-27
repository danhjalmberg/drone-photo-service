package io.github.danhjalmberg.dronephotoservice.models.components;

/**
 * Represents a drone battery whose capacity and charge are measured as
 * available operating time.
 *
 * <p>A battery is initially fully charged. Charging is capped at its capacity,
 * and consumption is bounded at zero, so the current charge always remains
 * within the range from zero to the configured capacity.</p>
 *
 * @author Dan Hjälmberg
 */
public interface Battery {

    /**
     * Returns the battery type name.
     *
     * @return battery type name
     */
    String getType();

    /**
     * Returns the maximum operating time provided by a full charge.
     *
     * @return capacity in simulation seconds
     */
    double getCapacitySeconds();

    /**
     * Returns the operating time available from the current charge.
     *
     * @return current charge in simulation seconds
     */
    double getCurrentChargeSeconds();

    /**
     * Increases the current charge by an amount of operating time without
     * exceeding the battery capacity.
     *
     * @param deltaSeconds non-negative operating time to add, in simulation
     *                     seconds
     * @throws IllegalArgumentException if {@code deltaSeconds} is negative
     */
    void charge(double deltaSeconds);

    /**
     * Decreases the current charge by an amount of operating time without
     * reducing it below zero.
     *
     * @param deltaSeconds non-negative operating time to consume, in
     *                     simulation seconds
     * @throws IllegalArgumentException if {@code deltaSeconds} is negative
     */
    void consume(double deltaSeconds);

    /**
     * Returns formatted battery type, charge, and capacity information.
     *
     * @return formatted battery information
     */
    @Override
    String toString();
}
