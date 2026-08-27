package io.github.danhjalmberg.dronephotoservice.models.map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link MapMetadataLoader} class.
 * The tests cover representative sidecar metadata loading, missing files,
 * malformed JSON, invalid property types, null arguments, and tolerance for
 * unknown JSON properties without attempting exhaustive coverage of every
 * file-system and Jackson deserialization failure.
 *
 * @author Dan Hjälmberg
 */
class MapMetadataLoaderTest {

    /**
     * Temporary directory supplied by JUnit for file-based tests.
     */
    @TempDir
    private Path tempDirectory;

    /**
     * Tests that a valid sidecar metadata file is loaded for an image file.
     *
     * @throws IOException if the temporary files cannot be written or read.
     */
    @Test
    void loadForImageFileReadsValidSidecarMetadata() throws IOException {

        Path imageFile = tempDirectory.resolve("test_map.jpg");
        Path metadataFile = tempDirectory.resolve("test_map.metadata.json");

        Files.createFile(imageFile);

        Files.writeString(metadataFile, """
                {
                  "schemaVersion": 1,
                  "title": "Test map",
                  "source": "Open data",
                  "metersPerPixel": 0.4,
                  "coordinateReferenceSystem": "EPSG:6708"
                }
                """);

        MapFileMetadata metadata = MapMetadataLoader.loadForImageFile(imageFile.toFile());

        assertNotNull(metadata);
        assertEquals(1, metadata.getSchemaVersion());
        assertEquals("Test map", metadata.getTitle());
        assertEquals("Open data", metadata.getSource());
        assertEquals(0.4, metadata.getMetersPerPixel());
        assertEquals("EPSG:6708", metadata.getCoordinateReferenceSystem());
    }

    /**
     * Tests that a missing sidecar metadata file results in null.
     *
     * @throws IOException if the temporary image file cannot be created.
     */
    @Test
    void loadForImageFileReturnsNullWhenSidecarFileIsMissing() throws IOException {

        Path imageFile = tempDirectory.resolve("test_map.jpg");
        Files.createFile(imageFile);

        MapFileMetadata metadata = MapMetadataLoader.loadForImageFile(imageFile.toFile());

        assertNull(metadata);
    }

    /**
     * Tests that malformed sidecar JSON is reported as an IOException.
     *
     * @throws IOException if the temporary files cannot be written.
     */
    @Test
    void loadForImageFileThrowsIOExceptionForMalformedJson() throws IOException {

        Path imageFile = tempDirectory.resolve("test_map.jpg");
        Path metadataFile = tempDirectory.resolve("test_map.metadata.json");

        Files.createFile(imageFile);
        Files.writeString(metadataFile, "{ invalid json }");

        assertThrows(IOException.class,
                () -> MapMetadataLoader.loadForImageFile(imageFile.toFile()));
    }

    /**
     * Tests that an incompatible JSON property type is reported as an IOException.
     *
     * @throws IOException if the temporary files cannot be written.
     */
    @Test
    void loadForImageFileThrowsIOExceptionForInvalidPropertyType()
            throws IOException {

        Path imageFile = tempDirectory.resolve("test_map.jpg");

        Path metadataFile = tempDirectory.resolve("test_map.metadata.json");

        Files.createFile(imageFile);

        Files.writeString(metadataFile, """
                {
                  "metersPerPixel": "not-a-number"
                }
                """);

        assertThrows(IOException.class,
                () -> MapMetadataLoader.loadForImageFile(
                        imageFile.toFile()));
    }

    /**
     * Tests that unknown JSON properties are ignored during deserialization.
     *
     * @throws IOException if the temporary files cannot be written or read.
     */
    @Test
    void loadForImageFileIgnoresUnknownProperties() throws IOException {

        Path imageFile = tempDirectory.resolve("test_map.jpg");

        Path metadataFile = tempDirectory.resolve("test_map.metadata.json");

        Files.createFile(imageFile);

        Files.writeString(metadataFile, """
                {
                  "title": "Test map",
                  "unknownProperty": "ignored"
                }
                """);

        MapFileMetadata metadata = MapMetadataLoader.loadForImageFile(imageFile.toFile());

        assertNotNull(metadata);
        assertEquals("Test map", metadata.getTitle());
    }

    /**
     * Tests that a null image file is rejected.
     */
    @Test
    void loadForImageFileRejectsNullImageFile() {

        assertThrows(IllegalArgumentException.class,
                () -> MapMetadataLoader.loadForImageFile(null));
    }

    /**
     * Tests that a null image resource path is rejected.
     */
    @Test
    void loadFromResourceRejectsNullImageResourcePath() {

        assertThrows(IllegalArgumentException.class,
                () -> MapMetadataLoader.loadFromResource(null));
    }

    /**
     * Tests that a missing metadata resource results in null.
     *
     * @throws IOException if resource access unexpectedly fails.
     */
    @Test
    void loadFromResourceReturnsNullWhenMetadataResourceIsMissing() throws IOException {

        MapFileMetadata metadata = MapMetadataLoader.loadFromResource(
                        "/maps/nonexistent_test_map.jpg");

        assertNull(metadata);
    }
}
