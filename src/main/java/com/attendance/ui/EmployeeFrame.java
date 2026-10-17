package com.attendance.ui;

import com.attendance.dao.DepartmentDAO;
import com.attendance.dao.EmployeeDAO;
import com.attendance.dao.UserDAO;
import com.attendance.model.Department;
import com.attendance.model.Employee;
import com.attendance.model.User;
import com.attendance.util.AppTheme;
import com.attendance.util.SessionManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Employee management screen: table + live search + add/update/delete form.
 * ADMINISTRATOR and MANAGER can edit; EMPLOYEE role is view-only.
 */
public class EmployeeFrame extends JFrame {

    private static final String[] COLUMNS = {
        "ID", "First Name", "Last Name", "Email", "Department",
        "Job Title", "Shift Start", "Shift End", "Status"
    };
    private static final String[] STATUSES = {"ACTIVE", "INACTIVE", "TERMINATED"};

    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L} .'-]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    // accepts 9:00, 09:00 or 09:00:00
    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]?\\d|2[0-3]):([0-5]\\d)(:([0-5]\\d))?$");

    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final UserDAO userDAO = new UserDAO();
    private final Map<Integer, String> deptNames = new HashMap<>();
    private List<Employee> currentEmployees = new ArrayList<>();

    // search
    private final JTextField searchField = new JTextField(25);
    private final JButton showAllBtn = new JButton("Show All");
    private final JLabel countLabel = new JLabel(" ");
    private final Timer searchTimer;

    // table
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override public Class<?> getColumnClass(int c) { return c == 0 ? Integer.class : String.class; }
    };
    private final JTable table = new JTable(tableModel);

    // form
    private final JTextField idField = new JTextField(8);
    private final JTextField firstNameField = new JTextField(16);
    private final JTextField lastNameField = new JTextField(16);
    private final JTextField emailField = new JTextField(16);
    private final JTextField jobTitleField = new JTextField(16);
    private final JTextField shiftStartField = new JTextField("09:00", 8);
    private final JTextField shiftEndField = new JTextField("18:00", 8);
    private final JComboBox<Department> deptCombo = new JComboBox<>();
    private final JComboBox<String> statusCombo = new JComboBox<>(STATUSES);

    private final JButton saveBtn = new JButton("SAVE");
    private final JButton updateBtn = new JButton("UPDATE");
    private final JButton deleteBtn = new JButton("DELETE");
    private final JButton clearBtn = new JButton("CLEAR");

    public EmployeeFrame() {
        AppTheme.brand(this, "Employees");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1050, 680);
        setMinimumSize(new Dimension(900, 600));

        // wait 250 ms after the last keystroke before querying the database
        searchTimer = new Timer(250, e -> refreshTable());
        searchTimer.setRepeats(false);

        buildUI();
        wireEvents();
        applyPermissions();
        loadDepartments();
        refreshTable();
        setLocationRelativeTo(null);
    }

    // ---------------------------------------------------------------- UI

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // top: search bar
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.add(new JLabel("Search (ID, name or job title):"));
        top.add(searchField);
        top.add(showAllBtn);
        top.add(Box.createHorizontalStrut(20));
        top.add(countLabel);
        root.add(top, BorderLayout.NORTH);

        // center: table
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(3).setPreferredWidth(200);
        root.add(new JScrollPane(table), BorderLayout.CENTER);

        // bottom: form + buttons
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Employee Details"));
        idField.setEditable(false);

        addField(form, 0, 0, "Employee ID:", idField);
        addField(form, 1, 0, "Status:", statusCombo);
        addField(form, 0, 1, "First Name:", firstNameField);
        addField(form, 1, 1, "Last Name:", lastNameField);
        addField(form, 0, 2, "Email:", emailField);
        addField(form, 1, 2, "Department:", deptCombo);
        addField(form, 0, 3, "Job Title:", jobTitleField);
        addField(form, 1, 3, "Shift Start (HH:mm):", shiftStartField);
        addField(form, 1, 4, "Shift End (HH:mm):", shiftEndField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        buttons.add(saveBtn);
        buttons.add(updateBtn);
        buttons.add(deleteBtn);
        buttons.add(clearBtn);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(form, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.SOUTH);
        root.add(bottom, BorderLayout.SOUTH);

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
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            if (row >= 0) {
                populateForm(table.convertRowIndexToModel(row));
            }
        });

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { searchTimer.restart(); }
            @Override public void removeUpdate(DocumentEvent e) { searchTimer.restart(); }
            @Override public void changedUpdate(DocumentEvent e) { searchTimer.restart(); }
        });

        showAllBtn.addActionListener(e -> searchField.setText(""));
        saveBtn.addActionListener(e -> saveEmployee());
        updateBtn.addActionListener(e -> updateEmployee());
        deleteBtn.addActionListener(e -> deleteEmployee());
        clearBtn.addActionListener(e -> clearForm());
    }

    private void applyPermissions() {
        boolean canEdit = SessionManager.isAdmin() || SessionManager.isManager();
        saveBtn.setEnabled(canEdit);
        updateBtn.setEnabled(canEdit);
        deleteBtn.setEnabled(canEdit);
        if (!canEdit) {
            setTitle(getTitle() + " (view only)");
        }
    }

    // -------------------------------------------------------------- data

    private void loadDepartments() {
        try {
            List<Department> depts = departmentDAO.getAllDepartments();
            deptCombo.removeAllItems();
            deptNames.clear();
            for (Department d : depts) {
                deptCombo.addItem(d); // shows the department name via toString()
                deptNames.put(d.getDepartmentId(), d.getDepartmentName());
            }
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    private void refreshTable() {
        try {
            currentEmployees = employeeDAO.searchEmployees(searchField.getText());
            tableModel.setRowCount(0);
            for (Employee e : currentEmployees) {
                tableModel.addRow(new Object[]{
                    e.getEmployeeId(),
                    e.getFirstName(),
                    e.getLastName(),
                    e.getEmail(),
                    deptNames.getOrDefault(e.getDepartmentId(), "Unknown"),
                    e.getJobTitle(),
                    shortTime(e.getShiftStart()),
                    shortTime(e.getShiftEnd()),
                    e.getStatus()
                });
            }
            countLabel.setText(currentEmployees.size() + " employee(s)");
        } catch (NumberFormatException ex) {
            // a very long number typed in the search box
            currentEmployees = new ArrayList<>();
            tableModel.setRowCount(0);
            countLabel.setText("0 employee(s)");
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    private void populateForm(int index) {
        if (index < 0 || index >= currentEmployees.size()) return;
        Employee e = currentEmployees.get(index);
        idField.setText(String.valueOf(e.getEmployeeId()));
        firstNameField.setText(e.getFirstName());
        lastNameField.setText(e.getLastName());
        emailField.setText(e.getEmail());
        jobTitleField.setText(e.getJobTitle());
        shiftStartField.setText(shortTime(e.getShiftStart()));
        shiftEndField.setText(shortTime(e.getShiftEnd()));
        statusCombo.setSelectedItem(e.getStatus());
        for (int i = 0; i < deptCombo.getItemCount(); i++) {
            if (deptCombo.getItemAt(i).getDepartmentId() == e.getDepartmentId()) {
                deptCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    private void clearForm() {
        idField.setText("");
        firstNameField.setText("");
        lastNameField.setText("");
        emailField.setText("");
        jobTitleField.setText("");
        shiftStartField.setText("09:00");
        shiftEndField.setText("18:00");
        statusCombo.setSelectedIndex(0);
        if (deptCombo.getItemCount() > 0) deptCombo.setSelectedIndex(0);
        table.clearSelection();
        firstNameField.requestFocus();
    }

    // ----------------------------------------------------------- actions

    private void saveEmployee() {
        if (!idField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "An existing employee is selected.\nUse UPDATE to change them, or CLEAR to add a new one.",
                    "Employee Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Employee emp = readAndValidateForm();
        if (emp == null) return;

        // popup: ask for this employee's login username and password
        AccountDialog dlg = new AccountDialog(this, emp, userDAO, SessionManager.isAdmin());
        dlg.setVisible(true);
        if (dlg.isCancelled()) return; // nothing is saved

        // save the employee first (a duplicate email is the most likely failure)
        try {
            if (!employeeDAO.addEmployee(emp)) return;
        } catch (SQLException ex) {
            handleSaveError(ex);
            return;
        }

        // then create the login account
        String message = "Employee added successfully.";
        User account = dlg.getAccount();
        if (account != null) {
            try {
                if (userDAO.addUser(account)) {
                    message += "\n\nLogin account created:\nUsername: " + account.getUsername()
                            + "\nRole: " + account.getRole();
                } else {
                    message += "\n\nWARNING: the login account was not created.";
                }
            } catch (SQLException ex) {
                message += "\n\nWARNING: the employee was saved but the login account could not be created:\n"
                        + ex.getMessage();
            }
        }
        JOptionPane.showMessageDialog(this, message);
        clearForm();
        refreshTable();
    }

    private void updateEmployee() {
        if (idField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Select an employee from the table first.",
                    "No Employee Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Employee emp = readAndValidateForm();
        if (emp == null) return;
        emp.setEmployeeId(Integer.parseInt(idField.getText()));

        try {
            if (employeeDAO.updateEmployee(emp)) {
                JOptionPane.showMessageDialog(this, "Employee updated successfully.");
                clearForm();
                refreshTable();
            }
        } catch (SQLException ex) {
            handleSaveError(ex);
        }
    }

    private void deleteEmployee() {
        if (idField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Select an employee from the table first.",
                    "No Employee Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String name = firstNameField.getText() + " " + lastNameField.getText();
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete " + name + "?\n\n"
                + "Their attendance logs and leave records will also be permanently deleted.\n"
                + "Tip: set the status to INACTIVE instead if you only want to deactivate them.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        try {
            if (employeeDAO.deleteEmployee(Integer.parseInt(idField.getText()))) {
                JOptionPane.showMessageDialog(this, "Employee deleted.");
                clearForm();
                refreshTable();
            }
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    // -------------------------------------------------------- validation

    /** Returns a validated Employee, or null after showing a message. */
    private Employee readAndValidateForm() {
        String first = firstNameField.getText().trim();
        String last = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String job = jobTitleField.getText().trim();

        if (first.isEmpty()) return fail("First name is required.", firstNameField);
        if (first.length() > 50) return fail("First name must be 50 characters or fewer.", firstNameField);
        if (!NAME_PATTERN.matcher(first).matches())
            return fail("First name may only contain letters, spaces, . ' and -", firstNameField);

        if (last.isEmpty()) return fail("Last name is required.", lastNameField);
        if (last.length() > 50) return fail("Last name must be 50 characters or fewer.", lastNameField);
        if (!NAME_PATTERN.matcher(last).matches())
            return fail("Last name may only contain letters, spaces, . ' and -", lastNameField);

        if (email.isEmpty()) return fail("Email is required.", emailField);
        if (email.length() > 100) return fail("Email must be 100 characters or fewer.", emailField);
        if (!EMAIL_PATTERN.matcher(email).matches())
            return fail("Please enter a valid email address (e.g. name@company.com).", emailField);

        Department dept = (Department) deptCombo.getSelectedItem();
        if (dept == null) return fail("Please select a department.", deptCombo);

        if (job.isEmpty()) return fail("Job title is required.", jobTitleField);
        if (job.length() > 50) return fail("Job title must be 50 characters or fewer.", jobTitleField);

        String start = normalizeTime(shiftStartField.getText());
        if (start == null) return fail("Shift start must be a time like 09:00 (24-hour).", shiftStartField);
        String end = normalizeTime(shiftEndField.getText());
        if (end == null) return fail("Shift end must be a time like 18:00 (24-hour).", shiftEndField);
        if (end.compareTo(start) <= 0)
            return fail("Shift end must be later than shift start.", shiftEndField);

        String status = (String) statusCombo.getSelectedItem();
        return new Employee(first, last, email, dept.getDepartmentId(), job, start, end, status);
    }

    private Employee fail(String message, JComponent focus) {
        JOptionPane.showMessageDialog(this, message, "Invalid Input", JOptionPane.WARNING_MESSAGE);
        focus.requestFocus();
        return null;
    }

    /** "9:00" / "09:00" / "09:00:00" -> "09:00:00", or null if invalid. */
    private static String normalizeTime(String raw) {
        Matcher m = TIME_PATTERN.matcher(raw.trim());
        if (!m.matches()) return null;
        int hour = Integer.parseInt(m.group(1));
        String sec = (m.group(4) == null) ? "00" : m.group(4);
        return String.format("%02d:%s:%s", hour, m.group(2), sec);
    }

    private static String shortTime(String t) {
        return (t != null && t.length() >= 5) ? t.substring(0, 5) : t;
    }

    // ------------------------------------------------------------ errors

    private void handleSaveError(SQLException ex) {
        if ("23505".equals(ex.getSQLState())) { // unique constraint
            JOptionPane.showMessageDialog(this,
                    "That email address is already used by another employee.",
                    "Duplicate Email", JOptionPane.ERROR_MESSAGE);
            emailField.requestFocus();
        } else if ("22001".equals(ex.getSQLState())) { // value too long
            JOptionPane.showMessageDialog(this,
                    "One of the values is too long for the database.",
                    "Value Too Long", JOptionPane.ERROR_MESSAGE);
        } else {
            showDbError(ex);
        }
    }

    private void showDbError(SQLException ex) {
        JOptionPane.showMessageDialog(this,
                "Database error:\n" + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace();
    }

    // ------------------------------------------------- account popup

    /** Modal popup asking for the new employee's login username and password. */
    private static class AccountDialog extends JDialog {

        private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9._]{3,50}$");

        private final UserDAO userDAO;
        private final Employee emp;

        private final JTextField usernameField = new JTextField(18);
        private final JPasswordField passwordField = new JPasswordField(18);
        private final JPasswordField confirmField = new JPasswordField(18);
        private final JComboBox<String> roleCombo =
                new JComboBox<>(new String[]{"EMPLOYEE", "MANAGER", "ADMINISTRATOR"});
        private final JLabel errorLabel = new JLabel(" ");

        private boolean cancelled = true; // closing the window counts as cancel
        private User account = null;     // set when the account is created

        AccountDialog(JFrame owner, Employee emp, UserDAO userDAO, boolean canPickRole) {
            super(owner, "Create Login Account", true);
            this.emp = emp;
            this.userDAO = userDAO;

            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setResizable(false);

            // suggest a username like the seed accounts: first initial + last name
            String suggestion = (emp.getFirstName().substring(0, 1) + emp.getLastName())
                    .toLowerCase().replaceAll("[^a-z0-9]", "");
            usernameField.setText(suggestion);

            roleCombo.setEnabled(canPickRole); // only administrators can choose another role

            JPanel root = new JPanel(new BorderLayout(0, 10));
            root.setBorder(BorderFactory.createEmptyBorder(16, 20, 14, 20));

            JLabel heading = new JLabel("Login account for " + emp.getFirstName() + " " + emp.getLastName());
            heading.setFont(heading.getFont().deriveFont(Font.BOLD, 15f));
            JLabel note = new JLabel("Password at least 6 characters");
            note.setForeground(Color.GRAY);
            JPanel top = new JPanel(new GridLayout(2, 1, 0, 4));
            top.add(heading);
            top.add(note);
            root.add(top, BorderLayout.NORTH);

            JPanel form = new JPanel(new GridBagLayout());
            addRow(form, 0, "Username:", usernameField);
            addRow(form, 1, "Password:", passwordField);
            addRow(form, 2, "Confirm Password:", confirmField);
            addRow(form, 3, "Role:", roleCombo);
            errorLabel.setForeground(new Color(190, 30, 30));
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.gridy = 4;
            gc.gridwidth = 2;
            gc.anchor = GridBagConstraints.WEST;
            gc.insets = new Insets(6, 4, 0, 4);
            form.add(errorLabel, gc);

            JPanel center = new JPanel(new BorderLayout());
            center.add(form, BorderLayout.NORTH);
            root.add(center, BorderLayout.CENTER);

            JButton createBtn = new JButton("CREATE ACCOUNT");
            JButton cancelBtn = new JButton("CANCEL");
            createBtn.addActionListener(e -> createAccount());
            cancelBtn.addActionListener(e -> dispose());

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            buttons.add(createBtn);
            buttons.add(cancelBtn);
            root.add(buttons, BorderLayout.SOUTH);

            setContentPane(root);
            getRootPane().setDefaultButton(createBtn); // Enter = create
            pack();
            setLocationRelativeTo(owner);
        }

        private void addRow(JPanel p, int row, String label, JComponent comp) {
            GridBagConstraints gc = new GridBagConstraints();
            gc.insets = new Insets(5, 4, 5, 4);
            gc.gridy = row;
            gc.anchor = GridBagConstraints.WEST;
            gc.gridx = 0;
            p.add(new JLabel(label), gc);
            gc.gridx = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            p.add(comp, gc);
        }

        private void createAccount() {
            String username = usernameField.getText().trim().toLowerCase();
            String password = new String(passwordField.getPassword());
            String confirm = new String(confirmField.getPassword());

            if (!USERNAME_PATTERN.matcher(username).matches()) {
                showError("Username must be 3-50 characters: lowercase letters, numbers, . or _ only.");
                usernameField.requestFocus();
                return;
            }
            if (password.length() < 6 || password.length() > 50) {
                showError("Password must be 6 to 50 characters.");
                passwordField.requestFocus();
                return;
            }
            if (!password.equals(confirm)) {
                showError("The passwords do not match.");
                confirmField.setText("");
                confirmField.requestFocus();
                return;
            }

            try {
                if (userDAO.usernameExists(username)) {
                    showError("That username is already taken. Please choose another.");
                    usernameField.requestFocus();
                    return;
                }
            } catch (SQLException ex) {
                showError("Database error: " + ex.getMessage());
                return;
            }

            String fullName = emp.getFirstName() + " " + emp.getLastName();
            if (fullName.length() > 100) {
                fullName = fullName.substring(0, 100);
            }
            String status = "ACTIVE".equals(emp.getStatus()) ? "ACTIVE" : "INACTIVE";

            account = new User(username, password, fullName, (String) roleCombo.getSelectedItem(), status);
            cancelled = false;
            dispose();
        }

        private void showError(String message) {
            errorLabel.setText("<html>" + message + "</html>");
            pack();
        }

        boolean isCancelled() { return cancelled; }

        User getAccount() { return account; }
    }
}
