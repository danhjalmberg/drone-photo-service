package io.github.danhjalmberg.dronephotoservice.models.tasks;

import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;
import io.github.danhjalmberg.dronephotoservice.settings.ModelSettings;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Tests frame retention behavior shared by task implementations. */
class TaskTest {

    /**
     * Video tasks retain the newest configured number of frames and matching
     * positions, rather than retaining the beginning of a long recording.
     */
    @Test
    void videoTaskKeepsNewestFramesAndPositions() {
        Task task = TaskFactory.INSTANCE.createTask(TaskType.VIDEO);
        BufferedImage finalImage = null;
        Vector2D finalPosition = null;

        for (int i = 0; i <= ModelSettings.VIDEO_TASK_MAX_FRAMES; i++) {
            finalImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            finalPosition = new Vector2D(i, 0.0);
            task.addImage(finalImage);
            task.addImagePosition(finalPosition);
        }

        assertEquals(ModelSettings.VIDEO_TASK_MAX_FRAMES, task.getImageCount());
        assertEquals(ModelSettings.VIDEO_TASK_MAX_FRAMES,
                task.getImagePositionsMeters().size());
        assertFalse(task.getImagePositionsMeters().contains(new Vector2D(0.0, 0.0)));
        assertSame(finalImage, task.getImages().get(task.getImageCount() - 1));
        assertEquals(finalPosition,
                task.getImagePositionsMeters().get(task.getImageCount() - 1));
    }
}
