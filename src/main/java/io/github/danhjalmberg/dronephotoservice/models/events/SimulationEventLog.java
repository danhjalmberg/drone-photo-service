package io.github.danhjalmberg.dronephotoservice.models.events;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe, bounded event history ordered by insertion sequence.
 *
 * <p>Each call to {@link #add(Duration, SimulationEventType, String, String)}
 * receives the next sequence number while holding this log's monitor. Events
 * are not sorted by their elapsed simulation timestamps. Only the latest
 * 1,000 events are retained.</p>
 *
 * <p>Consumers can poll incrementally with {@link #getEventsSince(long)}. A
 * consumer that falls behind the retention window cannot recover events that
 * have already been evicted.</p>
 */
public class SimulationEventLog {

    private static final int MAX_EVENTS = 1000;

    private final List<SimulationEvent> events = new ArrayList<>();
    private long nextSequenceNumber = 1L;

    /**
     * Creates an empty event log whose first event receives sequence number
     * {@code 1}.
     */
    public SimulationEventLog() {
    }

    /**
     * Appends an event and assigns its insertion sequence number.
     *
     * <p>If the retention limit is exceeded, the oldest event is discarded.</p>
     *
     * @param simulationTime elapsed simulation time
     * @param type           event type
     * @param sourceName     event source name
     * @param message        human-readable event message
     */
    public synchronized void add(
            Duration simulationTime,
            SimulationEventType type,
            String sourceName,
            String message) {

        events.add(new SimulationEvent(
                nextSequenceNumber++,
                simulationTime,
                type,
                sourceName,
                message));

        while (events.size() > MAX_EVENTS) {
            events.remove(0);
        }
    }

    /**
     * Returns retained events with sequence numbers greater than the supplied
     * value.
     *
     * <p>Results are ordered from oldest to newest. The returned list is
     * independent of the log and may be modified by the caller. A sequence
     * number older than the retained history does not restore evicted events.</p>
     *
     * @param sequenceNumber last sequence number already processed by the caller
     * @return copied list of newer retained events in insertion order
     */
    public synchronized List<SimulationEvent> getEventsSince(long sequenceNumber) {

        List<SimulationEvent> result = new ArrayList<>();

        for (SimulationEvent event : events) {
            if (event.getSequenceNumber() > sequenceNumber) {
                result.add(event);
            }
        }

        return result;
    }

    /**
     * Removes all events and resets the next sequence number to {@code 1}.
     *
     * <p>Consumers that retain a previously processed sequence number must
     * reset that cursor when the log is cleared.</p>
     */
    public synchronized void clear() {
        events.clear();
        nextSequenceNumber = 1L;
    }
}
