package io.github.danhjalmberg.dronephotoservice.models.snapshots;

/**
 * Immutable battery state captured for presentation outside the simulation
 * model.
 *
 * <p>The type and charge values describe the battery when the containing drone
 * snapshot was created. This object does not retain or expose the live battery
 * component.</p>
 *
 * @author Dan Hjälmberg
 */
public final class BatterySnapshot {

    private final String type;
    private final double capacitySeconds;
    private final double currentChargeSeconds;

    /**
     * Creates an immutable battery summary.
     *
     * @param type                 battery type name.
     * @param capacitySeconds      full-charge capacity in simulation seconds.
     * @param currentChargeSeconds available charge in simulation seconds.
     */
    public BatterySnapshot(
            String type,
            double capacitySeconds,
            double currentChargeSeconds) {

        this.type = type;
        this.capacitySeconds = capacitySeconds;
        this.currentChargeSeconds = currentChargeSeconds;
    }

    /**
     * Returns the captured battery type name.
     *
     * @return battery type name.
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the captured full-charge capacity.
     *
     * @return capacity in simulation seconds.
     */
    public double getCapacitySeconds() {
        return capacitySeconds;
    }

    /**
     * Returns the captured available charge.
     *
     * @return available charge in simulation seconds.
     */
    public double getCurrentChargeSeconds() {
        return currentChargeSeconds;
    }
}
