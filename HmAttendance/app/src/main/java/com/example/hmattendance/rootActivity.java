package com.example.hmattendance;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class rootActivity extends AppCompatActivity {
    private LinearLayout addClub, showClub, showStudent, updateClub;
    private Toolbar mainToolbar;
    private static final String APP_PREFS = "app_prefs";
    private static final String KEY_USER_EMAIL = "email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_root);

        mainToolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(mainToolbar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        addClub = findViewById(R.id.addClub);
        showClub = findViewById(R.id.showClub);
        showStudent = findViewById(R.id.showStudent);
        updateClub = findViewById(R.id.updateClub);

        addClub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addClubIntent= new Intent(rootActivity.this, AddClubActivity.class);
                startActivity(addClubIntent);
            }
        });
        showClub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent showClubIntent= new Intent(rootActivity.this, DisplayClubActivity.class);
                startActivity(showClubIntent);
            }
        });
        updateClub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent updateClubIntent= new Intent(rootActivity.this, UpdateClubActivity.class);
                startActivity(updateClubIntent);
            }
        });
        showStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent showStudentIntent= new Intent(rootActivity.this, AllStudentsActivity.class);
                startActivity(showStudentIntent);
            }
        });

        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        SharedPreferences prefs = getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
        String userEmail = prefs.getString(KEY_USER_EMAIL, null);

        if (userEmail != null) {
            toolbarTitle.setText(userEmail.toUpperCase());
        } else {
            toolbarTitle.setText("ADMIN PANEL");
        }
    }

    // --- Menu Methods ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the main_menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu); // CHANGED HERE
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            SharedPreferences prefs = getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(KEY_USER_EMAIL);
            editor.apply();

            Intent intent = new Intent(rootActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}