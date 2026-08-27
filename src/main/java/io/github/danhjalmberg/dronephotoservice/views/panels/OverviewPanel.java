package io.github.danhjalmberg.dronephotoservice.views.panels;

import io.github.danhjalmberg.dronephotoservice.settings.ModelSettings;
import io.github.danhjalmberg.dronephotoservice.settings.ViewSettings;
import io.github.danhjalmberg.dronephotoservice.views.support.ViewFactory;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Displays read-only overview tables for photo agencies, drones, and queued
 * tasks.
 *
 * <p>Table models are replaced on refresh and column widths are recalculated from
 * their contents. Drone selection is preserved by name across model replacement;
 * clicking empty table space clears it. Table construction also installs shared
 * Swing table colors and borders through {@link UIManager} defaults.</p>
 */
public class OverviewPanel extends JPanel {

    private static final int TABLE_ROW_HEIGHT = 20;
    private static final int TABLE_HEADER_HEIGHT = 24;
    private static final int TABLE_COLUMN_MARGIN = 12;
    private static final int TABLE_COLUMN_MAX_WIDTH = 250;

    private static final String[] PHOTO_AGENCY_COLUMNS = {
            "Name", "Created", "Pending Task", "Pending"
    };

    private static final String[] DRONE_COLUMNS = {
            "Name", "State", "Task", "Completed", "X", "Y"
    };

    private static final String[] TASK_COLUMNS = {
            "Name", "Type", "X", "Y"
    };

    private final JTable photoAgencyTable;
    private final JTable droneTable;
    private final JTable taskTable;

    /**
     * Creates the three vertically stacked, independently scrollable tables.
     */
    public OverviewPanel() {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        add(ViewFactory.createSpacedSubsectionTitleLabel("PHOTO AGENCIES"));
        photoAgencyTable = createTable(PHOTO_AGENCY_COLUMNS);
        add(createTableScrollPane(
                photoAgencyTable,
                ModelSettings.PHOTO_AGENCY_POOL_SIZE_MAX));

        add(ViewFactory.createSpacedSubsectionTitleLabel("DRONES"));
        droneTable = createTable(DRONE_COLUMNS);
        add(createTableScrollPane(
                droneTable,
                ModelSettings.DRONE_POOL_SIZE_MAX));

        droneTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                int row = droneTable.rowAtPoint(event.getPoint());

                if (row < 0) {
                    droneTable.clearSelection();
                }
            }
        });

        add(ViewFactory.createSpacedSubsectionTitleLabel("QUEUED TASKS"));
        taskTable = createTable(TASK_COLUMNS);
        add(createTableScrollPane(
                taskTable,
                ModelSettings.TASK_QUEUE_SIZE_MAX));
    }

    /**
     * Adds selection listener to the drone table.
     *
     * @param listener the listener to add
     */
    public void addDroneTableSelectionListener(ListSelectionListener listener) {
        droneTable.getSelectionModel().addListSelectionListener(listener);
    }

    /**
     * Returns the identity from column zero of the selected drone row.
     *
     * @return the name of the selected drone, or null if no drone is selected
     */
    public String getSelectedDroneName() {

        int selectedRow = droneTable.getSelectedRow();

        if (selectedRow < 0) {
            return null;
        }

        return droneTable.getValueAt(selectedRow, 0).toString();
    }

    /**
     * Selects the first row whose name equals {@code droneName}. A null or unknown
     * name clears selection.
     *
     * @param droneName the name of the drone to select
     */
    public void selectDroneByName(String droneName) {

        if (droneName == null) {
            droneTable.clearSelection();
            return;
        }

        for (int row = 0; row < droneTable.getRowCount(); row++) {
            if (droneName.equals(droneTable.getValueAt(row, 0))) {
                if (droneTable.getSelectedRow() != row) {
                    droneTable.setRowSelectionInterval(row, row);
                }
                return;
            }
        }

        droneTable.clearSelection();
    }

    /**
     * Displays the given data in the photo agency table.
     *
     * @param data the data to display, where each inner array represents a row
     */
    public void displayPhotoAgencyOverview(Object[][] data) {

        photoAgencyTable.setModel(createReadOnlyTableModel(data, PHOTO_AGENCY_COLUMNS));

        adjustColumnWidths(photoAgencyTable);
    }

    /**
     * Replaces drone rows while attempting to preserve selection by drone name.
     *
     * @param data the data to display, where each inner array represents a row
     */
    public void displayDroneOverview(Object[][] data) {

        String selectedDroneName = getSelectedDroneName();

        droneTable.setModel(createReadOnlyTableModel(data, DRONE_COLUMNS));

        adjustColumnWidths(droneTable);

        if (selectedDroneName != null) {
            selectDroneByName(selectedDroneName);
        }
    }

    /**
     * Displays the given data in the task table.
     *
     * @param data the data to display, where each inner array represents a row
     */
    public void displayTaskOverview(Object[][] data) {
        taskTable.setModel(createReadOnlyTableModel(data, TASK_COLUMNS));

        adjustColumnWidths(taskTable);
    }

    /**
     * Clears the tables in the overview panel.
     */
    public void clear() {

        displayPhotoAgencyOverview(new Object[0][PHOTO_AGENCY_COLUMNS.length]);

        displayDroneOverview(new Object[0][DRONE_COLUMNS.length]);

        displayTaskOverview(new Object[0][TASK_COLUMNS.length]);
    }

    /**
     * Creates a consistently styled, read-only table for the supplied columns.
     *
     * @param columnNames The names of the columns for the table model.
     * @return configured table with no rows
     */
    private JTable createTable(String[] columnNames) {

        UIManager.put("TableHeader.cellBorder", BorderFactory.createLineBorder(ViewSettings.TABLE_GRID_COLOR, 1));
        UIManager.put("TableHeader.border", BorderFactory.createLineBorder(Color.GREEN));
        UIManager.put("Table.scrollPaneBorder", BorderFactory.createLineBorder(ViewSettings.TABLE_GRID_COLOR, 2));

        JTable table = new JTable(new Object[0][columnNames.length], columnNames);

        // Disable auto-resizing to allow horizontal scrolling
        table.setAutoCreateColumnsFromModel(false);

        table.setRowHeight(TABLE_ROW_HEIGHT);
        table.setFillsViewportHeight(true);
        table.setShowGrid(true);
        table.setBorder(BorderFactory.createLineBorder(ViewSettings.TABLE_GRID_COLOR, 1));  // working

        adjustColumnWidths(table);

        return table;
    }

    /**
     * Creates a scroll pane for the specified table with a maximum height based on the number of rows.
     *
     * @param table   The table to be placed inside the scroll pane.
     * @param maxRows The maximum number of rows to display before scrolling is enabled.
     * @return A JScrollPane containing the specified table with a maximum height based on the number of rows.
     */
    private JScrollPane createTableScrollPane(JTable table, int maxRows) {
        JScrollPane scrollPane = new JScrollPane(table);

        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        Dimension size = getTableScrollPaneSize(table, maxRows);
        scrollPane.setPreferredSize(size);
        scrollPane.setMaximumSize(size);

        return scrollPane;
    }

    /**
     * Gets the size of a scroll pane for a table based on the number of rows
     * and the maximum number of rows to display.
     *
     * @param table   The table for which to calculate the scroll pane size.
     * @param maxRows The maximum number of rows to display before scrolling is enabled.
     * @return The dimension of the scroll pane based on the table and maximum rows.
     */
    private Dimension getTableScrollPaneSize(JTable table, int maxRows) {
        int height = TABLE_HEADER_HEIGHT + table.getRowHeight() * maxRows;

        return new Dimension(Integer.MAX_VALUE, height);
    }

    /**
     * Creates a read-only table model with the specified data and column names.
     *
     * @param data        The data to be displayed in the table model.
     * @param columnNames The names of the columns for the table model.
     * @return A read-only table model with the specified data and column names.
     */
    private DefaultTableModel createReadOnlyTableModel(
            Object[][] data,
            String[] columnNames) {

        return new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /**
     * Adjusts each column so that its header is always visible and its preferred
     * width accommodates the widest displayed cell, up to a maximum width.
     *
     * @param table the table whose columns should be adjusted
     */
    private void adjustColumnWidths(JTable table) {
        TableColumnModel columnModel = table.getColumnModel();

        for (int columnIndex = 0;
             columnIndex < columnModel.getColumnCount();
             columnIndex++) {

            TableColumn column = columnModel.getColumn(columnIndex);

            int headerWidth = getHeaderPreferredWidth(
                    table,
                    column,
                    columnIndex);

            int contentWidth = getContentPreferredWidth(
                    table,
                    columnIndex);

            int minimumWidth = headerWidth + TABLE_COLUMN_MARGIN;
            int preferredWidth = Math.max(headerWidth, contentWidth)
                    + TABLE_COLUMN_MARGIN;

            column.setMinWidth(minimumWidth);
            column.setPreferredWidth(
                    Math.min(preferredWidth, TABLE_COLUMN_MAX_WIDTH));
        }
    }

    /**
     * Returns the preferred width of a column header using the table header's
     * renderer.
     *
     * @param table       the table containing the column
     * @param column      the column whose header width is to be calculated
     * @param columnIndex the index of the column in the table model
     * @return the preferred width of the column header
     */
    private int getHeaderPreferredWidth(
            JTable table,
            TableColumn column,
            int columnIndex) {

        TableCellRenderer renderer = column.getHeaderRenderer();

        if (renderer == null) {
            renderer = table.getTableHeader().getDefaultRenderer();
        }

        Component component = renderer.getTableCellRendererComponent(
                table,
                column.getHeaderValue(),
                false,
                false,
                -1,
                columnIndex);

        return component.getPreferredSize().width;
    }

    /**
     * Returns the width required by the widest currently displayed cell in a column.
     *
     * @param table       the table containing the column
     * @param columnIndex the index of the column in the table model
     * @return the preferred width of the widest cell in the column
     */
    private int getContentPreferredWidth(
            JTable table,
            int columnIndex) {

        int width = 0;

        for (int rowIndex = 0;
             rowIndex < table.getRowCount();
             rowIndex++) {

            TableCellRenderer renderer =
                    table.getCellRenderer(rowIndex, columnIndex);

            Component component = table.prepareRenderer(
                    renderer,
                    rowIndex,
                    columnIndex);

            width = Math.max(
                    width,
                    component.getPreferredSize().width);
        }

        return width;
    }
}
