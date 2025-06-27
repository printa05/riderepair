package com.example.riderepair;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class user extends AppCompatActivity {

    private FirebaseFirestore db;
    private String userId;
    private String userName;
    private FusedLocationProviderClient fusedLocationClient;
    private double userLat = 0.0;
    private double userLng = 0.0;
    RecyclerView rvVehicles;
    ArrayList<Vehicle> vehiclesList = new ArrayList<>();
    VehicleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        } else {
            Toast.makeText(this, "Not Logged In", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Button btnAddVehicle = findViewById(R.id.btnAddVehicle);
        Button btnSOS = findViewById(R.id.btnSOS);
        Button btnSettings = findViewById(R.id.btnSettings);
        Button btnNotifications = findViewById(R.id.btnNotifications);
        Button btnLogout = findViewById(R.id.btnLogout);

        rvVehicles = findViewById(R.id.rvVehicles);
        if (rvVehicles != null) {
            rvVehicles.setLayoutManager(new LinearLayoutManager(this));
            adapter = new VehicleAdapter(vehiclesList);
            rvVehicles.setAdapter(adapter);
        } else {
            Log.e("UserActivity", "RecyclerView is null");
        }

        btnAddVehicle.setOnClickListener(v -> startActivity(new Intent(user.this, addvehicle.class)));
        btnSOS.setOnClickListener(v -> showVehicleSelectionDialog());
        btnSettings.setOnClickListener(v -> startActivity(new Intent(user.this, settings.class)));
        btnNotifications.setOnClickListener(v -> startActivity(new Intent(user.this, notifications.class)));
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(user.this, login.class));
            finish();
        });

        fetchUserProfile();
        loadVehicles();
    }

    private void fetchUserProfile() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        userName = doc.getString("name") != null ? doc.getString("name") : "UserX";
                        Log.d("UserProfile", "Fetched userName: " + userName + ", userId: " + userId);
                    } else {
                        Log.e("UserProfile", "Profile document does not exist for userId: " + userId);
                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error fetching profile: " + e.getMessage(), e);
                });
    }

    private void loadVehicles() {
        db.collection("vehicles")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    vehiclesList.clear();
                    if (e != null) {
                        Toast.makeText(this, "Error loading vehicles: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("Firestore", "Error loading vehicles: " + e.getMessage(), e);
                        return;
                    }
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Vehicle vehicle = new Vehicle(
                                    doc.getId(),
                                    doc.getString("userId"),
                                    doc.getString("name"),
                                    doc.getString("number"),
                                    doc.getString("type")
                            );
                            vehiclesList.add(vehicle);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    Log.d("Vehicles", "Loaded " + vehiclesList.size() + " vehicles for userId: " + userId);
                });
    }

    private void showVehicleSelectionDialog() {
        if (vehiclesList.isEmpty()) {
            Toast.makeText(this, "Add a vehicle first", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] vehicleNames = new String[vehiclesList.size()];
        for (int i = 0; i < vehiclesList.size(); i++) {
            vehicleNames[i] = vehiclesList.get(i).getName() + " (" + vehiclesList.get(i).getType() + ")";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Vehicle for SOS");
        builder.setItems(vehicleNames, (dialog, which) -> {
            Vehicle selectedVehicle = vehiclesList.get(which);
            sendSOSRequest(selectedVehicle);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void sendSOSRequest(Vehicle selectedVehicle) {
        if (userName == null) {
            Toast.makeText(this, "User profile not loaded yet, please try again", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        userLat = location.getLatitude();
                        userLng = location.getLongitude();
                        if (userLat < -90 || userLat > 90 || userLng < -180 || userLng > 180 || userLat == 0.0 || userLng == 0.0) {
                            Toast.makeText(this, "Invalid location data, please try again", Toast.LENGTH_SHORT).show();
                            Log.e("Location", "Invalid user location: lat=" + userLat + ", lng=" + userLng);
                            return;
                        }
                        Log.d("Location", "User location: lat=" + userLat + ", lng=" + userLng);

                        Map<String, Object> sosRequest = new HashMap<>();
                        sosRequest.put("userId", userId);
                        sosRequest.put("userName", userName);
                        sosRequest.put("vehicleName", selectedVehicle.getName());
                        sosRequest.put("vehicleNumber", selectedVehicle.getNumber());
                        sosRequest.put("vehicleType", selectedVehicle.getType());
                        sosRequest.put("lat", userLat);
                        sosRequest.put("lng", userLng);
                        sosRequest.put("status", "pending");
                        sosRequest.put("timestamp", System.currentTimeMillis());

                        db.collection("sos_requests")
                                .add(sosRequest)
                                .addOnSuccessListener(documentReference -> {
                                    String requestId = documentReference.getId();
                                    Log.d("SOSRequest", "SOS request created with ID: " + requestId + ", lat: " + userLat + ", lng: " + userLng);
                                    assignMechanics(requestId);
                                    Toast.makeText(this, "SOS Request Sent with " + selectedVehicle.getName(), Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to send SOS request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e("Firestore", "Error sending SOS: " + e.getMessage(), e);
                                });
                    } else {
                        Toast.makeText(this, "Unable to get location, please ensure location is enabled", Toast.LENGTH_SHORT).show();
                        Log.e("Location", "Location is null");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Location", "Error getting location: " + e.getMessage(), e);
                });
    }

    private void assignMechanics(String requestId) {
        db.collection("garages")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.e("AssignMechanics", "No garages found in Firestore");
                        Toast.makeText(this, "No mechanics registered in the system", Toast.LENGTH_LONG).show();
                        return;
                    }
                    int assignedCount = 0;
                    Log.d("AssignMechanics", "Processing " + queryDocumentSnapshots.size() + " garages for request ID: " + requestId);
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String mechanicId = doc.getString("mechanicId");
                        String garageId = doc.getId();

                        if (mechanicId == null) {
                            Log.e("AssignMechanics", "Invalid garage data for garage ID: " + garageId + ", mechanicId is null");
                            continue;
                        }

                        Map<String, Object> visibleTo = new HashMap<>();
                        visibleTo.put("mechanicId", mechanicId);

                        db.collection("sos_requests")
                                .document(requestId)
                                .collection("visible_to")
                                .document(mechanicId)
                                .set(visibleTo)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("AssignMechanics", "Mechanic ID: " + mechanicId + " added to visible_to for request ID: " + requestId);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Firestore", "Error adding mechanic ID: " + mechanicId + " to visible_to: " + e.getMessage(), e);
                                    Toast.makeText(this, "Failed to assign mechanic: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                        assignedCount++;
                    }
                    Log.d("AssignMechanics", "Total mechanics assigned: " + assignedCount);
                    if (assignedCount == 0) {
                        Toast.makeText(this, "No mechanics available to assign", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, assignedCount + " mechanics notified", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching garages: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to assign mechanics: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showVehicleSelectionDialog();
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}