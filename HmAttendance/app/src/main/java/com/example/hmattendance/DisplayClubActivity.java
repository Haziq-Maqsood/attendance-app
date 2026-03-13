package com.example.hmattendance;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hmattendance.models.Club;

import java.util.ArrayList;
import java.util.List;

public class DisplayClubActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ImageView backButton;
    private TextView toolbarTitle;
    private RecyclerView recyclerViewClubs;
    private TextView tvNoClubsMessage;

    private ClubDbHelper clubDbHelper;
    private ClubAdapter clubAdapter;
    private List<Club> clubList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_display_club);

        // --- Initialize Toolbar ---
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbarTitle = findViewById(R.id.toolbar_title);
        if (toolbarTitle != null) {
            toolbarTitle.setText(R.string.display_clubs_title);
        }

        backButton = findViewById(R.id.back_button);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish(); // Simply go back
            }
        });
        backButton.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });

        // --- Apply Window Insets ---
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- Initialize UI Elements ---
        recyclerViewClubs = findViewById(R.id.recycler_view_clubs);
        tvNoClubsMessage = findViewById(R.id.tv_no_clubs_message);

        // --- Setup RecyclerView ---
        recyclerViewClubs.setLayoutManager(new LinearLayoutManager(this));
        clubList = new ArrayList<>(); // Initialize with an empty list
        clubAdapter = new ClubAdapter(clubList);
        recyclerViewClubs.setAdapter(clubAdapter);

        // --- Initialize Database Helper ---
        clubDbHelper = new ClubDbHelper(this);

        // --- Load Clubs from Database ---
        loadClubs();
    }

    /**
     * Loads all club records from the database and updates the RecyclerView.
     */
    private void loadClubs() {
        List<Club> clubs = clubDbHelper.getAllClubs();
        if (clubs != null && !clubs.isEmpty()) {
            clubList.clear(); // Clear existing data
            clubList.addAll(clubs); // Add new data
            clubAdapter.notifyDataSetChanged(); // Notify adapter of data change
            tvNoClubsMessage.setVisibility(View.GONE);
            recyclerViewClubs.setVisibility(View.VISIBLE);
        } else {
            tvNoClubsMessage.setVisibility(View.VISIBLE);
            recyclerViewClubs.setVisibility(View.GONE);
            Toast.makeText(this, R.string.no_clubs_found, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload clubs in onResume in case data changed in another activity (e.g., AddClubActivity)
        loadClubs();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clubDbHelper != null) {
            clubDbHelper.close(); // Close the database connection
        }
    }
}