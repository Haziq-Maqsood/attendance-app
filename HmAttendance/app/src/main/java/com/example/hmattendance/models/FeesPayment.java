// models/FeesPayment.java
package com.example.hmattendance.models;

public class FeesPayment {
    private int id; // Primary key for the payment record
    private String studentId;
    private String feeType;
    private double amount; // Assuming fees have an amount
    private String paymentDate; // YYYY-MM-DD
    private String timestamp;   // YYYY-MM-DD HH:MM:SS
    private String clubName;    // To link payment to a club

    // Constructor for creating new payment records (ID will be auto-generated)
    public FeesPayment(String studentId, String feeType, double amount, String paymentDate, String timestamp, String clubName) {
        this.studentId = studentId;
        this.feeType = feeType;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.timestamp = timestamp;
        this.clubName = clubName;
    }

    // Constructor for retrieving from database (includes ID)
    public FeesPayment(int id, String studentId, String feeType, double amount, String paymentDate, String timestamp, String clubName) {
        this.id = id;
        this.studentId = studentId;
        this.feeType = feeType;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.timestamp = timestamp;
        this.clubName = clubName;
    }

    // Getters
    public int getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getFeeType() { return feeType; }
    public double getAmount() { return amount; }
    public String getPaymentDate() { return paymentDate; }
    public String getTimestamp() { return timestamp; }
    public String getClubName() { return clubName; }

    // Setters (if needed, e.g., if you plan to update records)
    public void setId(int id) { this.id = id; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setFeeType(String feeType) { this.feeType = feeType; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public void setClubName(String clubName) { this.clubName = clubName; }

    @Override
    public String toString() {
        return "FeesPayment{" +
                "id=" + id +
                ", studentId='" + studentId + '\'' +
                ", feeType='" + feeType + '\'' +
                ", amount=" + amount +
                ", paymentDate='" + paymentDate + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", clubName='" + clubName + '\'' +
                '}';
    }
}