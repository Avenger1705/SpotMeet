package com.spot.meet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapPickerActivity extends AppCompatActivity {

    private MapView mapView;
    private EditText etSearch;
    private FusedLocationProviderClient fusedLocationClient;
    private Button btnConfirm;
    private GeoPoint selectedLocation;
    private Geocoder geocoder;

    private final ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (fineLocationGranted != null && fineLocationGranted) {
                    moveToCurrentLocation();
                } else if (coarseLocationGranted != null && coarseLocationGranted) {
                    moveToCurrentLocation();
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("osmdroid", MODE_PRIVATE));
        setContentView(R.layout.activity_map_picker);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        geocoder = new Geocoder(this, Locale.getDefault());

        mapView = findViewById(R.id.map_view);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);
        mapView.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.ALWAYS);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(new GeoPoint(51.5074, -0.1278)); // Default: London

        etSearch = findViewById(R.id.et_search);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_search).setOnClickListener(v -> searchLocation());

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchLocation();
                return true;
            }
            return false;
        });

        FloatingActionButton fabMyLocation = findViewById(R.id.fab_my_location);
        fabMyLocation.setOnClickListener(v -> checkPermissionsAndMove());

        btnConfirm = findViewById(R.id.btn_confirm);
        btnConfirm.setOnClickListener(v -> {
            selectedLocation = (GeoPoint) mapView.getMapCenter();
            fetchAddressForSelected();
        });

        checkPermissionsAndMove();
    }

    private void fetchAddressForSelected() {
        if (selectedLocation == null) return;
        
        btnConfirm.setEnabled(false);
        btnConfirm.setText("Fetching Address...");

        new Thread(() -> {
            String addrStr = "";
            try {
                List<Address> addresses = geocoder.getFromLocation(selectedLocation.getLatitude(), selectedLocation.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    
                    StringBuilder sb = new StringBuilder();
                    if (addr.getLocality() != null) {
                        sb.append(addr.getLocality());
                    } else if (addr.getSubAdminArea() != null) {
                        sb.append(addr.getSubAdminArea());
                    }
                    
                    if (addr.getCountryName() != null) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(addr.getCountryName());
                    }
                    addrStr = sb.toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            final String finalAddr = addrStr;
            runOnUiThread(() -> {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("LATITUDE", selectedLocation.getLatitude());
                resultIntent.putExtra("LONGITUDE", selectedLocation.getLongitude());
                if (!finalAddr.isEmpty()) {
                    resultIntent.putExtra("ADDRESS", finalAddr);
                }
                setResult(RESULT_OK, resultIntent);
                finish();
            });
        }).start();
    }

    private void searchLocation() {
        String query = etSearch.getText().toString().trim();
        if (query.isEmpty()) return;

        new Thread(() -> {
            String result = null;
            GeoPoint found = null;
            try {
                List<Address> addresses = geocoder.getFromLocationName(query, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    found = new GeoPoint(address.getLatitude(), address.getLongitude());
                } else {
                    result = "Location not found";
                }
            } catch (IOException e) {
                result = "Search failed: " + e.getMessage();
            }
            final GeoPoint finalFound = found;
            final String finalResult = result;
            runOnUiThread(() -> {
                if (finalFound != null) {
                    mapView.getController().animateTo(finalFound);
                    mapView.getController().setZoom(17.0);
                    // Hide keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                } else {
                    Toast.makeText(this, finalResult, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void checkPermissionsAndMove() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }
        moveToCurrentLocation();
    }

    private void moveToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Use getCurrentLocation for fresh location instead of getLastLocation
        fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        mapView.getController().animateTo(geoPoint);
                        mapView.getController().setZoom(16.0);
                    } else {
                        // Fallback to getLastLocation
                        fusedLocationClient.getLastLocation().addOnSuccessListener(this, lastLoc -> {
                            if (lastLoc != null) {
                                GeoPoint geoPoint = new GeoPoint(lastLoc.getLatitude(), lastLoc.getLongitude());
                                mapView.getController().animateTo(geoPoint);
                                mapView.getController().setZoom(16.0);
                            } else {
                                Toast.makeText(this, "Could not get current location. Ensure GPS is enabled.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
    }

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
}
