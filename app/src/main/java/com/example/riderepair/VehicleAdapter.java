package com.example.riderepair;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder> {

    private final ArrayList<Vehicle> vehicles;

    public VehicleAdapter(ArrayList<Vehicle> vehicles) {
        this.vehicles = vehicles;
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.vehicle_item, parent, false);
        return new VehicleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        Vehicle vehicle = vehicles.get(position);
        holder.tvVehicleInfo.setText(vehicle.getName() + " (" + vehicle.getType() + ", " + vehicle.getNumber() + ")");
        holder.btnDelete.setOnClickListener(v -> {
            FirebaseFirestore.getInstance().collection("vehicles").document(vehicle.getId())
                    .delete();
        });
    }

    @Override
    public int getItemCount() {
        return vehicles.size();
    }

    static class VehicleViewHolder extends RecyclerView.ViewHolder {
        TextView tvVehicleInfo;
        Button btnDelete;

        public VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVehicleInfo = itemView.findViewById(R.id.tvVehicleInfo);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}