package com.example.simpleagendaapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    // Λίστα με τα ραντεβού που θα εμφανιστούν
    private List<Appointment> appointments;

    public interface OnAppointmentLongClickListener {
        void onLongClick(Appointment appointment);
    }

    private OnAppointmentLongClickListener listener;

    // Constructor: παίρνει λίστα και listener για τα ραντεβού
    public AppointmentAdapter(List<Appointment> appointments, OnAppointmentLongClickListener listener) {
        this.appointments = appointments;
        this.listener = listener;
    }

    // Ενημέρωση της λίστας ραντεβού και ανανέωση της προβολής
    public void updateAppointments(List<Appointment> newAppointments) {
        this.appointments = newAppointments;
        notifyDataSetChanged();
    }

    // Δημιουργία ViewHolder - καλείται όταν χρειάζεται νέο itemView
    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false);
        return new AppointmentViewHolder(itemView);
    }

    // Σύνδεση δεδομένων με τα στοιχεία προβολής του item
    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Appointment appointment = appointments.get(position);
        holder.titleText.setText(appointment.title);
        holder.timeText.setText(appointment.time);
        holder.noteText.setText(appointment.note);
        holder.locationText.setText(appointment.locationName);

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onLongClick(appointment);
            }
            return true;
        });

    }

    // Επιστροφή του αριθμού των στοιχείων της λίστας
    @Override
    public int getItemCount() {
        return appointments.size();
    }

    // ViewHolder που κατέχει τις αναφορές των TextView ενός item
    public static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, timeText, noteText, locationText;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.itemTitle);
            timeText = itemView.findViewById(R.id.itemTime);
            noteText = itemView.findViewById(R.id.itemNote);
            locationText = itemView.findViewById(R.id.itemLocation);
        }
    }
}
