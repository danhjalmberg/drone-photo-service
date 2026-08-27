package io.github.danhjalmberg.dronephotoservice.models.snapshots;

import io.github.danhjalmberg.dronephotoservice.models.tasks.TaskType;

import java.awt.image.BufferedImage;

/**
 * Provides the completed-task data needed to render a thumbnail entry.
 *
 * <p>The thumbnail is a shared mutable image reference rather than an image
 * copy.</p>
 */
public final class TaskThumbnailSnapshot {

    private final String name;
    private final TaskType type;
    private final BufferedImage thumbnailImage;

    /**
     * Creates a thumbnail record.
     *
     * @param name           task name.
     * @param type           application-defined task type.
     * @param thumbnailImage shared thumbnail image, or {@code null} if the task
     *                       has no retained image.
     */
    public TaskThumbnailSnapshot(
            String name,
            TaskType type,
            BufferedImage thumbnailImage) {

        this.name = name;
        this.type = type;
        this.thumbnailImage = thumbnailImage;
    }

    /**
     * Returns the name displayed by the task thumbnail.
     *
     * @return task name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the type displayed by the task thumbnail.
     *
     * @return application-defined task type
     */
    public TaskType getType() {
        return type;
    }

    /**
     * Returns the image displayed by the task thumbnail.
     *
     * <p>The image is shared with the snapshot producer and is not defensively
     * copied.</p>
     *
     * @return shared thumbnail image, or {@code null} if unavailable
     */
    public BufferedImage getThumbnailImage() {
        return thumbnailImage;
    }
}
