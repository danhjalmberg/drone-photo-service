/**
 * Records bounded events produced by the simulation in insertion order.
 *
 * <p>Immutable event values carry elapsed simulation timestamps, categories,
 * sources, messages, and log-assigned sequence numbers. The synchronized log
 * supports incremental polling while retaining only its latest entries.</p>
 */
package io.github.danhjalmberg.dronephotoservice.models.events;
