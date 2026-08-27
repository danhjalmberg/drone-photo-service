/**
 * Implements drones as mobile task consumers and simulation actors.
 *
 * <p>Physics updates control movement, battery behavior, timing, and state,
 * while dedicated actor threads perform image-result work. Drones acquire
 * queued tasks and return completed or aborted work to originating agencies.
 * {@link io.github.danhjalmberg.dronephotoservice.models.drones.DroneFactory}
 * selects a supported component-family factory and creates fully initialized
 * drones.</p>
 */
package io.github.danhjalmberg.dronephotoservice.models.drones;
