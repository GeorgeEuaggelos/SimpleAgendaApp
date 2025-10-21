package com.example.simpleagendaapp;

// Κλάση για ραντεβού
public class Appointment {
    public int id;
    public String title;
    public String note;
    public String time;
    public String locationName;

    // Constructor με ID (χρήσιμο για ραντεβού από τη βάση)
    public Appointment(int id ,String title, String note, String time, String locationName) {
        this.id = id;
        this.title = title;
        this.note = note;
        this.time = time;
        this.locationName = locationName;
    }

    // Constructor χωρίς ID (χρήσιμο για προσωρινή δημιουργία νέου ραντεβού)
    public Appointment(String title, String note, String time, String locationName) {
        this.title = title;
        this.note = note;
        this.time = time;
        this.locationName = locationName;
    }
}

