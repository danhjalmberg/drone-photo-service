package io.github.danhjalmberg.dronephotoservice.views.components;

import io.github.danhjalmberg.dronephotoservice.settings.ModelSettings;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Displays an unscaled camera image centered in the available area.
 *
 * <p>Without an image, its preferred size is the configured camera resolution.
 * Once assigned, the image's natural dimensions determine the preferred size.</p>
 *
 * @author Dan Hjälmberg
 */
public class CameraViewComponent extends JComponent {

    private BufferedImage photo;

    /**
     * Creates an empty camera view.
     */
    public CameraViewComponent() { }

    /**
     * @return configured camera size or current image dimensions
     */
    @Override
    public Dimension getPreferredSize() {
        if (photo == null) {
            return new Dimension(
                    ModelSettings.CAMERA_RESOLUTION_WIDTH,
                    ModelSettings.CAMERA_RESOLUTION_HEIGHT);
        } else {
            return new Dimension(photo.getWidth(), photo.getHeight());
        }
    }

    /**
     * Paints the current image at its natural size, centered without cropping or
     * scaling.
     *
     * @param g Swing graphics context
     */
    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        if (photo != null) {
            int x = (this.getWidth() - photo.getWidth(null)) / 2;
            int y = (this.getHeight() - photo.getHeight(null)) / 2;

            g2d.drawImage(photo, x, y, null);
        }
        g2d.dispose();
    }

    /**
     * Replaces the displayed photo and schedules repainting.
     *
     * @param photo aerial image, or {@code null} to clear the view
     */
    public void setPhoto(BufferedImage photo) {

        this.photo = photo;
        this.repaint();
    }
}
