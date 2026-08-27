package io.github.danhjalmberg.dronephotoservice.models.tasks;

/**
 * Requests one high-resolution photo at a target position.
 *
 * @author Dan Hjälmberg
 */
public class PhotoTask extends Task {

    /**
     * Creates a photo task with its default description.
     */
    public PhotoTask() {

        super(TaskType.PHOTO);

        String desc = """
                Go to target position.
                Take high resolution photo.""";
        setDescription(desc);
    }
}
