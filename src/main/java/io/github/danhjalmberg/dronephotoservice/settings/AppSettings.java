package io.github.danhjalmberg.dronephotoservice.settings;

/**
 * Application-wide settings that are not owned specifically by the model,
 * view, or controller layers.
 */
public final class AppSettings {

    /**
     * The stylized application name used by the main window header.
     */
    public static final String APPLICATION_NAME = "D R O N E   P H O T O   S E R V I C E";

    /**
     * The ordinary application name used by dialogs and metadata.
     */
    public static final String APPLICATION_DISPLAY_NAME = "Drone Photo Service";

    /**
     * The current application version.
     */
    public static final String APPLICATION_VERSION = "0.1.0-SNAPSHOT";

    /**
     * Project source-code and documentation URL.
     */
    public static final String PROJECT_URL = "https://github.com/danhjalmberg/drone-photo-service";

    /**
     * Resource path for the bundled demo map image.
     */
    public static final String DEMO_MAP_RESOURCE_PATH = "/maps/demo_map.jpg";

    /**
     * File name of the bundled demo map image.
     */
    public static final String DEMO_MAP_FILE_NAME = "demo_map.jpg";

    /**
     * Prevents instantiation of this constants class.
     */
    private AppSettings() {
    }
}
