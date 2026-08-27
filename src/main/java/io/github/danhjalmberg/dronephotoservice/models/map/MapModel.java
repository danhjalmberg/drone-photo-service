package io.github.danhjalmberg.dronephotoservice.models.map;

import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;
import io.github.danhjalmberg.dronephotoservice.settings.ModelSettings;
import io.github.danhjalmberg.dronephotoservice.support.ImageUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * Owns loaded map images, runtime metadata, and spatial coordinate
 * conversions.
 *
 * <p>A loaded raster is retained as the original image, cropped to a square
 * world image used by simulation and camera operations, and resampled as a
 * display image used by the graphical view. The model converts among world
 * meters, world-image pixels, and display pixels.</p>
 *
 * <p>Map loading is transactional: decoding, image processing, metadata
 * validation, and display-image creation complete before the active map state
 * is replaced. A failed replacement therefore preserves the previously loaded
 * map.</p>
 *
 * @author Dan Hjälmberg
 */
public class MapModel {

    private String mapFilePath;
    private String mapFileName;
    private BufferedImage mapImage;
    private BufferedImage mapImageCropped;
    private BufferedImage mapImageResampled;
    private MapMetadata metadata;

    /**
     * Creates an empty map model with no active map or metadata.
     */
    public MapModel() {
    }

    /**
     * Loads a classpath map resource and applies optional matching sidecar
     * metadata.
     *
     * <p>For example:</p>
     * <pre>
     * /maps/demo_map.jpg
     * /maps/demo_map.metadata.json
     * </pre>
     *
     * <p>The active map remains unchanged if loading fails. This method does
     * not close the caller-owned image stream.</p>
     *
     * @param inputStream image resource input stream
     * @param mapFileName name of the loaded map file
     * @param imageResourcePath classpath path of the map image resource
     * @throws MapLoadException if the map image or its metadata cannot be
     *                          read, validated, or processed
     * @throws IllegalArgumentException if {@code imageResourcePath} is
     *                                  {@code null}
     */
    public void loadMap(
            InputStream inputStream,
            String mapFileName,
            String imageResourcePath) throws MapLoadException {

        BufferedImage loadedImage = readMapImage(inputStream);

        final MapFileMetadata fileMetadata;

        try {
            fileMetadata = MapMetadataLoader.loadFromResource(imageResourcePath);

        } catch (IOException exception) {
            throw new MapLoadException("The bundled map metadata could not be read.", exception);
        }

        processLoadedMap(
                loadedImage,
                mapFileName,
                imageResourcePath,
                fileMetadata);
    }

    /**
     * Loads a map image from an input stream without sidecar metadata.
     *
     * <p>The configured default scale is used. The active map remains
     * unchanged if loading fails, and this method does not close the
     * caller-owned stream.</p>
     *
     * @param inputStream map image input stream
     * @param mapFileName name of the loaded map file
     * @throws MapLoadException if the map image cannot be read or processed
     */
    public void loadMap(
            InputStream inputStream,
            String mapFileName) throws MapLoadException {

        BufferedImage loadedImage =
                readMapImage(inputStream);

        processLoadedMap(
                loadedImage,
                mapFileName,
                mapFileName,
                null);
    }

    /**
     * Loads a map image file and applies optional sidecar metadata.
     *
     * <p>For an image named {@code user_map.jpg}, the expected sidecar is
     * {@code user_map.metadata.json}. The active map remains unchanged if
     * loading fails.</p>
     *
     * @param mapFile map image file
     * @throws MapLoadException if the map image or its metadata cannot be
     *                          read, validated, or processed
     */
    public void loadMap(File mapFile)
            throws MapLoadException {

        validateMapFile(mapFile);

        final BufferedImage loadedImage;

        try {
            loadedImage = ImageIO.read(mapFile);

        } catch (IOException exception) {
            throw new MapLoadException("The selected map image could not be read.", exception);
        }

        validateDecodedImage(loadedImage);

        final MapFileMetadata fileMetadata;

        try {
            fileMetadata = MapMetadataLoader.loadForImageFile(mapFile);

        } catch (IOException exception) {
            throw new MapLoadException("The map metadata file could not be read or parsed.", exception);
        }

        processLoadedMap(
                loadedImage,
                mapFile.getName(),
                mapFile.getAbsolutePath(),
                fileMetadata);
    }

    /**
     * Reads and decodes a map image from an input stream.
     *
     * @param inputStream image input stream
     * @return decoded image
     * @throws MapLoadException if the stream is null, unreadable, or does not
     *                          contain a supported image
     */
    private BufferedImage readMapImage(
            InputStream inputStream)
            throws MapLoadException {

        if (inputStream == null) {
            throw new MapLoadException("The map image input stream is not available.");
        }

        final BufferedImage loadedImage;

        try {
            loadedImage = ImageIO.read(inputStream);

        } catch (IOException exception) {
            throw new MapLoadException("The map image could not be read.", exception);
        }

        validateDecodedImage(loadedImage);

        return loadedImage;
    }

    /**
     * Verifies that image decoding produced an image.
     *
     * @param loadedImage decoded image
     * @throws MapLoadException if the data is not a supported image
     */
    private void validateDecodedImage(BufferedImage loadedImage) throws MapLoadException {

        if (loadedImage == null) {
            throw new MapLoadException("The selected file is not a supported image.");
        }
    }

    /**
     * Validates a map image file before reading it.
     *
     * @param mapFile map image file
     * @throws MapLoadException if the file is null, missing, not a regular file, or unreadable
     */
    private void validateMapFile(File mapFile) throws MapLoadException {

        if (mapFile == null) {
            throw new MapLoadException("No map image file was selected.");
        }

        if (!mapFile.isFile()) {
            throw new MapLoadException("The selected map image does not exist or is not a file.");
        }

        if (!mapFile.canRead()) {
            throw new MapLoadException("The selected map image cannot be read.");
        }
    }

    /**
     * Processes a decoded image and optional metadata as one transaction.
     *
     * <p>Active state is replaced only after every preparation and validation
     * step succeeds.</p>
     *
     * @param loadedImage decoded source image
     * @param fileName name of the loaded map file
     * @param filePath path or resource identifier of the loaded map file
     * @param fileMetadata optional parsed metadata
     * @throws MapLoadException if image processing or metadata validation fails
     */
    private void processLoadedMap(
            BufferedImage loadedImage,
            String fileName,
            String filePath,
            MapFileMetadata fileMetadata)
            throws MapLoadException {

        BufferedImage croppedImage = cropMapImage(loadedImage);

        validateWorldImageSize(croppedImage);

        BufferedImage resampledImage = resampleMapImage(croppedImage);

        MapMetadata newMetadata = createDefaultMetadata(
                        fileName,
                        loadedImage,
                        croppedImage,
                        resampledImage);

        try {
            newMetadata.applyFileMetadata(fileMetadata);

        } catch (IllegalArgumentException exception) {
            throw new MapLoadException(
                    "The map metadata contains invalid values: " + exception.getMessage(), exception);
        }

        // Commit the prepared state only after the transaction succeeds.
        mapFileName = fileName;
        mapFilePath = filePath;
        mapImage = loadedImage;
        mapImageCropped = croppedImage;
        mapImageResampled = resampledImage;
        metadata = newMetadata;
    }

    /**
     * Crops a decoded map image to a centered square.
     *
     * @param loadedImage decoded source image
     * @return cropped square image
     * @throws MapLoadException if cropping fails
     */
    private BufferedImage cropMapImage(
            BufferedImage loadedImage) throws MapLoadException {

        try {
            return ImageUtils.cropImageToSquare(loadedImage);

        } catch (RuntimeException exception) {
            throw new MapLoadException("The map image could not be cropped.", exception);
        }
    }

    /**
     * Resizes the cropped map image to the configured display dimensions.
     *
     * @param croppedImage cropped world image
     * @return resampled display image
     * @throws MapLoadException if resampling fails
     */
    private BufferedImage resampleMapImage(
            BufferedImage croppedImage) throws MapLoadException {

        try {
            return ImageUtils.resampleImage(
                    croppedImage,
                    ModelSettings.MAP_IMAGE_RESAMPLED_WIDTH,
                    ModelSettings.MAP_IMAGE_RESAMPLED_HEIGHT,
                    ModelSettings.MAP_INTERPOLATION);

        } catch (RuntimeException exception) {
            throw new MapLoadException("The map image could not be resized.", exception);
        }
    }

    /**
     * Reports whether complete map state is available.
     *
     * @return {@code true} if a map is loaded; otherwise {@code false}
     */
    public boolean hasMapLoaded() {
        return metadata != null;
    }

    /**
     * Creates default runtime metadata from prepared map images.
     *
     * @param mapFileName name of the loaded map file
     * @param originalImage original loaded image
     * @param croppedImage cropped world image
     * @param resampledImage resampled display image
     * @return metadata using the configured default scale
     */
    private MapMetadata createDefaultMetadata(
            String mapFileName,
            BufferedImage originalImage,
            BufferedImage croppedImage,
            BufferedImage resampledImage) {

        return new MapMetadata(
                mapFileName,
                originalImage.getWidth(),
                originalImage.getHeight(),
                croppedImage.getWidth(),
                croppedImage.getHeight(),
                resampledImage.getWidth(),
                resampledImage.getHeight(),
                ModelSettings.DEFAULT_MAP_METERS_PER_PIXEL,
                MapMetadata.ScaleSource.DEFAULT);
    }

    /**
     * Returns the mutable runtime metadata for the active map.
     *
     * @return active map metadata, or {@code null} if no map is loaded
     */
    public MapMetadata getMetadata() {
        return metadata;
    }

    /**
     * Returns the attribution text for the active map.
     *
     * @return attribution text, or {@code null} if unavailable
     * @throws IllegalStateException if no map is loaded
     */
    public String getMapAttribution() {
        requireMetadata();
        return metadata.getAttribution();
    }

    /**
     * Replaces the active map scale with a manual value.
     *
     * @param metersPerPixel positive, finite meters represented by one
     *                       world-image pixel
     * @throws IllegalStateException if no map is loaded
     * @throws IllegalArgumentException if {@code metersPerPixel} is not
     *                                  positive and finite
     */
    public void setMapMetersPerPixel(double metersPerPixel) {
        requireMetadata();
        metadata.setManualMetersPerPixel(metersPerPixel);
    }

    /**
     * Returns the active map scale.
     *
     * @return meters per world-image pixel
     * @throws IllegalStateException if no map is loaded
     */
    public double getMapMetersPerPixel() {
        requireMetadata();
        return metadata.getMetersPerPixel();
    }

    /**
     * Converts a signed world-image pixel distance to meters.
     *
     * @param pixels signed distance in world-image pixels
     * @return corresponding signed distance in meters
     * @throws IllegalStateException if no map is loaded
     */
    public double pixelsToMeters(double pixels) {
        requireMetadata();
        return metadata.pixelsToMeters(pixels);
    }

    /**
     * Converts a signed distance in meters to world-image pixels.
     *
     * @param meters signed distance in meters
     * @return corresponding signed distance in world-image pixels
     * @throws IllegalStateException if no map is loaded
     */
    public double metersToPixels(double meters) {
        requireMetadata();
        return metadata.metersToPixels(meters);
    }

    /**
     * Converts a position from world-image pixels to world meters.
     *
     * @param worldPointPixels position in world-image pixels
     * @return corresponding position in world meters
     * @throws IllegalStateException if no map is loaded
     * @throws NullPointerException if {@code worldPointPixels} is {@code null}
     */
    public Vector2D worldPixelsToMeters(Vector2D worldPointPixels) {
        requireMetadata();

        return new Vector2D(
                metadata.pixelsToMeters(worldPointPixels.getX()),
                metadata.pixelsToMeters(worldPointPixels.getY()));
    }

    /**
     * Converts a position from world meters to world-image pixels.
     *
     * @param worldPointMeters position in world meters
     * @return corresponding position in world-image pixels
     * @throws IllegalStateException if no map is loaded
     * @throws NullPointerException if {@code worldPointMeters} is {@code null}
     */
    public Vector2D worldMetersToPixels(Vector2D worldPointMeters) {

        requireMetadata();

        return new Vector2D(
                metadata.metersToPixels(worldPointMeters.getX()),
                metadata.metersToPixels(worldPointMeters.getY()));
    }

    /**
     * Verifies that runtime metadata is available for a loaded map.
     *
     * @throws IllegalStateException if no map is loaded
     */
    private void requireMetadata() {
        if (metadata == null) {
            throw new IllegalStateException("No map metadata is available because no map is loaded.");
        }
    }

    /**
     * Returns the active map file name.
     *
     * @return map file name, or {@code "No map loaded"} if unavailable
     */
    public String getMapFileName() {
        return mapFileName == null ? "No map loaded" : mapFileName;
    }

    /**
     * Returns the active map file path or resource identifier.
     *
     * @return map path or resource identifier, or {@code "No map loaded"} if
     *         unavailable
     */
    public String getMapFilePath() {

        return mapFilePath == null ? "No map loaded" : mapFilePath;
    }

    /**
     * Returns the original loaded map image.
     *
     * @return original image, or {@code null} if no map is loaded
     */
    public BufferedImage getMapImage() {
        return mapImage;
    }

    /**
     * Returns the cropped world image used by simulation and camera operations.
     *
     * @return cropped world image, or {@code null} if no map is loaded
     */
    public BufferedImage getMapImageCropped() {
        return mapImageCropped;
    }

    /**
     * Returns the resampled image used by the graphical view.
     *
     * @return display image, or {@code null} if no map is loaded
     */
    public BufferedImage getMapImageResampled() {
        return mapImageResampled;
    }

    /**
     * Returns the cropped world-image width.
     *
     * @return world-image width in pixels
     * @throws NullPointerException if no map is loaded
     */
    public int getWorldWidth() {
        return mapImageCropped.getWidth();
    }

    /**
     * Returns the cropped world-image height.
     *
     * @return world-image height in pixels
     * @throws NullPointerException if no map is loaded
     */
    public int getWorldHeight() {
        return mapImageCropped.getHeight();
    }

    /**
     * Returns the display-image width.
     *
     * @return display-image width in pixels
     * @throws NullPointerException if no map is loaded
     */
    public int getDisplayWidth() {
        return mapImageResampled.getWidth();
    }

    /**
     * Returns the display-image height.
     *
     * @return display-image height in pixels
     * @throws NullPointerException if no map is loaded
     */
    public int getDisplayHeight() {
        return mapImageResampled.getHeight();
    }

    /**
     * Converts a position from world-image pixels to display pixels.
     *
     * @param worldPoint position in world-image pixels
     * @return corresponding position in display pixels
     * @throws NullPointerException if no map is loaded or
     *                              {@code worldPoint} is {@code null}
     */
    public Vector2D worldToDisplay(Vector2D worldPoint) {

        double scaleX = (double) getDisplayWidth() / getWorldWidth();
        double scaleY = (double) getDisplayHeight() / getWorldHeight();

        return new Vector2D(
                worldPoint.getX() * scaleX,
                worldPoint.getY() * scaleY);
    }

    /**
     * Converts a position from display pixels to world-image pixels.
     *
     * @param displayPoint position in display pixels
     * @return corresponding position in world-image pixels
     * @throws NullPointerException if no map is loaded or
     *                              {@code displayPoint} is {@code null}
     */
    public Vector2D displayToWorld(Vector2D displayPoint) {

        double scaleX = (double) getWorldWidth() / getDisplayWidth();
        double scaleY = (double) getWorldHeight() / getDisplayHeight();

        return new Vector2D(
                displayPoint.getX() * scaleX,
                displayPoint.getY() * scaleY);
    }

    /**
     * Converts a position from world meters directly to display pixels.
     *
     * @param worldPointMeters position in world meters
     * @return corresponding position in display pixels
     * @throws IllegalStateException if no map is loaded
     * @throws NullPointerException if {@code worldPointMeters} is {@code null}
     */
    public Vector2D worldMetersToDisplay(Vector2D worldPointMeters) {
        return worldToDisplay(worldMetersToPixels(worldPointMeters));
    }

    /**
     * Converts a position from display pixels directly to world meters.
     *
     * @param displayPoint position in display pixels
     * @return corresponding position in world meters
     * @throws NullPointerException if no map is loaded or
     *                              {@code displayPoint} is {@code null}
     */
    public Vector2D displayToWorldMeters(Vector2D displayPoint) {
        return worldPixelsToMeters(displayToWorld(displayPoint));
    }

    /**
     * Replaces the active map's optional georeference information with manual
     * values.
     *
     * @param upperLeftLatitude latitude of the upper-left world-image pixel,
     *                          or {@code null}
     * @param upperLeftLongitude longitude of the upper-left world-image pixel,
     *                           or {@code null}
     * @param coordinateReferenceSystem coordinate reference system, or
     *                                  {@code null}
     * @throws IllegalStateException if no map is loaded
     * @throws IllegalArgumentException if a supplied latitude or longitude is
     *                                  non-finite or outside its valid range
     */
    public void setManualGeoReference(
            Double upperLeftLatitude,
            Double upperLeftLongitude,
            String coordinateReferenceSystem) {

        requireMetadata();

        metadata.setManualGeoReference(
                upperLeftLatitude,
                upperLeftLongitude,
                coordinateReferenceSystem);
    }

    /**
     * Creates a random position in world meters whose camera footprint remains
     * inside the cropped world image.
     *
     * @param random random-number generator
     * @return random camera-safe position in world meters
     * @throws NullPointerException if no map is loaded or {@code random} is
     *                              {@code null}
     */
    public Vector2D createRandomSafeWorldMeterPosition(Random random) {
        return worldPixelsToMeters(createRandomSafeWorldPixelPosition(random));
    }

    /**
     * Creates a random world-image pixel position whose camera footprint
     * remains inside the cropped image.
     *
     * @param random random-number generator
     * @return random camera-safe position in world-image pixels
     * @throws NullPointerException if no map is loaded or {@code random} is
     *                              {@code null}
     * @throws IllegalStateException if the loaded world image cannot contain
     *                               the configured camera-safe margins
     */
    public Vector2D createRandomSafeWorldPixelPosition(Random random) {

        int width = getWorldWidth();
        int height = getWorldHeight();

        int marginX = ModelSettings.CAMERA_SAFE_WORLD_PIXEL_MARGIN_X;
        int marginY = ModelSettings.CAMERA_SAFE_WORLD_PIXEL_MARGIN_Y;

        validateWorldMargins(width, height, marginX, marginY);

        int x = marginX + random.nextInt(width - 2 * marginX);
        int y = marginY + random.nextInt(height - 2 * marginY);

        return new Vector2D(x, y);
    }

    /**
     * Verifies that the cropped world image is large enough to contain the
     * configured camera-safe margins.
     *
     * @param croppedImage cropped world image
     * @throws MapLoadException if the image is too small for simulation use
     */
    private void validateWorldImageSize(BufferedImage croppedImage) throws MapLoadException {

        int minimumWidth = 2 * ModelSettings.CAMERA_SAFE_WORLD_PIXEL_MARGIN_X + 1;
        int minimumHeight = 2 * ModelSettings.CAMERA_SAFE_WORLD_PIXEL_MARGIN_Y + 1;

        if (croppedImage.getWidth() < minimumWidth || croppedImage.getHeight() < minimumHeight) {

            throw new MapLoadException(
                    "The map image is too small for the configured camera resolution. "
                            + "The cropped map must be at least "
                            + minimumWidth
                            + " × "
                            + minimumHeight
                            + " pixels.");
        }
    }

    /**
     * Validates camera-safe margins against world-image dimensions.
     *
     * @param width world-image width in pixels
     * @param height world-image height in pixels
     * @param marginX horizontal camera-safe margin in pixels
     * @param marginY vertical camera-safe margin in pixels
     * @throws IllegalStateException if the dimensions cannot contain the
     *                               margins
     */
    private void validateWorldMargins(
            int width,
            int height,
            int marginX,
            int marginY) {

        if (width <= 2 * marginX || height <= 2 * marginY) {
            throw new IllegalStateException("Map image is too small for the configured camera resolution.");
        }
    }
}
