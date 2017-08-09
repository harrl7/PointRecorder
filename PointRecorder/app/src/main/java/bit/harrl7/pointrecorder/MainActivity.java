package bit.harrl7.pointrecorder;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener
{
    private static final int locationPermissionsRequestCode = 42;
    private static final String TAG = MainActivity.class.getSimpleName();

    // Location
    private GoogleApiClient mGoogleApiClient;
    private Location lastLocation;
    private LocationRequest mLocationRequest;

    // Saved points
    List<WorldPoint> worldPointList;

    // Ui
    TextView tvLat;
    TextView tvLng;
    EditText txtLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Points
        worldPointList = new ArrayList<>();

        googleAPIConnection();

        // Location labels
        tvLat = (TextView) findViewById(R.id.tvLat);
        tvLng = (TextView) findViewById(R.id.tvLng);
        txtLabel = (EditText) findViewById(R.id.txtLabel);

        // Save point button
        Button btnSave = (Button) findViewById(R.id.btnSavePoint);
        btnSave.setOnClickListener(new BtnSavePointClickHandler());

        // Write to file
        Button btnWrite = (Button) findViewById(R.id.btnWrite);
        btnWrite.setOnClickListener(new OutputResultsClickHandler());

        // Clear list
        Button btnClear = (Button) findViewById(R.id.btnClear);
        btnClear.setOnClickListener(new ClearListClickHandler());

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.activity_main);

    }



    public class BtnSavePointClickHandler implements View.OnClickListener
    {

        @Override
        public void onClick(View v)
        {
            // Toast.makeText(MainActivity.this, "Saves", Toast.LENGTH_SHORT).show();

            // Add new point
            double lng = lastLocation.getLongitude();
            double lat = lastLocation.getLatitude();
            String label = txtLabel.getText().toString();

            tvLat.setText(""+lat);
            tvLng.setText(""+lng);

            WorldPoint worldPoint = new WorldPoint();
            worldPoint.lng = lng;
            worldPoint.lat = lat;
            worldPoint.label = label;

            worldPointList.add(worldPoint);
        }
    }


    public class OutputResultsClickHandler implements View.OnClickListener
    {
        @Override
        public void onClick(View v)
        {
            OutputResults();
        }
    }

    // Clear list
    public class ClearListClickHandler implements View.OnClickListener
    {

        @Override
        public void onClick(View v)
        {
            AlertDialog.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_Material_Dialog_Alert); }
            else { builder = new AlertDialog.Builder(MainActivity.this); }


            builder.setTitle("Delete")
                    .setMessage("Are you sure you want to delete saved points?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            // continue with delete
                            worldPointList.clear();
                            Toast.makeText(MainActivity.this, "List cleared", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            // do nothing
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }



    // Google location API ======================
    public void googleAPIConnection()
    {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }


    //Check the availability of Google Play Services on phone
    //May move to seperate class handling connections
    public boolean googleServicesAvailable()
    {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int isAvailable = api.isGooglePlayServicesAvailable(this);
        if (isAvailable == ConnectionResult.SUCCESS)
        {
            return true;
        }
        else if (api.isUserResolvableError(isAvailable))
        {
            Dialog dialog = api.getErrorDialog(this, isAvailable, 0);
            dialog.show();
        }
        else
        {
            Toast.makeText(this, "Can't get Map", Toast.LENGTH_LONG).show();
        }
        return false;


    }

    @Override
    public void onConnected( Bundle bundle)
    {
        Log.i(TAG, "onConnected()");
        getLastKnownLocation();
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        Log.w(TAG, "onConnectionSuspended()");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        Log.w(TAG, "onConnectionFailed()");
    }

    // Get last known location
    private void getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation()");
        if ( checkPermission() )
        {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if ( lastLocation != null )
            {
                Log.i(TAG, "LasKnown location. " + "Long: " + lastLocation.getLongitude() + " | Lat: " + lastLocation.getLatitude());
                startLocationUpdates();
            }
            else
            {
                Log.w(TAG, "No location retrieved yet");
                startLocationUpdates();
            }
        }
        else askPermission();
    }

    // Start location Updates
    private void startLocationUpdates()
    {
        Log.i(TAG, "startLocationUpdates()");
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setSmallestDisplacement(10)
                .setInterval(3500);
        //.setFastestInterval(FASTEST_INTERVAL);

        if (checkPermission())
        {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, (LocationListener) this);
        }

    }

    //When location is changed
    @Override
    public void onLocationChanged(Location location)
    {
        Log.d(TAG, "onLocationChanged ["+location+"]");
        tvLat.setText(location.getLatitude()+"");
        tvLng.setText(location.getLongitude()+"");

        lastLocation = location;
    }

    // Check for permission to access Location
    private boolean checkPermission()
    {
        Log.d(TAG, "checkPermission()");
        // Ask for permission if it wasn't granted yet
        return (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED );
    }

    // Asks for permission
    private void askPermission()
    {
        Log.d(TAG, "askPermission()");
        String[] permissionsIWant = new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION};

        ActivityCompat.requestPermissions(this, permissionsIWant, locationPermissionsRequestCode);
    }

    // Verify user's response of the permission requested
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        Log.d(TAG, "onRequestPermissionsResult()");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch ( requestCode ) {
            case 42: {
                if ( grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED ){
                    // Permission granted
                    getLastKnownLocation();

                } else {
                    // Permission denied
                    permissionsDenied();
                }
                break;
            }
        }
    }

    // App cannot work without the permissions
    private void permissionsDenied()
    {
        Log.w(TAG, "permissionsDenied()");
    }

    // End of location API ==========

    // Output results
    // Print results to .csv file at /VisScan
    public void OutputResults()
    {

        // Storage dir
        String docsFolder = Environment.getExternalStorageDirectory().toString();

        // Outer folder, make if doesn't exist already
        File resultsFolder = new File(docsFolder +"/WorldPoints");
        if(!resultsFolder.exists()) { resultsFolder.mkdirs(); }

        // Date
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yy_HH:mm:ss");
        String fileName = "points_" + df.format(new Date()) + ".csv";

        // Filename
        File file = new File(resultsFolder.getAbsoluteFile().toString(), fileName);
        try { file.createNewFile(); }
        catch (IOException e) { e.printStackTrace(); }

        // Create file text
        String csv = "label, latitude, longitude\r\n";
        for (WorldPoint item : worldPointList) { csv += item.ToCSV() + "\r\n"; }

        // Write results
        try (FileWriter fileWriter = new FileWriter(file)) { fileWriter.append(csv); }
        catch (IOException e) { e.printStackTrace(); }

        Toast.makeText(this, "saved as " + fileName, Toast.LENGTH_SHORT).show();
    }
}
