package io.github.danhjalmberg.dronephotoservice.controllers;

/**
 * Describes the controller-managed simulation lifecycle.
 */
public enum SimulationState {
    /**
     * No map is available, so simulation setup cannot begin.
     */
    NO_MAP_LOADED,
    /**
     * A map is loaded and configuration may be changed before starting.
     */
    READY,
    /**
     * Physics, actor work, and periodic view refresh are active.
     */
    RUNNING,
    /**
     * Physics and periodic refresh are stopped and actor work is suspended.
     */
    PAUSED,
    /**
     * Executor termination or failed-startup cleanup is in progress.
     */
    STOPPING,
    /**
     * The completed run remains available for inspection and export.
     */
    STOPPED
}
