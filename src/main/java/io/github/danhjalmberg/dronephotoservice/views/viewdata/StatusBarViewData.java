package io.github.danhjalmberg.dronephotoservice.views.viewdata;

import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;

/**
 * Supplies contextual values rendered by the compact status bar.
 *
 * <p>Unlike map-rendering data, the mouse position is expressed in world meters
 * after controller-side coordinate conversion. The south-panel view formats the
 * coordinates and provides fallbacks for absent text.</p>
 *
 * @author Dan Hjälmberg
 */
public class StatusBarViewData {

    private final Vector2D mouseWorldPositionMeters;
    private final String selectionText;
    private final int queuedTaskCount;
    private final int completedTaskCount;
    private final String activityMessage;

    /**
     * Creates a status-bar data value.
     *
     * @param mouseWorldPositionMeters current mouse position in world meters,
     *                                 or null if the mouse is outside the map
     * @param selectionText            current selection text
     * @param queuedTaskCount          number of queued tasks
     * @param completedTaskCount       number of completed tasks
     * @param activityMessage          contextual activity message text
     */
    public StatusBarViewData(
            Vector2D mouseWorldPositionMeters,
            String selectionText,
            int queuedTaskCount,
            int completedTaskCount,
            String activityMessage) {

        this.mouseWorldPositionMeters = mouseWorldPositionMeters;
        this.selectionText = selectionText;
        this.queuedTaskCount = queuedTaskCount;
        this.completedTaskCount = completedTaskCount;
        this.activityMessage = activityMessage;
    }

    /**
     * Returns the map position beneath the mouse pointer.
     *
     * @return mouse position in world meters, or {@code null} if unavailable
     */
    public Vector2D getMouseWorldPositionMeters() {
        return mouseWorldPositionMeters;
    }

    /**
     * Returns the text describing the current selection.
     *
     * @return selection text
     */
    public String getSelectionText() {
        return selectionText;
    }

    /**
     * Returns the number of tasks currently waiting in the queue.
     *
     * @return queued task count
     */
    public int getQueuedTaskCount() {
        return queuedTaskCount;
    }

    /**
     * Returns the number of completed tasks represented by this status data.
     *
     * @return completed task count
     */
    public int getCompletedTaskCount() {
        return completedTaskCount;
    }

    /**
     * Returns the contextual activity message displayed in the status bar.
     *
     * @return activity message text
     */
    public String getActivityMessage() {
        return activityMessage;
    }
}
