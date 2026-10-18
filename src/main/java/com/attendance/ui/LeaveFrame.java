package com.attendance.ui;

import com.attendance.dao.EmployeeDAO;
import com.attendance.dao.LeaveDAO;
import com.attendance.model.Employee;
import com.attendance.model.LeaveRecord;
import com.attendance.model.User;
import com.attendance.util.AppTheme;
import com.attendance.util.SessionManager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Leave management: file a request, then (managers/admins) approve, reject
 * or cancel pending requests. Anyone can file; only ADMINISTRATOR and MANAGER can review.
 */
public class LeaveFrame extends JFrame {

    private static final String[] COLUMNS = {
        "Leave ID", "Emp ID", "Employee", "Department", "Start", "End",
        "Type", "Status", "Reason", "Reviewed By"
    };
    private static final String[] LEAVE_TYPES = {"SICK", "VACATION", "EMERGENCY", "UNPAID"};
    private static final String[] STATUS_FILTER = {"ALL", "PENDING", "APPROVED", "REJECTED"};
    private static final int STATUS_COL = 7;

    private final LeaveDAO leaveDAO = new LeaveDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final boolean canReview = SessionManager.isAdmin() || SessionManager.isManager();

    // form
    private final JComboBox<Employee> employeeCombo = new JComboBox<>();
    private final JComboBox<String> typeCombo = new JComboBox<>(LEAVE_TYPES);
    private final JTextField startField = new JTextField(LocalDate.now().toString(), 10);
    private final JTextField endField = new JTextField(LocalDate.now().toString(), 10);
    private final JTextArea reasonArea = new JTextArea(3, 30);
    private final JButton fileBtn = new JButton("FILE REQUEST");
    private final JButton clearBtn = new JButton("CLEAR");

    // table
    private final JComboBox<String> statusFilter = new JComboBox<>(STATUS_FILTER);
    private final JButton refreshBtn = new JButton("Refresh");
    private final JLabel countLabel = new JLabel(" ");
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override public Class<?> getColumnClass(int c) {
            return (c == 0 || c == 1) ? Integer.class : String.class;
        }
    };
    private final JTable table = new JTable(tableModel);

    // review buttons
    private final JButton approveBtn = new JButton("APPROVE");
    private final JButton rejectBtn = new JButton("REJECT");
    private final JButton cancelBtn = new JButton("CANCEL REQUEST");
    private final JButton closeBtn = new JButton("CLOSE");

    public LeaveFrame() {
        AppTheme.brand(this, "Leave Requests");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1100, 700);
        setMinimumSize(new Dimension(950, 600));

        buildUI();
        wireEvents();
        loadEmployees();
        refreshTable();
        setLocationRelativeTo(null);
    }

    // ---------------------------------------------------------------- UI

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // --- file-a-request form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("File a Leave Request"));
        employeeCombo.setPreferredSize(new Dimension(260, 26));

        addField(form, 0, 0, "Employee:", employeeCombo);
        addField(form, 1, 0, "Leave Type:", typeCombo);
        addField(form, 0, 1, "Start Date (yyyy-MM-dd):", startField);
        addField(form, 1, 1, "End Date (yyyy-MM-dd):", endField);

        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 8, 5, 8);
        gc.gridy = 2;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.gridx = 0;
        form.add(new JLabel("Reason (optional):"), gc);
        gc.gridx = 1;
        gc.gridwidth = 3;
        gc.weightx = 1.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        form.add(new JScrollPane(reasonArea), gc);

        JPanel formButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        formButtons.add(fileBtn);
        formButtons.add(clearBtn);

        JPanel north = new JPanel(new BorderLayout());
        north.add(form, BorderLayout.CENTER);
        north.add(formButtons, BorderLayout.SOUTH);
        root.add(north, BorderLayout.NORTH);

        // --- requests table
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filters.add(new JLabel("Status:"));
        filters.add(statusFilter);
        filters.add(refreshBtn);
        filters.add(Box.createHorizontalStrut(12));
        filters.add(countLabel);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(8).setPreferredWidth(180);
        table.getColumnModel().getColumn(STATUS_COL).setCellRenderer(new StatusRenderer());

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setBorder(BorderFactory.createTitledBorder("Leave Requests"));
        center.add(filters, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        // --- review buttons
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        south.add(approveBtn);
        south.add(rejectBtn);
        south.add(cancelBtn);
        south.add(closeBtn);
        if (!canReview) {
            approveBtn.setEnabled(false);
            rejectBtn.setEnabled(false);
            cancelBtn.setEnabled(false);
            approveBtn.setToolTipText("Only managers and administrators can review requests");
            rejectBtn.setToolTipText("Only managers and administrators can review requests");
            cancelBtn.setToolTipText("Only managers and administrators can cancel requests");
        }
        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void addField(JPanel p, int col, int row, String label, JComponent comp) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 8, 5, 8);
        gc.gridy = row;
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = col * 2;
        p.add(new JLabel(label), gc);

        gc.gridx = col * 2 + 1;
        gc.weightx = 1.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        p.add(comp, gc);
    }

    private void wireEvents() {
        fileBtn.addActionListener(e -> fileRequest());
        clearBtn.addActionListener(e -> clearForm());
        refreshBtn.addActionListener(e -> refreshTable());
        statusFilter.addActionListener(e -> refreshTable());
        approveBtn.addActionListener(e -> review("APPROVED"));
        rejectBtn.addActionListener(e -> review("REJECTED"));
        cancelBtn.addActionListener(e -> cancelRequest());
        closeBtn.addActionListener(e -> dispose());
    }

    // -------------------------------------------------------------- data

    private void loadEmployees() {
        try {
            employeeCombo.removeAllItems();
            User currentUser = SessionManager.getCurrentUser();
            Integer loggedInEmpId = (currentUser != null) ? currentUser.getEmployeeId() : null;

            List<Employee> allEmployees = employeeDAO.getAllEmployees();

            if (canReview) {
                // Administrators and Managers can file for anyone
                for (Employee e : allEmployees) {
                    if ("ACTIVE".equals(e.getStatus())) {
                        employeeCombo.addItem(e);
                    }
                }
                employeeCombo.setEnabled(true);
            } else {
                // Regular EMPLOYEES: Only allow them to see and select themselves
                Employee self = null;
                if (loggedInEmpId != null) {
                    self = employeeDAO.getEmployeeById(loggedInEmpId);
                }

                if (self != null && "ACTIVE".equals(self.getStatus())) {
                    employeeCombo.addItem(self);
                    employeeCombo.setSelectedItem(self);
                    employeeCombo.setEnabled(false); // Locked
                } else {
                    // Fallback: If account is not linked to an employee_id
                    JOptionPane.showMessageDialog(this,
                            "Your user account is not linked to an active Employee profile.\nPlease contact an Administrator.",
                            "Account Unlinked", JOptionPane.WARNING_MESSAGE);
                    fileBtn.setEnabled(false);
                    return;
                }
            }

            fileBtn.setEnabled(employeeCombo.getItemCount() > 0);
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }
    private void refreshTable() {
        String filter = (String) statusFilter.getSelectedItem();
        try {
            List<Object[]> rows = leaveDAO.getAllLeavesWithDetails();
            tableModel.setRowCount(0);
            for (Object[] r : rows) {
                if ("ALL".equals(filter) || filter.equals(r[STATUS_COL])) {
                    tableModel.addRow(r);
                }
            }
            countLabel.setText(tableModel.getRowCount() + " request(s)");
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    private void clearForm() {
    if (canReview) {
        if (employeeCombo.getItemCount() > 0) employeeCombo.setSelectedIndex(0);
    }
    typeCombo.setSelectedIndex(0);
    startField.setText(LocalDate.now().toString());
    endField.setText(LocalDate.now().toString());
    reasonArea.setText("");
}

    // ----------------------------------------------------------- actions

    private void fileRequest() {
        Employee emp = (Employee) employeeCombo.getSelectedItem();
        if (emp == null) {
            warn("Please select an employee.", "No Employee Selected");
            return;
        }

        LocalDate start;
        LocalDate end;
        try {
            start = LocalDate.parse(startField.getText().trim());
        } catch (DateTimeParseException ex) {
            warn("Start date must look like " + LocalDate.now() + " (yyyy-MM-dd).", "Invalid Start Date");
            startField.requestFocus();
            return;
        }
        try {
            end = LocalDate.parse(endField.getText().trim());
        } catch (DateTimeParseException ex) {
            warn("End date must look like " + LocalDate.now() + " (yyyy-MM-dd).", "Invalid End Date");
            endField.requestFocus();
            return;
        }
        if (end.isBefore(start)) {
            warn("The end date cannot be before the start date.", "Invalid Dates");
            endField.requestFocus();
            return;
        }

        String reason = reasonArea.getText().trim();
        if (reason.length() > 255) {
            warn("The reason must be 255 characters or fewer (currently " + reason.length() + ").",
                    "Reason Too Long");
            reasonArea.requestFocus();
            return;
        }

        String type = (String) typeCombo.getSelectedItem();
        try {
            // block overlapping requests (rejected ones don't count)
            for (LeaveRecord l : leaveDAO.getLeavesByEmployee(emp.getEmployeeId())) {
                if ("REJECTED".equals(l.getStatus())) continue;
                LocalDate ls = LocalDate.parse(l.getStartDate());
                LocalDate le = LocalDate.parse(l.getEndDate());
                if (!start.isAfter(le) && !end.isBefore(ls)) {
                    warn("These dates overlap an existing " + l.getStatus() + " " + l.getLeaveType()
                            + " leave (" + ls + " to " + le + ").", "Overlapping Leave");
                    return;
                }
            }

            LeaveRecord rec = new LeaveRecord(emp.getEmployeeId(), start.toString(), end.toString(),
                    type, reason.isEmpty() ? null : reason);
            if (leaveDAO.fileLeave(rec)) {
                long days = ChronoUnit.DAYS.between(start, end) + 1;
                JOptionPane.showMessageDialog(this,
                        type + " leave filed for " + emp.getFirstName() + " " + emp.getLastName()
                        + "\n" + start + " to " + end + " (" + days + " day" + (days == 1 ? "" : "s") + ")"
                        + "\nStatus: PENDING",
                        "Request Filed", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                statusFilter.setSelectedIndex(0);
                refreshTable();
            }
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    private void review(String newStatus) {
        int row = selectedModelRow();
        if (row < 0) return;
        if (!"PENDING".equals(tableModel.getValueAt(row, STATUS_COL))) {
            warn("Only PENDING requests can be approved or rejected.\nThis one is already "
                    + tableModel.getValueAt(row, STATUS_COL) + ".", "Already Reviewed");
            return;
        }

        User reviewer = SessionManager.getCurrentUser();
        if (reviewer == null) {
            warn("You must be logged in to review requests.", "Not Logged In");
            return;
        }

        int leaveId = (Integer) tableModel.getValueAt(row, 0);
        String verb = "APPROVED".equals(newStatus) ? "Approve" : "Reject";
        int choice = JOptionPane.showConfirmDialog(this,
                verb + " " + tableModel.getValueAt(row, 6) + " leave for "
                + tableModel.getValueAt(row, 2) + "?\n"
                + tableModel.getValueAt(row, 4) + " to " + tableModel.getValueAt(row, 5),
                "Confirm " + verb, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        try {
            if (leaveDAO.updateStatus(leaveId, newStatus, reviewer.getUserId())) {
                refreshTable();
            }
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    private void cancelRequest() {
        int row = selectedModelRow();
        if (row < 0) return;
        if (!"PENDING".equals(tableModel.getValueAt(row, STATUS_COL))) {
            warn("Only PENDING requests can be cancelled.", "Cannot Cancel");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Cancel (delete) this request for " + tableModel.getValueAt(row, 2) + "?",
                "Confirm Cancel", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        try {
            if (leaveDAO.deleteLeave((Integer) tableModel.getValueAt(row, 0))) {
                refreshTable();
            }
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    /** Model row of the selected table row, or -1 after showing a message. */
    private int selectedModelRow() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            warn("Select a leave request in the table first.", "No Request Selected");
            return -1;
        }
        return table.convertRowIndexToModel(viewRow);
    }

    // ----------------------------------------------------------- helpers

    private void warn(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
    }

    private void showDbError(SQLException ex) {
        JOptionPane.showMessageDialog(this,
                "Database error:\n" + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace();
    }

    /** Colors the Status column: pending = orange, approved = green, rejected = red. */
    private static class StatusRenderer extends DefaultTableCellRenderer {
        StatusRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                String s = String.valueOf(value);
                if ("PENDING".equals(s)) c.setForeground(new Color(200, 110, 0));
                else if ("APPROVED".equals(s)) c.setForeground(new Color(0, 128, 0));
                else if ("REJECTED".equals(s)) c.setForeground(new Color(190, 30, 30));
                else c.setForeground(Color.BLACK);
            }
            return c;
        }
    }
}
