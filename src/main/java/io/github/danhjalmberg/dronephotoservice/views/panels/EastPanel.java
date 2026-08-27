package io.github.danhjalmberg.dronephotoservice.views.panels;

import io.github.danhjalmberg.dronephotoservice.models.snapshots.DroneSnapshot;
import io.github.danhjalmberg.dronephotoservice.models.snapshots.TaskDetailsSnapshot;
import io.github.danhjalmberg.dronephotoservice.settings.ViewSettings;
import io.github.danhjalmberg.dronephotoservice.views.components.RoundedPanel;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

/**
 * Composes the drone and task inspection region of the main window.
 *
 * <p>Non-null detail updates automatically select their corresponding tab;
 * clearing data leaves the currently visible tab unchanged. Task playback events
 * and live-camera state are forwarded to the owning detail panel.</p>
 *
 * @author Dan Hjälmberg
 */
public class EastPanel extends JPanel {

    private final JTabbedPane detailsTabbedPane;
    private final DroneDetailsPanel droneDetailsPanel;
    private final TaskDetailsPanel taskDetailsPanel;

    /**
     * Initializes the east panel and its child panels.
     */
    public EastPanel() {

        Dimension eastPanelSize = new Dimension(
                ViewSettings.EAST_PANEL_WIDTH,
                ViewSettings.EAST_PANEL_HEIGHT);
        setPreferredSize(eastPanelSize);
        setMinimumSize(eastPanelSize);
        setMaximumSize(eastPanelSize);

        setOpaque(false);
        setBorder(null);

        setLayout(new GridLayout(
                1,
                1,
                ViewSettings.PANEL_GAP,
                ViewSettings.PANEL_GAP));

        droneDetailsPanel = new DroneDetailsPanel();
        taskDetailsPanel = new TaskDetailsPanel();
        detailsTabbedPane = new JTabbedPane();

        add(createDetailsPanel());
    }

    /**
     * Creates the rounded details panel containing drone and task tabs.
     *
     * @return the created details panel
     */
    private JPanel createDetailsPanel() {

        JPanel detailsPanel = new RoundedPanel(ViewSettings.PANEL_CORNER_RADIUS);

        detailsPanel.setBorder(BorderFactory.createEmptyBorder(
                ViewSettings.PANEL_PADDING_TOP,
                ViewSettings.PANEL_PADDING_LEFT,
                ViewSettings.PANEL_PADDING_BOTTOM,
                ViewSettings.PANEL_PADDING_RIGHT));

        detailsPanel.setLayout(new BorderLayout());

        detailsTabbedPane.addTab("DRONE", droneDetailsPanel);
        detailsTabbedPane.addTab("TASK", taskDetailsPanel);

        detailsPanel.add(detailsTabbedPane, BorderLayout.CENTER);

        return detailsPanel;
    }

    /**
     * Sets the drone details tab as the selected tab.
     */
    public void showDroneDetailsTab() {

        detailsTabbedPane.setSelectedIndex(0);
    }

    /**
     * Sets the task details tab as the selected tab.
     */
    public void showTaskDetailsTab() {

        detailsTabbedPane.setSelectedIndex(1);
    }

    /**
     * Displays drone details and selects the drone tab for non-null data.
     *
     * @param drone the drone snapshot to display, or null if no drone is selected
     */
    public void displayDroneDetails(DroneSnapshot drone) {

        if (drone != null) {
            showDroneDetailsTab();
        }

        droneDetailsPanel.displayDroneDetails(drone);
    }

    /**
     * Displays the live camera image for the selected drone.
     *
     * @param image the live camera image, or null to clear the image
     */
    public void displayDroneLiveImage(BufferedImage image) {

        droneDetailsPanel.displayDroneLiveImage(image);
    }

    /**
     * Gets whether the live camera view is enabled.
     *
     * @return true if live camera view is enabled, otherwise false
     */
    public boolean isLiveCameraViewEnabled() {

        return droneDetailsPanel.isLiveCameraViewEnabled();
    }

    /**
     * Displays task details and selects the task tab for non-null data.
     *
     * @param task the task snapshot to display, or null if no task is selected
     */
    public void displayTaskDetails(TaskDetailsSnapshot task) {

        if (task != null) {
            showTaskDetailsTab();
        }

        taskDetailsPanel.displayTaskDetails(task);
    }

    /**
     * Displays the task result image.
     *
     * @param taskName the task name to display above the image
     * @param image the task result image, or null to clear the image
     */
    public void displayTaskResult(String taskName, BufferedImage image) {

        taskDetailsPanel.displayTaskResult(taskName, image);
    }

    /**
     * Adds a listener to the task result play button.
     *
     * @param listener the listener to add
     */
    public void addTaskResultPlayListener(ActionListener listener) {

        taskDetailsPanel.addTaskResultPlayListener(listener);
    }

    /**
     * Adds a listener to the task result stop button.
     *
     * @param listener the listener to add
     */
    public void addTaskResultStopListener(ActionListener listener) {

        taskDetailsPanel.addTaskResultStopListener(listener);
    }

    /**
     * Sets whether task playback can be started.
     *
     * @param enabled whether playback controls should be enabled
     */
    public void setTaskPlaybackControlsEnabled(boolean enabled) {

        taskDetailsPanel.setTaskPlaybackControlsEnabled(enabled);
    }

    /**
     * Sets the playback control state while playback is running or stopped.
     *
     * @param running whether task result playback is currently running
     */
    public void setTaskPlaybackRunning(boolean running) {

        taskDetailsPanel.setTaskPlaybackRunning(running);
    }

    /**
     * Clears both detail displays without changing the selected tab.
     */
    public void clear() {

        displayDroneDetails(null);
        displayTaskDetails(null);
    }
}
