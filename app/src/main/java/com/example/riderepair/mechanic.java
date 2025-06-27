package com.example.riderepair;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class mechanic extends AppCompatActivity {

    private FirebaseFirestore db;
    private String mechanicId;
    private boolean isAvailable = false;
    private LinearLayout sosContainer;
    private final List<SOSRequest> sosRequests = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mechanic);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            mechanicId = user.getUid();
        } else {
            Toast.makeText(this, "Not Logged In", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        sosContainer = findViewById(R.id.sosContainer);
        Spinner spinnerFilter = findViewById(R.id.spinnerFilter);
        Button btnAddGarage = findViewById(R.id.btnAddGarage);
        Button btnSettings = findViewById(R.id.btnSettings);
        Button btnLogout = findViewById(R.id.btnLogout);

        String[] filterOptions = {"Sort by Time"}; // Removed distance sorting
        android.widget.ArrayAdapter<String> spinnerAdapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, filterOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(spinnerAdapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sortSOSRequests();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnAddGarage.setOnClickListener(v -> startActivity(new Intent(mechanic.this, mechanic_settings.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(mechanic.this, mechanic_settings.class)));
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(mechanic.this, login.class));
            finish();
        });

        fetchMechanicProfile();
    }

    private void fetchMechanicProfile() {
        db.collection("users").document(mechanicId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        isAvailable = doc.getBoolean("isAvailable") != null && doc.getBoolean("isAvailable");
                        Log.d("MechanicProfile", "Mechanic ID: " + mechanicId + ", isAvailable: " + isAvailable);
                    } else {
                        Log.e("MechanicProfile", "Profile document does not exist for ID: " + mechanicId);
                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                    }
                    fetchGarageInfo();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching mechanic profile: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchGarageInfo() {
        db.collection("garages").whereEqualTo("mechanicId", mechanicId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        Log.d("MechanicProfile", "Garage found, ID: " + doc.getId());
                    } else {
                        Log.e("MechanicProfile", "No garage found for mechanic ID: " + mechanicId);
                        Toast.makeText(this, "No garage found, please add one in settings", Toast.LENGTH_LONG).show();
                    }
                    loadSOSRequests();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching garage: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to load garage: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadSOSRequests() {
        if (!isAvailable) {
            Toast.makeText(this, "You are not available to receive SOS requests", Toast.LENGTH_LONG).show();
            sosContainer.removeAllViews();
            sosRequests.clear();
            Log.d("SOSRequests", "Not loading requests: isAvailable is false");
            return;
        }

        db.collection("sos_requests")
                .whereEqualTo("status", "pending")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    sosRequests.clear();
                    sosContainer.removeAllViews();
                    if (e != null) {
                        Toast.makeText(this, "Error loading SOS requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("Firestore", "Snapshot listener error: " + e.getMessage(), e);
                        return;
                    }
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        Log.d("SOSRequests", "Found " + queryDocumentSnapshots.size() + " pending requests");
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String requestId = doc.getId();
                            Log.d("SOSRequests", "Checking request ID: " + requestId);
                            db.collection("sos_requests").document(requestId)
                                    .collection("visible_to")
                                    .document(mechanicId)
                                    .get()
                                    .addOnSuccessListener(visibleDoc -> {
                                        if (visibleDoc.exists()) {
                                            Double reqLat = doc.getDouble("lat");
                                            Double reqLng = doc.getDouble("lng");
                                            if (reqLat == null || reqLng == null || reqLat < -90 || reqLat > 90 || reqLng < -180 || reqLng > 180) {
                                                Log.e("SOSRequests", "Invalid coordinates for request ID: " + requestId + ", lat: " + reqLat + ", lng: " + reqLng);
                                                return;
                                            }
                                            SOSRequest request = new SOSRequest(
                                                    requestId,
                                                    doc.getString("userId"),
                                                    doc.getString("userName"),
                                                    doc.getString("vehicleName"),
                                                    doc.getString("vehicleNumber"),
                                                    doc.getString("vehicleType"),
                                                    reqLat,
                                                    reqLng,
                                                    doc.getString("status"),
                                                    doc.getLong("timestamp")
                                            );
                                            sosRequests.add(request);
                                            Log.d("SOSRequests", "Added request ID: " + requestId + ", User: " + request.getUserName() +
                                                    ", Location: (" + reqLat + "," + reqLng + ")");
                                            sortSOSRequests();
                                        } else {
                                            Log.d("SOSRequests", "Request ID: " + requestId + " not visible to mechanic ID: " + mechanicId);
                                        }
                                    })
                                    .addOnFailureListener(e2 -> {
                                        Log.e("Firestore", "Error checking visible_to for request " + requestId + ": " + e2.getMessage(), e2);
                                    });
                        }
                    } else {
                        Log.d("SOSRequests", "No pending SOS requests found in Firestore");
                        Toast.makeText(this, "No pending SOS requests available", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void sortSOSRequests() {
        // Sort by timestamp (newest first)
        Collections.sort(sosRequests, (r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
        displaySOSRequests();
    }

    private void displaySOSRequests() {
        sosContainer.removeAllViews();
        Log.d("SOSRequests", "Displaying " + sosRequests.size() + " requests");
        if (sosRequests.isEmpty()) {
            Toast.makeText(this, "No SOS requests assigned to you", Toast.LENGTH_LONG).show();
        }
        for (SOSRequest request : sosRequests) {
            View sosView = getLayoutInflater().inflate(R.layout.sos_request_item, sosContainer, false);

            TextView tvInfo = sosView.findViewById(R.id.tvInfo);
            TextView tvDistance = sosView.findViewById(R.id.tvDistance);
            TextView tvLocationClickable = sosView.findViewById(R.id.tvLocationClickable);
            Button btnAccept = sosView.findViewById(R.id.btnAccept);
            Button btnDeny = sosView.findViewById(R.id.btnDeny);

            tvInfo.setText("User: " + request.getUserName() + "\nVehicle: " + request.getVehicleName() +
                    " (" + request.getVehicleType() + ", " + request.getVehicleNumber() + ")\nStatus: " + request.getStatus());
            tvDistance.setText("Distance: N/A");
            Log.d("SOSRequests", "Displaying request ID: " + request.getId());

            tvLocationClickable.setOnClickListener(v -> {
                String uri = "geo:" + request.getLat() + "," + request.getLng() + "?q=" + request.getLat() + "," + request.getLng();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
            });

            btnAccept.setOnClickListener(v -> handleSOSAction(request, "accepted"));
            btnDeny.setOnClickListener(v -> handleSOSAction(request, "denied"));

            sosContainer.addView(sosView);
        }
    }

    private void handleSOSAction(SOSRequest request, String action) {
        db.collection("sos_requests").document(request.getId())
                .update("status", action)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "SOS " + action, Toast.LENGTH_SHORT).show();
                    sendNotificationToUser(request.getUserId(), request.getVehicleName(), action);
                    loadSOSRequests();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update SOS status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error updating SOS status: " + e.getMessage(), e);
                });
    }

    private void sendNotificationToUser(String userId, String vehicleName, String action) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("message", "Your SOS request for " + vehicleName + " has been " + action + " by a mechanic.");
        notification.put("status", action);
        notification.put("timestamp", System.currentTimeMillis());

        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Notification", "Notification sent to user ID: " + userId + ", vehicle: " + vehicleName + ", notification ID: " + documentReference.getId());
                    Toast.makeText(this, "Notification sent to user", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error sending notification: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to send notification: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}