package com.attendance.model;

public class Department {
    private int departmentId;
    private String departmentName;

    public Department() {}

    public Department(int departmentId, String departmentName) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
    }
    //DEPARTMENT ID
    public int getDepartmentId() {
        return departmentId; 
    }
    public void setDepartmentId(int departmentId) { 
        this.departmentId = departmentId; 
    }
    
    //DEPARTMENT NAME
    public String getDepartmentName() {
        return departmentName; 
    }
    public void setDepartmentName(String departmentName) { 
        this.departmentName = departmentName; 
    }
    
    @Override
    public String toString() {
        return departmentName;
    }
}