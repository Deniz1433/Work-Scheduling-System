package com.example.demo;

public class User {
    private String name;
    private String email;
    private boolean hasSelectedDays;
    private boolean isApproved;

    // Constructor
    public User(String name, String email, boolean hasSelectedDays, boolean isApproved) {
        this.name = name;
        this.email = email;
        this.hasSelectedDays = hasSelectedDays;
        this.isApproved = isApproved;
    }

    // Getters & Setters
    public String getName() { return name; }
    public String getEmail() { return email; }
    public boolean hasSelectedDays() { return hasSelectedDays; }
    public boolean isApproved() { return isApproved; }
}

}

