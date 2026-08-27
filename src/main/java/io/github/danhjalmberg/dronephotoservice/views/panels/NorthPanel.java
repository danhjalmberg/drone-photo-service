package io.github.danhjalmberg.dronephotoservice.views.panels;

import io.github.danhjalmberg.dronephotoservice.settings.AppSettings;
import io.github.danhjalmberg.dronephotoservice.settings.ViewSettings;
import io.github.danhjalmberg.dronephotoservice.views.components.KeyValueComponent;
import io.github.danhjalmberg.dronephotoservice.views.components.RoundedPanel;
import io.github.danhjalmberg.dronephotoservice.views.viewdata.SimulationHeaderViewData;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.Duration;

/**
 * Renders the compact main-window simulation header.
 * Raw header data is formatted here into friendly lifecycle labels, elapsed time,
 * speed, and a shortened map path with null fallbacks.
 *
 * @author Dan Hjälmberg
 */
public class NorthPanel extends JPanel {

    private JLabel titleLabel;
    private KeyValueComponent stateLabel;
    private KeyValueComponent timeLabel;
    private KeyValueComponent speedLabel;
    private KeyValueComponent mapLabel;

    /**
     * Initializes the north panel and its child panel.
     */
    public NorthPanel() {

        setOpaque(false);
        setBorder(null);

        setLayout(new GridLayout(
                1,
                1,
                ViewSettings.PANEL_GAP,
                ViewSettings.PANEL_GAP));

        add(createHeaderPanel());
    }

    /**
     * Formats and replaces every header value.
     *
     * @param data simulation header view data
     */
    public void displaySimulationHeader(SimulationHeaderViewData data) {

        titleLabel.setText(data.getApplicationName());
        mapLabel.setValueText(formatMapFilePath(data.getMapFilePath()));
        stateLabel.setValueText(formatState(data));
        timeLabel.setValueText(formatDuration(data.getSimulationTime()));
        speedLabel.setValueText("x" + formatSpeed(data.getSpeedMultiplier()));
    }

    /**
     * Creates the compact simulation header panel.
     *
     * @return the created header panel
     */
    private JPanel createHeaderPanel() {

        JPanel headerPanel = new RoundedPanel(ViewSettings.PANEL_CORNER_RADIUS);

        headerPanel.setBorder(BorderFactory.createEmptyBorder(
                ViewSettings.PANEL_PADDING_TOP,
                ViewSettings.PANEL_PADDING_LEFT,
                ViewSettings.PANEL_PADDING_BOTTOM,
                ViewSettings.PANEL_PADDING_RIGHT));

        headerPanel.setLayout(new GridBagLayout());

        titleLabel = createTitleLabel(AppSettings.APPLICATION_NAME);
        mapLabel = new KeyValueComponent("Map", "none", 13f);
        stateLabel = new KeyValueComponent("State", "No map", 13f);
        timeLabel = new KeyValueComponent("Time", "00:00:00", 13f);
        speedLabel = new KeyValueComponent("Speed", "x1", 13f);

        addHeaderSlot(headerPanel, titleLabel, 0, 400, 0.0);
        addHeaderSlot(headerPanel, mapLabel, 1, 800, 1.0);
        addHeaderSlot(headerPanel, stateLabel, 2, 120, 0.0);
        addHeaderSlot(headerPanel, timeLabel, 3, 120, 0.0);
        addHeaderSlot(headerPanel, speedLabel, 4, 120, 0.0);

        return headerPanel;
    }

    /**
     * Adds a fixed or flexible component slot to the simulation header.
     *
     * @param panel     panel receiving the component
     * @param component component to add
     * @param column    grid column index
     * @param width     preferred slot width
     * @param weightX   horizontal resize weight
     */
    private void addHeaderSlot(
            JPanel panel,
            JComponent component,
            int column,
            int width,
            double weightX) {

        Dimension size = new Dimension(width, component.getPreferredSize().height);

        component.setPreferredSize(size);
        component.setMinimumSize(size);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = 0;
        constraints.weightx = weightX;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, column == 0 ? 0 : 24, 0, 0);

        panel.add(component, constraints);
    }

    /**
     * Creates a title label with the specified text.
     *
     * @param text the text to display on the label
     * @return the created title label
     */
    private JLabel createTitleLabel(String text) {

        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        return label;
    }

    /**
     * Formats the simulation state for display.
     *
     * @param data the simulation header view data
     * @return the formatted simulation state
     */
    private String formatState(SimulationHeaderViewData data) {

        if (data.getSimulationState() == null) {
            return "Unknown";
        }

        return switch (data.getSimulationState()) {
            case NO_MAP_LOADED -> "No map";
            case READY -> "Ready";
            case RUNNING -> "Running";
            case PAUSED -> "Paused";
            case STOPPING -> "Stopping";
            case STOPPED -> "Stopped";
        };
    }

    /**
     * Formats the simulation timer for display in HH:mm:ss format.
     *
     * @param duration the duration to format
     * @return the formatted duration string
     */
    private String formatDuration(Duration duration) {

        if (duration == null) {
            return "00:00:00";
        }

        long seconds = duration.getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        return String.format("%02d:%02d:%02d",
                hours,
                minutes,
                remainingSeconds);
    }

    /**
     * Formats the simulation speed for display.
     *
     * @param speedMultiplier the speed multiplier to format
     * @return the formatted speed string
     */
    private String formatSpeed(double speedMultiplier) {

        if (speedMultiplier == Math.rint(speedMultiplier)) {
            return String.format("%.0f", speedMultiplier);
        }

        return String.format("%.2f", speedMultiplier);
    }

    /**
     * Formats the map file path for display, truncating if necessary.
     *
     * @param mapFilePath the map file path to format
     * @return the formatted map file path
     */
    private String formatMapFilePath(String mapFilePath) {

        if (mapFilePath == null || mapFilePath.isBlank()) {
            return "none";
        }

        int maxLength = 90;

        return mapFilePath.length() <= maxLength
                ? mapFilePath
                : "…" + mapFilePath.substring(mapFilePath.length() - maxLength + 1);
    }
}
