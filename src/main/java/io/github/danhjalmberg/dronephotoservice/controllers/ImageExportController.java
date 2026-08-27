package io.github.danhjalmberg.dronephotoservice.controllers;

import io.github.danhjalmberg.dronephotoservice.models.Model;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.TaskExportData;
import io.github.danhjalmberg.dronephotoservice.views.components.SaveProgressDialog;
import io.github.danhjalmberg.dronephotoservice.views.View;

import javax.swing.JFileChooser;
import javax.swing.SwingWorker;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinates asynchronous export of archived task images.
 *
 * <p>The controller captures export records while the simulation is stopped,
 * asks the user for a destination, disables conflicting controls, and performs
 * file I/O in a {@link SwingWorker}. Progress and completion handling return to
 * the EDT. Cancellation is cooperative and preserves completed files.</p>
 *
 * @author Dan Hjälmberg
 */
public class ImageExportController {

    private final Model model;
    private final View view;
    private final ControlStateController controlStateController;

    // Session-local chooser convenience; it is not persisted as application state.
    private File lastSaveDirectory;

    // Export worker currently owned by this controller
    // The field is assigned and cleared on Swing's Event Dispatch Thread
    private SwingWorker<File, Integer> activeWorker;

    // Indicates that cancellation was requested because the application is closing
    private boolean applicationShutdownRequested;

    private static final Logger LOGGER = Logger.getLogger(ImageExportController.class.getName());

    /**
     * Creates an image-export workflow controller.
     *
     * @param model the model
     * @param view the view
     * @param controlStateController the control state controller
     */
    public ImageExportController(
            Model model,
            View view,
            ControlStateController controlStateController) {

        this.model = model;
        this.view = view;
        this.controlStateController = controlStateController;
    }

    /**
     * Starts export of all currently archived task results.
     *
     * <p>The request is ignored unless control state permits export. Archive
     * records are captured before the chooser opens; their image lists are
     * structurally immutable, although contained images remain shared. Empty
     * archives produce an information dialog. File I/O runs in the background
     * while a modal progress dialog remains responsive through Swing's nested
     * event loop.</p>
     */
    public void saveImages() {

        if (!controlStateController.canSaveImages()) {
            return;
        }

        List<TaskExportData> tasksToSave = model.getTaskExportData();

        if (tasksToSave.isEmpty()) {
            view.displayInformationMessage(
                    "No images to save",
                    "There are no completed task images to export.");

            return;
        }

        JFileChooser fileChooser = view.getFileChooser();

        configureFileChooser(fileChooser);

        int result = fileChooser.showSaveDialog(view);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File saveRootDirectory = fileChooser.getSelectedFile();

        if (saveRootDirectory == null) {
            return;
        }

        lastSaveDirectory = saveRootDirectory;

        SaveProgressDialog progressDialog = new SaveProgressDialog(view, tasksToSave.size());

        // Both the Cancel Export button and the dialog's window-close button invoke this listener.
        progressDialog.addCancelListener(event -> cancelExport());

        controlStateController.setSaving(true);

        activeWorker = createSaveWorker(
                tasksToSave,
                saveRootDirectory,
                progressDialog);

        activeWorker.execute();
        progressDialog.setVisible(true);
    }

    /**
     * Requests cancellation of the active export.
     *
     * <p>The worker is interrupted and the exporter checks interruption between
     * tasks and animated-GIF frames. Completed files remain, while an incomplete
     * current task file is removed. A call without active export has no effect.</p>
     */
    public void cancelExport() {

        cancelActiveWorker();
    }

    /**
     * Requests silent export cancellation for application shutdown.
     *
     * <p>No completion, cancellation, or failure dialog is shown while the
     * application is closing. Calling this method repeatedly is safe.</p>
     */
    public void shutdown() {

        applicationShutdownRequested = true;
        cancelActiveWorker();
    }

    /**
     * Marks the active worker canceled and requests background interruption.
     */
    private void cancelActiveWorker() {

        SwingWorker<File, Integer> worker = activeWorker;

        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }
    }

    /**
     * Configures the shared file chooser for selecting an export directory.
     *
     * @param fileChooser file chooser to configure
     */
    private void configureFileChooser(JFileChooser fileChooser) {

        fileChooser.setDialogTitle("Choose folder for saved images");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.resetChoosableFileFilters();

        if (lastSaveDirectory != null && lastSaveDirectory.isDirectory()) {

            fileChooser.setCurrentDirectory(lastSaveDirectory);
        }
    }

    /**
     * Creates the background worker responsible for exporting task images.
     *
     * @param tasksToSave structurally immutable export records.
     * @param saveRootDirectory selected export root directory
     * @param progressDialog progress dialog to update
     * @return configured save worker
     */
    private SwingWorker<File, Integer> createSaveWorker(
            List<TaskExportData> tasksToSave,
            File saveRootDirectory,
            SaveProgressDialog progressDialog) {

        return new SwingWorker<>() {

            @Override
            protected File doInBackground() throws IOException {

                return model.saveImagesToDisk(
                        tasksToSave,
                        saveRootDirectory,
                        progress -> {
                            setProgress(progress);
                            publish(progress);
                        });
            }

            @Override
            protected void process(List<Integer> chunks) {

                if (chunks.isEmpty()) {
                    return;
                }

                int latestProgress = chunks.get(chunks.size() - 1);

                progressDialog.setProgress(latestProgress, tasksToSave.size());
            }

            @Override
            protected void done() {

                handleSaveCompletion(this, progressDialog);
            }
        };
    }

    /**
     * Finalizes export state on the EDT after success, failure, or cancellation.
     *
     * <p>The progress dialog is always disposed and the active-worker reference
     * is cleared when it still identifies this worker. Normal completion
     * restores controls; application shutdown remains silent and does not
     * re-enable them.</p>
     *
     * @param worker         completed export worker
     * @param progressDialog progress dialog to close
     */
    private void handleSaveCompletion(
            SwingWorker<File, Integer> worker,
            SaveProgressDialog progressDialog) {

        File savedDirectory = null;
        Throwable failure = null;
        boolean interrupted = false;
        boolean cancelled = false;

        try {
            savedDirectory = worker.get();

        } catch (CancellationException exception) {
            // SwingWorker.get() throws CancellationException after cancel(true)
            // This is an expected lifecycle outcome, not an export failure
            cancelled = true;

        } catch (InterruptedException exception) {
            // This means the EDT was interrupted while retrieving the result
            Thread.currentThread().interrupt();
            failure = exception;
            interrupted = true;

        } catch (ExecutionException exception) {
            failure = exception.getCause() == null
                    ? exception
                    : exception.getCause();

        } finally {
            if (activeWorker == worker) {
                activeWorker = null;
            }

            progressDialog.dispose();

            // During normal operation, restore controls after export
            // During application shutdown, the window is already closing and controls must not be re-enabled
            if (!applicationShutdownRequested) {
                controlStateController.setSaving(false);
            }
        }

        if (cancelled) {
            // Application shutdown cancellation is intentionally silent
            // There is no reason to show a dialog immediately before closing the window
            if (!applicationShutdownRequested) {
                view.displayInformationMessage(
                        "Export cancelled",
                        "Image export was cancelled.\n"
                                + "Files completed before cancellation were kept.");
            }

            return;
        }

        if (failure != null) {
            handleFailedSave(failure, interrupted);
            return;
        }

        // A shutdown request may arrive just as export finishes.
        // Do not display a success dialog while the application is closing.
        if (applicationShutdownRequested) {
            return;
        }

        view.displayInformationMessage(
                "Images saved",
                "Images were saved to:\n"
                        + savedDirectory.getAbsolutePath());
    }

    /**
     * Logs an export failure and displays an appropriate error dialog.
     *
     * @param failure underlying export failure
     * @param interrupted whether the waiting thread was interrupted
     */
    private void handleFailedSave(
            Throwable failure,
            boolean interrupted) {

        LOGGER.log(
                Level.WARNING,
                "Could not export task images.",
                failure);

        String message;

        if (interrupted) {
            message = "Image export was interrupted.";
        } else {
            message = "The task images could not be exported.\n"
                    + "Some files may already have been written.";
        }

        view.displayErrorMessage("Could not save images", message);
    }
}
