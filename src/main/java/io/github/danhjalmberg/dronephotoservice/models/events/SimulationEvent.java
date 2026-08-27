package io.github.danhjalmberg.dronephotoservice.models.events;

import io.github.danhjalmberg.dronephotoservice.support.TimeUtils;

import java.time.Duration;

/**
 * Immutable description of an event emitted by the simulation.
 *
 * <p>The sequence number identifies insertion order within the originating
 * {@link SimulationEventLog}; the elapsed simulation timestamp is descriptive
 * and does not determine log ordering.</p>
 */
public class SimulationEvent {

    private final long sequenceNumber;
    private final Duration simulationTime;
    private final SimulationEventType type;
    private final String sourceName;
    private final String message;

    /**
     * Creates a simulation event.
     *
     * @param sequenceNumber insertion sequence assigned by the originating log
     * @param simulationTime elapsed simulation time when the event occurred
     * @param type event type
     * @param sourceName name of the actor or system that produced the event
     * @param message human-readable event message
     */
    public SimulationEvent(
            long sequenceNumber,
            Duration simulationTime,
            SimulationEventType type,
            String sourceName,
            String message) {

        this.sequenceNumber = sequenceNumber;
        this.simulationTime = simulationTime;
        this.type = type;
        this.sourceName = sourceName;
        this.message = message;
    }

    /**
     * Returns the insertion sequence assigned by the originating log.
     *
     * @return event sequence number
     */
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Returns the elapsed simulation time associated with the event.
     *
     * @return the elapsed simulation time
     */
    public Duration getSimulationTime() {
        return simulationTime;
    }

    /**
     * Returns the event category.
     *
     * @return the event's type
     */
    public SimulationEventType getType() {
        return type;
    }

    /**
     * Returns the actor or subsystem that emitted the event.
     *
     * @return the name of the actor or system that produced the event
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Returns the human-readable event description.
     *
     * @return the event's message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Formats the elapsed simulation time as {@code HH:mm:ss}.
     *
     * @return formatted simulation time, or an empty string if the timestamp is
     *         {@code null}
     */
    public String getFormattedSimulationTime() {
        return TimeUtils.formatSimulationTime(simulationTime);
    }
}
