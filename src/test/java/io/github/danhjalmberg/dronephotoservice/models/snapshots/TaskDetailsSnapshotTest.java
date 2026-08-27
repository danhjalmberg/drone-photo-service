package io.github.danhjalmberg.dronephotoservice.models.snapshots;

import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;
import io.github.danhjalmberg.dronephotoservice.models.tasks.TaskType;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the collection-ownership contract of
 * {@link TaskDetailsSnapshot}.
 *
 * @author Dan Hjälmberg
 */
class TaskDetailsSnapshotTest {

    /**
     * Tests that later changes to the source list do not change the snapshot.
     */
    @Test
    void constructorCopiesImageList() {
        BufferedImage firstImage = createImage();
        BufferedImage secondImage = createImage();
        List<BufferedImage> sourceImages = new ArrayList<>();
        sourceImages.add(firstImage);

        TaskDetailsSnapshot snapshot = createSnapshot(sourceImages);
        sourceImages.add(secondImage);

        assertEquals(List.of(firstImage), snapshot.getImages());
    }

    /**
     * Tests that callers cannot mutate the image list through the getter.
     */
    @Test
    void getImagesReturnsUnmodifiableList() {
        TaskDetailsSnapshot snapshot = createSnapshot(List.of(createImage()));

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.getImages().clear());
    }

    /**
     * Tests that the shallow copy retains image references without duplicating
     * their potentially large pixel data.
     */
    @Test
    void constructorRetainsImageReferences() {
        BufferedImage image = createImage();

        TaskDetailsSnapshot snapshot = createSnapshot(List.of(image));

        assertSame(image, snapshot.getImages().get(0));
    }

    /**
     * Creates representative task details with the supplied images.
     *
     * @param images images to retain
     * @return constructed task-details snapshot
     */
    private static TaskDetailsSnapshot createSnapshot(
            List<BufferedImage> images) {

        return new TaskDetailsSnapshot(
                "task_1",
                TaskType.PHOTO,
                "photo_agency_1",
                Duration.ZERO,
                Duration.ZERO,
                Duration.ZERO,
                Duration.ZERO,
                Vector2D.ZERO,
                images.size(),
                images.isEmpty() ? null : images.get(0),
                images);
    }

    /**
     * Creates a minimal image suitable for identity and list-ownership tests.
     *
     * @return new test image
     */
    private static BufferedImage createImage() {
        return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    }
}
