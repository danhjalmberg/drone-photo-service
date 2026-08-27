package io.github.danhjalmberg.dronephotoservice.models.components;

import io.github.danhjalmberg.dronephotoservice.support.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * Provides a grayscale camera that converts captured colors to luminance
 * values.
 *
 * @author Dan Hjälmberg
 */
public class CameraGrayscale implements Camera {

    private final String type = "Grayscale";

    /**
     * Creates a grayscale camera.
     */
    public CameraGrayscale() {
    }

    /**
     * {@inheritDoc}
     */
    public String getType() {
        return type;
    }

    /**
     * Converts the source image to grayscale using weighted RGB luminance.
     *
     * @param image source image
     * @return new grayscale image
     * @throws NullPointerException if {@code image} is {@code null}
     */
    @Override
    public BufferedImage applyFilter(BufferedImage image) {

        return ImageUtils.convertToGrayscaleByWeightedRGB(image);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "Camera type: Grayscale Photo\n";
    }
}
