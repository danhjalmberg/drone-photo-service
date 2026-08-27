package io.github.danhjalmberg.dronephotoservice.controllers;

import io.github.danhjalmberg.dronephotoservice.models.Model;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.TaskDetailsSnapshot;
import io.github.danhjalmberg.dronephotoservice.models.tasks.TaskType;
import io.github.danhjalmberg.dronephotoservice.settings.ModelSettings;
import io.github.danhjalmberg.dronephotoservice.views.View;

import javax.swing.Timer;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Plays archived task-result images using a Swing timer.
 *
 * <p>Playback runs on the EDT, displays each retained frame once, and then
 * stops on the final frame. Frame delay is derived from task type.</p>
 *
 * @author Dan Hjälmberg
 */
public class TaskPlaybackController {

    private final Model model;
    private final View view;

    private Timer playbackTimer;
    private int playbackFrameIndex = 0;

    /**
     * Creates a task-result playback controller.
     *
     * @param model the model to use for retrieving task details
     * @param view the view to use for displaying task results
     */
    public TaskPlaybackController(
            Model model,
            View view) {

        this.model = model;
        this.view = view;
    }

    /**
     * Starts playback for an archived task containing more than one image.
     *
     * <p>A null name, missing task, or single-image result has no effect. A
     * valid request stops existing playback, begins immediately at frame zero,
     * and updates the view's playback-running state.</p>
     *
     * @param selectedTaskName archived task name.
     */
    public void playSelectedTaskResult(String selectedTaskName) {

        if (selectedTaskName == null) {
            return;
        }

        TaskDetailsSnapshot selectedTask =
                model.getArchivedTaskDetails(selectedTaskName);

        if (selectedTask == null || selectedTask.getImages().size() <= 1) {
            return;
        }

        stopSelectedTaskResult(selectedTaskName);

        int frameDelayMs = getTaskResultFrameDelayMs(selectedTask.getType());

        playbackFrameIndex = 0;

        playbackTimer = new Timer(frameDelayMs, event -> {

            List<BufferedImage> images = selectedTask.getImages();

            if (images.isEmpty()) {
                stopSelectedTaskResult(selectedTaskName);
                return;
            }

            view.displayTaskResult(
                    selectedTask.getName(),
                    images.get(playbackFrameIndex));

            playbackFrameIndex++;

            if (playbackFrameIndex >= images.size()) {
                playbackTimer.stop();
                playbackTimer = null;
                playbackFrameIndex = 0;
                view.setTaskPlaybackRunning(false);
            }
        });

        playbackTimer.setRepeats(true);
        playbackTimer.setInitialDelay(0);
        playbackTimer.start();

        view.setTaskPlaybackRunning(true);
    }

    /**
     * Stops current playback and restores the named task's preview image.
     *
     * <p>If the name is null or no longer archived, playback still stops and the
     * frame index and view state are reset.</p>
     *
     * @param selectedTaskName task whose preview should be restored.
     */
    public void stopSelectedTaskResult(String selectedTaskName) {

        if (playbackTimer != null) {
            playbackTimer.stop();
            playbackTimer = null;
        }

        playbackFrameIndex = 0;

        TaskDetailsSnapshot selectedTask =
                model.getArchivedTaskDetails(selectedTaskName);

        if (selectedTask != null) {
            view.displayTaskResult(
                    selectedTask.getName(),
                    selectedTask.getPreviewImage());
        }

        view.setTaskPlaybackRunning(false);
    }

    /**
     * Returns the playback delay configured for a task type.
     *
     * @param taskType application-defined task type.
     * @return video or zoom frame delay, or the photo fallback delay.
     */
    private int getTaskResultFrameDelayMs(TaskType taskType) {
        return switch (taskType) {
            case PHOTO -> ModelSettings.PHOTO_TASK_PLAYBACK_FRAME_DELAY_MS;
            case VIDEO -> ModelSettings.VIDEO_TASK_FRAME_DELAY_MS;
            case ZOOM -> ModelSettings.ZOOM_TASK_FRAME_DELAY_MS;
        };
    }

    /**
     * Stops playback resources without performing further view updates.
     * Calling this method more than once is safe.
     */
    public void shutdown() {

        if (playbackTimer != null) {
            playbackTimer.stop();
            playbackTimer = null;
        }

        playbackFrameIndex = 0;
    }
}
