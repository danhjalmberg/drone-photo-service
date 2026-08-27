package io.github.danhjalmberg.dronephotoservice.models.tasks;

import io.github.danhjalmberg.dronephotoservice.models.snapshots.TaskExportData;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.TaskThumbnailSnapshot;
import io.github.danhjalmberg.dronephotoservice.settings.ModelSettings;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Retains completed tasks in a bounded, insertion-ordered archive.
 *
 * <p>When capacity is exceeded, the oldest task is removed and its image
 * references are cleared to release the largest retained data. Other task
 * state, including capture positions, remains on the removed object. Public
 * operations are synchronized so archive access is serialized.</p>
 *
 * @author Dan Hjälmberg
 */
public class TaskArchive {

    private final Deque<Task> archivedTasks;

    /**
     * Creates an empty task archive.
     */
    public TaskArchive() {
        this.archivedTasks = new ArrayDeque<>();
    }

    /**
     * Appends a completed task to the archive.
     *
     * <p>If the configured capacity is exceeded, the oldest task is removed
     * and its image references are cleared. A {@code null} task is ignored.</p>
     *
     * @param task completed task to archive.
     */
    public synchronized void add(Task task) {
        if (task == null) {
            return;
        }

        archivedTasks.addLast(task);

        while (archivedTasks.size() > ModelSettings.TASK_ARCHIVE_MAX_SIZE) {
            Task removedTask = archivedTasks.removeFirst();
            removedTask.clearImages();
        }
    }

    /**
     * Clears the archive and releases image references from all archived tasks.
     */
    public synchronized void clear() {
        for (Task task : archivedTasks) {
            task.clearImages();
        }

        archivedTasks.clear();
    }

    /**
     * Returns a snapshot of the archived task references in insertion order.
     *
     * <p>Changing the returned list does not affect this archive; the mutable
     * {@link Task} instances themselves are not copied.</p>
     *
     * @return copied list of archived tasks.
     */
    public synchronized List<Task> getTasks() {
        return new ArrayList<>(archivedTasks);
    }

    /**
     * Gets the number of archived tasks.
     *
     * @return archive size.
     */
    public synchronized int size() {
        return archivedTasks.size();
    }

    /**
     * Returns the most recently archived task.
     *
     * @return latest archived task, or {@code null} if the archive is empty.
     */
    public synchronized Task getLatestTask() {
        return archivedTasks.peekLast();
    }

    /**
     * Creates export records for all archived tasks in insertion order.
     *
     * <p>Each record receives a new image list, but the mutable images
     * themselves are shared with the archived task.</p>
     *
     * @return task export data snapshots.
     */
    public synchronized List<TaskExportData> getTaskExportData() {

        return archivedTasks.stream()
                .map(task -> new TaskExportData(
                        task.getName(),
                        task.getType(),
                        new ArrayList<>(task.getImages())))
                .collect(Collectors.toList());
    }

    /**
     * Returns formatted diagnostic text for all archived tasks in insertion
     * order.
     *
     * @return archived-task diagnostic text.
     */
    public synchronized String getDiagnosticText() {
        StringBuilder diagnosticText = new StringBuilder();

        for (Task task : archivedTasks) {
            diagnosticText.append(task).append("\n");
        }

        return diagnosticText.toString();
    }

    /**
     * Finds the first archived task with the supplied name.
     *
     * @param taskName name of the archived task.
     * @return matching task, or {@code null} if the name is {@code null} or absent.
     */
    public synchronized Task getTaskByName(String taskName) {

        if (taskName == null) {
            return null;
        }

        for (Task task : archivedTasks) {
            if (taskName.equals(task.getName())) {
                return task;
            }
        }

        return null;
    }

    /**
     * Creates thumbnail records for up to the latest {@code maxCount} tasks.
     *
     * <p>The selected records remain ordered from oldest to newest. Video
     * tasks use their last retained frame; photo and zoom tasks use their
     * first. Thumbnail images are shared references, not image copies.</p>
     *
     * @param maxCount maximum number of thumbnails to return.
     * @return thumbnail records, or an empty list when {@code maxCount} is not positive.
     */
    public synchronized List<TaskThumbnailSnapshot> getLatestTaskThumbnails(int maxCount) {

        if (maxCount <= 0) {
            return List.of();
        }

        List<Task> tasks = new ArrayList<>(archivedTasks);
        int fromIndex = Math.max(0, tasks.size() - maxCount);

        return tasks.subList(fromIndex, tasks.size())
                .stream()
                .map(task -> {
                    List<BufferedImage> images = new ArrayList<>(task.getImages());
                    BufferedImage thumbnailImage = getThumbnailImageForTask(task, images);

                    return new TaskThumbnailSnapshot(
                            task.getName(),
                            task.getType(),
                            thumbnailImage);
                })
                .collect(Collectors.toList());
    }

    /**
     * Selects the image used to represent a task.
     *
     * @param task task whose preview image should be found.
     * @param images task image list.
     * @return preview image, or {@code null} if no image is available.
     */
    private BufferedImage getPreviewImageForTask(Task task, List<BufferedImage> images) {
        if (images.isEmpty()) {
            return null;
        }

        if (task.getType() == TaskType.VIDEO) {
            return images.get(images.size() - 1);
        }

        return images.get(0);
    }

    /**
     * Selects the thumbnail image for a task.
     *
     * @param task task whose thumbnail image should be found.
     * @param images task image list.
     * @return thumbnail image, or {@code null} if no image is available.
     */
    private BufferedImage getThumbnailImageForTask(
            Task task,
            List<BufferedImage> images) {
        return getPreviewImageForTask(task, images);
    }
}
