package com.example.riderepair;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
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

public class settings extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;
    private TextView tvName, tvEmail;
    private Button btnEditProfile, btnAddVehicle, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        } else {
            Toast.makeText(this, "Not Logged In", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnAddVehicle = findViewById(R.id.btnAddVehicle);
        btnLogout = findViewById(R.id.btnLogout);

        fetchUserProfile();

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnAddVehicle.setOnClickListener(v -> startActivity(new Intent(settings.this, addvehicle.class)));
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(settings.this, login.class));
            finish();
        });
    }

    private void fetchUserProfile() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name") != null ? documentSnapshot.getString("name") : "Unknown";
                        String email = documentSnapshot.getString("email") != null ? documentSnapshot.getString("email") : "Unknown";
                        tvName.setText(name);
                        tvEmail.setText(email);
                    } else {
                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
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

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    tvName.setText(newName);
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                });

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.updateProfile(new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build());
        }
    }
}