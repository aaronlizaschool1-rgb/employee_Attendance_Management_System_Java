package com.attendance.model;

public class Employee {
    private int employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private int departmentId;
    private String jobTitle;
    private String shiftStart;
    private String shiftEnd;
    private String status;

    public Employee() {}

    public Employee(int employeeId, String firstName, String lastName, String email, 
                    int departmentId, String jobTitle, String shiftStart, String shiftEnd, String status) {
        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.departmentId = departmentId;
        this.jobTitle = jobTitle;
        this.shiftStart = shiftStart;
        this.shiftEnd = shiftEnd;
        this.status = status;
    }

        public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getDepartmentId() { return departmentId; }
    public void setDepartmentId(int departmentId) { this.departmentId = departmentId; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getShiftStart() { return shiftStart; }
    public void setShiftStart(String shiftStart) { this.shiftStart = shiftStart; }

    public String getShiftEnd() { return shiftEnd; }
    public void setShiftEnd(String shiftEnd) { this.shiftEnd = shiftEnd; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}