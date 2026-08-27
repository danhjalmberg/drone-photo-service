package io.github.danhjalmberg.dronephotoservice.views.components;


import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Non-opaque panel that paints its background as an antialiased rounded
 * rectangle using the panel's current background color.
 */
public class RoundedPanel extends JPanel {

    private final int cornerRadius;

    /**
     * Creates a transparent Swing panel with rounded background painting.
     *
     * @param radius arc width and height used by the rounded rectangle
     */
    public RoundedPanel(int radius) {
        this.cornerRadius = radius;
        setOpaque(false);
    }

    /**
     * Delegates normal panel painting and then fills the rounded background on a
     * copied graphics context.
     *
     * @param g Swing graphics context, copied before customization
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(getBackground());
        g2.fillRoundRect(
                0,
                0,
                getWidth(),
                getHeight(),
                cornerRadius,
                cornerRadius);

        g2.dispose();
    }
}
