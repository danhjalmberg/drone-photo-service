package io.github.danhjalmberg.dronephotoservice.views.panels;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.util.Objects;

/**
 * Organizes photo-agency, drone, and completed-task diagnostics into tabs.
 * Exact empty strings receive user-facing empty-monitor messages; other values,
 * including {@code null}, are forwarded to the text component.
 */
public class MonitorsPanel extends JPanel {

    private final MonitorTabPanel photoAgencyMonitorPanel;
    private final MonitorTabPanel droneMonitorPanel;
    private final MonitorTabPanel completedTaskMonitorPanel;

    /**
     * Creates the three monitor tabs.
     */
    public MonitorsPanel() {
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();

        photoAgencyMonitorPanel = new MonitorTabPanel();
        droneMonitorPanel = new MonitorTabPanel();
        completedTaskMonitorPanel = new MonitorTabPanel();

        tabs.addTab("PHOTO AGENCIES", photoAgencyMonitorPanel);
        tabs.addTab("DRONES", droneMonitorPanel);
        tabs.addTab("COMPLETED TASKS", completedTaskMonitorPanel);

        add(tabs, BorderLayout.CENTER);
    }

    /**
     * Updates the photo agency monitor display with the given count and text.
     *
     * @param photoAgencyCount          the number of active photo agencies
     * @param photoAgencyDiagnosticText diagnostic text for photo agencies
     */
    public void displayPhotoAgencyMonitor(
            int photoAgencyCount,
            String photoAgencyDiagnosticText) {

        photoAgencyMonitorPanel.setCounterText(
                "Number of active Photo Agencies: " + photoAgencyCount);

        photoAgencyMonitorPanel.setMonitorText(
                Objects.equals(photoAgencyDiagnosticText, "")
                        ? "The photo agency monitor is empty."
                        : photoAgencyDiagnosticText);
    }

    /**
     * Updates the drone monitor display with the given count and text.
     *
     * @param droneCount          the number of active drones
     * @param droneDiagnosticText diagnostic text for drones
     */
    public void displayDroneMonitor(
            int droneCount,
            String droneDiagnosticText) {

        droneMonitorPanel.setCounterText(
                "Number of active Drones: " + droneCount);

        droneMonitorPanel.setMonitorText(
                Objects.equals(droneDiagnosticText, "")
                        ? "The drone monitor is empty."
                        : droneDiagnosticText);
    }

    /**
     * Updates the completed-task monitor display with the given count and text.
     *
     * @param completedTaskCount       the number of completed tasks
     * @param archivedTaskDiagnosticText diagnostic text for archived tasks
     */
    public void displayCompletedTaskMonitor(
            int completedTaskCount,
            String archivedTaskDiagnosticText) {

        completedTaskMonitorPanel.setCounterText(
                "Number of completed Tasks: " + completedTaskCount);

        completedTaskMonitorPanel.setMonitorText(
                Objects.equals(archivedTaskDiagnosticText, "")
                        ? "The task monitor is empty."
                        : archivedTaskDiagnosticText);
    }

    /**
     * Resets all counters to zero and displays their empty-state messages.
     */
    public void clear() {
        displayPhotoAgencyMonitor(0, "");
        displayDroneMonitor(0, "");
        displayCompletedTaskMonitor(0, "");
    }
}
