package com.attendance.model;

public class AttendanceLog {
    private int logId;
    private int employeeId;
    private String logDate;
    private String timeIn;
    private String timeOut;
    private String status; 

    public AttendanceLog() {}

    public AttendanceLog(int logId, int employeeId, String logDate, String timeIn, String timeOut, String status) {
        this.logId = logId;
        this.employeeId = employeeId;
        this.logDate = logDate;
        this.timeIn = timeIn;
        this.timeOut = timeOut;
        this.status = status;
    }

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getLogDate() { return logDate; }
    public void setLogDate(String logDate) { this.logDate = logDate; }

    public String getTimeIn() { return timeIn; }
    public void setTimeIn(String timeIn) { this.timeIn = timeIn; }

    public String getTimeOut() { return timeOut; }
    public void setTimeOut(String timeOut) { this.timeOut = timeOut; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}