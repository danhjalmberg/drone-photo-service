package io.github.danhjalmberg.dronephotoservice.controllers;

import io.github.danhjalmberg.dronephotoservice.models.Model;
import io.github.danhjalmberg.dronephotoservice.models.events.SimulationEvent;
import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.DroneSnapshot;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.PhotoAgencySnapshot;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.TaskSnapshot;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.TaskThumbnailSnapshot;
import io.github.danhjalmberg.dronephotoservice.views.View;
import io.github.danhjalmberg.dronephotoservice.views.viewdata.MapDroneViewData;
import io.github.danhjalmberg.dronephotoservice.views.viewdata.MapTaskViewData;
import io.github.danhjalmberg.dronephotoservice.views.viewdata.StatusBarViewData;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Projects current model state into refresh-driven Swing views.
 *
 * <p>The GUI refresh timer invokes the main update on the EDT. Lightweight
 * tables, map data, events, thumbnails, status, and live selection details are
 * updated independently. Large diagnostic text monitors are throttled by wall
 * time, while archived task details remain selection-driven so playback is not
 * disturbed.</p>
 *
 * @author Dan Hjälmberg
 */
public class ViewRefreshController {

    private static final long MONITOR_REFRESH_INTERVAL_MS = 1000;

    private final Model model;
    private final View view;
    private final SelectionController selectionController;

    private List<String> visibleThumbnailTaskNames = List.of();
    private String lastThumbnailSelectedTaskName;

    private Vector2D mouseDisplayPosition;

    private long lastMonitorRefreshMs;

    private long lastDisplayedEventSequenceNumber;
    private String latestActivityMessage = "Ready";

    /**
     * Creates a view-refresh coordinator with an empty event cursor and cache.
     *
     * @param model the model to use
     * @param view the view to use
     * @param selectionController the selection controller to use
     */
    public ViewRefreshController(
            Model model,
            View view,
            SelectionController selectionController) {

        this.model = model;
        this.view = view;
        this.selectionController = selectionController;
    }

    /**
     * Refreshes all periodic view projections from current model state.
     */
    public void updateGUI() {

        updateMonitors();
        updateEventLog();

        updatePhotoAgencyOverview();
        updateDroneOverview();
        updateTaskOverview();
        updateMapView();
        updateTaskThumbnails();
        updateStatusBar();

        // Drone details are refresh-driven because selected drone telemetry changes live.
        // Task details are selection-driven because archived task result display should not
        // be reset every GUI tick, especially during playback.
        selectionController.updateDroneDetails();
    }

    /**
     * Refreshes large diagnostic text views at most once per wall-clock second.
     */
    private void updateMonitors() {

        long nowMs = System.currentTimeMillis();

        if (nowMs - lastMonitorRefreshMs < MONITOR_REFRESH_INTERVAL_MS) {
            return;
        }

        lastMonitorRefreshMs = nowMs;

        updatePhotoAgencyMonitor();
        updateDroneMonitor();
        updateCompletedTaskMonitor();
    }

    /**
     * Displays agency count and concatenated diagnostic text.
     */
    private void updatePhotoAgencyMonitor() {
        view.displayPhotoAgencyMonitor(
                model.getPhotoAgencyCount(),
                model.getPhotoAgencyDiagnosticText());
    }

    /**
     * Projects agency summaries into overview-table rows.
     */
    private void updatePhotoAgencyOverview() {

        List<PhotoAgencySnapshot> snapshots = model.getPhotoAgencySnapshots();

        Object[][] data = snapshots.stream()
                .map(snapshot -> new Object[] {
                        snapshot.getName(),
                        snapshot.getCreatedTaskCount(),
                        snapshot.getPendingTaskName(),
                        snapshot.hasPendingTask() ? "Yes" : "No"
                })
                .toArray(Object[][]::new);

        view.displayPhotoAgencyOverview(data);
    }

    /**
     * Displays drone count and concatenated diagnostic information.
     */
    private void updateDroneMonitor() {
        view.displayDroneMonitor(
                model.getDroneCount(),
                model.getDroneDiagnosticText());
    }

    /**
     * Appends events newer than the display cursor and records latest activity.
     *
     * <p>The model history is bounded, so events evicted before this refresh
     * cannot be recovered.</p>
     */
    private void updateEventLog() {

        List<SimulationEvent> events =
                model.getSimulationEventsSince(lastDisplayedEventSequenceNumber);

        if (events.isEmpty()) {
            return;
        }

        SimulationEvent latestEvent = events.get(events.size() - 1);

        lastDisplayedEventSequenceNumber = latestEvent.getSequenceNumber();

        // Append the retained sequence while status shows only its latest activity.
        view.appendSimulationEvents(events);

        latestActivityMessage = latestEvent.getMessage();
    }

    /**
     * Resets the event cursor and default status message for a new run.
     *
     * <p>This resets presentation state; model event clearing is owned by
     * {@link Model#resetSimulation()}.</p>
     */
    public void resetEventLog() {

        lastDisplayedEventSequenceNumber = 0;
        latestActivityMessage = "Ready";
    }

    /**
     * Projects drone snapshots into overview-table rows.
     */
    private void updateDroneOverview() {

        List<DroneSnapshot> snapshots = model.getDroneSnapshots();

        Object[][] data = snapshots.stream()
                .map(snapshot -> new Object[] {
                        snapshot.getName(),
                        snapshot.getState().getDisplayName(),
                        snapshot.getAssignedTask() == null
                                ? ""
                                : snapshot.getAssignedTask().getName(),
                        snapshot.getCompletedTasks().size(),
                        String.format("%.0f", snapshot.getCurrentPositionMeters().getX()),
                        String.format("%.0f", snapshot.getCurrentPositionMeters().getY())
                })
                .toArray(Object[][]::new);

        view.displayDroneOverview(data);
    }

    /**
     * Displays archive count and formatted archived-task information.
     */
    private void updateCompletedTaskMonitor() {
        view.displayCompletedTaskMonitor(
                model.getTaskArchiveSize(),
                model.getArchivedTaskDiagnosticText());
    }

    /**
     * Projects current queued-task summaries into overview-table rows.
     */
    private void updateTaskOverview() {

        List<TaskSnapshot> snapshots = model.getQueuedTaskSnapshots();

        Object[][] data = snapshots.stream()
                .map(snapshot -> new Object[] {
                        snapshot.getName(),
                        snapshot.getType().getDisplayName(),
                        String.format("%.0f", snapshot.getTargetPositionMeters().getX()),
                        String.format("%.0f", snapshot.getTargetPositionMeters().getY())
                })
                .toArray(Object[][]::new);

        view.displayTaskOverview(data);
    }

    /**
     * Rebuilds display-coordinate map data for queued tasks and drones.
     *
     * <p>Drone data includes base and current positions, assigned targets,
     * bounded completed-task markers, and display-coordinate video trails.
     * The active resampled map image and attribution are then rendered with
     * those projections.</p>
     */
    public void updateMapView() {

        List<MapTaskViewData> taskViewData = model.getQueuedTaskSnapshots()
                .stream()
                .map(task -> new MapTaskViewData(
                        model.worldMetersToDisplay(task.getTargetPositionMeters()),
                        task.getName()))
                .collect(Collectors.toList());

        List<MapDroneViewData> droneViewData = model.getDroneSnapshots()
                .stream()
                .map(drone -> new MapDroneViewData(
                        model.worldMetersToDisplay(drone.getBasePositionMeters()),
                        model.worldMetersToDisplay(drone.getCurrentPositionMeters()),
                        drone.getName(),
                        drone.getAssignedTask() == null
                                ? null
                                : new MapTaskViewData(
                                        model.worldMetersToDisplay(
                                                drone.getAssignedTask().getTargetPositionMeters()),
                                                drone.getAssignedTask().getName()),
                        drone.getCompletedTasks()
                                .stream()
                                .map(task -> new MapTaskViewData(
                                        model.worldMetersToDisplay(task.getTargetPositionMeters()),
                                        task.getName(),
                                        task.getType(),
                                        task.getImagePositionsMeters()
                                                .stream()
                                                .map(model::worldMetersToDisplay)
                                                .collect(Collectors.toList())))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        view.displayMap(
                model.getMapImageResampled(),
                model.getMapAttribution(),
                taskViewData,
                droneViewData);
    }

    /**
     * Updates the latest known map mouse position and refreshes the status bar.
     *
     * @param mouseDisplayPosition mouse position in display pixels, or null
     */
    public void updateMapMousePosition(Vector2D mouseDisplayPosition) {

        this.mouseDisplayPosition = mouseDisplayPosition;
        updateStatusBar();
    }

    /**
     * Refreshes latest-task thumbnails when membership or selection changes.
     *
     * <p>Task names form the membership cache because archived thumbnail content
     * is stable after completion. Avoiding equivalent redraws saves repeated
     * image scaling and component reconstruction.</p>
     */
    public void updateTaskThumbnails() {

        List<TaskThumbnailSnapshot> thumbnails =
                model.getLatestTaskThumbnails(view.getTaskThumbnailCapacity());

        List<String> taskNames = thumbnails.stream()
                .map(TaskThumbnailSnapshot::getName)
                .collect(Collectors.toList());

        boolean thumbnailListChanged =
                !taskNames.equals(visibleThumbnailTaskNames);

        String selectedTaskName = selectionController.getSelectedTaskName();

        boolean selectionChanged =
                !Objects.equals(selectedTaskName, lastThumbnailSelectedTaskName);

        if (!thumbnailListChanged && !selectionChanged) {
            return;
        }

        visibleThumbnailTaskNames = taskNames;
        lastThumbnailSelectedTaskName = selectedTaskName;

        view.displayTaskThumbnails(thumbnails, selectedTaskName);
    }

    /**
     * Updates the compact status bar with current contextual information
     * and the latest simulation activity message.
     */
    private void updateStatusBar() {

        Vector2D mouseWorldPosition = mouseDisplayPosition == null
                ? null
                : model.displayToWorldMeters(mouseDisplayPosition);

        StatusBarViewData data = new StatusBarViewData(
                mouseWorldPosition,
                selectionController.getSelectionStatusText(),
                model.getQueuedTaskCount(),
                model.getTaskArchiveSize(),
                latestActivityMessage);

        view.displayStatusBar(data);
    }
}
