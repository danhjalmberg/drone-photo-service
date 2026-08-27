package io.github.danhjalmberg.dronephotoservice.controllers;

import io.github.danhjalmberg.dronephotoservice.models.Model;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.DroneSnapshot;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.TaskDetailsSnapshot;
import io.github.danhjalmberg.dronephotoservice.views.View;
import io.github.danhjalmberg.dronephotoservice.views.viewdata.MapSelection;

/**
 * Synchronizes drone and archived-task selection across map, tables,
 * thumbnails, details panels, and playback.
 *
 * <p>Drone and task selections are mutually exclusive. Changing or clearing
 * selection first stops task playback so a timer cannot continue updating a
 * result that is no longer selected.</p>
 *
 * @author Dan Hjälmberg
 */
public class SelectionController {

    private final Model model;
    private final View view;
    private final TaskPlaybackController taskPlaybackController;
    private Runnable updateTaskThumbnails = () -> { };

    private String selectedDroneName;
    private String selectedTaskName;

    /**
     * Creates a selection coordinator without an initial selection.
     *
     * @param model the model
     * @param view the view
     * @param taskPlaybackController the task playback controller
     */
    public SelectionController(
            Model model,
            View view,
            TaskPlaybackController taskPlaybackController) {

        this.model = model;
        this.view = view;
        this.taskPlaybackController = taskPlaybackController;
    }

    /**
     * Returns the selected drone name.
     *
     * @return selected name, or {@code null}.
     */
    public String getSelectedDroneName() {
        return selectedDroneName;
    }

    /**
     * Returns the selected archived-task name.
     *
     * @return selected name, or {@code null}.
     */
    public String getSelectedTaskName() {
        return selectedTaskName;
    }

    /**
     * Sets the callback used to refresh thumbnail selection state.
     *
     * <p>A {@code null} callback is replaced by a no-op.</p>
     *
     * @param updateTaskThumbnails the runnable to update task thumbnails
     */
    public void setUpdateTaskThumbnails(Runnable updateTaskThumbnails) {
        this.updateTaskThumbnails = updateTaskThumbnails == null
                ? () -> { }
                : updateTaskThumbnails;
    }

    /**
     * Applies a drone, completed-task, or empty map selection.
     *
     * @param selection map selection, or {@code null} to clear selection.
     * @throws IllegalStateException if the selection type is unrecognized.
     */
    public void selectMapEntity(MapSelection selection) {

        if (selection == null) {
            clearSelection();
            return;
        }

        MapSelection.Type selectionType = selection.getType();

        switch (selectionType) {

            case DRONE -> selectDrone(selection.getName());
            case COMPLETED_TASK -> selectTask(selection.getName());
            case NONE -> clearSelection();
            default -> throw new IllegalStateException(
                    "Unhandled map selection type: " + selectionType);
        }
    }

    /**
     * Selects a drone name and clears any task selection.
     *
     * <p>The name is retained without model validation. If no drone currently
     * matches it, the details and live-image views are cleared.</p>
     *
     * @param droneName the name of the drone to select
     */
    public void selectDrone(String droneName) {

        taskPlaybackController.stopSelectedTaskResult(selectedTaskName);

        selectedDroneName = droneName;

        if (droneName != null) {
            selectedTaskName = null;
        }

        view.selectDroneByName(selectedDroneName);
        view.selectTaskByName(selectedTaskName);

        updateDroneDetails();
        updateTaskDetails();
    }

    /**
     * Selects an archived task name and clears any drone selection.
     *
     * <p>If the archive no longer contains the named task, the selection is
     * cleared while details are refreshed.</p>
     *
     * @param taskName the name of the task to select
     */
    public void selectTask(String taskName) {

        taskPlaybackController.stopSelectedTaskResult(selectedTaskName);

        selectedTaskName = taskName;

        if (taskName != null) {
            selectedDroneName = null;
        }

        view.selectTaskByName(selectedTaskName);
        view.selectDroneByName(selectedDroneName);

        updateTaskThumbnails.run();
        updateTaskDetails();
        updateDroneDetails();
    }

    /**
     * Gets a compact text representation of the current selection.
     *
     * @return selected drone or task text, or "none" if nothing is selected
     */
    public String getSelectionStatusText() {

        if (selectedDroneName != null) {
            return selectedDroneName;
        }

        if (selectedTaskName != null) {
            return selectedTaskName;
        }

        return "none";
    }

    /**
     * Stops playback and clears drone and task selection everywhere in the view.
     */
    public void clearSelection() {

        taskPlaybackController.stopSelectedTaskResult(selectedTaskName);

        selectedDroneName = null;
        selectedTaskName = null;

        view.selectDroneByName(null);
        view.selectTaskByName(null);

        updateTaskThumbnails.run();
        updateDroneDetails();
        updateTaskDetails();
    }

    /**
     * Refreshes details and optional live imagery for the selected drone.
     *
     * <p>This runs on every GUI refresh because drone telemetry changes during
     * simulation. A missing selection or missing drone clears both views.</p>
     */
    public void updateDroneDetails() {

        if (selectedDroneName == null) {
            view.displayDroneDetails(null);
            view.displayDroneLiveImage(null);
            return;
        }

        DroneSnapshot selectedDrone = model.getDroneSnapshots()
                .stream()
                .filter(drone -> drone.getName().equals(selectedDroneName))
                .findFirst()
                .orElse(null);

        view.displayDroneDetails(selectedDrone);

        if (selectedDrone != null && view.isLiveCameraViewEnabled()) {
            view.displayDroneLiveImage(model.getDroneLiveCameraImage(selectedDroneName));
        } else {
            view.displayDroneLiveImage(null);
        }
    }

    /**
     * Refreshes details for the selected archived task.
     *
     * <p>Task details are selection-driven rather than periodic so playback is
     * not reset on every GUI tick. If the task has disappeared from the bounded
     * archive, its selection and thumbnail highlight are cleared.</p>
     */
    public void updateTaskDetails() {

        if (selectedTaskName == null) {
            view.displayTaskDetails(null);
            return;
        }

        TaskDetailsSnapshot selectedTask =
                model.getArchivedTaskDetails(selectedTaskName);

        if (selectedTask == null) {
            selectedTaskName = null;
            view.selectTaskByName(null);
            view.displayTaskDetails(null);
            updateTaskThumbnails.run();
            return;
        }

        view.displayTaskDetails(selectedTask);
    }
}
