package io.github.danhjalmberg.dronephotoservice.models.map;

/**
 * Indicates that a map image or its associated metadata could not be loaded.
 *
 * @author Dan Hjälmberg
 */
public class MapLoadException extends Exception {

    /**
     * Creates an exception with a description of the map-loading failure.
     *
     * @param message failure description
     */
    public MapLoadException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a failure description and underlying cause.
     *
     * @param message failure description
     * @param cause underlying cause
     */
    public MapLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
