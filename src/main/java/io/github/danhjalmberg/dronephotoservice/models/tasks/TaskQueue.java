package io.github.danhjalmberg.dronephotoservice.models.tasks;

import io.github.danhjalmberg.dronephotoservice.settings.ModelSettings;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Provides the bounded FIFO queue shared by task producers and drones.
 *
 * <p>Insertion is non-blocking: a producer receives {@code false} when the
 * queue is full. Retrieval is also non-blocking and returns {@code null} when
 * no task is available. The backing queue can be replaced before a simulation
 * run to apply a different capacity.</p>
 *
 * @author Dan Hjälmberg
 */
public final class TaskQueue {

    /**
     * Shared application task queue.
     */
    public static final TaskQueue INSTANCE = new TaskQueue();

    private volatile LinkedBlockingQueue<Task> tasks =
            new LinkedBlockingQueue<>(ModelSettings.TASK_QUEUE_SIZE_DEFAULT);


    /**
     * Prevents construction outside this singleton class.
     */
    private TaskQueue() {
    }

    /**
     * Replaces the backing queue with an empty queue of the given capacity.
     *
     * <p>Any currently queued tasks are discarded. This operation is intended
     * for configuration before a simulation starts.</p>
     *
     * @param capacity maximum number of queued tasks.
     * @throws IllegalArgumentException if {@code capacity} is not positive.
     */
    public synchronized void resetCapacity(int capacity) {
        tasks = new LinkedBlockingQueue<>(capacity);
    }

    /**
     * Attempts to append a task without waiting for capacity.
     *
     * @param task task to enqueue.
     * @return {@code true} if accepted; {@code false} if the queue is full.
     * @throws NullPointerException if {@code task} is {@code null}.
     */
    public synchronized boolean addTask(Task task) {
        return tasks.offer(task);
    }

    /**
     * Removes and returns the oldest queued task without waiting.
     *
     * @return oldest task, or {@code null} if the queue is empty.
     */
    public synchronized Task getTask() {
        return tasks.poll();
    }

    /**
     * Returns the live backing queue.
     *
     * <p>The returned object is thread-safe, but mutations bypass this class's
     * synchronized operations and directly affect application state. Callers
     * that only need a stable view should make their own copy.</p>
     *
     * @return current backing queue.
     */
    public synchronized LinkedBlockingQueue<Task> getTasks() {
        return tasks;
    }

    /**
     * Reports whether the queue currently contains no tasks.
     *
     * @return {@code true} if the queue is empty.
     */
    public synchronized boolean isEmpty() {
        return tasks.isEmpty();
    }

    /**
     * Removes all queued tasks.
     */
    public synchronized void clear() {
        tasks.clear();
    }
}
