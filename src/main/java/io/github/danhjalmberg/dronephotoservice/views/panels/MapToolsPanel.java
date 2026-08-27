package io.github.danhjalmberg.dronephotoservice.views.panels;

import io.github.danhjalmberg.dronephotoservice.settings.ViewSettings;
import io.github.danhjalmberg.dronephotoservice.views.components.MapLegendComponent;
import io.github.danhjalmberg.dronephotoservice.views.components.MapSettingsComponent;
import io.github.danhjalmberg.dronephotoservice.views.components.RoundedPanel;
import io.github.danhjalmberg.dronephotoservice.views.support.ViewFactory;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Dimension;
import java.util.function.Consumer;

/**
 * Combines the map legend with overlay visibility settings.
 * Checkbox actions are translated into boolean callbacks for labels and completed
 * video trails; either callback may be {@code null}.
 */
public class MapToolsPanel extends RoundedPanel {

    private final MapSettingsComponent mapSettingsComponent;

    /**
     * Creates the map tools panel with a legend and map display settings.
     *
     * @param showLabelsListener      listener to be notified when the show labels setting is changed
     * @param showVideoTrailsListener listener to be notified when the show video trails setting is changed
     */
    public MapToolsPanel(Consumer<Boolean> showLabelsListener,
                         Consumer<Boolean> showVideoTrailsListener) {

        super(ViewSettings.PANEL_CORNER_RADIUS);

        Dimension size = new Dimension(
                ViewSettings.MAP_TOOLS_WIDTH,
                ViewSettings.MAP_TOOLS_HEIGHT);

        setPreferredSize(size);
        setMinimumSize(new Dimension(ViewSettings.MAP_TOOLS_WIDTH, 0));
        setMaximumSize(new Dimension(ViewSettings.MAP_TOOLS_WIDTH, Integer.MAX_VALUE));

        setBorder(BorderFactory.createEmptyBorder(
                ViewSettings.PANEL_PADDING_TOP,
                ViewSettings.PANEL_PADDING_LEFT,
                ViewSettings.PANEL_PADDING_BOTTOM,
                ViewSettings.PANEL_PADDING_RIGHT));

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JLabel legendLabel = ViewFactory.createSubsectionTitleLabel("MAP LEGEND");
        legendLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 6, 0));
        add(legendLabel);

        MapLegendComponent mapLegendComponent = new MapLegendComponent();
        mapLegendComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(mapLegendComponent);

        add(ViewFactory.createSeparator());

        JLabel settingsLabel = ViewFactory.createSubsectionTitleLabel("GRAPHIC SETTINGS");
        settingsLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 6, 0));
        add(settingsLabel);

        mapSettingsComponent = new MapSettingsComponent();
        mapSettingsComponent.setAlignmentX(Component.LEFT_ALIGNMENT);

        mapSettingsComponent.addShowLabelsListener(event -> {
            if (showLabelsListener != null) {
                showLabelsListener.accept(mapSettingsComponent.isShowLabelsSelected());
            }
        });

        mapSettingsComponent.addShowVideoTrailsListener(event -> {
            if (showVideoTrailsListener != null) {
                showVideoTrailsListener.accept(
                        mapSettingsComponent.isShowVideoTrailsSelected());
            }
        });

        add(mapSettingsComponent);
    }
}
