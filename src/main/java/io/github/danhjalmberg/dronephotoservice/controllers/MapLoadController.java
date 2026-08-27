package io.github.danhjalmberg.dronephotoservice.controllers;

import io.github.danhjalmberg.dronephotoservice.models.Model;
import io.github.danhjalmberg.dronephotoservice.models.map.MapLoadException;
import io.github.danhjalmberg.dronephotoservice.settings.AppSettings;
import io.github.danhjalmberg.dronephotoservice.views.View;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinates bundled and user-selected map loading with lifecycle and view
 * state.
 *
 * <p>Successful loading resets run-specific state and enters {@link
 * SimulationState#READY}. Failed transactional loads preserve the active map
 * and lifecycle state and are reported through logging and an error dialog.</p>
 *
 * @author Dan Hjälmberg
 */
public class MapLoadController {

    private static final Logger LOGGER = Logger.getLogger(MapLoadController.class.getName());

    private final Model model;
    private final View view;
    private final Runnable updateMapView;
    private final SimulationController simulationController;
    private final ControlStateController controlStateController;

    /**
     * Last directory used for loading a map during the current application session.
     */
    private File lastMapDirectory;

    /**
     * Creates a controller for map loading.
     *
     * @param model application model
     * @param view application view
     * @param updateMapView callback for refreshing the displayed map
     * @param simulationController simulation lifecycle controller
     * @param controlStateController controller for enabled/disabled GUI state
     */
    public MapLoadController(
            Model model,
            View view,
            Runnable updateMapView,
            SimulationController simulationController,
            ControlStateController controlStateController) {

        this.model = model;
        this.view = view;
        this.updateMapView = updateMapView;
        this.simulationController = simulationController;
        this.controlStateController = controlStateController;
    }

    /**
     * Loads the bundled demo map and optional classpath sidecar metadata.
     *
     * <p>This initialization operation does not apply the interactive
     * lifecycle-state guard used by {@link #loadMap()}.</p>
     */
    public void loadDemoMap() {

        try (InputStream inputStream =
                     MapLoadController.class.getResourceAsStream(
                             AppSettings.DEMO_MAP_RESOURCE_PATH)) {

            if (inputStream == null) {
                throw new MapLoadException("The bundled demo map resource was not found.");
            }

            model.loadMap(
                    inputStream,
                    AppSettings.DEMO_MAP_FILE_NAME,
                    AppSettings.DEMO_MAP_RESOURCE_PATH);

            handleSuccessfulMapLoad();

        } catch (MapLoadException exception) {
            handleFailedMapLoad(
                    "Could not load demo map",
                    exception);

        } catch (IOException exception) {
            handleFailedMapLoad(
                    "Could not load demo map",
                    new MapLoadException(
                            "The bundled demo map input stream could not be closed.",
                            exception));
        }
    }

    /**
     * Opens the image chooser and loads the selected map when lifecycle permits.
     *
     * <p>Only {@link SimulationState#NO_MAP_LOADED} and {@link
     * SimulationState#READY} allow replacement. Cancellation or an empty file
     * selection has no effect. A successfully selected directory becomes the
     * chooser's session-local starting directory.</p>
     */
    public void loadMap() {

        if (!isMapLoadAllowed()) {
            return;
        }

        JFileChooser fileChooser = view.getFileChooser();

        configureFileChooser(fileChooser);

        int result = fileChooser.showOpenDialog(view);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File mapFile = fileChooser.getSelectedFile();

        if (mapFile == null) {
            return;
        }

        File parentDirectory = mapFile.getParentFile();

        if (parentDirectory != null) {
            lastMapDirectory = parentDirectory;
        }

        try {
            model.loadMap(mapFile);
            handleSuccessfulMapLoad();

        } catch (MapLoadException exception) {
            handleFailedMapLoad("Could not load map", exception);
        }
    }

    /**
     * Reports whether loading or replacing the map is allowed in the current
     * simulation state.
     *
     * @return {@code true} if a map may be loaded.
     */
    private boolean isMapLoadAllowed() {

        SimulationState state = simulationController.getSimulationState();

        return state == SimulationState.NO_MAP_LOADED || state == SimulationState.READY;
    }

    /**
     * Configures the map-image file chooser.
     *
     * @param fileChooser file chooser to configure
     */
    private void configureFileChooser(JFileChooser fileChooser) {

        fileChooser.setDialogTitle("Load map image");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.resetChoosableFileFilters();

        if (lastMapDirectory != null && lastMapDirectory.isDirectory()) {

            fileChooser.setCurrentDirectory(lastMapDirectory);
        }

        String[] extensions = ImageIO.getReaderFileSuffixes();

        FileNameExtensionFilter filter = new FileNameExtensionFilter("Image files", extensions);

        fileChooser.setFileFilter(filter);
    }

    /**
     * Resets run-specific state, redraws the map and metadata, and reapplies
     * controls after a successful load.
     */
    private void handleSuccessfulMapLoad() {

        simulationController.prepareForLoadedMap();

        updateMapView.run();

        view.displayMapMetadata(model.getMapMetadata());

        controlStateController.updateControls();
    }

    /**
     * Logs and displays a failed transactional load, then reapplies controls
     * without changing lifecycle state.
     *
     * @param title error dialog title
     * @param exception map-loading failure
     */
    private void handleFailedMapLoad(
            String title,
            MapLoadException exception) {

        LOGGER.log(
                Level.WARNING,
                title,
                exception);

        view.displayErrorMessage(
                title,
                exception.getMessage());

        controlStateController.updateControls();
    }
}
