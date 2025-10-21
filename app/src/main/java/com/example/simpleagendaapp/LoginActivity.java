package com.example.simpleagendaapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    EditText userName, password;
    Button loginButton, fingerprintButton;
    TextView signupText;
    DBHandler dbHandler;
    String savedUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userName = findViewById(R.id.username);
        password = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        signupText = findViewById(R.id.signupText);
        fingerprintButton = findViewById(R.id.fingerprintButton);

        // Αρχικοποίηση της βάσης δεδομένων
        dbHandler = new DBHandler(this);

        // Ανάκτηση αποθηκευμένου ονόματος χρήστη από SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        savedUsername = prefs.getString("username", null);

        // Αν υπάρχει αποθηκευμένος χρήστης και η συσκευή υποστηρίζει βιομετρικά, εμφάνισε βιομετρικά
        if (savedUsername != null) {
            BiometricManager biometricManager = BiometricManager.from(this);
            if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
                showFingerprintPrompt();
            }
        }

        // Χειρισμός κουμπιού σύνδεσης
        loginButton.setOnClickListener(view -> {
            String enteredUsername = userName.getText().toString().trim();
            String enteredPassword = password.getText().toString().trim();

            // Έλεγχος για κενά πεδία
            if (enteredUsername.isEmpty() || enteredPassword.isEmpty()) {
                Toast.makeText(this, "Συμπλήρωσε όλα τα πεδία!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Έλεγχος εγκυρότητας χρήστη στη βάση
            boolean valid = dbHandler.validateUser(enteredUsername, enteredPassword);

            if (valid) {
                // Αποθήκευση username και μεταφορά στην αρχική της εφαρμογή
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("username", enteredUsername);
                editor.apply();

                Toast.makeText(this, "Επιτυχής σύνδεση!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                // Μήνυμα αποτυχίας σύνδεσης
                Toast.makeText(this, "Λάθος στοιχεία!", Toast.LENGTH_SHORT).show();
            }
        });

        // Μεταφορά στην εγγραφής όταν πατηθεί το κείμενο "Κάνε εγγραφή"
        signupText.setOnClickListener(view -> {
            startActivity(new Intent(this, RegistrationActivity.class));
        });

        // Χειρισμός κουμπιού fingerprint
        fingerprintButton.setOnClickListener(v -> {
            BiometricManager biometricManager = BiometricManager.from(this);
            if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
                showFingerprintPrompt();
            } else {
                Toast.makeText(this, "Η συσκευή δεν υποστηρίζει βιομετρικά.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Εμφάνιση του βιομετρικού παραθύρου για ταυτοποίηση
    private void showFingerprintPrompt() {
        if (savedUsername == null) {
            Toast.makeText(this, "Δεν υπάρχει αποθηκευμένος χρήστης.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ρυθμίσεις του παραθύρου βιομετρικής ταυτοποίησης
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Σύνδεση με Δακτυλικό Αποτύπωμα")
                .setSubtitle("Αγγίξτε τον αισθητήρα για είσοδο")
                .setNegativeButtonText("Άκυρο")
                .build();

        // Ορισμός callback για τις διαφορετικές περιπτώσεις ταυτοποίησης
        BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);

                        Toast.makeText(getApplicationContext(), "Επιτυχής ταυτοποίηση", Toast.LENGTH_SHORT).show();

                        // Προαιρετική αποθήκευση του username
                        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("username", savedUsername);
                        editor.apply();

                        // Εκκίνηση της αρχικής της εφαρμογής
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Toast.makeText(getApplicationContext(), "Σφάλμα: " + errString, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(getApplicationContext(), "Μη έγκυρη ταυτοποίηση", Toast.LENGTH_SHORT).show();
                    }
                });

        // Ενεργοποίηση ταυτοποίησης
        biometricPrompt.authenticate(promptInfo);
    }
}
