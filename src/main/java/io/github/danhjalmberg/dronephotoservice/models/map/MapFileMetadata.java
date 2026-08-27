package io.github.danhjalmberg.dronephotoservice.models.map;

/**
 * Represents metadata parsed from an optional JSON sidecar file for a map
 * image.
 *
 * <p>This mutable data-transfer object mirrors the external JSON structure for
 * Jackson deserialization. It contains only file-supplied values; dimensions
 * and other image-derived values belong to runtime {@link MapMetadata}.</p>
 *
 * <p>Optional text values are trimmed when assigned, and blank text is stored
 * as {@code null}.</p>
 *
 * @author Dan Hjälmberg
 */
public class MapFileMetadata {

    private int schemaVersion;
    private String title;
    private String source;
    private String license;
    private String attribution;
    private Double metersPerPixel;
    private String coordinateReferenceSystem;
    private Double upperLeftLatitude;
    private Double upperLeftLongitude;
    private String derivedFrom;
    private String modified;

    /**
     * Creates empty file metadata for Jackson deserialization.
     */
    public MapFileMetadata() {
    }

    /**
     * Returns the metadata schema version.
     *
     * @return schema version
     */
    public int getSchemaVersion() {
        return schemaVersion;
    }

    /**
     * Sets the metadata schema version.
     *
     * @param schemaVersion schema version
     */
    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    /**
     * Returns the human-readable map title.
     *
     * @return map title, or {@code null} if unspecified
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the human-readable map title after normalizing optional text.
     *
     * @param title map title; blank text is stored as {@code null}
     */
    public void setTitle(String title) {
        this.title = clean(title);
    }

    /**
     * Returns the map data source.
     *
     * @return map data source, or {@code null} if unspecified
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the map data source after normalizing optional text.
     *
     * @param source map data source; blank text is stored as {@code null}
     */
    public void setSource(String source) {
        this.source = clean(source);
    }

    /**
     * Returns the map license.
     *
     * @return map license, or {@code null} if unspecified
     */
    public String getLicense() {
        return license;
    }

    /**
     * Sets the map license after normalizing optional text.
     *
     * @param license map license; blank text is stored as {@code null}
     */
    public void setLicense(String license) {
        this.license = clean(license);
    }

    /**
     * Returns the attribution text that should be displayed with the map.
     *
     * @return attribution text, or {@code null} if unspecified
     */
    public String getAttribution() {
        return attribution;
    }

    /**
     * Sets the map attribution after normalizing optional text.
     *
     * @param attribution attribution text; blank text is stored as
     *                    {@code null}
     */
    public void setAttribution(String attribution) {
        this.attribution = clean(attribution);
    }

    /**
     * Returns the scale in meters per world-image pixel.
     *
     * @return meters per world-image pixel, or {@code null} if unspecified
     */
    public Double getMetersPerPixel() {
        return metersPerPixel;
    }

    /**
     * Sets the scale in meters per world-image pixel.
     *
     * @param metersPerPixel meters per world-image pixel, or {@code null}
     */
    public void setMetersPerPixel(Double metersPerPixel) {
        this.metersPerPixel = metersPerPixel;
    }

    /**
     * Returns the coordinate reference system description.
     *
     * @return coordinate reference system, or {@code null} if unspecified
     */
    public String getCoordinateReferenceSystem() {
        return coordinateReferenceSystem;
    }

    /**
     * Sets the coordinate reference system after normalizing optional text.
     *
     * @param coordinateReferenceSystem coordinate reference system; blank text
     *                                  is stored as {@code null}
     */
    public void setCoordinateReferenceSystem(String coordinateReferenceSystem) {
        this.coordinateReferenceSystem = clean(coordinateReferenceSystem);
    }

    /**
     * Returns the latitude of the upper-left world-image pixel.
     *
     * @return latitude, or {@code null} if unspecified
     */
    public Double getUpperLeftLatitude() {
        return upperLeftLatitude;
    }

    /**
     * Sets the latitude of the upper-left world-image pixel.
     *
     * @param upperLeftLatitude latitude, or {@code null}
     */
    public void setUpperLeftLatitude(Double upperLeftLatitude) {
        this.upperLeftLatitude = upperLeftLatitude;
    }

    /**
     * Returns the longitude of the upper-left world-image pixel.
     *
     * @return longitude, or {@code null} if unspecified
     */
    public Double getUpperLeftLongitude() {
        return upperLeftLongitude;
    }

    /**
     * Sets the longitude of the upper-left world-image pixel.
     *
     * @param upperLeftLongitude longitude, or {@code null}
     */
    public void setUpperLeftLongitude(Double upperLeftLongitude) {
        this.upperLeftLongitude = upperLeftLongitude;
    }

    /**
     * Returns a description of the dataset from which the map was derived.
     *
     * @return source-dataset description, or {@code null} if unspecified
     */
    public String getDerivedFrom() {
        return derivedFrom;
    }

    /**
     * Sets the source-dataset description after normalizing optional text.
     *
     * @param derivedFrom source-dataset description; blank text is stored as
     *                    {@code null}
     */
    public void setDerivedFrom(String derivedFrom) {
        this.derivedFrom = clean(derivedFrom);
    }

    /**
     * Returns a description of modifications made to the source image.
     *
     * @return modification description, or {@code null} if unspecified
     */
    public String getModified() {
        return modified;
    }

    /**
     * Sets the modification description after normalizing optional text.
     *
     * @param modified modification description; blank text is stored as
     *                 {@code null}
     */
    public void setModified(String modified) {
        this.modified = clean(modified);
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
