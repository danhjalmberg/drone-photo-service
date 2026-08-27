package io.github.danhjalmberg.dronephotoservice.settings;

import java.awt.Color;
import java.awt.Font;

/**
 * Defines dimensions, spacing, typography, colors, and drawing constants for
 * the Swing view.
 *
 * <p>Dimension and radius values are expressed in logical pixels. The
 * constants are shared defaults rather than live user preferences; changing
 * one takes effect only after recompiling and recreating the affected view.</p>
 */
public final class ViewSettings {

    /**
     * Prevents instantiation of this constants class.
     */
    private ViewSettings() {
    }

    // ########################################################################
    // Frame
    // ########################################################################

    /**
     * Minimum main-frame width.
     */
    public static final int FRAME_WIDTH_MIN = 1920;
    /**
     * Maximum main-frame width.
     */
    public static final int FRAME_WIDTH_MAX = 3000;
    /**
     * Preferred main-frame width.
     */
    public static final int FRAME_WIDTH_PREFERRED = 1920;
    /**
     * Minimum main-frame height.
     */
    public static final int FRAME_HEIGHT_MIN = 1080;
    /**
     * Maximum main-frame height.
     */
    public static final int FRAME_HEIGHT_MAX = 2000;
    /**
     * Preferred main-frame height.
     */
    public static final int FRAME_HEIGHT_PREFERRED = 1080;
    /**
     * Padding between the frame edge and its root content.
     */
    public static final int FRAME_PADDING = 16;

    // ########################################################################
    // Colors
    // ########################################################################

    /**
     * Background color of the root panel.
     */
    public static final Color ROOT_PANEL_BACKGROUND_COLOR = new Color(60, 63, 65);
    /**
     * Background color of primary cards.
     */
    public static final Color CARD_BACKGROUND_COLOR = new Color(43, 43, 43);
    /**
     * Accent background color of nested cards.
     */
    public static final Color NESTED_CARD_BACKGROUND_COLOR = new Color(204, 119, 34);
    /**
     * Foreground color of buttons.
     */
    public static final Color BUTTON_FOREGROUND_COLOR = new Color(230, 230, 230);
    /**
     * Background color of buttons.
     */
    public static final Color BUTTON_BACKGROUND_COLOR = new Color(70, 70, 70);
    /**
     * Foreground color of labels.
     */
    public static final Color LABEL_FOREGROUND_COLOR = new Color(204, 119, 34);
    /**
     * Foreground color of tabbed panes.
     */
    public static final Color TABBEDPANE_FOREGROUND_COLOR = new Color(230, 230, 230);
    /**
     * Accent background color of tabbed panes.
     */
    public static final Color TABBEDPANE_BACKGROUND_COLOR = new Color(204, 119, 34);
    /**
     * Foreground color of sliders.
     */
    public static final Color SLIDER_FOREGROUND_COLOR = new Color(230, 230, 230);
    /**
     * Background color of sliders.
     */
    public static final Color SLIDER_BACKGROUND_COLOR = new Color(55, 55, 55);
    /**
     * Background color of separators.
     */
    public static final Color SEPARATOR_BACKGROUND_COLOR = new Color(90, 90, 90);
    /**
     * Background color of text areas.
     */
    public static final Color TEXTAREA_BACKGROUND_COLOR = new Color(45, 45, 45);
    /**
     * Foreground color of text areas.
     */
    public static final Color TEXTAREA_FOREGROUND_COLOR = new Color(230, 230, 230);
    /**
     * Caret color of text areas.
     */
    public static final Color TEXTAREA_CARET_COLOR = new Color(230, 230, 230);
    /**
     * Grid-line color of tables.
     */
    public static final Color TABLE_GRID_COLOR = new Color(43, 43, 43);

    // ########################################################################
    // Fonts
    // ########################################################################

    /**
     * Default font for general view text.
     */
    public static final Font FONT_DEFAULT = new Font(Font.SANS_SERIF, Font.BOLD, 11);
    /**
     * Font used for symbols and labels drawn over the map.
     */
    public static final Font FONT_MAP_SYMBOLS = new Font(Font.SANS_SERIF, Font.BOLD | Font.ITALIC, 11);
    /**
     * Font used for photo-agency data.
     */
    public static final Font FONT_PHOTO_AGENCY = new Font(Font.MONOSPACED, Font.PLAIN, 11);
    /**
     * Font used for task data.
     */
    public static final Font FONT_TASK = new Font(Font.MONOSPACED, Font.PLAIN, 11);
    /**
     * Font used for drone data.
     */
    public static final Font FONT_DRONE = new Font(Font.MONOSPACED, Font.PLAIN, 11);
    /**
     * Font used for editable input.
     */
    public static final Font FONT_INPUT = new Font(Font.SERIF, Font.ITALIC, 11);
    /**
     * Font used for formatted output.
     */
    public static final Font FONT_OUTPUT = new Font(Font.MONOSPACED, Font.PLAIN, 11);
    /**
     * Default font color where a component does not supply a semantic color.
     */
    public static final Color FONT_COLOR_DEFAULT = Color.BLACK;

    // ########################################################################
    // Panels and Layout
    // ########################################################################

    /**
     * Standard gap between adjacent panels.
     */
    public static final int PANEL_GAP = 8;
    /**
     * Standard corner radius of rounded panels.
     */
    public static final int PANEL_CORNER_RADIUS = 8;
    /**
     * Standard empty-border thickness.
     */
    public static final int EMPTY_BORDER_WIDTH = 10;
    /**
     * Standard top padding inside panels.
     */
    public static final int PANEL_PADDING_TOP = 10;
    /**
     * Standard bottom padding inside panels.
     */
    public static final int PANEL_PADDING_BOTTOM = 10;
    /**
     * Standard left padding inside panels.
     */
    public static final int PANEL_PADDING_LEFT = 10;
    /**
     * Standard right padding inside panels.
     */
    public static final int PANEL_PADDING_RIGHT = 10;

    /**
     * Preferred width of the frame's north region.
     */
    public static final int NORTH_PANEL_WIDTH = 2048;
    /**
     * Preferred height of the frame's north region.
     */
    public static final int NORTH_PANEL_HEIGHT = 64;
    /**
     * Preferred width of the title panel.
     */
    public static final int TITLE_PANEL_WIDTH = 2048;
    /**
     * Preferred height of the title panel.
     */
    public static final int TITLE_PANEL_HEIGHT = 64;

    /**
     * Preferred width of the frame's west region.
     */
    public static final int WEST_PANEL_WIDTH = 420;
    /**
     * Preferred height of the frame's west region.
     */
    public static final int WEST_PANEL_HEIGHT = 1280;
    /**
     * Preferred width of the settings panel.
     */
    public static final int SETTINGS_PANEL_WIDTH = 512;
    /**
     * Preferred height of the settings panel.
     */
    public static final int SETTINGS_PANEL_HEIGHT = 512;
    /**
     * Preferred width of the photo-agency panel.
     */
    public static final int PHOTO_AGENCY_PANEL_WIDTH = 512;
    /**
     * Preferred height of the photo-agency panel.
     */
    public static final int PHOTO_AGENCY_PANEL_HEIGHT = 256;
    /**
     * Preferred width of the drone panel.
     */
    public static final int DRONE_PANEL_WIDTH = 512;
    /**
     * Preferred height of the drone panel.
     */
    public static final int DRONE_PANEL_HEIGHT = 256;
    /**
     * Preferred width of the task panel.
     */
    public static final int TASK_PANEL_WIDTH = 512;
    /**
     * Preferred height of the task panel.
     */
    public static final int TASK_PANEL_HEIGHT = 256;

    /**
     * Preferred width of the frame's center region.
     */
    public static final int CENTER_PANEL_WIDTH = 1024;
    /**
     * Preferred height of the frame's center region.
     */
    public static final int CENTER_PANEL_HEIGHT = 1280;
    /**
     * Preferred width of the controls panel.
     */
    public static final int CONTROLS_PANEL_WIDTH = 1024;
    /**
     * Preferred height of the controls panel.
     */
    public static final int CONTROLS_PANEL_HEIGHT = 64;
    /**
     * Preferred width of the map panel.
     */
    public static final int MAP_PANEL_WIDTH = 760;
    /**
     * Preferred height of the map panel.
     */
    public static final int MAP_PANEL_HEIGHT = 900;
    /**
     * Preferred width of the map-tools area.
     */
    public static final int MAP_TOOLS_WIDTH = 180;
    /**
     * Preferred height of the map-tools area.
     */
    public static final int MAP_TOOLS_HEIGHT = 900;
    /**
     * Preferred width of the thumbnail panel.
     */
    public static final int THUMBS_PANEL_WIDTH = 1024;
    /**
     * Preferred height of the thumbnail panel.
     */
    public static final int THUMBS_PANEL_HEIGHT = 128;

    /**
     * Preferred width of the frame's east region.
     */
    public static final int EAST_PANEL_WIDTH = 420;
    /**
     * Preferred height of the frame's east region.
     */
    public static final int EAST_PANEL_HEIGHT = 1280;
    /**
     * Preferred width of the drone-details panel.
     */
    public static final int DRONE_DETAILS_PANEL_WIDTH = 512;
    /**
     * Preferred height of the drone-details panel.
     */
    public static final int DRONE_DETAILS_PANEL_HEIGHT = 640;
    /**
     * Preferred width of the photo panel.
     */
    public static final int PHOTO_PANEL_WIDTH = 512;
    /**
     * Preferred height of the photo panel.
     */
    public static final int PHOTO_PANEL_HEIGHT = 640;

    /**
     * Preferred width of the frame's south region.
     */
    public static final int SOUTH_PANEL_WIDTH = 2048;
    /**
     * Preferred height of the frame's south region.
     */
    public static final int SOUTH_PANEL_HEIGHT = 64;
    /**
     * Preferred width of the status panel.
     */
    public static final int STATUS_PANEL_WIDTH = 2048;
    /**
     * Preferred height of the status panel.
     */
    public static final int STATUS_PANEL_HEIGHT = 64;

    // ########################################################################
    // Buttons
    // ########################################################################

    /**
     * Corner radius of rounded buttons.
     */
    public static final int BUTTON_CORNER_RADIUS = 10;
    /**
     * Standard control-button width.
     */
    public static final int CONTROL_BUTTON_WIDTH = 96;
    /**
     * Standard control-button height.
     */
    public static final int CONTROL_BUTTON_HEIGHT = 30;
    /**
     * Width and height of a control-button icon.
     */
    public static final int CONTROL_BUTTON_ICON_SIZE = 16;
    /**
     * Gap between a control-button icon and its text.
     */
    public static final int CONTROL_BUTTON_ICON_TEXT_GAP = 8;

    // ########################################################################
    // Text Areas
    // ########################################################################

    /**
     * Insets applied around text-area content.
     */
    public static final int TEXTAREA_MARGIN_INSET = 10;

    // ########################################################################
    // Map Component
    // ########################################################################

    /**
     * Radius of point symbols drawn on the map.
     */
    public static final int POINT_RADIUS = 6;

    /**
     * Color of an enqueued-task map symbol.
     */
    public static final Color ENQUEUED_TASK_SYMBOL_COLOR = Color.RED;
    /**
     * Color of an assigned-task map symbol.
     */
    public static final Color ASSIGNED_TASK_SYMBOL_COLOR = Color.YELLOW;
    /**
     * Color of a completed-task map symbol.
     */
    public static final Color COMPLETED_TASK_SYMBOL_COLOR = Color.GREEN;
    /**
     * Color of the drone-base map symbol.
     */
    public static final Color DRONE_BASE_POSITION_SYMBOL_COLOR = Color.BLUE;

    /**
     * Radius used when hit-testing a drone symbol.
     */
    public static final int DRONE_HITBOX_RADIUS = 16;
    /**
     * Radius of the hover highlight around a drone.
     */
    public static final int HOVERED_DRONE_HIGHLIGHT_RADIUS = 16;
    /**
     * Radius of the selection highlight around a drone.
     */
    public static final int SELECTED_DRONE_HIGHLIGHT_RADIUS = 20;

    /**
     * Color of drone map symbols.
     */
    public static final Color DRONE_SYMBOL_COLOR = Color.BLUE;

    /**
     * Translucent fill color of a hovered-drone highlight.
     */
    public static final Color HOVERED_DRONE_HIGHLIGHT_FILL_COLOR = new Color(255, 255, 255, 70);
    /**
     * Translucent border color of a hovered-drone highlight.
     */
    public static final Color HOVERED_DRONE_HIGHLIGHT_BORDER_COLOR = new Color(255, 255, 255, 140);
    /**
     * Translucent fill color of a selected-drone highlight.
     */
    public static final Color SELECTED_DRONE_HIGHLIGHT_FILL_COLOR = new Color(204, 119, 34, 90);
    /**
     * Translucent border color of a selected-drone highlight.
     */
    public static final Color SELECTED_DRONE_HIGHLIGHT_BORDER_COLOR = new Color(204, 119, 34, 180);

    /**
     * Radius used when hit-testing a completed-task symbol.
     */
    public static final int COMPLETED_TASK_HITBOX_RADIUS = 10;
    /**
     * Radius of the hover highlight around a completed task.
     */
    public static final int HOVERED_TASK_HIGHLIGHT_RADIUS = 10;
    /**
     * Radius of the selection highlight around a completed task.
     */
    public static final int SELECTED_TASK_HIGHLIGHT_RADIUS = 14;

    /**
     * Translucent fill color of a hovered-task highlight.
     */
    public static final Color HOVERED_TASK_HIGHLIGHT_FILL_COLOR = new Color(255, 255, 255, 70);
    /**
     * Translucent border color of a hovered-task highlight.
     */
    public static final Color HOVERED_TASK_HIGHLIGHT_BORDER_COLOR = new Color(255, 255, 255, 140);
    /**
     * Translucent fill color of a selected-task highlight.
     */
    public static final Color SELECTED_TASK_HIGHLIGHT_FILL_COLOR = new Color(204, 119, 34, 90);
    /**
     * Translucent border color of a selected-task highlight.
     */
    public static final Color SELECTED_TASK_HIGHLIGHT_BORDER_COLOR = new Color(204, 119, 34, 180);

    /**
     * Base color of points in a captured-video trail.
     */
    public static final Color VIDEO_TRAIL_SYMBOL_COLOR = Color.WHITE;
    /**
     * Alpha composite applied when drawing each video-trail point.
     */
    public static final float VIDEO_TRAIL_ALPHA = 0.02f;
    /**
     * Radius of each video-trail point.
     */
    public static final double VIDEO_TRAIL_POINT_RADIUS = 2.0;

    // ########################################################################
    // Thumbnail Strip
    // ########################################################################

    /**
     * Maximum number of task thumbnails displayed in the strip.
     */
    public static final int TASK_THUMBNAIL_STRIP_SIZE = 8;
    /**
     * Width and height of a square task-thumbnail image.
     */
    public static final int TASK_THUMBNAIL_IMAGE_SIZE = 72;
    /**
     * Width of a task-thumbnail card.
     */
    public static final int TASK_THUMBNAIL_CARD_WIDTH = 84;
    /**
     * Height of a task-thumbnail card.
     */
    public static final int TASK_THUMBNAIL_CARD_HEIGHT = 108;
}
