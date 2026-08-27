package io.github.danhjalmberg.dronephotoservice.views.panels;

import io.github.danhjalmberg.dronephotoservice.models.snapshots.TaskDetailsSnapshot;
import io.github.danhjalmberg.dronephotoservice.models.tasks.TaskType;
import io.github.danhjalmberg.dronephotoservice.settings.ViewSettings;
import io.github.danhjalmberg.dronephotoservice.support.TimeUtils;
import io.github.danhjalmberg.dronephotoservice.views.components.CameraViewComponent;
import io.github.danhjalmberg.dronephotoservice.views.support.ControlButtonFactory;
import io.github.danhjalmberg.dronephotoservice.views.support.ViewFactory;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

/**
 * Displays archived task details, preview imagery, and playback controls.
 *
 * <p>Video and zoom tasks with more than one captured image are playable. A
 * details update restores the preview image and derives initial control state;
 * playback updates subsequently exchange the enabled Play and Stop buttons.</p>
 */
public class TaskDetailsPanel extends JPanel {

    private final JLabel nameValueLabel;
    private final JLabel typeValueLabel;
    private final JLabel agencyValueLabel;
    private final JLabel createdValueLabel;
    private final JLabel startedValueLabel;
    private final JLabel imageTimeValueLabel;
    private final JLabel endedValueLabel;
    private final JLabel positionValueLabel;
    private final JLabel imageCountValueLabel;

    private final JLabel resultLabel;
    private final JButton playButton;
    private final JButton stopButton;
    private final CameraViewComponent resultImageComponent;

    /**
     * Initializes the task details panel.
     */
    public TaskDetailsPanel() {

        nameValueLabel = ViewFactory.createDetailValueLabel("No task selected.");
        typeValueLabel = ViewFactory.createDetailValueLabel("");
        agencyValueLabel = ViewFactory.createDetailValueLabel("");
        createdValueLabel = ViewFactory.createDetailValueLabel("");
        startedValueLabel = ViewFactory.createDetailValueLabel("");
        imageTimeValueLabel = ViewFactory.createDetailValueLabel("");
        endedValueLabel = ViewFactory.createDetailValueLabel("");
        positionValueLabel = ViewFactory.createDetailValueLabel("");
        imageCountValueLabel = ViewFactory.createDetailValueLabel("");

        resultLabel = new JLabel("No task selected.", SwingConstants.CENTER);
        resultLabel.setFont(ViewSettings.FONT_DEFAULT);
        resultLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        playButton = ControlButtonFactory.createControlButton(
                "Play",
                "Play",
                "play.png");

        stopButton = ControlButtonFactory.createControlButton(
                "Stop",
                "Stop",
                "stop.png");

        playButton.setEnabled(false);
        stopButton.setEnabled(false);

        resultImageComponent = new CameraViewComponent();

        setLayout(new BorderLayout(
                ViewSettings.PANEL_GAP,
                ViewSettings.PANEL_GAP));

        setOpaque(false);

        add(createTaskDataPanel(), BorderLayout.NORTH);
        add(createTaskResultPanel(), BorderLayout.CENTER);
    }

    /**
     * Creates the task data panel.
     *
     * @return the created task data panel
     */
    private JPanel createTaskDataPanel() {

        JPanel taskDataPanel = new JPanel();
        taskDataPanel.setOpaque(false);
        taskDataPanel.setLayout(new BoxLayout(taskDataPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = ViewFactory.createSpacedSectionTitleLabel("Task Data");
        taskDataPanel.add(titleLabel);
        taskDataPanel.add(ViewFactory.createSeparator());

        JPanel rowsPanel = new JPanel(new GridBagLayout());
        rowsPanel.setOpaque(false);
        rowsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        int row = 0;
        ViewFactory.addDetailRow(rowsPanel, row++, "Name:", nameValueLabel);
        ViewFactory.addDetailRow(rowsPanel, row++, "Type:", typeValueLabel);
        ViewFactory.addDetailRow(rowsPanel, row++, "Created by:", agencyValueLabel);
        ViewFactory.addDetailRow(rowsPanel, row++, "Created:", createdValueLabel);
        ViewFactory.addDetailRow(rowsPanel, row++, "Started:", startedValueLabel);
        ViewFactory.addDetailRow(rowsPanel, row++, "Image time:", imageTimeValueLabel);
        ViewFactory.addDetailRow(rowsPanel, row++, "Ended:", endedValueLabel);
        ViewFactory.addDetailRow(rowsPanel, row++, "Position:", positionValueLabel);
        ViewFactory.addDetailRow(rowsPanel, row++, "Images:", imageCountValueLabel);

        taskDataPanel.add(rowsPanel);

        return taskDataPanel;
    }

    /**
     * Creates the task result panel.
     *
     * @return the created task result panel
     */
    private JPanel createTaskResultPanel() {

        JPanel taskResultPanel = new JPanel();
        taskResultPanel.setOpaque(false);
        taskResultPanel.setLayout(new BoxLayout(taskResultPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = ViewFactory.createSpacedSectionTitleLabel("Task Result");
        taskResultPanel.add(titleLabel);
        taskResultPanel.add(ViewFactory.createSeparator());

        taskResultPanel.add(resultLabel);

        JPanel playbackControlsPanel = new JPanel(new FlowLayout(
                FlowLayout.CENTER,
                ViewSettings.PANEL_GAP,
                ViewSettings.PANEL_GAP));

        playbackControlsPanel.setOpaque(false);
        playbackControlsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        playbackControlsPanel.add(playButton);
        playbackControlsPanel.add(stopButton);

        taskResultPanel.add(playbackControlsPanel);

        JPanel wrapper = new JPanel();
        wrapper.setBackground(ViewSettings.CARD_BACKGROUND_COLOR);
        wrapper.add(resultImageComponent);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        taskResultPanel.add(wrapper);

        return taskResultPanel;
    }

    /**
     * Adds a listener to the task result play button.
     *
     * @param listener the listener to add
     */
    public void addTaskResultPlayListener(ActionListener listener) {
        playButton.addActionListener(listener);
    }

    /**
     * Adds a listener to the task result stop button.
     *
     * @param listener the listener to add
     */
    public void addTaskResultStopListener(ActionListener listener) {
        stopButton.addActionListener(listener);
    }

    /**
     * Replaces formatted task fields, preview, and playback eligibility, or
     * clears the panel for {@code null}.
     *
     * @param task the task snapshot to display, or null if no task is selected
     */
    public void displayTaskDetails(TaskDetailsSnapshot task) {

        if (task == null) {
            clearTaskDetails();
            return;
        }

        nameValueLabel.setText(task.getName());
        typeValueLabel.setText(task.getType().getDisplayName());
        agencyValueLabel.setText(task.getPhotoAgencyName());
        createdValueLabel.setText(TimeUtils.formatSimulationTime(task.getCreationSimulationTime()));
        startedValueLabel.setText(TimeUtils.formatSimulationTime(task.getStartSimulationTime()));
        imageTimeValueLabel.setText(TimeUtils.formatSimulationTime(task.getImageSimulationTime()));
        endedValueLabel.setText(TimeUtils.formatSimulationTime(task.getCompletionSimulationTime()));

        positionValueLabel.setText(String.format(
                "%.0f, %.0f",
                task.getPositionMeters().getX(),
                task.getPositionMeters().getY()));

        imageCountValueLabel.setText(String.valueOf(task.getImageCount()));

        displayTaskResult(task.getName(), task.getPreviewImage());

        boolean playable =
                (task.getType() == TaskType.VIDEO || task.getType() == TaskType.ZOOM)
                        && task.getImageCount() > 1;

        setTaskPlaybackControlsEnabled(playable);
    }

    /**
     * Restores the no-selection label, clears the image, and disables playback.
     */
    public void clearTaskDetails() {

        nameValueLabel.setText("No task selected.");
        typeValueLabel.setText("");
        agencyValueLabel.setText("");
        createdValueLabel.setText("");
        startedValueLabel.setText("");
        imageTimeValueLabel.setText("");
        endedValueLabel.setText("");
        positionValueLabel.setText("");
        imageCountValueLabel.setText("");

        displayTaskResult("No task selected.", null);
        setTaskPlaybackControlsEnabled(false);
    }

    /**
     * Displays the task result image.
     *
     * @param taskName the task name to display above the image
     * @param image the task result image, or null to clear the image
     */
    public void displayTaskResult(String taskName, BufferedImage image) {

        resultLabel.setText(taskName);
        resultImageComponent.setPhoto(image);
    }

    /**
     * Sets idle playback eligibility: Play follows {@code enabled} and Stop is
     * disabled.
     *
     * @param enabled whether playback controls should be enabled
     */
    public void setTaskPlaybackControlsEnabled(boolean enabled) {

        playButton.setEnabled(enabled);
        stopButton.setEnabled(false);
    }

    /**
     * Exchanges Play and Stop enabled state for active or stopped playback.
     *
     * @param running whether task result playback is currently running
     */
    public void setTaskPlaybackRunning(boolean running) {

        playButton.setEnabled(!running);
        stopButton.setEnabled(running);
    }
}
