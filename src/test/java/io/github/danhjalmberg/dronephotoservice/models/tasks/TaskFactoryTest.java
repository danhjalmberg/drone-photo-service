package io.github.danhjalmberg.dronephotoservice.models.tasks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** Tests creation of every supported task type. */
class TaskFactoryTest {

    @Test
    void createsEverySupportedTaskType() {
        assertCreatedType(TaskType.PHOTO, PhotoTask.class);
        assertCreatedType(TaskType.VIDEO, VideoTask.class);
        assertCreatedType(TaskType.ZOOM, ZoomTask.class);
    }

    private static void assertCreatedType(
            TaskType taskType,
            Class<? extends Task> expectedClass) {
        Task task = TaskFactory.INSTANCE.createTask(taskType);
        assertInstanceOf(expectedClass, task);
        assertEquals(taskType, task.getType());
    }
}
