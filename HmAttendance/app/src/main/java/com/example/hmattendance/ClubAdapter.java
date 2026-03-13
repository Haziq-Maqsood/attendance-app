package com.example.hmattendance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hmattendance.models.Club;

import java.util.List;

public class ClubAdapter extends RecyclerView.Adapter<ClubAdapter.ClubViewHolder> {

    private List<Club> clubList;

    public ClubAdapter(List<Club> clubList) {
        this.clubList = clubList;
    }

    // Method to update the data in the adapter
    public void setClubList(List<Club> newClubList) {
        this.clubList = newClubList;
        notifyDataSetChanged(); // Notify RecyclerView that data has changed
    }

    @NonNull
    @Override
    public ClubViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.club_list_item, parent, false);
        return new ClubViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClubViewHolder holder, int position) {
        Club club = clubList.get(position);
        holder.bind(club);
    }

    @Override
    public int getItemCount() {
        return clubList.size();
    }

    // ViewHolder class
    static class ClubViewHolder extends RecyclerView.ViewHolder {
        TextView tvClubName, tvInstructorName, tvClubEmail, tvInitials;

        public ClubViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClubName = itemView.findViewById(R.id.tv_club_name);
            tvInstructorName = itemView.findViewById(R.id.tv_instructor_name);
            tvClubEmail = itemView.findViewById(R.id.tv_club_email);
            tvInitials = itemView.findViewById(R.id.tv_initials);
        }

        public void bind(Club club) {
            tvClubName.setText(itemView.getContext().getString(R.string.club_name_format, club.getClubName()));
            tvInstructorName.setText(itemView.getContext().getString(R.string.instructor_name_format, club.getInstructorName()));
            tvClubEmail.setText(itemView.getContext().getString(R.string.email_format, club.getEmail()));
            tvInitials.setText(itemView.getContext().getString(R.string.initials_format, club.getInitials()));
            // Do NOT display password for security reasons
        }
    }
}