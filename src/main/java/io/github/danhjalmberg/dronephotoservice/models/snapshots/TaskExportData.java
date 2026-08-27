package io.github.danhjalmberg.dronephotoservice.models.snapshots;

import io.github.danhjalmberg.dronephotoservice.models.tasks.TaskType;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Provides export-oriented data for a completed task.
 *
 * <p>The image list is copied into an unmodifiable list, preventing structural
 * changes after construction. The mutable {@link BufferedImage} instances are
 * shared with the model and are not copied.</p>
 */
public final class TaskExportData {

    private final String name;
    private final TaskType taskType;
    private final List<BufferedImage> images;

    /**
     * Creates an export record.
     *
     * @param name     task name.
     * @param taskType application-defined task type.
     * @param images   images to export.
     * @throws NullPointerException if {@code images} or any contained image is
     *                              {@code null}.
     */
    public TaskExportData(String name, TaskType taskType, List<BufferedImage> images) {
        this.name = name;
        this.taskType = taskType;
        this.images = List.copyOf(images);
    }

    /**
     * Returns the name used to identify the task during export.
     *
     * @return task name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the task type used to select its export format.
     *
     * @return application-defined task type
     */
    public TaskType getTaskType() {
        return taskType;
    }

    /**
     * Returns the unmodifiable image list.
     *
     * @return shared mutable images in an unmodifiable list.
     */
    public List<BufferedImage> getImages() {
        return images;
    }
}
