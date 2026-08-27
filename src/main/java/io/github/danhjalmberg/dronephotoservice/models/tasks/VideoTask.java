package io.github.danhjalmberg.dronephotoservice.models.tasks;

/**
 * Requests a fixed-rate sequence of frames while flying toward a target.
 *
 * <p>Recording begins when drone processing starts and finishes at the target.
 * Capture timing is independent of GUI refreshes; frame positions are
 * interpolated along movement steps. When the configured retention limit is
 * reached, the task keeps the newest frames and corresponding positions.</p>
 *
 * @author Dan Hjälmberg
 */
public class VideoTask extends Task {

    /**
     * Creates a video task with its default description.
     */
    public VideoTask() {

        super(TaskType.VIDEO);

        String desc = """
                Fly to target position.
                Record video frames during the flight.""";

        setDescription(desc);
    }
}
