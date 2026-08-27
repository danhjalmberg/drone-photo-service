package io.github.danhjalmberg.dronephotoservice.models.snapshots;

import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;
import io.github.danhjalmberg.dronephotoservice.models.tasks.TaskType;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.List;

/**
 * Provides detailed task state for presentation and result playback.
 *
 * <p>Timestamps are elapsed simulation times, and the task position is
 * expressed in world meters. The image list is copied at construction and is
 * unmodifiable. The preview image and contained {@link BufferedImage} instances
 * remain shared mutable references and must be treated as read-only.</p>
 *
 * @author Dan Hjälmberg
 */
public final class TaskDetailsSnapshot {

    private final String name;
    private final TaskType type;
    private final String photoAgencyName;
    private final Duration creationSimulationTime;
    private final Duration startSimulationTime;
    private final Duration imageSimulationTime;
    private final Duration completionSimulationTime;
    private final Vector2D positionMeters;
    private final int imageCount;
    private final BufferedImage previewImage;
    private final List<BufferedImage> images;

    /**
     * Creates a detailed task view.
     *
     * @param name                     task name.
     * @param type                     application-defined task type.
     * @param photoAgencyName          originating agency name, or an empty string if absent.
     * @param creationSimulationTime   elapsed task-creation time.
     * @param startSimulationTime      elapsed processing-start time, or {@code null}.
     * @param imageSimulationTime      elapsed capture time, or {@code null}.
     * @param completionSimulationTime elapsed completion time, or {@code null}.
     * @param positionMeters           target position in world meters.
     * @param imageCount               number of retained task images.
     * @param previewImage             shared preview image, or {@code null} if unavailable.
     * @param images                   retained images; the list structure is copied.
     * @throws NullPointerException if {@code images} or one of its elements is
     *                              {@code null}.
     */
    public TaskDetailsSnapshot(
            String name,
            TaskType type,
            String photoAgencyName,
            Duration creationSimulationTime,
            Duration startSimulationTime,
            Duration imageSimulationTime,
            Duration completionSimulationTime,
            Vector2D positionMeters,
            int imageCount,
            BufferedImage previewImage,
            List<BufferedImage> images) {

        this.name = name;
        this.type = type;
        this.photoAgencyName = photoAgencyName;
        this.creationSimulationTime = creationSimulationTime;
        this.startSimulationTime = startSimulationTime;
        this.imageSimulationTime = imageSimulationTime;
        this.completionSimulationTime = completionSimulationTime;
        this.positionMeters = positionMeters;
        this.imageCount = imageCount;
        this.previewImage = previewImage;
        this.images = List.copyOf(images);
    }

    /**
     * Returns the task name.
     *
     * @return task name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the application-defined task type.
     *
     * @return task type.
     */
    public TaskType getType() {
        return type;
    }

    /**
     * Returns the originating photo-agency name.
     *
     * @return agency name, or an empty string if absent.
     */
    public String getPhotoAgencyName() {
        return photoAgencyName;
    }

    /**
     * Returns the elapsed simulation time at which the task was created.
     *
     * @return elapsed creation time.
     */
    public Duration getCreationSimulationTime() {
        return creationSimulationTime;
    }

    /**
     * Returns the elapsed simulation time at which processing started.
     *
     * @return elapsed start time, or {@code null} if not started.
     */
    public Duration getStartSimulationTime() {
        return startSimulationTime;
    }

    /**
     * Returns the elapsed simulation time associated with capture.
     *
     * @return elapsed capture time, or {@code null} if no capture is recorded.
     */
    public Duration getImageSimulationTime() {
        return imageSimulationTime;
    }

    /**
     * Returns the elapsed simulation time at which processing completed.
     *
     * @return elapsed completion time, or {@code null} if not completed.
     */
    public Duration getCompletionSimulationTime() {
        return completionSimulationTime;
    }

    /**
     * Returns the task target position in world meters.
     *
     * @return target position in world meters.
     */
    public Vector2D getPositionMeters() {
        return positionMeters;
    }

    /**
     * Returns the captured image count recorded for this view.
     *
     * @return recorded image count.
     */
    public int getImageCount() {
        return imageCount;
    }

    /**
     * Returns the shared preview image.
     *
     * @return preview image, or {@code null} if unavailable.
     */
    public BufferedImage getPreviewImage() {
        return previewImage;
    }

    /**
     * Returns the retained task images.
     *
     * <p>The list structure is unmodifiable. Its mutable images are shared
     * references and must be treated as read-only.</p>
     *
     * @return unmodifiable image list.
     */
    public List<BufferedImage> getImages() {
        return images;
    }
}
