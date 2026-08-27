package io.github.danhjalmberg.dronephotoservice.models;

import io.github.danhjalmberg.dronephotoservice.models.events.SimulationEvent;
import io.github.danhjalmberg.dronephotoservice.models.events.SimulationEventType;
import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for selected deterministic behavior of the {@link Model} class.
 * The tests cover representative simulation timing, clock management, event
 * handling, pool-size validation, base-position validation, and reset behavior
 * without duplicating tests for delegated map and archive functionality or
 * starting simulation actor threads.
 *
 * @author Dan Hjälmberg
 */
class ModelTest {

    /**
     * Tolerance used when comparing floating-point results.
     */
    private static final double DELTA = 1.0e-9;

    /**
     * Model under test.
     */
    private Model model;

    /**
     * Creates a clean model before each test.
     */
    @BeforeEach
    void setUp() {
        model = new Model();
        model.resetSimulation();
    }

    /**
     * Stops any actors and executor pools left by a test.
     *
     * <p>This failure-safe cleanup also runs when a test assertion fails before
     * reaching an explicit lifecycle assertion or shutdown call.</p>
     *
     * @throws InterruptedException if actor-pool shutdown is interrupted
     */
    @AfterEach
    void tearDown() throws InterruptedException {
        model.stopSimulationActors();
        model.shutdownActorPools();
    }

    /**
     * Tests simulation timing calculations and clock behavior.
     */
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    class SimulationTimingTests {

        /**
         * Tests that the configured simulation tick is returned as the physics step.
         */
        @Test
        void setSimulationTickMsUpdatesPhysicsStep() {
            model.setSimulationTickMs(125);

            assertEquals(125, model.getPhysicsStepMs());
        }

        /**
         * Tests that invalid ticks are rejected without replacing the current tick.
         *
         * @param tickMs invalid simulation tick duration.
         */
        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void setSimulationTickMsRejectsNonPositiveValues(int tickMs) {
            model.setSimulationTickMs(125);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> model.setSimulationTickMs(tickMs));
            assertEquals(125, model.getPhysicsStepMs());
        }

        /**
         * Tests that invalid speed multipliers are rejected without replacing the
         * current multiplier.
         *
         * @param multiplier invalid simulation speed multiplier.
         */
        @ParameterizedTest
        @MethodSource("invalidSpeedMultipliers")
        void setSimulationSpeedMultiplierRejectsInvalidValues(double multiplier) {
            model.setSimulationSpeedMultiplier(2.0);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> model.setSimulationSpeedMultiplier(multiplier));
            assertEquals(2.0, model.getSimulationSpeedMultiplier(), DELTA);
        }

        /**
         * Supplies non-positive and non-finite speed multipliers.
         *
         * @return invalid multiplier arguments.
         */
        private Stream<Arguments> invalidSpeedMultipliers() {
            return Stream.of(
                    Arguments.of(0.0),
                    Arguments.of(-1.0),
                    Arguments.of(Double.NaN),
                    Arguments.of(Double.POSITIVE_INFINITY),
                    Arguments.of(Double.NEGATIVE_INFINITY));
        }

        /**
         * Tests conversion of the simulation tick from milliseconds to seconds.
         */
        @Test
        void getPhysicsDeltaTimeSecondsConvertsMillisecondsToSeconds() {
            model.setSimulationTickMs(250);

            assertEquals(0.25, model.getPhysicsDeltaTimeSeconds(), DELTA);
        }

        /**
         * Tests calculation of actor sleep from the physics step and speed multiplier.
         */
        @Test
        void getActorSleepMsUsesSpeedMultiplier() {
            model.setSimulationTickMs(100);
            model.setSimulationSpeedMultiplier(4.0);

            assertEquals(25, model.getActorSleepMs());
        }

        /**
         * Tests that actor sleep is clamped to at least one millisecond.
         */
        @Test
        void getActorSleepMsDoesNotReturnLessThanOne() {
            model.setSimulationTickMs(10);
            model.setSimulationSpeedMultiplier(1000.0);

            assertEquals(1, model.getActorSleepMs());
        }

        /**
         * Tests that a physics update advances the simulation clock by one step.
         */
        @Test
        void updatePhysicsAdvancesSimulationClockByOneStep() {
            model.setSimulationTickMs(50);

            model.updatePhysics();

            assertEquals(Duration.ofMillis(50), model.getSimulationTime());
        }

        /**
         * Tests that repeated physics updates accumulate simulation time.
         */
        @Test
        void repeatedPhysicsUpdatesAccumulateSimulationTime() {
            model.setSimulationTickMs(40);

            model.updatePhysics();
            model.updatePhysics();
            model.updatePhysics();

            assertEquals(Duration.ofMillis(120), model.getSimulationTime());
        }

        /**
         * Tests that resetting the simulation clock returns elapsed time to zero.
         */
        @Test
        void resetSimulationClockReturnsTimeToZero() {
            model.setSimulationTickMs(50);
            model.updatePhysics();

            model.resetSimulationClock();

            assertEquals(Duration.ZERO, model.getSimulationTime());
        }
    }

    /**
     * Tests chronological simulation-event behavior.
     */
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    class SimulationEventTests {

        /**
         * Tests that an added event can be retrieved in insertion order.
         */
        @Test
        void addSimulationEventStoresEvent() {
            model.addSimulationEvent(
                    Duration.ofSeconds(2),
                    SimulationEventType.TASK_ASSIGNED,
                    "drone_1",
                    "task_1 assigned");

            List<SimulationEvent> events = model.getSimulationEventsSince(0);

            assertEquals(1, events.size());
            assertEquals(Duration.ofSeconds(2), events.get(0).getSimulationTime());
            assertEquals(SimulationEventType.TASK_ASSIGNED, events.get(0).getType());
            assertEquals("drone_1", events.get(0).getSourceName());
            assertEquals("task_1 assigned", events.get(0).getMessage());
        }

        /**
         * Tests that all simulation event arguments are required.
         *
         * @param simulationTime simulation time.
         * @param type event type.
         * @param sourceName event source.
         * @param message event message.
         */
        @ParameterizedTest
        @MethodSource("eventArgumentsContainingNull")
        void addSimulationEventRejectsNullArguments(
                Duration simulationTime,
                SimulationEventType type,
                String sourceName,
                String message) {

            assertThrows(
                    NullPointerException.class,
                    () -> model.addSimulationEvent(
                            simulationTime,
                            type,
                            sourceName,
                            message));
        }

        /**
         * Provides event argument combinations containing one null value.
         *
         * @return invalid event argument combinations.
         */
        private Stream<Arguments> eventArgumentsContainingNull() {
            return Stream.of(
                    Arguments.of(
                            null,
                            SimulationEventType.TASK_ASSIGNED,
                            "drone_1",
                            "message"),
                    Arguments.of(
                            Duration.ZERO,
                            null,
                            "drone_1",
                            "message"),
                    Arguments.of(
                            Duration.ZERO,
                            SimulationEventType.TASK_ASSIGNED,
                            null,
                            "message"),
                    Arguments.of(
                            Duration.ZERO,
                            SimulationEventType.TASK_ASSIGNED,
                            "drone_1",
                            null)
            );
        }

        /**
         * Tests that reset clears previously recorded simulation events.
         */
        @Test
        void resetSimulationClearsSimulationEvents() {
            model.addSimulationEvent(
                    Duration.ZERO,
                    SimulationEventType.TASK_ASSIGNED,
                    "drone_1",
                    "task assigned");

            model.resetSimulation();

            assertTrue(model.getSimulationEventsSince(0).isEmpty());
        }
    }

    /**
     * Tests validation and empty-model behavior.
     */
    @Nested
    class ValidationTests {

        /**
         * Tests that non-positive photo-agency pool sizes are rejected.
         *
         * @param poolSize invalid pool size.
         */
        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void createPhotoAgencyPoolRejectsNonPositiveSize(int poolSize) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> model.createPhotoAgencyPool(poolSize));
        }

        /**
         * Tests that non-positive drone pool sizes are rejected.
         *
         * @param poolSize invalid pool size.
         */
        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void createDronePoolRejectsNonPositiveSize(int poolSize) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> model.createDronePool(poolSize));
        }

        /**
         * Tests that an active photo-agency executor cannot be replaced and
         * that creation becomes available again after termination.
         *
         * @throws InterruptedException if actor-pool shutdown is interrupted
         */
        @Test
        void createPhotoAgencyPoolRequiresPreviousPoolToTerminate()
                throws InterruptedException {

            model.createPhotoAgencyPool(1);

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> model.createPhotoAgencyPool(1));

            assertEquals(
                    "Photo agency pool cannot be replaced while it is active or terminating.",
                    exception.getMessage());

            assertTrue(model.shutdownActorPools());

            assertDoesNotThrow(() -> model.createPhotoAgencyPool(1));
            assertTrue(model.shutdownActorPools());
        }

        /**
         * Tests that an active drone executor cannot be replaced and that
         * creation becomes available again after termination.
         *
         * @throws InterruptedException if actor-pool shutdown is interrupted
         */
        @Test
        void createDronePoolRequiresPreviousPoolToTerminate()
                throws InterruptedException {

            model.createDronePool(1);

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> model.createDronePool(1));

            assertEquals(
                    "Drone pool cannot be replaced while it is active or terminating.",
                    exception.getMessage());

            assertTrue(model.shutdownActorPools());

            assertDoesNotThrow(() -> model.createDronePool(1));
            assertTrue(model.shutdownActorPools());
        }

        /**
         * Tests that model drone creation selects and constructs a supported
         * enum-backed drone configuration.
         *
         * @throws InterruptedException if actor-pool shutdown is interrupted
         */
        @Test
        void addDroneCreatesSupportedRandomDroneType() {
            model.createDronePool(1);

            model.addDrone();
            assertEquals(1, model.getDroneCount());
        }

        /**
         * Tests that a null base position is rejected.
         */
        @Test
        void setBasePositionMetersRejectsNull() {
            assertThrows(NullPointerException.class,
                    () -> model.setBasePositionMeters(null));
        }

        /**
         * Tests that an empty model returns no live camera image.
         */
        @Test
        void getDroneLiveCameraImageReturnsNullWhenDroneIsMissing() {
            assertNull(model.getDroneLiveCameraImage("drone_1"));
            assertNull(model.getDroneLiveCameraImage(null));
        }

        /**
         * Tests that reset leaves actor and archive collections empty
         * and resets the simulation clock.
         */
        @Test
        void resetSimulationRestoresEmptyModelState() {
            model.setSimulationTickMs(50);
            model.updatePhysics();

            model.resetSimulation();

            assertEquals(0, model.getPhotoAgencyCount());
            assertEquals(0, model.getDroneCount());
            assertEquals(0, model.getQueuedTaskCount());
            assertEquals(0, model.getTaskArchiveSize());
            assertEquals(Duration.ZERO, model.getSimulationTime());
        }

        /**
         * Tests that setting a non-null base position is accepted.
         */
        @Test
        void setBasePositionMetersAcceptsValidPosition() {
            assertDoesNotThrow(
                    () -> model.setBasePositionMeters(
                            new Vector2D(10.0, 20.0)));
        }

        /**
         * Tests that reset is rejected while a photo-agency executor is active.
         */
        @Test
        void resetSimulationRejectsActivePhotoAgencyPool() throws InterruptedException {

            model.createPhotoAgencyPool(1);

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    model::resetSimulation);

            assertEquals(
                    "Simulation cannot be reset while actor executors are active or still terminating.",
                    exception.getMessage());

            assertTrue(model.shutdownActorPools());

            assertDoesNotThrow(model::resetSimulation);
        }

        /**
         * Tests that reset is rejected while a drone executor is active.
         */
        @Test
        void resetSimulationRejectsActiveDronePool() throws InterruptedException {

            model.createDronePool(1);

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    model::resetSimulation);

            assertEquals(
                    "Simulation cannot be reset while actor executors are active or still terminating.",
                    exception.getMessage());

            assertTrue(model.shutdownActorPools());

            assertDoesNotThrow(model::resetSimulation);
        }
    }
}
