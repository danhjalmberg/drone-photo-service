/**
 * Defines presentation-specific data exchanged between controllers and views.
 *
 * <p>Controllers adapt domain snapshots and lifecycle state into small,
 * Swing-independent carriers tailored to individual UI regions. Map carriers use
 * display-pixel coordinates, whereas status-bar coordinates use world meters;
 * keeping these conversions at the presentation boundary prevents Swing and
 * display concerns from entering the simulation domain.</p>
 */
package io.github.danhjalmberg.dronephotoservice.views.viewdata;
