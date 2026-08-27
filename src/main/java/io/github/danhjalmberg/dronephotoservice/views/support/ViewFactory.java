package io.github.danhjalmberg.dronephotoservice.views.support;

import io.github.danhjalmberg.dronephotoservice.settings.ViewSettings;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;

/**
 * Creates and lays out small, consistently styled Swing components.
 *
 * <p>These helpers centralize recurring labels, separators, detail rows, and
 * telemetry controls. Application-specific component composition remains in the
 * panel classes.</p>
 */
public final class ViewFactory {

    private static final int DETAIL_KEY_COLUMN_WIDTH = 112;

    /**
     * Prevents instantiation of this utility class.
     */
    private ViewFactory() { }

    /**
     * Creates a left-aligned section title with bottom spacing.
     *
     * @param text the text to display in the label
     * @return configured title label
     */
    public static JLabel createSectionTitleLabel(String text) {

        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        return label;
    }

    /**
     * Creates a section title with additional top spacing.
     *
     * @param text the text to display in the label
     * @return configured spaced title label
     */
    public static JLabel createSpacedSectionTitleLabel(String text) {

        JLabel label = createSectionTitleLabel(text);
        label.setBorder(BorderFactory.createEmptyBorder(12, 0, 6, 0));

        return label;
    }

    /**
     * Creates a compact, left-aligned subsection title.
     *
     * @param text the text to display in the label
     * @return configured subsection title label
     */
    public static JLabel createSubsectionTitleLabel(String text) {

        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));

        return label;
    }

    /**
     * Creates a subsection title with additional surrounding spacing.
     *
     * @param text the text to display in the label
     * @return configured spaced subsection title label
     */
    public static JLabel createSpacedSubsectionTitleLabel(String text) {

        JLabel label = createSubsectionTitleLabel(text);
        label.setBorder(BorderFactory.createEmptyBorder(12, 0, 6, 0));

        return label;
    }

    /**
     * Creates a one-pixel horizontal separator that may expand in width.
     *
     * @return separator panel
     */
    public static JPanel createSeparator() {

        JPanel separator = new JPanel();

        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setPreferredSize(new Dimension(1, 1));
        separator.setBackground(ViewSettings.SEPARATOR_BACKGROUND_COLOR);
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);

        return separator;
    }

    /**
     * Creates a regular-weight label for a detail-row key.
     *
     * @param text the text for the label
     * @return configured key label
     */
    public static JLabel createDetailKeyLabel(String text) {

        JLabel label = new JLabel(text);

        label.setFont(label.getFont().deriveFont(11f));
        label.setHorizontalAlignment(SwingConstants.LEFT);

        return label;
    }

    /**
     * Creates a bold label for a detail-row value.
     *
     * @param text the text for the label
     * @return configured value label
     */
    public static JLabel createDetailValueLabel(String text) {

        JLabel label = new JLabel(text);

        label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
        label.setHorizontalAlignment(SwingConstants.LEFT);

        return label;
    }

    /**
     * Adds a key and value component as one row in a GridBag-layout panel.
     *
     * @param panel GridBag-layout panel receiving both components
     * @param row grid row index
     * @param key key-label text
     * @param valueComponent component displayed in the value column
     */
    public static void addDetailRow(
            JPanel panel,
            int row,
            String key,
            Component valueComponent) {

        addDetailRow(
                panel,
                row,
                key,
                valueComponent,
                null);
    }

    /**
     * Adds a detail row whose key column uses the shared fixed width.
     *
     * @param panel GridBag-layout panel receiving both components
     * @param row grid row index
     * @param key key-label text
     * @param valueComponent component displayed in the value column
     */
    public static void addAlignedDetailRow(
            JPanel panel,
            int row,
            String key,
            Component valueComponent) {

        addDetailRow(
                panel,
                row,
                key,
                valueComponent,
                DETAIL_KEY_COLUMN_WIDTH);
    }

    /**
     * Adds a detail row with an optional fixed key-column width.
     *
     * @param panel GridBag-layout panel receiving both components
     * @param row grid row index
     * @param key key-label text
     * @param valueComponent component displayed in the value column
     * @param fixedKeyWidth fixed key width in pixels, or {@code null} to retain
     *                      the preferred width
     */
    private static void addDetailRow(
            JPanel panel,
            int row,
            String key,
            Component valueComponent,
            Integer fixedKeyWidth) {

        JLabel keyLabel = createDetailKeyLabel(key);

        if (fixedKeyWidth != null) {
            Dimension preferredSize = keyLabel.getPreferredSize();

            Dimension fixedSize = new Dimension(
                    fixedKeyWidth,
                    preferredSize.height);

            keyLabel.setPreferredSize(fixedSize);
            keyLabel.setMinimumSize(fixedSize);
        }

        GridBagConstraints keyConstraints = new GridBagConstraints();
        keyConstraints.gridx = 0;
        keyConstraints.gridy = row;
        keyConstraints.anchor = GridBagConstraints.WEST;
        keyConstraints.insets = new Insets(8, 0, 0, 12);

        panel.add(keyLabel, keyConstraints);

        GridBagConstraints valueConstraints = new GridBagConstraints();
        valueConstraints.gridx = 1;
        valueConstraints.gridy = row;
        valueConstraints.weightx = 1.0;
        valueConstraints.anchor = GridBagConstraints.WEST;
        valueConstraints.fill = GridBagConstraints.HORIZONTAL;
        valueConstraints.insets = new Insets(8, 0, 0, 0);

        panel.add(valueComponent, valueConstraints);
    }

    /**
     * Creates a string-painted telemetry progress bar with the range 0–100.
     *
     * @return telemetry progress bar initialized to zero with empty text
     */
    public static JProgressBar createTelemetryProgressBar() {

        JProgressBar progressBar = new JProgressBar(0, 100);

        progressBar.setStringPainted(true);
        progressBar.setValue(0);
        progressBar.setString("");

        progressBar.setPreferredSize(new Dimension(140, 20));
        progressBar.setMinimumSize(new Dimension(120, 20));

        return progressBar;
    }
}
