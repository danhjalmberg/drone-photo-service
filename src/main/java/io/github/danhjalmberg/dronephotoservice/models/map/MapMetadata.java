package io.github.danhjalmberg.dronephotoservice.models.map;

/**
 * Represents runtime metadata for the currently loaded and processed map.
 *
 * <p>The metadata combines dimensions derived from the original, cropped
 * world, and display images with optional descriptive and georeference values.
 * Map scale is expressed as meters per world-image pixel and remains separate
 * from display-image scaling.</p>
 *
 * <p>Scale and georeference values record whether they originated from
 * defaults, file metadata, or a manual override. This object belongs to one
 * loaded map and is not a global application setting.</p>
 *
 * @author Dan Hjälmberg
 */
public class MapMetadata {

    private final double MIN_LATITUDE = -90.0;
    private final double MAX_LATITUDE = 90.0;
    private final double MIN_LONGITUDE = -180.0;
    private final double MAX_LONGITUDE = 180.0;

    /**
     * Identifies the source of the active georeference information.
     */
    public enum GeoReferenceSource {
        /** No georeference information is available. */
        UNKNOWN,

        /** Georeference information was supplied manually. */
        MANUAL,

        /** Georeference information was loaded from file metadata. */
        FILE_METADATA
    }

    /**
     * Identifies the source of the active map scale.
     */
    public enum ScaleSource {
        /** The configured default scale is active. */
        DEFAULT,

        /** A manually supplied scale is active. */
        MANUAL,

        /** A scale loaded from file metadata is active. */
        FILE_METADATA
    }

    private final String fileName;
    private final int originalWidthPixels;
    private final int originalHeightPixels;
    private final int worldWidthPixels;
    private final int worldHeightPixels;
    private final int displayWidthPixels;
    private final int displayHeightPixels;

    private double metersPerPixel;
    private ScaleSource scaleSource;

    private Double upperLeftLatitude;
    private Double upperLeftLongitude;
    private String coordinateReferenceSystem;
    private GeoReferenceSource geoReferenceSource = GeoReferenceSource.UNKNOWN;

    private String title;
    private String source;
    private String license;
    private String attribution;
    private String derivedFrom;
    private String modified;

    /**
     * Creates runtime metadata from loaded and processed map dimensions.
     *
     * @param fileName loaded map file name
     * @param originalWidthPixels width of the original image in pixels
     * @param originalHeightPixels height of the original image in pixels
     * @param worldWidthPixels width of the cropped world image in pixels
     * @param worldHeightPixels height of the cropped world image in pixels
     * @param displayWidthPixels width of the display image in pixels
     * @param displayHeightPixels height of the display image in pixels
     * @param metersPerPixel positive, finite meters represented by one
     *                       world-image pixel
     * @param scaleSource source of the initial scale value
     * @throws IllegalArgumentException if {@code metersPerPixel} is not
     *                                  positive and finite
     */
    public MapMetadata(
            String fileName,
            int originalWidthPixels,
            int originalHeightPixels,
            int worldWidthPixels,
            int worldHeightPixels,
            int displayWidthPixels,
            int displayHeightPixels,
            double metersPerPixel,
            ScaleSource scaleSource) {

        validateMetersPerPixel(metersPerPixel);

        this.fileName = fileName;
        this.originalWidthPixels = originalWidthPixels;
        this.originalHeightPixels = originalHeightPixels;
        this.worldWidthPixels = worldWidthPixels;
        this.worldHeightPixels = worldHeightPixels;
        this.displayWidthPixels = displayWidthPixels;
        this.displayHeightPixels = displayHeightPixels;
        this.metersPerPixel = metersPerPixel;
        this.scaleSource = scaleSource;
    }

    /**
     * Returns the loaded map file name.
     *
     * @return map file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the original image width.
     *
     * @return original image width in pixels
     */
    public int getOriginalWidthPixels() {
        return originalWidthPixels;
    }

    /**
     * Returns the original image height.
     *
     * @return original image height in pixels
     */
    public int getOriginalHeightPixels() {
        return originalHeightPixels;
    }

    /**
     * Returns the cropped world-image width.
     *
     * @return world-image width in pixels
     */
    public int getWorldWidthPixels() {
        return worldWidthPixels;
    }

    /**
     * Returns the cropped world-image height.
     *
     * @return world-image height in pixels
     */
    public int getWorldHeightPixels() {
        return worldHeightPixels;
    }

    /**
     * Returns the display-image width.
     *
     * @return display-image width in pixels
     */
    public int getDisplayWidthPixels() {
        return displayWidthPixels;
    }

    /**
     * Returns the display-image height.
     *
     * @return display-image height in pixels
     */
    public int getDisplayHeightPixels() {
        return displayHeightPixels;
    }

    /**
     * Returns the number of meters represented by one world-image pixel.
     *
     * @return meters per world-image pixel
     */
    public double getMetersPerPixel() {
        return metersPerPixel;
    }

    /**
     * Replaces the active map scale with a manual value.
     *
     * @param metersPerPixel positive, finite meters represented by one
     *                       world-image pixel
     * @throws IllegalArgumentException if {@code metersPerPixel} is not
     *                                  positive and finite
     */
    public void setManualMetersPerPixel(double metersPerPixel) {

        validateMetersPerPixel(metersPerPixel);
        this.metersPerPixel = metersPerPixel;
        this.scaleSource = ScaleSource.MANUAL;
    }

    /**
     * Returns the source of the active scale value.
     *
     * @return active scale source
     */
    public ScaleSource getScaleSource() {
        return scaleSource;
    }

    /**
     * Returns the physical width represented by the cropped world image.
     *
     * @return world width in meters
     */
    public double getWorldWidthMeters() {
        return worldWidthPixels * metersPerPixel;
    }

    /**
     * Returns the physical height represented by the cropped world image.
     *
     * @return world height in meters
     */
    public double getWorldHeightMeters() {
        return worldHeightPixels * metersPerPixel;
    }

    /**
     * Converts world-image pixels to meters.
     *
     * @param pixels signed distance in world-image pixels
     * @return corresponding signed distance in meters
     */
    public double pixelsToMeters(double pixels) {
        return pixels * metersPerPixel;
    }

    /**
     * Converts meters to world-image pixels.
     *
     * @param meters signed distance in meters
     * @return corresponding signed distance in world-image pixels
     */
    public double metersToPixels(double meters) {
        return meters / metersPerPixel;
    }

    /**
     * Returns the latitude of the upper-left world-image pixel.
     *
     * @return upper-left latitude, or {@code null} if unknown
     */
    public Double getUpperLeftLatitude() {
        return upperLeftLatitude;
    }

    /**
     * Returns the longitude of the upper-left world-image pixel.
     *
     * @return upper-left longitude, or {@code null} if unknown
     */
    public Double getUpperLeftLongitude() {
        return upperLeftLongitude;
    }

    /**
     * Returns the coordinate reference system associated with the optional
     * georeference values.
     *
     * @return coordinate reference system, or {@code null} if unknown
     */
    public String getCoordinateReferenceSystem() {
        return coordinateReferenceSystem;
    }

    /**
     * Returns the source of the active georeference information.
     *
     * @return active georeference source
     */
    public GeoReferenceSource getGeoReferenceSource() {
        return geoReferenceSource;
    }

    /**
     * Returns the human-readable map title.
     *
     * @return map title, or {@code null} if unavailable
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the map data source.
     *
     * @return map data source, or {@code null} if unavailable
     */
    public String getSource() {
        return source;
    }

    /**
     * Returns the map license.
     *
     * @return map license, or {@code null} if unavailable
     */
    public String getLicense() {
        return license;
    }

    /**
     * Returns the attribution text that should be displayed with the map.
     *
     * @return attribution text, or {@code null} if unavailable
     */
    public String getAttribution() {
        return attribution;
    }

    /**
     * Returns a description of the dataset from which the map was derived.
     *
     * @return source-dataset description, or {@code null} if unavailable
     */
    public String getDerivedFrom() {
        return derivedFrom;
    }

    /**
     * Returns a description of modifications made to the source image.
     *
     * @return modification description, or {@code null} if unavailable
     */
    public String getModified() {
        return modified;
    }

    /**
     * Replaces the optional georeference information with manually supplied
     * values.
     *
     * <p>Coordinates may be {@code null}. Blank coordinate-reference-system
     * text is normalized to {@code null}. A successful call records the source
     * as {@link GeoReferenceSource#MANUAL}, including when all values are
     * absent.</p>
     *
     * @param upperLeftLatitude latitude of the upper-left world-image pixel,
     *                          or {@code null}
     * @param upperLeftLongitude longitude of the upper-left world-image pixel,
     *                           or {@code null}
     * @param coordinateReferenceSystem coordinate reference system, or
     *                                  {@code null}
     * @throws IllegalArgumentException if a supplied latitude or longitude is
     *                                  non-finite or outside its valid range
     */
    public void setManualGeoReference(
            Double upperLeftLatitude,
            Double upperLeftLongitude,
            String coordinateReferenceSystem) {

        validateLatitude(upperLeftLatitude);
        validateLongitude(upperLeftLongitude);

        this.upperLeftLatitude = upperLeftLatitude;
        this.upperLeftLongitude = upperLeftLongitude;
        this.coordinateReferenceSystem =
                coordinateReferenceSystem == null || coordinateReferenceSystem.isBlank()
                        ? null
                        : coordinateReferenceSystem.trim();

        this.geoReferenceSource = GeoReferenceSource.MANUAL;
    }

    /**
     * Validates a meters-per-pixel value.
     *
     * @param metersPerPixel value to validate
     * @throws IllegalArgumentException if the value is not positive and finite
     */
    private void validateMetersPerPixel(double metersPerPixel) {

        if (!Double.isFinite(metersPerPixel) || metersPerPixel <= 0.0) {
            throw new IllegalArgumentException("Meters per pixel must be a positive finite value.");
        }
    }

    /**
     * Validates the latitude value.
     *
     * @param latitude value to validate, or {@code null}
     * @throws IllegalArgumentException if the value is non-finite or outside
     *                                  the inclusive range {@code [MIN_LATITUDE, MAX_LATITUDE]}
     */
    private void validateLatitude(Double latitude) {

        if (latitude == null) {
            return;
        }

        if (!Double.isFinite(latitude) || latitude < MIN_LATITUDE || latitude > MAX_LATITUDE) {

            throw new IllegalArgumentException(
                    "Latitude must be a finite value between " + MIN_LATITUDE + " and " + MAX_LATITUDE + ".");
        }
    }

    /**
     * Validates the longitude value.
     *
     * @param longitude value to validate, or {@code null}
     * @throws IllegalArgumentException if the value is non-finite or outside
     *                                  the inclusive range {@code [MIN_LONGITUDE, MAX_LONGITUDE]}
     */
    private void validateLongitude(Double longitude) {

        if (longitude == null) {
            return;
        }

        if (!Double.isFinite(longitude) || longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE) {

            throw new IllegalArgumentException(
                    "Longitude must be a finite value between " + MIN_LONGITUDE + " and " + MAX_LONGITUDE + ".");
        }
    }

    /**
     * Applies values parsed from JSON sidecar metadata.
     *
     * <p>A {@code null} metadata object is ignored. Descriptive values replace
     * the current descriptive metadata after optional text is normalized.
     * Scale is replaced only when supplied, and georeference information is
     * replaced when at least one georeference value is supplied.</p>
     *
     * <p>All supplied numeric values are validated before any state is
     * changed, so invalid file metadata leaves the existing metadata
     * unchanged.</p>
     *
     * @param metadataFile parsed file metadata, or {@code null}
     * @throws IllegalArgumentException if an invalid scale, latitude, or
     *                                  longitude is supplied
     */
    public void applyFileMetadata(MapFileMetadata metadataFile) {

        if (metadataFile == null) {
            return;
        }

        Double fileScale = metadataFile.getMetersPerPixel();
        Double fileLatitude = metadataFile.getUpperLeftLatitude();
        Double fileLongitude = metadataFile.getUpperLeftLongitude();
        String fileCoordinateReferenceSystem = clean(metadataFile.getCoordinateReferenceSystem());

        if (fileScale != null) {
            validateMetersPerPixel(fileScale);
        }

        validateLatitude(fileLatitude);
        validateLongitude(fileLongitude);

        // Assign only after all validation succeeds.
        title = clean(metadataFile.getTitle());
        source = clean(metadataFile.getSource());
        license = clean(metadataFile.getLicense());
        attribution = clean(metadataFile.getAttribution());
        derivedFrom = clean(metadataFile.getDerivedFrom());
        modified = clean(metadataFile.getModified());

        if (fileScale != null) {
            metersPerPixel = fileScale;
            scaleSource = ScaleSource.FILE_METADATA;
        }

        boolean hasGeoReference = fileLatitude != null
                || fileLongitude != null
                || fileCoordinateReferenceSystem != null;

        if (hasGeoReference) {
            upperLeftLatitude = fileLatitude;
            upperLeftLongitude = fileLongitude;
            coordinateReferenceSystem =  fileCoordinateReferenceSystem;
            geoReferenceSource = GeoReferenceSource.FILE_METADATA;
        }
    }

    /**
     * Cleans optional text values by trimming whitespace and converting blank
     * strings to null.
     *
     * @param value text value
     * @return trimmed value, or {@code null} if the value is null or blank
     */
    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
