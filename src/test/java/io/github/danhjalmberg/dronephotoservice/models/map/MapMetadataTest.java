package io.github.danhjalmberg.dronephotoservice.models.map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link MapMetadata} class.
 * The tests cover representative construction, unit conversion, scale updates,
 * georeference handling, file metadata application, and validation behavior
 * without attempting exhaustive coverage of every possible metadata value.
 *
 * @author Dan Hjälmberg
 */
class MapMetadataTest {

    /**
     * Tolerance used when comparing floating-point results.
     */
    private static final double DELTA = 1.0e-9;

    /**
     * Default meters-per-pixel value used by test metadata.
     */
    private static final double DEFAULT_METERS_PER_PIXEL = 0.25;

    /**
     * Tests construction and image-derived metadata values.
     */
    @Nested
    class ConstructionAndDimensionTests {

        /**
         * Tests that the constructor stores the supplied file name and image dimensions.
         */
        @Test
        void constructorStoresFileNameAndImageDimensions() {
            MapMetadata metadata = createMetadata();

            assertEquals("test_map.jpg", metadata.getFileName());
            assertEquals(1200, metadata.getOriginalWidthPixels());
            assertEquals(1000, metadata.getOriginalHeightPixels());
            assertEquals(1000, metadata.getWorldWidthPixels());
            assertEquals(1000, metadata.getWorldHeightPixels());
            assertEquals(680, metadata.getDisplayWidthPixels());
            assertEquals(680, metadata.getDisplayHeightPixels());
        }

        /**
         * Tests that the constructor stores the supplied scale and scale source.
         */
        @Test
        void constructorStoresScaleAndScaleSource() {
            MapMetadata metadata = createMetadata();

            assertEquals(DEFAULT_METERS_PER_PIXEL, metadata.getMetersPerPixel(), DELTA);
            assertEquals(MapMetadata.ScaleSource.DEFAULT, metadata.getScaleSource());
        }

        /**
         * Tests calculation of the real-world map dimensions in meters.
         */
        @Test
        void worldDimensionMethodsUseWorldPixelsAndMapScale() {
            MapMetadata metadata = createMetadata();

            assertEquals(250.0, metadata.getWorldWidthMeters(), DELTA);
            assertEquals(250.0, metadata.getWorldHeightMeters(), DELTA);
        }
    }

    /**
     * Tests conversion between world-image pixels and real-world meters.
     */
    @Nested
    class UnitConversionTests {

        /**
         * Tests conversion from world-image pixels to meters.
         */
        @Test
        void pixelsToMetersUsesCurrentMapScale() {
            MapMetadata metadata = createMetadata();

            assertEquals(25.0, metadata.pixelsToMeters(100.0), DELTA);
        }

        /**
         * Tests conversion from meters to world-image pixels.
         */
        @Test
        void metersToPixelsUsesCurrentMapScale() {
            MapMetadata metadata = createMetadata();

            assertEquals(100.0, metadata.metersToPixels(25.0), DELTA);
        }

        /**
         * Tests that converting pixels to meters and back preserves the original value.
         */
        @Test
        void pixelMeterRoundTripReturnsOriginalDistance() {
            MapMetadata metadata = createMetadata();
            double originalPixels = 347.75;

            double meters = metadata.pixelsToMeters(originalPixels);
            double convertedPixels = metadata.metersToPixels(meters);

            assertEquals(originalPixels, convertedPixels, DELTA);
        }

        /**
         * Tests pixel-to-meter conversion for zero and negative distances.
         *
         * @param pixels input distance in pixels.
         * @param expectedMeters expected distance in meters.
         */
        @ParameterizedTest
        @CsvSource({
                "0.0,   0.0",
                "-50.0, -12.5"
        })
        void pixelsToMetersSupportsZeroAndNegativeDistances(
                double pixels,
                double expectedMeters) {

            MapMetadata metadata = createMetadata();

            assertEquals(
                    expectedMeters,
                    metadata.pixelsToMeters(pixels),
                    DELTA);
        }

        /**
         * Tests meter-to-pixel conversion for zero and negative distances.
         *
         * @param meters input distance in meters.
         * @param expectedPixels expected distance in pixels.
         */
        @ParameterizedTest
        @CsvSource({
                "0.0,   0.0",
                "-12.5, -50.0"
        })
        void metersToPixelsSupportsZeroAndNegativeDistances(
                double meters,
                double expectedPixels) {

            MapMetadata metadata = createMetadata();

            assertEquals(
                    expectedPixels,
                    metadata.metersToPixels(meters),
                    DELTA);
        }
    }

    /**
     * Tests manual scale and georeference updates.
     */
    @Nested
    class ManualMetadataTests {

        /**
         * Tests that a manual scale update changes the scale and its source.
         */
        @Test
        void setManualMetersPerPixelUpdatesScaleAndSource() {
            MapMetadata metadata = createMetadata();

            metadata.setManualMetersPerPixel(0.5);

            assertEquals(0.5, metadata.getMetersPerPixel(), DELTA);
            assertEquals(MapMetadata.ScaleSource.MANUAL, metadata.getScaleSource());
            assertEquals(500.0, metadata.getWorldWidthMeters(), DELTA);
        }

        /**
         * Tests that valid manual georeference information is stored and marked as manual.
         */
        @Test
        void setManualGeoReferenceStoresValidValues() {
            MapMetadata metadata = createMetadata();

            metadata.setManualGeoReference(
                    46.0712,
                    13.2345,
                    " EPSG:4326 ");

            assertEquals(46.0712, metadata.getUpperLeftLatitude(), DELTA);
            assertEquals(13.2345, metadata.getUpperLeftLongitude(), DELTA);
            assertEquals("EPSG:4326", metadata.getCoordinateReferenceSystem());
            assertEquals(MapMetadata.GeoReferenceSource.MANUAL, metadata.getGeoReferenceSource());
        }

        /**
         * Tests that blank manual coordinate reference system text is stored as null.
         */
        @Test
        void setManualGeoReferenceConvertsBlankCoordinateSystemToNull() {
            MapMetadata metadata = createMetadata();

            metadata.setManualGeoReference(null, null, "   ");

            assertNull(metadata.getUpperLeftLatitude());
            assertNull(metadata.getUpperLeftLongitude());
            assertNull(metadata.getCoordinateReferenceSystem());
            assertEquals(MapMetadata.GeoReferenceSource.MANUAL, metadata.getGeoReferenceSource());
        }

        /**
         * Tests that latitude and longitude boundary values are accepted.
         *
         * @param latitude valid boundary latitude.
         * @param longitude valid boundary longitude.
         */
        @ParameterizedTest
        @CsvSource({
                "-90.0, -180.0",
                "90.0, 180.0"
        })
        void setManualGeoReferenceAcceptsBoundaryValues(
                double latitude,
                double longitude) {

            MapMetadata metadata = createMetadata();

            metadata.setManualGeoReference(
                    latitude,
                    longitude,
                    "EPSG:4326");

            assertEquals(
                    latitude,
                    metadata.getUpperLeftLatitude(),
                    DELTA);

            assertEquals(
                    longitude,
                    metadata.getUpperLeftLongitude(),
                    DELTA);
        }
    }

    /**
     * Tests application of optional metadata loaded from a sidecar file.
     */
    @Nested
    class FileMetadataTests {

        /**
         * Tests that null file metadata leaves the existing metadata unchanged.
         */
        @Test
        void applyFileMetadataIgnoresNull() {
            MapMetadata metadata = createMetadata();

            metadata.applyFileMetadata(null);

            assertEquals(DEFAULT_METERS_PER_PIXEL, metadata.getMetersPerPixel(), DELTA);
            assertEquals(MapMetadata.ScaleSource.DEFAULT, metadata.getScaleSource());
            assertEquals(MapMetadata.GeoReferenceSource.UNKNOWN, metadata.getGeoReferenceSource());
        }

        /**
         * Tests that file metadata updates descriptive, scale, and georeference values.
         */
        @Test
        void applyFileMetadataAppliesAvailableValuesAndSources() {
            MapMetadata metadata = createMetadata();
            MapFileMetadata fileMetadata = new MapFileMetadata();

            fileMetadata.setTitle(" Test map ");
            fileMetadata.setSource(" Regional open data ");
            fileMetadata.setLicense(" IODL 2.0 ");
            fileMetadata.setAttribution(" Map provider ");
            fileMetadata.setDerivedFrom(" Orthophoto ");
            fileMetadata.setModified(" Cropped and resampled ");
            fileMetadata.setMetersPerPixel(0.4);
            fileMetadata.setUpperLeftLatitude(46.1);
            fileMetadata.setUpperLeftLongitude(13.2);
            fileMetadata.setCoordinateReferenceSystem("EPSG:6708");

            metadata.applyFileMetadata(fileMetadata);

            assertEquals("Test map", metadata.getTitle());
            assertEquals("Regional open data", metadata.getSource());
            assertEquals("IODL 2.0", metadata.getLicense());
            assertEquals("Map provider", metadata.getAttribution());
            assertEquals("Orthophoto", metadata.getDerivedFrom());
            assertEquals("Cropped and resampled", metadata.getModified());
            assertEquals(0.4, metadata.getMetersPerPixel(), DELTA);
            assertEquals(MapMetadata.ScaleSource.FILE_METADATA, metadata.getScaleSource());
            assertEquals(46.1, metadata.getUpperLeftLatitude(), DELTA);
            assertEquals(13.2, metadata.getUpperLeftLongitude(), DELTA);
            assertEquals("EPSG:6708", metadata.getCoordinateReferenceSystem());
            assertEquals(MapMetadata.GeoReferenceSource.FILE_METADATA, metadata.getGeoReferenceSource());
        }

        /**
         * Tests that invalid file metadata is rejected when applied, without changing the existing scale.
         */
        @Test
        void applyFileMetadataRejectsInvalidScaleWithoutChangingExistingScale() {

            MapMetadata metadata = createMetadata();

            MapFileMetadata fileMetadata = new MapFileMetadata();
            fileMetadata.setMetersPerPixel(0.0);

            assertThrows(IllegalArgumentException.class,
                    () -> metadata.applyFileMetadata(fileMetadata));

            assertEquals(DEFAULT_METERS_PER_PIXEL, metadata.getMetersPerPixel(), DELTA);

            assertEquals(MapMetadata.ScaleSource.DEFAULT, metadata.getScaleSource());
        }

        /**
         * Tests that blank descriptive file metadata values are stored as null.
         */
        @Test
        void applyFileMetadataConvertsBlankDescriptiveValuesToNull() {
            MapMetadata metadata = createMetadata();
            MapFileMetadata fileMetadata = new MapFileMetadata();

            fileMetadata.setTitle(" ");
            fileMetadata.setSource("");
            fileMetadata.setLicense("   ");
            fileMetadata.setAttribution(null);

            metadata.applyFileMetadata(fileMetadata);

            assertNull(metadata.getTitle());
            assertNull(metadata.getSource());
            assertNull(metadata.getLicense());
            assertNull(metadata.getAttribution());
        }

        /**
         * Tests that invalid incoming file metadata does not partially replace
         * previously stored descriptive, scale, or georeference metadata.
         */
        @Test
        void applyFileMetadataRejectsInvalidValuesWithoutChangingExistingMetadata() {

            MapMetadata metadata = createMetadata();

            MapFileMetadata existingMetadata = new MapFileMetadata();
            existingMetadata.setTitle("Existing map");
            existingMetadata.setSource("Existing source");
            existingMetadata.setLicense("Existing license");
            existingMetadata.setMetersPerPixel(0.5);
            existingMetadata.setUpperLeftLatitude(46.0);
            existingMetadata.setUpperLeftLongitude(13.0);
            existingMetadata.setCoordinateReferenceSystem("EPSG:4326");

            metadata.applyFileMetadata(existingMetadata);

            MapFileMetadata invalidMetadata = new MapFileMetadata();
            invalidMetadata.setTitle("Replacement map");
            invalidMetadata.setSource("Replacement source");
            invalidMetadata.setLicense("Replacement license");
            invalidMetadata.setMetersPerPixel(0.75);
            invalidMetadata.setUpperLeftLatitude(91.0);
            invalidMetadata.setUpperLeftLongitude(14.0);
            invalidMetadata.setCoordinateReferenceSystem("EPSG:6708");

            assertThrows(
                    IllegalArgumentException.class,
                    () -> metadata.applyFileMetadata(invalidMetadata));

            assertEquals("Existing map", metadata.getTitle());
            assertEquals("Existing source", metadata.getSource());
            assertEquals("Existing license", metadata.getLicense());

            assertEquals(
                    0.5,
                    metadata.getMetersPerPixel(),
                    DELTA);

            assertEquals(
                    MapMetadata.ScaleSource.FILE_METADATA,
                    metadata.getScaleSource());

            assertEquals(
                    46.0,
                    metadata.getUpperLeftLatitude(),
                    DELTA);

            assertEquals(
                    13.0,
                    metadata.getUpperLeftLongitude(),
                    DELTA);

            assertEquals(
                    "EPSG:4326",
                    metadata.getCoordinateReferenceSystem());

            assertEquals(
                    MapMetadata.GeoReferenceSource.FILE_METADATA,
                    metadata.getGeoReferenceSource());
        }

        /**
         * Tests that a blank coordinate reference system without coordinates does not
         * mark the metadata as having a file georeference.
         */
        @Test
        void applyFileMetadataIgnoresBlankCoordinateReferenceSystem() {

            MapMetadata metadata = createMetadata();
            MapFileMetadata fileMetadata = new MapFileMetadata();

            fileMetadata.setCoordinateReferenceSystem("   ");

            metadata.applyFileMetadata(fileMetadata);

            assertNull(metadata.getCoordinateReferenceSystem());

            assertEquals(
                    MapMetadata.GeoReferenceSource.UNKNOWN,
                    metadata.getGeoReferenceSource());
        }
    }

    /**
     * Tests rejection of invalid scale and georeference values.
     */
    @Nested
    class ValidationTests {

        /**
         * Tests that construction rejects a non-positive or non-finite map scale.
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
        void constructorRejectsInvalidMetersPerPixel(double metersPerPixel) {
            assertThrows(IllegalArgumentException.class,
                    () -> createMetadataWithScale(metersPerPixel));
        }

        /**
         * Tests that manual scale updates reject invalid map scales.
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
        void setManualMetersPerPixelRejectsInvalidValues(
                double metersPerPixel) {

            MapMetadata metadata = createMetadata();

            assertThrows(
                    IllegalArgumentException.class,
                    () -> metadata.setManualMetersPerPixel(metersPerPixel));
        }

        /**
         * Tests that manual scale updates reject invalid map scales without changing the existing scale.
         */
        @Test
        void setManualMetersPerPixelRejectsInvalidValueWithoutChangingExistingScale() {

            MapMetadata metadata = createMetadata();

            assertThrows(IllegalArgumentException.class,
                    () -> metadata.setManualMetersPerPixel(0.0));

            assertEquals(DEFAULT_METERS_PER_PIXEL, metadata.getMetersPerPixel(), DELTA);

            assertEquals(MapMetadata.ScaleSource.DEFAULT, metadata.getScaleSource());
        }

        /**
         * Tests that manual georeference updates reject latitude values that are
         * outside the valid range or are non-finite.
         *
         * @param latitude invalid latitude.
         */
        @ParameterizedTest
        @ValueSource(doubles = {
                -90.1,
                90.1,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        })
        void setManualGeoReferenceRejectsInvalidLatitude(double latitude) {

            MapMetadata metadata = createMetadata();

            assertThrows(IllegalArgumentException.class,
                    () -> metadata.setManualGeoReference(
                            latitude,
                            13.0,
                            "EPSG:4326"));
        }

        /**
         * Tests that manual georeference updates reject longitude values that are
         * outside the valid range or are non-finite.
         *
         * @param longitude invalid longitude.
         */
        @ParameterizedTest
        @ValueSource(doubles = {
                -180.1,
                180.1,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        })
        void setManualGeoReferenceRejectsInvalidLongitude(double longitude) {

            MapMetadata metadata = createMetadata();

            assertThrows(
                    IllegalArgumentException.class,
                    () -> metadata.setManualGeoReference(
                            46.0,
                            longitude,
                            "EPSG:4326"));
        }

        /**
         * Tests that invalid file metadata is rejected when applied.
         */
        @Test
        void applyFileMetadataRejectsInvalidScaleAndGeoreferenceValues() {
            MapMetadata metadata = createMetadata();

            MapFileMetadata invalidScale = new MapFileMetadata();
            invalidScale.setMetersPerPixel(0.0);

            MapFileMetadata invalidLatitude = new MapFileMetadata();
            invalidLatitude.setUpperLeftLatitude(91.0);

            MapFileMetadata nonFiniteLatitude = new MapFileMetadata();
            nonFiniteLatitude.setUpperLeftLatitude(Double.NaN);

            MapFileMetadata invalidLongitude = new MapFileMetadata();
            invalidLongitude.setUpperLeftLongitude(-181.0);

            MapFileMetadata nonFiniteLongitude = new MapFileMetadata();
            nonFiniteLongitude.setUpperLeftLongitude(Double.POSITIVE_INFINITY);

            assertThrows(IllegalArgumentException.class,
                    () -> metadata.applyFileMetadata(invalidScale));
            assertThrows(IllegalArgumentException.class,
                    () -> metadata.applyFileMetadata(invalidLatitude));
            assertThrows(IllegalArgumentException.class,
                    () -> metadata.applyFileMetadata(nonFiniteLatitude));
            assertThrows(IllegalArgumentException.class,
                    () -> metadata.applyFileMetadata(invalidLongitude));
            assertThrows(IllegalArgumentException.class,
                    () -> metadata.applyFileMetadata(nonFiniteLongitude));
        }
    }

    // ########################################################################
    // Helper methods
    // ########################################################################

    /**
     * Creates representative valid metadata for use by the tests.
     *
     * @return valid map metadata.
     */
    private static MapMetadata createMetadata() {
        return createMetadataWithScale(DEFAULT_METERS_PER_PIXEL);
    }

    /**
     * Creates representative metadata with a supplied map scale.
     *
     * @param metersPerPixel map scale in meters per world-image pixel.
     * @return map metadata using the supplied scale.
     */
    private static MapMetadata createMetadataWithScale(double metersPerPixel) {
        return new MapMetadata(
                "test_map.jpg",
                1200,
                1000,
                1000,
                1000,
                680,
                680,
                metersPerPixel,
                MapMetadata.ScaleSource.DEFAULT);
    }
}
