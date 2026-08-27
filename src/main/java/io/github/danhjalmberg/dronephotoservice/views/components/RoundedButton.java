package io.github.danhjalmberg.dronephotoservice.views.components;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Button with an antialiased rounded background and interaction-state colors.
 * Standard Swing painting remains responsible for text, icons, and disabled
 * presentation after the custom background is drawn.
 */
public class RoundedButton extends JButton {

    private final int cornerRadius;

    /**
     * Creates a non-opaque button with custom background painting.
     *
     * @param text the text to display on the button
     * @param cornerRadius arc width and height used by the rounded rectangle
     */
    public RoundedButton(String text, int cornerRadius) {
        super(text);

        this.cornerRadius = cornerRadius;

        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
    }

    /**
     * Paints pressed, rollover, or normal background color before delegating
     * content painting to {@link JButton}.
     *
     * @param graphics Swing graphics context, copied before customization
     */
    @Override
    protected void paintComponent(Graphics graphics) {

        Graphics2D g2 = (Graphics2D) graphics.create();

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        if (getModel().isPressed()) {
            g2.setColor(new Color(90, 90, 90));
        } else if (getModel().isRollover()) {
            g2.setColor(new Color(85, 85, 85));
        } else {
            g2.setColor(new Color(70, 70, 70));
        }

        g2.fillRoundRect(
                0,
                0,
                getWidth(),
                getHeight(),
                cornerRadius,
                cornerRadius);

        g2.dispose();

        super.paintComponent(graphics);
    }
}
