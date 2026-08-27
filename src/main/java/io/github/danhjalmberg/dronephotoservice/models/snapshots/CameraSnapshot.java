package io.github.danhjalmberg.dronephotoservice.models.snapshots;

/**
 * Immutable camera state captured for presentation outside the simulation
 * model.
 *
 * <p>The presentation layer requires only the installed camera type. Image
 * filtering behavior remains encapsulated by the live camera component owned
 * by the drone.</p>
 *
 * @author Dan Hjälmberg
 */
public final class CameraSnapshot {

    private final String type;

    /**
     * Creates an immutable camera summary.
     *
     * @param type camera type name.
     */
    public CameraSnapshot(String type) {
        this.type = type;
    }

    /**
     * Returns the captured camera type name.
     *
     * @return camera type name.
     */
    public String getType() {
        return type;
    }
}
