package io.github.danhjalmberg.dronephotoservice.models.tasks;

/**
 * Requests a recorded zoom sequence centered on a target position.
 *
 * @author Dan Hjälmberg
 */
public class ZoomTask extends Task {

    /**
     * Creates a zoom task with its default description.
     */
    public ZoomTask() {

        super(TaskType.ZOOM);

        String desc = """
                Go to target position.
                Zoom in on target.
                Record a video.""";
        setDescription(desc);
    }
}
