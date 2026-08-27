package io.github.danhjalmberg.dronephotoservice.views.panels;

import io.github.danhjalmberg.dronephotoservice.models.snapshots.DroneSnapshot;
import io.github.danhjalmberg.dronephotoservice.settings.ViewSettings;
import io.github.danhjalmberg.dronephotoservice.views.components.CameraViewComponent;
import io.github.danhjalmberg.dronephotoservice.views.support.ViewFactory;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;

/**
 * Displays telemetry and an optional live camera image for the selected drone.
 *
 * <p>Snapshot values are formatted for presentation in world meters. Battery and
 * speed telemetry are normalized to 0–100 progress values. Clearing details also
 * clears the last camera image but retains the user's live-view checkbox choice.</p>
 */
public class DroneDetailsPanel extends JPanel {

    private final JLabel nameValueLabel;
    private final JLabel batteryValueLabel;
    private final JProgressBar batteryProgressBar;
    private final JLabel cameraValueLabel;
    private final JLabel motorValueLabel;
    private final JProgressBar speedProgressBar;
    private final JLabel stateValueLabel;
    private final JLabel taskValueLabel;
    private final JLabel completedValueLabel;
    private final JLabel basePositionValueLabel;
    private final JLabel currentPositionValueLabel;

    private final JCheckBox liveViewCheckBox;
    private final CameraViewComponent liveImageComponent;

    /**
     * Initializes the drone details panel.
     */
    public DroneDetailsPanel() {

        nameValueLabel = ViewFactory.createDetailValueLabel("No drone selected.");
        batteryValueLabel = ViewFactory.createDetailValueLabel("");
        batteryProgressBar = ViewFactory.createTelemetryProgressBar();
        cameraValueLabel = ViewFactory.createDetailValueLabel("");
        motorValueLabel = ViewFactory.createDetailValueLabel("");
        speedProgressBar = ViewFactory.createTelemetryProgressBar();
        stateValueLabel = ViewFactory.createDetailValueLabel("");
        taskValueLabel = ViewFactory.createDetailValueLabel("");
        completedValueLabel = ViewFactory.createDetailValueLabel("");
        basePositionValueLabel = ViewFactory.createDetailValueLabel("");
        currentPositionValueLabel = ViewFactory.createDetailValueLabel("");

        liveViewCheckBox = new JCheckBox("Enable live camera view");
        liveViewCheckBox.setOpaque(false);

        liveImageComponent = new CameraViewComponent();

        setLayout(new BorderLayout(
                ViewSettings.PANEL_GAP,
                ViewSettings.PANEL_GAP));

        setOpaque(false);

        add(createDroneDataPanel(), BorderLayout.NORTH);
        add(createDroneVisualPanel(), BorderLayout.CENTER);
    }

    /**
     * Creates the drone data panel.
     *
     * @return the created drone data panel
     */
    private JPanel createDroneDataPanel() {

        JPanel droneDataPanel = new JPanel();
        droneDataPanel.setOpaque(false);
        droneDataPanel.setLayout(new BoxLayout(droneDataPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = ViewFactory.createSpacedSectionTitleLabel("Drone Data");
        droneDataPanel.add(titleLabel);
        droneDataPanel.add(ViewFactory.createSeparator());

        JPanel rowsPanel = new JPanel(new GridBagLayout());
        rowsPanel.setOpaque(false);
        rowsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        int row = 0;
        ViewFactory.addDetailRow(rowsPanel, row++, "Name:", nameValueLabel);
        ViewFactory.addDetailRow(rowsPanel, row++, "Battery type:", batteryValueLabel);
        ViewFactory.addDetailRow(rowsPanel, row++, "Battery level:", batteryProgressBar);
        ViewFactory.addDetailRow(rowsPanel, row++, "Camera:", cameraValueLabel);
        ViewFactory.addDetailRow(rowsPanel, row++, "Motor:", motorValueLabel);
        ViewFactory.addDetailRow(rowsPanel, row++, "Speed:", speedProgressBar);
        ViewFactory.addDetailRow(rowsPanel, row++, "State:", stateValueLabel);
        ViewFactory.addDetailRow(rowsPanel, row++, "Task:", taskValueLabel);
        ViewFactory.addDetailRow(rowsPanel, row++, "Completed:", completedValueLabel);
        ViewFactory.addDetailRow(rowsPanel, row++, "Base position:", basePositionValueLabel);
        ViewFactory.addDetailRow(rowsPanel, row++, "Current position:", currentPositionValueLabel);

        droneDataPanel.add(rowsPanel);

        return droneDataPanel;
    }

    /**
     * Creates the drone visual panel.
     *
     * @return the created drone visual panel
     */
    private JPanel createDroneVisualPanel() {

        JPanel droneVisualPanel = new JPanel();
        droneVisualPanel.setOpaque(false);
        droneVisualPanel.setLayout(new BoxLayout(droneVisualPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = ViewFactory.createSpacedSectionTitleLabel("Live Camera View");
        droneVisualPanel.add(titleLabel);
        droneVisualPanel.add(ViewFactory.createSeparator());

        JPanel liveViewCheckBoxPanel = new JPanel(new FlowLayout(
                FlowLayout.CENTER,
                ViewSettings.PANEL_GAP,
                ViewSettings.PANEL_GAP));

        liveViewCheckBoxPanel.setOpaque(false);
        liveViewCheckBoxPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        liveViewCheckBoxPanel.add(liveViewCheckBox);
        droneVisualPanel.add(liveViewCheckBoxPanel);

        JPanel wrapper = new JPanel();
        wrapper.setBackground(ViewSettings.CARD_BACKGROUND_COLOR);
        wrapper.add(liveImageComponent);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        droneVisualPanel.add(wrapper);

        return droneVisualPanel;
    }

    /**
     * Replaces all telemetry fields from a snapshot, or clears them for
     * {@code null}.
     *
     * @param drone the drone snapshot to display, or null if no drone is selected
     */
    public void displayDroneDetails(DroneSnapshot drone) {

        if (drone == null) {
            clearDroneDetails();
            return;
        }

        nameValueLabel.setText(drone.getName());
        batteryValueLabel.setText(drone.getBattery().getType());

        updateBatteryProgress(drone);
        updateSpeedProgress(drone);

        cameraValueLabel.setText(drone.getCamera().getType());
        motorValueLabel.setText(drone.getMotor().getType());
        stateValueLabel.setText(drone.getState().getDisplayName());

        taskValueLabel.setText(
                drone.getAssignedTask() == null
                        ? "None"
                        : drone.getAssignedTask().getName());

        completedValueLabel.setText(
                String.valueOf(drone.getCompletedTasks().size()));

        basePositionValueLabel.setText(String.format(
                "%.0f, %.0f",
                drone.getBasePositionMeters().getX(),
                drone.getBasePositionMeters().getY()));

        currentPositionValueLabel.setText(String.format(
                "%.0f, %.0f",
                drone.getCurrentPositionMeters().getX(),
                drone.getCurrentPositionMeters().getY()));
    }

    /**
     * Restores the no-selection state and removes the camera image.
     */
    public void clearDroneDetails() {

        nameValueLabel.setText("No drone selected.");
        batteryValueLabel.setText("");
        batteryProgressBar.setValue(0);
        batteryProgressBar.setString("");
        batteryProgressBar.setToolTipText(null);
        cameraValueLabel.setText("");
        motorValueLabel.setText("");
        speedProgressBar.setValue(0);
        speedProgressBar.setString("");
        speedProgressBar.setToolTipText(null);
        stateValueLabel.setText("");
        taskValueLabel.setText("");
        completedValueLabel.setText("");
        basePositionValueLabel.setText("");
        currentPositionValueLabel.setText("");

        displayDroneLiveImage(null);
    }

    /**
     * Displays the live camera image.
     *
     * @param image the live camera image, or null to clear the image
     */
    public void displayDroneLiveImage(BufferedImage image) {

        liveImageComponent.setPhoto(image);
    }

    /**
     * Reports whether live-camera refresh is enabled by the user.
     *
     * @return {@code true} when the user has enabled live-camera refresh
     */
    public boolean isLiveCameraViewEnabled() {

        return liveViewCheckBox.isSelected();
    }

    /**
     * Updates the battery progress bar.
     *
     * @param drone the drone snapshot containing battery data
     */
    private void updateBatteryProgress(DroneSnapshot drone) {

        double currentCharge = drone.getBattery().getCurrentChargeSeconds();
        double capacity = drone.getBattery().getCapacitySeconds();

        int batteryPercent = capacity <= 0.0
                ? 0
                : (int) Math.round((currentCharge / capacity) * 100.0);

        batteryPercent = Math.max(0, Math.min(100, batteryPercent));

        batteryProgressBar.setValue(batteryPercent);
        batteryProgressBar.setString(batteryPercent + "%");
        batteryProgressBar.setToolTipText(String.format(
                "%.0f / %.0f seconds",
                currentCharge,
                capacity));
    }

    /**
     * Updates the speed progress bar.
     *
     * @param drone the drone snapshot containing motor speed data
     */
    private void updateSpeedProgress(DroneSnapshot drone) {

        double currentSpeed = drone.getMotor().getCurrentSpeed();
        double maxSpeed = drone.getMotor().getMaxSpeed();

        int speedPercent = maxSpeed <= 0.0
                ? 0
                : (int) Math.round((currentSpeed / maxSpeed) * 100.0);

        speedPercent = Math.max(0, Math.min(100, speedPercent));

        speedProgressBar.setValue(speedPercent);
        speedProgressBar.setString(String.format(
                "%.0f / %.0f m/s",
                currentSpeed,
                maxSpeed));

        speedProgressBar.setToolTipText(speedPercent + "% of max speed");
    }
}
