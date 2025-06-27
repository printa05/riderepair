package com.example.riderepair;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class addvehicle extends AppCompatActivity {

    private EditText etVehicleName, etVehicleNumber;
    private Spinner spinnerVehicleType;
    private Button btnSave;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_addvehicle);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setTitle("Add Vehicle");

        etVehicleName = findViewById(R.id.etVehicleName);
        etVehicleNumber = findViewById(R.id.etVehicleNumber);
        spinnerVehicleType = findViewById(R.id.spinnerVehicleType);
        btnSave = findViewById(R.id.btnSave);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.vehicle_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicleType.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        btnSave.setOnClickListener(v -> saveVehicleToFirestore());
    }

    private void saveVehicleToFirestore() {
        String name = etVehicleName.getText().toString().trim();
        String number = etVehicleNumber.getText().toString().trim();
        String type = spinnerVehicleType.getSelectedItem().toString();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(number)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> newVehicle = new HashMap<>();
        newVehicle.put("userId", userId);
        newVehicle.put("name", name);
        newVehicle.put("number", number);
        newVehicle.put("type", type);
        newVehicle.put("timestamp", System.currentTimeMillis());

        db.collection("vehicles")
                .add(newVehicle)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Vehicle Added", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save vehicle", Toast.LENGTH_SHORT).show();
                });
    }
}