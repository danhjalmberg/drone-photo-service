package io.github.danhjalmberg.dronephotoservice.support;

import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;
import java.util.Objects;

/**
 * Provides deterministic {@link BufferedImage} cropping, resampling, and color
 * conversion operations.
 *
 * <p>Unless a method explicitly returns a subimage view, operations allocate a
 * new result and leave the source unchanged. Resampling and subpixel cropping
 * render into {@link BufferedImage#TYPE_INT_RGB} and therefore discard source
 * transparency.</p>
 *
 * @author Dan Hjälmberg
 */
public final class ImageUtils {

    /**
     * Prevents instantiation of this utility class.
     */
    private ImageUtils() {
    }

    /**
     * Returns the largest centered square region as a shared subimage view.
     * Changes to the returned raster may therefore affect the source image.
     *
     * @param image source image
     * @return centered square view backed by the source raster
     * @throws NullPointerException if the image is null
     */
    public static BufferedImage cropImageToSquare(BufferedImage image) {

        Objects.requireNonNull(image, "Image must not be null.");

        int width = image.getWidth();
        int height = image.getHeight();

        int side = Math.min(width, height);
        int x = (width - side) / 2;
        int y = (height - side) / 2;

        return image.getSubimage(
                x,
                y,
                side,
                side);
    }

    /**
     * Resamples an image into a new opaque RGB image of the requested dimensions.
     *
     * @param image         the input image to resample
     * @param width         the target width
     * @param height        the target height
     * @param interpolation the interpolation method to use for resampling
     * @return newly allocated RGB image
     * @throws NullPointerException     if {@code interpolation} is {@code null}
     * @throws IllegalArgumentException if either target dimension is not positive
     */
    public static BufferedImage resampleImage(
            BufferedImage image,
            int width,
            int height,
            ImageInterpolation interpolation) {

        BufferedImage resampledImage = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = resampledImage.createGraphics();

        g2d.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                interpolation.getRenderingHint());

        g2d.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        g2d.drawImage(image, 0, 0, width, height, null);
        g2d.dispose();

        return resampledImage;
    }

    /**
     * Extracts a fixed-size region centered at a possibly fractional source
     * coordinate.
     *
     * <p>The source is translated onto a new RGB canvas using the requested
     * interpolation. Portions outside the source bounds remain black rather than
     * shrinking the result.</p>
     *
     * @param source        source image
     * @param center        crop center in source-image pixels
     * @param dimension     output width and height in pixels
     * @param interpolation interpolation used for fractional sampling
     * @return newly allocated fixed-size RGB crop
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if either crop dimension is not positive
     */
    public static BufferedImage cropImageSubpixel(
            BufferedImage source,
            Vector2D center,
            Dimension dimension,
            ImageInterpolation interpolation) {

        Objects.requireNonNull(source, "Source image must not be null.");
        Objects.requireNonNull(center, "Crop center must not be null.");
        Objects.requireNonNull(dimension, "Crop dimensions must not be null.");
        Objects.requireNonNull(interpolation, "Interpolation must not be null.");

        if (dimension.width <= 0 || dimension.height <= 0) {
            throw new IllegalArgumentException(
                    "Crop dimensions must be greater than zero.");
        }

        BufferedImage output = new BufferedImage(
                dimension.width,
                dimension.height,
                BufferedImage.TYPE_INT_RGB);

        double sourceX = center.getX() - dimension.width * 0.5;
        double sourceY = center.getY() - dimension.height * 0.5;

        Graphics2D g2d = output.createGraphics();

        try {
            g2d.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    interpolation.getRenderingHint());

            g2d.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);

            g2d.drawImage(
                    source,
                    AffineTransform.getTranslateInstance(-sourceX, -sourceY),
                    null);

        } finally {
            g2d.dispose();
        }

        return output;
    }

    /**
     * Converts an image through Java's gray color space using
     * {@link ColorConvertOp}.
     *
     * @param imageIn the input color image
     * @return the grayscale image
     */
    public static BufferedImage convertToGrayscaleByColorSpace(
            BufferedImage imageIn) {

        BufferedImageOp bufferedImageOp = new ColorConvertOp(
                ColorSpace.getInstance(ColorSpace.CS_GRAY),
                null);

        return bufferedImageOp.filter(imageIn, null);
    }

    /**
     * Renders an image into a newly allocated {@code TYPE_BYTE_GRAY} buffer.
     *
     * @param imageIn the input color image
     * @return the grayscale image
     */
    public static BufferedImage convertToGrayscaleByImageType(
            BufferedImage imageIn) {

        BufferedImage imageOut = new BufferedImage(
                imageIn.getWidth(),
                imageIn.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);

        Graphics2D g2d = imageOut.createGraphics();
        g2d.drawImage(imageIn, 0, 0, null);
        g2d.dispose();

        return imageOut;
    }

    /**
     * Produces grayscale RGB channels using gamma-corrected relative luminance.
     * The result has the same dimensions and buffered-image type as the source.
     *
     * @param imageIn the input color image
     * @return the grayscale image
     * @see <a href="https://stackoverflow.com/questions/9131678/convert-a-rgb-image-to-grayscale-image-reducing-the-memory-in-java">RGB to grayscale</a>
     */
    public static BufferedImage convertToGrayscaleByWeightedRGB(
            BufferedImage imageIn) {

        BufferedImage imageOut = new BufferedImage(
                imageIn.getWidth(),
                imageIn.getHeight(),
                imageIn.getType());

        Graphics2D g2d = imageOut.createGraphics();
        g2d.drawImage(imageIn, 0, 0, null);
        g2d.dispose();

        for (int x = 0; x < imageOut.getWidth(); ++x) {
            for (int y = 0; y < imageOut.getHeight(); ++y) {

                int rgb = imageOut.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                double rr = Math.pow(r / 255.0, 2.2);
                double gg = Math.pow(g / 255.0, 2.2);
                double bb = Math.pow(b / 255.0, 2.2);

                double lum = 0.2126 * rr + 0.7152 * gg + 0.0722 * bb;

                int grayLevel = (int) (255.0 * Math.pow(lum, 1.0 / 2.2));
                int gray = (grayLevel << 16) + (grayLevel << 8) + grayLevel;

                imageOut.setRGB(x, y, gray);
            }
        }

        return imageOut;
    }

    /**
     * Renders an image into a newly allocated {@code TYPE_BYTE_BINARY} buffer.
     *
     * @param imageIn the input color image
     * @return the black and white image
     */
    public static BufferedImage convertToBlackAndWhite(BufferedImage imageIn) {

        BufferedImage imageOut = new BufferedImage(
                imageIn.getWidth(),
                imageIn.getHeight(),
                BufferedImage.TYPE_BYTE_BINARY);

        Graphics2D g2d = imageOut.createGraphics();
        g2d.drawImage(imageIn, 0, 0, null);
        g2d.dispose();

        return imageOut;
    }

    /**
     * Inverts RGB channels in a new image while preserving each pixel's alpha.
     * The result uses the source image's buffered-image type.
     *
     * @param imageIn the input color image
     * @return the image with inverted colors
     * @see <a href="https://stackoverflow.com/questions/21899824/java-convert-a-greyscale-and-sepia-version-of-an-image-with-bufferedimage">
     * Converting an image with {@code BufferedImage}</a>
     */
    public static BufferedImage convertToNegative(BufferedImage imageIn) {

        BufferedImage imageOut = new BufferedImage(
                imageIn.getWidth(),
                imageIn.getHeight(),
                imageIn.getType());

        Graphics2D g2d = imageOut.createGraphics();
        g2d.drawImage(imageIn, 0, 0, null);
        g2d.dispose();

        for (int x = 0; x < imageIn.getWidth(); x++) {
            for (int y = 0; y < imageIn.getHeight(); y++) {

                int rgb = imageIn.getRGB(x, y);
                Color color = new Color(rgb, true);

                int r = 255 - color.getRed();
                int g = 255 - color.getGreen();
                int b = 255 - color.getBlue();

                color = new Color(r, g, b, color.getAlpha());
                imageOut.setRGB(x, y, color.getRGB());
            }
        }

        return imageOut;
    }
}
