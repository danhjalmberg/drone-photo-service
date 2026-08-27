package io.github.danhjalmberg.dronephotoservice.models.components;

import io.github.danhjalmberg.dronephotoservice.support.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * Provides a negative camera that inverts captured RGB color values while
 * preserving transparency.
 *
 * @author Dan Hjälmberg
 */
public class CameraNegative implements Camera {

    private final String type = "Negative";

    /**
     * Creates a negative camera.
     */
    public CameraNegative() {
    }

    /**
     * {@inheritDoc}
     */
    public String getType() {
        return type;
    }

    /**
     * Converts the source image to a color negative.
     *
     * @param image source image
     * @return new image with inverted RGB values and preserved alpha values
     * @throws NullPointerException if {@code image} is {@code null}
     */
    @Override
    public BufferedImage applyFilter(BufferedImage image) {

        return ImageUtils.convertToNegative(image);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "Camera type: Negative Photo\n";
    }
}
