package io.github.danhjalmberg.dronephotoservice.controllers;

/**
 * Defines action-command strings shared by Swing controls and dispatching.
 */
public final class Commands {

    /**
     * Prevents instantiation of this constants class.
     */
    private Commands() {
    }

    /**
     * Opens the user map-loading workflow.
     */
    public static final String LOAD_MAP = "Load Map";
    /**
     * Requests orderly application shutdown.
     */
    public static final String EXIT = "Exit";
    /**
     * Discards a stopped run and prepares another.
     */
    public static final String NEW = "New";
    /**
     * Starts a prepared simulation.
     */
    public static final String START = "Start";
    /**
     * Pauses a running simulation.
     */
    public static final String PAUSE = "Pause";
    /**
     * Resumes a paused simulation.
     */
    public static final String RESUME = "Resume";
    /**
     * Stops a running or paused simulation.
     */
    public static final String STOP = "Stop";
    /**
     * Opens the completed-image export workflow.
     */
    public static final String SAVE_IMAGES = "Save Images";
    /**
     * Displays application help.
     */
    public static final String HELP = "Help";
    /**
     * Displays application information.
     */
    public static final String ABOUT = "About";
}
