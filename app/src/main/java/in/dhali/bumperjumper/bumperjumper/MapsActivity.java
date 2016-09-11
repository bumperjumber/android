package in.dhali.bumperjumper.bumperjumper;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationParams;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, SensorEventListener {

    private static final String TAG = "BJ.MA";
    private GoogleMap mMap;
//    private Marker marker;
    private Circle circle;
    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private Sensor sensorSignificantMotion;
    File traceFile;
    BufferedWriter traceBufferedWriter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        SimpleDateFormat formatYmd = new SimpleDateFormat("yyyy-MM-dd-HH", Locale.ENGLISH);
        this.traceFile = new File("sdcard/bumper-jumper-" + formatYmd.format(new Date()) + ".trace.txt");

        Log.i(TAG, "File: " + this.traceFile.getAbsolutePath());
        this.requestMissingPermissions();
    }

    private boolean requestMissingPermissions() {
        String[] requiredPermissions = new String[]{
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> missingPermissions = new ArrayList<>();

        for (String requiredPermission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, requiredPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(requiredPermission);
            }
        }

        if (missingPermissions.size() > 0) {
            Log.d(TAG, "requestPerm: " + missingPermissions);

            ActivityCompat.requestPermissions(this, missingPermissions.toArray(new String[missingPermissions.size()]), 1);

            return false;
        } else {
            doStuffAfterPermission();

            return true;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        try {
            traceBufferedWriter.write("# Stop" + new Date().toString() + "\n");
            this.traceBufferedWriter.flush();
        } catch (IOException e) {
            Log.e(TAG, "flush", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");

        for (int i = 0; i < permissions.length && i < grantResults.length; i++) {
            Log.d(TAG, permissions[i] + ": " + grantResults[i]);
        }

        this.requestMissingPermissions();
    }

    private double gpsLat = 0, gpsLng = 0;
    private float gpsSpeed = 0, gpsAccuracy = 0.000005f;
    private final float gpsFilterLevel = 0;

    private void addBumpsToMap() {
        InputStream in = getResources().openRawResource(R.raw.bumps);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                out.append(line);
                out.append('\n');
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(out.toString());   //Prints the string content read from input stream

        ArrayList<Bump> bumps = Bump.fromLines(out.toString());
        LatLng latLng = null;

        for (Bump bump : bumps) {
            // int red = Math.min(255, (int) (bump.energy * 256 / 1000d));
            // int green = 255 - red;

            latLng = new LatLng(bump.lat, bump.lng);

            CircleOptions bumpCircle = new CircleOptions()
                    .center(latLng);

            if (bump.energy < 250) {
                int c = Color.rgb(255, 165, 0);
                bumpCircle.radius(1).fillColor(c).strokeColor(c).zIndex(1);
            } else {
                bumpCircle.radius(4).fillColor(Color.RED).strokeColor(Color.RED).zIndex((float)bump.energy);
            }

            mMap.addCircle(bumpCircle);
        }
        if (latLng != null) {
            Log.d(TAG, "Move camera " + latLng);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }

    private void doStuffAfterPermission() {

        try {
            traceBufferedWriter = new BufferedWriter(new FileWriter(traceFile, true));
            traceBufferedWriter.write("# Start" + new Date().toString() + "\n");
        } catch (IOException e) {
            Log.e(TAG, "Open BR", e);
            e.printStackTrace();
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        // sensorAccelerometer = sensorManager.getDefaultSensor();
        // sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        for (int sensorType :
                new int[]{
                        Sensor.TYPE_ACCELEROMETER,
                        Sensor.TYPE_GRAVITY,
                        Sensor.TYPE_GYROSCOPE,
                        Sensor.TYPE_LINEAR_ACCELERATION,
                        Sensor.TYPE_ROTATION_VECTOR}) {
            sensorManager.registerListener(
                    this,
                    sensorManager.getDefaultSensor(sensorType),
                    SensorManager.SENSOR_DELAY_NORMAL);
        }

        sensorSignificantMotion = sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
        sensorManager.registerListener(this, sensorSignificantMotion, SensorManager.SENSOR_DELAY_NORMAL);


        SmartLocation.with(this).location().config(LocationParams.NAVIGATION).start(new OnLocationUpdatedListener() {
            @Override
            public void onLocationUpdated(Location location) {
                long ms = System.currentTimeMillis();
                int type = 0;
                double lat = location.getLatitude();
                double lng = location.getLongitude();

                if (Math.abs(gpsLat - lat) < gpsAccuracy && Math.abs(gpsLng - lng) < gpsAccuracy) {
                    return;
                }

                float speed = location.getSpeed();
                float accuracy = location.getAccuracy();

                String line = String.format("%d,%d,%f,%f,%f,%f", ms, type, lat, lng, speed, accuracy);

                // Log.d(TAG, line);

                try {
                    traceBufferedWriter.write(line + "\n");
                } catch (IOException e) {
                    Log.e(TAG, "WriteGPS", e);
                }

                // Log.d(TAG, line);
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                circle.setCenter(latLng);
//                marker.setPosition(latLng);
            }
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        // this.marker = mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney").flat(true));
        this.circle = mMap.addCircle(new CircleOptions().center(sydney).radius(3).fillColor(Color.BLUE).strokeColor(Color.BLUE));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 17));

        this.addBumpsToMap();
    }

    public float[] accelerometerValues = new float[]{0, 0, 0};
    public float accelerometerAccuracy = 0;
    public static final float accelerometerFilterLevel = 0.2f;

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        int type = sensor.getType();
        long ms = System.currentTimeMillis();
        String line = null;

        switch (type) {
            //
            case Sensor.TYPE_ACCELEROMETER:
                if (Math.abs(event.values[0] - accelerometerValues[0]) < accelerometerFilterLevel &&
                        Math.abs(event.values[1] - accelerometerValues[1]) < accelerometerFilterLevel &&
                        Math.abs(event.values[2] - accelerometerValues[2]) < accelerometerFilterLevel
                        ) {
                    break;
                }

                accelerometerValues = event.values.clone();
                accelerometerAccuracy = event.accuracy;


                line = String.format("%d,%d,%f,%f,%f,%f", ms, type,
                        accelerometerValues[0], accelerometerValues[1], accelerometerValues[2],
                        accelerometerAccuracy);

                // Log.d(TAG, line);

                try {
                    traceBufferedWriter.write(line + "\n");
                } catch (IOException e) {
                    Log.e(TAG, "WriteSensor", e);
                }
                break;

            default:
                line = String.format("%d,%d,%f,%f,%f,%f", ms, type,
                        event.values[0], event.values[1], event.values[2],
                        event.accuracy);

                // Log.d(TAG, line);

                try {
                    traceBufferedWriter.write(line + "\n");
                } catch (IOException e) {
                    Log.e(TAG, "WriteSensor", e);
                }
                break;


        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {


    }
}