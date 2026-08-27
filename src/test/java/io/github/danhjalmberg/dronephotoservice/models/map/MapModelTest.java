package io.github.danhjalmberg.dronephotoservice.models.map;

import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;
import io.github.danhjalmberg.dronephotoservice.settings.ModelSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link MapModel} class.
 * The tests cover representative map initialization, image state, metadata
 * delegation, unit conversion, coordinate conversion, random safe positions,
 * and validation without attempting exhaustive coverage of image loading and
 * processing error conditions.
 *
 * @author Dan Hjälmberg
 */
class MapModelTest {

    /**
     * Tolerance used when comparing floating-point results.
     */
    private static final double DELTA = 1.0e-9;

    /**
     * Width of the representative source image used by the tests.
     */
    private static final int SOURCE_WIDTH = 1000;

    /**
     * Height of the representative source image used by the tests.
     * The source image is cropped to a centered square with this side length.
     */
    private static final int SOURCE_HEIGHT = 800;

    /**
     * Name assigned to the representative map.
     */
    private static final String MAP_FILE_NAME = "test_map.png";

    /**
     * Map model under test.
     */
    private MapModel mapModel;

    /**
     * Temporary directory supplied by JUnit for file-based map loading tests.
     */
    @TempDir
    private Path tempDirectory;

    /**
     * Creates and initializes a map model before each test.
     *
     * @throws IOException if the in-memory PNG image cannot be created
     * @throws MapLoadException if the representative test image cannot be loaded
     */
    @BeforeEach
    void setUp() throws IOException, MapLoadException {

        mapModel = createLoadedMapModel(
                SOURCE_WIDTH,
                SOURCE_HEIGHT);
    }

    /**
     * Tests map loading and stored image state.
     */
    @Nested
    class LoadingAndImageStateTests {

        /**
         * Tests that loading a valid image initializes all processed image state.
         *
         * @throws IOException if the in-memory PNG image cannot be created
         * @throws MapLoadException if the test image cannot be loaded
         */
        @Test
        void loadMapInitializesOriginalCroppedAndDisplayImages()
                throws IOException, MapLoadException {

            MapModel model = new MapModel();

            model.loadMap(
                    createImageInputStream(
                            SOURCE_WIDTH,
                            SOURCE_HEIGHT),
                    MAP_FILE_NAME);

            assertTrue(model.hasMapLoaded());
            assertNotNull(model.getMapImage());
            assertNotNull(model.getMapImageCropped());
            assertNotNull(model.getMapImageResampled());
            assertNotNull(model.getMetadata());
        }

        /**
         * Tests that loading stores the supplied map name and the fallback path
         * used by the input-stream overload.
         */
        @Test
        void loadMapStoresMapFileNameAndFallbackPath() {

            assertEquals(MAP_FILE_NAME, mapModel.getMapFileName());
            assertEquals(MAP_FILE_NAME, mapModel.getMapFilePath());
        }

        /**
         * Tests that an unloaded model reports fallback map identification and
         * reports that no map has been loaded.
         */
        @Test
        void unloadedModelReturnsFallbackMapIdentification() {

            MapModel unloadedModel = new MapModel();

            assertEquals("No map loaded", unloadedModel.getMapFileName());
            assertEquals("No map loaded", unloadedModel.getMapFilePath());
            assertFalse(unloadedModel.hasMapLoaded());
            assertNull(unloadedModel.getMapImage());
            assertNull(unloadedModel.getMapImageCropped());
            assertNull(unloadedModel.getMapImageResampled());
            assertNull(unloadedModel.getMetadata());
        }

        /**
         * Tests that unsupported image data is rejected.
         */
        @Test
        void loadMapRejectsUnsupportedImageData() {

            MapModel model = new MapModel();

            InputStream invalidInput = new ByteArrayInputStream("not an image".getBytes());

            assertThrows(
                    MapLoadException.class,
                    () -> model.loadMap(
                            invalidInput,
                            MAP_FILE_NAME));

            assertFalse(model.hasMapLoaded());
        }

        /**
         * Tests that a null input stream is rejected.
         */
        @Test
        void loadMapRejectsNullInputStream() {

            MapModel model = new MapModel();

            assertThrows(
                    MapLoadException.class,
                    () -> model.loadMap(
                            null,
                            MAP_FILE_NAME));

            assertFalse(model.hasMapLoaded());
        }

        /**
         * Tests that a failed replacement load leaves the previously loaded map
         * and metadata unchanged.
         *
         * @throws IOException if the initial PNG image cannot be created
         * @throws MapLoadException if the initial valid map cannot be loaded
         */
        @Test
        void failedLoadDoesNotReplacePreviouslyLoadedMap()
                throws IOException, MapLoadException {

            MapModel model = createLoadedMapModel(SOURCE_WIDTH, SOURCE_HEIGHT);
            BufferedImage originalImage = model.getMapImage();
            BufferedImage originalCroppedImage = model.getMapImageCropped();
            BufferedImage originalDisplayImage = model.getMapImageResampled();
            MapMetadata originalMetadata = model.getMetadata();

            InputStream invalidInput =
                    new ByteArrayInputStream(
                            "not an image".getBytes());

            assertThrows(
                    MapLoadException.class,
                    () -> model.loadMap(
                            invalidInput,
                            "invalid_map.png"));

            assertSame(originalImage, model.getMapImage());
            assertSame(originalCroppedImage, model.getMapImageCropped());
            assertSame(originalDisplayImage, model.getMapImageResampled());
            assertSame(originalMetadata, model.getMetadata());
            assertEquals(MAP_FILE_NAME, model.getMapFileName());
            assertEquals(MAP_FILE_NAME, model.getMapFilePath());
        }

        /**
         * Tests that an image too small for the configured camera margins is
         * rejected during loading.
         *
         * @throws IOException if the in-memory PNG image cannot be created
         */
        @Test
        void loadMapRejectsImageTooSmallForCameraMargins() throws IOException {

            int tooSmallWidth = ModelSettings.CAMERA_SAFE_WORLD_PIXEL_MARGIN_X * 2;
            int tooSmallHeight = ModelSettings.CAMERA_SAFE_WORLD_PIXEL_MARGIN_Y * 2;

            MapModel model = new MapModel();

            assertThrows(
                    MapLoadException.class,
                    () -> model.loadMap(
                            createImageInputStream(
                                    tooSmallWidth,
                                    tooSmallHeight),
                            "small_map.png"));

            assertFalse(model.hasMapLoaded());
        }
    }

    /**
     * Tests map loading from disk together with optional sidecar metadata.
     */
    @Nested
    class FileLoadingTests {

        /**
         * Tests that loading a map file applies valid sidecar metadata.
         *
         * @throws IOException if the temporary image or metadata cannot be written
         * @throws MapLoadException if the map cannot be loaded
         */
        @Test
        void loadMapFileAppliesValidSidecarMetadata() throws IOException, MapLoadException {

            Path imagePath = tempDirectory.resolve("map.png");
            Path metadataPath = tempDirectory.resolve("map.metadata.json");

            writePngImage(
                    imagePath,
                    SOURCE_WIDTH,
                    SOURCE_HEIGHT);

            Files.writeString(metadataPath, """
                {
                  "title": "File map",
                  "source": "Test source",
                  "metersPerPixel": 0.5,
                  "coordinateReferenceSystem": "EPSG:4326",
                  "upperLeftLatitude": 46.0,
                  "upperLeftLongitude": 13.0
                }
                """);

            MapModel model = new MapModel();

            model.loadMap(imagePath.toFile());

            MapMetadata metadata = model.getMetadata();

            assertTrue(model.hasMapLoaded());
            assertEquals("map.png", model.getMapFileName());
            assertEquals(imagePath.toFile().getAbsolutePath(), model.getMapFilePath());
            assertEquals("File map", metadata.getTitle());
            assertEquals("Test source", metadata.getSource());
            assertEquals(0.5, metadata.getMetersPerPixel(), DELTA);
            assertEquals(MapMetadata.ScaleSource.FILE_METADATA, metadata.getScaleSource());
            assertEquals(46.0, metadata.getUpperLeftLatitude(), DELTA);
            assertEquals(13.0, metadata.getUpperLeftLongitude(), DELTA);
            assertEquals("EPSG:4326", metadata.getCoordinateReferenceSystem());
            assertEquals(MapMetadata.GeoReferenceSource.FILE_METADATA, metadata.getGeoReferenceSource());
        }

        /**
         * Tests that malformed sidecar JSON causes the complete map load to fail.
         *
         * @throws IOException if the temporary files cannot be written
         */
        @Test
        void loadMapFileRejectsMalformedSidecarMetadata()
                throws IOException {

            Path imagePath = tempDirectory.resolve("map.png");
            Path metadataPath = tempDirectory.resolve("map.metadata.json");

            writePngImage(
                    imagePath,
                    SOURCE_WIDTH,
                    SOURCE_HEIGHT);

            Files.writeString(metadataPath, "{ invalid json }");

            MapModel model = new MapModel();

            assertThrows(MapLoadException.class,
                    () -> model.loadMap(
                            imagePath.toFile()));

            assertFalse(model.hasMapLoaded());
        }

        /**
         * Tests that invalid sidecar metadata does not replace an existing map.
         *
         * @throws IOException if the temporary files cannot be written
         * @throws MapLoadException if the initial valid map cannot be loaded
         */
        @Test
        void invalidSidecarMetadataDoesNotReplaceExistingMap()
                throws IOException, MapLoadException {

            MapModel model = createLoadedMapModel(SOURCE_WIDTH, SOURCE_HEIGHT);

            BufferedImage originalImage = model.getMapImage();
            MapMetadata originalMetadata = model.getMetadata();
            BufferedImage originalCroppedImage = model.getMapImageCropped();
            BufferedImage originalDisplayImage = model.getMapImageResampled();
            String originalPath = model.getMapFilePath();

            Path imagePath = tempDirectory.resolve("replacement.png");
            Path metadataPath = tempDirectory.resolve("replacement.metadata.json");

            writePngImage(imagePath, SOURCE_WIDTH, SOURCE_HEIGHT);

            Files.writeString(metadataPath, """
                {
                  "title": "Invalid replacement",
                  "metersPerPixel": 0.0
                }
                """);

            assertThrows(MapLoadException.class,
                    () -> model.loadMap(
                            imagePath.toFile()));

            assertSame(originalImage, model.getMapImage());
            assertSame(originalMetadata, model.getMetadata());
            assertSame(originalCroppedImage, model.getMapImageCropped());
            assertSame(originalDisplayImage, model.getMapImageResampled());
            assertEquals(originalPath, model.getMapFilePath());
            assertEquals(MAP_FILE_NAME, model.getMapFileName());
        }

        /**
         * Tests that a missing map file is rejected.
         */
        @Test
        void loadMapFileRejectsMissingFile() {

            MapModel model = new MapModel();

            Path missingPath = tempDirectory.resolve("missing.png");

            assertThrows(
                    MapLoadException.class,
                    () -> model.loadMap(
                            missingPath.toFile()));

            assertFalse(model.hasMapLoaded());
        }

        /**
         * Tests that a null map file is rejected.
         */
        @Test
        void loadMapFileRejectsNullFile() {

            MapModel model = new MapModel();

            assertThrows(
                    MapLoadException.class,
                    () -> model.loadMap((java.io.File) null));

            assertFalse(model.hasMapLoaded());
        }

        /**
         * Tests that a map image without a sidecar metadata file loads using
         * default metadata.
         *
         * @throws IOException if the temporary image cannot be written
         * @throws MapLoadException if the map cannot be loaded
         */
        @Test
        void loadMapFileWithoutSidecarUsesDefaultMetadata()
                throws IOException, MapLoadException {

            Path imagePath = tempDirectory.resolve("map_without_metadata.png");

            writePngImage(imagePath, SOURCE_WIDTH, SOURCE_HEIGHT);

            MapModel model = new MapModel();

            model.loadMap(imagePath.toFile());

            MapMetadata metadata = model.getMetadata();

            assertTrue(model.hasMapLoaded());

            assertEquals(
                    ModelSettings.DEFAULT_MAP_METERS_PER_PIXEL,
                    metadata.getMetersPerPixel(),
                    DELTA);

            assertEquals(
                    MapMetadata.ScaleSource.DEFAULT,
                    metadata.getScaleSource());

            assertEquals(
                    MapMetadata.GeoReferenceSource.UNKNOWN,
                    metadata.getGeoReferenceSource());
        }
    }

    /**
     * Tests dimensions and metadata created from the loaded image.
     */
    @Nested
    class DimensionAndMetadataTests {

        /**
         * Tests dimensions of the cropped world image and resampled display image.
         */
        @Test
        void dimensionMethodsReturnProcessedImageDimensions() {
            assertEquals(SOURCE_HEIGHT, mapModel.getWorldWidth());
            assertEquals(SOURCE_HEIGHT, mapModel.getWorldHeight());
            assertEquals(ModelSettings.MAP_IMAGE_RESAMPLED_WIDTH, mapModel.getDisplayWidth());
            assertEquals(ModelSettings.MAP_IMAGE_RESAMPLED_HEIGHT, mapModel.getDisplayHeight());
        }

        /**
         * Tests that loading creates default metadata from the image dimensions.
         */
        @Test
        void loadMapCreatesDefaultMetadataFromProcessedImages() {
            MapMetadata metadata = mapModel.getMetadata();

            assertEquals(MAP_FILE_NAME, metadata.getFileName());
            assertEquals(SOURCE_WIDTH, metadata.getOriginalWidthPixels());
            assertEquals(SOURCE_HEIGHT, metadata.getOriginalHeightPixels());
            assertEquals(SOURCE_HEIGHT, metadata.getWorldWidthPixels());
            assertEquals(SOURCE_HEIGHT, metadata.getWorldHeightPixels());
            assertEquals(ModelSettings.MAP_IMAGE_RESAMPLED_WIDTH, metadata.getDisplayWidthPixels());
            assertEquals(ModelSettings.MAP_IMAGE_RESAMPLED_HEIGHT, metadata.getDisplayHeightPixels());
            assertEquals(ModelSettings.DEFAULT_MAP_METERS_PER_PIXEL, metadata.getMetersPerPixel(), DELTA);
            assertEquals(MapMetadata.ScaleSource.DEFAULT, metadata.getScaleSource());
        }

        /**
         * Tests that map attribution delegates to the loaded metadata.
         */
        @Test
        void getMapAttributionReturnsMetadataAttribution() {
            assertEquals(mapModel.getMetadata().getAttribution(), mapModel.getMapAttribution());
        }

        /**
         * Tests that manual map scale updates are delegated to the metadata.
         */
        @Test
        void setMapMetersPerPixelUpdatesMetadataScaleAndSource() {
            mapModel.setMapMetersPerPixel(0.5);

            assertEquals(0.5, mapModel.getMapMetersPerPixel(), DELTA);
            assertEquals(MapMetadata.ScaleSource.MANUAL, mapModel.getMetadata().getScaleSource());
        }

        /**
         * Tests that manual georeference updates are delegated to the metadata.
         */
        @Test
        void setManualGeoReferenceUpdatesMetadata() {
            mapModel.setManualGeoReference(
                    46.0712,
                    13.2345,
                    " EPSG:4326 ");

            MapMetadata metadata = mapModel.getMetadata();

            assertEquals(46.0712, metadata.getUpperLeftLatitude(), DELTA);
            assertEquals(13.2345, metadata.getUpperLeftLongitude(), DELTA);
            assertEquals("EPSG:4326", metadata.getCoordinateReferenceSystem());
            assertEquals(MapMetadata.GeoReferenceSource.MANUAL, metadata.getGeoReferenceSource());
        }
    }

    /**
     * Tests scalar and vector conversion between pixels and meters.
     */
    @Nested
    class UnitConversionTests {

        /**
         * Tests scalar conversion between world-image pixels and meters.
         */
        @Test
        void scalarConversionMethodsUseCurrentMapScale() {
            mapModel.setMapMetersPerPixel(0.25);

            assertEquals(25.0, mapModel.pixelsToMeters(100.0), DELTA);
            assertEquals(100.0, mapModel.metersToPixels(25.0), DELTA);
        }

        /**
         * Tests vector conversion from world-image pixels to meters.
         */
        @Test
        void worldPixelsToMetersConvertsBothComponents() {
            mapModel.setMapMetersPerPixel(0.25);

            Vector2D result = mapModel.worldPixelsToMeters(
                    new Vector2D(400.0, 200.0));

            assertVectorEquals(100.0, 50.0, result);
        }

        /**
         * Tests vector conversion from world meters to world-image pixels.
         */
        @Test
        void worldMetersToPixelsConvertsBothComponents() {
            mapModel.setMapMetersPerPixel(0.25);

            Vector2D result = mapModel.worldMetersToPixels(
                    new Vector2D(100.0, 50.0));

            assertVectorEquals(400.0, 200.0, result);
        }

        /**
         * Tests that converting world pixels to meters and back preserves the point.
         */
        @Test
        void worldPixelMeterRoundTripReturnsOriginalPoint() {
            Vector2D original = new Vector2D(347.75, 612.25);

            Vector2D meters = mapModel.worldPixelsToMeters(original);
            Vector2D converted = mapModel.worldMetersToPixels(meters);

            assertVectorEquals(original.getX(), original.getY(), converted);
        }
    }

    /**
     * Tests conversion between world-image and display coordinates.
     */
    @Nested
    class CoordinateConversionTests {

        /**
         * Tests conversion from world-image coordinates to display coordinates.
         */
        @Test
        void worldToDisplayScalesCoordinatesUsingImageDimensions() {
            Vector2D result = mapModel.worldToDisplay(new Vector2D(400.0, 200.0));

            assertVectorEquals(340.0, 170.0, result);
        }

        /**
         * Tests conversion from display coordinates to world-image coordinates.
         */
        @Test
        void displayToWorldScalesCoordinatesUsingImageDimensions() {
            Vector2D result = mapModel.displayToWorld(new Vector2D(340.0, 170.0));

            assertVectorEquals(400.0, 200.0, result);
        }

        /**
         * Tests that world-to-display-to-world conversion preserves the point.
         */
        @Test
        void worldDisplayRoundTripReturnsOriginalPoint() {
            Vector2D original = new Vector2D(123.5, 678.25);

            Vector2D display = mapModel.worldToDisplay(original);
            Vector2D converted = mapModel.displayToWorld(display);

            assertVectorEquals(original.getX(), original.getY(), converted);
        }

        /**
         * Tests direct conversion from world meters to display pixels.
         */
        @Test
        void worldMetersToDisplayCombinesUnitAndDisplayConversion() {
            mapModel.setMapMetersPerPixel(0.25);

            Vector2D result = mapModel.worldMetersToDisplay(new Vector2D(100.0, 50.0));

            assertVectorEquals(340.0, 170.0, result);
        }

        /**
         * Tests direct conversion from display pixels to world meters.
         */
        @Test
        void displayToWorldMetersCombinesDisplayAndUnitConversion() {
            mapModel.setMapMetersPerPixel(0.25);

            Vector2D result = mapModel.displayToWorldMeters(new Vector2D(340.0, 170.0));

            assertVectorEquals(100.0, 50.0, result);
        }

        /**
         * Tests that world-meter-to-display-to-world-meter conversion preserves the point.
         */
        @Test
        void worldMeterDisplayRoundTripReturnsOriginalPoint() {
            Vector2D original = new Vector2D(37.25, 146.5);

            Vector2D display = mapModel.worldMetersToDisplay(original);
            Vector2D converted = mapModel.displayToWorldMeters(display);

            assertVectorEquals(original.getX(), original.getY(), converted);
        }
    }

    /**
     * Tests generation and validation of random safe map positions.
     */
    @Nested
    class RandomPositionTests {

        /**
         * Tests that random safe world-pixel positions respect the camera margins.
         */
        @Test
        void createRandomSafeWorldPixelPositionStaysInsideCameraMargins() {

            Random random = new Random(12345L);

            for (int i = 0; i < 20; i++) {

                Vector2D position = mapModel.createRandomSafeWorldPixelPosition(random);

                assertTrue(position.getX() >= ModelSettings.CAMERA_SAFE_WORLD_PIXEL_MARGIN_X);
                assertTrue(position.getX()
                        < mapModel.getWorldWidth() - ModelSettings.CAMERA_SAFE_WORLD_PIXEL_MARGIN_X);
                assertTrue(position.getY() >= ModelSettings.CAMERA_SAFE_WORLD_PIXEL_MARGIN_Y);
                assertTrue(position.getY()
                        < mapModel.getWorldHeight() - ModelSettings.CAMERA_SAFE_WORLD_PIXEL_MARGIN_Y);
            }
        }

        /**
         * Tests that random safe world-meter positions correspond to valid pixel positions.
         */
        @Test
        void createRandomSafeWorldMeterPositionUsesCurrentMapScale() {

            mapModel.setMapMetersPerPixel(0.5);
            Random random = new Random(9876L);

            Vector2D positionMeters = mapModel.createRandomSafeWorldMeterPosition(random);
            Vector2D positionPixels = mapModel.worldMetersToPixels(positionMeters);

            assertTrue(positionPixels.getX() >= ModelSettings.CAMERA_SAFE_WORLD_PIXEL_MARGIN_X);
            assertTrue(positionPixels.getX()
                    < mapModel.getWorldWidth() - ModelSettings.CAMERA_SAFE_WORLD_PIXEL_MARGIN_X);
            assertTrue(positionPixels.getY() >= ModelSettings.CAMERA_SAFE_WORLD_PIXEL_MARGIN_Y);
            assertTrue(positionPixels.getY()
                    < mapModel.getWorldHeight() - ModelSettings.CAMERA_SAFE_WORLD_PIXEL_MARGIN_Y);
        }
    }

    /**
     * Tests operations that require a loaded map or map metadata.
     */
    @Nested
    class ValidationTests {

        /**
         * Tests that metadata-dependent operations reject an unloaded model.
         */
        @Test
        void metadataDependentOperationsRejectUnloadedModel() {
            MapModel unloadedModel = new MapModel();

            assertThrows(IllegalStateException.class, unloadedModel::getMapAttribution);
            assertThrows(IllegalStateException.class, unloadedModel::getMapMetersPerPixel);
            assertThrows(IllegalStateException.class,
                    () -> unloadedModel.setMapMetersPerPixel(0.25));
            assertThrows(IllegalStateException.class,
                    () -> unloadedModel.pixelsToMeters(10.0));
            assertThrows(IllegalStateException.class,
                    () -> unloadedModel.worldPixelsToMeters(Vector2D.ZERO));
            assertThrows(IllegalStateException.class,
                    () -> unloadedModel.setManualGeoReference(
                            null,
                            null,
                            null));
        }

        /**
         * Tests that coordinate conversion requiring processed images rejects an unloaded model.
         */
        @Test
        void coordinateConversionRejectsUnloadedModel() {
            MapModel unloadedModel = new MapModel();

            assertThrows(NullPointerException.class,
                    () -> unloadedModel.worldToDisplay(Vector2D.ZERO));
            assertThrows(NullPointerException.class,
                    () -> unloadedModel.displayToWorld(Vector2D.ZERO));
        }

        /**
         * Tests that invalid manual map scales are rejected.
         *
         * @param metersPerPixel invalid map scale.
         */
        @ParameterizedTest
        @ValueSource(doubles = {
                0.0,
                -0.25,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        })
        void setMapMetersPerPixelRejectsInvalidValues(
                double metersPerPixel) {

            assertThrows(
                    IllegalArgumentException.class,
                    () -> mapModel.setMapMetersPerPixel(metersPerPixel));
        }

        /**
         * Tests that invalid manual map scales are rejected without changing the stored metadata.
         */
        @Test
        void setMapMetersPerPixelRejectsInvalidValueWithoutChangingMetadata() {

            double originalScale = mapModel.getMapMetersPerPixel();

            MapMetadata.ScaleSource originalSource = mapModel.getMetadata().getScaleSource();

            assertThrows(IllegalArgumentException.class,
                    () -> mapModel.setMapMetersPerPixel(0.0));

            assertEquals(originalScale, mapModel.getMapMetersPerPixel(), DELTA);

            assertEquals(originalSource, mapModel.getMetadata().getScaleSource());
        }
    }

    // ########################################################################
    // Helper methods
    // ########################################################################

    /**
     * Creates and loads a map model from an in-memory PNG image.
     *
     * @param width source image width
     * @param height source image height
     * @return initialized map model
     * @throws IOException if the in-memory PNG image cannot be created
     * @throws MapLoadException if the map cannot be loaded
     */
    private static MapModel createLoadedMapModel(
            int width,
            int height)
            throws IOException, MapLoadException {

        MapModel model = new MapModel();

        model.loadMap(
                createImageInputStream(
                        width,
                        height),
                MAP_FILE_NAME);

        return model;
    }

    /**
     * Creates an input stream containing an in-memory PNG image.
     *
     * @param width image width.
     * @param height image height.
     * @return input stream containing PNG data.
     * @throws IOException if the PNG image cannot be encoded.
     */
    private static InputStream createImageInputStream(int width, int height)
            throws IOException {

        BufferedImage image = createImage(width, height);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        if (!ImageIO.write(image, "png", outputStream)) {
            throw new IOException("No PNG image writer is available.");
        }

        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * Creates a representative RGB image with the supplied dimensions.
     *
     * @param width image width.
     * @param height image height.
     * @return created image.
     */
    private static BufferedImage createImage(int width, int height) {
        return new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_RGB);
    }

    /**
     * Asserts that a vector contains the expected x- and y-components.
     *
     * @param expectedX expected x-component.
     * @param expectedY expected y-component.
     * @param actual actual vector to inspect.
     */
    private static void assertVectorEquals(
            double expectedX,
            double expectedY,
            Vector2D actual) {

        assertEquals(expectedX, actual.getX(), DELTA);
        assertEquals(expectedY, actual.getY(), DELTA);
    }

    /**
     * Writes a representative PNG image to disk.
     *
     * @param path output file path
     * @param width image width
     * @param height image height
     * @throws IOException if the image cannot be written
     */
    private static void writePngImage(
            Path path,
            int width,
            int height)
            throws IOException {

        BufferedImage image =
                createImage(width, height);

        if (!ImageIO.write(
                image,
                "png",
                path.toFile())) {

            throw new IOException(
                    "No PNG image writer is available.");
        }
    }
}
