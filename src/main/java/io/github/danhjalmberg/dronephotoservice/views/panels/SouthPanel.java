package io.github.danhjalmberg.dronephotoservice.views.panels;

import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;
import io.github.danhjalmberg.dronephotoservice.settings.ViewSettings;
import io.github.danhjalmberg.dronephotoservice.views.components.KeyValueComponent;
import io.github.danhjalmberg.dronephotoservice.views.components.RoundedPanel;
import io.github.danhjalmberg.dronephotoservice.views.viewdata.StatusBarViewData;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

/**
 * Renders the compact main-window contextual status bar.
 * World-meter mouse coordinates, current selection, task counts, and activity
 * text are formatted with user-facing fallbacks.
 *
 * @author Dan Hjälmberg
 */
public class SouthPanel extends JPanel {

    private KeyValueComponent mouseLabel;
    private KeyValueComponent selectionLabel;
    private KeyValueComponent queueLabel;
    private KeyValueComponent completedLabel;
    private KeyValueComponent activityLabel;

    /**
     * Initializes the south panel and its compact status bar.
     */
    public SouthPanel() {

        setOpaque(false);
        setBorder(null);

        setLayout(new GridLayout(
                1,
                1,
                ViewSettings.PANEL_GAP,
                ViewSettings.PANEL_GAP));

        add(createStatusPanel());
    }

    /**
     * Formats and replaces every status-bar value.
     *
     * @param data status bar view data
     */
    public void displayStatusBar(StatusBarViewData data) {

        mouseLabel.setValueText(
                formatMousePosition(data.getMouseWorldPositionMeters()));

        selectionLabel.setValueText(
                formatText(data.getSelectionText(), "none"));

        queueLabel.setValueText(
                String.valueOf(data.getQueuedTaskCount()));

        completedLabel.setValueText(
                String.valueOf(data.getCompletedTaskCount()));

        activityLabel.setValueText(
                formatText(data.getActivityMessage(), "Ready"));
    }

    /**
     * Creates the compact status panel.
     *
     * @return the created status panel
     */
    private JPanel createStatusPanel() {

        JPanel statusPanel = new RoundedPanel(ViewSettings.PANEL_CORNER_RADIUS);

        statusPanel.setBorder(BorderFactory.createEmptyBorder(
                ViewSettings.PANEL_PADDING_TOP,
                ViewSettings.PANEL_PADDING_LEFT,
                ViewSettings.PANEL_PADDING_BOTTOM,
                ViewSettings.PANEL_PADDING_RIGHT));

        statusPanel.setLayout(new GridBagLayout());

        mouseLabel = new KeyValueComponent("Mouse", "—", 12f);
        selectionLabel = new KeyValueComponent("Selection", "none", 12f);
        queueLabel = new KeyValueComponent("Enqueued tasks", "0", 12f);
        completedLabel = new KeyValueComponent("Completed tasks", "0", 12f);
        activityLabel = new KeyValueComponent("Activity", "Ready", 12f);

        addStatusSlot(statusPanel, createLeftMargin(), 0, 420, 0.0);
        addStatusSlot(statusPanel, mouseLabel, 1, 200, 0.0);
        addStatusSlot(statusPanel, selectionLabel, 2, 160, 0.0);
        addStatusSlot(statusPanel, queueLabel, 3, 160, 0.0);
        addStatusSlot(statusPanel, completedLabel, 4, 160, 0.0);
        addStatusSlot(statusPanel, activityLabel, 5, 600, 1.0);

        return statusPanel;
    }

    /**
     * Creates an empty left margin panel for the status bar.
     *
     * @return the created left margin panel
     */
    private JPanel createLeftMargin() {

        JPanel margin = new JPanel();
        margin.setOpaque(false);
        return margin;
    }

    /**
     * Adds a fixed or flexible component slot to the status bar.
     *
     * @param panel panel receiving the component
     * @param component component to add
     * @param column grid column index
     * @param width preferred slot width
     * @param weightX horizontal resize weight
     */
    private void addStatusSlot(
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
     * Formats the current mouse position for display.
     *
     * @param position mouse position in world meters, or null
     * @return formatted mouse position
     */
    private String formatMousePosition(Vector2D position) {

        if (position == null) {
            return "—";
        }

        return String.format("x=%.0f m, y=%.0f m",
                position.getX(),
                position.getY());
    }

    /**
     * Formats nullable text values for display.
     *
     * @param text fallback text
     * @param fallback fallback text if value is null or blank
     * @return formatted text
     */
    private String formatText(String text, String fallback) {

        if (text == null || text.isBlank()) {
            return fallback;
        }

        int maxLength = 60;

        return text.length() <= maxLength
                ? text
                : text.substring(0, maxLength - 1) + "…";
    }
}
