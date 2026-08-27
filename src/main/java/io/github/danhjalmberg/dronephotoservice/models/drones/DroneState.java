package io.github.danhjalmberg.dronephotoservice.models.drones;

/**
 * Describes the current operational phase of a drone.
 */
public enum DroneState {

    /**
     * Waiting fully charged at base without assigned work.
     */
    IDLE("Idle"),
    /**
     * Replenishing battery charge at base.
     */
    CHARGING("Charging"),
    /**
     * Traveling toward the assigned task target.
     */
    MOVING_TO_TASK("Moving to task"),
    /**
     * Stationary at the target while capture work completes.
     */
    PROCESSING_TASK("Processing task"),
    /**
     * Traveling toward the drone's base position.
     */
    RETURNING_TO_BASE("Returning to base");

    private final String displayName;

    DroneState(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable state name used by the GUI.
     *
     * @return display name.
     */
    public String getDisplayName() {
        return displayName;
    }
}
