package io.github.danhjalmberg.dronephotoservice.models.photo_agencies;

import io.github.danhjalmberg.dronephotoservice.models.Model;
import io.github.danhjalmberg.dronephotoservice.models.events.SimulationEventType;
import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;
import io.github.danhjalmberg.dronephotoservice.models.tasks.Task;
import io.github.danhjalmberg.dronephotoservice.models.tasks.TaskFactory;
import io.github.danhjalmberg.dronephotoservice.models.tasks.TaskType;
import io.github.danhjalmberg.dronephotoservice.models.tasks.TaskQueue;
import io.github.danhjalmberg.dronephotoservice.settings.ModelSettings;
import io.github.danhjalmberg.dronephotoservice.support.TimeUtils;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Produces capture tasks and receives their completed or aborted results.
 *
 * <p>Physics updates advance a randomized production countdown and submit
 * production work to this actor's bounded work queue. The dedicated actor
 * thread creates or retries tasks and attempts non-blocking insertion into the
 * shared {@link TaskQueue}. A task remains pending while that queue is full.</p>
 *
 * <p>Completed tasks are timestamped and transferred to the model archive.
 * Aborted tasks have their execution state reset and enter a retry backlog;
 * returned work is prioritized over creation of new work.</p>
 *
 * @author Dan Hjälmberg
 */
public class PhotoAgency implements Runnable {

    private static final int PHOTO_AGENCY_WORK_QUEUE_CAPACITY = 10;

    private final int id;
    private final String name;
    private int taskId = 0;
    private final TaskQueue taskQueue;
    private Task task;
    private final Model model;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);

    private final BlockingQueue<Runnable> photoAgencyWorkQueue =
            new LinkedBlockingQueue<>(PHOTO_AGENCY_WORK_QUEUE_CAPACITY);

    private final Queue<Task> returnedTasks = new ArrayDeque<>();
    private boolean pendingTaskIsReturned = false;

    private double timeUntilNextTaskSeconds;
    private final Random random = new Random();
    private volatile boolean taskCreationWorkQueued = false;

    private String log = "";

    /**
     * Creates an agency connected to the supplied queue and model.
     *
     * @param id        agency identifier used to generate its name.
     * @param taskQueue shared task queue used for submission.
     * @param model     model used for positions, events, and task archival.
     */
    public PhotoAgency(int id, TaskQueue taskQueue, Model model) {
        this.id = id;
        this.name = "photo_agency_" + id;
        this.taskQueue = taskQueue;
        this.model = model;

        log(this.name);
    }

    /**
     * Returns the agency identifier.
     *
     * @return agency identifier.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the generated agency name.
     *
     * @return name in the form {@code photo_agency_<id>}.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the number of new tasks created by this agency.
     *
     * <p>Retries of aborted tasks do not increment this count.</p>
     *
     * @return number of created tasks.
     */
    public synchronized int getCreatedTaskCount() {
        return taskId;
    }

    /**
     * Returns the task currently awaiting successful queue insertion.
     *
     * <p>This does not report tasks still held in the aborted-task retry
     * backlog.</p>
     *
     * @return name of pending task, or empty string if no pending task.
     */
    public synchronized String getPendingTaskName() {
        return task == null ? "" : task.getName();
    }

    /**
     * Reports whether a task is currently awaiting successful queue insertion.
     *
     * <p>This does not include tasks still in the retry backlog.</p>
     *
     * @return {@code true} if an insertion candidate is pending.
     */
    public synchronized boolean hasPendingTask() {
        return task != null;
    }

    /**
     * Runs queued agency work on the calling thread until stopped or interrupted.
     *
     * <p>Pausing delays execution of dequeued work without terminating the
     * loop. An interrupt restores the thread's interrupt status and ends the
     * actor.</p>
     */
    @Override
    public void run() {

        log("Running in " + Thread.currentThread().getName());

        isRunning.set(true);

        resetProductionCountdown();

        while (isRunning.get()) {
            try {
                Runnable work = photoAgencyWorkQueue.poll(
                        ModelSettings.ACTOR_IDLE_PAUSE_MS,
                        TimeUnit.MILLISECONDS);

                if (work == null) {
                    continue;
                }

                while (isPaused.get() && isRunning.get()) {
                    Thread.sleep(ModelSettings.ACTOR_IDLE_PAUSE_MS);
                }

                if (!isRunning.get()) {
                    return;
                }

                work.run();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Requests cooperative suspension of queued agency work.
     */
    public void pause() {
        isPaused.set(true);
    }

    /**
     * Allows queued agency work to resume.
     */
    public void resume() {
        isPaused.set(false);
    }

    /**
     * Requests cooperative termination of the actor loop.
     */
    public void stop() {
        isRunning.set(false);
    }


    /**
     * Attempts to submit work to the bounded agency work queue.
     *
     * @param work the work to be executed.
     * @return {@code true} if accepted; {@code false} if the queue is full.
     */
    private boolean submitPhotoAgencyWork(Runnable work) {

        if (!photoAgencyWorkQueue.offer(work)) {
            log("Photo agency work queue full. Dropping work item.");
            return false;
        }

        return true;
    }

    /**
     * Selects a new production delay from the configured interval range.
     */
    private void resetProductionCountdown() {

        double min = ModelSettings.TASK_CREATION_INTERVAL_MIN_SECONDS;
        double max = ModelSettings.TASK_CREATION_INTERVAL_MAX_SECONDS;

        timeUntilNextTaskSeconds = min + random.nextDouble() * (max - min);
    }

    /**
     * Advances task production by one physics update.
     *
     * <p>No work is scheduled while the actor is stopped or paused. A pending
     * task is retried without advancing the creation countdown. At most one
     * production work item may be outstanding.</p>
     *
     * @param dt             simulated seconds advanced by the update.
     * @param simulationTime elapsed simulation time.
     */
    public synchronized void updateProduction(double dt, Duration simulationTime) {

        // If the thread is not running, or the simulation is paused, do nothing
        if (!isRunning.get() || isPaused.get()) {
            return;
        }

        // If there is a pending task, try to enqueue it
        if (task != null) {
            queueTaskProductionWork(simulationTime);
            return;
        }

        // Countdown for next task creation
        timeUntilNextTaskSeconds -= dt;

        // When the countdown interval is over, enqueue task creation work
        if (timeUntilNextTaskSeconds <= 0.0) {
            queueTaskProductionWork(simulationTime);
        }
    }

    /**
     * Schedules the next create-or-retry attempt on the agency actor.
     *
     * @param simulationTime elapsed simulation time.
     */
    private void queueTaskProductionWork(Duration simulationTime) {

        if (taskCreationWorkQueued) {
            return;
        }

        taskCreationWorkQueued = true;

        boolean submitted = submitPhotoAgencyWork(() -> {
            try {
                synchronized (this) {

                    if (task == null) {
                        Task returnedTask = returnedTasks.poll();

                        if (returnedTask != null) {
                            task = returnedTask;
                            pendingTaskIsReturned = true;

                        } else {
                            task = createTask(simulationTime);
                            pendingTaskIsReturned = false;
                        }
                    }

                    if (taskQueue.addTask(task)) {

                        if (pendingTaskIsReturned) {
                            log(task.getName() + " requeued.");

                            model.addSimulationEvent(
                                    simulationTime,
                                    SimulationEventType.TASK_REQUEUED,
                                    name,
                                    task.getName() + " requeued by " + name);

                        } else {
                            log(task.getName() + " enqueued.");

                            model.addSimulationEvent(
                                    simulationTime,
                                    SimulationEventType.TASK_ENQUEUED,
                                    name,
                                    task.getName() + " enqueued by " + name);
                        }

                        task = null;
                        pendingTaskIsReturned = false;
                        resetProductionCountdown();
                    }
                }
            } finally {
                taskCreationWorkQueued = false;
            }
        });

        if (!submitted) {
            taskCreationWorkQueued = false;
        }
    }

    /**
     * Creates and initializes a randomly selected supported task type.
     *
     * <p>The task receives the supplied creation time, an agency-local sequence
     * name, this agency as its origin, and a random safe target in world
     * meters. Creation does not submit the task to the shared queue.</p>
     *
     * @param simulationTime elapsed simulation time.
     * @return initialized task.
     */
    public Task createTask(Duration simulationTime) {

        TaskType[] taskTypes = TaskType.values();
        TaskType taskType = taskTypes[random.nextInt(taskTypes.length)];

        TaskFactory taskFactory = TaskFactory.INSTANCE;
        Task newTask = taskFactory.createTask(taskType);

        newTask.setCreationSimulationTime(simulationTime);

        newTask.setName(++taskId, this);

        Vector2D positionMeters = model.createRandomTaskPositionMeters();
        newTask.setTargetPositionMeters(positionMeters);

        log(newTask.getName()
                + " created at "
                + TimeUtils.formatSimulationTime(newTask.getCreationSimulationTime()));

        model.addSimulationEvent(
                simulationTime,
                SimulationEventType.TASK_CREATED,
                name,
                newTask.getName() + " created");

        return newTask;
    }

    /**
     * Records and archives a task completed by a drone.
     *
     * <p>A {@code null} task is ignored. The supplied elapsed time becomes the
     * task's completion timestamp before the archive event is emitted and the
     * task is transferred to the model archive.</p>
     *
     * @param task           completed task.
     * @param simulationTime elapsed simulation time.
     */
    public synchronized void receiveCompletedTask(Task task, Duration simulationTime) {

        if (task == null) {
            return;
        }

        task.setCompletionSimulationTime(simulationTime);

        log(task.getName() + " completed at " + TimeUtils.formatSimulationTime(task.getCompletionSimulationTime()));

        model.addSimulationEvent(
                simulationTime,
                SimulationEventType.TASK_ARCHIVED,
                name,
                task.getName() + " archived");

        model.addTaskToArchive(task);
    }

    /**
     * Resets an aborted task and appends it to the retry backlog.
     *
     * <p>A {@code null} task is ignored. Retry submission occurs on a later
     * production update and retains the task's identity, origin, creation
     * time, description, and target.</p>
     *
     * @param task      aborted task.
     * @param droneName name of the drone that aborted the task.
     */
    public synchronized void receiveAbortedTask(Task task, String droneName) {

        if (task == null) {
            return;
        }

        log(task.getName()
                + " returned by "
                + droneName
                + " due to insufficient battery.");

        task.resetExecutionState();
        returnedTasks.offer(task);
    }

    /**
     * Appends a line to the agency's in-memory diagnostic log.
     *
     * @param message message to append.
     */
    public synchronized void log(String message) {
        log += (message + "\n");
    }

    /**
     * Returns the accumulated diagnostic log.
     *
     * @return the diagnostic log as a string.
     */
    public synchronized String getLog() {
        return log;
    }
}
