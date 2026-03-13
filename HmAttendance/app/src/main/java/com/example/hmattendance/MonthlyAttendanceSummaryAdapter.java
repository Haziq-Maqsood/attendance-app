package com.example.hmattendance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hmattendance.models.MonthlyAttendanceSummary;

import java.util.List;

public class MonthlyAttendanceSummaryAdapter extends RecyclerView.Adapter<MonthlyAttendanceSummaryAdapter.SummaryViewHolder> {

    private List<MonthlyAttendanceSummary> summaryList;

    public MonthlyAttendanceSummaryAdapter(List<MonthlyAttendanceSummary> summaryList) {
        this.summaryList = summaryList;
    }

    @NonNull
    @Override
    public SummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_monthly_attendance_summary, parent, false);
        return new SummaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SummaryViewHolder holder, int position) {
        MonthlyAttendanceSummary summary = summaryList.get(position);

        holder.tvStudentNameSummary.setText(summary.getStudentName());
        holder.id.setText(summary.getStudentId());
        holder.tvPresentDays.setText(String.valueOf(summary.getTotalPresentDays()));
        holder.tvAbsentDays.setText(String.valueOf(summary.getTotalAbsentDays()));
        holder.tvTotalMarkedDays.setText(String.valueOf(summary.getTotalMarkedDays()));
    }

    @Override
    public int getItemCount() {
        return summaryList.size();
    }

    // Method to update the data in the adapter and refresh the RecyclerView
    public void updateData(List<MonthlyAttendanceSummary> newSummaryList) {
        this.summaryList = newSummaryList;
        notifyDataSetChanged();
    }

    public static class SummaryViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentNameSummary,id;
        TextView tvPresentDays;
        TextView tvAbsentDays;
        TextView tvTotalMarkedDays;

        public SummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentNameSummary = itemView.findViewById(R.id.tv_student_name_summary);
            id = itemView.findViewById(R.id.tv_student_id);
            tvPresentDays = itemView.findViewById(R.id.tv_present_days);
            tvAbsentDays = itemView.findViewById(R.id.tv_absent_days);
            tvTotalMarkedDays = itemView.findViewById(R.id.tv_total_marked_days);
        }
    }
}