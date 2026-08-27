package io.github.danhjalmberg.dronephotoservice.support;

import java.awt.RenderingHints;

/**
 * Defines the interpolation algorithms available for image resampling.
 *
 * <p>Each value maps an application-level interpolation choice to the
 * corresponding Java 2D rendering hint used by image-processing operations.</p>
 */
public enum ImageInterpolation {

    /**
     * Chooses the nearest source pixel without blending neighboring pixels.
     */
    NEAREST_NEIGHBOR(RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR),
    /**
     * Blends the nearest two-by-two source neighborhood.
     */
    BILINEAR(RenderingHints.VALUE_INTERPOLATION_BILINEAR),
    /**
     * Uses bicubic interpolation for smoother, more expensive resampling.
     */
    BICUBIC(RenderingHints.VALUE_INTERPOLATION_BICUBIC);

    private final Object renderingHint;

    /**
     *  Creates a new interpolation setting with the specified Java 2D rendering hint.
     *
     * @param renderingHint Java 2D value for {@link RenderingHints#KEY_INTERPOLATION}
     */
    ImageInterpolation(Object renderingHint) {
        this.renderingHint = renderingHint;
    }

    /**
     * Returns the Java 2D rendering hint represented by this interpolation
     * setting.
     *
     * @return Java 2D value for {@link RenderingHints#KEY_INTERPOLATION}
     */
    public Object getRenderingHint() {
        return renderingHint;
    }
}
