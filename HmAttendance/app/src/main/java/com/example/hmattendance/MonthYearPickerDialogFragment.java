package com.example.hmattendance;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class MonthYearPickerDialogFragment extends DialogFragment {

    private DatePickerDialog.OnDateSetListener listener;

    public void setListener(DatePickerDialog.OnDateSetListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH); // Day is needed for DatePickerDialog constructor

        DatePickerDialog dialog = new DatePickerDialog(getActivity(),
                (view, year1, month1, dayOfMonth) -> listener.onDateSet(view, year1, month1, dayOfMonth),
                year, month, day) {

            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                // Hide day field using its ID
                int dayId = getContext().getResources().getIdentifier("android:id/day", null, null);
                if (dayId != 0) {
                    View dayPicker = findViewById(dayId);
                    if (dayPicker != null) {
                        dayPicker.setVisibility(View.GONE);
                    }
                }
            }
        };

        return dialog;
    }
}