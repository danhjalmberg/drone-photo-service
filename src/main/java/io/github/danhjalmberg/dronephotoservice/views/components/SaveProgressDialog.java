package io.github.danhjalmberg.dronephotoservice.views.components;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;

/**
 * Displays progress and requests cancellation during task-image export.
 *
 * <p>The dialog is modeless so application shutdown remains available. Both the
 * Cancel Export button and window close control produce the same one-time
 * cancellation callback. The dialog disables repeated requests but neither owns
 * nor directly cancels the export worker.</p>
 *
 * @author Dan Hjälmberg
 */
public class SaveProgressDialog extends JDialog {

    private final JLabel statusLabel;
    private final JProgressBar progressBar;
    private final JButton cancelButton;

    private boolean cancellationRequested;

    /**
     * Creates an export progress dialog.
     *
     * @param owner parent application frame
     * @param maxTasks number of tasks included in the export
     */
    public SaveProgressDialog(JFrame owner, int maxTasks) {

        super(owner, "Saving images", false);

        statusLabel = new JLabel("Preparing to save images...");
        progressBar = new JProgressBar(0, maxTasks);
        progressBar.setStringPainted(true);
        cancelButton = new JButton("Cancel Export");

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(statusLabel, BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.CENTER);
        panel.add(cancelButton, BorderLayout.SOUTH);

        setContentPane(panel);
        setPreferredSize(new Dimension(360, 150));
        pack();
        setLocationRelativeTo(owner);
        setResizable(false);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                cancelButton.doClick();
            }
        });
    }

    /**
     * Registers the operation that requests export cancellation.
     *
     * <p>When cancellation is requested, the button is disabled immediately and
     * the status label communicates that cancellation may take a short time.</p>
     *
     * @param listener listener that requests cancellation from the export controller
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void addCancelListener(ActionListener listener) {

        Objects.requireNonNull(
                listener,
                "Cancel listener must not be null.");

        cancelButton.addActionListener(event -> {

            if (cancellationRequested) {
                return;
            }

            cancellationRequested = true;
            cancelButton.setEnabled(false);
            statusLabel.setText("Cancelling export...");

            listener.actionPerformed(event);
        });
    }

    /**
     * Updates the progress range, completed count, and status text.
     *
     * <p> Progress values may already be queued on the EDT when cancellation is
     * requested. In that case, the progress bar may still advance, but the
     * cancellation status message is preserved.</p>
     *
     * @param value number of completed tasks
     * @param maxTasks total number of tasks
     */
    public void setProgress(int value, int maxTasks) {

        progressBar.setMaximum(maxTasks);
        progressBar.setValue(value);
        progressBar.setString(value + " / " + maxTasks);

        if (!cancellationRequested) {
            statusLabel.setText("Saving task " + value + " of " + maxTasks + "...");
        }
    }
}
