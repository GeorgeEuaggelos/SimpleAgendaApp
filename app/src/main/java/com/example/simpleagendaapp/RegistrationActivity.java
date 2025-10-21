package com.example.simpleagendaapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegistrationActivity extends AppCompatActivity {

    EditText usernameField, passwordField;
    Button registerButton;
    DBHandler dbHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        usernameField = findViewById(R.id.registerUsername);
        passwordField = findViewById(R.id.registerPassword);
        registerButton = findViewById(R.id.registerButton);

        // Αρχικοποίηση της βάσης δεδομένων
        dbHandler = new DBHandler(this);

        // Ρύθμιση click listener για το κουμπί εγγραφής
        registerButton.setOnClickListener(v -> {
            // Ανάκτηση τιμών από τα πεδία εισαγωγής
            String username = usernameField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            // Έλεγχος αν τα πεδία είναι κενά
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Συμπλήρωσε όλα τα πεδία!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Εισαγωγή νέου χρήστη στη βάση
            long result = dbHandler.insertUser(username, password);

            // Έλεγχος αν ο χρήστης υπάρχει ήδη ή η εγγραφή ήταν επιτυχής
            if (result == -1) {
                Toast.makeText(this, "Ο χρήστης υπάρχει ήδη!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Επιτυχής εγγραφή!", Toast.LENGTH_SHORT).show();
                // Μεταφορά πίσω στη σελίδα σύνδεσης
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
        });
    }
}
