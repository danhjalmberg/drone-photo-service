package io.github.danhjalmberg.dronephotoservice.views.viewdata;

import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;

import java.util.List;

/**
 * Supplies the presentation data required to draw one drone on the map.
 *
 * <p>All positions, including those nested in task data, are display coordinates
 * in pixels relative to the map image. The assigned task may be {@code null}.
 * The completed-task list is copied at construction and exposed as an
 * unmodifiable list.</p>
 *
 * @author Dan Hjälmberg
 */
public final class MapDroneViewData {

    private final Vector2D basePosition;
    private final Vector2D currentPosition;
    private final String name;
    private final MapTaskViewData assignedTask;
    private final List<MapTaskViewData> completedTasks;

    /**
     * Creates map presentation data for a drone.
     *
     * @param basePosition    drone base in map-image pixels
     * @param currentPosition current drone position in map-image pixels
     * @param name            drone name used for labels and selection
     * @param assignedTask    assigned task data, or {@code null}
     * @param completedTasks  completed tasks drawn for this drone
     * @throws NullPointerException if {@code completedTasks} or one of its
     *                              elements is {@code null}
     */
    public MapDroneViewData(Vector2D basePosition,
                            Vector2D currentPosition,
                            String name,
                            MapTaskViewData assignedTask,
                            List<MapTaskViewData> completedTasks) {
        this.basePosition = basePosition;
        this.currentPosition = currentPosition;
        this.name = name;
        this.assignedTask = assignedTask;
        this.completedTasks = List.copyOf(completedTasks);
    }

    /**
     * Returns the drone's base position on the displayed map.
     *
     * @return drone base in map-image pixels
     */
    public Vector2D getBasePosition() {
        return basePosition;
    }

    /**
     * Returns the drone's current position on the displayed map.
     *
     * @return current drone position in map-image pixels
     */
    public Vector2D getCurrentPosition() {
        return currentPosition;
    }

    /**
     * Returns the drone name used by the map view.
     *
     * @return drone name used for labels and selection
     */
    public String getName() {
        return name;
    }

    /**
     * Returns presentation data for the drone's currently assigned task.
     *
     * @return assigned task data, or {@code null} if no task is assigned
     */
    public MapTaskViewData getAssignedTask() {
        return assignedTask;
    }

    /**
     * Returns the completed-task presentation data associated with this drone.
     *
     * @return unmodifiable completed-task list
     */
    public List<MapTaskViewData> getCompletedTasks() {
        return completedTasks;
    }
}
