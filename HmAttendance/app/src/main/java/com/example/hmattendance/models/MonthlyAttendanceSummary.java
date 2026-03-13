package com.example.hmattendance.models;

public class MonthlyAttendanceSummary {
    private String studentId;
    private String studentName; // Added for convenience in display
    private int totalPresentDays;
    private int totalAbsentDays;
    private int totalLateDays;
    private int totalExcusedDays;
    private int totalMarkedDays; // Total days attendance was recorded for this student in the month

    public MonthlyAttendanceSummary(String studentId, int totalPresentDays, int totalAbsentDays,
                                    int totalLateDays, int totalExcusedDays, int totalMarkedDays) {
        this.studentId = studentId;
        this.totalPresentDays = totalPresentDays;
        this.totalAbsentDays = totalAbsentDays;
        this.totalLateDays = totalLateDays;
        this.totalExcusedDays = totalExcusedDays;
        this.totalMarkedDays = totalMarkedDays;
        // studentName will be set after fetching from StudentDbHelper
        this.studentName = "";
    }

    // Constructor with student name, useful if you join directly in query (advanced)
    public MonthlyAttendanceSummary(String studentId, String studentName, int totalPresentDays, int totalAbsentDays,
                                    int totalLateDays, int totalExcusedDays, int totalMarkedDays) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.totalPresentDays = totalPresentDays;
        this.totalAbsentDays = totalAbsentDays;
        this.totalLateDays = totalLateDays;
        this.totalExcusedDays = totalExcusedDays;
        this.totalMarkedDays = totalMarkedDays;
    }

    // Getters
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public int getTotalPresentDays() { return totalPresentDays; }
    public int getTotalAbsentDays() { return totalAbsentDays; }
    public int getTotalLateDays() { return totalLateDays; }
    public int getTotalExcusedDays() { return totalExcusedDays; }
    public int getTotalMarkedDays() { return totalMarkedDays; }

    // Setter for studentName (if you fetch it separately)
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
}