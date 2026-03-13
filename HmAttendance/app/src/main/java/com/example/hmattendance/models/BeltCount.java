package com.example.hmattendance.models;

public class BeltCount {
    private String beltName;
    private int count;

    public BeltCount(String beltName, int count) {
        this.beltName = beltName;
        this.count = count;
    }

    public String getBeltName() {
        return beltName;
    }

    public int getCount() {
        return count;
    }
}