package io.github.danhjalmberg.dronephotoservice.views.components;

import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;
import io.github.danhjalmberg.dronephotoservice.settings.ModelSettings;
import io.github.danhjalmberg.dronephotoservice.settings.ViewSettings;
import io.github.danhjalmberg.dronephotoservice.views.support.MapSymbolPainter;
import io.github.danhjalmberg.dronephotoservice.views.viewdata.MapDroneViewData;
import io.github.danhjalmberg.dronephotoservice.views.viewdata.MapSelection;
import io.github.danhjalmberg.dronephotoservice.views.viewdata.MapTaskViewData;
import io.github.danhjalmberg.dronephotoservice.models.tasks.TaskType;

import javax.swing.JComponent;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Renders the map image and its interactive simulation overlay.
 *
 * <p>Task and drone positions are map-image pixel coordinates. This component
 * adds the image's centering offset for painting and hit testing, reports mouse
 * positions relative to the image, and emits typed selections for drones and
 * completed tasks. Queued and assigned tasks are visual only.</p>
 *
 * @author Dan Hjälmberg
 */
public class MapComponent extends JComponent {

    private BufferedImage mapImage;
    private List<MapTaskViewData> tasks;
    private List<MapDroneViewData> drones;

    private String hoveredDroneName;
    private String selectedDroneName;

    private String hoveredCompletedTaskName;
    private String selectedCompletedTaskName;

    private boolean showLabels = true;
    private boolean showVideoTrails = true;

    private Consumer<MapSelection> mapSelectionListener;
    private Consumer<Vector2D> mousePositionListener;

    private String attributionText;

    /**
     * Creates an empty map component and installs mouse interaction handling.
     */
    public MapComponent() {


        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent event) {

                MapSelection selection = findSelectionAt(event.getPoint());

                if (mapSelectionListener != null) {
                    mapSelectionListener.accept(selection);
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hoveredDroneName = null;
                hoveredCompletedTaskName = null;
                setCursor(Cursor.getDefaultCursor());

                if (mousePositionListener != null) {
                    mousePositionListener.accept(null);
                }

                repaint();
            }
        });

        addMouseMotionListener(new MouseAdapter() {

            @Override
            public void mouseMoved(MouseEvent event) {

                if (mousePositionListener != null) {
                    mousePositionListener.accept(toMapDisplayPosition(event.getPoint()));
                }

                MapSelection selection = findSelectionAt(event.getPoint());

                updateCursor(selection);

                String droneName = selection.getType() == MapSelection.Type.DRONE
                        ? selection.getName()
                        : null;

                String completedTaskName = selection.getType() == MapSelection.Type.COMPLETED_TASK
                        ? selection.getName()
                        : null;

                boolean changed =
                        !Objects.equals(hoveredDroneName, droneName)
                                || !Objects.equals(hoveredCompletedTaskName, completedTaskName);

                if (changed) {
                    hoveredDroneName = droneName;
                    hoveredCompletedTaskName = completedTaskName;
                    repaint();
                }
            }
        });
    }

    /**
     * Updates the mouse cursor according to the current map interaction state.
     *
     * @param selection current map selection under the mouse
     */
    private void updateCursor(MapSelection selection) {

        if (mapImage == null) {
            setCursor(Cursor.getDefaultCursor());
        } else if (selection.isNone()) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    /**
     * Returns the loaded image size or the configured default map size.
     *
     * @return map canvas size in pixels
     */
    private Dimension getMapCanvasSize() {
        if (mapImage == null) {
            return new Dimension(
                    ModelSettings.MAP_IMAGE_RESAMPLED_WIDTH,
                    ModelSettings.MAP_IMAGE_RESAMPLED_HEIGHT);
        }

        return new Dimension(
                mapImage.getWidth(),
                mapImage.getHeight());
    }

    /**
     * Returns the preferred size of this component.
     *
     * @return current map canvas size
     */
    @Override
    public Dimension getPreferredSize() {
        return getMapCanvasSize();
    }

    /**
     * Returns the minimum size of this component.
     *
     * @return current map canvas size
     */
    @Override
    public Dimension getMinimumSize() {
        return getMapCanvasSize();
    }

    /**
     * Returns the maximum size of this component.
     *
     * @return current map canvas size
     */
    @Override
    public Dimension getMaximumSize() {
        return getMapCanvasSize();
    }


    /**
     * Paints the centered map, simulation symbols, optional labels and trails,
     * hover/selection highlights, and attribution.
     *
     * @param g Swing graphics context
     */
    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        try {
            configureGraphics(g2d);

            if (mapImage == null) {
                drawNoMapLoadedMessage(g2d);
                return;
            }

            // Set font for map symbols
            g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 10f));

            int mapOffsetX = getMapOffsetX();
            int mapOffsetY = getMapOffsetY();

            drawMapImage(g2d, mapImage, mapOffsetX, mapOffsetY);
            drawQueuedTasks(g2d, tasks, mapOffsetX, mapOffsetY, showLabels);
            drawDrones(g2d, drones, mapOffsetX, mapOffsetY);
            drawAttribution(g2d, mapOffsetX, mapOffsetY);

        } finally {
            g2d.dispose();
        }
    }

    /**
     * Configures rendering hints used for the map and its overlay.
     *
     * @param g2d graphics context to configure
     */
    private void configureGraphics(Graphics2D g2d) {

        g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setRenderingHint(
                RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        g2d.setRenderingHint(
                RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);

        g2d.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g2d.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
    }

    /**
     * Draws the map image at its centered component position.
     *
     * @param g2d        graphics context
     * @param image      map image to draw
     * @param mapOffsetX horizontal image-centering offset
     * @param mapOffsetY vertical image-centering offset
     */
    private void drawMapImage(
            Graphics2D g2d,
            BufferedImage image,
            int mapOffsetX,
            int mapOffsetY) {

        // Set alpha to darken map image, for enhanced visibility of symbols
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));

        g2d.drawImage(image, mapOffsetX, mapOffsetY, null);

        // Reset alpha after the map has been rendered, to get full visibility of symbols
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    /**
     * Draws all tasks currently waiting in the task queue.
     *
     * @param g2d           graphics context
     * @param queuedTasks   queued-task presentation data, or {@code null}
     * @param mapOffsetX    horizontal image-centering offset
     * @param mapOffsetY    vertical image-centering offset
     * @param labelsVisible whether labels should be drawn
     */
    private void drawQueuedTasks(
            Graphics2D g2d,
            List<MapTaskViewData> queuedTasks,
            int mapOffsetX,
            int mapOffsetY,
            boolean labelsVisible) {

        if (queuedTasks == null) {
            return;
        }

        int radius = ViewSettings.POINT_RADIUS;

        for (MapTaskViewData task : queuedTasks) {
            Vector2D taskPosition = task.getTargetPosition();

            double taskX = mapOffsetX + taskPosition.getX();
            double taskY = mapOffsetY + taskPosition.getY();

            MapSymbolPainter.drawEnqueuedTaskSymbol(g2d, taskX, taskY);

            if (labelsVisible) {
                drawLabel(g2d, task.getName(), taskX + 2 * radius, taskY + 2 * radius);
            }
        }
    }

    /**
     * Draws all drones and their associated task information.
     *
     * @param g2d        graphics context
     * @param droneData  drone presentation data, or {@code null}
     * @param mapOffsetX horizontal image-centering offset
     * @param mapOffsetY vertical image-centering offset
     */
    private void drawDrones(
            Graphics2D g2d,
            List<MapDroneViewData> droneData,
            int mapOffsetX,
            int mapOffsetY) {

        if (droneData == null) {
            return;
        }

        for (MapDroneViewData drone : droneData) {
            drawDrone(g2d, drone, mapOffsetX, mapOffsetY);
        }
    }

    /**
     * Draws a drone, its base, and its associated tasks.
     *
     * @param g2d        graphics context
     * @param drone      drone presentation data
     * @param mapOffsetX horizontal image-centering offset
     * @param mapOffsetY vertical image-centering offset
     */
    private void drawDrone(
            Graphics2D g2d,
            MapDroneViewData drone,
            int mapOffsetX,
            int mapOffsetY) {

        drawDroneBase(g2d, drone, mapOffsetX, mapOffsetY);

        Vector2D currentPosition = drone.getCurrentPosition();
        double droneX = mapOffsetX + currentPosition.getX();
        double droneY = mapOffsetY + currentPosition.getY();

        drawDroneHighlights(g2d, drone, droneX, droneY);
        MapSymbolPainter.drawDroneSymbol(g2d, droneX, droneY);

        if (showLabels) {
            int radius = ViewSettings.POINT_RADIUS;
            drawLabel(g2d, drone.getName(), droneX + radius, droneY - radius);
        }

        drawAssignedTask(g2d, drone, mapOffsetX, mapOffsetY);
        drawCompletedTasks(g2d, drone, mapOffsetX, mapOffsetY);
    }

    /**
     * Draws the base position belonging to a drone.
     *
     * @param g2d        graphics context
     * @param drone      drone presentation data
     * @param mapOffsetX horizontal image-centering offset
     * @param mapOffsetY vertical image-centering offset
     */
    private void drawDroneBase(
            Graphics2D g2d,
            MapDroneViewData drone,
            int mapOffsetX,
            int mapOffsetY) {

        Vector2D basePosition = drone.getBasePosition();

        double baseX = mapOffsetX + basePosition.getX();
        double baseY = mapOffsetY + basePosition.getY();

        MapSymbolPainter.drawBaseSymbol(g2d, baseX, baseY);

        if (showLabels) {
            drawLabel(g2d, "base position", baseX + 12, baseY + 12);
        }
    }

    /**
     * Draws the applicable hover or selection highlight for a drone.
     *
     * <p>The hover highlight is omitted when the drone is selected to avoid
     * displaying multiple highlights around the same symbol.</p>
     *
     * @param g2d    graphics context
     * @param drone  drone presentation data
     * @param droneX drone x coordinate in the component
     * @param droneY drone y coordinate in the component
     */
    private void drawDroneHighlights(
            Graphics2D g2d,
            MapDroneViewData drone,
            double droneX,
            double droneY) {

        String droneName = drone.getName();

        if (droneName.equals(hoveredDroneName)
                && !droneName.equals(selectedDroneName)) {

            MapSymbolPainter.drawHighlight(
                    g2d,
                    droneX,
                    droneY,
                    ViewSettings.HOVERED_DRONE_HIGHLIGHT_RADIUS,
                    ViewSettings.HOVERED_DRONE_HIGHLIGHT_FILL_COLOR,
                    ViewSettings.HOVERED_DRONE_HIGHLIGHT_BORDER_COLOR);
        }

        if (droneName.equals(selectedDroneName)) {
            MapSymbolPainter.drawHighlight(
                    g2d,
                    droneX,
                    droneY,
                    ViewSettings.SELECTED_DRONE_HIGHLIGHT_RADIUS,
                    ViewSettings.SELECTED_DRONE_HIGHLIGHT_FILL_COLOR,
                    ViewSettings.SELECTED_DRONE_HIGHLIGHT_BORDER_COLOR);
        }
    }

    /**
     * Draws the task currently assigned to a drone.
     *
     * @param g2d        graphics context
     * @param drone      drone presentation data
     * @param mapOffsetX horizontal image-centering offset
     * @param mapOffsetY vertical image-centering offset
     */
    private void drawAssignedTask(
            Graphics2D g2d,
            MapDroneViewData drone,
            int mapOffsetX,
            int mapOffsetY) {

        MapTaskViewData assignedTask = drone.getAssignedTask();

        if (assignedTask == null) {
            return;
        }

        Vector2D taskPosition = assignedTask.getTargetPosition();

        double taskX = mapOffsetX + taskPosition.getX();
        double taskY = mapOffsetY + taskPosition.getY();

        MapSymbolPainter.drawAssignedTaskSymbol(g2d, taskX, taskY);

        if (showLabels) {
            int radius = ViewSettings.POINT_RADIUS;
            drawLabel(
                    g2d,
                    assignedTask.getName(),
                    taskX + 2 * radius,
                    taskY + 2 * radius);
        }
    }

    /**
     * Draws all tasks completed by a drone in their presentation order.
     *
     * @param g2d        graphics context
     * @param drone      drone presentation data
     * @param mapOffsetX horizontal image-centering offset
     * @param mapOffsetY vertical image-centering offset
     */
    private void drawCompletedTasks(
            Graphics2D g2d,
            MapDroneViewData drone,
            int mapOffsetX,
            int mapOffsetY) {

        // Iterate over completed tasks stored in drone memory
        for (MapTaskViewData completedTask : drone.getCompletedTasks()) {
            drawCompletedTask(
                    g2d,
                    completedTask,
                    mapOffsetX,
                    mapOffsetY);
        }
    }

    /**
     * Draws a completed task, including its trail and interaction highlights.
     *
     * @param g2d           graphics context
     * @param completedTask completed-task presentation data
     * @param mapOffsetX    horizontal image-centering offset
     * @param mapOffsetY    vertical image-centering offset
     */
    private void drawCompletedTask(
            Graphics2D g2d,
            MapTaskViewData completedTask,
            int mapOffsetX,
            int mapOffsetY) {

        Vector2D taskPosition = completedTask.getTargetPosition();

        double taskX = mapOffsetX + taskPosition.getX();
        double taskY = mapOffsetY + taskPosition.getY();

        drawVideoTrail(g2d, completedTask, mapOffsetX, mapOffsetY);
        drawCompletedTaskHighlights(g2d, completedTask, taskX, taskY);
        MapSymbolPainter.drawCompletedTaskSymbol(g2d, taskX, taskY);

        if (showLabels) {
            int radius = ViewSettings.POINT_RADIUS;
            drawLabel(
                    g2d,
                    completedTask.getName(),
                    taskX + 2 * radius,
                    taskY + 2 * radius);
        }
    }

    /**
     * Draws the recorded image positions for a completed video task.
     *
     * @param g2d           graphics context
     * @param completedTask completed-task presentation data
     * @param mapOffsetX    horizontal image-centering offset
     * @param mapOffsetY    vertical image-centering offset
     */
    private void drawVideoTrail(
            Graphics2D g2d,
            MapTaskViewData completedTask,
            int mapOffsetX,
            int mapOffsetY) {

        if (!showVideoTrails || completedTask.getType() != TaskType.VIDEO) {
            return;
        }

        for (Vector2D imagePosition : completedTask.getImagePositions()) {
            double imageX = mapOffsetX + imagePosition.getX();
            double imageY = mapOffsetY + imagePosition.getY();

            MapSymbolPainter.drawVideoTrailPoint(g2d, imageX, imageY);
        }
    }

    /**
     * Draws the applicable hover or selection highlight for a completed task.
     *
     * <p>The hover highlight is omitted when the task is selected to avoid
     * displaying multiple highlights around the same symbol.</p>
     *
     * @param g2d           graphics context
     * @param completedTask completed-task presentation data
     * @param taskX         task x coordinate in the component
     * @param taskY         task y coordinate in the component
     */
    private void drawCompletedTaskHighlights(
            Graphics2D g2d,
            MapTaskViewData completedTask,
            double taskX,
            double taskY) {

        String taskName = completedTask.getName();

        if (taskName.equals(hoveredCompletedTaskName)
                && !taskName.equals(selectedCompletedTaskName)) {

            MapSymbolPainter.drawHighlight(
                    g2d,
                    taskX,
                    taskY,
                    ViewSettings.HOVERED_TASK_HIGHLIGHT_RADIUS,
                    ViewSettings.HOVERED_TASK_HIGHLIGHT_FILL_COLOR,
                    ViewSettings.HOVERED_TASK_HIGHLIGHT_BORDER_COLOR);
        }

        if (taskName.equals(selectedCompletedTaskName)) {
            MapSymbolPainter.drawHighlight(
                    g2d,
                    taskX,
                    taskY,
                    ViewSettings.SELECTED_TASK_HIGHLIGHT_RADIUS,
                    ViewSettings.SELECTED_TASK_HIGHLIGHT_FILL_COLOR,
                    ViewSettings.SELECTED_TASK_HIGHLIGHT_BORDER_COLOR);
        }
    }

    /**
     * Replaces the displayed map image and updates component layout.
     *
     * @param mapImage map image, or {@code null} to show the empty-map state
     */
    public void setMapImage(BufferedImage mapImage) {

        this.mapImage = mapImage;
        revalidate();
        repaint();
    }

    /**
     * Replaces queued-task presentation data without repainting immediately.
     * The normal batched update assigns drones next, which triggers the repaint.
     *
     * @param tasks queued-task data, or {@code null}
     */
    public void setTasks(List<MapTaskViewData> tasks) {
        this.tasks = tasks;
    }

    /**
     * Replaces drone presentation data and repaints the overlay.
     *
     * @param drones drone data, or {@code null}
     */
    public void setDrones(List<MapDroneViewData> drones) {
        this.drones = drones;
        repaint();
    }

    /**
     * Changes the drone highlight and repaints the map.
     *
     * @param selectedDroneName selected drone name, or {@code null}
     */
    public void setSelectedDroneName(String selectedDroneName) {
        this.selectedDroneName = selectedDroneName;
        repaint();
    }

    /**
     * Changes the completed-task highlight and repaints the map.
     *
     * @param selectedCompletedTaskName selected task name, or {@code null}
     */
    public void setSelectedCompletedTaskName(String selectedCompletedTaskName) {
        this.selectedCompletedTaskName = selectedCompletedTaskName;
        repaint();
    }

    /**
     * Sets the callback for map clicks. Every click produces a value, including
     * {@link MapSelection.Type#NONE} when no selectable symbol was hit.
     *
     * @param mapSelectionListener callback to replace, or {@code null}
     */
    public void setMapSelectionListener(Consumer<MapSelection> mapSelectionListener) {
        this.mapSelectionListener = mapSelectionListener;
    }

    /**
     * Sets the callback for mouse movement over the displayed map image.
     *
     * @param mousePositionListener listener receiving map display coordinates,
     *                              or {@code null} outside the image
     */
    public void setMousePositionListener(Consumer<Vector2D> mousePositionListener) {
        this.mousePositionListener = mousePositionListener;
    }

    /**
     * Sets whether map symbol labels should be drawn.
     *
     * @param showLabels whether labels should be shown
     */
    public void setShowLabels(boolean showLabels) {
        this.showLabels = showLabels;
        repaint();
    }

    /**
     * Sets whether completed video task trails should be drawn on the map.
     *
     * @param showVideoTrails whether completed video trails should be shown
     */
    public void setShowVideoTrails(boolean showVideoTrails) {
        this.showVideoTrails = showVideoTrails;
        repaint();
    }

    /**
     * Clears task and drone data while retaining the map image and display options.
     */
    public void clearSymbols() {
        this.tasks = null;
        this.drones = null;
        repaint();
    }

    /**
     * Sets the attribution text displayed on top of the map image.
     *
     * <p>Blank text is normalized to {@code null}.</p>
     *
     * @param attributionText attribution text, or {@code null}
     */
    public void setAttributionText(String attributionText) {
        this.attributionText =
                attributionText == null || attributionText.isBlank()
                        ? null
                        : attributionText.trim();

        repaint();
    }

    /**
     * @return horizontal image-centering offset in component pixels
     */
    private int getMapOffsetX() {
        if (mapImage == null) {
            return 0;
        }
        return (this.getWidth() - mapImage.getWidth()) / 2;
    }

    /**
     * @return vertical image-centering offset in component pixels
     */
    private int getMapOffsetY() {
        if (mapImage == null) {
            return 0;
        }
        return (this.getHeight() - mapImage.getHeight()) / 2;
    }

    /**
     * Converts a component mouse point to map display coordinates.
     * The returned coordinates are relative to the displayed map image, not to the
     * full Swing component. If the mouse is outside the displayed map image, null
     * is returned.
     *
     * @param point mouse point in component coordinates
     * @return mouse point relative to the map image, or null if outside the map
     */
    private Vector2D toMapDisplayPosition(Point point) {

        if (mapImage == null) {
            return null;
        }

        int mapOffsetX = getMapOffsetX();
        int mapOffsetY = getMapOffsetY();

        double mapX = point.getX() - mapOffsetX;
        double mapY = point.getY() - mapOffsetY;

        if (mapX < 0
                || mapY < 0
                || mapX >= mapImage.getWidth()
                || mapY >= mapImage.getHeight()) {
            return null;
        }

        return new Vector2D(mapX, mapY);
    }

    /**
     * Hit-tests selectable symbols, giving drones priority over completed tasks.
     *
     * @param point point in component coordinates
     * @return typed selection, or a no-selection value
     */
    private MapSelection findSelectionAt(Point point) {

        String droneName = findDroneNameAt(point);

        if (droneName != null) {
            return MapSelection.drone(droneName);
        }

        String completedTaskName = findCompletedTaskNameAt(point);

        if (completedTaskName != null) {
            return MapSelection.completedTask(completedTaskName);
        }

        return MapSelection.none();
    }

    /**
     * Finds the topmost drone hit by searching presentation order backwards.
     *
     * @param point point in component coordinates
     * @return drone name, or {@code null}
     */
    private String findDroneNameAt(Point point) {

        if (drones == null) {
            return null;
        }

        int mapOffsetX = getMapOffsetX();
        int mapOffsetY = getMapOffsetY();

        for (int i = drones.size() - 1; i >= 0; i--) {

            MapDroneViewData drone = drones.get(i);

            double droneX = mapOffsetX + drone.getCurrentPosition().getX();
            double droneY = mapOffsetY + drone.getCurrentPosition().getY();

            if (point.distance(droneX, droneY) <= ViewSettings.DRONE_HITBOX_RADIUS) {
                return drone.getName();
            }
        }

        return null;
    }

    /**
     * Finds the topmost completed task by searching drones and their task lists
     * backwards.
     *
     * @param point point in component coordinates
     * @return completed-task name, or {@code null}
     */
    private String findCompletedTaskNameAt(Point point) {

        if (drones == null) {
            return null;
        }

        int mapOffsetX = getMapOffsetX();
        int mapOffsetY = getMapOffsetY();

        for (int i = drones.size() - 1; i >= 0; i--) {

            MapDroneViewData drone = drones.get(i);

            List<MapTaskViewData> completedTasks = drone.getCompletedTasks();

            for (int j = completedTasks.size() - 1; j >= 0; j--) {

                MapTaskViewData task = completedTasks.get(j);

                double taskX = mapOffsetX + task.getTargetPosition().getX();
                double taskY = mapOffsetY + task.getTargetPosition().getY();

                if (point.distance(taskX, taskY) <= ViewSettings.COMPLETED_TASK_HITBOX_RADIUS) {
                    return task.getName();
                }
            }
        }
        return null;
    }

    /**
     * Draws a centered fallback message when no map image is loaded.
     *
     * @param g2d graphics context
     */
    private void drawNoMapLoadedMessage(Graphics2D g2d) {

        String text = "No map loaded";

        g2d.setFont(g2d.getFont().deriveFont(16f));

        FontMetrics metrics = g2d.getFontMetrics();
        int textWidth = metrics.stringWidth(text);
        int textHeight = metrics.getAscent();

        int x = (getWidth() - textWidth) / 2;
        int y = (getHeight() + textHeight) / 2;

        g2d.setColor(getForeground());
        g2d.drawString(text, x, y);
    }

    /**
     * Draws a label with the given text at the specified coordinates.
     *
     * @param g2d  the graphics context to use for drawing
     * @param text the text to draw
     * @param x    the x coordinate of the label
     * @param y    the y coordinate of the label
     */
    private void drawLabel(Graphics2D g2d, String text, double x, double y) {
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, (float) x, (float) y);
    }

    /**
     * Draws map attribution text in the lower-left corner of the map image.
     *
     * @param g2d        graphics context.
     * @param mapOffsetX x offset of the map image.
     * @param mapOffsetY y offset of the map image.
     */
    private void drawAttribution(
            Graphics2D g2d,
            int mapOffsetX,
            int mapOffsetY) {

        if (mapImage == null || attributionText == null) {
            return;
        }

        int padding = 6;
        int x = mapOffsetX + padding;
        int y = mapOffsetY + mapImage.getHeight() - padding;

        g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 10f));

        // Add a black text shadow behind the text
        g2d.setColor(Color.BLACK);
        g2d.drawString(attributionText, x + 1, y + 1);
        g2d.setColor(Color.WHITE);
        g2d.drawString(attributionText, x, y);
    }
}
