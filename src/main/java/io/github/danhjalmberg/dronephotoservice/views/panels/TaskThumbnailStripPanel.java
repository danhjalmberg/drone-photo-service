package io.github.danhjalmberg.dronephotoservice.views.panels;

import io.github.danhjalmberg.dronephotoservice.models.snapshots.TaskThumbnailSnapshot;
import io.github.danhjalmberg.dronephotoservice.settings.ViewSettings;
import io.github.danhjalmberg.dronephotoservice.views.components.RoundedPanel;
import io.github.danhjalmberg.dronephotoservice.views.components.TaskThumbnailCard;
import io.github.danhjalmberg.dronephotoservice.views.support.ViewFactory;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * Displays the latest completed tasks as a fixed-height thumbnail strip.
 *
 * <p>Every update rebuilds the card components, applies selection by task name,
 * and installs hover and click behavior. A {@code null} or empty snapshot list
 * displays the empty-state label.</p>
 */
public class TaskThumbnailStripPanel extends RoundedPanel {

    private final JPanel cardPanel;
    private final JLabel emptyLabel;
    private Consumer<String> taskThumbnailSelectionListener;

    /**
     * Creates a horizontally flexible strip with right-aligned cards.
     */
    public TaskThumbnailStripPanel() {
        super(ViewSettings.PANEL_CORNER_RADIUS);

        Dimension size = new Dimension(
                ViewSettings.THUMBS_PANEL_WIDTH,
                ViewSettings.THUMBS_PANEL_HEIGHT);

        setPreferredSize(size);
        setMinimumSize(new Dimension(0, ViewSettings.THUMBS_PANEL_HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, ViewSettings.THUMBS_PANEL_HEIGHT));

        setBorder(BorderFactory.createEmptyBorder(
                ViewSettings.PANEL_PADDING_TOP,
                ViewSettings.PANEL_PADDING_LEFT,
                ViewSettings.PANEL_PADDING_BOTTOM,
                ViewSettings.PANEL_PADDING_RIGHT));

        setLayout(new BorderLayout(
                ViewSettings.PANEL_GAP,
                ViewSettings.PANEL_GAP));

        JLabel stripLabel = ViewFactory.createSubsectionTitleLabel("COMPLETED TASKS");
        stripLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, ViewSettings.PANEL_GAP));
        add(stripLabel, BorderLayout.WEST);

        cardPanel = new JPanel(new FlowLayout(
                FlowLayout.RIGHT,
                ViewSettings.PANEL_GAP,
                0));
        cardPanel.setOpaque(false);

        emptyLabel = new JLabel("No completed tasks yet");
        cardPanel.add(emptyLabel);

        add(cardPanel, BorderLayout.CENTER);
    }

    /**
     * Sets the callback receiving the name of a clicked completed task.
     *
     * @param listener callback to replace, or {@code null}
     */
    public void addTaskThumbnailSelectionListener(Consumer<String> listener) {
        this.taskThumbnailSelectionListener = listener;
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

        cardPanel.removeAll();

        if (thumbnails == null || thumbnails.isEmpty()) {
            cardPanel.add(emptyLabel);
        } else {
            for (TaskThumbnailSnapshot thumbnail : thumbnails) {
                TaskThumbnailCard card = new TaskThumbnailCard(
                        thumbnail.getName(),
                        thumbnail.getType().getDisplayName(),
                        thumbnail.getThumbnailImage());

                card.setSelected(thumbnail.getName().equals(selectedTaskName));

                card.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent event) {
                        if (taskThumbnailSelectionListener != null) {
                            taskThumbnailSelectionListener.accept(card.getTaskName());
                        }
                    }

                    @Override
                    public void mouseEntered(MouseEvent event) {
                        card.setHovered(true);
                    }

                    @Override
                    public void mouseExited(MouseEvent event) {
                        card.setHovered(false);
                    }
                });

                cardPanel.add(card);
            }
        }

        cardPanel.revalidate();
        cardPanel.repaint();
    }

    /**
     * Clears all cards and restores the empty-state label.
     */
    public void clear() {
        displayTaskThumbnails(List.of(), null);
    }
}
