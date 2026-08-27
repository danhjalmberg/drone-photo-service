/**
 * Organizes the application's Swing interface into functional screen regions.
 *
 * <p>Composition panels expose facades for the main window, while leaf panels
 * own formatting and widget state for simulation setup, maps, tables, monitors,
 * details, playback, event history, and status. Panels publish Swing events or
 * presentation values and leave workflow decisions to controllers.</p>
 */
package io.github.danhjalmberg.dronephotoservice.views.panels;
