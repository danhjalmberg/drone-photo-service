/**
 * Coordinates application workflows between the simulation model and Swing
 * view.
 *
 * <p>The root controller composes specialized controllers for simulation
 * lifecycle, command dispatch, view refresh, selection and archive viewing,
 * playback, map loading, control state, and image export. They coordinate EDT
 * work with background physics, export, and executor-shutdown workflows while
 * keeping the model independent of Swing.</p>
 */
package io.github.danhjalmberg.dronephotoservice.controllers;
