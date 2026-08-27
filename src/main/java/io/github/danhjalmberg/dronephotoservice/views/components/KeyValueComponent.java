package io.github.danhjalmberg.dronephotoservice.views.components;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Font;

/**
 * Displays a fixed key and mutable value in a transparent horizontal row.
 * The component appends a colon to the plain-weight key and emphasizes the value
 * with bold text.
 */
public class KeyValueComponent extends JPanel {

    private final JLabel keyLabel;
    private final JLabel valueLabel;

    /**
     * Creates a key-value row using the default 12-point font size.
     *
     * @param key   the key of the key-value component
     * @param value the value of the key-value component
     */
    public KeyValueComponent(String key, String value) {

        this(key, value, 12f);
    }

    /**
     * Creates a key-value row using the specified font size.
     *
     * @param key      the key of the key-value component
     * @param value    the value of the key-value component
     * @param fontSize the font size of the key and value labels
     */
    public KeyValueComponent(String key, String value, float fontSize) {

        setOpaque(false);

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        keyLabel = new JLabel(key + ":");
        keyLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        valueLabel = new JLabel(value);

        Font baseFont = keyLabel.getFont().deriveFont(fontSize);
        keyLabel.setFont(baseFont.deriveFont(Font.PLAIN));
        valueLabel.setFont(baseFont.deriveFont(Font.BOLD));

        add(keyLabel);
        add(valueLabel);
    }

    /**
     * Replaces the value-label text; the key remains unchanged.
     *
     * @param value the value to set
     */
    public void setValueText(String value) {
        valueLabel.setText(value);
    }
}
