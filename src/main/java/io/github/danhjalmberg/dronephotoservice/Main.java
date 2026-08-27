package io.github.danhjalmberg.dronephotoservice;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import io.github.danhjalmberg.dronephotoservice.controllers.Controller;
import io.github.danhjalmberg.dronephotoservice.models.Model;
import io.github.danhjalmberg.dronephotoservice.views.View;

import java.awt.EventQueue;

/**
 * Launches and composes the Drone Photo Service desktop application.
 *
 * <p>Startup configures the optional FlatLaf theme and then creates the root
 * {@link Model}, {@link View}, and {@link Controller} on Swing's Event
 * Dispatch Thread. Constructor injection establishes the dependencies between
 * the three MVC layers.</p>
 *
 * @author Dan Hjälmberg
 */
public final class Main {

    /**
     * Whether startup replaces the platform look and feel with FlatLaf.
     */
    private static final boolean USE_FLATLAF = true;

    /**
     * Prevents instantiation of this application entry-point class.
     */
    private Main() {
    }

    /**
     * Configures the look and feel and schedules construction of the
     * application object graph on Swing's Event Dispatch Thread.
     *
     * <p>Command-line arguments are currently ignored. The method returns
     * after placing the construction task on the event queue; the Swing event
     * thread subsequently owns application startup and user-interface work.</p>
     *
     * @param args command-line arguments; currently ignored
     */
    public static void main(final String[] args) {

        if (USE_FLATLAF) {
            // Register the bundled overrides before FlatLaf reads its defaults.
            FlatLaf.registerCustomDefaultsSource("themes");
            FlatDarkLaf.setup();
        }

        EventQueue.invokeLater(() -> {
            Model model = new Model();
            View view = new View();
            new Controller(model, view);
        });
    }
}
