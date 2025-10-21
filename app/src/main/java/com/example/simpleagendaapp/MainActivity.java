package com.example.simpleagendaapp;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    CalendarView calendarView;
    EditText noteField, locationField;
    ImageView profileIcon, weatherIcon;
    TextView weatherTemp;
    Button mapsButton, saveButton, cancelButton;
    EditText titleField;
    EditText timeField;
    DBHandler dbHandler;
    FusedLocationProviderClient fusedLocationClient;
    Location lastKnownLocation;
    String selectedDate = "";
    RecyclerView appointmentList;
    AppointmentAdapter appointmentAdapter;

    private Appointment currentEditingAppointment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Προσαρμογή περιθωρίων για κάλυψη της οθόνης απο άκρη σε άκρη
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        calendarView = findViewById(R.id.calendarView);
        noteField = findViewById(R.id.noteField);
        locationField = findViewById(R.id.locationField);
        mapsButton = findViewById(R.id.mapsButton);
        saveButton = findViewById(R.id.saveButton);
        profileIcon = findViewById(R.id.profileIcon);
        weatherIcon = findViewById(R.id.weatherIcon);
        weatherTemp = findViewById(R.id.weatherTemp);
        titleField = findViewById(R.id.titleField);
        timeField = findViewById(R.id.timeField);
        appointmentList = findViewById(R.id.appointmentList);
        dbHandler = new DBHandler(this);

        mapsButton.setVisibility(View.GONE);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        appointmentList.setLayoutManager(new LinearLayoutManager(this));

        // Listener αλλαγής ημερομηνίας στο ημερολόγιο
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = year + "-" + String.format("%02d", month + 1) + "-" + String.format("%02d", dayOfMonth);
            noteField.setVisibility(View.VISIBLE);
            updateAppointmentList();
            if (lastKnownLocation != null) {
                getWeatherData(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), selectedDate);
            } else {
                requestLocation();
            }
        });

        profileIcon.setOnClickListener(v -> showUserMenu());

        long currentDateMillis = calendarView.getDate();
        Date currentDate = new Date(currentDateMillis);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDate = sdf.format(currentDate);
        noteField.setVisibility(View.VISIBLE);
        getLastLocationThenWeather();

        //Listener για μεταφορά στην εφαρμογή 'Χάρτες' μετά απο αναζήτηση με βάση την τοποθεσία που έχει καταχωρηθεί
        mapsButton.setOnClickListener(v -> {
            String location = locationField.getText().toString().trim();
            if (!location.isEmpty()) {
                Uri uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(location));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
                mapIntent.setPackage("com.google.android.apps.maps");
                try {
                    startActivity(mapIntent);
                } catch (Exception e) {
                    Intent fallbackIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(location)));
                    startActivity(fallbackIntent);
                }
            } else {
                Toast.makeText(this, "Πληκτρολόγησε μια τοποθεσία.", Toast.LENGTH_SHORT).show();
            }
        });

        //Listener για διαχείρηση του πεδίου ώρας
        timeField.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            int hour = now.get(Calendar.HOUR_OF_DAY);
            int minute = now.get(Calendar.MINUTE);

            TimePickerDialog timePicker = new TimePickerDialog(
                    MainActivity.this,
                    (view, hourOfDay, minute1) -> {
                        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1);
                        timeField.setText(formattedTime);
                    },
                    hour, minute,
                    true
            );
            timePicker.show();
        });

        // Αρχικοποίηση adapter για τα σημέιωση και ορισμός των ενεργειών επεξεργασίας/διαγραφής
        appointmentAdapter = new AppointmentAdapter(new ArrayList<>(), appointment -> {
            PopupMenu popup = new PopupMenu(MainActivity.this, appointmentList);
            popup.getMenu().add("Επεξεργασία");
            popup.getMenu().add("Διαγραφή");

            popup.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Επεξεργασία")) {
                    editAppointment(appointment);
                } else if (item.getTitle().equals("Διαγραφή")) {
                    deleteAppointment(appointment.id);
                }
                return true;
            });

            popup.show();
        });

        appointmentList.setAdapter(appointmentAdapter);
        setInitialSaveButtonListener();
        updateAppointmentList();

        cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setVisibility(View.GONE);

    }

    private void setInitialSaveButtonListener() {
        // Ορισμός συμπεριφοράς του κουμπιού "Αποθήκευση"
        saveButton.setOnClickListener(v -> {
            String title = titleField.getText().toString().trim();
            String note = noteField.getText().toString().trim();
            String locationName = locationField.getText().toString().trim();
            String date = selectedDate;
            String time = timeField.getText().toString().trim();
            String createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            if (title.isEmpty()) {
                Toast.makeText(this, "Ο τίτλος είναι υποχρεωτικός.", Toast.LENGTH_SHORT).show();
                return;
            }

            double lat = 0.0;
            double lon = 0.0;

            if (lastKnownLocation != null) {
                lat = lastKnownLocation.getLatitude();
                lon = lastKnownLocation.getLongitude();
            }

            // Αν έχει δοθεί όνομα τοποθεσίας, προχωρά στην αποθήκευσή της
            int locationId = -1;
            if (!locationName.isEmpty()) {
                locationId = dbHandler.getLocationIdByName(locationName);
                if (locationId == -1) {
                    locationId = (int) dbHandler.insertLocation(locationName, lat, lon);
                }
            }

            SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            String username = prefs.getString("username", null);
            int userId = dbHandler.getUserId(username);

            if (userId == -1) {
                Toast.makeText(this, "Σφάλμα χρήστη. Κάνε login ξανά.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentEditingAppointment != null) {
                // Αν υπάρχει ενεργή εγγραφή προς επεξεργασία, την ενημερώνει
                dbHandler.updateAppointment(currentEditingAppointment.id, title, note, time, locationId);
                Toast.makeText(this, "Το ραντεβού ενημερώθηκε.", Toast.LENGTH_SHORT).show();
                currentEditingAppointment = null;
                saveButton.setText("Αποθήκευση");
            } else {
                // Αλλιώς δημιουργεί νέα εγγραφή
                long appointmentId = dbHandler.insertAppointment(
                        userId,
                        locationId,
                        title,
                        note,
                        date,
                        time,
                        createdAt
                );

                if (appointmentId != -1) {
                    Toast.makeText(this, "Το ραντεβού αποθηκεύτηκε.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Αποτυχία αποθήκευσης.", Toast.LENGTH_SHORT).show();
                }
            }

            clearFields();
            cancelButton.setVisibility(View.GONE);
            updateAppointmentList();
        });
    }

    private void updateAppointmentList() {
        // Ενημέρωση λίστας εγγραφών από τη βάση με βάση το χρήστη και την ημερομηνία
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", null);
        int userId = dbHandler.getUserId(username);
        List<Appointment> appointments = dbHandler.getAppointmentsForUserOnDate(userId, selectedDate);

        appointmentAdapter.updateAppointments(appointments);

        TextView emptyMessage = findViewById(R.id.emptyMessage);
        if (appointments.isEmpty()) {
            emptyMessage.setVisibility(View.VISIBLE);
        } else {
            emptyMessage.setVisibility(View.GONE);
        }
    }

    private void editAppointment(Appointment appointment) {
        // Εμφάνιση των δεδομένων μίας εγγραφής στα πεδία για επεξεργασία
        currentEditingAppointment = appointment;

        titleField.setText(appointment.title);
        noteField.setText(appointment.note);
        locationField.setText(appointment.locationName);
        timeField.setText(appointment.time);

        saveButton.setText("Αποθήκευση");

        cancelButton.setVisibility(View.VISIBLE);
        cancelButton.setOnClickListener(v -> {
            clearFields();
            currentEditingAppointment = null;
            saveButton.setText("Αποθήκευση");
            cancelButton.setVisibility(View.GONE);
        });
    }

    private void deleteAppointment(int appointmentId) {
        // Διαγραφή εγγραφής από βάση και ενημέρωση λίστας
        dbHandler.deleteAppointment(appointmentId);
        Toast.makeText(this, "Το ραντεβού διαγράφηκε.", Toast.LENGTH_SHORT).show();
        updateAppointmentList();
    }

    private void clearFields() {
        // Καθαρισμός όλων των πεδίων εισαγωγής
        titleField.setText("");
        noteField.setText("");
        locationField.setText("");
        timeField.setText("");
    }

    public String getSelectedDate() {
        return selectedDate;
    } // Επιστρέφει την επιλεγμένη ημερομηνία

    public void showMapButton(boolean visible) {
        runOnUiThread(() -> mapsButton.setVisibility(visible ? View.VISIBLE : View.GONE)); // Διαχείρηση εμφάνισης κουμπιού αναζήτησης
    }

    private void getLastLocationThenWeather() {
        // Έλεγχος δικαιωμάτων τοποθεσίας και λήψη της τελευταίας γνωστής τοποθεσίας
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 1);
            return;
        }

        // Ανάκτηση τελευταίας τοποθεσίας και εμφάνιση πρόγνωσης καιρού
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                lastKnownLocation = location;
                Log.d("LOCATION", "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
                getWeatherData(location.getLatitude(), location.getLongitude(), selectedDate);
                GPSTracker gpsTracker = new GPSTracker(this);
                gpsTracker.requestLocationUpdates();
            } else {
                // Αν δεν υπάρχει τοποθεσία, κρύβουμε τα στοιχεία καιρού
                weatherTemp.setVisibility(View.GONE);
                weatherIcon.setVisibility(View.GONE);
                requestLocation();
            }
        }).addOnFailureListener(e -> {
            // Σε περίπτωση αποτυχίας εντοπισμού
            weatherTemp.setVisibility(View.GONE);
            weatherIcon.setVisibility(View.GONE);
            Toast.makeText(this, "Σφάλμα λήψης τοποθεσίας.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        });
    }

    private void requestLocation() {
        // Δημιουργία αιτήματος για λήψη τοποθεσίας
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);

        // Callback για αποτελέσματα τοποθεσίας
        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                fusedLocationClient.removeLocationUpdates(this);
                if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                    lastKnownLocation = locationResult.getLastLocation();
                    Log.d("LOCATION", "Λήφθηκαν συντεταγμένες: Lat=" + lastKnownLocation.getLatitude()
                            + ", Lon=" + lastKnownLocation.getLongitude());
                    getWeatherData(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), selectedDate);
                    GPSTracker gpsTracker = new GPSTracker(MainActivity.this);
                    gpsTracker.requestLocationUpdates();
                } else {
                    Toast.makeText(MainActivity.this, "Αδυναμία λήψης τοποθεσίας.", Toast.LENGTH_SHORT).show();
                }
            }
        }, null);
    }

    private void getWeatherData(double lat, double lon, String date) {
        // Ανάκτηση δεδομένων καιρού από το OpenWeatherMap API
        String apiKey = "b0a1c0886920d01187a8072079d4e5bf";
        String url = "https://api.openweathermap.org/data/2.5/forecast?lat=" + lat +
                "&lon=" + lon + "&units=metric&lang=el&appid=" + apiKey;

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray list = response.getJSONArray("list");
                        JSONObject latestForDate = null;
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                        for (int i = 0; i < list.length(); i++) {
                            JSONObject item = list.getJSONObject(i);
                            String dt_txt = item.getString("dt_txt");

                            if (dt_txt.startsWith(date)) {
                                latestForDate = item; // συνεχίζει να ενημερώνεται μέχρι το τελευταίο της μέρας
                            }
                        }

                        if (latestForDate != null) {
                            JSONObject main = latestForDate.getJSONObject("main");
                            double temp = main.getDouble("temp");
                            String roundedTemp = String.valueOf(Math.round(temp));

                            JSONArray weatherArray = latestForDate.getJSONArray("weather");
                            String iconCode = weatherArray.getJSONObject(0).getString("icon");
                            String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";

                            weatherTemp.setText(roundedTemp + "°C");
                            Glide.with(this).load(iconUrl).into(weatherIcon);

                            weatherTemp.setVisibility(View.VISIBLE);
                            weatherIcon.setVisibility(View.VISIBLE);
                        } else {
                            // Αν δεν βρεθεί πρόγνωση για την ημερομηνία
                            weatherTemp.setVisibility(View.GONE);
                            weatherIcon.setVisibility(View.GONE);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> error.printStackTrace());

        queue.add(jsonObjectRequest);
    }

    private void showUserMenu() {
        // Εμφάνιση μενού προφίλ με επιλογή αποσύνδεσης
        PopupMenu popupMenu = new PopupMenu(MainActivity.this, profileIcon);

        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", "User");

        popupMenu.getMenu().add(username).setEnabled(false);
        popupMenu.getMenu().add("Αποσύνδεση");

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Αποσύνδεση")) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.apply();

                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
            return true;
        });

        popupMenu.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Χειρισμός αποτελέσματος αιτήματος για δικαιώματα τοποθεσίας
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Άδεια τοποθεσίας δόθηκε.", Toast.LENGTH_SHORT).show();
            getLastLocationThenWeather();
        } else {
            Toast.makeText(this, "Η άδεια τοποθεσίας απαιτείται για τον καιρό.", Toast.LENGTH_SHORT).show();
        }
    }
}
