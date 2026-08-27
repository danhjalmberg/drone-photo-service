package io.github.danhjalmberg.dronephotoservice.views.panels;

import io.github.danhjalmberg.dronephotoservice.models.map.MapMetadata;
import io.github.danhjalmberg.dronephotoservice.settings.ModelSettings;
import io.github.danhjalmberg.dronephotoservice.settings.ViewSettings;
import io.github.danhjalmberg.dronephotoservice.views.support.ViewFactory;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.Hashtable;

/**
 * Presents map, simulation, and GUI settings in separate tabs.
 *
 * <p>Discrete tick, speed, and refresh sliders expose mapped domain values rather
 * than their internal indices. Simulation setup locking affects queue and actor
 * pool sizes only; timing and refresh controls remain editable during a run.</p>
 */
public class SettingsPanel extends JPanel {

    private static final String[] SIMULATION_TICK_LABELS =
            {"1 ms", "5 ms", "10 ms", "25 ms", "50 ms", "100 ms", "500 ms", "1000 ms"};
    private static final int[] SIMULATION_TICK_MS_VALUES =
            {1, 5, 10, 25, 50, 100, 500, 1000};
    private static final int SIMULATION_TICK_DEFAULT_INDEX = 4;

    static {
        if (SIMULATION_TICK_LABELS.length != SIMULATION_TICK_MS_VALUES.length) {
            throw new IllegalStateException("Simulation tick labels and values must have the same length.");
        }
    }

    private static final String[] SIMULATION_SPEED_LABELS =
            {"x0.25", "x0.5", "x1", "x2", "x4", "x8", "x16"};
    private static final double[] SIMULATION_SPEED_MULTIPLIERS =
            {0.25, 0.5, 1.0, 2.0, 4.0, 8.0, 16.0};
    private static final int SIMULATION_SPEED_DEFAULT_INDEX = 2;

    static {
        if (SIMULATION_SPEED_LABELS.length != SIMULATION_SPEED_MULTIPLIERS.length) {
            throw new IllegalStateException("Simulation speed labels and values must have the same length.");
        }
    }

    private static final String[] GUI_REFRESH_INTERVAL_LABELS =
            {"1 ms", "5 ms", "10 ms", "25 ms", "50 ms", "100 ms", "500 ms", "1000 ms"};
    private static final int[] GUI_REFRESH_INTERVAL_MS_VALUES =
            {1, 5, 10, 25, 50, 100, 500, 1000};
    private static final int GUI_REFRESH_INTERVAL_DEFAULT_INDEX = 4;

    static {
        if (GUI_REFRESH_INTERVAL_LABELS.length != GUI_REFRESH_INTERVAL_MS_VALUES.length) {
            throw new IllegalStateException("GUI refresh interval labels and values must have the same length.");
        }
    }

    private MapSettingsPanel mapSettingsPanel;

    private JSlider simulationTickSlider;
    private JSlider simulationSpeedSlider;
    private JSlider taskQueueSizeSlider;
    private JSlider photoAgencyPoolSizeSlider;
    private JSlider dronePoolSizeSlider;
    private JSlider guiRefreshSlider;

    /**
     * Creates and initializes the map, simulation, and GUI settings tabs.
     */
    public SettingsPanel() {

        setLayout(new java.awt.BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("MAP", createMapSettingsTab());
        tabs.addTab("SIMULATION", createSimulationSettingsTab());
        tabs.addTab("GUI", createGuiSettingsTab());

        add(tabs, java.awt.BorderLayout.CENTER);
    }

    // ########################################################################
    // Map Settings
    // ########################################################################

    /**
     * Creates the map settings tab panel.
     *
     * @return the map settings tab panel
     */
    private JPanel createMapSettingsTab() {
        mapSettingsPanel = new MapSettingsPanel();
        return mapSettingsPanel;
    }

    /**
     * Displays the map metadata in the map settings panel.
     *
     * @param metadata the map metadata to display
     */
    public void displayMapMetadata(MapMetadata metadata) {
        mapSettingsPanel.displayMapMetadata(metadata);
    }

    /**
     * Gets the meters per pixel input from the map settings panel.
     *
     * @return the meters per pixel input
     */
    public double getMapMetersPerPixelInput() {
        return mapSettingsPanel.getMetersPerPixelInput();
    }

    /**
     * Adds a listener to the apply map scale button in the map settings panel.
     *
     * @param listener the listener to add
     */
    public void addApplyMapScaleListener(ActionListener listener) {
        mapSettingsPanel.addApplyMapScaleListener(listener);
    }

    // ########################################################################
    // Simulation Settings
    // ########################################################################

    /**
     * Maps the selected tick-slider index to its configured millisecond value.
     *
     * @return the simulation tick duration in milliseconds
     */
    public int getSimulationTickMs() {
        return SIMULATION_TICK_MS_VALUES[simulationTickSlider.getValue()];
    }

    /**
     * Adds a listener to the simulation tick slider.
     *
     * @param listener the listener to add
     */
    public void addSimulationTickSliderListener(ChangeListener listener) {
        simulationTickSlider.addChangeListener(listener);
    }

    /**
     * Maps the selected speed-slider index to its configured multiplier.
     *
     * @return the simulation speed multiplier
     */
    public double getSimulationSpeedMultiplier() {
        return SIMULATION_SPEED_MULTIPLIERS[simulationSpeedSlider.getValue()];
    }

    /**
     * Adds a listener to the simulation speed slider.
     *
     * @param listener the listener to add
     */
    public void addSimulationSpeedSliderListener(ChangeListener listener) {
        simulationSpeedSlider.addChangeListener(listener);
    }

    /**
     * Gets the size of the task queue from the slider.
     *
     * @return the size of the task queue
     */
    public int getTaskQueueSize() {
        return taskQueueSizeSlider.getValue();
    }

    /**
     * Gets the size of the photo agency thread pool from the slider.
     *
     * @return the size of the photo agency thread pool
     */
    public int getPhotoAgencyPoolSize() {
        return photoAgencyPoolSizeSlider.getValue();
    }

    /**
     * Gets the size of the drone thread pool from the slider.
     *
     * @return the size of the drone thread pool
     */
    public int getDronePoolSize() {
        return dronePoolSizeSlider.getValue();
    }

    // ########################################################################
    // GUI Settings
    // ########################################################################

    /**
     * Maps the selected refresh-slider index to its configured millisecond value.
     *
     * @return the GUI refresh interval in milliseconds
     */
    public int getGuiRefreshIntervalMs() {
        return GUI_REFRESH_INTERVAL_MS_VALUES[guiRefreshSlider.getValue()];
    }

    /**
     * Adds a listener to the GUI refresh interval slider.
     *
     * @param listener the listener to add
     */
    public void addGuiRefreshSliderListener(ChangeListener listener) {
        guiRefreshSlider.addChangeListener(listener);
    }

    /**
     * Enables or disables editing of the loaded map scale.
     *
     * @param enabled whether map-scale editing is enabled
     */
    public void setMapScaleControlsEnabled(boolean enabled) {

        mapSettingsPanel.setMapScaleControlsEnabled(enabled);
    }

    /**
     * Enables or disables queue and actor-pool sizing for a new simulation.
     * Timing, speed, and GUI refresh remain editable.
     *
     * @param enabled whether simulation setup controls are enabled
     */
    public void setSimulationSetupControlsEnabled(boolean enabled) {

        taskQueueSizeSlider.setEnabled(enabled);
        photoAgencyPoolSizeSlider.setEnabled(enabled);
        dronePoolSizeSlider.setEnabled(enabled);

    }

    /**
     * Creates the simulation settings tab panel.
     *
     * @return the simulation settings tab panel
     */
    private JPanel createSimulationSettingsTab() {

        JPanel panel = createSettingsTabPanel();

        panel.add(ViewFactory.createSectionTitleLabel("Simulation Settings"));
        panel.add(ViewFactory.createSeparator());

        simulationTickSlider = addCustomSlider(
                panel,
                "Simulation tick interval",
                SIMULATION_TICK_DEFAULT_INDEX,
                SIMULATION_TICK_LABELS);

        simulationSpeedSlider = addCustomSlider(
                panel,
                "Simulation speed",
                SIMULATION_SPEED_DEFAULT_INDEX,
                SIMULATION_SPEED_LABELS);


        taskQueueSizeSlider = addSlider(
                panel,
                "Task queue size",
                ModelSettings.TASK_QUEUE_SIZE_MIN,
                ModelSettings.TASK_QUEUE_SIZE_MAX,
                ModelSettings.TASK_QUEUE_SIZE_DEFAULT,
                ModelSettings.TASK_QUEUE_SIZE_MAJOR_TICK,
                0,
                " tasks");

        photoAgencyPoolSizeSlider = addSlider(
                panel,
                "Photo agency thread pool size",
                ModelSettings.PHOTO_AGENCY_POOL_SIZE_MIN,
                ModelSettings.PHOTO_AGENCY_POOL_SIZE_MAX,
                ModelSettings.PHOTO_AGENCY_POOL_SIZE_DEFAULT,
                ModelSettings.PHOTO_AGENCY_POOL_SIZE_MAJOR_TICK,
                0,
                " photo agencies");

        dronePoolSizeSlider = addSlider(
                panel,
                "Drone thread pool size",
                ModelSettings.DRONE_POOL_SIZE_MIN,
                ModelSettings.DRONE_POOL_SIZE_MAX,
                ModelSettings.DRONE_POOL_SIZE_DEFAULT,
                ModelSettings.DRONE_POOL_SIZE_MAJOR_TICK,
                0,
                " drones");

        return panel;
    }

    /**
     * Creates the GUI settings tab panel.
     *
     * @return the created GUI settings tab panel
     */
    private JPanel createGuiSettingsTab() {

        JPanel panel = createSettingsTabPanel();

        panel.add(ViewFactory.createSectionTitleLabel("GUI Settings"));
        panel.add(ViewFactory.createSeparator());

        guiRefreshSlider = addCustomSlider(
                panel,
                "GUI refresh interval",
                GUI_REFRESH_INTERVAL_DEFAULT_INDEX,
                GUI_REFRESH_INTERVAL_LABELS);

        return panel;
    }

    /**
     * Creates a tab panel.
     *
     * @return the created tab panel
     */
    private JPanel createSettingsTabPanel() {

        JPanel panel = new JPanel();

        panel.setOpaque(true);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(
                ViewSettings.PANEL_PADDING_TOP,
                0,
                ViewSettings.PANEL_PADDING_BOTTOM,
                0));

        return panel;
    }

    /**
     * Adds a slider to the given panel with the specified parameters.
     *
     * @param panel            the panel to which the slider will be added
     * @param title            the title of the slider
     * @param min              the minimum value of the slider
     * @param max              the maximum value of the slider
     * @param value            the initial value of the slider
     * @param majorTickSpacing the spacing between major ticks
     * @param minorTickSpacing the  spacing between minor ticks
     * @param suffix           the suffix to display next to the slider value
     * @return the created slider
     */
    private JSlider addSlider(
            JPanel panel,
            String title,
            int min,
            int max,
            int value,
            int majorTickSpacing,
            int minorTickSpacing,
            String suffix) {

        JLabel titleLabel = createSliderTitleLabel(title);
        panel.add(titleLabel);

        JLabel valueLabel = createSliderValueLabel(value + suffix);
        panel.add(valueLabel);

        JSlider slider = new JSlider(min, max, value);

        slider.setMajorTickSpacing(majorTickSpacing);

        if (minorTickSpacing > 0) {
            slider.setMinorTickSpacing(minorTickSpacing);
        }

        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setForeground(ViewSettings.SLIDER_FOREGROUND_COLOR);
        slider.setBackground(ViewSettings.SLIDER_BACKGROUND_COLOR);
        slider.setOpaque(false);
        slider.setAlignmentX(Component.LEFT_ALIGNMENT);

        slider.addChangeListener(event ->
                valueLabel.setText(slider.getValue() + suffix));

        panel.add(slider);

        return slider;
    }

    /**
     * Creates a slider whose values are represented by custom labels.
     * The slider itself returns an index (0..n-1). The caller is responsible
     * for mapping that index to the corresponding setting value.
     *
     * @param panel        the panel to which the slider will be added
     * @param title        the slider title
     * @param defaultIndex the initially selected index
     * @param labels       the labels to display for each slider position
     * @return the created slider
     * @throws IllegalArgumentException if the labels and values arrays have different lengths
     *                                  or if the defaultIndex is out of bounds
     */
    private JSlider addCustomSlider(
            JPanel panel,
            String title,
            int defaultIndex,
            String[] labels) {

        if (defaultIndex < 0 || defaultIndex >= labels.length) {
            throw new IllegalArgumentException(
                    "Invalid default index: " + defaultIndex);
        }

        JLabel titleLabel = createSliderTitleLabel(title);
        panel.add(titleLabel);

        JLabel valueLabel = createSliderValueLabel(labels[defaultIndex]);
        panel.add(valueLabel);

        JSlider slider = new JSlider(0, labels.length - 1, defaultIndex);

        slider.setMajorTickSpacing(1);
        slider.setSnapToTicks(true);

        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();

        for (int i = 0; i < labels.length; i++) {
            JLabel label = new JLabel(labels[i]);
            label.setFont(ViewSettings.FONT_DEFAULT);
            labelTable.put(i, label);
        }

        slider.setLabelTable(labelTable);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setForeground(ViewSettings.SLIDER_FOREGROUND_COLOR);
        slider.setBackground(ViewSettings.SLIDER_BACKGROUND_COLOR);
        slider.setOpaque(false);
        slider.setAlignmentX(Component.LEFT_ALIGNMENT);

        slider.addChangeListener(event ->
                valueLabel.setText(labels[slider.getValue()]));

        panel.add(slider);

        return slider;
    }

    /**
     * Creates the slider title label with the specified text.
     *
     * @param text the text to display on the label
     * @return the created slider title label
     */
    private JLabel createSliderTitleLabel(String text) {

        JLabel label = new JLabel(text);

        label.setFont(ViewSettings.FONT_DEFAULT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));

        return label;
    }

    /**
     * Creates the slider value label with the specified text.
     *
     * @param text the text to display on the label
     * @return the created slider value label
     */
    private JLabel createSliderValueLabel(String text) {

        JLabel label = new JLabel(text);

        label.setFont(ViewSettings.FONT_DEFAULT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }
}
