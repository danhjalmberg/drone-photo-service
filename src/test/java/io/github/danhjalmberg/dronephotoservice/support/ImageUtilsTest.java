package io.github.danhjalmberg.dronephotoservice.support;

import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link ImageUtils} class.
 * The tests cover representative image cropping, resampling, subpixel
 * extraction, grayscale conversion, black-and-white conversion, and negative
 * conversion without attempting exhaustive pixel-level verification of every
 * interpolation and color-processing implementation.
 *
 * @author Dan Hjälmberg
 */
class ImageUtilsTest {

    /**
     * Tests cropping images to centered square regions.
     */
    @Nested
    class SquareCropTests {

        /**
         * Tests that a null source image is rejected.
         */
        @Test
        void cropImageToSquareRejectsNullImage() {

            assertThrows(NullPointerException.class,
                    () -> ImageUtils.cropImageToSquare(null));
        }

        /**
         * Tests that a landscape image is cropped to a centered square using its height.
         */
        @Test
        void cropImageToSquareCentersLandscapeImage() {
            BufferedImage source = createCoordinateImage(6, 4);

            BufferedImage result = ImageUtils.cropImageToSquare(source);

            assertEquals(4, result.getWidth());
            assertEquals(4, result.getHeight());
            assertEquals(source.getRGB(1, 0), result.getRGB(0, 0));
            assertEquals(source.getRGB(4, 3), result.getRGB(3, 3));
        }

        /**
         * Tests that a portrait image is cropped to a centered square using its width.
         */
        @Test
        void cropImageToSquareCentersPortraitImage() {
            BufferedImage source = createCoordinateImage(4, 6);

            BufferedImage result = ImageUtils.cropImageToSquare(source);

            assertEquals(4, result.getWidth());
            assertEquals(4, result.getHeight());
            assertEquals(source.getRGB(0, 1), result.getRGB(0, 0));
            assertEquals(source.getRGB(3, 4), result.getRGB(3, 3));
        }

        /**
         * Tests that a square image retains its full dimensions and pixel content.
         */
        @Test
        void cropImageToSquarePreservesSquareImage() {
            BufferedImage source = createCoordinateImage(4, 4);

            BufferedImage result = ImageUtils.cropImageToSquare(source);

            assertEquals(4, result.getWidth());
            assertEquals(4, result.getHeight());
            assertEquals(source.getRGB(0, 0), result.getRGB(0, 0));
            assertEquals(source.getRGB(3, 3), result.getRGB(3, 3));
        }
    }

    /**
     * Tests image resampling and rectangular cropping.
     */
    @Nested
    class ResamplingAndCropTests {

        /**
         * Tests that resampling returns an image with the requested dimensions and type.
         */
        @Test
        void resampleImageReturnsRequestedDimensions() {
            BufferedImage source = createCoordinateImage(8, 6);

            BufferedImage result = ImageUtils.resampleImage(
                    source,
                    3,
                    5,
                    ImageInterpolation.NEAREST_NEIGHBOR);

            assertNotNull(result);
            assertEquals(3, result.getWidth());
            assertEquals(5, result.getHeight());
            assertEquals(BufferedImage.TYPE_INT_RGB, result.getType());
        }

        /**
         * Tests that subpixel cropping returns an image with the requested dimensions.
         */
        @Test
        void cropImageSubpixelReturnsRequestedDimensions() {
            BufferedImage source = createCoordinateImage(10, 10);

            BufferedImage result = ImageUtils.cropImageSubpixel(
                    source,
                    new Vector2D(5.5, 4.5),
                    new Dimension(4, 6),
                    ImageInterpolation.BILINEAR);

            assertNotNull(result);
            assertEquals(4, result.getWidth());
            assertEquals(6, result.getHeight());
            assertEquals(BufferedImage.TYPE_INT_RGB, result.getType());
        }

        /**
         * Tests that an integer-aligned subpixel crop extracts the expected source region.
         */
        @Test
        void cropImageSubpixelExtractsExpectedRegionAtIntegerCenter() {

            BufferedImage source = createCoordinateImage(10, 10);

            BufferedImage result = ImageUtils.cropImageSubpixel(
                    source,
                    new Vector2D(5.0, 5.0),
                    new Dimension(4, 4),
                    ImageInterpolation.NEAREST_NEIGHBOR);

            assertEquals(source.getRGB(3, 3), result.getRGB(0, 0));
            assertEquals(source.getRGB(6, 6), result.getRGB(3, 3));
        }

        /**
         * Tests that a subpixel crop near an image boundary still has the requested dimensions.
         */
        @Test
        void cropImageSubpixelPreservesDimensionsNearBoundary() {

            BufferedImage source = createCoordinateImage(10, 10);

            BufferedImage result = ImageUtils.cropImageSubpixel(
                    source,
                    new Vector2D(1.0, 1.0),
                    new Dimension(4, 4),
                    ImageInterpolation.NEAREST_NEIGHBOR);

            assertEquals(4, result.getWidth());
            assertEquals(4, result.getHeight());
        }

        /**
         * Tests that a null source image is rejected.
         */
        @Test
        void cropImageSubpixelRejectsNullSource() {

            assertThrows(
                    NullPointerException.class,
                    () -> ImageUtils.cropImageSubpixel(
                            null,
                            new Vector2D(5.0, 5.0),
                            new Dimension(4, 4),
                            ImageInterpolation.BILINEAR));
        }

        /**
         * Tests that non-positive crop dimensions are rejected.
         */
        @Test
        void cropImageSubpixelRejectsNonPositiveDimensions() {

            BufferedImage source = createCoordinateImage(10, 10);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> ImageUtils.cropImageSubpixel(
                            source,
                            new Vector2D(5.0, 5.0),
                            new Dimension(0, 4),
                            ImageInterpolation.BILINEAR));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> ImageUtils.cropImageSubpixel(
                            source,
                            new Vector2D(5.0, 5.0),
                            new Dimension(4, 0),
                            ImageInterpolation.BILINEAR));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> ImageUtils.cropImageSubpixel(
                            source,
                            new Vector2D(5.0, 5.0),
                            new Dimension(-1, 4),
                            ImageInterpolation.BILINEAR));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> ImageUtils.cropImageSubpixel(
                            source,
                            new Vector2D(5.0, 5.0),
                            new Dimension(4, -1),
                            ImageInterpolation.BILINEAR));
        }
    }

    /**
     * Tests representative grayscale and black-and-white conversions.
     */
    @Nested
    class GrayscaleAndBinaryTests {

        /**
         * Tests that color-space grayscale conversion preserves image dimensions.
         */
        @Test
        void convertToGrayscaleByColorSpacePreservesDimensions() {
            BufferedImage source = createSolidImage(
                    3,
                    2,
                    new Color(100, 150, 200));

            BufferedImage result =
                    ImageUtils.convertToGrayscaleByColorSpace(source);

            assertEquals(3, result.getWidth());
            assertEquals(2, result.getHeight());
        }

        /**
         * Tests that image-type grayscale conversion produces a byte-gray image.
         */
        @Test
        void convertToGrayscaleByImageTypeReturnsByteGrayImage() {
            BufferedImage source = createSolidImage(
                    3,
                    2,
                    new Color(100, 150, 200));

            BufferedImage result =
                    ImageUtils.convertToGrayscaleByImageType(source);

            assertEquals(3, result.getWidth());
            assertEquals(2, result.getHeight());
            assertEquals(BufferedImage.TYPE_BYTE_GRAY, result.getType());
        }

        /**
         * Tests that weighted RGB grayscale conversion produces equal color channels.
         */
        @Test
        void convertToGrayscaleByWeightedRGBProducesEqualColorChannels() {
            BufferedImage source = createSolidImage(
                    1,
                    1,
                    new Color(120, 80, 40));

            BufferedImage result =
                    ImageUtils.convertToGrayscaleByWeightedRGB(source);

            Color converted = new Color(result.getRGB(0, 0));

            assertEquals(converted.getRed(), converted.getGreen());
            assertEquals(converted.getGreen(), converted.getBlue());
        }

        /**
         * Tests that black-and-white conversion produces a binary image.
         */
        @Test
        void convertToBlackAndWhiteReturnsBinaryImage() {
            BufferedImage source = createSolidImage(
                    3,
                    2,
                    new Color(100, 150, 200));

            BufferedImage result =
                    ImageUtils.convertToBlackAndWhite(source);

            assertEquals(3, result.getWidth());
            assertEquals(2, result.getHeight());
            assertEquals(BufferedImage.TYPE_BYTE_BINARY, result.getType());
        }
    }

    /**
     * Tests conversion to a photographic negative.
     */
    @Nested
    class NegativeConversionTests {

        /**
         * Tests that negative conversion inverts RGB values and preserves alpha.
         */
        @Test
        void convertToNegativeInvertsColorChannelsAndPreservesAlpha() {
            BufferedImage source = new BufferedImage(
                    1,
                    1,
                    BufferedImage.TYPE_INT_ARGB);

            Color original = new Color(10, 20, 30, 40);
            source.setRGB(0, 0, original.getRGB());

            BufferedImage result = ImageUtils.convertToNegative(source);
            Color converted = new Color(result.getRGB(0, 0), true);

            assertEquals(245, converted.getRed());
            assertEquals(235, converted.getGreen());
            assertEquals(225, converted.getBlue());
            assertEquals(40, converted.getAlpha());
        }

        /**
         * Tests that negative conversion does not modify the source image.
         */
        @Test
        void convertToNegativeLeavesSourceImageUnchanged() {
            BufferedImage source = createSolidImage(
                    1,
                    1,
                    new Color(10, 20, 30));

            int originalRgb = source.getRGB(0, 0);

            ImageUtils.convertToNegative(source);

            assertEquals(originalRgb, source.getRGB(0, 0));
        }
    }

    // ########################################################################
    // Helper methods
    // ########################################################################

    /**
     * Creates an RGB image whose pixels encode their coordinates in the red and
     * green color channels.
     *
     * @param width  image width.
     * @param height image height.
     * @return image containing coordinate-dependent colors.
     */
    private static BufferedImage createCoordinateImage(int width, int height) {
        BufferedImage image = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, new Color(x, y, 0).getRGB());
            }
        }

        return image;
    }

    /**
     * Creates an RGB image filled with one color.
     *
     * @param width  image width.
     * @param height image height.
     * @param color  fill color.
     * @return solid-color image.
     */
    private static BufferedImage createSolidImage(
            int width,
            int height,
            Color color) {

        BufferedImage image = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, color.getRGB());
            }
        }

        return image;
    }
}
