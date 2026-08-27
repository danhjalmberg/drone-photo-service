package io.github.danhjalmberg.dronephotoservice.views;

import io.github.danhjalmberg.dronephotoservice.models.events.SimulationEvent;
import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;
import io.github.danhjalmberg.dronephotoservice.models.map.MapMetadata;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.DroneSnapshot;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.TaskDetailsSnapshot;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.TaskThumbnailSnapshot;
import io.github.danhjalmberg.dronephotoservice.settings.AppSettings;
import io.github.danhjalmberg.dronephotoservice.settings.ViewSettings;
import io.github.danhjalmberg.dronephotoservice.views.components.AboutDialog;
import io.github.danhjalmberg.dronephotoservice.views.components.ApplicationMenuBar;
import io.github.danhjalmberg.dronephotoservice.views.components.HelpDialog;
import io.github.danhjalmberg.dronephotoservice.views.panels.CenterPanel;
import io.github.danhjalmberg.dronephotoservice.views.panels.EastPanel;
import io.github.danhjalmberg.dronephotoservice.views.panels.NorthPanel;
import io.github.danhjalmberg.dronephotoservice.views.panels.SouthPanel;
import io.github.danhjalmberg.dronephotoservice.views.panels.WestPanel;
import io.github.danhjalmberg.dronephotoservice.views.viewdata.MapDroneViewData;
import io.github.danhjalmberg.dronephotoservice.views.viewdata.MapSelection;
import io.github.danhjalmberg.dronephotoservice.views.viewdata.MapTaskViewData;
import io.github.danhjalmberg.dronephotoservice.views.viewdata.SimulationHeaderViewData;
import io.github.danhjalmberg.dronephotoservice.views.viewdata.StatusBarViewData;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionListener;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * Main Swing window and controller-facing presentation facade.
 *
 * <p>The frame composes north header, west inspection, center interaction, east
 * detail, and south status regions beneath a shared menu bar. Controllers
 * register user-interaction callbacks and publish snapshots or view data through
 * this class; the view never reads mutable domain objects directly.</p>
 *
 * <p>Construction creates the complete component hierarchy, shared file chooser,
 * and menu bar, sizes and centers the frame, installs a do-nothing close policy,
 * and makes the window visible. Callers are expected to construct and use this
 * Swing facade on the event-dispatch thread.</p>
 *
 * @author Dan Hjälmberg
 */
public class View extends JFrame {

    private ApplicationMenuBar applicationMenuBar;
    private AboutDialog aboutDialog;
    private HelpDialog helpDialog;
    private JFileChooser fileChooser;

    private NorthPanel northPanel;
    private WestPanel westPanel;
    private CenterPanel centerPanel;
    private EastPanel eastPanel;
    private SouthPanel southPanel;

    /**
     * Creates, lays out, and displays the main application window.
     */
    public View() {

        createMenuBar();

        JPanel rootPanel = createRootPanel();
        setContentPane(rootPanel);

        rootPanel.add(createNorthPanel(), BorderLayout.NORTH);
        rootPanel.add(createWestPanel(), BorderLayout.WEST);
        rootPanel.add(createCenterPanel(), BorderLayout.CENTER);
        rootPanel.add(createEastPanel(), BorderLayout.EAST);
        rootPanel.add(createSouthPanel(), BorderLayout.SOUTH);

        setPreferredSize(new Dimension(
                ViewSettings.FRAME_WIDTH_PREFERRED,
                ViewSettings.FRAME_HEIGHT_PREFERRED));
        pack();
        setMinimumSize(new Dimension(
                ViewSettings.FRAME_WIDTH_MIN,
                ViewSettings.FRAME_HEIGHT_MIN));
        setLocationRelativeTo(null);
        setResizable(true);

        setTitle(AppSettings.APPLICATION_NAME);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setVisible(true);
    }

    /**
     * Creates the padded five-region application container.
     *
     * @return configured root panel
     */
    private JPanel createRootPanel() {

        JPanel rootPanel = new JPanel(new BorderLayout(
                ViewSettings.PANEL_GAP,
                ViewSettings.PANEL_GAP));

        rootPanel.setBackground(ViewSettings.ROOT_PANEL_BACKGROUND_COLOR);
        rootPanel.setBorder(BorderFactory.createEmptyBorder(
                ViewSettings.FRAME_PADDING,
                ViewSettings.FRAME_PADDING,
                ViewSettings.FRAME_PADDING,
                ViewSettings.FRAME_PADDING));

        return rootPanel;
    }

    /**
     * @return initialized header region
     */
    private JPanel createNorthPanel() {

        northPanel = new NorthPanel();
        return northPanel;
    }

    /**
     * @return initialized settings and monitoring region
     */
    private JPanel createWestPanel() {

        westPanel = new WestPanel();
        return westPanel;
    }

    /**
     * @return initialized primary interaction region
     */
    private JPanel createCenterPanel() {

        centerPanel = new CenterPanel();
        return centerPanel;
    }

    /**
     * @return initialized details region
     */
    private JPanel createEastPanel() {

        eastPanel = new EastPanel();
        return eastPanel;
    }

    /**
     * @return initialized status region
     */
    private JPanel createSouthPanel() {

        southPanel = new SouthPanel();
        return southPanel;
    }

    /**
     * Installs the menu bar and creates the reusable file chooser.
     */
    private void createMenuBar() {

        applicationMenuBar = new ApplicationMenuBar();
        setJMenuBar(applicationMenuBar);

        fileChooser = new JFileChooser();
    }

    /**
     * Returns the shared chooser whose previous directory and selection state may
     * be retained between workflows.
     *
     * @return reusable application file chooser
     */
    public JFileChooser getFileChooser() {

        return fileChooser;
    }

    /**
     * Lazily creates and displays the reusable modal About dialog.
     */
    public void displayAboutDialog() {

        if (aboutDialog == null) {
            aboutDialog = new AboutDialog(this);
        }

        aboutDialog.setVisible(true);
    }

    /**
     * Lazily creates and displays the modeless Help dialog. Repeated requests
     * preserve its user-adjusted location and bring the existing instance forward.
     */
    public void displayHelpDialog() {

        if (helpDialog == null) {
            helpDialog = new HelpDialog(this);
        }

        helpDialog.setVisible(true);
        helpDialog.toFront();
        helpDialog.requestFocus();
    }

    /**
     * Adds the shared command listener to every menu item and primary command
     * button. Repeated calls add additional listeners.
     *
     * @param commandListener listener receiving shared command strings
     */
    public void addCommandListener(ActionListener commandListener) {

        applicationMenuBar.addCommandListener(commandListener);
        centerPanel.addCommandListener(commandListener);
    }

    /**
     * Adds a listener to the map scale input field and apply button in the settings panel.
     *
     * @param listener the listener to add
     */
    public void addApplyMapScaleListener(ActionListener listener) {
        westPanel.addApplyMapScaleListener(listener);
    }

    /**
     * Adds a listener to the simulation tick slider.
     *
     * @param listener the listener to add
     */
    public void addSimulationTickSliderListener(ChangeListener listener) {

        westPanel.addSimulationTickSliderListener(listener);
    }

    /**
     * Adds a listener to the simulation speed multiplier slider.
     *
     * @param listener the listener to add
     */
    public void addSimulationSpeedSliderListener(ChangeListener listener) {

        westPanel.addSimulationSpeedSliderListener(listener);
    }

    /**
     * Adds a listener to the GUI refresh interval slider.
     *
     * @param listener change listener to be added to the slider.
     */
    public void addGuiRefreshSliderListener(ChangeListener listener) {

        westPanel.addGuiRefreshSliderListener(listener);
    }

    /**
     * Adds a list selection listener to the drone list.
     *
     * @param listener list selection listener to be added to the drone table.
     */
    public void addDroneTableSelectionListener(ListSelectionListener listener) {

        westPanel.addDroneTableSelectionListener(listener);
    }

    /**
     * Sets the callback receiving the typed result of every map click.
     *
     * @param listener callback to replace, or {@code null}
     */
    public void addMapSelectionListener(Consumer<MapSelection> listener) {

        centerPanel.addMapSelectionListener(listener);
    }

    /**
     * Registers an Escape-key action active anywhere in the focused window.
     *
     * @param listener the listener to be called when the Escape key is pressed.
     */
    public void addClearDroneSelectionAction(ActionListener listener) {

        getRootPane().registerKeyboardAction(
                listener,
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * Sets the callback receiving the name of a clicked completed-task thumbnail.
     *
     * @param listener callback to replace, or {@code null}
     */
    public void addTaskThumbnailSelectionListener(Consumer<String> listener) {

        centerPanel.addTaskThumbnailSelectionListener(listener);
    }

    /**
     * Adds a listener to the task-result Play button. Playback eligibility is
     * controlled separately and currently applies to multi-frame video and zoom
     * tasks.
     *
     * @param listener the listener to be called when the play button is pressed.
     */
    public void addTaskResultPlayListener(ActionListener listener) {

        eastPanel.addTaskResultPlayListener(listener);
    }

    /**
     * Adds a listener to the task-result Stop button.
     *
     * @param listener the listener to be called when the stop button is pressed.
     */
    public void addTaskResultStopListener(ActionListener listener) {

        eastPanel.addTaskResultStopListener(listener);
    }

    /**
     * Adds a listener that receives map mouse movement positions in display pixels.
     *
     * @param listener listener receiving the mouse position, or null when mouse exits map
     */
    public void addMapMousePositionListener(Consumer<Vector2D> listener) {

        centerPanel.addMapMousePositionListener(listener);
    }

    /**
     * Registers the operation to invoke when the user requests that the
     * application window be closed. The listener is responsible for completing
     * application shutdown and disposing the window.
     * Multiple registrations produce multiple window listeners.
     *
     * @param closeAction non-null orderly-shutdown request
     * @throws NullPointerException if {@code closeAction} is {@code null}
     */
    public void addApplicationCloseListener(Runnable closeAction) {

        Objects.requireNonNull(
                closeAction,
                "Application close action must not be null.");

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                closeAction.run();
            }
        });
    }

    /**
     * Disposes the reusable Help dialog before disposing the main frame. Swing
     * disposes other owned windows as part of frame disposal.
     */
    @Override
    public void dispose() {

        if (helpDialog != null) {
            helpDialog.dispose();
            helpDialog = null;
        }

        super.dispose();
    }

    /**
     * Parses the manual meters-per-pixel input.
     *
     * @return the meters-per-pixel value
     * @throws NumberFormatException if the field does not contain a valid number
     */
    public double getMapMetersPerPixelInput() {

        return westPanel.getMapMetersPerPixelInput();
    }

    /**
     * Returns the mapped simulation-tick value rather than the slider index.
     *
     * @return the simulation tick interval in milliseconds.
     */
    public int getSimulationTickMs() {

        return westPanel.getSimulationTickMs();
    }

    /**
     * Returns the mapped speed multiplier rather than the slider index.
     *
     * @return the simulation speed multiplier, where 1.0 is real-time speed, 2.0 is double speed, etc.
     */
    public double getSimulationSpeedMultiplier() {

        return westPanel.getSimulationSpeedMultiplier();
    }

    /**
     * Gets the size of the task queue from the slider.
     *
     * @return the size of the task queue
     */
    public int getTaskQueueSize() {

        return westPanel.getTaskQueueSize();
    }

    /**
     * Gets the size of the photo agency thread pool from the slider.
     *
     * @return the size of the photo agency thread pool
     */
    public int getPhotoAgencyPoolSize() {

        return westPanel.getPhotoAgencyPoolSize();
    }

    /**
     * Gets the size of the drone thread pool from the slider.
     *
     * @return number of drone threads.
     */
    public int getDronePoolSize() {

        return westPanel.getDronePoolSize();
    }

    /**
     * Returns the mapped GUI-refresh value rather than the slider index.
     *
     * @return the GUI refresh interval in milliseconds.
     */
    public int getGuiRefreshIntervalMs() {

        return westPanel.getGuiRefreshIntervalMs();
    }

    /**
     * Gets the currently selected drone name from the drone table.
     *
     * @return selected drone name, or {@code null}
     */
    public String getSelectedDroneName() {

        return westPanel.getSelectedDroneName();
    }

    /**
     * Reports whether the drone-details panel has live-camera refresh enabled.
     *
     * @return {@code true} when live-camera refresh is enabled
     */
    public boolean isLiveCameraViewEnabled() {

        return eastPanel.isLiveCameraViewEnabled();
    }

    /**
     * Gets the maximum number of task thumbnails that can be displayed in the
     * thumbnail strip panel.
     *
     * @return the maximum number of task thumbnails that can be displayed
     */
    public int getTaskThumbnailCapacity() {
        return ViewSettings.TASK_THUMBNAIL_STRIP_SIZE;
    }

    /**
     * Applies identical lifecycle-derived command state to both the menu and
     * center control panel.
     *
     * @param newEnabled        whether the "New" button should be enabled
     * @param startEnabled      whether the "Start" button should be enabled
     * @param pauseEnabled      whether the "Pause" button should be enabled
     * @param resumeEnabled     whether the "Resume" button should be enabled
     * @param stopEnabled       whether the "Stop" button should be enabled
     * @param saveImagesEnabled whether the "Save Images" button should be enabled
     */
    public void setSimulationControls(
            boolean newEnabled,
            boolean startEnabled,
            boolean pauseEnabled,
            boolean resumeEnabled,
            boolean stopEnabled,
            boolean saveImagesEnabled) {

        applicationMenuBar.setSimulationControls(
                newEnabled,
                startEnabled,
                pauseEnabled,
                resumeEnabled,
                stopEnabled,
                saveImagesEnabled);

        centerPanel.setSimulationControls(
                newEnabled,
                startEnabled,
                pauseEnabled,
                resumeEnabled,
                stopEnabled,
                saveImagesEnabled);
    }

    /**
     * Enables or disables map-scale editing.
     *
     * @param enabled whether map-scale controls are enabled
     */
    public void setMapScaleControlsEnabled(boolean enabled) {

        westPanel.setMapScaleControlsEnabled(enabled);
    }

    /**
     * Enables or disables controls used to configure a new simulation.
     *
     * @param enabled whether simulation setup controls are enabled
     */
    public void setSimulationSetupControlsEnabled(boolean enabled) {

        westPanel.setSimulationSetupControlsEnabled(enabled);
    }

    /**
     * Enables or disables all controls used to load a map.
     *
     * @param enabled whether map loading is enabled
     */
    public void setMapLoadControlsEnabled(boolean enabled) {

        applicationMenuBar.setMapLoadControlsEnabled(enabled);
        centerPanel.setMapLoadControlsEnabled(enabled);
    }

    /**
     * Temporarily disables conflicting menu and button commands during export.
     * Passing {@code false} does not restore controls; the control-state
     * controller reapplies normal lifecycle state separately.
     *
     * @param saving whether task images are currently being saved
     */
    public void setSavingControls(boolean saving) {

        applicationMenuBar.setSavingControls(saving);
        centerPanel.setSavingControls(saving);
    }

    /**
     * Applies idle playback eligibility: Play follows {@code enabled} and Stop is
     * disabled.
     *
     * @param enabled whether the playback controls should be enabled
     */
    public void setTaskPlaybackControlsEnabled(boolean enabled) {

        eastPanel.setTaskPlaybackControlsEnabled(enabled);
    }

    /**
     * Exchanges Play and Stop enabled state for active or stopped playback.
     *
     * @param running whether the task result playback is currently running
     */
    public void setTaskPlaybackRunning(boolean running) {

        eastPanel.setTaskPlaybackRunning(running);
    }

    /**
     * Displays the compact simulation header.
     *
     * @param data simulation header view data
     */
    public void displaySimulationHeader(SimulationHeaderViewData data) {

        northPanel.displaySimulationHeader(data);
    }

    /**
     * Displays loaded-map metadata, or the no-map state for {@code null}.
     *
     * @param metadata the map metadata to be displayed
     */
    public void displayMapMetadata(MapMetadata metadata) {
        westPanel.displayMapMetadata(metadata);
    }

    /**
     * Replaces the photo-agency overview rows.
     *
     * @param data the data to be displayed in the table
     */
    public void displayPhotoAgencyOverview(Object[][] data) {

        westPanel.displayPhotoAgencyOverview(data);
    }

    /**
     * Replaces drone overview rows while preserving selection by name when
     * possible.
     *
     * @param data the data to be displayed in the table
     */
    public void displayDroneOverview(Object[][] data) {

        westPanel.displayDroneOverview(data);
    }

    /**
     * Replaces queued-task overview rows.
     *
     * @param data the data to be displayed in the table
     */
    public void displayTaskOverview(Object[][] data) {

        westPanel.displayTaskOverview(data);
    }

    /**
     * Updates the photo-agency monitor count and diagnostic text.
     *
     * @param photoAgencyCount          the number of active photo agencies
     * @param photoAgencyDiagnosticText diagnostic text for photo agencies
     */
    public void displayPhotoAgencyMonitor(
            int photoAgencyCount,
            String photoAgencyDiagnosticText) {

        westPanel.displayPhotoAgencyMonitor(
                photoAgencyCount,
                photoAgencyDiagnosticText);
    }

    /**
     * Updates the drone monitor count and diagnostic text.
     *
     * @param droneCount          the number of active drones
     * @param droneDiagnosticText diagnostic text for drones
     */
    public void displayDroneMonitor(
            int droneCount,
            String droneDiagnosticText) {

        westPanel.displayDroneMonitor(droneCount, droneDiagnosticText);
    }

    /**
     * Updates the completed-task monitor count and diagnostic text.
     *
     * @param completedTaskCount       the number of completed tasks
     * @param archivedTaskDiagnosticText diagnostic text for archived tasks
     */
    public void displayCompletedTaskMonitor(
            int completedTaskCount,
            String archivedTaskDiagnosticText) {

        westPanel.displayCompletedTaskMonitor(
                completedTaskCount,
                archivedTaskDiagnosticText);
    }

    /**
     * Appends simulation events to the event log panel.
     *
     * @param events events to append
     */
    public void appendSimulationEvents(List<SimulationEvent> events) {

        westPanel.appendSimulationEvents(events);
    }

    /**
     * Replaces the map image, attribution, and display-coordinate overlays.
     *
     * @param mapImage        map image, or {@code null} for the no-map state
     * @param attributionText map attribution, or {@code null}
     * @param tasks           queued tasks in map-image pixels
     * @param drones          drones and nested task data in map-image pixels
     */
    public void displayMap(BufferedImage mapImage,
                           String attributionText,
                           List<MapTaskViewData> tasks,
                           List<MapDroneViewData> drones) {

        centerPanel.displayMap(mapImage, attributionText, tasks, drones);
    }

    /**
     * Updates the thumbnail strip of completed tasks with new thumbnails
     * and highlights the thumbnail of the selected task.
     *
     * @param thumbnails       the list of task thumbnail snapshots
     * @param selectedTaskName the name of the selected task
     */
    public void displayTaskThumbnails(

            List<TaskThumbnailSnapshot> thumbnails,
            String selectedTaskName) {

        centerPanel.displayTaskThumbnails(thumbnails, selectedTaskName);
    }

    /**
     * Updates drone details and selects the drone tab for non-null data.
     *
     * @param drone snapshot to display, or {@code null} to clear details
     */
    public void displayDroneDetails(DroneSnapshot drone) {

        eastPanel.displayDroneDetails(drone);
    }

    /**
     * Updates the drone live image in the drone details panel.
     *
     * @param image live camera image, or {@code null} to clear it
     */
    public void displayDroneLiveImage(BufferedImage image) {

        eastPanel.displayDroneLiveImage(image);
    }

    /**
     * Updates task details and selects the task tab for non-null data.
     *
     * @param task snapshot to display, or {@code null} to clear details
     */
    public void displayTaskDetails(TaskDetailsSnapshot task) {

        eastPanel.displayTaskDetails(task);
    }

    /**
     * Replaces the task-result label and image, including individual playback
     * frames.
     *
     * @param taskName task name displayed above the image
     * @param photo    result image, or {@code null} to clear it
     */
    public void displayTaskResult(String taskName, BufferedImage photo) {

        eastPanel.displayTaskResult(taskName, photo);
    }

    /**
     * Displays the compact status bar containing contextual GUI information
     * and the latest simulation activity.
     *
     * @param data status bar view data
     */
    public void displayStatusBar(StatusBarViewData data) {

        southPanel.displayStatusBar(data);
    }

    /**
     * Clears simulation-derived presentation while preserving the loaded map,
     * map metadata, user settings, header, and status bar.
     *
     * <p>This removes overview and monitor data, displayed events, map symbols,
     * selection highlights, thumbnails, details, and result imagery.</p>
     */
    public void clearSimulationDisplay() {

        westPanel.clearSimulationData();
        centerPanel.clearMapSymbols();
        selectDroneByName(null);
        selectTaskByName(null);
        centerPanel.clearTaskThumbnails();

        eastPanel.clear();
    }

    /**
     * Displays an informational message dialog.
     *
     * @param title   the dialog title
     * @param message the message to display
     */
    public void displayInformationMessage(
            String title,
            String message) {

        JOptionPane.showMessageDialog(
                this,
                message,
                title,
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Displays an error message dialog with the given title and message.
     *
     * @param title   the title of the error message dialog
     * @param message the message to display in the error message dialog
     */
    public void displayErrorMessage(
            String title,
            String message) {

        JOptionPane.showMessageDialog(
                this,
                message,
                title,
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Synchronizes the map highlight and overview-table selection by drone name.
     * A null or unknown name clears the table selection and produces no visible
     * map highlight.
     *
     * @param droneName the name of the drone to select
     */
    public void selectDroneByName(String droneName) {

        centerPanel.selectDroneByName(droneName);
        westPanel.selectDroneByName(droneName);
    }

    /**
     * Sets the completed-task map highlight by name. A null or unknown name
     * produces no visible highlight.
     *
     * @param taskName the name of the task to select
     */
    public void selectTaskByName(String taskName) {

        centerPanel.selectTaskByName(taskName);
    }
}
