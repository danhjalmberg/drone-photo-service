package io.github.danhjalmberg.dronephotoservice.views.components;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.event.ActionListener;

/**
 * Presents map-overlay visibility options.
 *
 * <p>Both labels and completed-video trails are selected initially; listeners are
 * attached directly to the corresponding checkboxes.</p>
 */
public class MapSettingsComponent extends JPanel {

    private final JCheckBox showLabelsCheckBox;
    private final JCheckBox showVideoTrailsCheckBox;

    /**
     * Creates the two vertically arranged, initially selected options.
     */
    public MapSettingsComponent() {

        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        showLabelsCheckBox = new JCheckBox("Show map labels", true);
        showLabelsCheckBox.setOpaque(false);
        showLabelsCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        showVideoTrailsCheckBox = new JCheckBox("Show video trails", true);
        showVideoTrailsCheckBox.setOpaque(false);
        showVideoTrailsCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(showLabelsCheckBox);
        add(showVideoTrailsCheckBox);
    }

    /**
     * Reports whether the map-label option is selected.
     *
     * @return {@code true} when map labels are enabled
     */
    public boolean isShowLabelsSelected() {
        return showLabelsCheckBox.isSelected();
    }

    /**
     * Adds listener for label visibility changes.
     *
     * @param listener listener to add
     */
    public void addShowLabelsListener(ActionListener listener) {
        showLabelsCheckBox.addActionListener(listener);
    }

    /**
     * Reports whether the completed-video-trail option is selected.
     *
     * @return {@code true} when completed-video trails are enabled
     */
    public boolean isShowVideoTrailsSelected() {
        return showVideoTrailsCheckBox.isSelected();
    }

    /**
     * Adds listener for video trail visibility changes.
     *
     * @param listener listener to add
     */
    public void addShowVideoTrailsListener(ActionListener listener) {
        showVideoTrailsCheckBox.addActionListener(listener);
    }
}
