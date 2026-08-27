package io.github.danhjalmberg.dronephotoservice.views.panels;

import io.github.danhjalmberg.dronephotoservice.models.events.SimulationEvent;
import io.github.danhjalmberg.dronephotoservice.settings.ViewSettings;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.text.DefaultCaret;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Insets;
import java.util.List;

/**
 * Displays a bounded chronological projection of simulation events.
 *
 * <p>Events are appended as fixed-column, monospaced lines and the oldest lines
 * are removed after the display exceeds 500 entries. The caret does not follow
 * appended text automatically, preserving the user's scroll position.</p>
 */
public class EventLogPanel extends JPanel {

    private static final int MAX_DISPLAYED_EVENTS = 500;

    private final JTextArea textArea;

    /**
     * Creates an empty, read-only event log.
     */
    public EventLogPanel() {

        setLayout(new BorderLayout());

        textArea = new JTextArea();
        textArea.setEditable(false);

        Font currentFont = textArea.getFont();
        textArea.setFont(new Font(
                Font.MONOSPACED,
                currentFont.getStyle(),
                currentFont.getSize() - 1));

        textArea.setMargin(new Insets(
                ViewSettings.TEXTAREA_MARGIN_INSET,
                ViewSettings.TEXTAREA_MARGIN_INSET,
                ViewSettings.TEXTAREA_MARGIN_INSET,
                ViewSettings.TEXTAREA_MARGIN_INSET));

        textArea.setBackground(ViewSettings.TEXTAREA_BACKGROUND_COLOR);
        textArea.setForeground(ViewSettings.TEXTAREA_FOREGROUND_COLOR);
        textArea.setCaretColor(ViewSettings.TEXTAREA_CARET_COLOR);

        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Formats and appends events in supplied order, then enforces the display
     * limit. An empty list has no effect.
     *
     * @param events events to append
     */
    public void appendEvents(List<SimulationEvent> events) {

        for (SimulationEvent event : events) {
            textArea.append(formatEvent(event));
            textArea.append("\n");
        }

        trimOldEvents();
    }

    /**
     * Removes oldest lines if the text area exceeds the display limit. A Swing
     * document-offset failure is ignored so logging cannot disrupt the UI.
     */
    private void trimOldEvents() {

        int lineCount = textArea.getLineCount();

        if (lineCount <= MAX_DISPLAYED_EVENTS) {
            return;
        }

        try {
            int linesToRemove = lineCount - MAX_DISPLAYED_EVENTS;

            int endOffset = textArea.getLineEndOffset(linesToRemove - 1);

            textArea.getDocument().remove(
                    0,
                    endOffset);

        } catch (Exception exception) {
            // Ignore trimming failures.
        }
    }

    /**
     * Clears the event log display.
     */
    public void clear() {
        textArea.setText("");
    }

    /**
     * Formats one event for display.
     *
     * @param event simulation event
     * @return formatted event text
     */
    private String formatEvent(SimulationEvent event) {

        return String.format(
                "%-10s %-18s %-18s %s",
                event.getFormattedSimulationTime(),
                event.getType(),
                event.getSourceName(),
                event.getMessage());
    }
}
