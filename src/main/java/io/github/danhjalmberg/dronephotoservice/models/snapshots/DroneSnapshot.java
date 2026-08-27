package io.github.danhjalmberg.dronephotoservice.models.snapshots;

import io.github.danhjalmberg.dronephotoservice.models.drones.DroneState;
import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;

import java.util.List;

/**
 * Provides the drone data consumed by controllers and views without exposing
 * the drone object itself.
 *
 * <p>All fields describe the drone at creation time. Component state is exposed
 * through immutable value snapshots rather than live simulation components.
 * The supplied completed-task list is retained directly rather than copied by
 * this class.</p>
 *
 * @author Dan Hjälmberg
 */
public final class DroneSnapshot {

    private final String name;
    private final BatterySnapshot battery;
    private final CameraSnapshot camera;
    private final MotorSnapshot motor;

    private final Vector2D basePositionMeters;
    private final Vector2D currentPositionMeters;

    private final DroneState state;

    private final TaskSnapshot assignedTask;
    private final List<TaskSnapshot> completedTasks;

    /**
     * Creates a view of the supplied drone state.
     *
     * @param name                  drone name.
     * @param battery               captured battery state.
     * @param camera                captured camera state.
     * @param motor                 captured motor state.
     * @param basePositionMeters    base position in world meters.
     * @param currentPositionMeters current position in world meters.
     * @param state                 current lifecycle state.
     * @param assignedTask          assigned task, or {@code null} when idle.
     * @param completedTasks        completed-task history retained by reference.
     */
    public DroneSnapshot(
            String name,
            BatterySnapshot battery,
            CameraSnapshot camera,
            MotorSnapshot motor,
            Vector2D basePositionMeters,
            Vector2D currentPositionMeters,
            DroneState state,
            TaskSnapshot assignedTask,
            List<TaskSnapshot> completedTasks) {

        this.name = name;
        this.battery = battery;
        this.camera = camera;
        this.motor = motor;
        this.basePositionMeters = basePositionMeters;
        this.currentPositionMeters = currentPositionMeters;
        this.state = state;
        this.assignedTask = assignedTask;
        this.completedTasks = completedTasks;
    }

    /**
     * Returns the drone name.
     *
     * @return the name of the drone.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the battery state captured for this drone.
     *
     * @return immutable battery snapshot.
     */
    public BatterySnapshot getBattery() {
        return battery;
    }

    /**
     * Returns the camera state captured for this drone.
     *
     * @return immutable camera snapshot.
     */
    public CameraSnapshot getCamera() {
        return camera;
    }

    /**
     * Returns the motor state captured for this drone.
     *
     * @return immutable motor snapshot.
     */
    public MotorSnapshot getMotor() {
        return motor;
    }

    /**
     * Returns the drone base position in world meters.
     *
     * @return base position in meters.
     */
    public Vector2D getBasePositionMeters() {
        return basePositionMeters;
    }

    /**
     * Returns the current drone position in world meters.
     *
     * @return current position in meters.
     */
    public Vector2D getCurrentPositionMeters() {
        return currentPositionMeters;
    }

    /**
     * Returns the lifecycle state captured for the drone.
     *
     * @return the current state of the drone.
     */
    public DroneState getState() {
        return state;
    }

    /**
     * Returns the task assigned when this object was created.
     *
     * @return assigned task, or {@code null} when no task was assigned.
     */
    public TaskSnapshot getAssignedTask() {
        return assignedTask;
    }

    /**
     * Returns the completed-task list supplied at construction.
     *
     * <p>The list is not copied or wrapped by this class.</p>
     *
     * @return the list of tasks completed by the drone.
     */
    public List<TaskSnapshot> getCompletedTasks() {
        return completedTasks;
    }
}
