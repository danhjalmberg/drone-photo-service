package io.github.danhjalmberg.dronephotoservice.views.panels;

import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;
import io.github.danhjalmberg.dronephotoservice.settings.ViewSettings;
import io.github.danhjalmberg.dronephotoservice.views.viewdata.MapDroneViewData;
import io.github.danhjalmberg.dronephotoservice.views.viewdata.MapSelection;
import io.github.danhjalmberg.dronephotoservice.views.viewdata.MapTaskViewData;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.TaskThumbnailSnapshot;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

/**
 * Composes the interactive center region of the main window.
 *
 * <p>The panel arranges simulation controls, the map, map-display tools, and
 * completed-task thumbnails. Its public API is a facade that forwards controller
 * callbacks and presentation updates to those child panels.</p>
 */
public class CenterPanel extends JPanel {

    private final SimulationControlsPanel simulationControlsPanel;
    private final MapPanel mapPanel;
    private final MapToolsPanel mapToolsPanel;
    private final TaskThumbnailStripPanel taskThumbnailStripPanel;

    /**
     * Creates the center panel with its sub-panels and layout.
     */
    public CenterPanel() {

        Dimension size = new Dimension(
                ViewSettings.CENTER_PANEL_WIDTH,
                ViewSettings.CENTER_PANEL_HEIGHT);
        setPreferredSize(size);
        setMinimumSize(size);

        setOpaque(false);
        setBorder(null);

        setLayout(new BorderLayout(
                ViewSettings.PANEL_GAP,
                ViewSettings.PANEL_GAP));

        simulationControlsPanel = new SimulationControlsPanel();
        mapPanel = new MapPanel();
        mapToolsPanel = new MapToolsPanel(
                mapPanel::setShowLabels,
                mapPanel::setShowVideoTrails);
        taskThumbnailStripPanel = new TaskThumbnailStripPanel();

        add(simulationControlsPanel, BorderLayout.NORTH);
        add(mapPanel, BorderLayout.CENTER);
        add(mapToolsPanel, BorderLayout.EAST);
        add(taskThumbnailStripPanel, BorderLayout.SOUTH);
    }

    /**
     * Registers a command listener with every simulation-control button.
     *
     * @param listener the listener to be added.
     */
    public void addCommandListener(ActionListener listener) {
        simulationControlsPanel.addCommandListener(listener);
    }

    /**
     * Delegates the enabling/disabling of simulation control buttons to the simulation controls panel.
     *
     * @param newEnabled        whether the "New" button should be enabled.
     * @param startEnabled      whether the "Start" button should be enabled.
     * @param pauseEnabled      whether the "Pause" button should be enabled.
     * @param resumeEnabled     whether the "Resume" button should be enabled.
     * @param stopEnabled       whether the "Stop" button should be enabled.
     * @param saveImagesEnabled whether the "Save Images" button should be enabled.
     */
    public void setSimulationControls(
            boolean newEnabled,
            boolean startEnabled,
            boolean pauseEnabled,
            boolean resumeEnabled,
            boolean stopEnabled,
            boolean saveImagesEnabled) {

        simulationControlsPanel.setSimulationControls(
                newEnabled,
                startEnabled,
                pauseEnabled,
                resumeEnabled,
                stopEnabled,
                saveImagesEnabled);
    }

    /**
     * Enables or disables the map loading button.
     *
     * @param enabled whether the Load button should be enabled
     */
    public void setMapLoadControlsEnabled(boolean enabled) {

        simulationControlsPanel.setMapLoadControlsEnabled(enabled);
    }

    /**
     * Delegates the enabling/disabling of saving controls to the simulation controls panel.
     *
     * @param saving whether the saving controls should be enabled.
     */
    public void setSavingControls(boolean saving) {
        simulationControlsPanel.setSavingControls(saving);
    }

    /**
     * Replaces the map image, attribution, and overlay data as one presentation
     * update.
     *
     * @param mapImage          the image of the map.
     * @param attributionText   the attribution text for the map.
     * @param tasks             the list of tasks to be displayed on the map.
     * @param drones            the list of drones to be displayed on the map.
     */
    public void displayMap(
            BufferedImage mapImage,
            String attributionText,
            List<MapTaskViewData> tasks,
            List<MapDroneViewData> drones) {

        mapPanel.displayMap(mapImage, attributionText, tasks, drones);
    }

    /**
     * Delegates the addition of a map selection listener to the map panel.
     *
     * @param listener the listener to be added.
     */
    public void addMapSelectionListener(Consumer<MapSelection> listener) {
        mapPanel.addMapSelectionListener(listener);
    }

    /**
     * Delegates the addition of a map mouse position listener to the map panel.
     *
     * @param listener listener receiving map mouse positions in display pixels,
     *                 or null when the mouse exits the map
     */
    public void addMapMousePositionListener(Consumer<Vector2D> listener) {

        mapPanel.addMapMousePositionListener(listener);
    }

    /**
     * Delegates the selection of a drone by name to the map panel.
     *
     * @param droneName the name of the drone to be selected.
     */
    public void selectDroneByName(String droneName) {
        mapPanel.selectDroneByName(droneName);
    }

    /**
     * Delegates the selection of a task by name to the map panel.
     *
     * @param taskName the name of the task to be selected.
     */
    public void selectTaskByName(String taskName) {
        mapPanel.selectTaskByName(taskName);
    }

    /**
     * Delegates the clearing of map symbols to the map panel.
     */
    public void clearMapSymbols() {
        mapPanel.clearSymbols();
    }

    /**
     * Delegates the addition of a task thumbnail selection listener to the task thumbnail strip panel.
     *
     * @param listener the listener to be added.
     */
    public void addTaskThumbnailSelectionListener(Consumer<String> listener) {
        taskThumbnailStripPanel.addTaskThumbnailSelectionListener(listener);
    }

    /**
     * Delegates the display of task thumbnails to the task thumbnail strip panel.
     *
     * @param thumbnails        the list of task thumbnail snapshots to be displayed.
     * @param selectedTaskName  the name of the task to be selected.
     */
    public void displayTaskThumbnails(
            List<TaskThumbnailSnapshot> thumbnails,
            String selectedTaskName) {

        taskThumbnailStripPanel.displayTaskThumbnails(thumbnails, selectedTaskName);
    }

    /**
     * Delegates the clearing of task thumbnails to the task thumbnail strip panel.
     */
    public void clearTaskThumbnails() {
        taskThumbnailStripPanel.clear();
    }
}
