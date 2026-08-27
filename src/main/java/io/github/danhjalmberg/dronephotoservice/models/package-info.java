/**
 * Owns the simulation domain and its application-wide state.
 *
 * <p>The main model coordinates actors, tasks, map data, simulation time,
 * event history, archives, and actor executors. Controllers primarily consume
 * operations and read-oriented snapshots; individual snapshot types document
 * where mutable component or image references remain shared.</p>
 */
package io.github.danhjalmberg.dronephotoservice.models;
