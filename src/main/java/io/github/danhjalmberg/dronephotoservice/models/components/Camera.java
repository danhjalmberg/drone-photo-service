package io.github.danhjalmberg.dronephotoservice.models.components;

import java.awt.image.BufferedImage;

/**
 * Defines the image-processing behavior of a drone camera.
 *
 * <p>Camera variants determine whether captured images retain their original
 * colors or are converted to grayscale or negative color values.</p>
 *
 * @author Dan Hjälmberg
 */
public interface Camera {

    /**
     * Returns the camera type name.
     *
     * @return camera type name
     */
    String getType();

    /**
     * Applies this camera's image-processing behavior to a captured image.
     *
     * @param image source image
     * @return processed image; may be the source image when no conversion is
     *         required
     */
    BufferedImage applyFilter(BufferedImage image);

    /**
     * Returns formatted camera information.
     *
     * @return formatted camera information
     */
    @Override
    String toString();
}
