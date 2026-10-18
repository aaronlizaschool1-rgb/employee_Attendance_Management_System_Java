package com.attendance.model;

public class User {
    private int userId;
    private Integer employeeId;
    private String username;
    private String password;
    private String fullName;
    private String role;
    private String status;

    public User() {}
    // Constructor for read Only
    public User(int userId,Integer employeeId, String username, String password, String fullName, String role, String status) {
        this.userId = userId;
        this.employeeId = employeeId;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
    }
    
    // Constructor for inserting new user - auto increment
    public User(Integer employeeId,String username, String password, String fullName, String role, String status) {
        this.employeeId = employeeId;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
    }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public Integer getEmployeeId(){ return employeeId; } // since the schema changed and employeeid became a foreign key, model layer should change
    public void setEmployeeId(Integer employeeId){ this.employeeId = employeeId;}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    @Override
    public String toString() {
        return fullName + " (" + role + ")";
    }
}