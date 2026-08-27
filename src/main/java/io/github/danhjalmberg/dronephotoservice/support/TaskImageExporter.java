package io.github.danhjalmberg.dronephotoservice.support;

import io.github.danhjalmberg.dronephotoservice.models.snapshots.TaskExportData;
import io.github.danhjalmberg.dronephotoservice.models.tasks.TaskType;
import io.github.danhjalmberg.dronephotoservice.settings.ModelSettings;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Exports immutable completed-task image data into one timestamped run directory.
 *
 * <p>Photo tasks write their first image as a GIF; video and zoom tasks write all
 * frames as looping animated GIFs using their configured playback delay. Tasks
 * without images produce no file but are still reported as processed. Export is
 * cooperatively cancellable through worker-thread interruption.</p>
 *
 * <p>Each output is written to a temporary file in the run directory and moved
 * into place only after writing completes. Cancellation or failure removes the
 * temporary file while preserving files from completed tasks and the run
 * directory. Task names are sanitized before use as file-name components.</p>
 *
 * @author Dan Hjälmberg
 */
public final class TaskImageExporter {

    /**
     * Prevents instantiation of this utility class.
     */
    private TaskImageExporter() {
    }

    /**
     * Creates a run directory, exports tasks in list order, and reports the
     * one-based processed count after each task completes.
     *
     * <p>An empty list still creates and returns a run directory. The callback is
     * invoked synchronously on the calling thread and may be {@code null}.</p>
     *
     * @param tasks             completed task export snapshots
     * @param saveRootDirectory selected root export directory
     * @param progressCallback  optional progress callback
     * @return created simulation export directory
     * @throws NullPointerException if the list or one of its elements is null
     * @throws IOException          if the destination cannot be used, a task type is
     *                              unsupported, writing fails, or cancellation is detected
     */
    public static File saveImagesToDisk(
            List<TaskExportData> tasks,
            File saveRootDirectory,
            IntConsumer progressCallback)
            throws IOException {

        Objects.requireNonNull(tasks, "Tasks must not be null.");

        File simulationDirectory = createSimulationSaveDirectory(saveRootDirectory);

        for (int i = 0; i < tasks.size(); i++) {

            checkForCancellation();

            TaskExportData task = Objects.requireNonNull(
                    tasks.get(i),
                    "Task export data must not be null.");

            saveImagesToDisk(task, simulationDirectory);

            if (progressCallback != null) {
                progressCallback.accept(i + 1);
            }
        }

        return simulationDirectory;
    }

    /**
     * Creates a millisecond-timestamped directory in the selected root. A name
     * collision or any other {@code mkdirs()} failure is reported as I/O failure.
     *
     * @param saveRootDirectory root export directory
     * @return created simulation export directory
     * @throws IOException if the directory cannot be created
     */
    private static File createSimulationSaveDirectory(File saveRootDirectory) throws IOException {

        validateSaveRootDirectory(saveRootDirectory);

        String timestamp = DateTimeFormatter
                .ofPattern("yyyy-MM-dd_HH-mm-ss_SSS")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());

        File simulationDirectory = new File(
                saveRootDirectory,
                "simulation_run_" + timestamp);

        if (!simulationDirectory.mkdirs()) {
            throw new IOException(
                    "Could not create export directory: "
                            + simulationDirectory.getAbsolutePath());
        }

        return simulationDirectory;
    }

    /**
     * Validates the selected root export directory.
     *
     * @param saveRootDirectory selected root directory
     * @throws IOException if the directory cannot be used for export
     */
    private static void validateSaveRootDirectory(
            File saveRootDirectory) throws IOException {

        if (saveRootDirectory == null) {
            throw new IOException(
                    "No export directory was selected.");
        }

        if (!saveRootDirectory.exists()) {
            throw new IOException(
                    "The selected export directory does not exist: "
                            + saveRootDirectory.getAbsolutePath());
        }

        if (!saveRootDirectory.isDirectory()) {
            throw new IOException(
                    "The selected export location is not a directory: "
                            + saveRootDirectory.getAbsolutePath());
        }

        if (!saveRootDirectory.canWrite()) {
            throw new IOException(
                    "The selected export directory is not writable: "
                            + saveRootDirectory.getAbsolutePath());
        }
    }

    /**
     * Dispatches a non-empty task to its format-specific exporter. Empty-image
     * tasks have no filesystem effect.
     *
     * @param task          task export data
     * @param saveDirectory destination directory
     * @throws IOException if writing fails
     */
    private static void saveImagesToDisk(
            TaskExportData task,
            File saveDirectory) throws IOException {

        List<BufferedImage> images = task.getImages();

        if (images.isEmpty()) {
            return;
        }

        boolean animated = switch (task.getTaskType()) {
            case PHOTO -> false;
            case VIDEO, ZOOM -> true;
        };

        if (animated) {
            saveAnimatedTask(task, saveDirectory);
        } else {
            savePhotoTask(task, saveDirectory);
        }
    }

    /**
     * Saves the first photo-task image as a GIF. The final path is published only
     * after the blocking ImageIO write completes successfully.
     *
     * @param task          photo task export data
     * @param saveDirectory destination directory
     * @throws IOException if no GIF writer is available or writing fails
     */
    private static void savePhotoTask(
            TaskExportData task,
            File saveDirectory) throws IOException {

        Path imageFile = resolveOutputFile(task, saveDirectory);

        writeCompletedFile(imageFile, temporaryFile -> {
            checkForCancellation();

            boolean written = ImageIO.write(
                    task.getImages().get(0),
                    "gif",
                    temporaryFile.toFile());

            if (!written) {
                throw new IOException("No GIF image writer is available.");
            }
        });
    }

    /**
     * Saves all video- or zoom-task images as an indefinitely looping animated
     * GIF. The final path is published only after the writer completes.
     *
     * @param task          animated task export data
     * @param saveDirectory destination directory
     * @throws IOException if writing fails or export is canceled
     */
    private static void saveAnimatedTask(
            TaskExportData task,
            File saveDirectory) throws IOException {

        Path gifFile = resolveOutputFile(task, saveDirectory);

        int frameDelayMs;

        if (task.getTaskType() == TaskType.VIDEO) {
            frameDelayMs = ModelSettings.VIDEO_TASK_FRAME_DELAY_MS;
        } else {
            frameDelayMs = ModelSettings.ZOOM_TASK_FRAME_DELAY_MS;
        }

        writeCompletedFile(gifFile, temporaryFile -> {
            checkForCancellation();

            AnimatedGifWriter.writeGif(
                    task.getImages(),
                    temporaryFile.toFile(),
                    frameDelayMs,
                    true);
        });
    }

    /**
     * Resolves a sanitized task output name beneath the export directory.
     *
     * @param task          task export data
     * @param saveDirectory destination directory
     * @return normalized output path
     * @throws IOException if the resolved path escapes the export directory
     */
    private static Path resolveOutputFile(
            TaskExportData task,
            File saveDirectory) throws IOException {

        Path exportDirectory = saveDirectory.toPath()
                .toAbsolutePath()
                .normalize();

        String fileName = sanitizeFileNameComponent(task.getName()) + "_"
                + task.getTaskType().getSerializedValue() + ".gif";
        Path outputFile = exportDirectory.resolve(fileName).normalize();

        if (!exportDirectory.equals(outputFile.getParent())) {
            throw new IOException("Invalid task image output path: " + outputFile);
        }

        return outputFile;
    }

    /**
     * Replaces characters unsafe in a file-name component with underscores.
     *
     * @param value source task name
     * @return non-empty safe file-name component
     */
    private static String sanitizeFileNameComponent(String value) {

        String sanitized = Objects.requireNonNull(
                        value,
                        "Task name must not be null.")
                .replaceAll("[^A-Za-z0-9._-]", "_");

        if (sanitized.isEmpty() || sanitized.equals(".") || sanitized.equals("..")) {
            return "task";
        }

        return sanitized;
    }

    /**
     * Writes to a temporary sibling and moves the completed file into place.
     *
     * @param outputFile final output path
     * @param fileWriter operation that populates the temporary file
     * @throws IOException if writing, cancellation checking, or moving fails
     */
    private static void writeCompletedFile(
            Path outputFile,
            TemporaryFileWriter fileWriter) throws IOException {

        Path temporaryFile = Files.createTempFile(
                outputFile.getParent(),
                ".task-image-",
                ".tmp");

        try {
            fileWriter.write(temporaryFile);
            checkForCancellation();
            moveCompletedFile(temporaryFile, outputFile);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    /**
     * Moves a completed temporary file into place, preferring an atomic move.
     *
     * @param temporaryFile completed temporary file
     * @param outputFile    final output path
     * @throws IOException if the file cannot be moved
     */
    private static void moveCompletedFile(
            Path temporaryFile,
            Path outputFile) throws IOException {

        try {
            Files.move(
                    temporaryFile,
                    outputFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(
                    temporaryFile,
                    outputFile,
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Operation that writes one task image output to a temporary file.
     */
    @FunctionalInterface
    private interface TemporaryFileWriter {

        /**
         * Populates the supplied temporary file.
         *
         * @param temporaryFile temporary output path
         * @throws IOException if writing fails
         */
        void write(Path temporaryFile) throws IOException;
    }

    /**
     * Converts worker-thread interruption into an I/O cancellation signal.
     * The interrupted status is inspected but not cleared. This allows callers
     * and higher-level lifecycle code to continue observing the interruption.
     *
     * @throws InterruptedIOException if the current thread has been interrupted
     */
    private static void checkForCancellation()
            throws InterruptedIOException {

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedIOException(
                    "Image export was cancelled.");
        }
    }
}
