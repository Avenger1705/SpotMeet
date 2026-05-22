package com.spot.meet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private EventAdapter adapter;
    private List<Event> eventList;
    private FirebaseFirestore db;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Apply Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.history_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        tvEmpty = findViewById(R.id.tv_empty);

        rvHistory = findViewById(R.id.rv_history);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        eventList = new ArrayList<>();
        adapter = new EventAdapter(this, eventList, event -> {
            Intent intent = new Intent(HistoryActivity.this, EventDetailActivity.class);
            intent.putExtra("EVENT_ID", event.id);
            startActivity(intent);
        });
        rvHistory.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }

    private void loadHistory() {
        SharedPreferences prefs = getSharedPreferences("spotmeet_prefs", MODE_PRIVATE);
        String currentUsername = prefs.getString("user_username", "Unknown");

        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            event.id = doc.getId();
                            // Include ONLY if user created it OR user booked it
                            boolean isCreator = currentUsername.equals(event.creatorUsername);
                            boolean isBooked = event.bookedUsers != null && event.bookedUsers.contains(currentUsername);
                            
                            if (isCreator || isBooked) {
                                eventList.add(event);
                            }
                        }
                    }

                    eventList.sort((e1, e2) -> Long.compare(e2.eventTimestamp, e1.eventTimestamp));

                    adapter.notifyDataSetChanged();
                    
                    if (eventList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load history: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
