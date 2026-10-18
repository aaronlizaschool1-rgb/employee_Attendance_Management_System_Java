package com.attendance.ui;

import com.attendance.dao.AttendanceDAO;
import com.attendance.dao.EmployeeDAO;
import com.attendance.model.AttendanceLog;
import com.attendance.model.Employee;
import com.attendance.model.User;
import com.attendance.util.AppTheme;
import com.attendance.util.SessionManager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Daily Attendance Kiosk and History Viewer.
 * - Clock In / Clock Out by employee ID
 * - Administrators and Managers can log attendance for any employee ID
 * - Regular EMPLOYEES are locked exclusively to their own linked employee ID
 * - JTable of a day's attendance (loaded from AttendanceDAO.getDailyAttendanceReport)
 * - Date and status filters
 */
public class AttendanceFrame extends JFrame {

    // Minutes after shift start that still count as ON_TIME.
    private static final int GRACE_MINUTES = 0;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("EEEE, MMM dd yyyy   HH:mm:ss");
    private static final String[] COLUMNS = {
        "Log ID", "Emp ID", "Employee", "Department", "Date", "Time In", "Time Out", "Status"
    };

    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final boolean canManageAll = SessionManager.isAdmin() || SessionManager.isManager();

    private JTextField employeeIdField;
    private JButton clockInBtn;
    private JButton clockOutBtn;
    private JButton clearBtn;
    private JTextField dateField;
    private JComboBox<String> statusFilter;
    private JLabel clockLabel;
    private JLabel countLabel;
    private JTable table;
    private DefaultTableModel tableModel;
    private Timer clockTimer;

    public AttendanceFrame() {
        AppTheme.brand(this, "Attendance");
        setSize(1000, 620);
        setMinimumSize(new Dimension(900, 520));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        buildUI();
        applyRoleRestrictions();
        startClock();
        loadTable();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (clockTimer != null) {
                    clockTimer.stop();
                }
            }
        });
    }

    // ------------------------------------------------------------------ UI

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setContentPane(root);

        // ---- Header (title + live clock)
        JLabel title = new JLabel("Attendance Kiosk");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));

        clockLabel = new JLabel("", SwingConstants.RIGHT);
        clockLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPanel header = new JPanel(new BorderLayout());
        header.add(title, BorderLayout.WEST);
        header.add(clockLabel, BorderLayout.EAST);

        // ---- Kiosk panel (clock in / out)
        employeeIdField = new JTextField(10);
        clockInBtn = new JButton("CLOCK IN");
        clockOutBtn = new JButton("CLOCK OUT");
        clearBtn = new JButton("CLEAR");

        clockInBtn.addActionListener(e -> clockIn());
        clockOutBtn.addActionListener(e -> clockOut());
        clearBtn.addActionListener(e -> {
            if (canManageAll) {
                employeeIdField.setText("");
                employeeIdField.requestFocusInWindow();
            }
        });

        JPanel kioskPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        kioskPanel.setBorder(BorderFactory.createTitledBorder("Clock In / Clock Out"));
        kioskPanel.add(new JLabel("Employee ID:"));
        kioskPanel.add(employeeIdField);
        kioskPanel.add(clockInBtn);
        kioskPanel.add(clockOutBtn);
        kioskPanel.add(clearBtn);

        // ---- Filter panel
        dateField = new JTextField(LocalDate.now().toString(), 10);
        statusFilter = new JComboBox<>(new String[]{"ALL", "ON_TIME", "LATE", "UNDERTIME", "HALF_DAY"});
        JButton loadBtn = new JButton("LOAD");
        JButton todayBtn = new JButton("TODAY");
        countLabel = new JLabel(" ");

        loadBtn.addActionListener(e -> loadTable());
        todayBtn.addActionListener(e -> {
            dateField.setText(LocalDate.now().toString());
            loadTable();
        });
        statusFilter.addActionListener(e -> loadTable());

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filters"));
        filterPanel.add(new JLabel("Date (yyyy-MM-dd):"));
        filterPanel.add(dateField);
        filterPanel.add(loadBtn);
        filterPanel.add(todayBtn);
        filterPanel.add(new JLabel("Status:"));
        filterPanel.add(statusFilter);
        filterPanel.add(countLabel);

        // ---- North stack
        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(header);
        north.add(Box.createVerticalStrut(8));
        north.add(kioskPanel);
        north.add(Box.createVerticalStrut(6));
        north.add(filterPanel);
        root.add(north, BorderLayout.NORTH);

        // ---- Table
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(7).setCellRenderer(new StatusRenderer());
        root.add(new JScrollPane(table), BorderLayout.CENTER);

        // ---- Footer
        JButton backBtn = new JButton("BACK TO DASHBOARD");
        backBtn.addActionListener(e -> {
            new DashboardFrame().setVisible(true);
            dispose();
        });
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        footer.add(backBtn);
        root.add(footer, BorderLayout.SOUTH);
    }

    /**
     * Applies permission locks: if standard employee, auto-fill and lock their ID.
     */
    private void applyRoleRestrictions() {
        if (!canManageAll) {
            User currentUser = SessionManager.getCurrentUser();
            Integer loggedInEmpId = (currentUser != null) ? currentUser.getEmployeeId() : null;

            if (loggedInEmpId != null && loggedInEmpId > 0) {
                employeeIdField.setText(String.valueOf(loggedInEmpId));
                employeeIdField.setEditable(false);
                employeeIdField.setEnabled(false);
                clearBtn.setEnabled(false);
            } else {
                employeeIdField.setEditable(false);
                employeeIdField.setEnabled(false);
                clockInBtn.setEnabled(false);
                clockOutBtn.setEnabled(false);
                clearBtn.setEnabled(false);
                JOptionPane.showMessageDialog(this,
                        "Your user account is not linked to an active Employee profile.\nPlease contact an Administrator.",
                        "Account Unlinked", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void startClock() {
        updateClock();
        clockTimer = new Timer(1000, e -> updateClock());
        clockTimer.start();
    }

    private void updateClock() {
        clockLabel.setText(java.time.LocalDateTime.now().format(CLOCK_FMT));
    }

    // ------------------------------------------------------------- Actions

    private void clockIn() {
        Employee emp = validateEmployee();
        if (emp == null) {
            return;
        }

        String today = LocalDate.now().toString();
        try {
            if (attendanceDAO.hasClockedInToday(emp.getEmployeeId(), today)) {
                JOptionPane.showMessageDialog(this,
                        emp.getFirstName() + " " + emp.getLastName() + " already clocked in today.",
                        "Duplicate Clock-In", JOptionPane.WARNING_MESSAGE);
                return;
            }

            LocalTime now = LocalTime.now().withNano(0);
            LocalTime shiftStart = LocalTime.parse(emp.getShiftStart());
            String status = now.isAfter(shiftStart.plusMinutes(GRACE_MINUTES)) ? "LATE" : "ON_TIME";

            if (attendanceDAO.recordClockIn(emp.getEmployeeId(), today, now.format(TIME_FMT), status)) {
                JOptionPane.showMessageDialog(this,
                        "Clocked in: " + emp.getFirstName() + " " + emp.getLastName()
                        + "\nTime: " + now.format(TIME_FMT) + "\nStatus: " + status,
                        "Clock-In Successful", JOptionPane.INFORMATION_MESSAGE);
                afterClockAction();
            } else {
                JOptionPane.showMessageDialog(this, "Clock-in was not saved. Please try again.",
                        "Clock-In Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                    "This employee's shift start time is not in a valid format: " + emp.getShiftStart(),
                    "Invalid Shift Data", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    private void clockOut() {
        Employee emp = validateEmployee();
        if (emp == null) {
            return;
        }

        String today = LocalDate.now().toString();
        try {
            AttendanceLog todayLog = null;
            for (AttendanceLog log : attendanceDAO.getLogsByEmployee(emp.getEmployeeId())) {
                if (today.equals(log.getLogDate())) {
                    todayLog = log;
                    break;
                }
            }

            if (todayLog == null) {
                JOptionPane.showMessageDialog(this,
                        emp.getFirstName() + " " + emp.getLastName() + " has not clocked in today.",
                        "No Clock-In Found", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (todayLog.getTimeOut() != null) {
                JOptionPane.showMessageDialog(this,
                        emp.getFirstName() + " " + emp.getLastName() + " already clocked out at "
                        + todayLog.getTimeOut() + ".",
                        "Already Clocked Out", JOptionPane.WARNING_MESSAGE);
                return;
            }

            LocalTime now = LocalTime.now().withNano(0);
            LocalTime shiftEnd = LocalTime.parse(emp.getShiftEnd());
            if (now.isBefore(shiftEnd)) {
                int choice = JOptionPane.showConfirmDialog(this,
                        "Shift ends at " + shiftEnd + ". Clocking out now counts as undertime.\nContinue?",
                        "Early Clock-Out", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (choice != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            if (attendanceDAO.recordClockOut(emp.getEmployeeId(), today, now.format(TIME_FMT))) {
                JOptionPane.showMessageDialog(this,
                        "Clocked out: " + emp.getFirstName() + " " + emp.getLastName()
                        + "\nTime: " + now.format(TIME_FMT),
                        "Clock-Out Successful", JOptionPane.INFORMATION_MESSAGE);
                afterClockAction();
            } else {
                JOptionPane.showMessageDialog(this, "Clock-out was not saved. Please try again.",
                        "Clock-Out Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                    "This employee's shift end time is not in a valid format: " + emp.getShiftEnd(),
                    "Invalid Shift Data", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    private void afterClockAction() {
        if (canManageAll) {
            employeeIdField.setText("");
            employeeIdField.requestFocusInWindow();
        }
        dateField.setText(LocalDate.now().toString());
        statusFilter.setSelectedItem("ALL");
        loadTable();
    }

    /** Loads the table for the date in dateField, applying the status filter. */
    private void loadTable() {
        String dateText = dateField.getText().trim();
        try {
            LocalDate.parse(dateText);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                    "Please enter the date as yyyy-MM-dd, for example " + LocalDate.now() + ".",
                    "Invalid Date", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            List<Object[]> rows = attendanceDAO.getDailyAttendanceReport(dateText);

            Map<Integer, Employee> employees = new HashMap<>();
            for (Employee e : employeeDAO.getAllEmployees()) {
                employees.put(e.getEmployeeId(), e);
            }

            String filter = (String) statusFilter.getSelectedItem();
            tableModel.setRowCount(0);

            for (Object[] row : rows) {
                String status = String.valueOf(row[7]);
                String timeOut = String.valueOf(row[6]);
                Employee emp = employees.get((Integer) row[1]);

                if ("ON_TIME".equals(status) && emp != null && !timeOut.startsWith("--")) {
                    try {
                        if (LocalTime.parse(timeOut).isBefore(LocalTime.parse(emp.getShiftEnd()))) {
                            status = "UNDERTIME";
                        }
                    } catch (DateTimeParseException ignored) {
                        // keep the stored status if time parsing fails
                    }
                }

                if ("ALL".equals(filter) || filter.equals(status)) {
                    row[7] = status;
                    tableModel.addRow(row);
                }
            }
            countLabel.setText("Records: " + tableModel.getRowCount());
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    // ------------------------------------------------------------- Helpers

    /** Validates the ID field and returns the ACTIVE employee, enforcing ownership if regular employee. */
    private Employee validateEmployee() {
        String idText = employeeIdField.getText().trim();

        if (idText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an Employee ID.",
                    "Missing Input", JOptionPane.WARNING_MESSAGE);
            employeeIdField.requestFocusInWindow();
            return null;
        }
        if (!idText.matches("\\d{1,9}")) {
            JOptionPane.showMessageDialog(this, "Employee ID must be a number.",
                    "Invalid Input", JOptionPane.WARNING_MESSAGE);
            employeeIdField.requestFocusInWindow();
            return null;
        }

        int enteredId = Integer.parseInt(idText);

        // Security check: Standard employees must only log their own ID
        if (!canManageAll) {
            User currentUser = SessionManager.getCurrentUser();
            Integer loggedInEmpId = (currentUser != null) ? currentUser.getEmployeeId() : null;
            if (loggedInEmpId == null || enteredId != loggedInEmpId) {
                JOptionPane.showMessageDialog(this,
                        "Unauthorized: You can only clock in or out for yourself.",
                        "Access Denied", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }

        try {
            Employee emp = employeeDAO.getEmployeeById(enteredId);
            if (emp == null) {
                JOptionPane.showMessageDialog(this, "No employee found with ID " + idText + ".",
                        "Employee Not Found", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            if (!"ACTIVE".equalsIgnoreCase(emp.getStatus())) {
                JOptionPane.showMessageDialog(this,
                        emp.getFirstName() + " " + emp.getLastName() + " is " + emp.getStatus()
                        + " and cannot clock in or out.",
                        "Employee Not Active", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            return emp;
        } catch (SQLException ex) {
            showDbError(ex);
            return null;
        }
    }

    private void showDbError(SQLException ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this,
                "A database error occurred:\n" + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Colors the Status column text so late and undertime rows stand out. */
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
                if ("ON_TIME".equals(s)) {
                    c.setForeground(new Color(0, 128, 0));
                } else if ("LATE".equals(s)) {
                    c.setForeground(new Color(200, 80, 0));
                } else if ("UNDERTIME".equals(s) || "HALF_DAY".equals(s)) {
                    c.setForeground(new Color(180, 130, 0));
                } else {
                    c.setForeground(Color.BLACK);
                }
            }
            return c;
        }
    }
}