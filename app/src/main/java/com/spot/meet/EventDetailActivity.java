package com.spot.meet;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import org.osmdroid.views.overlay.Marker;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class EventDetailActivity extends AppCompatActivity {

    private static final String TAG = "EventDetail";
    private static final int REQ_NOTIF = 101;

    private ImageView ivEventImage;
    private TextView tvTitle, tvDesc, tvAboutDesc, tvPhone, tvPlacesInfo, tvEventDatetime, tvCreator;
    private Button btnBook, btnReminder, btnCall, btnGoogleMaps;
    private MapView mapView;
    private FirebaseFirestore db;

    private String eventId;
    private String currentUsername;
    private String currentUserEmail;
    private long eventTimestamp = 0;
    private String eventTitle = "";
    private double currentEventLat = 0.0;
    private double currentEventLng = 0.0;
    private String currentCreatorPhone = "";
    private String currentMainImageUrl = "";
    private String currentThumbnailUrl = "";
    private List<String> currentExtraImages = new ArrayList<>();

    private List<String> currentBookedUsers = new ArrayList<>();
    private String eventCreator = "";
    private boolean isCurrentUserAdmin = false;
    private DatabaseHelper dbHelper;
    private int galleryIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(getApplicationContext(),
                getSharedPreferences("osmdroid", MODE_PRIVATE));

        setContentView(R.layout.activity_event_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.event_detail_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        dbHelper = new DatabaseHelper(this);

        SharedPreferences prefs = getSharedPreferences("spotmeet_prefs", MODE_PRIVATE);
        currentUsername  = prefs.getString("user_username", "Unknown");
        currentUserEmail = prefs.getString("user_email", "unknown@test.com");

        eventId = getIntent().getStringExtra("EVENT_ID");
        if (eventId == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ImageView btnBack = findViewById(R.id.btn_back);
        ivEventImage    = findViewById(R.id.iv_event_image);
        tvTitle         = findViewById(R.id.tv_title);
        tvCreator       = findViewById(R.id.tv_creator);
        tvDesc          = findViewById(R.id.tv_desc);
        tvAboutDesc     = findViewById(R.id.tv_about_desc);
        tvPhone         = findViewById(R.id.tv_phone);
        tvPlacesInfo    = findViewById(R.id.tv_places_info);
        tvEventDatetime = findViewById(R.id.tv_event_datetime);
        mapView         = findViewById(R.id.map_view);
        btnBook         = findViewById(R.id.btn_book);
        btnReminder     = findViewById(R.id.btn_reminder);
        btnCall         = findViewById(R.id.btn_call);
        btnGoogleMaps   = findViewById(R.id.btn_google_maps);

        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);
        mapView.getZoomController().setVisibility(
                org.osmdroid.views.CustomZoomButtonsController.Visibility.ALWAYS);

        createNotificationChannel();
        requestNotificationPermission();

        btnBack.setOnClickListener(v -> finish());
        btnBook.setOnClickListener(v -> bookEvent());

        btnReminder.setOnClickListener(v -> scheduleReminder());

        btnCall.setOnClickListener(v -> {
            if (!currentCreatorPhone.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_DIAL,
                        android.net.Uri.parse("tel:" + currentCreatorPhone)));
            } else {
                Toast.makeText(this, "No phone number available for this event.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        btnGoogleMaps.setOnClickListener(v -> {
            if (currentEventLat != 0 && currentEventLng != 0) {
                String uri = "geo:" + currentEventLat + "," + currentEventLng + "?q=" + currentEventLat + "," + currentEventLng + "(" + eventTitle + ")";
                Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri)));
                }
            }
        });

        ivEventImage.setOnClickListener(v -> {
            galleryIndex = 0;
            showImageGallery();
        });

        loadEventDetails();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    ReminderReceiver.CHANNEL_ID,
                    "Event Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            ch.setDescription("Reminders for your SpotMeet events");
            ch.enableVibration(true);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
            }
        }
    }

    // ─── Schedule AlarmManager reminder ─────────────────────────────────────

    private void scheduleReminder() {
        if (eventTimestamp <= 0) {
            Toast.makeText(this, "Event date is not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
                Toast.makeText(this, "Please allow notifications then try again.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String[] options = {"10 minutes before", "30 minutes before", "1 hour before", "2 hours before", "1 day before"};
        long[] offsets = {
                10 * 60 * 1000L,
                30 * 60 * 1000L,
                60 * 60 * 1000L,
                2 * 60 * 60 * 1000L,
                24 * 60 * 60 * 1000L
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Remind me...")
                .setItems(options, (dialog, which) -> {
                    long offset = offsets[which];
                    long reminderAt = eventTimestamp - offset;

                    if (reminderAt <= System.currentTimeMillis()) {
                        Toast.makeText(this, "This time has already passed!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    setAlarm(reminderAt);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setAlarm(long reminderAt) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_TITLE,    eventTitle);
        intent.putExtra(ReminderReceiver.EXTRA_EVENT_ID, eventId);

        PendingIntent pi = PendingIntent.getBroadcast(
                this,
                eventId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pi);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, reminderAt, pi);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        Toast.makeText(this,
                "Reminder set for " + sdf.format(new Date(reminderAt)),
                Toast.LENGTH_LONG).show();

        btnReminder.setText("REMINDER SET ✓");
        // btnReminder.setEnabled(false); // keep it enabled so they can change it if they want
    }

    // ─── Image gallery ───────────────────────────────────────────────────────

    private void showImageGallery() {
        List<String> all = new ArrayList<>();
        if (!currentMainImageUrl.isEmpty()) all.add(currentMainImageUrl);
        all.addAll(currentExtraImages);

        if (all.isEmpty()) {
            Toast.makeText(this, "No images available.", Toast.LENGTH_SHORT).show();
            return;
        }

        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        
        android.widget.RelativeLayout root = new android.widget.RelativeLayout(this);
        root.setBackgroundColor(0xFF000000);

        // Apply Window Insets for Fullscreen Dialog
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Main Image View
        ImageView mainIv = new ImageView(this);
        mainIv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        android.widget.RelativeLayout.LayoutParams mainLP = new android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT);
        root.addView(mainIv, mainLP);

        // Close button
        TextView closeBtn = new TextView(this);
        closeBtn.setText("✕");
        closeBtn.setTextColor(0xFFFFFFFF);
        closeBtn.setTextSize(30f);
        closeBtn.setPadding(dp(20), dp(20), dp(20), dp(20));
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        root.addView(closeBtn);

        // Navigation Arrows
        TextView prevBtn = new TextView(this);
        prevBtn.setText("❮");
        prevBtn.setTextColor(0xFFFFFFFF);
        prevBtn.setTextSize(40f);
        prevBtn.setPadding(dp(20), dp(50), dp(20), dp(50));
        android.widget.RelativeLayout.LayoutParams prevLP = new android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT);
        prevLP.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
        prevLP.addRule(android.widget.RelativeLayout.ALIGN_PARENT_START);
        root.addView(prevBtn, prevLP);

        TextView nextBtn = new TextView(this);
        nextBtn.setText("❯");
        nextBtn.setTextColor(0xFFFFFFFF);
        nextBtn.setTextSize(40f);
        nextBtn.setPadding(dp(20), dp(50), dp(20), dp(50));
        android.widget.RelativeLayout.LayoutParams nextLP = new android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT);
        nextLP.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
        nextLP.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END);
        root.addView(nextBtn, nextLP);

        // Counter
        TextView counterTv = new TextView(this);
        counterTv.setTextColor(0xFFFFFFFF);
        counterTv.setTextSize(16f);
        android.widget.RelativeLayout.LayoutParams counterLP = new android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT);
        counterLP.addRule(android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM);
        counterLP.addRule(android.widget.RelativeLayout.CENTER_HORIZONTAL);
        counterLP.setMargins(0, 0, 0, dp(40));
        root.addView(counterTv, counterLP);

        java.lang.Runnable updateGallery = () -> {
            String url = all.get(galleryIndex);
            counterTv.setText((galleryIndex + 1) + " / " + all.size());
            prevBtn.setVisibility(galleryIndex > 0 ? View.VISIBLE : View.GONE);
            nextBtn.setVisibility(galleryIndex < all.size() - 1 ? View.VISIBLE : View.GONE);

            if (url.contains("ngrok") || url.contains("onrender")) {
                com.bumptech.glide.load.model.GlideUrl glideUrl = new com.bumptech.glide.load.model.GlideUrl(
                        url,
                        new com.bumptech.glide.load.model.LazyHeaders.Builder()
                                .addHeader("ngrok-skip-browser-warning", "69420")
                                .addHeader("User-Agent", "Mozilla/5.0")
                                .build()
                );
                Glide.with(this)
                        .load(glideUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.bg_image_placeholder)
                        .into(mainIv);
            } else {
                Glide.with(this)
                        .load(url)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.bg_image_placeholder)
                        .into(mainIv);
            }
        };

        prevBtn.setOnClickListener(v -> {
            if (galleryIndex > 0) {
                galleryIndex--;
                updateGallery.run();
            }
        });

        nextBtn.setOnClickListener(v -> {
            if (galleryIndex < all.size() - 1) {
                galleryIndex++;
                updateGallery.run();
            }
        });

        updateGallery.run();
        dialog.setContentView(root);
        dialog.show();
    }

    // ─── Load event ──────────────────────────────────────────────────────────

    private void loadEventDetails() {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        if (event != null) {
                            eventTitle         = event.title != null ? event.title : "";
                            eventTimestamp     = event.eventTimestamp;
                            currentEventLat    = event.lat;
                            currentEventLng    = event.lng;
                            currentCreatorPhone = event.phone != null ? event.phone : "";
                            currentMainImageUrl = event.mainImageUrl != null ? event.mainImageUrl : "";
                            currentThumbnailUrl = event.thumbnailUrl != null ? event.thumbnailUrl : "";


                            if (event.otherImagesUrls != null) {
                                currentExtraImages.clear();
                                currentExtraImages.addAll(event.otherImagesUrls);
                            }

                            if (event.bookedUsers != null) {
                                currentBookedUsers.clear();
                                currentBookedUsers.addAll(event.bookedUsers);
                            }

                            tvTitle.setText(eventTitle);
                            tvCreator.setText("Created by " +
                                    (event.creatorUsername != null ? event.creatorUsername : "Unknown"));
                            tvDesc.setText(event.description);
                            if (tvAboutDesc != null) tvAboutDesc.setText(event.description);
                            tvPhone.setText(currentCreatorPhone);
                            tvPlacesInfo.setText("Available Places: "
                                    + event.availablePlaces + " / " + event.totalPlaces);

                            // ── Check permissions for Edit/Delete ──
                            SharedPreferences prefs = getSharedPreferences("spotmeet_prefs", MODE_PRIVATE);
                            String loggedInUser = prefs.getString("user_username", "");
                            isCurrentUserAdmin = prefs.getBoolean("is_admin", false);
                            eventCreator = event.creatorUsername != null ? event.creatorUsername : "";

                            if (loggedInUser.equalsIgnoreCase(eventCreator) || isCurrentUserAdmin) {
                                findViewById(R.id.ll_management).setVisibility(View.VISIBLE);
                                findViewById(R.id.btn_delete_event).setOnClickListener(v -> confirmDelete());
                                findViewById(R.id.btn_edit_event).setOnClickListener(v -> {
                                    Intent intent = new Intent(EventDetailActivity.this, CreateEventActivity.class);
                                    intent.putExtra("EDIT_MODE", true);
                                    intent.putExtra("EVENT_ID", eventId);
                                    startActivity(intent);
                                });
                            }

                            if (event.eventTimestamp > 0) {
                                SimpleDateFormat sdf =
                                        new SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault());
                                tvEventDatetime.setText("Event Time: "
                                        + sdf.format(new Date(event.eventTimestamp)));
                            } else {
                                tvEventDatetime.setText("Time: TBA");
                            }

                            // Load main image with Glide (with placeholder + error + logging)
                            if (!currentMainImageUrl.isEmpty()) {
                                Log.d(TAG, "Loading main image: " + currentMainImageUrl);
                                if (currentMainImageUrl.contains("ngrok") || currentMainImageUrl.contains("onrender")) {
                                    com.bumptech.glide.load.model.Headers headers = new com.bumptech.glide.load.model.LazyHeaders.Builder()
                                            .addHeader("ngrok-skip-browser-warning", "69420")
                                            .addHeader("User-Agent", "Mozilla/5.0")
                                            .build();

                                    com.bumptech.glide.load.model.GlideUrl mainGlideUrl = new com.bumptech.glide.load.model.GlideUrl(
                                            currentMainImageUrl, headers);

                                    Glide.with(this)
                                            .load(mainGlideUrl)
                                            .thumbnail(Glide.with(this).load(currentThumbnailUrl.contains("ngrok") || currentThumbnailUrl.contains("onrender") 
                                                    ? new com.bumptech.glide.load.model.GlideUrl(currentThumbnailUrl, headers) 
                                                    : currentThumbnailUrl))
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                            .placeholder(R.drawable.bg_image_placeholder)
                                            .error(R.drawable.bg_image_placeholder)
                                            .centerCrop()
                                            .into(ivEventImage);
                                } else {
                                    Glide.with(this)
                                            .load(currentMainImageUrl)
                                            .thumbnail(Glide.with(this).load(currentThumbnailUrl))
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                            .placeholder(R.drawable.bg_image_placeholder)
                                            .error(R.drawable.bg_image_placeholder)
                                            .centerCrop()
                                            .into(ivEventImage);
                                }

                            }

                            // ── Populating thumbnails for "Optional Photos" ──
                            android.widget.HorizontalScrollView hsvExtra = findViewById(R.id.hsv_extra_images);
                            android.widget.LinearLayout llExtraContainer = findViewById(R.id.ll_extra_images_container);
                            llExtraContainer.removeAllViews();

                            if (currentExtraImages != null && !currentExtraImages.isEmpty()) {
                                hsvExtra.setVisibility(View.VISIBLE);
                                for (int i = 0; i < currentExtraImages.size(); i++) {
                                    final int index = i + 1; // Since main image is at 0 in gallery
                                    String url = currentExtraImages.get(i);
                                    
                                    ImageView thumbnail = new ImageView(this);
                                    android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(dp(80), dp(80));
                                    lp.setMargins(0, 0, dp(12), 0);
                                    thumbnail.setLayoutParams(lp);
                                    thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                    thumbnail.setBackgroundColor(0xFF2A2A4A);
                                    
                                    // Make it rounded
                                    thumbnail.setClipToOutline(true);
                                    thumbnail.setOutlineProvider(new android.view.ViewOutlineProvider() {
                                        @Override
                                        public void getOutline(View view, android.graphics.Outline outline) {
                                            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(8));
                                        }
                                    });

                                    if (url.contains("ngrok") || url.contains("onrender")) {
                                        com.bumptech.glide.load.model.GlideUrl thumbUrl = new com.bumptech.glide.load.model.GlideUrl(
                                                url,
                                                new com.bumptech.glide.load.model.LazyHeaders.Builder()
                                                        .addHeader("ngrok-skip-browser-warning", "69420")
                                                        .addHeader("User-Agent", "Mozilla/5.0")
                                                        .build()
                                        );

                                        Glide.with(this)
                                                .load(thumbUrl)
                                                .centerCrop()
                                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                                .placeholder(R.drawable.bg_image_placeholder)
                                                .into(thumbnail);
                                    } else {
                                        Glide.with(this)
                                                .load(url)
                                                .centerCrop()
                                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                                .placeholder(R.drawable.bg_image_placeholder)
                                                .into(thumbnail);
                                    }

                                    thumbnail.setOnClickListener(v -> {
                                        galleryIndex = index;
                                        showImageGallery();
                                    });
                                    llExtraContainer.addView(thumbnail);
                                }
                            } else {
                                hsvExtra.setVisibility(View.GONE);
                            }

                            mapView.getController().setZoom(16.0);
                            GeoPoint eventPoint = new GeoPoint(event.lat, event.lng);
                            mapView.getController().setCenter(eventPoint);

                            // Add actual marker
                            mapView.getOverlays().clear();
                            Marker marker = new Marker(mapView);
                            marker.setPosition(eventPoint);
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            marker.setTitle(eventTitle);
                            marker.setSnippet(event.locationName);
                            mapView.getOverlays().add(marker);
                            mapView.invalidate();

                            // ── Button states ────────────────────────────────
                            boolean isPast    = event.eventTimestamp > 0
                                    && event.eventTimestamp < System.currentTimeMillis();
                            boolean hasBooked = event.bookedUsers != null
                                    && event.bookedUsers.contains(currentUsername);
                            boolean isCreator = event.creatorUsername != null
                                    && event.creatorUsername.equals(currentUsername);

                            if (isPast) {
                                btnBook.setEnabled(false);
                                btnBook.setText("EVENT COMPLETED");
                                btnReminder.setVisibility(View.GONE);
                            } else if (hasBooked) {
                                btnBook.setEnabled(false);
                                btnBook.setText("ALREADY BOOKED");
                                btnReminder.setVisibility(View.VISIBLE);
                            } else if (event.availablePlaces <= 0) {
                                btnBook.setEnabled(false);
                                btnBook.setText("SOLD OUT");
                                btnReminder.setVisibility(View.GONE);
                            } else if (isCreator) {
                                btnBook.setEnabled(false);
                                btnBook.setText("YOUR EVENT");
                                btnReminder.setVisibility(View.VISIBLE);
                            } else {
                                btnBook.setEnabled(true);
                                btnBook.setText("BOOK NOW");
                                btnReminder.setVisibility(View.GONE);
                            }
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load details", Toast.LENGTH_SHORT).show());
    }

    // ─── dp helper ───────────────────────────────────────────────────────────

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // ─── Book event ──────────────────────────────────────────────────────────

    private void bookEvent() {
        btnBook.setEnabled(false);
        btnBook.setText("Processing...");

        DocumentReference eventRef = db.collection("events").document(eventId);

        db.runTransaction(transaction -> {
            Event event = transaction.get(eventRef).toObject(Event.class);
            if (event == null) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException(
                        "Event not found",
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND);
            }
            if (event.availablePlaces <= 0) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException(
                        "Sold out",
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED);
            }
            if (event.bookedUsers != null && event.bookedUsers.contains(currentUsername)) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException(
                        "Already booked",
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.ALREADY_EXISTS);
            }

            transaction.update(eventRef, "availablePlaces", event.availablePlaces - 1);

            List<String> updated = event.bookedUsers != null
                    ? new ArrayList<>(event.bookedUsers) : new ArrayList<>();
            updated.add(currentUsername);
            transaction.update(eventRef, "bookedUsers", updated);

            return null;

        }).addOnSuccessListener(aVoid -> {
            String reservationId = "EVT:" + eventId
                    + "_USR:" + currentUsername
                    + "_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

            EmailSender.sendBookingEmail(
                    currentUserEmail, currentUsername, eventTitle,
                    currentEventLat, currentEventLng, reservationId,
                    new EmailSender.EmailCallback() {
                        @Override public void onSuccess(String code) {
                            runOnUiThread(() -> {
                                Toast.makeText(EventDetailActivity.this,
                                        "Check your email for QR ticket!", Toast.LENGTH_LONG).show();
                                loadEventDetails();
                            });
                        }
                        @Override public void onFailure(String message) {
                            runOnUiThread(() -> {
                                Toast.makeText(EventDetailActivity.this,
                                        "Booked, but email failed to send.", Toast.LENGTH_LONG).show();
                                loadEventDetails();
                            });
                        }
                    });

        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to book: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            btnBook.setEnabled(true);
            btnBook.setText("BOOK NOW");
        });
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
    private void confirmDelete() {
        // If it's an admin deleting someone else's event, ask for a reason
        SharedPreferences prefs = getSharedPreferences("spotmeet_prefs", MODE_PRIVATE);
        String loggedInUser = prefs.getString("user_username", "");

        if (isCurrentUserAdmin && !loggedInUser.equalsIgnoreCase(eventCreator)) {
            showAdminDeleteDialog();
        } else {
            // Normal creator deletion
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Event")
                    .setMessage("Are you sure you want to delete your event?")
                    .setPositiveButton("Delete", (dialog, which) -> executeDeletion(null))
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void showAdminDeleteDialog() {
        android.widget.EditText etReason = new android.widget.EditText(this);
        etReason.setHint("e.g. Inappropriate content, spam, etc.");
        
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(24), dp(8), dp(24), dp(8));
        etReason.setLayoutParams(lp);
        container.addView(etReason);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete for Violation")
                .setMessage("Please provide a reason. This will be sent to the creator.")
                .setView(container)
                .setPositiveButton("Confirm Deletion", (dialog, which) -> {
                    String reason = etReason.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(this, "Reason is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    executeDeletion(reason);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeDeletion(String adminReason) {
        // ── Step 1: Cancel any local reminder set for this event ──
        cancelLocalReminder();

        // ── Step 2: Notify Booked Users ──
        if (currentBookedUsers != null && !currentBookedUsers.isEmpty()) {
            for (String username : currentBookedUsers) {
                // Email
                dbHelper.getUserEmailByUsername(username, email -> {
                    if (email != null) {
                        sendCancellationEmailToBookedUser(email);
                    }
                });

                // App Notification (Firestore-based)
                Map<String, Object> notif = new HashMap<>();
                notif.put("recipient", username);
                notif.put("message", "Event '" + eventTitle + "' has been cancelled.");
                notif.put("title", "Event Cancelled");
                notif.put("eventId", eventId);
                notif.put("timestamp", com.google.firebase.Timestamp.now());
                notif.put("read", false);

                db.collection("user_notifications").add(notif);
            }
        }

        // ── Step 3: Handle Admin/Creator specific notification ──
        if (adminReason != null) {
            dbHelper.getUserEmailByUsername(eventCreator, email -> {
                if (email != null) {
                    sendDeletionEmail(email, adminReason);
                }
                performFirestoreDelete();
            });
        } else {
            performFirestoreDelete();
        }
    }

    private void cancelLocalReminder() {
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                this,
                eventId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am != null) {
            am.cancel(pi);
            Log.d("EventDetail", "Cancelled local reminder for " + eventId);
        }
    }

    private void sendCancellationEmailToBookedUser(String userEmail) {
        EmailSender.sendCancellationNotice(userEmail, eventTitle, new EmailSender.EmailCallback() {
            @Override public void onSuccess(String code) {}
            @Override public void onFailure(String message) {}
        });
    }

    private void sendDeletionEmail(String creatorEmail, String reason) {
        EmailSender.sendDeletionNotice(creatorEmail, eventTitle, reason, new EmailSender.EmailCallback() {
            @Override public void onSuccess(String code) { /* ignored */ }
            @Override public void onFailure(String message) { /* ignored */ }
        });
    }

    private void performFirestoreDelete() {
        db.collection("events").document(eventId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Event deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
