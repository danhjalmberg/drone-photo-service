package io.github.danhjalmberg.dronephotoservice.models.tasks;

import io.github.danhjalmberg.dronephotoservice.settings.ModelSettings;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link TaskArchive} class.
 * The tests cover representative insertion, ordering, defensive copying,
 * clearing, null handling, and bounded-capacity behavior without attempting
 * exhaustive coverage of every snapshot and formatting method.
 *
 * @author Dan Hjälmberg
 */
class TaskArchiveTest {

    /**
     * Tests basic archive insertion and retrieval behavior.
     */
    @Nested
    class StorageTests {

        /**
         * Tests that a new archive is empty.
         */
        @Test
        void constructorCreatesEmptyArchive() {
            TaskArchive archive = new TaskArchive();

            assertEquals(0, archive.size());
            assertNull(archive.getLatestTask());
            assertTrue(archive.getTasks().isEmpty());
        }

        /**
         * Tests that adding tasks preserves insertion order and updates the latest task.
         */
        @Test
        void addStoresTasksInInsertionOrder() {
            TaskArchive archive = new TaskArchive();
            Task first = createPhotoTask();
            Task second = createPhotoTask();

            archive.add(first);
            archive.add(second);

            List<Task> tasks = archive.getTasks();

            assertEquals(2, archive.size());
            assertSame(first, tasks.get(0));
            assertSame(second, tasks.get(1));
            assertSame(second, archive.getLatestTask());
        }

        /**
         * Tests that adding null leaves the archive unchanged.
         */
        @Test
        void addIgnoresNullTask() {
            TaskArchive archive = new TaskArchive();

            archive.add(null);

            assertEquals(0, archive.size());
        }

        /**
         * Tests that the returned task list is a defensive copy.
         */
        @Test
        void getTasksReturnsDefensiveCopy() {
            TaskArchive archive = new TaskArchive();
            archive.add(createPhotoTask());

            List<Task> tasks = archive.getTasks();
            tasks.clear();

            assertEquals(1, archive.size());
        }
    }

    /**
     * Tests archive cleanup and bounded-capacity behavior.
     */
    @Nested
    class CapacityAndCleanupTests {

        /**
         * Tests that clearing the archive removes tasks and releases their images.
         */
        @Test
        void clearRemovesTasksAndClearsImages() {
            TaskArchive archive = new TaskArchive();
            Task task = createPhotoTask();
            task.addImage(createImage());

            archive.add(task);
            archive.clear();

            assertEquals(0, archive.size());
            assertEquals(0, task.getImageCount());
        }

        /**
         * Tests that exceeding archive capacity removes the oldest task
         * and clears its image references.
         */
        @Test
        void addBeyondCapacityRemovesOldestTaskAndClearsItsImages() {
            TaskArchive archive = new TaskArchive();
            Task oldestTask = createPhotoTask();
            oldestTask.addImage(createImage());
            archive.add(oldestTask);

            for (int i = 0; i < ModelSettings.TASK_ARCHIVE_MAX_SIZE; i++) {
                archive.add(createPhotoTask());
            }

            assertEquals(ModelSettings.TASK_ARCHIVE_MAX_SIZE, archive.size());
            assertFalse(archive.getTasks().contains(oldestTask));
            assertEquals(0, oldestTask.getImageCount());
        }
    }

    // ########################################################################
    // Helper methods
    // ########################################################################

    /**
     * Creates a photo task through the application's task factory.
     *
     * @return new photo task.
     */
    private static Task createPhotoTask() {
        return TaskFactory.INSTANCE.createTask(TaskType.PHOTO);
    }

    /**
     * Creates a small representative image for archive cleanup tests.
     *
     * @return created image.
     */
    private static BufferedImage createImage() {
        return new BufferedImage(
                2,
                2,
                BufferedImage.TYPE_INT_RGB);
    }
}
