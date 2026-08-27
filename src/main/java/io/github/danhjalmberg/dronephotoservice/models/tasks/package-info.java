/**
 * Defines photo, video, and zoom work and its lifecycle containers.
 *
 * <p>Tasks retain their origin, target, simulation timestamps, captured
 * images, and capture positions. The shared bounded task queue coordinates
 * producers and drones, while each model's bounded archive retains completed
 * task results and releases image references as old entries are evicted.</p>
 */
package io.github.danhjalmberg.dronephotoservice.models.tasks;
