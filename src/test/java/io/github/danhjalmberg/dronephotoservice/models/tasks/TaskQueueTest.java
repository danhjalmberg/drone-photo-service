package io.github.danhjalmberg.dronephotoservice.models.tasks;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link TaskQueue} singleton.
 * The tests cover representative FIFO ordering, bounded-capacity behavior,
 * clearing, and capacity reset without attempting multithreaded timing tests.
 *
 * @author Dan Hjälmberg
 */
class TaskQueueTest {

    /**
     * Task queue under test.
     */
    private TaskQueue taskQueue;

    /**
     * Resets the singleton queue before each test.
     */
    @BeforeEach
    void setUp() {
        taskQueue = TaskQueue.INSTANCE;
        taskQueue.resetCapacity(3);
        taskQueue.clear();
    }

    /**
     * Restores the queue to an empty state after each test.
     */
    @AfterEach
    void tearDown() {
        taskQueue.clear();
    }

    /**
     * Tests basic queue storage and retrieval behavior.
     */
    @Nested
    class StorageTests {

        /**
         * Tests that a cleared queue is empty and returns no task.
         */
        @Test
        void clearLeavesQueueEmpty() {
            taskQueue.addTask(createTask(TaskType.PHOTO));

            taskQueue.clear();

            assertTrue(taskQueue.isEmpty());
            assertNull(taskQueue.getTask());
        }

        /**
         * Tests that tasks are retrieved in first-in, first-out order.
         */
        @Test
        void getTaskReturnsTasksInFifoOrder() {
            Task first = createTask(TaskType.PHOTO);
            Task second = createTask(TaskType.VIDEO);

            taskQueue.addTask(first);
            taskQueue.addTask(second);

            assertSame(first, taskQueue.getTask());
            assertSame(second, taskQueue.getTask());
            assertNull(taskQueue.getTask());
        }

        /**
         * Tests that adding a task reports success and makes the queue non-empty.
         */
        @Test
        void addTaskStoresTask() {
            boolean added = taskQueue.addTask(createTask(TaskType.PHOTO));

            assertTrue(added);
            assertFalse(taskQueue.isEmpty());
            assertEquals(1, taskQueue.getTasks().size());
        }
    }

    /**
     * Tests bounded-capacity and reset behavior.
     */
    @Nested
    class CapacityTests {

        /**
         * Tests that adding beyond capacity fails without removing queued tasks.
         */
        @Test
        void addTaskReturnsFalseWhenQueueIsFull() {
            Task first = createTask(TaskType.PHOTO);
            Task second = createTask(TaskType.VIDEO);
            Task rejected = createTask(TaskType.ZOOM);

            taskQueue.resetCapacity(2);
            taskQueue.addTask(first);
            taskQueue.addTask(second);

            boolean added = taskQueue.addTask(rejected);

            assertFalse(added);
            assertEquals(List.of(first, second), List.copyOf(taskQueue.getTasks()));
        }

        /**
         * Tests that resetting capacity recreates and empties the queue.
         */
        @Test
        void resetCapacityCreatesEmptyQueueWithNewCapacity() {
            taskQueue.addTask(createTask(TaskType.PHOTO));

            taskQueue.resetCapacity(1);

            assertTrue(taskQueue.isEmpty());
            assertTrue(taskQueue.addTask(createTask(TaskType.PHOTO)));
            assertFalse(taskQueue.addTask(createTask(TaskType.VIDEO)));
        }
    }

    // ########################################################################
    // Helper methods
    // ########################################################################

    /**
     * Creates a minimal concrete task for queue tests.
     *
     * @param type task type.
     * @return created task.
     */
    private static Task createTask(TaskType type) {
        return new Task(type) {
        };
    }
}
