package com.attendance.model;

public class LeaveRecord {
    private int leaveId;
    private int employeeId;
    private String startDate;
    private String endDate;
    private String leaveType;
    private String status; 
    private String reason;
    private Integer reviewedBy; // int to integer to hold null values - naka NULL leaverecord.reviewedby

    public LeaveRecord() {}

    public LeaveRecord(int leaveId, int employeeId, String startDate, String endDate, 
                       String leaveType, String status, String reason, int reviewedBy) {
        this.leaveId = leaveId;
        this.employeeId = employeeId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.leaveType = leaveType;
        this.status = status;
        this.reason = reason;
        this.reviewedBy = reviewedBy;
    }
    
    // Constructor for filing a NEW leave request
    public LeaveRecord(int employeeId, String startDate, String endDate, 
                       String leaveType, String reason) {
        this.employeeId = employeeId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.leaveType = leaveType;
        this.status = "PENDING";
        this.reason = reason;
        this.reviewedBy = null;
    }
    
    public int getLeaveId() { return leaveId; }
    public void setLeaveId(int leaveId) { this.leaveId = leaveId; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public int getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(int reviewedBy) { this.reviewedBy = reviewedBy; }
}