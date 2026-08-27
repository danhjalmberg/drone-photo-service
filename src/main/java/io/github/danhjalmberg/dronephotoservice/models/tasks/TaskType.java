package io.github.danhjalmberg.dronephotoservice.models.tasks;

/**
 * Identifies the capture-work types supported by the application.
 */
public enum TaskType {

    PHOTO("Photo", "PhotoTask"),
    VIDEO("Video", "VideoTask"),
    ZOOM("Zoom", "ZoomTask");

    private final String displayName;
    private final String serializedValue;

    /**
     * Creates a task type.
     *
     * @param displayName     human-readable name used by the GUI
     * @param serializedValue stable value used in external output
     */
    TaskType(String displayName, String serializedValue) {
        this.displayName = displayName;
        this.serializedValue = serializedValue;
    }

    /**
     * Returns the human-readable name used by the GUI.
     *
     * @return human-readable name used by the GUI
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the stable value used in external output.
     *
     * @return stable value used in external output
     */
    public String getSerializedValue() {
        return serializedValue;
    }
}
