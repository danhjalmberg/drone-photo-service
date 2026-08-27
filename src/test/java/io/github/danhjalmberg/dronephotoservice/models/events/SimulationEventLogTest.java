package io.github.danhjalmberg.dronephotoservice.models.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link SimulationEventLog} class.
 * The tests cover representative sequence numbering, filtering, clearing,
 * defensive copying, and bounded-history behavior.
 *
 * @author Dan Hjälmberg
 */
class SimulationEventLogTest {

    /**
     * Event log under test.
     */
    private SimulationEventLog eventLog;

    /**
     * Creates an empty event log before each test.
     */
    @BeforeEach
    void setUp() {
        eventLog = new SimulationEventLog();
    }

    /**
     * Tests sequence numbering and event retrieval.
     */
    @Nested
    class SequenceAndRetrievalTests {

        /**
         * Tests that events receive increasing sequence numbers in insertion order.
         */
        @Test
        void addAssignsIncreasingSequenceNumbers() {
            eventLog.add(
                    Duration.ofSeconds(1),
                    SimulationEventType.SIMULATION_STARTED,
                    "system",
                    "started");

            eventLog.add(
                    Duration.ofSeconds(2),
                    SimulationEventType.TASK_CREATED,
                    "agency_1",
                    "task created");

            List<SimulationEvent> events = eventLog.getEventsSince(0);

            assertEquals(1L, events.get(0).getSequenceNumber());
            assertEquals(2L, events.get(1).getSequenceNumber());
        }

        /**
         * Tests that retrieval returns only events newer than the supplied sequence number.
         */
        @Test
        void getEventsSinceReturnsOnlyNewerEvents() {
            addEvents(3);

            List<SimulationEvent> events = eventLog.getEventsSince(1);

            assertEquals(2, events.size());
            assertEquals(2L, events.get(0).getSequenceNumber());
            assertEquals(3L, events.get(1).getSequenceNumber());
        }

        /**
         * Tests that modifying a returned list does not modify the event log.
         */
        @Test
        void getEventsSinceReturnsDefensiveList() {
            addEvents(2);

            List<SimulationEvent> events = eventLog.getEventsSince(0);
            events.clear();

            assertEquals(2, eventLog.getEventsSince(0).size());
        }
    }

    /**
     * Tests clearing and bounded-history behavior.
     */
    @Nested
    class LifecycleTests {

        /**
         * Tests that clearing removes events and resets sequence numbering.
         */
        @Test
        void clearRemovesEventsAndResetsSequenceNumbers() {
            addEvents(2);

            eventLog.clear();
            eventLog.add(
                    Duration.ZERO,
                    SimulationEventType.SIMULATION_STARTED,
                    "system",
                    "restarted");

            List<SimulationEvent> events = eventLog.getEventsSince(0);

            assertEquals(1, events.size());
            assertEquals(1L, events.get(0).getSequenceNumber());
        }

        /**
         * Tests that the log retains only its latest one thousand events.
         */
        @Test
        void addDiscardsOldestEventsBeyondMaximumSize() {
            addEvents(1001);

            List<SimulationEvent> events = eventLog.getEventsSince(0);

            assertEquals(1000, events.size());
            assertEquals(2L, events.get(0).getSequenceNumber());
            assertEquals(1001L, events.get(events.size() - 1).getSequenceNumber());
        }

        /**
         * Tests that requesting events after the latest sequence returns an empty list.
         */
        @Test
        void getEventsSinceReturnsEmptyListWhenNoNewerEventsExist() {
            addEvents(2);

            assertTrue(eventLog.getEventsSince(2).isEmpty());
        }
    }

    // ########################################################################
    // Helper methods
    // ########################################################################

    /**
     * Adds a specified number of representative events to the log.
     *
     * @param count number of events to add.
     */
    private void addEvents(int count) {
        for (int i = 1; i <= count; i++) {
            eventLog.add(
                    Duration.ofSeconds(i),
                    SimulationEventType.TASK_CREATED,
                    "agency_1",
                    "event_" + i);
        }
    }
}
