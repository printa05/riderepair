package com.example.riderepair;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class mechanic_settings extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String mechanicId;
    private String garageDocId;
    private TextView tvName, tvEmail, tvGarageName, tvGarageAddress;
    private Button btnEditProfile, btnAddGarage, btnEditGarage, btnLogout;
    private Switch switchAvailability;
    private LinearLayout garageInfoLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mechanic_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            mechanicId = user.getUid();
        } else {
            Toast.makeText(this, "Not Logged In", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvGarageName = findViewById(R.id.tvGarageName);
        tvGarageAddress = findViewById(R.id.tvGarageAddress);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnAddGarage = findViewById(R.id.btnAddGarage);
        btnEditGarage = findViewById(R.id.btnEditGarage);
        btnLogout = findViewById(R.id.btnLogout);
        switchAvailability = findViewById(R.id.switchAvailability);
        garageInfoLayout = findViewById(R.id.garageInfoLayout);

        fetchMechanicProfile();
        fetchGarageInfo();

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnAddGarage.setOnClickListener(v -> showAddGarageDialog());
        btnEditGarage.setOnClickListener(v -> showEditGarageDialog());
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(mechanic_settings.this, login.class));
            finish();
        });

        switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateAvailability(isChecked);
        });
    }

    private void fetchMechanicProfile() {
        db.collection("users").document(mechanicId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name") != null ? documentSnapshot.getString("name") : "Unknown";
                        String email = documentSnapshot.getString("email") != null ? documentSnapshot.getString("email") : "Unknown";
                        Boolean isAvailable = documentSnapshot.getBoolean("isAvailable");
                        tvName.setText(name);
                        tvEmail.setText(email);
                        switchAvailability.setChecked(isAvailable != null ? isAvailable : true);
                    } else {
                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showEditProfileDialog() {
        EditText etName = new EditText(this);
        etName.setInputType(InputType.TYPE_CLASS_TEXT);
        etName.setText(tvName.getText().toString());

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Edit Profile")
                .setMessage("Update your name:")
                .setView(etName)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateProfile(newName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateProfile(String newName) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);

        db.collection("users").document(mechanicId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    tvName.setText(newName);
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.updateProfile(new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build());
        }
    }

    private void fetchGarageInfo() {
        db.collection("garages")
                .whereEqualTo("mechanicId", mechanicId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            garageDocId = doc.getId();
                            String garageName = doc.getString("name") != null ? doc.getString("name") : "Unknown";
                            String address = doc.getString("address") != null ? doc.getString("address") : "Unknown";
                            Double lat = doc.getDouble("lat");
                            Double lng = doc.getDouble("lng");
                            tvGarageName.setText(garageName);
                            tvGarageAddress.setText(address + (lat != null && lng != null ? " (Lat: " + lat + ", Lng: " + lng + ")" : ""));
                            garageInfoLayout.setVisibility(View.VISIBLE);
                            btnAddGarage.setVisibility(View.GONE);
                            Log.d("FetchGarage", "Garage ID: " + garageDocId + ", Lat: " + lat + ", Lng: " + lng);
                        }
                    } else {
                        garageInfoLayout.setVisibility(View.GONE);
                        btnAddGarage.setVisibility(View.VISIBLE);
                        Log.d("FetchGarage", "No garage found for mechanic ID: " + mechanicId);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load garage info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("FetchGarage", "Error: " + e.getMessage());
                });
    }

    private void showAddGarageDialog() {
        EditText etGarageName = new EditText(this);
        etGarageName.setInputType(InputType.TYPE_CLASS_TEXT);
        etGarageName.setHint("Garage Name");

        EditText etAddress = new EditText(this);
        etAddress.setInputType(InputType.TYPE_CLASS_TEXT);
        etAddress.setHint("Address");

        EditText etLatitude = new EditText(this);
        etLatitude.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        etLatitude.setHint("Latitude (e.g., 22.31)");
        etLatitude.setText("22.31"); // Bhayli, Vadodara

        EditText etLongitude = new EditText(this);
        etLongitude.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        etLongitude.setHint("Longitude (e.g., 73.13)");
        etLongitude.setText("73.13"); // Bhayli, Vadodara

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);
        layout.addView(etGarageName);
        layout.addView(etAddress);
        layout.addView(etLatitude);
        layout.addView(etLongitude);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Add Garage")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String garageName = etGarageName.getText().toString().trim();
                    String address = etAddress.getText().toString().trim();
                    String latStr = etLatitude.getText().toString().trim();
                    String lngStr = etLongitude.getText().toString().trim();
                    if (garageName.isEmpty() || address.isEmpty() || latStr.isEmpty() || lngStr.isEmpty()) {
                        Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double lat = Double.parseDouble(latStr);
                        double lng = Double.parseDouble(lngStr);
                        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                            Toast.makeText(this, "Invalid latitude or longitude range", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        addGarage(garageName, address, lat, lng);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid latitude or longitude format", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addGarage(String garageName, String address, double lat, double lng) {
        Map<String, Object> garage = new HashMap<>();
        garage.put("mechanicId", mechanicId);
        garage.put("name", garageName);
        garage.put("address", address);
        garage.put("lat", lat);
        garage.put("lng", lng);
        garage.put("timestamp", System.currentTimeMillis());

        db.collection("garages")
                .add(garage)
                .addOnSuccessListener(documentReference -> {
                    garageDocId = documentReference.getId();
                    tvGarageName.setText(garageName);
                    tvGarageAddress.setText(address + " (Lat: " + lat + ", Lng: " + lng + ")");
                    garageInfoLayout.setVisibility(View.VISIBLE);
                    btnAddGarage.setVisibility(View.GONE);
                    Toast.makeText(this, "Garage added at Lat: " + lat + ", Lng: " + lng, Toast.LENGTH_LONG).show();
                    Log.d("AddGarage", "Garage added with ID: " + garageDocId + ", lat: " + lat + ", lng: " + lng);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to add garage: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("AddGarage", "Error: " + e.getMessage());
                });
    }

    private void showEditGarageDialog() {
        EditText etGarageName = new EditText(this);
        etGarageName.setInputType(InputType.TYPE_CLASS_TEXT);
        etGarageName.setText(tvGarageName.getText().toString());

        EditText etAddress = new EditText(this);
        etAddress.setInputType(InputType.TYPE_CLASS_TEXT);
        etAddress.setText(tvGarageAddress.getText().toString().split(" \\(Lat")[0]);

        EditText etLatitude = new EditText(this);
        etLatitude.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        etLatitude.setHint("Latitude (e.g., 22.31)");
        etLatitude.setText("22.31");

        EditText etLongitude = new EditText(this);
        etLongitude.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        etLongitude.setHint("Longitude (e.g., 73.13)");
        etLongitude.setText("73.13");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);
        layout.addView(etGarageName);
        layout.addView(etAddress);
        layout.addView(etLatitude);
        layout.addView(etLongitude);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Edit Garage Info")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String garageName = etGarageName.getText().toString().trim();
                    String address = etAddress.getText().toString().trim();
                    String latStr = etLatitude.getText().toString().trim();
                    String lngStr = etLongitude.getText().toString().trim();
                    if (garageName.isEmpty() || address.isEmpty() || latStr.isEmpty() || lngStr.isEmpty()) {
                        Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double lat = Double.parseDouble(latStr);
                        double lng = Double.parseDouble(lngStr);
                        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                            Toast.makeText(this, "Invalid latitude or longitude range", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        updateGarage(garageName, address, lat, lng);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid latitude or longitude format", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateGarage(String garageName, String address, double lat, double lng) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", garageName);
        updates.put("address", address);
        updates.put("lat", lat);
        updates.put("lng", lng);

        db.collection("garages").document(garageDocId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    tvGarageName.setText(garageName);
                    tvGarageAddress.setText(address + " (Lat: " + lat + ", Lng: " + lng + ")");
                    Toast.makeText(this, "Garage updated at Lat: " + lat + ", Lng: " + lng, Toast.LENGTH_LONG).show();
                    Log.d("UpdateGarage", "Garage updated with ID: " + garageDocId + ", lat: " + lat + ", lng: " + lng);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update garage: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("UpdateGarage", "Error: " + e.getMessage());
                });
    }

    private void updateAvailability(boolean isAvailable) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isAvailable", isAvailable);

        db.collection("users").document(mechanicId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, isAvailable ? "You are now available" : "You are now unavailable", Toast.LENGTH_SHORT).show();
                    Log.d("Availability", "Set to: " + isAvailable);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update availability: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    switchAvailability.setChecked(!isAvailable);
                    Log.e("Availability", "Error: " + e.getMessage());
                });
    }
}