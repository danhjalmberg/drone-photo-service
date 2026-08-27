package io.github.danhjalmberg.dronephotoservice.models.drones;

/**
 * Identifies the drone configurations supported by the application.
 */
public enum DroneType {

    TYPE_1("Type 1", "type1"),
    TYPE_2("Type 2", "type2"),
    TYPE_3("Type 3", "type3");

    private final String displayName;
    private final String serializedValue;

    /**
     * Creates a drone-type identifier with explicit external representations.
     *
     * @param displayName human-readable name used for presentation
     * @param serializedValue stable identifier used for external output
     */
    DroneType(String displayName, String serializedValue) {
        this.displayName = displayName;
        this.serializedValue = serializedValue;
    }

    /**
     * Returns the human-readable drone-type name.
     *
     * @return display name used for presentation
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the stable external drone-type identifier.
     *
     * @return serialized identifier used for configuration or external output
     */
    public String getSerializedValue() {
        return serializedValue;
    }
}
