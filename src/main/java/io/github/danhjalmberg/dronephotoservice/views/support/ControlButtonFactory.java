package io.github.danhjalmberg.dronephotoservice.views.support;

import io.github.danhjalmberg.dronephotoservice.settings.ViewSettings;
import io.github.danhjalmberg.dronephotoservice.views.components.RoundedButton;

import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.Dimension;

/**
 * Creates consistently styled application control buttons.
 *
 * <p>The factory applies dimensions, typography, colors, padding, and custom
 * rounded-button painting from {@link ViewSettings}. It configures action-command
 * strings but does not attach listeners.</p>
 */
public final class ControlButtonFactory {

    /**
     * Prevents instantiation of this utility class.
     */
    private ControlButtonFactory() { }

    /**
     * Creates a fixed-size control button with text, action command, and a scaled
     * dark-theme icon.
     *
     * @param text button label
     * @param actionCommand action command sent to the controller
     * @param iconFileName icon file name inside the icon resource folder
     * @return configured button without an action listener
     * @throws NullPointerException if the icon resource does not exist
     */
    public static JButton createControlButton(
            String text,
            String actionCommand,
            String iconFileName) {

        JButton button = new RoundedButton(text, ViewSettings.BUTTON_CORNER_RADIUS);

        button.setActionCommand(actionCommand);
        button.setIcon(IconLoader.loadScaledIcon(
                iconFileName,
                ViewSettings.CONTROL_BUTTON_ICON_SIZE));

        button.setHorizontalTextPosition(SwingConstants.RIGHT);
        button.setVerticalTextPosition(SwingConstants.CENTER);
        button.setIconTextGap(ViewSettings.CONTROL_BUTTON_ICON_TEXT_GAP);

        Dimension buttonSize = new Dimension(
                ViewSettings.CONTROL_BUTTON_WIDTH,
                ViewSettings.CONTROL_BUTTON_HEIGHT);
        button.setPreferredSize(buttonSize);
        button.setMinimumSize(buttonSize);
        button.setMaximumSize(buttonSize);

        button.setFont(ViewSettings.FONT_DEFAULT);
        button.setForeground(ViewSettings.BUTTON_FOREGROUND_COLOR);
        button.setBackground(ViewSettings.BUTTON_BACKGROUND_COLOR);

        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);

        button.setBorder(new EmptyBorder(4, 8, 4, 8));

        return button;
    }

    /**
     * Creates a fixed-size, text-only action button.
     *
     * @param text button label
     * @return configured button without an action command or listener
     */
    public static JButton createSmallActionButton(String text) {

        JButton button = new RoundedButton(text, ViewSettings.BUTTON_CORNER_RADIUS);

        Dimension buttonSize = new Dimension(90, 24);
        button.setPreferredSize(buttonSize);
        button.setMinimumSize(buttonSize);
        button.setMaximumSize(buttonSize);

        button.setFont(ViewSettings.FONT_DEFAULT);
        button.setForeground(ViewSettings.BUTTON_FOREGROUND_COLOR);
        button.setBackground(ViewSettings.BUTTON_BACKGROUND_COLOR);

        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorder(new EmptyBorder(3, 8, 3, 8));

        return button;
    }
}
