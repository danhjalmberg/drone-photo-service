package io.github.danhjalmberg.dronephotoservice.views.components;

import io.github.danhjalmberg.dronephotoservice.views.support.MapSymbolPainter;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Paints a fixed-size legend for the map's symbol categories.
 * It delegates symbol rendering to {@link MapSymbolPainter}, keeping legend
 * shapes and colors synchronized with the interactive map.
 */
public class MapLegendComponent extends JComponent {

    private static final int WIDTH = 160;
    private static final int HEIGHT = 150;

    private static final int SYMBOL_X = 22;
    private static final int TEXT_X = 46;
    private static final int FIRST_ROW_Y = 24;
    private static final int ROW_HEIGHT = 24;

    /**
     * Creates a transparent legend with fixed dimensions.
     */
    public MapLegendComponent() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setMinimumSize(new Dimension(WIDTH, HEIGHT));
        setMaximumSize(new Dimension(WIDTH, HEIGHT));
        setOpaque(false);
    }

    /**
     * Paints symbols and labels with antialiased graphics.
     *
     * @param g Swing graphics context
     */
    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setFont(getFont().deriveFont(11f));
        g2d.setColor(getForeground());

        int rowY = FIRST_ROW_Y;

        MapSymbolPainter.drawBaseSymbol(g2d, SYMBOL_X, rowY);
        drawLegendText(g2d, "Base position", rowY);

        rowY += ROW_HEIGHT;
        MapSymbolPainter.drawDroneSymbol(g2d, SYMBOL_X, rowY);
        drawLegendText(g2d, "Drone", rowY);

        rowY += ROW_HEIGHT;
        MapSymbolPainter.drawEnqueuedTaskSymbol(g2d, SYMBOL_X, rowY);
        drawLegendText(g2d, "Enqueued task", rowY);

        rowY += ROW_HEIGHT;
        MapSymbolPainter.drawAssignedTaskSymbol(g2d, SYMBOL_X, rowY);
        drawLegendText(g2d, "Assigned task", rowY);

        rowY += ROW_HEIGHT;
        MapSymbolPainter.drawCompletedTaskSymbol(g2d, SYMBOL_X, rowY);
        drawLegendText(g2d, "Completed task", rowY);

        g2d.dispose();
    }

    /**
     * Draws the text label for one legend row.
     *
     * @param g2d   graphics context.
     * @param text  legend row text.
     * @param y     row center y coordinate.
     */
    private void drawLegendText(Graphics2D g2d, String text, int y) {
        g2d.setColor(getForeground());
        g2d.drawString(text, TEXT_X, y + 4);
    }
}
