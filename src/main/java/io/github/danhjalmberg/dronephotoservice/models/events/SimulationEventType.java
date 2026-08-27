package io.github.danhjalmberg.dronephotoservice.models.events;

/**
 * Categorizes lifecycle and operational events emitted by the simulation.
 */
public enum SimulationEventType {

    /**
     * A new simulation run became active.
     */
    SIMULATION_STARTED,
    /**
     * An active simulation was paused.
     */
    SIMULATION_PAUSED,
    /**
     * A paused simulation resumed.
     */
    SIMULATION_RESUMED,
    /**
     * A simulation run stopped.
     */
    SIMULATION_STOPPED,

    /**
     * A photo agency created a task.
     */
    TASK_CREATED,
    /**
     * A task was accepted by the shared task queue.
     */
    TASK_ENQUEUED,
    /**
     * A drone accepted a queued task.
     */
    TASK_ASSIGNED,
    /**
     * A drone began performing its assigned task.
     */
    TASK_STARTED,
    /**
     * A drone completed a task.
     */
    TASK_COMPLETED,
    /**
     * A task attempt ended before completion.
     */
    TASK_ABORTED,
    /**
     * A previously aborted task was accepted by the queue again.
     */
    TASK_REQUEUED,
    /**
     * A completed task was added to the task archive.
     */
    TASK_ARCHIVED,

    /**
     * A drone began returning to its base position.
     */
    DRONE_RETURNING_TO_BASE,
    /**
     * A drone began charging at its base position.
     */
    DRONE_CHARGING,

    /**
     * A task could not be enqueued because the queue was full.
     */
    QUEUE_FULL
}
