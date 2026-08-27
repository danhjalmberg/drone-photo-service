package io.github.danhjalmberg.dronephotoservice.views.support;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.net.URL;
import java.util.Objects;

/**
 * Loads dark-theme icons from the application class path.
 *
 * <p>Icon names are resolved below {@code /icons/dark/}. Missing resources are
 * treated as programming or packaging errors and cause an exception rather than
 * producing an empty icon.</p>
 */
public final class IconLoader {

    private static final String DARK_ICON_PATH = "/icons/dark/";

    /**
     * Prevents instantiation of this utility class.
     */
    private IconLoader() { }

    /**
     * Loads an icon from the dark-theme resource directory.
     *
     * @param iconFileName the name of the icon file to load
     * @return loaded icon
     * @throws NullPointerException if the named resource does not exist
     */
    public static ImageIcon loadIcon(String iconFileName) {

        URL resource = IconLoader.class.getResource(DARK_ICON_PATH + iconFileName);

        return new ImageIcon(Objects.requireNonNull(
                resource,
                "Icon resource not found: " + DARK_ICON_PATH + iconFileName));
    }

    /**
     * Loads an icon and smoothly scales it to a square of the requested size.
     *
     * @param iconFileName the name of the icon file to load
     * @param size target width and height in pixels
     * @return loaded and scaled icon
     * @throws NullPointerException if the named resource does not exist
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    public static ImageIcon loadScaledIcon(String iconFileName, int size) {

        ImageIcon icon = loadIcon(iconFileName);

        Image scaledImage = icon.getImage().getScaledInstance(
                size,
                size,
                Image.SCALE_SMOOTH);

        return new ImageIcon(scaledImage);
    }
}
