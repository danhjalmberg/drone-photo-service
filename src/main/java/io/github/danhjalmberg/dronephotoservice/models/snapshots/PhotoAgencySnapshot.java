package io.github.danhjalmberg.dronephotoservice.models.snapshots;


/**
 * Immutable summary of a photo agency for presentation outside the model.
 *
 * <p>The pending-task fields describe the agency state when the snapshot was
 * created; they do not retain the task or agency object.</p>
 *
 * @author Dan Hjälmberg
 */
public final class PhotoAgencySnapshot {

    private final int id;
    private final String name;
    private final int createdTaskCount;
    private final String pendingTaskName;
    private final boolean hasPendingTask;

    /**
     * Creates an agency summary.
     *
     * @param id               agency identifier.
     * @param name             agency name.
     * @param createdTaskCount number of tasks created by the agency.
     * @param pendingTaskName  pending task name, or an empty string if absent.
     * @param hasPendingTask   whether the agency has work awaiting submission.
     */
    public PhotoAgencySnapshot(int id, String name, int createdTaskCount,
                               String pendingTaskName, boolean hasPendingTask) {
        this.id = id;
        this.name = name;
        this.createdTaskCount = createdTaskCount;
        this.pendingTaskName = pendingTaskName;
        this.hasPendingTask = hasPendingTask;
    }

    /**
     * Returns the photo agency's identifier.
     *
     * @return agency identifier
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the photo agency's display name.
     *
     * @return agency name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the number of tasks created by the agency when this snapshot was
     * taken.
     *
     * @return number of tasks created by the agency
     */
    public int getCreatedTaskCount() {
        return createdTaskCount;
    }

    /**
     * Returns the name of the task awaiting submission when this snapshot was
     * taken.
     *
     * @return pending task name, or an empty string if absent
     */
    public String getPendingTaskName() {
        return pendingTaskName;
    }

    /**
     * Reports whether the agency had work awaiting submission when this snapshot
     * was taken.
     *
     * @return {@code true} if the agency has work awaiting submission
     */
    public boolean hasPendingTask() {
        return hasPendingTask;
    }
}
