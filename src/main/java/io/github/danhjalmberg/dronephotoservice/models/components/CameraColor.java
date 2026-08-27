package io.github.danhjalmberg.dronephotoservice.models.components;

import java.awt.image.BufferedImage;

/**
 * Provides a color camera that preserves captured images without applying a
 * color conversion.
 *
 * @author Dan Hjälmberg
 */
public class CameraColor implements Camera {

    private final String type = "Color";

    /**
     * Creates a color camera.
     */
    public CameraColor() {
    }

    /**
     * {@inheritDoc}
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the source image without modification.
     *
     * @param image source image
     * @return the same image instance supplied in {@code image}
     */
    @Override
    public BufferedImage applyFilter(BufferedImage image) {

        return image;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "Camera type: Color Photo\n";
    }
}
