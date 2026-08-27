package io.github.danhjalmberg.dronephotoservice.models.tasks;

/**
 * Creates the task implementations supported by the application.
 *
 * @author Dan Hjälmberg
 */
public final class TaskFactory {

    /**
     * Shared stateless task factory.
     */
    public static final TaskFactory INSTANCE = new TaskFactory();

    /**
     * Prevents construction outside this singleton class.
     */
    private TaskFactory() {
    }

    /**
     * Creates a new task for an application-defined type.
     *
     * @param taskType supported task type.
     * @return a new task.
     * @throws NullPointerException if {@code taskType} is {@code null}.
     */
    public Task createTask(TaskType taskType) {

        return switch (taskType) {
            case PHOTO -> new PhotoTask();
            case VIDEO -> new VideoTask();
            case ZOOM -> new ZoomTask();
        };
    }
}
