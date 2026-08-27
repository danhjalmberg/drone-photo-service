package io.github.danhjalmberg.dronephotoservice.views.panels;

import io.github.danhjalmberg.dronephotoservice.models.map.MapMetadata;
import io.github.danhjalmberg.dronephotoservice.settings.ViewSettings;
import io.github.danhjalmberg.dronephotoservice.views.support.ControlButtonFactory;
import io.github.danhjalmberg.dronephotoservice.views.support.ViewFactory;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

/**
 * Displays loaded-map metadata and accepts a manual map-scale override.
 *
 * <p>Optional metadata uses a dash placeholder. Scale input accepts either a dot
 * or comma decimal separator and may be submitted by the Apply button or Enter.
 * Input parsing is intentionally left to the caller-facing getter.</p>
 */
public class MapSettingsPanel extends JPanel {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.###");

    private final JLabel fileNameLabel;
    private final JLabel titleLabel;
    private final JLabel sourceLabel;
    private final JLabel licenseLabel;
    private final JLabel derivedFromLabel;

    private final JLabel originalSizeLabel;
    private final JLabel worldSizePixelsLabel;
    private final JLabel displaySizeLabel;
    private final JLabel worldSizeMetersLabel;
    private final JLabel scaleSourceLabel;

    private final JLabel geoReferenceSourceLabel;
    private final JLabel coordinateReferenceSystemLabel;
    private final JLabel upperLeftLatitudeLabel;
    private final JLabel upperLeftLongitudeLabel;

    private final JTextField metersPerPixelField;

    private final JButton applyScaleButton;

    /**
     * Creates the map settings panel.
     */
    public MapSettingsPanel() {

        setOpaque(true);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(
                ViewSettings.PANEL_PADDING_TOP,
                0,
                ViewSettings.PANEL_PADDING_BOTTOM,
                0));

        fileNameLabel = ViewFactory.createDetailValueLabel("No map loaded");
        titleLabel = ViewFactory.createDetailValueLabel("-");
        sourceLabel = ViewFactory.createDetailValueLabel("-");
        licenseLabel = ViewFactory.createDetailValueLabel("-");
        derivedFromLabel = ViewFactory.createDetailValueLabel("-");

        originalSizeLabel = ViewFactory.createDetailValueLabel("-");
        worldSizePixelsLabel = ViewFactory.createDetailValueLabel("-");
        displaySizeLabel = ViewFactory.createDetailValueLabel("-");
        worldSizeMetersLabel = ViewFactory.createDetailValueLabel("-");
        scaleSourceLabel = ViewFactory.createDetailValueLabel("-");

        geoReferenceSourceLabel = ViewFactory.createDetailValueLabel("-");
        coordinateReferenceSystemLabel = ViewFactory.createDetailValueLabel("-");
        upperLeftLatitudeLabel = ViewFactory.createDetailValueLabel("-");
        upperLeftLongitudeLabel = ViewFactory.createDetailValueLabel("-");

        metersPerPixelField = createInputField(80);
        applyScaleButton = ControlButtonFactory.createSmallActionButton("Apply");

        JPanel metadataSection = createMetadataSection();
        JPanel scaleSection = createScaleSection();
        JPanel geoReferenceSection = createGeoReferenceSection();

        add(fixSectionHeight(metadataSection));
        add(fixSectionHeight(scaleSection));
        add(fixSectionHeight(geoReferenceSection));
        add(Box.createVerticalGlue());

        clear();
    }

    /**
     * Allows a section to grow horizontally, but prevents it from stretching vertically.
     *
     * @param section section panel.
     * @return the same section panel with constrained maximum height.
     */
    private JPanel fixSectionHeight(JPanel section) {

        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        Dimension preferredSize = section.getPreferredSize();

        section.setMaximumSize(new Dimension(
                Integer.MAX_VALUE,
                preferredSize.height));

        return section;
    }

    /**
     * Formats all metadata sections, or restores the no-map state for
     * {@code null}.
     *
     * @param metadata map metadata, or null if no map is loaded.
     */
    public void displayMapMetadata(MapMetadata metadata) {

        if (metadata == null) {
            clear();
            return;
        }

        fileNameLabel.setText(metadata.getFileName());
        titleLabel.setText(formatOptionalText(metadata.getTitle()));
        sourceLabel.setText(formatOptionalText(metadata.getSource()));
        licenseLabel.setText(formatOptionalText(metadata.getLicense()));
        derivedFromLabel.setText(formatOptionalText(metadata.getDerivedFrom()));

        originalSizeLabel.setText(formatPixelSize(
                metadata.getOriginalWidthPixels(),
                metadata.getOriginalHeightPixels()));

        worldSizePixelsLabel.setText(formatPixelSize(
                metadata.getWorldWidthPixels(),
                metadata.getWorldHeightPixels()));

        displaySizeLabel.setText(formatPixelSize(
                metadata.getDisplayWidthPixels(),
                metadata.getDisplayHeightPixels()));

        worldSizeMetersLabel.setText(
                DECIMAL_FORMAT.format(metadata.getWorldWidthMeters())
                        + " × "
                        + DECIMAL_FORMAT.format(metadata.getWorldHeightMeters())
                        + " m");

        scaleSourceLabel.setText(metadata.getScaleSource().toString());

        metersPerPixelField.setText(DECIMAL_FORMAT.format(metadata.getMetersPerPixel()));

        geoReferenceSourceLabel.setText(metadata.getGeoReferenceSource().toString());
        coordinateReferenceSystemLabel.setText(formatOptionalText(metadata.getCoordinateReferenceSystem()));
        upperLeftLatitudeLabel.setText(formatOptionalDouble(metadata.getUpperLeftLatitude()));
        upperLeftLongitudeLabel.setText(formatOptionalDouble(metadata.getUpperLeftLongitude()));
    }

    // ########################################################################
    // Getters
    // ########################################################################

    /**
     * Parses trimmed meters-per-pixel input after normalizing comma to dot.
     *
     * @return entered scale value
     * @throws NumberFormatException if input is not a valid number
     */
    public double getMetersPerPixelInput() {
        return Double.parseDouble(
                metersPerPixelField.getText().trim().replace(',', '.'));
    }

    // ########################################################################
    // Listeners
    // ########################################################################

    /**
     * Adds the same listener to the Apply button and text-field Enter action.
     *
     * @param listener action listener.
     */
    public void addApplyMapScaleListener(ActionListener listener) {
        applyScaleButton.addActionListener(listener);
        metersPerPixelField.addActionListener(listener);
    }

    // ########################################################################
    // Flow control
    // ########################################################################

    /**
     * Enables or disables map scale editing controls.
     *
     * @param enabled true if editing should be enabled.
     */
    public void setMapScaleControlsEnabled(boolean enabled) {
        metersPerPixelField.setEnabled(enabled);
        applyScaleButton.setEnabled(enabled);
    }

    /**
     * Restores placeholders, empties scale input, and disables scale editing.
     */
    public void clear() {
        fileNameLabel.setText("No map loaded");
        titleLabel.setText("-");
        sourceLabel.setText("-");
        licenseLabel.setText("-");
        derivedFromLabel.setText("-");

        originalSizeLabel.setText("-");
        worldSizePixelsLabel.setText("-");
        displaySizeLabel.setText("-");
        worldSizeMetersLabel.setText("-");
        scaleSourceLabel.setText("-");
        metersPerPixelField.setText("");

        geoReferenceSourceLabel.setText("-");
        upperLeftLatitudeLabel.setText("-");
        upperLeftLongitudeLabel.setText("-");
        coordinateReferenceSystemLabel.setText("-");

        setMapScaleControlsEnabled(false);
    }

    /**
     * Creates the read-only image metadata section.
     *
     * @return created metadata section.
     */
    private JPanel createMetadataSection() {

        JPanel wrapper = createSectionWrapper("Map Settings");
        JPanel rowsPanel = createRowsPanel();
        wrapper.add(rowsPanel);

        int row = 0;
        ViewFactory.addAlignedDetailRow(rowsPanel, row++, "Loaded file:", fileNameLabel);
        ViewFactory.addAlignedDetailRow(rowsPanel, row++, "Title:", titleLabel);
        ViewFactory.addAlignedDetailRow(rowsPanel, row++, "Source:", sourceLabel);
        ViewFactory.addAlignedDetailRow(rowsPanel, row++, "License:", licenseLabel);
        ViewFactory.addAlignedDetailRow(rowsPanel, row++, "Derived from:", derivedFromLabel);

        ViewFactory.addAlignedDetailRow(rowsPanel, row++, "Original size:", originalSizeLabel);
        ViewFactory.addAlignedDetailRow(rowsPanel, row++, "World size:", worldSizePixelsLabel);
        ViewFactory.addAlignedDetailRow(rowsPanel, row++, "Display size:", displaySizeLabel);
        ViewFactory.addAlignedDetailRow(rowsPanel, row++, "World size meters:", worldSizeMetersLabel);
        ViewFactory.addAlignedDetailRow(rowsPanel, row++, "Scale source:", scaleSourceLabel);

        return wrapper;
    }

    /**
     * Creates the editable scale section.
     *
     * @return created scale section.
     */
    private JPanel createScaleSection() {

        JPanel wrapper = createSectionWrapper("Scale");
        wrapper.add(createScaleInputRow());

        return wrapper;
    }

    /**
     * Creates the read-only georeference metadata section.
     *
     * @return created georeference section.
     */
    private JPanel createGeoReferenceSection() {

        JPanel wrapper = createSectionWrapper("Georeference");
        JPanel rowsPanel = createRowsPanel();
        wrapper.add(rowsPanel);

        int row = 0;
        ViewFactory.addAlignedDetailRow(rowsPanel, row++, "Source:", geoReferenceSourceLabel);
        ViewFactory.addAlignedDetailRow(rowsPanel, row++, "CRS:", coordinateReferenceSystemLabel);
        ViewFactory.addAlignedDetailRow(rowsPanel, row++, "Upper-left latitude:", upperLeftLatitudeLabel);
        ViewFactory.addAlignedDetailRow(rowsPanel, row++, "Upper-left longitude:", upperLeftLongitudeLabel);

        return wrapper;
    }

    /**
     * Creates a wrapper panel for a titled section, with a title label and a separator.
     *
     * @param title section title.
     * @return created wrapper panel.
     */
    private JPanel createSectionWrapper(String title) {

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        wrapper.add(ViewFactory.createSpacedSectionTitleLabel(title));
        wrapper.add(ViewFactory.createSeparator());

        return wrapper;
    }

    /**
     * Creates a panel with GridBagLayout for arranging rows of components.
     *
     * @return created rows panel.
     */
    private JPanel createRowsPanel() {

        JPanel rowsPanel = new JPanel(new GridBagLayout());
        rowsPanel.setOpaque(false);
        rowsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        return rowsPanel;
    }

    /**
     * Creates the row for editing the map scale.
     *
     * @return created scale input row.
     */
    private JPanel createScaleInputRow() {

        JPanel rowsPanel = createRowsPanel();

        JPanel valuePanel = new JPanel(new BorderLayout(8, 0));
        valuePanel.setOpaque(false);

        valuePanel.add(metersPerPixelField, BorderLayout.WEST);
        valuePanel.add(applyScaleButton, BorderLayout.EAST);

        ViewFactory.addAlignedDetailRow(
                rowsPanel,
                0,
                "Meters/pixel:",
                valuePanel);

        return rowsPanel;
    }

    /**
     * Creates a compact text field for map metadata input.
     *
     * @param width preferred field width.
     * @return created text field.
     */
    private JTextField createInputField(int width) {

        JTextField field = new JTextField();
        field.setFont(ViewSettings.FONT_DEFAULT);

        Dimension size = new Dimension(width, 24);
        field.setPreferredSize(size);
        field.setMinimumSize(size);
        field.setMaximumSize(size);

        return field;
    }

    /**
     * Creates a string representation of a pixel size.
     *
     * @param width width in pixels.
     * @param height height in pixels.
     * @return formatted pixel size string.
     */
    private String formatPixelSize(int width, int height) {
        return width + " × " + height + " px";
    }

    /**
     * Creates a string representation of an optional Double value.
     *
     * @param value value to format.
     * @return formatted string, or empty string if value is null.
     */
    private String formatOptionalDouble(Double value) {
        return value == null ? "" : DECIMAL_FORMAT.format(value);
    }

    /**
     * Creates a string representation of optional text metadata.
     *
     * @param value metadata text value.
     * @return formatted string, or empty string if value is null.
     */
    private String formatOptionalText(String value) {
        return value == null ? "" : value;
    }
}
