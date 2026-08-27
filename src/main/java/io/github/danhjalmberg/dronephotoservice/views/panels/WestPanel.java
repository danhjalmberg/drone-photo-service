package io.github.danhjalmberg.dronephotoservice.views.panels;

import io.github.danhjalmberg.dronephotoservice.models.events.SimulationEvent;
import io.github.danhjalmberg.dronephotoservice.models.map.MapMetadata;
import io.github.danhjalmberg.dronephotoservice.settings.ViewSettings;
import io.github.danhjalmberg.dronephotoservice.views.components.RoundedPanel;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Composes the settings, overview, monitor, and event-log tabs.
 *
 * <p>This main-window facade exposes the values and listener hooks required by
 * controllers while leaving formatting, table state, and text retention to its
 * child panels.</p>
 *
 * @author Dan Hjälmberg
 */
public class WestPanel extends JPanel {

    private final SettingsPanel settingsPanel;
    private final OverviewPanel overviewPanel;
    private final MonitorsPanel monitorsPanel;
    private final EventLogPanel eventLogPanel;

    /**
     * Initializes the west panel and its child panels.
     */
    public WestPanel() {

        Dimension westPanelSize = new Dimension(
                ViewSettings.WEST_PANEL_WIDTH,
                ViewSettings.WEST_PANEL_HEIGHT);
        setPreferredSize(westPanelSize);
        setMinimumSize(westPanelSize);
        setMaximumSize(westPanelSize);

        setOpaque(false);
        setBorder(null);

        setLayout(new GridLayout(
                1,
                1,
                ViewSettings.PANEL_GAP,
                ViewSettings.PANEL_GAP));

        settingsPanel = new SettingsPanel();
        overviewPanel = new OverviewPanel();
        monitorsPanel = new MonitorsPanel();
        eventLogPanel = new EventLogPanel();

        add(createWestTabbedPanel());
    }

    /**
     * Creates the rounded container holding all four west-side tabs.
     *
     * @return the created west tabbed panel
     */
    private JPanel createWestTabbedPanel() {

        JPanel panel = new RoundedPanel(ViewSettings.PANEL_CORNER_RADIUS);

        panel.setBorder(BorderFactory.createEmptyBorder(
                ViewSettings.PANEL_PADDING_TOP,
                ViewSettings.PANEL_PADDING_LEFT,
                ViewSettings.PANEL_PADDING_BOTTOM,
                ViewSettings.PANEL_PADDING_RIGHT));

        panel.setLayout(new BorderLayout());

        JTabbedPane westPanelTabs = new JTabbedPane();

        westPanelTabs.addTab("SETTINGS", settingsPanel);
        westPanelTabs.addTab("OVERVIEW", overviewPanel);
        westPanelTabs.addTab("MONITORS", monitorsPanel);
        westPanelTabs.addTab("EVENT LOG", eventLogPanel);

        panel.add(westPanelTabs, BorderLayout.CENTER);

        return panel;
    }

    // ########################################################################
    // Settings panel
    // ########################################################################

    /**
     * Displays the map metadata in the settings panel.
     *
     * @param metadata the map metadata to display
     */
    public void displayMapMetadata(MapMetadata metadata) {
        settingsPanel.displayMapMetadata(metadata);
    }

    /**
     * Gets the meters per pixel input from the settings panel.
     *
     * @return the meters per pixel input
     */
    public double getMapMetersPerPixelInput() {
        return settingsPanel.getMapMetersPerPixelInput();
    }

    /**
     * Adds a listener to the map scale input field and apply button in the settings panel.
     *
     * @param listener the listener to add
     */
    public void addApplyMapScaleListener(ActionListener listener) {
        settingsPanel.addApplyMapScaleListener(listener);
    }

    /**
     * Gets the simulation tick duration from the settings panel.
     *
     * @return the simulation tick duration in milliseconds
     */
    public int getSimulationTickMs() {
        return settingsPanel.getSimulationTickMs();
    }

    /**
     * Adds a listener to the simulation tick slider.
     *
     * @param listener the listener to add
     */
    public void addSimulationTickSliderListener(ChangeListener listener) {
        settingsPanel.addSimulationTickSliderListener(listener);
    }

    /**
     * Gets the simulation speed multiplier from the settings panel.
     *
     * @return the simulation speed multiplier
     */
    public double getSimulationSpeedMultiplier() {
        return settingsPanel.getSimulationSpeedMultiplier();
    }

    /**
     * Adds a listener to the simulation speed slider.
     *
     * @param listener the listener to add
     */
    public void addSimulationSpeedSliderListener(ChangeListener listener) {
        settingsPanel.addSimulationSpeedSliderListener(listener);
    }

    /**
     * Gets the task queue size from the settings panel.
     *
     * @return the task queue size
     */
    public int getTaskQueueSize() {
        return settingsPanel.getTaskQueueSize();
    }

    /**
     * Gets the photo agency pool size from the settings panel.
     *
     * @return the photo agency pool size
     */
    public int getPhotoAgencyPoolSize() {
        return settingsPanel.getPhotoAgencyPoolSize();
    }

    /**
     * Gets the drone pool size from the settings panel.
     *
     * @return the drone pool size
     */
    public int getDronePoolSize() {
        return settingsPanel.getDronePoolSize();
    }

    /**
     * Gets the GUI refresh interval from the settings panel.
     *
     * @return the GUI refresh interval in milliseconds
     */
    public int getGuiRefreshIntervalMs() {
        return settingsPanel.getGuiRefreshIntervalMs();
    }

    /**
     * Adds a listener to the GUI refresh interval slider.
     *
     * @param listener the listener to add
     */
    public void addGuiRefreshSliderListener(ChangeListener listener) {
        settingsPanel.addGuiRefreshSliderListener(listener);
    }

    /**
     * Enables or disables map-scale editing.
     *
     * @param enabled whether map-scale controls are enabled
     */
    public void setMapScaleControlsEnabled(boolean enabled) {

        settingsPanel.setMapScaleControlsEnabled(enabled);
    }

    /**
     * Enables or disables controls used to configure a new simulation.
     *
     * @param enabled whether simulation setup controls are enabled
     */
    public void setSimulationSetupControlsEnabled(boolean enabled) {

        settingsPanel.setSimulationSetupControlsEnabled(enabled);
    }

    // ########################################################################
    // Overview panel
    // ########################################################################

    /**
     * Adds a selection listener to the drone overview table.
     *
     * @param listener the listener to add
     */
    public void addDroneTableSelectionListener(ListSelectionListener listener) {
        overviewPanel.addDroneTableSelectionListener(listener);
    }

    /**
     * Gets the name of the currently selected drone.
     *
     * @return the selected drone name, or null if none is selected
     */
    public String getSelectedDroneName() {
        return overviewPanel.getSelectedDroneName();
    }

    /**
     * Selects the drone with the specified name in the overview table.
     *
     * @param droneName the drone name to select
     */
    public void selectDroneByName(String droneName) {
        overviewPanel.selectDroneByName(droneName);
    }

    /**
     * Displays photo agency overview data.
     *
     * @param data the overview table data
     */
    public void displayPhotoAgencyOverview(Object[][] data) {

        overviewPanel.displayPhotoAgencyOverview(data);
    }

    /**
     * Displays drone overview data.
     *
     * @param data the overview table data
     */
    public void displayDroneOverview(Object[][] data) {

        overviewPanel.displayDroneOverview(data);
    }

    /**
     * Displays task overview data.
     *
     * @param data the overview table data
     */
    public void displayTaskOverview(Object[][] data) {

        overviewPanel.displayTaskOverview(data);
    }

    // ########################################################################
    // Monitors panel
    // ########################################################################

    /**
     * Displays photo agency monitor information.
     *
     * @param photoAgencyCount          the number of photo agencies
     * @param photoAgencyDiagnosticText diagnostic text for photo agencies
     */
    public void displayPhotoAgencyMonitor(
            int photoAgencyCount,
            String photoAgencyDiagnosticText) {

        monitorsPanel.displayPhotoAgencyMonitor(
                photoAgencyCount,
                photoAgencyDiagnosticText);
    }

    /**
     * Displays drone monitor information.
     *
     * @param droneCount          the number of drones
     * @param droneDiagnosticText diagnostic text for drones
     */
    public void displayDroneMonitor(
            int droneCount,
            String droneDiagnosticText) {

        monitorsPanel.displayDroneMonitor(
                droneCount,
                droneDiagnosticText);
    }

    /**
     * Displays completed-task monitor information.
     *
     * @param completedTaskCount       the number of completed tasks
     * @param archivedTaskDiagnosticText diagnostic text for archived tasks
     */
    public void displayCompletedTaskMonitor(
            int completedTaskCount,
            String archivedTaskDiagnosticText) {

        monitorsPanel.displayCompletedTaskMonitor(
                completedTaskCount,
                archivedTaskDiagnosticText);
    }

    /**
     * Clears simulation-derived overview, monitor, and event-log content while
     * preserving user settings and loaded-map metadata.
     */
    public void clearSimulationData() {
        overviewPanel.clear();
        monitorsPanel.clear();
        eventLogPanel.clear();
    }

    // ########################################################################
    // Event Log panel
    // ########################################################################

    /**
     * Appends simulation events to the event log.
     *
     * @param events events to append
     */
    public void appendSimulationEvents(List<SimulationEvent> events) {

        eventLogPanel.appendEvents(events);
    }
}
