package io.github.danhjalmberg.dronephotoservice.views.panels;

import io.github.danhjalmberg.dronephotoservice.controllers.Commands;
import io.github.danhjalmberg.dronephotoservice.settings.ViewSettings;
import io.github.danhjalmberg.dronephotoservice.views.components.RoundedPanel;
import io.github.danhjalmberg.dronephotoservice.views.support.ControlButtonFactory;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

/**
 * Presents the primary map, simulation-lifecycle, and export commands.
 * Buttons publish the shared controller action commands; enabled states are
 * supplied externally from lifecycle and export state.
 */
public class SimulationControlsPanel extends RoundedPanel {

    private final JButton loadMapButton;
    private final JButton simulationNewButton;
    private final JButton simulationStartButton;
    private final JButton simulationPauseButton;
    private final JButton simulationResumeButton;
    private final JButton simulationStopButton;
    private final JButton saveImagesButton;

    /**
     * Creates the simulation controls panel with buttons for loading a map,
     * creating and starting a simulation, controlling an active simulation,
     * and saving completed task images.
     */
    public SimulationControlsPanel() {
        super(ViewSettings.PANEL_CORNER_RADIUS);

        Dimension size = new Dimension(
                ViewSettings.CONTROLS_PANEL_WIDTH,
                ViewSettings.CONTROLS_PANEL_HEIGHT);

        setPreferredSize(size);
        setMinimumSize(new Dimension(0, ViewSettings.CONTROLS_PANEL_HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, ViewSettings.CONTROLS_PANEL_HEIGHT));

        setBorder(BorderFactory.createEmptyBorder(
                ViewSettings.PANEL_PADDING_TOP,
                ViewSettings.PANEL_PADDING_LEFT,
                ViewSettings.PANEL_PADDING_BOTTOM,
                ViewSettings.PANEL_PADDING_RIGHT));

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel buttonsPanel = new JPanel(new FlowLayout(
                FlowLayout.CENTER,
                ViewSettings.PANEL_GAP,
                ViewSettings.PANEL_GAP));

        buttonsPanel.setOpaque(false);
        buttonsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        loadMapButton = ControlButtonFactory.createControlButton(
                "Load", Commands.LOAD_MAP, "map.png");

        simulationNewButton = ControlButtonFactory.createControlButton(
                "New", Commands.NEW, "add.png");

        simulationStartButton = ControlButtonFactory.createControlButton(
                "Start", Commands.START, "play.png");

        simulationPauseButton = ControlButtonFactory.createControlButton(
                "Pause", Commands.PAUSE, "pause.png");

        simulationResumeButton = ControlButtonFactory.createControlButton(
                "Resume", Commands.RESUME, "resume.png");

        simulationStopButton = ControlButtonFactory.createControlButton(
                "Stop", Commands.STOP, "stop.png");

        saveImagesButton = ControlButtonFactory.createControlButton(
                "Save", Commands.SAVE_IMAGES, "save.png");

        buttonsPanel.add(loadMapButton);
        buttonsPanel.add(simulationNewButton);
        buttonsPanel.add(simulationStartButton);
        buttonsPanel.add(simulationPauseButton);
        buttonsPanel.add(simulationResumeButton);
        buttonsPanel.add(simulationStopButton);
        buttonsPanel.add(saveImagesButton);

        add(buttonsPanel);
    }

    /**
     * Adds the supplied listener to every command button in the panel.
     *
     * @param listener the action listener to add
     */
    public void addCommandListener(ActionListener listener) {
        loadMapButton.addActionListener(listener);
        simulationNewButton.addActionListener(listener);
        simulationStartButton.addActionListener(listener);
        simulationPauseButton.addActionListener(listener);
        simulationResumeButton.addActionListener(listener);
        simulationStopButton.addActionListener(listener);
        saveImagesButton.addActionListener(listener);
    }

    /**
     * Enables or disables simulation commands according to the current
     * simulation lifecycle state.
     *
     * @param newEnabled whether the New button should be enabled
     * @param startEnabled whether the Start button should be enabled
     * @param pauseEnabled whether the Pause button should be enabled
     * @param resumeEnabled whether the Resume button should be enabled
     * @param stopEnabled whether the Stop button should be enabled
     * @param saveImagesEnabled whether the Save button should be enabled
     */
    public void setSimulationControls(
            boolean newEnabled,
            boolean startEnabled,
            boolean pauseEnabled,
            boolean resumeEnabled,
            boolean stopEnabled,
            boolean saveImagesEnabled) {

        simulationNewButton.setEnabled(newEnabled);
        simulationStartButton.setEnabled(startEnabled);
        simulationPauseButton.setEnabled(pauseEnabled);
        simulationResumeButton.setEnabled(resumeEnabled);
        simulationStopButton.setEnabled(stopEnabled);
        saveImagesButton.setEnabled(saveImagesEnabled);
    }

    /**
     * Enables or disables the map loading button.
     *
     * @param enabled whether the Load button should be enabled
     */
    public void setMapLoadControlsEnabled(boolean enabled) {

        loadMapButton.setEnabled(enabled);
    }

    /**
     * Temporarily disables simulation controls while task images are being
     * saved. When saving finishes, {@code ControlStateController} restores
     * the controls appropriate for the current lifecycle state.
     *
     * @param saving whether task images are currently being saved
     */
    public void setSavingControls(boolean saving) {

        if (!saving) {
            return;
        }

        loadMapButton.setEnabled(false);
        simulationNewButton.setEnabled(false);
        simulationStartButton.setEnabled(false);
        simulationPauseButton.setEnabled(false);
        simulationResumeButton.setEnabled(false);
        simulationStopButton.setEnabled(false);
        saveImagesButton.setEnabled(false);
    }
}
