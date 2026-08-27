package io.github.danhjalmberg.dronephotoservice.models.map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Loads optional JSON sidecar metadata associated with map images.
 *
 * <p>The loader maps external JSON into {@link MapFileMetadata} without
 * modifying runtime {@link MapMetadata}. {@link MapModel} is responsible for
 * combining parsed file metadata with values derived from the loaded image.</p>
 *
 * @author Dan Hjälmberg
 */
public final class MapMetadataLoader {

    private static final String METADATA_EXTENSION = ".metadata.json";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Prevents instantiation of this static utility class.
     */
    private MapMetadataLoader() {
    }

    /**
     * Loads optional sidecar metadata for a classpath map resource.
     *
     * <p>For an image resource named {@code /maps/demo_map.jpg}, the matching
     * resource is {@code /maps/demo_map.metadata.json}.</p>
     *
     * @param imageResourcePath classpath path of the map image resource,
     *                          for example {@code /maps/demo_map.jpg}
     * @return parsed metadata, or {@code null} if the matching resource does
     *         not exist
     * @throws IllegalArgumentException if {@code imageResourcePath} is
     *                                  {@code null}
     * @throws IOException if the metadata resource exists but cannot be read
     *                     or parsed
     */
    public static MapFileMetadata loadFromResource(String imageResourcePath) throws IOException {

        if (imageResourcePath == null) {
            throw new IllegalArgumentException("Image resource path must not be null.");
        }

        String metadataResourcePath = createMetadataPath(imageResourcePath);

        try (InputStream inputStream = MapMetadataLoader.class.getResourceAsStream(metadataResourcePath)) {

            if (inputStream == null) {
                return null;
            }

            return read(inputStream);
        }
    }

    /**
     * Loads optional sidecar metadata located next to a map image file.
     *
     * <p>For an image named {@code my_map.jpg}, this method looks for
     * {@code my_map.metadata.json} in the same directory.</p>
     *
     * @param imageFile map image file
     * @return parsed metadata, or {@code null} if the matching file does not
     *         exist
     * @throws IllegalArgumentException if {@code imageFile} is {@code null}
     * @throws IOException if the metadata file exists but cannot be read or
     *                     parsed
     */
    public static MapFileMetadata loadForImageFile(File imageFile) throws IOException {

        if (imageFile == null) {
            throw new IllegalArgumentException("Image file must not be null.");
        }

        File metadataFile = createMetadataFile(imageFile);

        if (!metadataFile.isFile()) {
            return null;
        }

        return OBJECT_MAPPER.readValue(metadataFile, MapFileMetadata.class);

    }

    /**
     * Reads metadata from an input stream.
     *
     * @param inputStream input stream containing JSON metadata
     * @return parsed file metadata
     * @throws IOException if the JSON cannot be read or parsed
     */
    private static MapFileMetadata read(InputStream inputStream) throws IOException {

        return OBJECT_MAPPER.readValue(inputStream, MapFileMetadata.class);
    }

    /**
     * Creates the expected metadata resource path for a map image resource.
     *
     * @param imageResourcePath image resource path
     * @return matching metadata resource path
     */
    private static String createMetadataPath(String imageResourcePath) {

        int extensionIndex = imageResourcePath.lastIndexOf('.');

        if (extensionIndex < 0) {
            return imageResourcePath + METADATA_EXTENSION;
        }

        return imageResourcePath.substring(0, extensionIndex)
                + METADATA_EXTENSION;
    }

    /**
     * Creates the expected sidecar metadata file for a map image file.
     *
     * @param imageFile map image file
     * @return matching sidecar metadata file
     */
    private static File createMetadataFile(File imageFile) {

        String fileName = imageFile.getName();
        int extensionIndex = fileName.lastIndexOf('.');

        String metadataFileName = extensionIndex < 0
                ? fileName + METADATA_EXTENSION
                : fileName.substring(0, extensionIndex) + METADATA_EXTENSION;

        return new File(imageFile.getParentFile(), metadataFileName);
    }
}
