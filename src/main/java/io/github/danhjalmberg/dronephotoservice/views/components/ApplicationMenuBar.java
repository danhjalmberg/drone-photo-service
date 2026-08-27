package io.github.danhjalmberg.dronephotoservice.views.components;

import io.github.danhjalmberg.dronephotoservice.controllers.Commands;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * Provides File, Simulation, and Help commands for the main window.
 *
 * <p>Menu items publish the shared {@link Commands} action strings. Lifecycle
 * and export methods only project controller state into enabled flags; this
 * component does not decide which transitions are valid.</p>
 *
 * @author Dan Hjälmberg
 */
public class ApplicationMenuBar extends JMenuBar {

    private JMenuItem loadMapItem;
    private JMenuItem saveImagesItem;
    private JMenuItem exitItem;
    private JMenuItem simulationNewItem;
    private JMenuItem simulationStartItem;
    private JMenuItem simulationPauseItem;
    private JMenuItem simulationResumeItem;
    private JMenuItem simulationStopItem;
    private JMenuItem helpItem;
    private JMenuItem aboutItem;

    /**
     * Creates all menus, commands, mnemonics, and keyboard accelerators.
     */
    public ApplicationMenuBar() {

        JMenu fileMenu = createFileMenu();
        JMenu simulationMenu = createSimulationMenu();
        JMenu helpMenu = createHelpMenu();

        add(fileMenu);
        add(simulationMenu);
        add(helpMenu);
    }

    /**
     * Adds the same command listener to every actionable menu item.
     *
     * @param listener the command listener to add
     */
    public void addCommandListener(ActionListener listener) {

        loadMapItem.addActionListener(listener);
        saveImagesItem.addActionListener(listener);
        exitItem.addActionListener(listener);
        simulationNewItem.addActionListener(listener);
        simulationStartItem.addActionListener(listener);
        simulationPauseItem.addActionListener(listener);
        simulationResumeItem.addActionListener(listener);
        simulationStopItem.addActionListener(listener);
        helpItem.addActionListener(listener);
        aboutItem.addActionListener(listener);
    }

    /**
     * Applies the lifecycle-derived enabled state of simulation and export items.
     *
     * @param newEnabled whether the new item should be enabled
     * @param startEnabled whether the start item should be enabled
     * @param pauseEnabled whether the pause item should be enabled
     * @param resumeEnabled whether the resume item should be enabled
     * @param stopEnabled whether the stop item should be enabled
     * @param saveImagesEnabled whether the save images item should be enabled
     */
    public void setSimulationControls(
            boolean newEnabled,
            boolean startEnabled,
            boolean pauseEnabled,
            boolean resumeEnabled,
            boolean stopEnabled,
            boolean saveImagesEnabled) {

        simulationNewItem.setEnabled(newEnabled);
        simulationStartItem.setEnabled(startEnabled);
        simulationPauseItem.setEnabled(pauseEnabled);
        simulationResumeItem.setEnabled(resumeEnabled);
        simulationStopItem.setEnabled(stopEnabled);
        saveImagesItem.setEnabled(saveImagesEnabled);
    }

    /**
     * Disables commands that conflict with an active image export.
     * Passing {@code false} has no effect; normal state is restored separately by
     * the control-state controller.
     *
     * @param saving whether images are currently being saved
     */
    public void setSavingControls(boolean saving) {

        if (!saving) {
            return;
        }

        loadMapItem.setEnabled(false);
        saveImagesItem.setEnabled(false);

        simulationNewItem.setEnabled(false);
        simulationStartItem.setEnabled(false);
        simulationPauseItem.setEnabled(false);
        simulationResumeItem.setEnabled(false);
        simulationStopItem.setEnabled(false);

    }

    /**
     * Enables or disables commands for loading a map.
     *
     * @param enabled whether map loading is enabled
     */
    public void setMapLoadControlsEnabled(boolean enabled) {

        loadMapItem.setEnabled(enabled);
    }

    /**
     * Creates the file menu.
     *
     * @return the created file menu
     */
    private JMenu createFileMenu() {

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');

        loadMapItem = createMenuItem(
                Commands.LOAD_MAP,
                Commands.LOAD_MAP,
                KeyEvent.VK_O,
                InputEvent.CTRL_DOWN_MASK,
                KeyEvent.VK_O);

        saveImagesItem = createMenuItem(
                Commands.SAVE_IMAGES,
                Commands.SAVE_IMAGES,
                KeyEvent.VK_S,
                InputEvent.CTRL_DOWN_MASK,
                KeyEvent.VK_S);

        exitItem = new JMenuItem(Commands.EXIT);
        exitItem.setActionCommand(Commands.EXIT);
        exitItem.setMnemonic(KeyEvent.VK_X);

        fileMenu.add(loadMapItem);
        fileMenu.add(saveImagesItem);
        fileMenu.addSeparator(); // horizontal line between menu items
        fileMenu.add(exitItem);

        return fileMenu;
    }

    /**
     * Creates the simulation menu.
     *
     * @return the created simulation menu
     */
    private JMenu createSimulationMenu() {

        JMenu simulationMenu = new JMenu("Simulation");
        simulationMenu.setMnemonic('S');

        simulationNewItem = createMenuItem(
                Commands.NEW,
                Commands.NEW,
                KeyEvent.VK_N,
                InputEvent.CTRL_DOWN_MASK,
                KeyEvent.VK_N);

        simulationStartItem = createMenuItem(
                Commands.START,
                Commands.START,
                KeyEvent.VK_R,
                InputEvent.CTRL_DOWN_MASK,
                KeyEvent.VK_R);

        simulationPauseItem = createMenuItem(
                Commands.PAUSE,
                Commands.PAUSE,
                KeyEvent.VK_U,
                InputEvent.CTRL_DOWN_MASK,
                KeyEvent.VK_U);

        simulationResumeItem = createMenuItem(
                Commands.RESUME,
                Commands.RESUME,
                KeyEvent.VK_M,
                InputEvent.CTRL_DOWN_MASK,
                KeyEvent.VK_M);

        simulationStopItem = createMenuItem(
                Commands.STOP,
                Commands.STOP,
                KeyEvent.VK_P,
                InputEvent.CTRL_DOWN_MASK,
                KeyEvent.VK_P);

        simulationMenu.add(simulationNewItem);
        simulationMenu.add(simulationStartItem);
        simulationMenu.add(simulationPauseItem);
        simulationMenu.add(simulationResumeItem);
        simulationMenu.add(simulationStopItem);

        return simulationMenu;
    }

    /**
     * Creates the help menu.
     *
     * @return the created help menu
     */
    private JMenu createHelpMenu() {

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic('H');

        helpItem = new JMenuItem(Commands.HELP);
        helpItem.setActionCommand(Commands.HELP);
        helpItem.setMnemonic(KeyEvent.VK_H);

        aboutItem = new JMenuItem(Commands.ABOUT);
        aboutItem.setActionCommand(Commands.ABOUT);
        aboutItem.setMnemonic(KeyEvent.VK_A);

        helpMenu.add(helpItem);
        helpMenu.addSeparator();
        helpMenu.add(aboutItem);

        return helpMenu;
    }

    /**
     * Creates a menu item with action command, accelerator, and mnemonic.
     *
     * @param text the menu item text
     * @param actionCommand the action command
     * @param acceleratorKey the accelerator key
     * @param acceleratorModifier the accelerator modifier
     * @param mnemonic the mnemonic key
     * @return the created menu item
     */
    private JMenuItem createMenuItem(
            String text,
            String actionCommand,
            int acceleratorKey,
            int acceleratorModifier,
            int mnemonic) {

        JMenuItem item = new JMenuItem(text);

        item.setActionCommand(actionCommand);
        item.setAccelerator(KeyStroke.getKeyStroke(
                acceleratorKey,
                acceleratorModifier));
        item.setMnemonic(mnemonic);

        return item;
    }
}
