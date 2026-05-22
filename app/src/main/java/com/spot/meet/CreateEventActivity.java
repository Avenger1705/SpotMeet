package com.spot.meet;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CreateEventActivity extends AppCompatActivity {

    private static final String TAG              = "CreateEvent";
    private static final String IMAGE_UPLOAD_URL =
            "http://192.168.1.11:5001/upload-image";

    private ImageView  ivEventImage;
    private EditText   etTitle, etDesc, etPhone, etPlaces, etLat, etLng, etLocationName;
    private TextView   tvDate, tvTime, tvPickedLocation;
    private Button     btnCreate, btnAddMoreImages;
    private android.widget.LinearLayout llMoreImages;

    private Uri        selectedImageUri = null;
    private List<Uri>  moreImageUris    = new ArrayList<>();
    private FirebaseFirestore db;
    private String     currentUsername;
    private Calendar   eventCalendar;
    private boolean    isEditMode = false;
    private String     editEventId = null;
    private String     currentMainImageUrl = "";
    private String     currentThumbnailUrl = "";
    private List<String> currentExtraImages = new ArrayList<>();


    private final ActivityResultLauncher<String> pickMedia =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this).load(uri).centerCrop().into(ivEventImage);
                }
            });

    private final ActivityResultLauncher<String> pickMultipleMedia =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    moreImageUris.addAll(uris);
                    llMoreImages.removeAllViews();
                    for (Uri uri : moreImageUris) {
                        ImageView iv = new ImageView(this);
                        android.widget.LinearLayout.LayoutParams lp =
                                new android.widget.LinearLayout.LayoutParams(200, 200);
                        lp.setMargins(0, 0, 16, 0);
                        iv.setLayoutParams(lp);
                        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        Glide.with(this).load(uri).into(iv);
                        llMoreImages.addView(iv);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> pickLocation =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    double lat    = result.getData().getDoubleExtra("LATITUDE", 0);
                    double lng    = result.getData().getDoubleExtra("LONGITUDE", 0);
                    String address = result.getData().getStringExtra("ADDRESS");

                    etLat.setText(String.valueOf(lat));
                    etLng.setText(String.valueOf(lng));
                    if (address != null && !address.trim().isEmpty()) {
                        etLocationName.setText(address);
                    }
                    tvPickedLocation.setText("Location imported from Map");
                    tvPickedLocation.setTextColor(0xFF00C853);
                }
            });

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        db             = FirebaseFirestore.getInstance();
        eventCalendar  = Calendar.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.create_event_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        SharedPreferences prefs = getSharedPreferences("spotmeet_prefs", MODE_PRIVATE);
        currentUsername = prefs.getString("user_username", "Unknown");

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        ivEventImage    = findViewById(R.id.iv_event_image);
        etTitle         = findViewById(R.id.et_title);
        etDesc          = findViewById(R.id.et_desc);
        etPhone         = findViewById(R.id.et_phone);
        etPlaces        = findViewById(R.id.et_places);
        etLocationName  = findViewById(R.id.et_location_name);
        tvDate          = findViewById(R.id.tv_date);
        tvTime          = findViewById(R.id.tv_time);
        etLat           = findViewById(R.id.et_lat);
        etLng           = findViewById(R.id.et_lng);
        btnCreate       = findViewById(R.id.btn_create);
        btnAddMoreImages = findViewById(R.id.btn_add_more_images);
        llMoreImages    = findViewById(R.id.ll_more_images);
        tvPickedLocation = findViewById(R.id.tv_picked_location);

        isEditMode = getIntent().getBooleanExtra("EDIT_MODE", false);
        editEventId = getIntent().getStringExtra("EVENT_ID");

        if (isEditMode && editEventId != null) {
            ((TextView)findViewById(R.id.tv_create_title)).setText("Edit Event");
            btnCreate.setText("UPDATE EVENT");
            loadEventForEditing();
        }

        tvDate.setOnClickListener(v -> showDatePicker());
        tvTime.setOnClickListener(v -> showTimePicker());
        findViewById(R.id.btn_pick_map).setOnClickListener(v ->
                pickLocation.launch(new Intent(this, MapPickerActivity.class)));
        ivEventImage.setOnClickListener(v -> pickMedia.launch("image/*"));
        btnAddMoreImages.setOnClickListener(v -> pickMultipleMedia.launch("image/*"));
        btnCreate.setOnClickListener(v -> handleCreateEvent());
    }

    
    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            eventCalendar.set(Calendar.YEAR, year);
            eventCalendar.set(Calendar.MONTH, month);
            eventCalendar.set(Calendar.DAY_OF_MONTH, day);
            tvDate.setText(day + "/" + (month + 1) + "/" + year);
        }, eventCalendar.get(Calendar.YEAR),
                eventCalendar.get(Calendar.MONTH),
                eventCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(this, (view, hour, minute) -> {
            eventCalendar.set(Calendar.HOUR_OF_DAY, hour);
            eventCalendar.set(Calendar.MINUTE, minute);
            tvTime.setText(String.format("%02d:%02d", hour, minute));
        }, eventCalendar.get(Calendar.HOUR_OF_DAY),
                eventCalendar.get(Calendar.MINUTE), true).show();
    }

    
    private void handleCreateEvent() {
        String title        = etTitle.getText().toString().trim();
        String desc         = etDesc.getText().toString().trim();
        String phone        = etPhone.getText().toString().trim();
        String locationName = etLocationName.getText().toString().trim();
        String placesStr    = etPlaces.getText().toString().trim();
        String latStr       = etLat.getText().toString().trim();
        String lngStr       = etLng.getText().toString().trim();

        if (title.isEmpty() || desc.isEmpty() || phone.isEmpty()
                || locationName.isEmpty() || placesStr.isEmpty()
                || latStr.isEmpty() || lngStr.isEmpty()
                || tvDate.getText().toString().equals("Select Date")
                || tvTime.getText().toString().equals("Select Time")) {
            Toast.makeText(this, "Please fill all fields including Date and Time",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri == null && !isEditMode) {
            Toast.makeText(this, "Please select a main image", Toast.LENGTH_SHORT).show();
            return;
        }

        int    places;
        double finalLat, finalLng;
        try {
            places   = Integer.parseInt(placesStr);
            finalLat = Double.parseDouble(latStr);
            finalLng = Double.parseDouble(lngStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format for places or coordinates",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (places <= 0) {
            Toast.makeText(this, "Number of places must be greater than 0",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        long finalEventTimestamp = eventCalendar.getTimeInMillis();

        btnCreate.setEnabled(false);
        btnCreate.setText("UPLOADING MAIN IMAGE…");

        if (selectedImageUri != null) {
            uploadToServer(selectedImageUri,
                (mainUrl, thumbUrl) -> {
                    handleExtraImagesAndSave(title, desc, phone, locationName, places,
                            finalLat, finalLng, mainUrl, thumbUrl, finalEventTimestamp);
                },
                e -> {
                    Log.e(TAG, "Main image upload failed", e);
                    Toast.makeText(this, "Main image upload failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    btnCreate.setEnabled(true);
                    btnCreate.setText(isEditMode ? "UPDATE EVENT" : "CREATE EVENT");
                });
        } else {
            handleExtraImagesAndSave(title, desc, phone, locationName, places,
                    finalLat, finalLng, currentMainImageUrl, currentThumbnailUrl, finalEventTimestamp);
        }
    }

    private void handleExtraImagesAndSave(String title, String desc, String phone,
                                         String locationName, int places,
                                         double finalLat, double finalLng,
                                         String mainUrl, String thumbUrl, long finalEventTimestamp) {
        if (moreImageUris.isEmpty()) {
            saveEventToFirestore(title, desc, phone, locationName, places,
                    finalLat, finalLng, mainUrl, thumbUrl, finalEventTimestamp, currentExtraImages);
        } else {
            uploadExtrasAndSave(title, desc, phone, locationName, places,
                    finalLat, finalLng, mainUrl, thumbUrl, finalEventTimestamp);
        }
    }


    
    private void uploadExtrasAndSave(String title, String desc, String phone, String locationName,
                                     int places, double lat, double lng,
                                     String mainUrl, String thumbUrl, long evTimestamp) {
        btnCreate.setText("UPLOADING EXTRA IMAGES…");

        int total                     = moreImageUris.size();
        AtomicInteger remaining       = new AtomicInteger(total);
        List<String> extraUrls        = java.util.Collections.synchronizedList(new ArrayList<>());

        for (Uri uri : moreImageUris) {
            uploadToServer(uri,
                    (url, ignoredThumb) -> {
                        extraUrls.add(url);
                        if (remaining.decrementAndGet() == 0) {
                            saveEventToFirestore(title, desc, phone, locationName, places,
                                    lat, lng, mainUrl, thumbUrl, evTimestamp, new ArrayList<>(extraUrls));
                        }
                    },

                    err -> {
                        Log.e(TAG, "Extra image upload failed", err);
                        if (remaining.decrementAndGet() == 0) {
                            saveEventToFirestore(title, desc, phone, locationName, places,
                                    lat, lng, mainUrl, thumbUrl, evTimestamp, new ArrayList<>(extraUrls));
                        }
                    });
        }
    }

    
    public interface UploadCallback {
        void onSuccess(String originalUrl, String thumbnailUrl);
    }

    
    private void uploadToServer(Uri uri,
                                UploadCallback onSuccess,
                                java.util.function.Consumer<Exception> onError) {
        new Thread(() -> {
            try {
                String[] results = performMultipartUpload(uri);
                runOnUiThread(() -> onSuccess.onSuccess(results[0], results[1]));
            } catch (Exception e) {
                runOnUiThread(() -> onError.accept(e));
            }
        }).start();
    }

    private String[] performMultipartUpload(Uri uri) throws Exception {
        String boundary = "----SpotMeet" + System.currentTimeMillis();
        String CRLF     = "\r\n";

        URL url = new URL(IMAGE_UPLOAD_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type",
                "multipart/form-data; boundary=" + boundary);
        conn.setRequestProperty("ngrok-skip-browser-warning", "69420");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setConnectTimeout(60_000);
        conn.setReadTimeout(60_000);

        try (OutputStream out = conn.getOutputStream()) {
            String partHeader =
                    "--" + boundary + CRLF
                    + "Content-Disposition: form-data; name=\"image\"; filename=\"img_"
                    + System.currentTimeMillis() + ".jpg\"" + CRLF
                    + "Content-Type: image/jpeg" + CRLF
                    + CRLF;
            out.write(partHeader.getBytes(StandardCharsets.UTF_8));


            byte[] compressedBytes = BitmapUtils.getCompressedImageBytes(this, uri, 1280, 1280, 80);
            if (compressedBytes != null) {
                out.write(compressedBytes);
            } else {
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    if (in == null) throw new Exception("Cannot open image URI");
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                }
            }

            out.write((CRLF + "--" + boundary + "--" + CRLF)
                    .getBytes(StandardCharsets.UTF_8));
            out.flush();
        }

        int status = conn.getResponseCode();
        if (status == 200 || status == 201) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream in = conn.getInputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) != -1) baos.write(buf, 0, n);
            }
            String responseBody = baos.toString("UTF-8");
            Log.d(TAG, "Upload response: " + responseBody);
            JSONObject json = new JSONObject(responseBody);
            return new String[]{json.getString("url"), json.getString("thumbnail_url")};
        } else {
            throw new Exception("Server returned HTTP " + status);
        }
    }


    
    private void loadEventForEditing() {
        if (editEventId == null) return;
        db.collection("events").document(editEventId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            etTitle.setText(event.title);
                            etDesc.setText(event.description);
                            etPhone.setText(event.phone);
                            etPlaces.setText(String.valueOf(event.totalPlaces));
                            etLat.setText(String.valueOf(event.lat));
                            etLng.setText(String.valueOf(event.lng));
                            etLocationName.setText(event.locationName);

                            if (event.eventTimestamp > 0) {
                                eventCalendar.setTimeInMillis(event.eventTimestamp);
                                SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
                                tvDate.setText(sdfDate.format(new Date(event.eventTimestamp)));
                                tvTime.setText(sdfTime.format(new Date(event.eventTimestamp)));
                            }

                            currentMainImageUrl = event.mainImageUrl != null ? event.mainImageUrl : "";
                            currentThumbnailUrl = event.thumbnailUrl != null ? event.thumbnailUrl : "";
                            if (!currentMainImageUrl.isEmpty()) {

                                Glide.with(this).load(currentMainImageUrl).centerCrop().into(ivEventImage);
                            }

                            if (event.otherImagesUrls != null) {
                                currentExtraImages.clear();
                                currentExtraImages.addAll(event.otherImagesUrls);
                                
                                displayExistingExtras();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show());
    }

    private void displayExistingExtras() {
        llMoreImages.removeAllViews();
        for (String url : currentExtraImages) {
            ImageView iv = new ImageView(this);
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(200, 200);
            lp.setMargins(0, 0, 16, 0);
            iv.setLayoutParams(lp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(this).load(url).into(iv);
            llMoreImages.addView(iv);
        }
    }

    private void saveEventToFirestore(String title, String desc, String phone,
                                      String locationName, int places,
                                      double lat, double lng, String imageUrl,
                                      String thumbUrl,
                                      long evTimestamp, List<String> extraImageUrls) {
        btnCreate.setText("SAVING…");



        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("description", desc);
        data.put("phone", phone);
        data.put("locationName", locationName);
        data.put("totalPlaces", places);
        data.put("availablePlaces", places); 
        data.put("lat", lat);
        data.put("lng", lng);
        data.put("mainImageUrl", imageUrl);
        data.put("thumbnailUrl", thumbUrl);
        data.put("otherImagesUrls", extraImageUrls);
        data.put("eventTimestamp", evTimestamp);

        
        if (!isEditMode) {
            data.put("creatorUsername", currentUsername);
            data.put("timestamp", System.currentTimeMillis());
            data.put("participants", new ArrayList<>());

            db.collection("events").add(data)
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(this, "Event created! 🎉", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnCreate.setEnabled(true);
                        btnCreate.setText("CREATE EVENT");
                    });
        } else {
            db.collection("events").document(editEventId)
                    .update(data)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Event updated! ✅", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnCreate.setEnabled(true);
                        btnCreate.setText("UPDATE EVENT");
                    });
        }
    }
}
