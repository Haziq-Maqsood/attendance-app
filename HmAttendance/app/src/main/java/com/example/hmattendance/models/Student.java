package com.example.hmattendance.models;

// Removed: import android.graphics.Bitmap; // No longer storing Bitmap directly in model

public class Student {
    private String id;
    private String imagePath; // CHANGED: Stores the path to the image file
    private String name;
    private String fatherName;
    private int age;
    private String admissionDate;
    private String gender;
    private String belt;
    private String email;
    private String phone;
    private String address;
    private String clubName;

    // Updated Constructor
    public Student(String id, String imagePath, String name, String fatherName, int age, // CHANGED: imagePath (String)
                   String admissionDate, String gender, String belt, String email,
                   String phone, String address, String clubName) {
        this.id = id;
        this.imagePath = imagePath; // Initialize imagePath
        this.name = name;
        this.fatherName = fatherName;
        this.age = age;
        this.admissionDate = admissionDate;
        this.gender = gender;
        this.belt = belt;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.clubName = clubName;
    }

    // Constructor without ID (for adding new students)
    public Student(String imagePath, String name, String fatherName, int age, // CHANGED: imagePath (String)
                   String admissionDate, String gender, String belt, String email,
                   String phone, String address, String clubName) {
        this(null, imagePath, name, fatherName, age, admissionDate, // Pass imagePath
                gender, belt, email, phone, address, clubName);
    }

    // Constructor for partial student data (e.g., list display where image not needed immediately)
    public Student(String id, String name, String clubName) {
        this.id = id;
        this.name = name;
        this.clubName = clubName;
        // Other fields implicitly null or default as they are not passed
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getImagePath() { // CHANGED: Returns String path
        return imagePath;
    }

    public String getName() {
        return name;
    }

    public String getFatherName() {
        return fatherName;
    }

    public int getAge() {
        return age;
    }

    public String getAdmissionDate() {
        return admissionDate;
    }

    public String getGender() {
        return gender;
    }

    public String getBelt() {
        return belt;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getClubName() {
        return clubName;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setImagePath(String imagePath) { // CHANGED: Accepts String path
        this.imagePath = imagePath;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFatherName(String fatherName) {
        this.fatherName = fatherName;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setAdmissionDate(String admissionDate) {
        this.admissionDate = admissionDate;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setBelt(String belt) {
        this.belt = belt;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }
}