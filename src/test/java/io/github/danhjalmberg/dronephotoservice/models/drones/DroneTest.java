package io.github.danhjalmberg.dronephotoservice.models.drones;

import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.DroneSnapshot;
import io.github.danhjalmberg.dronephotoservice.models.tasks.Task;
import io.github.danhjalmberg.dronephotoservice.models.tasks.TaskFactory;
import io.github.danhjalmberg.dronephotoservice.models.tasks.TaskType;
import io.github.danhjalmberg.dronephotoservice.settings.ModelSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Unit tests for selected deterministic behavior of the {@link Drone} class.
 * The tests cover representative identity, position initialization, battery
 * sufficiency, synchronous movement, state transitions, battery consumption,
 * and defensive copying without starting the drone worker thread or processing
 * camera tasks.
 *
 * @author Dan Hjälmberg
 */
class DroneTest {

    /**
     * Tolerance used when comparing floating-point results.
     */
    private static final double DELTA = 1.0e-9;

    /**
     * Drone under test.
     */
    private Drone drone;

    /**
     * Creates a representative assembled drone before each test.
     */
    @BeforeEach
    void setUp() {
        drone = new DroneFactory().createDrone(DroneType.TYPE_1);
    }

    /**
     * Tests identity and initial position behavior.
     */
    @Nested
    class IdentityAndPositionTests {

        /**
         * Tests that setting an identifier creates the expected drone name.
         */
        @Test
        void setIdCreatesDroneName() {
            drone.setName(7);

            assertEquals("drone_7", drone.getName());
        }

        /**
         * Tests that setting the base position also initializes the current position.
         */
        @Test
        void setBasePositionInitializesCurrentPosition() {
            Vector2D basePosition = new Vector2D(100.0, 200.0);

            drone.setBasePositionMeters(basePosition);

            assertSame(basePosition, drone.getBasePositionMeters());
            assertSame(basePosition, drone.getCurrentPositionMeters());
        }

        /**
         * Tests that the completed-task list is returned as a defensive copy.
         */
        @Test
        void getCompletedTasksReturnsDefensiveCopy() {
            List<?> firstResult = drone.getCompletedTasks();
            List<?> secondResult = drone.getCompletedTasks();

            assertNotSame(firstResult, secondResult);
        }
    }

    /**
     * Tests battery sufficiency calculations.
     */
    @Nested
    class BatteryTests {

        /**
         * Tests that a fully charged drone at its base has sufficient battery.
         */
        @Test
        void sufficientBatteryReturnsTrueForFullyChargedDroneAtBase() {
            Vector2D basePosition = new Vector2D(0.0, 0.0);
            drone.setBasePositionMeters(basePosition);

            assertTrue(drone.sufficientBattery());
        }

        /**
         * Tests that a drone at its base with only the safety margin remaining
         * does not have sufficient battery.
         */
        @Test
        void sufficientBatteryRequiresMoreThanSafetyMargin() {
            Vector2D basePosition = new Vector2D(0.0, 0.0);
            drone.setBasePositionMeters(basePosition);

            double remainingCharge = 30.0;
            double amountToConsume =
                    drone.getBattery().getCapacitySeconds() - remainingCharge;

            drone.getBattery().consume(amountToConsume);

            assertFalse(drone.sufficientBattery());
        }
    }

    /**
     * Tests immutable presentation snapshots of drone component state.
     */
    @Nested
    class SnapshotTests {

        /**
         * Tests that battery and motor values remain fixed after the live
         * components change.
         */
        @Test
        void componentValuesRemainFixedAfterSnapshotCreation() {
            drone.setName(3);
            drone.setBasePositionMeters(new Vector2D(10.0, 20.0));
            drone.getMotor().setSpeed(12.5);

            double capturedCharge =
                    drone.getBattery().getCurrentChargeSeconds();
            DroneSnapshot snapshot = drone.createSnapshot();

            drone.getBattery().consume(5.0);
            drone.getMotor().setSpeed(2.0);

            assertEquals("drone_3", snapshot.getName());
            assertEquals(
                    capturedCharge,
                    snapshot.getBattery().getCurrentChargeSeconds(),
                    DELTA);
            assertEquals(
                    drone.getBattery().getCapacitySeconds(),
                    snapshot.getBattery().getCapacitySeconds(),
                    DELTA);
            assertEquals(12.5, snapshot.getMotor().getCurrentSpeed(), DELTA);
            assertEquals(drone.getCamera().getType(), snapshot.getCamera().getType());
        }
    }

    /**
     * Tests synchronous movement and state transitions.
     */
    @Nested
    class MovementTests {

        /**
         * Tests that moving toward a task changes state, advances position,
         * and consumes battery.
         */
        @Test
        void moveToTaskAdvancesDroneAndConsumesBattery() {
            drone.setBasePositionMeters(new Vector2D(0.0, 0.0));

            Task task = createTaskAt(new Vector2D(100.0, 0.0));
            drone.setTask(task);

            double initialCharge = drone.getBattery().getCurrentChargeSeconds();

            drone.moveToTask(1.0);

            assertEquals(DroneState.MOVING_TO_TASK, drone.getState());
            assertTrue(drone.getCurrentPositionMeters().getX() > 0.0);
            assertEquals(0.0, drone.getCurrentPositionMeters().getY(), DELTA);
            assertEquals(initialCharge - 1.0, drone.getBattery().getCurrentChargeSeconds(), DELTA);
        }

        /**
         * Tests that movement does not overshoot a nearby task position.
         */
        @Test
        void moveToTaskStopsAtNearbyTarget() {
            drone.setBasePositionMeters(new Vector2D(0.0, 0.0));

            Vector2D target = new Vector2D(1.0, 0.0);
            drone.setTask(createTaskAt(target));

            drone.moveToTask(10.0);

            assertSame(target, drone.getCurrentPositionMeters());
            assertEquals(0.0, drone.getMotor().getCurrentSpeed(), DELTA);
        }

        /**
         * Tests that returning to base changes state and reaches the base
         * without overshooting.
         */
        @Test
        void returnToBaseMovesDroneBackToBase() {
            Vector2D basePosition = new Vector2D(0.0, 0.0);
            drone.setBasePositionMeters(basePosition);

            drone.setTask(createTaskAt(new Vector2D(10.0, 0.0)));
            drone.moveToTask(10.0);

            drone.returnToBase(10.0, Duration.ofSeconds(20));

            assertEquals(DroneState.RETURNING_TO_BASE, drone.getState());
            assertSame(basePosition, drone.getCurrentPositionMeters());
            assertEquals(0.0, drone.getMotor().getCurrentSpeed(), DELTA);
        }
    }

    /**
     * Tests priority separation between essential task results and optional
     * video-frame work.
     */
    @Nested
    class WorkQueueTests {

        /**
         * A full optional queue must not prevent the only task-result job from
         * being submitted.
         *
         * @throws Exception if reflection fails
         */
        @Test
        void fullVideoFrameQueueDoesNotRejectTaskResultWork() throws Exception {
            BlockingQueue<Runnable> videoQueue = getWorkQueue("videoFrameWorkQueue");

            while (videoQueue.remainingCapacity() > 0) {
                videoQueue.add(() -> {
                });
            }

            Task videoTask = TaskFactory.INSTANCE.createTask(TaskType.VIDEO);
            videoTask.setTargetPositionMeters(new Vector2D(10.0, 10.0));
            drone.setTask(videoTask);

            drone.processVideoTask(0.05, Duration.ofSeconds(1));

            BlockingQueue<Runnable> essentialQueue =
                    getWorkQueue("essentialDroneWorkQueue");

            assertEquals(ModelSettings.VIDEO_TASK_MAX_FRAMES, videoQueue.size());
            assertEquals(1, essentialQueue.size());
            assertEquals(DroneState.PROCESSING_TASK, drone.getState());
        }

        /**
         * A full pending-frame window discards its oldest job, not its newest.
         *
         * @throws Exception if reflection fails
         */
        @Test
        void fullVideoFrameQueueKeepsNewestPendingWork() throws Exception {
            AtomicBoolean oldestExecuted = new AtomicBoolean(false);
            AtomicBoolean newestExecuted = new AtomicBoolean(false);

            submitVideoFrameWork(() -> oldestExecuted.set(true));

            BlockingQueue<Runnable> videoQueue = getWorkQueue("videoFrameWorkQueue");
            while (videoQueue.remainingCapacity() > 0) {
                submitVideoFrameWork(() -> {
                });
            }

            submitVideoFrameWork(() -> newestExecuted.set(true));

            Runnable work;
            while ((work = videoQueue.poll()) != null) {
                work.run();
            }

            assertFalse(oldestExecuted.get());
            assertTrue(newestExecuted.get());
        }

        /**
         * Aborting a task removes all of its not-yet-started worker jobs.
         *
         * @throws Exception if reflection fails
         */
        @Test
        void abortClearsQueuedFrameAndResultWork() throws Exception {
            Task videoTask = TaskFactory.INSTANCE.createTask(TaskType.VIDEO);
            videoTask.setTargetPositionMeters(new Vector2D(10.0, 10.0));
            drone.setTask(videoTask);

            BlockingQueue<Runnable> videoQueue = getWorkQueue("videoFrameWorkQueue");
            BlockingQueue<Runnable> essentialQueue =
                    getWorkQueue("essentialDroneWorkQueue");

            videoQueue.add(() -> {
            });
            videoQueue.add(() -> {
            });
            essentialQueue.add(() -> {
            });

            drone.discardTask(Duration.ofSeconds(1));

            assertTrue(videoQueue.isEmpty());
            assertTrue(essentialQueue.isEmpty());
            assertNull(drone.getTask());
        }

    }

    // ########################################################################
    // Helper methods
    // ########################################################################

    /**
     * Creates a photo task at the supplied world-meter position.
     *
     * @param targetPosition target position in world meters.
     * @return configured photo task.
     */
    private static Task createTaskAt(Vector2D targetPosition) {
        Task task = TaskFactory.INSTANCE.createTask(TaskType.PHOTO);
        task.setTargetPositionMeters(targetPosition);
        return task;
    }

    /**
     * Gets a private drone work queue for deterministic queue behavior tests.
     *
     * @param fieldName queue field name
     * @return selected work queue
     * @throws Exception if reflection fails
     */
    @SuppressWarnings("unchecked")
    private BlockingQueue<Runnable> getWorkQueue(String fieldName) throws Exception {
        Field field = Drone.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (BlockingQueue<Runnable>) field.get(drone);
    }

    /**
     * Invokes optional-frame submission for deterministic rolling-window tests.
     *
     * @param work work to submit
     * @throws Exception if reflection fails
     */
    private void submitVideoFrameWork(Runnable work) throws Exception {
        Method method = Drone.class.getDeclaredMethod("submitVideoFrameWork", Runnable.class);
        method.setAccessible(true);
        method.invoke(drone, work);
    }

}
