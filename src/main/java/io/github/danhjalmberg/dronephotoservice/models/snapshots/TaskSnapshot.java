package io.github.danhjalmberg.dronephotoservice.models.snapshots;

import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;
import io.github.danhjalmberg.dronephotoservice.models.tasks.TaskType;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable positional summary of a task.
 *
 * <p>The target and capture positions are expressed in world meters. Capture
 * positions are copied on input and output; {@link Vector2D} is itself
 * immutable.</p>
 *
 * @author Dan Hjälmberg
 */
public final class TaskSnapshot {

    private final String name;
    private final TaskType type;
    private final Vector2D targetPositionMeters;
    private final List<Vector2D> imagePositionsMeters;

    /**
     * Creates a task summary without image capture positions.
     *
     * @param name                 task name.
     * @param type                 application-defined task type.
     * @param targetPositionMeters target position in world meters.
     */
    public TaskSnapshot(
            String name,
            TaskType type,
            Vector2D targetPositionMeters) {

        this(name, type, targetPositionMeters, List.of());
    }

    /**
     * Creates a task summary with image capture positions.
     *
     * @param name                 task name.
     * @param type                 application-defined task type.
     * @param targetPositionMeters target position in world meters.
     * @param imagePositionsMeters capture positions in world meters; {@code null}
     *                             is treated as an empty list.
     */
    public TaskSnapshot(
            String name,
            TaskType type,
            Vector2D targetPositionMeters,
            List<Vector2D> imagePositionsMeters) {

        this.name = name;
        this.type = type;
        this.targetPositionMeters = targetPositionMeters;
        this.imagePositionsMeters = imagePositionsMeters == null
                ? List.of()
                : new ArrayList<>(imagePositionsMeters);
    }

    /**
     * Returns the task name.
     *
     * @return the name of the task.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the application-defined task type.
     *
     * @return the type of the task.
     */
    public TaskType getType() {
        return type;
    }

    /**
     * Returns the target position in world meters.
     *
     * @return task position in meters.
     */
    public Vector2D getTargetPositionMeters() {
        return targetPositionMeters;
    }

    /**
     * Returns a copy of the image capture positions.
     *
     * @return image capture positions in world meters.
     */
    public List<Vector2D> getImagePositionsMeters() {
        return new ArrayList<>(imagePositionsMeters);
    }
}
