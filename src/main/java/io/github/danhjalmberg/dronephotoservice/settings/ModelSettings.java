package io.github.danhjalmberg.dronephotoservice.settings;

import io.github.danhjalmberg.dronephotoservice.support.ImageInterpolation;

/**
 * Defines defaults, limits, and derived constants for the simulation model.
 *
 * <p>The settings cover simulation timing, maps, task production and storage,
 * drones and their components, and image capture. Width and height are stored
 * separately rather than in mutable {@link java.awt.Dimension} instances.</p>
 */
public final class ModelSettings {

    /**
     * Prevents instantiation of this constants class.
     */
    private ModelSettings() {
    }

    // ########################################################################
    // Simulation
    // ########################################################################

    /**
     * Simulation-state update interval, in milliseconds.
     */
    public static final int SIMULATION_TICK_MS = 50;

    /**
     * Multiplier applied to the amount of simulated time advanced per tick.
     */
    public static final double SIMULATION_SPEED_MULTIPLIER = 1.0;

    /**
     * Pause between work attempts by an idle actor, in milliseconds.
     */
    public static final int ACTOR_IDLE_PAUSE_MS = 1000;

    // ########################################################################
    // Map
    // ########################################################################

    /**
     * Width to which a loaded map image is resampled, in pixels.
     */
    public static final int MAP_IMAGE_RESAMPLED_WIDTH = 680;
    /**
     * Height to which a loaded map image is resampled, in pixels.
     */
    public static final int MAP_IMAGE_RESAMPLED_HEIGHT = 680;

    /**
     * Interpolation used when resampling loaded map images.
     */
    public static final ImageInterpolation MAP_INTERPOLATION = ImageInterpolation.BICUBIC;

    // ########################################################################
    // Map metadata
    // ########################################################################

    /**
     * Default real-world map scale, in meters represented by one pixel.
     */
    public static final double DEFAULT_MAP_METERS_PER_PIXEL = 0.25;

    // ########################################################################
    // Photo Agencies
    // ########################################################################

    /**
     * Initial number of photo agencies.
     */
    public static final int PHOTO_AGENCY_POOL_SIZE_DEFAULT = 5;
    /**
     * Smallest selectable number of photo agencies.
     */
    public static final int PHOTO_AGENCY_POOL_SIZE_MIN = 1;
    /**
     * Largest selectable number of photo agencies.
     */
    public static final int PHOTO_AGENCY_POOL_SIZE_MAX = 10;
    /**
     * Major tick spacing for the photo-agency pool-size control.
     */
    public static final int PHOTO_AGENCY_POOL_SIZE_MAJOR_TICK = 1;

    /**
     * Minimum random interval between task creations, in seconds.
     */
    public static final double TASK_CREATION_INTERVAL_MIN_SECONDS = 5.0;
    /**
     * Maximum random interval between task creations, in seconds.
     */
    public static final double TASK_CREATION_INTERVAL_MAX_SECONDS = 20.0;

    // ########################################################################
    // Task Queue
    // ########################################################################

    /**
     * Initial maximum number of queued tasks.
     */
    public static final int TASK_QUEUE_SIZE_DEFAULT = 5;
    /**
     * Smallest selectable task-queue capacity.
     */
    public static final int TASK_QUEUE_SIZE_MIN = 1;
    /**
     * Largest selectable task-queue capacity.
     */
    public static final int TASK_QUEUE_SIZE_MAX = 20;
    /**
     * Major tick spacing for the task-queue capacity control.
     */
    public static final int TASK_QUEUE_SIZE_MAJOR_TICK = 1;

    // ########################################################################
    // Drones
    // ########################################################################

    /**
     * Initial number of drones.
     */
    public static final int DRONE_POOL_SIZE_DEFAULT = 5;
    /**
     * Smallest selectable number of drones.
     */
    public static final int DRONE_POOL_SIZE_MIN = 1;
    /**
     * Largest selectable number of drones.
     */
    public static final int DRONE_POOL_SIZE_MAX = 10;
    /**
     * Major tick spacing for the drone pool-size control.
     */
    public static final int DRONE_POOL_SIZE_MAJOR_TICK = 1;

    // ########################################################################
    // Battery
    // ########################################################################

    /**
     * Operating duration of a low-capacity battery, in seconds.
     */
    public static final int BATTERY_LOW_CAPACITY_OPERATION_SECONDS = 360;
    /**
     * Operating duration of a medium-capacity battery, in seconds.
     */
    public static final int BATTERY_MEDIUM_CAPACITY_OPERATION_SECONDS = 720;
    /**
     * Operating duration of a high-capacity battery, in seconds.
     */
    public static final int BATTERY_HIGH_CAPACITY_OPERATION_SECONDS = 1080;
    /**
     * Battery time reserved when deciding whether a task is safe, in seconds.
     */
    public static final int BATTERY_DURATION_SAFETY_MARGIN_SECONDS = 30;

    // ########################################################################
    // Motor
    // ########################################################################

    /**
     * Travel speed of a low-speed motor, in meters per second.
     */
    public static final double DRONE_LOW_SPEED_METERS_PER_SECOND = 4.0;
    /**
     * Travel speed of a medium-speed motor, in meters per second.
     */
    public static final double DRONE_MEDIUM_SPEED_METERS_PER_SECOND = 8.0;
    /**
     * Travel speed of a high-speed motor, in meters per second.
     */
    public static final double DRONE_HIGH_SPEED_METERS_PER_SECOND = 12.0;

    /**
     * Drone acceleration, in meters per second squared.
     */
    public static final double DRONE_ACCELERATION_METERS_PER_SECOND_SQUARED = 2.0;
    /**
     * Drone deceleration magnitude, in meters per second squared.
     */
    public static final double DRONE_DECELERATION_METERS_PER_SECOND_SQUARED = 4.0;

    // ########################################################################
    // Camera
    // ########################################################################

    /**
     * Captured image width, in pixels.
     */
    public static final int CAMERA_RESOLUTION_WIDTH = 400;
    /**
     * Captured image height, in pixels.
     */
    public static final int CAMERA_RESOLUTION_HEIGHT = 400;
    /**
     * Minimum horizontal distance from the camera center to a map edge, in pixels.
     */
    public static final int CAMERA_SAFE_WORLD_PIXEL_MARGIN_X = CAMERA_RESOLUTION_WIDTH / 2;
    /**
     * Minimum vertical distance from the camera center to a map edge, in pixels.
     */
    public static final int CAMERA_SAFE_WORLD_PIXEL_MARGIN_Y = CAMERA_RESOLUTION_HEIGHT / 2;

    /**
     * Interpolation used when sampling images from the map.
     */
    public static final ImageInterpolation CAMERA_INTERPOLATION = ImageInterpolation.BICUBIC;

    // ########################################################################
    // Photo Tasks
    // ########################################################################

    /**
     * Delay spent positioned at a photo target before capture, in milliseconds.
     */
    public static final int PHOTO_TASK_TARGET_DELAY_MS = 2000;
    /**
     * Maximum number of images retained by a photo task.
     */
    public static final int PHOTO_TASK_MAX_IMAGES = 1;

    /**
     * Delay between playback frames, in milliseconds.
     */
    public static final int PHOTO_TASK_PLAYBACK_FRAME_DELAY_MS = 2000;

    // ########################################################################
    // Zoom Tasks
    // ########################################################################

    /**
     * Delay spent positioned at a zoom target before capture, in milliseconds.
     */
    public static final int ZOOM_TASK_TARGET_DELAY_MS = 2000;
    /**
     * Zoom animation capture rate, in frames per second.
     */
    public static final int ZOOM_TASK_FPS = 20;
    /**
     * Zoom animation duration, in seconds.
     */
    public static final int ZOOM_TASK_DURATION_SECONDS = 5;
    /**
     * Number of frames derived from the zoom rate and duration.
     */
    public static final int ZOOM_TASK_FRAME_COUNT = ZOOM_TASK_FPS * ZOOM_TASK_DURATION_SECONDS;
    /**
     * Delay between zoom frames, in milliseconds.
     */
    public static final int ZOOM_TASK_FRAME_DELAY_MS = 1000 / ZOOM_TASK_FPS;
    /**
     * Initial zoom scale, where {@code 1.0} represents the full camera area.
     */
    public static final double ZOOM_TASK_START_SCALE = 1.0;
    /**
     * Final zoom scale relative to the full camera area.
     */
    public static final double ZOOM_TASK_END_SCALE = 0.40;
    /**
     * Maximum number of images retained by a zoom task.
     */
    public static final int ZOOM_TASK_MAX_FRAMES = ZOOM_TASK_FRAME_COUNT;

    // ########################################################################
    // Video Tasks
    // ########################################################################

    /**
     * Delay spent positioned at a video target before capture, in milliseconds.
     */
    public static final int VIDEO_TASK_TARGET_DELAY_MS = 1000;
    /**
     * Video capture rate, in frames per second.
     */
    public static final int VIDEO_TASK_FPS = 20;
    /**
     * Delay between video frames, in milliseconds.
     */
    public static final int VIDEO_TASK_FRAME_DELAY_MS = 1000 / VIDEO_TASK_FPS;
    /**
     * Maximum number of images retained by a video task.
     */
    public static final int VIDEO_TASK_MAX_FRAMES = 200;

    // ########################################################################
    // Archive
    // ########################################################################

    /**
     * Maximum number of completed tasks retained in the archive.
     */
    public static final int TASK_ARCHIVE_MAX_SIZE = 50;
    /**
     * Maximum number of completed-task map markers retained for display.
     */
    public static final int COMPLETED_TASK_MARKER_HISTORY_MAX_SIZE = 200;
}
