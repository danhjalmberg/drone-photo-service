package io.github.danhjalmberg.dronephotoservice.models.components;

import io.github.danhjalmberg.dronephotoservice.settings.ModelSettings;

/**
 * Provides the low-capacity battery configuration.
 *
 * @author Dan Hjälmberg
 */
public class BatteryLowCapacity implements Battery {

    private final String type = "Low Capacity";
    private final double capacity = ModelSettings.BATTERY_LOW_CAPACITY_OPERATION_SECONDS;
    private double currentCharge;

    /**
     * Creates a fully charged low-capacity battery.
     */
    public BatteryLowCapacity() {
        this.currentCharge = capacity;
    }

    /**
     * {@inheritDoc}
     */
    public String getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    public double getCapacitySeconds() {
        return capacity;
    }

    /**
     * {@inheritDoc}
     */
    public double getCurrentChargeSeconds() {
        return currentCharge;
    }

    /**
     * {@inheritDoc}
     */
    public void charge(double deltaSeconds) {
        if (deltaSeconds < 0.0) {
            throw new IllegalArgumentException(
                    "deltaSeconds must not be negative.");
        }

        currentCharge = Math.min(capacity, currentCharge + deltaSeconds);
    }

    /**
     * {@inheritDoc}
     */
    public void consume(double deltaSeconds) {
        if (deltaSeconds < 0.0) {
            throw new IllegalArgumentException(
                    "deltaSeconds must not be negative.");
        }

        currentCharge = Math.max(0.0, currentCharge - deltaSeconds);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "Battery type: Low Capacity\n"
                + "Battery level: "
                + String.format("%.0f", currentCharge)
                + " of "
                + String.format("%.0f", capacity)
                + "\n";
    }
}
