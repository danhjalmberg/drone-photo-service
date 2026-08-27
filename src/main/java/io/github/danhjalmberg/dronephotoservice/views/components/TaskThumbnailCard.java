package io.github.danhjalmberg.dronephotoservice.views.components;

import io.github.danhjalmberg.dronephotoservice.settings.ViewSettings;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

/**
 * Displays a selectable completed-task thumbnail, name, and type.
 *
 * <p>The source image is smoothly scaled to the configured square thumbnail
 * dimensions; a missing image produces a solid placeholder. Selection styling
 * takes precedence over hover styling. Mouse listeners are installed by the
 * containing thumbnail strip rather than by this component.</p>
 *
 * @author Dan Hjälmberg
 */
public class TaskThumbnailCard extends JPanel {

    private final String taskName;
    private final JLabel imageLabel;
    private final JLabel nameLabel;
    private final JLabel typeLabel;

    private boolean selected;
    private boolean hovered;

    /**
     * Creates a fixed-size completed-task card.
     *
     * @param taskName task identity returned by {@link #getTaskName()}
     * @param taskType type text displayed below the name
     * @param image    source image, or {@code null} for a placeholder
     */
    public TaskThumbnailCard(
            String taskName,
            String taskType,
            BufferedImage image) {

        this.taskName = taskName;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Dimension taskThumbnailCardSize = new Dimension(
                ViewSettings.TASK_THUMBNAIL_CARD_WIDTH,
                ViewSettings.TASK_THUMBNAIL_CARD_HEIGHT);
        setPreferredSize(taskThumbnailCardSize);
        setMinimumSize(taskThumbnailCardSize);
        setMaximumSize(taskThumbnailCardSize);

        setOpaque(true);
        setBackground(ViewSettings.CARD_BACKGROUND_COLOR);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        imageLabel = new JLabel();
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(
                ViewSettings.TASK_THUMBNAIL_IMAGE_SIZE,
                ViewSettings.TASK_THUMBNAIL_IMAGE_SIZE));
        imageLabel.setIcon(createThumbnailIcon(image));
        imageLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        nameLabel = new JLabel(taskName, SwingConstants.CENTER);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 11f));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        typeLabel = new JLabel(taskType, SwingConstants.CENTER);
        typeLabel.setFont(typeLabel.getFont().deriveFont(9f));
        typeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        typeLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        add(imageLabel);
        add(nameLabel);
        add(typeLabel);

        setSelected(false);
    }

    /**
     * Returns the name of the task represented by this card.
     *
     * @return task name represented by this card
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     * Changes selection styling.
     *
     * @param selected whether the card is selected
     */
    public void setSelected(boolean selected) {

        this.selected = selected;
        updateBorder();
    }

    /**
     * Changes hover styling without overriding selection styling.
     *
     * @param hovered whether the pointer is over the card
     */
    public void setHovered(boolean hovered) {
        this.hovered = hovered;
        updateBorder();
    }

    /**
     * Applies the selected, hovered, or neutral border and schedules repainting.
     */
    private void updateBorder() {

        if (selected) {
            setBorder(BorderFactory.createLineBorder(
                    ViewSettings.SELECTED_TASK_HIGHLIGHT_BORDER_COLOR,
                    2));

        } else if (hovered) {
            setBorder(BorderFactory.createLineBorder(
                    ViewSettings.HOVERED_TASK_HIGHLIGHT_BORDER_COLOR,
                    2));

        } else {
            setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        }

        repaint();
    }

    /**
     * Creates the square image or placeholder displayed by this card.
     *
     * @param sourceImage source image, or {@code null}
     * @return fixed-size thumbnail icon
     */
    private ImageIcon createThumbnailIcon(BufferedImage sourceImage) {

        BufferedImage thumbnail = new BufferedImage(
                ViewSettings.TASK_THUMBNAIL_IMAGE_SIZE,
                ViewSettings.TASK_THUMBNAIL_IMAGE_SIZE,
                BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = thumbnail.createGraphics();

        g2d.setColor(ViewSettings.TEXTAREA_BACKGROUND_COLOR);
        g2d.fillRect(
                0,
                0,
                ViewSettings.TASK_THUMBNAIL_IMAGE_SIZE,
                ViewSettings.TASK_THUMBNAIL_IMAGE_SIZE);

        if (sourceImage != null) {
            Image scaledImage = sourceImage.getScaledInstance(
                    ViewSettings.TASK_THUMBNAIL_IMAGE_SIZE,
                    ViewSettings.TASK_THUMBNAIL_IMAGE_SIZE,
                    Image.SCALE_SMOOTH);

            g2d.drawImage(scaledImage, 0, 0, null);
        }

        g2d.dispose();

        return new ImageIcon(thumbnail);
    }
}
