package io.github.danhjalmberg.dronephotoservice.views.viewdata;

import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;
import io.github.danhjalmberg.dronephotoservice.models.tasks.TaskType;

import java.util.ArrayList;
import java.util.List;

/**
 * Supplies the presentation data required to draw one task on the map.
 *
 * <p>The target and captured-image positions are display coordinates in pixels
 * relative to the map image. Type and image positions are needed for completed
 * task rendering, such as video trails, and may be omitted for queued or assigned
 * tasks. Image-position lists are defensively copied on input and output.</p>
 *
 * @author Dan Hjälmberg
 */
public final class MapTaskViewData {

    private final Vector2D targetPosition;
    private final String name;
    private final TaskType type;
    private final List<Vector2D> imagePositions;

    /**
     * Creates basic task data without a type or captured-image positions.
     *
     * @param targetPosition task target in map-image pixels
     * @param name           task name used for labels and selection
     */
    public MapTaskViewData(
            Vector2D targetPosition,
            String name) {

        this(targetPosition, name, null, List.of());
    }

    /**
     * Creates task data including its application-defined type and captured-image
     * positions. A {@code null} position list is treated as empty.
     *
     * @param targetPosition task target in map-image pixels
     * @param name           task name used for labels and selection
     * @param type           application-defined task type used by map rendering
     * @param imagePositions captured-image positions in map-image pixels, or
     *                       {@code null}
     */
    public MapTaskViewData(
            Vector2D targetPosition,
            String name,
            TaskType type,
            List<Vector2D> imagePositions) {

        this.targetPosition = targetPosition;
        this.name = name;
        this.type = type;
        this.imagePositions = imagePositions == null
                ? List.of()
                : new ArrayList<>(imagePositions);
    }

    /**
     * Returns the task's target position on the displayed map.
     *
     * @return task target in map-image pixels
     */
    public Vector2D getTargetPosition() {
        return targetPosition;
    }

    /**
     * Returns the task name used by the map view.
     *
     * @return task name used for labels and selection
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the task type used to select its map presentation.
     *
     * @return application-defined task type, or {@code null} when omitted
     */
    public TaskType getType() {
        return type;
    }

    /**
     * Returns the positions at which the task captured its images.
     *
     * <p>The returned list is a defensive copy and may be modified without
     * changing this view-data instance.</p>
     *
     * @return defensive copy of captured-image positions in map-image pixels
     */
    public List<Vector2D> getImagePositions() {
        return new ArrayList<>(imagePositions);
    }
}
