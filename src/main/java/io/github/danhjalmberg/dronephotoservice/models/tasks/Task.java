package io.github.danhjalmberg.dronephotoservice.models.tasks;

import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;
import io.github.danhjalmberg.dronephotoservice.models.photo_agencies.PhotoAgency;
import io.github.danhjalmberg.dronephotoservice.settings.ModelSettings;
import io.github.danhjalmberg.dronephotoservice.support.TimeUtils;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Describes a unit of capture work assigned to a drone.
 *
 * <p>A task is created by a {@link PhotoAgency}, submitted through a
 * {@link TaskQueue}, processed by a drone, and retained by the model after
 * completion. It records its origin, target in world meters, elapsed
 * simulation timestamps, captured images, and the world-meter positions at
 * which those images were captured. Drone-specific execution remains outside
 * this job-order abstraction.</p>
 *
 * <p>An aborted task may be returned to its originating agency and submitted
 * again after {@link #resetExecutionState()} clears the previous attempt.</p>
 *
 * @author Dan Hjälmberg
 */

public abstract class Task {

    private PhotoAgency photoAgency;
    private String name;
    private final TaskType type;
    private Vector2D targetPositionMeters;
    private String description = "";
    private Duration creationSimulationTime;
    private Duration startSimulationTime;
    private Duration imageSimulationTime;
    private Duration completionSimulationTime;
    private final List<BufferedImage> images;
    private final List<Vector2D> imagePositionsMeters;

    /**
     * Creates a task of the supplied application-defined type.
     *
     * <p>The creation timestamp initially equals {@link Duration#ZERO}; the
     * remaining execution timestamps are unset.</p>
     *
     * @param type task type used for display and image-retention rules.
     */
    public Task(TaskType type) {
        this.type = type;
        this.creationSimulationTime = Duration.ZERO;
        this.images = Collections.synchronizedList(new ArrayList<>());
        this.imagePositionsMeters = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Associates this task with its originating agency and assigns its name.
     *
     * @param id agency-local task identifier.
     * @param photoAgency agency that created the task.
     */
    public void setName(int id, PhotoAgency photoAgency) {
        this.photoAgency = photoAgency;
        this.name = "task_" + photoAgency.getId() + "_" + id;
    }

    /**
     * Returns the agency that created this task.
     *
     * @return originating agency, or {@code null} before {@link #setName(int, PhotoAgency)}.
     */
    public PhotoAgency getPhotoAgency() {
        return photoAgency;
    }

    /**
     * Returns the application-defined task type.
     *
     * @return task type supplied at construction.
     */
    public TaskType getType() {
        return type;
    }

    /**
     * Returns the generated task name.
     *
     * @return task name, or {@code null} before {@link #setName(int, PhotoAgency)}.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the task target position in world meters.
     *
     * @param targetPositionMeters target position in world meters; may be {@code null}.
     */
    public void setTargetPositionMeters(Vector2D targetPositionMeters) {
        this.targetPositionMeters = targetPositionMeters;
    }

    /**
     * Gets the task target position in world meters.
     *
     * @return target position in world meters, or {@code null} if unset.
     */
    public Vector2D getTargetPositionMeters() {
        return targetPositionMeters;
    }

    /**
     * Sets the task description.
     *
     * @param description task description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the elapsed simulation time at which this task was created.
     *
     * @param creationSimulationTime creation time in elapsed simulation time.
     */
    public void setCreationSimulationTime(Duration creationSimulationTime) {
        this.creationSimulationTime = creationSimulationTime;
    }

    /**
     * Gets task creation time.
     *
     * @return elapsed creation time.
     */
    public Duration getCreationSimulationTime() {
        return creationSimulationTime;
    }

    /**
     * Sets the elapsed simulation time at which processing started.
     *
     * @param startSimulationTime elapsed processing-start time.
     */
    public void setStartSimulationTime(Duration startSimulationTime) {
        this.startSimulationTime = startSimulationTime;
    }

    /**
     * Gets task start time.
     *
     * @return elapsed processing-start time, or {@code null} if not started.
     */
    public Duration getStartSimulationTime() {
        return startSimulationTime;
    }

    /**
     * Sets the elapsed simulation time associated with the task's capture.
     *
     * @param imageSimulationTime image time stamp.
     */
    public void setImageSimulationTime(Duration imageSimulationTime) {
        this.imageSimulationTime = imageSimulationTime;
    }

    /**
     * Gets image time stamp.
     *
     * @return elapsed capture time, or {@code null} if no capture is recorded.
     */
    public Duration getImageSimulationTime() {
        return imageSimulationTime;
    }

    /**
     * Sets the elapsed simulation time at which processing completed.
     *
     * @param completionSimulationTime elapsed completion time.
     */
    public void setCompletionSimulationTime(Duration completionSimulationTime) {
        this.completionSimulationTime = completionSimulationTime;
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
     * Adds a captured image subject to the limit for this task type.
     *
     * <p>Video tasks discard their oldest frame when full. Other task types
     * ignore additional images after reaching their limit. A {@code null}
     * image is ignored.</p>
     *
     * @param image photo/frame captured for this task.
     */
    public void addImage(BufferedImage image) {

        if (image == null) {
            return;
        }

        synchronized (images) {

            if (images.size() >= getMaxImageCount()) {

                if (keepsNewestImages()) {
                    images.remove(0);
                } else {
                    return;
                }
            }

            images.add(image);
        }
    }

    /**
     * Returns a snapshot of the current image references.
     *
     * <p>The returned list may be modified without affecting this task, but
     * the mutable {@link BufferedImage} instances are not copied.</p>
     *
     * @return copied list of images.
     */
    public List<BufferedImage> getImages() {

        synchronized (images) {
            return new ArrayList<>(images);
        }
    }

    /**
     * Adds the world-meter position at which a task image was captured.
     *
     * <p>Position retention follows the same type-specific limit as image
     * retention. Images and positions are stored separately, so callers are
     * responsible for adding corresponding entries consistently. A
     * {@code null} position is ignored.</p>
     *
     * @param positionMeters world meter position where an image was captured.
     */
    public void addImagePosition(Vector2D positionMeters) {

        if (positionMeters == null) {
            return;
        }

        synchronized (imagePositionsMeters) {

            if (imagePositionsMeters.size() >= getMaxImageCount()) {

                if (keepsNewestImages()) {
                    imagePositionsMeters.remove(0);
                } else {
                    return;
                }
            }

            imagePositionsMeters.add(positionMeters);
        }
    }

    /**
     * Returns a snapshot of the image capture positions.
     *
     * @return copied list of image capture positions in world meters.
     */
    public List<Vector2D> getImagePositionsMeters() {

        synchronized (imagePositionsMeters) {
            return new ArrayList<>(imagePositionsMeters);
        }
    }

    /**
     * Clears all image capture positions stored in this task.
     */
    public void clearImagePositions() {

        synchronized (imagePositionsMeters) {
            imagePositionsMeters.clear();
        }
    }

    /**
     * Clears all stored image references.
     */
    public void clearImages() {

        synchronized (images) {
            images.clear();
        }
    }

    /**
     * Gets the number of images currently stored in the task.
     *
     * @return number of stored images.
     */
    public int getImageCount() {

        synchronized (images) {
            return images.size();
        }
    }

    /**
     * Gets the maximum number of images allowed for this task type.
     *
     * @return image/frame limit.
     */
    private int getMaxImageCount() {

        return switch (type) {
            case PHOTO -> ModelSettings.PHOTO_TASK_MAX_IMAGES;
            case VIDEO -> ModelSettings.VIDEO_TASK_MAX_FRAMES;
            case ZOOM -> ModelSettings.ZOOM_TASK_MAX_FRAMES;
        };
    }

    /**
     * Indicates whether full retention buffers discard their oldest entry.
     *
     * @return {@code true} for the rolling video buffer; otherwise {@code false}.
     */
    private boolean keepsNewestImages() {
        return type == TaskType.VIDEO;
    }


    /**
     * Resets state from a processing attempt so the task can be submitted again.
     *
     * <p>The creation time, identity, origin, target, and description are
     * retained. Execution timestamps, images, and capture positions are
     * cleared.</p>
     */
    public void resetExecutionState() {
        startSimulationTime = null;
        imageSimulationTime = null;
        completionSimulationTime = null;
        clearImages();
        clearImagePositions();
    }

    /**
     * Formats a world meter position for log output.
     *
     * @param positionMeters position in world meters.
     * @return formatted meter position.
     */
    private String formatMeterPosition(Vector2D positionMeters) {

        if (positionMeters == null) {
            return "";
        }

        return "("
                + String.format("%.0f", positionMeters.getX())
                + ", "
                + String.format("%.0f", positionMeters.getY())
                + ")";
    }

    /**
     * Returns a multiline description of this task and its current state.
     *
     * @return formatted task information.
     */
    @Override
    public String toString() {

        StringBuilder info = new StringBuilder();
        info.append(name).append("\n");

        info.append("Created by: ").append(photoAgency.getName()).append(" at ")
                .append(TimeUtils.formatSimulationTime(creationSimulationTime)).append("\n");

        info.append("Description:\n").append(description).append("\n");

        info.append("Target position m: ")
                .append(formatMeterPosition(targetPositionMeters))
                .append("\n");

        if (startSimulationTime != null) {
            info.append(String.format("%-23s%s%n", "Processing started at: ",
                    TimeUtils.formatSimulationTime(startSimulationTime)));
        }
        if (imageSimulationTime != null) {
            info.append(String.format("%-23s%s%n", "Image time stamp: ",
                    TimeUtils.formatSimulationTime(imageSimulationTime)));
        }
        if (completionSimulationTime != null) {
            info.append(String.format("%-23s%s%n", "Processing ended at: ",
                    TimeUtils.formatSimulationTime(completionSimulationTime)));
        }
        info.append("Number of images: ").append(getImageCount()).append("\n");

        return info.toString();
    }
}
