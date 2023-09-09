package com.example.noiselevel;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.Place.Field;




import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class MainActivity<LineChart> extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseHelper dbHelper;
    private EditText noiseLevelEditText;
    private Button submitButton;
    private Button enableGPSButton;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private String getSelectedSortingCriteria = "timestamp";

    // Flag to check if the device is online
    private boolean isOnline = false;

    // Location tracking
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    // Places API
    private PlacesClient placesClient;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this); // Initialize the database helper

        // Check for battery optimization and prompt the user to disable it if needed
        if (isBatteryOptimizationEnabled()) {
            showBatteryOptimizationDialog();
        } else {
            continueWithInitialization(); // Continue with app initialization
        }
    }

    // Method to check if the app is being optimized for battery usage
    private boolean isBatteryOptimizationEnabled() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            String packageName = getPackageName();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return !powerManager.isIgnoringBatteryOptimizations(packageName);
            }
        }
        return false;
    }

    // Method to show a dialog prompting the user to disable battery optimization
    private void showBatteryOptimizationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.battery_optimization)
                .setMessage(R.string.battery_optimization_message)
                .setPositiveButton(R.string.disable_battery_optimization, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Open battery optimization settings
                        openBatteryOptimizationSettings();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Close the dialog and finish the app
                        finish();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Method to open battery optimization settings
    private void openBatteryOptimizationSettings() {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent.setAction(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        } else {
            intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
        }
        startActivity(intent);
    }

    // Continue with app initialization if battery optimization is disabled
    private void continueWithInitialization() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialize UI Elements
        noiseLevelEditText = findViewById(R.id.noiseLevelEditText);
        submitButton = findViewById(R.id.submitButton);
        enableGPSButton = findViewById(R.id.enableGPSButton); // Initialize the GPS Button

        // Check for location permissions
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Location permission is not granted, request it from the user
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            // Location permission is already granted, proceed with map setup
            setupMap();
        }

        // Check for internet connectivity
        isOnline = isNetworkAvailable();

        // Historical data button click listener
        findViewById(R.id.historicalDataButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isOnline) {
                    // Device is online, open historical data activity
                    openHistoricalDataActivity();
                } else {
                    // Device is offline, show a message
                    showOfflineMessage();
                }
            }
        });

        // Exit button click listener
        findViewById(R.id.exitButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showExitDialog();
            }
        });

        // Enable GPS button click listener
        enableGPSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open device location settings
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        // Submit button click listener for saving noise level data
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String noiseLevelStr = noiseLevelEditText.getText().toString().trim();

                if (!TextUtils.isEmpty(noiseLevelStr)) {
                    try {
                        double noiseLevel = Double.parseDouble(noiseLevelStr);

                        // Create a NoiseData object with the noise level and current timestamp
                        NoiseData noiseData = new NoiseData(noiseLevel, System.currentTimeMillis());

                        // Save the historical data
                        saveHistoricalData(noiseData);

                        // Optionally, you can clear the EditText field after saving
                        noiseLevelEditText.setText("");

                        // Notify the user that data has been saved
                        Toast.makeText(MainActivity.this, R.string.saved, Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        // Handle the case where the input is not a valid double
                        Toast.makeText(MainActivity.this, R.string.invalid_input, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Handle the case where the input is empty
                    Toast.makeText(MainActivity.this, R.string.enter_noise_level, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Check for internet connectivity periodically and sync data if online
        scheduleDataSyncTask();

        // Initialize Places API
        initPlacesAPI();

        // Initialize location tracking
        initLocationTracking();
    }


    // Initialize Places API
    private void initPlacesAPI() {
        String apiKey = getString(R.string.google_places_api_key);

        if (!apiKey.isEmpty()) {
            // Initialize Places
            if (!Places.isInitialized()) {
                Places.initialize(getApplicationContext(), apiKey);

                // Create a PlacesClient
                placesClient = Places.createClient(this);

                // Initialize the AutocompleteSupportFragment for place searching
                AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                        getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

                if (autocompleteFragment != null) {
                    autocompleteFragment.setPlaceFields(Arrays.asList(Field.ID, Field.NAME, Field.ADDRESS));
                    autocompleteFragment.setHint("Search places");
//                    autocompleteFragment.setMode(AutocompleteActivityMode.OVERLAY);

                    autocompleteFragment.setOnPlaceSelectedListener(new AutocompleteListener<Place>() {
                        @Override
                        public void onPlaceSelected(@NonNull Place place) {
                            // Handle the selected place, e.g., display its details
                            String placeName = place.getName();
                            String placeAddress = place.getAddress();
                            LatLng placeLatLng = place.getLatLng();
                            // Do something with the place information
                        }

                        @Override
                        public void onError(@NonNull com.google.android.libraries.places.api.model.Status status) {
                            // Handle any errors
                            Toast.makeText(MainActivity.this, "Place selection error: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Log.e("AutocompleteFragment", "AutocompleteSupportFragment not found.");
                }
            }
        } else {
            Log.e("API Key", "Google Places API key is missing in your strings.xml file.");
        }
    }

    // Initialize location tracking
    private void initLocationTracking() {
        // Initialize fusedLocationClient and locationRequest
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000); // Update interval in milliseconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Initialize locationCallback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    double latitude = locationResult.getLastLocation().getLatitude();
                    double longitude = locationResult.getLastLocation().getLongitude();

                    // Update the map with the user's current location
                    LatLng userLocation = new LatLng(latitude, longitude);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                }
            }
        };
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    // Method to set up the map
    private void setupMap() {
        // Check if GPS is enabled
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // GPS is not enabled, show the "Enable GPS" button
            enableGPSButton.setVisibility(View.VISIBLE);
        } else {
            // GPS is enabled, hide the "Enable GPS" button
            enableGPSButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted, proceed with map setup
                setupMap();
            } else {
                // Location permission denied, show a message or handle it as needed
                Toast.makeText(this, "Location permission denied. You can enable it in app settings.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Add instance variables to store filter criteria
    private String selectedSortingCriteria = "timestamp"; // Default sorting by timestamp
    private double minNoiseLevelFilter = 0; // Default minimum noise level filtering

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Apply data filtering and sorting based on user preferences
        applyDataFiltersAndSorting(selectedSortingCriteria, minNoiseLevelFilter);

        // Example noise data
        NoiseData kamparNoiseData = new NoiseData(75.5, System.currentTimeMillis());

        // Get noise level and coordinates from the NoiseData object
        double kamparNoiseLevel = kamparNoiseData.getKamparNoiseLevel();
        LatLng kamparCoordinates = new LatLng(4.3065, 101.1432); // Kampar coordinates

        MarkerOptions markerOptions = new MarkerOptions()
                .position(kamparCoordinates)
                .title(getString(R.string.current_noise) + " " + kamparNoiseLevel);

        Marker marker = mMap.addMarker(markerOptions);
        marker.setTag(kamparNoiseData); // Store the NoiseData object as a tag for the marker

        mMap.moveCamera(CameraUpdateFactory.newLatLng(kamparCoordinates));

        // Set Up zoom controls on the map
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Save historical noise level data
        saveHistoricalData(kamparNoiseData);
    }

    // Create a method to apply data filtering and sorting
    private void applyDataFiltersAndSorting(String sortingCriteria, double minNoiseLevel) {
        // Retrieve and display historical data based on sorting criteria and filters
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                DatabaseContract.HistoryEntry.COLUMN_NOISE_LEVEL,
                DatabaseContract.HistoryEntry.COLUMN_TIMESTAMP
        };

        String selection = DatabaseContract.HistoryEntry.COLUMN_NOISE_LEVEL + " >= ?";
        String[] selectionArgs = {String.valueOf(minNoiseLevel)};

        String orderBy = null;

        switch (sortingCriteria) {
            case "timestamp":
                orderBy = DatabaseContract.HistoryEntry.COLUMN_TIMESTAMP + " ASC";
                break;
            case "location":
                // Add sorting logic for location if needed
                break;
            case "noise_level":
                orderBy = DatabaseContract.HistoryEntry.COLUMN_NOISE_LEVEL + " ASC";
                break;
            // Add additional cases for other sorting criteria as needed
        }

        Cursor cursor = db.query(
                DatabaseContract.HistoryEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                orderBy
        );

        if (cursor != null && cursor.moveToFirst()) {
            mMap.clear(); // Clear existing markers on the map
            do {
                @SuppressLint("Range") double noiseLevel = cursor.getDouble(cursor.getColumnIndex(DatabaseContract.HistoryEntry.COLUMN_NOISE_LEVEL));
                @SuppressLint("Range") long timestamp = cursor.getLong(cursor.getColumnIndex(DatabaseContract.HistoryEntry.COLUMN_TIMESTAMP));

                // Check if the noise level and timestamp are valid
                if (noiseLevel >= 0 && timestamp >= 0) {
                    // Create a LatLng objects with coordinates of your choice
                    LatLng coordinates = new LatLng(4.3065, 101.1432);

                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(coordinates)
                            .title(getString(R.string.marker_title) + " " + noiseLevel)
                            .snippet(getString(R.string.marker_timestamp) + " " + timestamp);

                    mMap.addMarker(markerOptions);
                } else {
                    Log.e("InvalidData", "Invalid noise level or timestamp: " +
                            "Noise Level: " + noiseLevel + ", Timestamp: " + timestamp);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, R.string.error_occurred, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } while (cursor.moveToNext());

            cursor.close();
        }
    }

    // Open HistoricalDataActivity
    private void openHistoricalDataActivity() {
        Intent intent = new Intent(this, HistoricalDataActivity.class);
        startActivity(intent);
    }

    // Show an error dialog to the user
    private void showErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.error_occurred)
                .setMessage(R.string.invalid_data_detected)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Close the dialog
                        dialogInterface.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void saveHistoricalData(NoiseData noiseData) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.HistoryEntry.COLUMN_NOISE_LEVEL, noiseData.getKamparNoiseLevel());
        values.put(DatabaseContract.HistoryEntry.COLUMN_TIMESTAMP, noiseData.getTimestamp());

        long newRowId = db.insert(DatabaseContract.HistoryEntry.TABLE_NAME, null, values);
        if (newRowId != -1) {
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.exit_confirmation)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish(); // Close the app
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Dismiss the dialog
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Method to check for internet connectivity
    private void showOfflineMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.offline_mode)
                .setMessage(R.string.offline_message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Close the dialog
                        dialogInterface.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void scheduleDataSyncTask() {
        // Define the work request with your synchronization task
        PeriodicWorkRequest dataSyncWorkRequest = new PeriodicWorkRequest.Builder(
                DataSyncWorker.class, // Replace with your Worker class
                1, // Repeat interval in hours
                TimeUnit.HOURS
        ).build();

        // Enqueue the work request
        WorkManager.getInstance(this).enqueue(dataSyncWorkRequest);
    }
}
