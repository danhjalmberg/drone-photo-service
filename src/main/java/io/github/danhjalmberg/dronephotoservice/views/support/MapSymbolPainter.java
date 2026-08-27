package io.github.danhjalmberg.dronephotoservice.views.support;

import io.github.danhjalmberg.dronephotoservice.settings.ViewSettings;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;

/**
 * Draws the shared symbol vocabulary used by the map and its legend.
 *
 * <p>Coordinates and radii are expressed in the supplied graphics context's user
 * space, normally component pixels. Symbol colors and standard radii come from
 * {@link ViewSettings}. Methods leave the graphics color changed; the video-trail
 * method temporarily changes and then restores the composite.</p>
 */
public final class MapSymbolPainter {

    private static final double BASE_OUTER_RADIUS = 12.0;
    private static final double BASE_RING_WIDTH = 6.0;

    /**
     * Prevents instantiation of this utility class.
     */
    private MapSymbolPainter() {
    }

    /**
     * Draws the drone base position symbol.
     *
     * @param g2d graphics context to modify
     * @param centerX symbol center x-coordinate in user space
     * @param centerY symbol center y-coordinate in user space
     */
    public static void drawBaseSymbol(Graphics2D g2d, double centerX, double centerY) {

        Shape donut = donut(centerX, centerY, BASE_OUTER_RADIUS, BASE_RING_WIDTH);

        g2d.setColor(ViewSettings.DRONE_BASE_POSITION_SYMBOL_COLOR);
        g2d.fill(donut);

        g2d.setColor(Color.BLACK);
        g2d.draw(donut);
    }

    /**
     * Draws the drone symbol.
     *
     * @param g2d graphics context to modify
     * @param centerX symbol center x-coordinate in user space
     * @param centerY symbol center y-coordinate in user space
     */
    public static void drawDroneSymbol(Graphics2D g2d, double centerX, double centerY) {

        g2d.setColor(ViewSettings.DRONE_SYMBOL_COLOR);
        g2d.fill(circle(centerX, centerY, ViewSettings.POINT_RADIUS));

    }

    /**
     * Draws the enqueued task symbol.
     *
     * @param g2d graphics context to modify
     * @param centerX symbol center x-coordinate in user space
     * @param centerY symbol center y-coordinate in user space
     */
    public static void drawEnqueuedTaskSymbol(Graphics2D g2d, double centerX, double centerY) {

        g2d.setColor(ViewSettings.ENQUEUED_TASK_SYMBOL_COLOR);
        g2d.fill(circle(centerX, centerY, ViewSettings.POINT_RADIUS));
    }

    /**
     * Draws the assigned task symbol.
     *
     * @param g2d graphics context to modify
     * @param centerX symbol center x-coordinate in user space
     * @param centerY symbol center y-coordinate in user space
     */
    public static void drawAssignedTaskSymbol(Graphics2D g2d, double centerX, double centerY) {

        g2d.setColor(ViewSettings.ASSIGNED_TASK_SYMBOL_COLOR);
        g2d.fill(circle(centerX, centerY, ViewSettings.POINT_RADIUS));
    }

    /**
     * Draws the completed task symbol.
     *
     * @param g2d graphics context to modify
     * @param centerX symbol center x-coordinate in user space
     * @param centerY symbol center y-coordinate in user space
     */
    public static void drawCompletedTaskSymbol(Graphics2D g2d, double centerX, double centerY) {

        g2d.setColor(ViewSettings.COMPLETED_TASK_SYMBOL_COLOR);
        g2d.fill(circle(centerX, centerY, ViewSettings.POINT_RADIUS / 2.0));
    }

    /**
     * Draws one video trail point.
     *
     * @param g2d graphics context to modify
     * @param centerX point center x-coordinate in user space
     * @param centerY point center y-coordinate in user space
     */
    public static void drawVideoTrailPoint(
            Graphics2D g2d,
            double centerX,
            double centerY) {

        Composite oldComposite = g2d.getComposite();

        g2d.setComposite(
                AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER,
                        ViewSettings.VIDEO_TRAIL_ALPHA));

        g2d.setColor(ViewSettings.VIDEO_TRAIL_SYMBOL_COLOR);
        g2d.fill(circle(centerX, centerY, ViewSettings.VIDEO_TRAIL_POINT_RADIUS));

        g2d.setComposite(oldComposite);
    }

    /**
     * Draws a filled and outlined circular highlight behind a symbol.
     *
     * @param g2d graphics context to modify
     * @param centerX highlight center x-coordinate in user space
     * @param centerY highlight center y-coordinate in user space
     * @param radius highlight radius in user-space units
     * @param fillColor highlight fill color
     * @param borderColor highlight outline color
     */
    public static void drawHighlight(
            Graphics2D g2d,
            double centerX,
            double centerY,
            double radius,
            Color fillColor,
            Color borderColor) {

        Shape highlight = circle(centerX, centerY, radius);

        g2d.setColor(fillColor);
        g2d.fill(highlight);

        g2d.setColor(borderColor);
        g2d.draw(highlight);
    }

    /**
     * Creates a circle centered on the supplied user-space coordinate.
     *
     * @param centerX center x-coordinate
     * @param centerY center y-coordinate
     * @param radius circle radius
     * @return circle shape
     */
    private static Shape circle(double centerX, double centerY, double radius) {
        return new Ellipse2D.Double(
                centerX - radius,
                centerY - radius,
                radius * 2.0,
                radius * 2.0);
    }

    /**
     * Creates a ring by subtracting a concentric inner circle from an outer one.
     *
     * @param centerX center x-coordinate
     * @param centerY center y-coordinate
     * @param outerRadius outer radius
     * @param width ring width
     * @return ring-shaped area
     */
    private static Shape donut(
            double centerX,
            double centerY,
            double outerRadius,
            double width) {

        Ellipse2D outer = new Ellipse2D.Double(
                centerX - outerRadius,
                centerY - outerRadius,
                outerRadius * 2,
                outerRadius * 2);

        Ellipse2D inner = new Ellipse2D.Double(
                centerX - outerRadius + width,
                centerY - outerRadius + width,
                outerRadius * 2 - width * 2,
                outerRadius * 2 - width * 2);

        Area area = new Area(outer);
        area.subtract(new Area(inner));

        return area;
    }
}
