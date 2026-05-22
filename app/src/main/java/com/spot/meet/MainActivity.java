package com.spot.meet;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.google.firebase.firestore.ListenerRegistration;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvEvents;
    private EventAdapter adapter;
    private List<Event> eventList;
    private List<Event> allEvents; // Original data
    private FirebaseFirestore db;

    private EditText etSearch;
    private LinearLayout searchContainer;
    private String selectedFilter = "all";
    private FusedLocationProviderClient fusedLocationClient;
    private Location userLocation;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String NOTIF_CHANNEL_ID = "SpotMeet_Notifs";
    private ListenerRegistration notificationListener;
    private ListenerRegistration eventListener;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Apply Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        SharedPreferences prefs = getSharedPreferences("spotmeet_prefs", MODE_PRIVATE);
        String username = prefs.getString("user_username", "User");

        TextView tvWelcome = findViewById(R.id.tv_welcome);
        tvWelcome.setText("Hello, " + username + "! 👋");

        ImageView btnLogout = findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Logout", (dialog, which) -> logout())
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        ImageView btnHistory = findViewById(R.id.btn_history);
        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, HistoryActivity.class));
        });

        ImageView btnRefresh = findViewById(R.id.btn_refresh);
        btnRefresh.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing events...", Toast.LENGTH_SHORT).show();
            loadEvents(); // Manual reload if needed, although snaplistener handles it
        });

        FloatingActionButton fabAddEvent = findViewById(R.id.fab_add_event);
        fabAddEvent.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateEventActivity.class);
            startActivity(intent);
        });

        rvEvents = findViewById(R.id.rv_events);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        
        eventList = new ArrayList<>();
        allEvents = new ArrayList<>();
        adapter = new EventAdapter(this, eventList, event -> {
            Intent intent = new Intent(MainActivity.this, EventDetailActivity.class);
            intent.putExtra("EVENT_ID", event.id);
            startActivity(intent);
        });
        rvEvents.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        currentUsername = prefs.getString("user_username", "Unknown");

        createNotificationChannel();
        setupSearchAndFilters();
        startNotificationListener();
    }

    private void setupSearchAndFilters() {
        etSearch = findViewById(R.id.et_search);
        searchContainer = findViewById(R.id.search_container);
        
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEvents(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.filter_all).setOnClickListener(v -> selectFilter("all"));
        findViewById(R.id.filter_date).setOnClickListener(v -> selectFilter("date"));
        findViewById(R.id.filter_place).setOnClickListener(v -> selectFilter("place"));
        findViewById(R.id.filter_description).setOnClickListener(v -> selectFilter("description"));
        findViewById(R.id.filter_creator).setOnClickListener(v -> selectFilter("creator"));
        findViewById(R.id.filter_phone).setOnClickListener(v -> selectFilter("phone"));
        findViewById(R.id.filter_closest).setOnClickListener(v -> selectFilter("closest"));
        
        findViewById(R.id.btn_filters_toggle).setOnClickListener(v -> {
            View filterScroll = findViewById(R.id.filter_scroll);
            if (filterScroll.getVisibility() == View.VISIBLE) {
                filterScroll.setVisibility(View.GONE);
            } else {
                filterScroll.setVisibility(View.VISIBLE);
            }
        });
    }

    private void selectFilter(String filter) {
        if (selectedFilter.equals(filter) && !filter.equals("all")) {
            selectedFilter = "all";
        } else {
            selectedFilter = filter;
        }
        
        // Reset styles
        int[] filterIds = {R.id.filter_all, R.id.filter_date, R.id.filter_place, 
                           R.id.filter_description, R.id.filter_creator, R.id.filter_phone, R.id.filter_closest};
        
        for (int id : filterIds) {
            TextView tv = findViewById(id);
            tv.setBackgroundResource(R.drawable.bg_pill);
            tv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tv.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        // Apply style to selected
        int selectedId = R.id.filter_all;
        switch (selectedFilter) {
            case "date": selectedId = R.id.filter_date; break;
            case "place": selectedId = R.id.filter_place; break;
            case "description": selectedId = R.id.filter_description; break;
            case "creator": selectedId = R.id.filter_creator; break;
            case "phone": selectedId = R.id.filter_phone; break;
            case "closest": selectedId = R.id.filter_closest; break;
        }
        
        TextView selectedTv = findViewById(selectedId);
        selectedTv.setBackgroundResource(R.drawable.bg_pill_selected);
        selectedTv.setTextColor(ContextCompat.getColor(this, R.color.white));
        selectedTv.setTypeface(null, android.graphics.Typeface.BOLD);

        if (selectedFilter.equals("closest")) {
            checkLocationAndFilter();
        } else {
            filterEvents(etSearch.getText().toString());
        }
    }

    private void checkLocationAndFilter() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    userLocation = location;
                    filterEvents(etSearch.getText().toString());
                } else {
                    Toast.makeText(this, "Could not get your location.", Toast.LENGTH_SHORT).show();
                    selectFilter("all");
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationAndFilter();
            } else {
                Toast.makeText(this, "Location permission denied. Cannot use 'Closest' filter.", Toast.LENGTH_SHORT).show();
                selectFilter("all");
            }
        }
    }

    private void filterEvents(String query) {
        List<Event> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();

        for (Event event : allEvents) {
            boolean matches = false;
            
            if (lowerQuery.isEmpty()) {
                matches = true;
            } else {
                switch (selectedFilter) {
                    case "all":
                        matches = (event.title != null && event.title.toLowerCase().contains(lowerQuery)) ||
                                  (event.description != null && event.description.toLowerCase().contains(lowerQuery)) ||
                                  (event.locationName != null && event.locationName.toLowerCase().contains(lowerQuery)) ||
                                  (event.creatorUsername != null && event.creatorUsername.toLowerCase().contains(lowerQuery)) ||
                                  (event.phone != null && event.phone.toLowerCase().contains(lowerQuery)) ||
                                  new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(event.eventTimestamp)).toLowerCase().contains(lowerQuery);
                        break;
                    case "date":
                        matches = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(event.eventTimestamp)).toLowerCase().contains(lowerQuery);
                        break;
                    case "place":
                        matches = (event.locationName != null && event.locationName.toLowerCase().contains(lowerQuery));
                        break;
                    case "description":
                        matches = (event.description != null && event.description.toLowerCase().contains(lowerQuery));
                        break;
                    case "creator":
                        matches = (event.creatorUsername != null && event.creatorUsername.toLowerCase().contains(lowerQuery));
                        break;
                    case "phone":
                        matches = (event.phone != null && event.phone.toLowerCase().contains(lowerQuery));
                        break;
                    case "closest":
                        matches = (event.title != null && event.title.toLowerCase().contains(lowerQuery)) || 
                                  (event.description != null && event.description.toLowerCase().contains(lowerQuery));
                        break;
                }
            }

            if (matches) {
                filtered.add(event);
            }
        }

        if (selectedFilter.equals("closest") && userLocation != null) {
            Collections.sort(filtered, (e1, e2) -> {
                float[] res1 = new float[1];
                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), e1.lat, e1.lng, res1);
                float[] res2 = new float[1];
                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), e2.lat, e2.lng, res2);
                return Float.compare(res1[0], res2[0]);
            });
        } else {
            // Default sort by timestamp
            filtered.sort((e1, e2) -> Long.compare(e2.timestamp, e1.timestamp));
        }

        eventList.clear();
        eventList.addAll(filtered);
        adapter.updateList(eventList, lowerQuery);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    private void loadEvents() {
        if (eventListener != null) {
            eventListener.remove();
        }

        eventListener = db.collection("events")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Error listening for updates", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        allEvents.clear();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Event event = doc.toObject(Event.class);
                            if (event != null) {
                                event.id = doc.getId();
                                // Don't show past events to normal users
                                if (event.eventTimestamp > System.currentTimeMillis()) {
                                    allEvents.add(event);
                                }
                            }
                        }
                        filterEvents(etSearch.getText().toString());
                    }
                });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIF_CHANNEL_ID,
                    "SpotMeet Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications about cancelled events and other updates");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private void startNotificationListener() {
        if (currentUsername == null || currentUsername.equals("Unknown")) return;

        notificationListener = db.collection("user_notifications")
                .whereEqualTo("recipient", currentUsername)
                .whereEqualTo("read", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        android.util.Log.w("MainActivity", "Listen failed.", e);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String title = doc.getString("title");
                            String message = doc.getString("message");
                            showSystemNotification(title, message);

                            // Mark as read
                            doc.getReference().update("read", true);
                        }
                    }
                });
    }

    private void showSystemNotification(String title, String message) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round) // Using app icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) {
            notificationListener.remove();
        }
        if (eventListener != null) {
            eventListener.remove();
        }
    }

    private void logout() {
        // Clear session
        if (notificationListener != null) {
            notificationListener.remove();
        }
        if (eventListener != null) {
            eventListener.remove();
        }
        getSharedPreferences("spotmeet_prefs", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}