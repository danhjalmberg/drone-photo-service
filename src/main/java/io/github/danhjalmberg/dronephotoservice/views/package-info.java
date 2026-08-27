/**
 * Implements the Swing presentation boundary and main-window façade.
 *
 * <p>The root view composes functional panels, exposes user interactions through
 * listener registration, and renders immutable snapshots and
 * presentation-specific data supplied by controllers. Swing components own only
 * widget and formatting state; they neither access live model objects nor make
 * workflow decisions. View construction and updates belong on Swing's
 * event-dispatch thread.</p>
 */
package io.github.danhjalmberg.dronephotoservice.views;
