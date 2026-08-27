package io.github.danhjalmberg.dronephotoservice.views.panels;

import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;
import io.github.danhjalmberg.dronephotoservice.settings.ViewSettings;
import io.github.danhjalmberg.dronephotoservice.views.components.MapComponent;
import io.github.danhjalmberg.dronephotoservice.views.components.RoundedPanel;
import io.github.danhjalmberg.dronephotoservice.views.viewdata.MapDroneViewData;
import io.github.danhjalmberg.dronephotoservice.views.viewdata.MapSelection;
import io.github.danhjalmberg.dronephotoservice.views.viewdata.MapTaskViewData;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

/**
 * Hosts the interactive map in a resizable rounded presentation panel.
 *
 * <p>The panel forwards display-pixel overlay data and interaction callbacks to
 * {@link MapComponent}. Supplying a {@code null} image clears overlay symbols
 * while retaining map display preferences.</p>
 */
public class MapPanel extends RoundedPanel {

    private final MapComponent mapComponent;

    /**
     * Creates the map panel with a map component to display the map image,
     * map fallback text, and simulation symbols.
     */
    public MapPanel() {
        super(ViewSettings.PANEL_CORNER_RADIUS);

        Dimension size = new Dimension(
                ViewSettings.MAP_PANEL_WIDTH,
                ViewSettings.MAP_PANEL_HEIGHT);

        setPreferredSize(size);
        setMinimumSize(new Dimension(0, 0));

        setBorder(BorderFactory.createEmptyBorder(
                ViewSettings.PANEL_PADDING_TOP,
                ViewSettings.PANEL_PADDING_LEFT,
                ViewSettings.PANEL_PADDING_BOTTOM,
                ViewSettings.PANEL_PADDING_RIGHT));

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        mapComponent = new MapComponent();

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setBackground(ViewSettings.CARD_BACKGROUND_COLOR);
        wrapper.add(mapComponent);
        add(wrapper);
    }

    /**
     * Updates the map image and map overlays.
     *
     * @param mapImage          the image of the map
     * @param attributionText   the attribution text for the map
     * @param tasks             all tasks in the queue
     * @param drones            all drones
     */
    public void displayMap(
            BufferedImage mapImage,
            String attributionText,
            List<MapTaskViewData> tasks,
            List<MapDroneViewData> drones) {

        mapComponent.setMapImage(mapImage);
        mapComponent.setAttributionText(attributionText);

        if (mapImage == null) {
            mapComponent.clearSymbols();
            return;
        }

        mapComponent.setTasks(tasks);
        mapComponent.setDrones(drones);
    }

    /**
     * Sets the listener receiving the typed result of every map click.
     *
     * @param listener selection callback to replace, or {@code null}
     */
    public void addMapSelectionListener(Consumer<MapSelection> listener) {
        mapComponent.setMapSelectionListener(listener);
    }

    /**
     * Adds a listener that receives map mouse movement positions in display pixels.
     *
     * @param listener listener receiving the mouse position, or null when mouse exits map
     */
    public void addMapMousePositionListener(Consumer<Vector2D> listener) {

        mapComponent.setMousePositionListener(listener);
    }

    /**
     * Sets the drone name used for highlighting. A {@code null} or unknown name
     * produces no visible highlight.
     *
     * @param droneName the name of the drone to be selected
     */
    public void selectDroneByName(String droneName) {
        mapComponent.setSelectedDroneName(droneName);
    }

    /**
     * Sets the completed-task name used for highlighting. A {@code null} or
     * unknown name produces no visible highlight.
     *
     * @param taskName the name of the task to be selected
     */
    public void selectTaskByName(String taskName) {
        mapComponent.setSelectedCompletedTaskName(taskName);
    }

    /**
     * Sets whether to show labels for drones and tasks on the map.
     *
     * @param showLabels true to show labels, false to hide them
     */
    public void setShowLabels(boolean showLabels) {
        mapComponent.setShowLabels(showLabels);
    }

    /**
     * Sets whether completed video task trails should be shown on the map.
     *
     * @param showVideoTrails true to show video trails, false to hide them.
     */
    public void setShowVideoTrails(boolean showVideoTrails) {
        mapComponent.setShowVideoTrails(showVideoTrails);
    }

    /**
     * Clears the map component of all symbols, such as drones and tasks.
     */
    public void clearSymbols() {
        mapComponent.clearSymbols();
    }
}
