package io.github.danhjalmberg.dronephotoservice.views.panels;

import io.github.danhjalmberg.dronephotoservice.settings.ViewSettings;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.text.DefaultCaret;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.util.Objects;

/**
 * Displays one monitor count and a read-only, wrapped diagnostic text area.
 * The caret remains stationary, and identical text updates are ignored to avoid
 * resetting selection or scroll-related presentation unnecessarily.
 */
public class MonitorTabPanel extends JPanel {

    private final JLabel counterLabel;
    private final JTextArea textArea;

    /**
     * Creates an empty counter and monitor text area.
     */
    public MonitorTabPanel() {

        setOpaque(true);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(
                ViewSettings.PANEL_PADDING_TOP,
                0,
                ViewSettings.PANEL_PADDING_BOTTOM,
                0));

        counterLabel = new JLabel("", SwingConstants.CENTER);
        counterLabel.setFont(counterLabel.getFont().deriveFont(11f));
        counterLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        counterLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        add(counterLabel);

        textArea = new JTextArea();

        Font currentFont = textArea.getFont();
        textArea.setFont(new Font(
                Font.MONOSPACED,
                currentFont.getStyle(),
                currentFont.getSize() - 1));

        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setMargin(new Insets(
                ViewSettings.TEXTAREA_MARGIN_INSET,
                ViewSettings.TEXTAREA_MARGIN_INSET,
                ViewSettings.TEXTAREA_MARGIN_INSET,
                ViewSettings.TEXTAREA_MARGIN_INSET));
        textArea.setEditable(false);

        textArea.setBackground(ViewSettings.TEXTAREA_BACKGROUND_COLOR);
        textArea.setForeground(ViewSettings.TEXTAREA_FOREGROUND_COLOR);
        textArea.setCaretColor(ViewSettings.TEXTAREA_CARET_COLOR);

        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(scrollPane);
    }

    /**
     * Updates the counter label.
     *
     * @param text counter text
     */
    public void setCounterText(String text) {
        counterLabel.setText(text);
    }

    /**
     * Replaces monitor text only when it differs from the displayed value.
     *
     * @param text monitor text
     */
    public void setMonitorText(String text) {

        if (!Objects.equals(textArea.getText(), text)) {
            textArea.setText(text);
        }
    }
}
