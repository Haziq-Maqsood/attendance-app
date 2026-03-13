package com.example.hmattendance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hmattendance.models.Student;

import java.util.List;
import java.util.Map;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.StudentViewHolder> {

    private List<Student> studentList;
    // Map to store attendance status for each student: StudentId -> Status (e.g., "Present", "Absent")
    private Map<String, String> attendanceStatusMap;
    private String[] attendanceStatuses; // Array of status strings (from resources)

    public AttendanceAdapter(List<Student> studentList, Map<String, String> attendanceStatusMap, String[] attendanceStatuses) {
        this.studentList = studentList;
        this.attendanceStatusMap = attendanceStatusMap;
        this.attendanceStatuses = attendanceStatuses;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = studentList.get(position);
        holder.tvStudentName.setText(student.getName());
        holder.id.setText(student.getId());

        // Get current status for this student from the map
        String currentStatus = attendanceStatusMap.get(student.getId());

        // Set the appropriate radio button based on the current status
        if (currentStatus != null) {
            for (int i = 0; i < holder.rgAttendanceStatus.getChildCount(); i++) {
                RadioButton radioButton = (RadioButton) holder.rgAttendanceStatus.getChildAt(i);
                if (radioButton.getText().toString().equalsIgnoreCase(currentStatus)) {
                    radioButton.setChecked(true);
                    break;
                }
            }
        } else {
            // Default to 'Absent' if no status is found
            holder.rbAbsent.setChecked(true);
            attendanceStatusMap.put(student.getId(), "Absent"); // Initialize map for new entries
        }

        // Listener to update the map when a radio button is selected
        holder.rgAttendanceStatus.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selectedRadioButton = group.findViewById(checkedId);
            if (selectedRadioButton != null) {
                attendanceStatusMap.put(student.getId(), selectedRadioButton.getText().toString());
            }
        });
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    // Call this method to update the data in the adapter and refresh the RecyclerView
    public void updateData(List<Student> newStudentList, Map<String, String> newAttendanceStatusMap) {
        this.studentList = newStudentList;
        this.attendanceStatusMap = newAttendanceStatusMap;
        notifyDataSetChanged();
    }

    // Method to retrieve the final attendance status map for saving
    public Map<String, String> getAttendanceStatusMap() {
        return attendanceStatusMap;
    }

    public static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName,id;
        RadioGroup rgAttendanceStatus;
        RadioButton rbPresent, rbAbsent, rbLate, rbExcused;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tv_student_name);
            id = itemView.findViewById(R.id.tv_student_id);
            rgAttendanceStatus = itemView.findViewById(R.id.rg_attendance_status);
            rbPresent = itemView.findViewById(R.id.rb_present);
            rbAbsent = itemView.findViewById(R.id.rb_absent);

        }
    }
}