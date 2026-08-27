package io.github.danhjalmberg.dronephotoservice.views.components;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Displays operational help for the application.
 *
 * <p>The resizable dialog is modeless so it can remain open during simulation
 * interaction. Help topics start collapsed, expand independently, and are
 * presented in a vertically scrolling view. Closing hides the reusable dialog.</p>
 *
 * @author Dan Hjälmberg
 */
public class HelpDialog extends JDialog {

    private static final int DIALOG_WIDTH = 480;
    private static final int DIALOG_HEIGHT = 700;
    private static final int CONTENT_WIDTH = 440;

    private static final String COLLAPSED_ARROW = "\u25B6";
    private static final String EXPANDED_ARROW = "\u25BC";

    /**
     * Text content for one named help-reference item.
     */
    private static final class ReferenceItem {

        private final String name;
        private final String description;

        private ReferenceItem(
                String name,
                String description) {

            this.name = name;
            this.description = description;
        }
    }

    /**
     * Creates a modeless application help dialog.
     *
     * @param owner parent application frame
     */
    public HelpDialog(JFrame owner) {

        super(owner, "Drone Photo Service Help", false);

        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setLayout(new BorderLayout());

        JScrollPane scrollPane = createHelpScrollPane();
        add(scrollPane, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        setPreferredSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
        pack();

        setMinimumSize(new Dimension(400, 400));
        setLocationRelativeTo(owner);
        setResizable(true);
    }

    /**
     * Creates the scrollable help content.
     *
     * @return scroll pane containing all help sections
     */
    private JScrollPane createHelpScrollPane() {

        JPanel contentPanel = createHelpContentPanel();

        JScrollPane scrollPane = new JScrollPane(contentPanel);

        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        return scrollPane;
    }

    /**
     * Creates and populates the complete help-content panel.
     *
     * @return populated help-content panel
     */
    private JPanel createHelpContentPanel() {

        JPanel contentPanel = new JPanel();

        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        addTitle(
                contentPanel,
                "Drone Photo Service Help");

        addParagraph(
                contentPanel,
                """
                        This guide explains how to prepare, run, inspect, and save the
                        results of a drone photo service simulation.
                        """);

        addQuickStartSection(contentPanel);
        addSimulationControlsSection(contentPanel);
        addMapSymbolsSection(contentPanel);
        addSelectionSection(contentPanel);
        addReviewingResultsSection(contentPanel);
        addSavingResultsSection(contentPanel);
        addSimulationSettingsSection(contentPanel);
        addDisplaySettingsSection(contentPanel);
        addInterfaceReferenceSection(contentPanel);

        // Keeps the content aligned to the top if the dialog becomes taller
        // than the help text.
        contentPanel.add(Box.createVerticalGlue());

        return contentPanel;
    }

    /**
     * Adds the main help title.
     *
     * @param parent destination panel
     * @param text   title text
     */
    private void addTitle(JPanel parent, String text) {

        JLabel label = new JLabel(text, SwingConstants.LEFT);

        label.setFont(label.getFont().deriveFont(
                Font.BOLD,
                label.getFont().getSize2D() + 5.0f));

        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        parent.add(label);
        parent.add(Box.createVerticalStrut(12));
    }

    /**
     * Adds a collapsible section containing one text paragraph.
     *
     * @param parent  destination panel
     * @param heading section heading
     * @param text    section text
     */
    private void addTextSection(
            JPanel parent,
            String heading,
            String text) {

        JPanel bodyPanel = createSectionBodyPanel();

        addParagraph(bodyPanel, text);
        addCollapsibleSection(parent, heading, bodyPanel);
    }

    /**
     * Adds a collapsible section containing named reference items.
     *
     * @param parent  destination panel
     * @param heading section heading
     * @param items   reference items to display
     */
    private void addReferenceSection(
            JPanel parent,
            String heading,
            ReferenceItem... items) {

        JPanel bodyPanel = createSectionBodyPanel();

        for (ReferenceItem referenceItem : items) {
            addReferenceItem(
                    bodyPanel,
                    referenceItem.name,
                    referenceItem.description);
        }

        addCollapsibleSection(parent, heading, bodyPanel);
    }

    /**
     * Adds a prepared body panel as a collapsible help section.
     *
     * @param parent    destination panel
     * @param heading   section heading
     * @param bodyPanel expandable section content
     */
    private void addCollapsibleSection(
            JPanel parent,
            String heading,
            JPanel bodyPanel) {

        parent.add(Box.createVerticalStrut(8));

        JPanel sectionPanel = createCollapsibleSection(
                heading,
                bodyPanel);

        sectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(sectionPanel);
    }

    /**
     * Creates the expandable body panel used by a help section.
     *
     * @return empty section body panel
     */
    private JPanel createSectionBodyPanel() {

        JPanel bodyPanel = new JPanel();

        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setOpaque(false);
        bodyPanel.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 0));
        bodyPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        return bodyPanel;
    }

    /**
     * Creates one collapsible help section.
     *
     * @param heading   section heading
     * @param bodyPanel expandable section content
     * @return collapsible section panel
     */
    private JPanel createCollapsibleSection(
            String heading,
            JPanel bodyPanel) {

        JPanel sectionPanel = new JPanel();

        sectionPanel.setLayout(new BoxLayout(sectionPanel, BoxLayout.Y_AXIS));
        sectionPanel.setOpaque(false);

        JToggleButton headingButton = new JToggleButton(
                formatSectionHeading(heading, false));

        headingButton.setHorizontalAlignment(SwingConstants.LEFT);

        headingButton.setFont(
                headingButton.getFont().deriveFont(
                        Font.BOLD,
                        headingButton.getFont().getSize2D() + 2.0f));

        headingButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        /*
         * Let BoxLayout stretch the heading to the available content width
         * while retaining its preferred height.
         */
        Dimension headingSize = headingButton.getPreferredSize();

        headingButton.setMaximumSize(new Dimension(
                Integer.MAX_VALUE,
                headingSize.height));

        headingButton.setToolTipText("Expand " + heading);

        // Every section starts collapsed.
        bodyPanel.setVisible(false);

        headingButton.addActionListener(event -> {

            boolean expanded = headingButton.isSelected();

            bodyPanel.setVisible(expanded);

            headingButton.setText(
                    formatSectionHeading(heading, expanded));

            headingButton.setToolTipText(
                    (expanded ? "Collapse " : "Expand ")
                            + heading);

            /*
             * Visibility changes affect the preferred height of this section
             * and therefore require the scrollable content to be laid out again.
             */
            sectionPanel.revalidate();
            sectionPanel.repaint();
        });

        sectionPanel.add(headingButton);
        sectionPanel.add(bodyPanel);

        return sectionPanel;
    }

    /**
     * Formats a section heading with an expansion-state indicator.
     *
     * @param heading  section heading
     * @param expanded whether the section is expanded
     * @return formatted heading
     */
    private String formatSectionHeading(
            String heading,
            boolean expanded) {

        String arrow = expanded
                ? EXPANDED_ARROW
                : COLLAPSED_ARROW;

        return arrow + "  " + heading;
    }

    /**
     * Creates one help-reference item.
     *
     * @param name        item name
     * @param description item description
     * @return created reference item
     */
    private static ReferenceItem item(
            String name,
            String description) {

        return new ReferenceItem(name, description);
    }

    /**
     * Adds a wrapped, read-only paragraph.
     *
     * @param parent destination panel
     * @param text   paragraph text
     */
    private void addParagraph(
            JPanel parent,
            String text) {

        parent.add(createParagraph(text));
    }

    /**
     * Creates a wrapped, read-only paragraph.
     *
     * @param text paragraph text
     * @return created paragraph component
     */
    private JTextArea createParagraph(String text) {

        JTextArea textArea = new JTextArea(text.strip());

        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setOpaque(false);
        textArea.setBorder(null);

        JLabel referenceLabel = new JLabel();

        textArea.setFont(referenceLabel.getFont());
        textArea.setForeground(referenceLabel.getForeground());
        textArea.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));
        textArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        return textArea;
    }

    /**
     * Adds a reference item consisting of a bold item name and an indented
     * wrapped description.
     *
     * @param parent      destination panel
     * @param item        item name
     * @param description item description
     */
    private void addReferenceItem(
            JPanel parent,
            String item,
            String description) {

        JLabel itemLabel = new JLabel("\u2022  " + item);

        itemLabel.setFont(itemLabel.getFont().deriveFont(Font.BOLD));

        itemLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        parent.add(itemLabel);
        parent.add(Box.createVerticalStrut(4));

        JTextArea descriptionArea = createParagraph(description);

        descriptionArea.setBorder(
                BorderFactory.createEmptyBorder(0, 18, 0, 0));

        parent.add(descriptionArea);
        parent.add(Box.createVerticalStrut(12));
    }

    /**
     * Adds the basic simulation workflow.
     *
     * @param parent destination panel
     */
    private void addQuickStartSection(JPanel parent) {

        addTextSection(
                parent,
                "Quick Start",
                """
                        1. Review the bundled demo map, or select Load to open another map.\n
                        2. Adjust the map scale and simulation settings before starting.\n
                        3. Select Start to begin the simulation.\n
                        4. Observe drones and tasks on the map. Use Pause and Resume when
                           necessary.\n
                        5. Select drones or completed tasks to inspect their details and
                           results.\n
                        6. Select Stop to finish the current simulation.\n
                        7. Select Save to export completed task results, or New to discard
                           the stopped run and prepare another simulation.
                        """);
    }

    /**
     * Adds a reference for the main command buttons.
     *
     * @param parent destination panel
     */
    private void addSimulationControlsSection(JPanel parent) {

        addReferenceSection(
                parent,
                "Simulation Controls",
                item(
                        "Load",
                        """
                                Opens an image file and uses it as the simulation map.
                                A map can be replaced only before a simulation is running.
                                """),

                item(
                        "New",
                        """
                                Discards the completed run while retaining the current
                                map. This command becomes available after the simulation
                                has stopped.
                                """),

                item(
                        "Start",
                        """
                                Starts a prepared simulation using the current settings.
                                """),

                item(
                        "Pause",
                        """
                                Temporarily stops simulation time and simulation activity.
                                The current run and its results are preserved.
                                """),

                item(
                        "Resume",
                        """
                                Continues a paused simulation from its current state.
                                """),

                item(
                        "Stop",
                        """
                                Ends the current run. The map and completed results remain
                                available until New is selected.
                                """),

                item(
                        "Save",
                        """
                                Exports the results of completed tasks. Saving is
                                available after the simulation has stopped.
                                """));
    }

    /**
     * Adds the map-symbol reference.
     *
     * @param parent destination panel
     */
    private void addMapSymbolsSection(JPanel parent) {

        addReferenceSection(
                parent,
                "Map Symbols",
                item(
                        "Blue ring — Base position",
                        """
                                The home position to which drones return for charging and
                                between assignments.
                                """),

                item(
                        "Blue circle — Drone",
                        """
                                The current position of an active drone.
                                """),

                item(
                        "Red circle — Enqueued task",
                        """
                                A task waiting in the shared queue for a drone.
                                """),

                item(
                        "Yellow circle — Assigned task",
                        """
                                A task that has been accepted by a drone.
                                """),

                item(
                        "Small green circle — Completed task",
                        """
                                A completed task whose details and result can be
                                inspected.
                                """),

                item(
                        "Faint white points — Video trail",
                        """
                                Capture positions belonging to completed video tasks.
                                Trails can be shown or hidden using the map display
                                controls.
                                """));
    }

    /**
     * Adds instructions for selecting and inspecting entities.
     *
     * @param parent destination panel
     */
    private void addSelectionSection(JPanel parent) {

        addReferenceSection(
                parent,
                "Selection and Details",
                item(
                        "Select a drone",
                        """
                                Click a drone on the map, or select it in the drone
                                overview table. The Drone details tab displays its state,
                                assignment, position, battery, speed, and camera
                                information.
                                """),

                item(
                        "Select a completed task",
                        """
                                Click a completed task on the map, or select its
                                thumbnail. The Task details tab displays its type,
                                agency, timestamps, position, image count, and result.
                                """),

                item(
                        "Change selection",
                        """
                                Selecting a drone clears the current task selection.
                                Selecting a task clears the current drone selection.
                                """),

                item(
                        "Clear selection",
                        """
                                Click an empty part of the map or press Escape.
                                """),

                item(
                        "Status bar",
                        """
                                The status bar shows the current mouse position,
                                selection, enqueued and completed task counts, and latest
                                activity.
                                """));
    }


    /**
     * Adds instructions for reviewing task results.
     *
     * @param parent destination panel
     */
    private void addReviewingResultsSection(JPanel parent) {

        addReferenceSection(
                parent,
                "Reviewing Results",
                item(
                        "Task thumbnails",
                        """
                                Recently completed tasks appear below the map. Select a
                                thumbnail to open its details and result.
                                """),

                item(
                        "Photo task",
                        """
                                Displays the captured result as a single image.
                                """),

                item(
                        "Video task",
                        """
                                Displays a preview frame. Use Play to view the captured
                                frame sequence and Stop to end playback.
                                """),

                item(
                        "Zoom task",
                        """
                                Displays a preview frame. Use Play to view the generated
                                zoom sequence and Stop to end playback.
                                """),

                item(
                        "Playback",
                        """
                                Play is available for video and zoom results containing
                                more than one frame. Changing or clearing the selection
                                stops the current playback.
                                """));
    }


    /**
     * Adds instructions for exporting completed task results.
     *
     * @param parent destination panel
     */
    private void addSavingResultsSection(JPanel parent) {

        addReferenceSection(
                parent,
                "Saving Results",
                item(
                        "Availability",
                        """
                                Stop the simulation before exporting. Save remains
                                disabled while the simulation is running, paused, or
                                stopping.
                                """),

                item(
                        "Destination",
                        """
                                Select Save or File → Save Images, then choose the
                                destination directory.
                                """),

                item(
                        "Progress",
                        """
                                Export runs in the background. A progress dialog reports
                                the number of processed tasks.
                                """),

                item(
                        "Cancellation",
                        """
                                Select Cancel Export, or close the progress dialog, to
                                request cancellation. Files completed before cancellation
                                are retained; the current incomplete task file is removed.
                                """),

                item(
                        "Empty archive",
                        """
                                If no completed task images are available, the application
                                reports that there is nothing to export.
                                """));
    }

    /**
     * Adds a reference for simulation-related settings.
     *
     * @param parent destination panel
     */
    private void addSimulationSettingsSection(JPanel parent) {

        addReferenceSection(
                parent,
                "Simulation Settings",
                item(
                        "Simulation tick interval",
                        """
                                Sets the duration of each simulation update. Smaller
                                intervals produce more frequent, finer-grained updates;
                                larger intervals advance the simulation in coarser steps.
                                """),

                item(
                        "Simulation speed",
                        """
                                Controls how quickly simulation time advances relative
                                to real-world time.
                                """),

                item(
                        "Task queue size",
                        """
                                Sets the maximum number of tasks that may wait for
                                assignment. A full queue prevents agencies from adding
                                further tasks until space becomes available.
                                """),

                item(
                        "Photo agency thread pool size",
                        """
                                Sets the number of photo agencies that produce tasks
                                during the simulation.
                                """),

                item(
                        "Drone thread pool size",
                        """
                                Sets the number of drones available to accept and
                                perform tasks.
                                """),

                item(
                        "Availability",
                        """
                                Simulation settings configure a new run and cannot be
                                changed while that run is active or after it has stopped.
                                Select New before configuring the next run.
                                """));
    }

    /**
     * Adds a reference for presentation-related settings.
     *
     * @param parent destination panel
     */
    private void addDisplaySettingsSection(JPanel parent) {

        addReferenceSection(
                parent,
                "Display Settings",
                item(
                        "GUI refresh interval",
                        """
                                Controls how often live simulation information is redrawn.
                                This affects presentation frequency, not simulation time.
                                """),

                item(
                        "Show map labels",
                        """
                                Shows or hides text labels beside map entities.
                                """),

                item(
                        "Show video trails",
                        """
                                Shows or hides the capture-position trails of completed
                                video tasks.
                                """),

                item(
                        "Enable live camera view",
                        """
                                Displays the latest camera image for the selected drone.
                                This option is available in the Drone details panel.
                                """),

                item(
                        "Map scale",
                        """
                                Defines the number of real-world meters represented by
                                one source-map pixel. The scale affects simulation
                                distances and may be edited only before starting the
                                simulation.
                                """));
    }

    /**
     * Adds a reference for the main information areas.
     *
     * @param parent destination panel
     */
    private void addInterfaceReferenceSection(JPanel parent) {

        addReferenceSection(
                parent,
                "Interface Reference",
                item(
                        "Settings",
                        """
                                Displays map metadata, simulation settings, and GUI
                                settings.
                                """),

                item(
                        "Overview",
                        """
                                Displays current photo agencies, drones, and enqueued
                                tasks in tabular form.
                                """),

                item(
                        "Monitors",
                        """
                                Displays detailed current information about photo
                                agencies, drones, and completed tasks.
                                """),

                item(
                        "Event Log",
                        """
                                Lists lifecycle and simulation events in chronological
                                order.
                                """),

                item(
                        "Map",
                        """
                                Visualizes the active map, drone positions, task
                                positions, selections, labels, and video trails.
                                """),

                item(
                        "Drone details",
                        """
                                Displays live information for the selected drone,
                                including optional camera output.
                                """),

                item(
                        "Task details",
                        """
                                Displays archived information and imagery for the
                                selected completed task.
                                """),

                item(
                        "Status bar",
                        """
                                Summarizes the mouse position, current selection, task
                                counts, and latest simulation activity.
                                """));
    }


    /**
     * Creates the bottom button row.
     *
     * @return panel containing the Close button
     */
    private JPanel createButtonPanel() {

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(event -> setVisible(false));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 12, 8));
        buttonPanel.add(closeButton);

        return buttonPanel;
    }
}
