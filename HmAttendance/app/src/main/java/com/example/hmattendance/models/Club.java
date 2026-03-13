package com.example.hmattendance.models;

public class Club {
    private String clubName; // Primary Key
    private String instructorName;
    private String email;    // NOT NULL
    private String password; // NOT NULL
    private String initials; // TEXT

    // Constructor for creating a new Club
    public Club(String clubName, String instructorName, String email, String password, String initials) {
        this.clubName = clubName;
        this.instructorName = instructorName;
        this.email = email;
        this.password = password;
        this.initials = initials;
    }

    // Getters
    public String getClubName() {
        return clubName;
    }

    public String getInstructorName() {
        return instructorName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getInitials() {
        return initials;
    }

    // Setters (if you need to modify fields after object creation, e.g., for updates)
    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    public void setInstructorName(String instructorName) {
        this.instructorName = instructorName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setInitials(String initials) {
        this.initials = initials;
    }
}