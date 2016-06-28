package com.gmail.kelvinmeyer13.findwifi;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private LatLng userPosition;
    private boolean locationOn = false;
    private LocationManager locationManager;
    private static GoogleMap mMap;
    private static PlaceLocation[] wifiLocations;
    //preferences
    private static int topX;
    private static boolean showOpenOnly;

    public void setUserPosition(double lat,double lng){
        userPosition = new LatLng(lat,lng);
    }

    public LatLng getUserPosition(){
        return userPosition;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putDouble("lat", getUserPosition().latitude );
        outState.putDouble("lng", getUserPosition().longitude );
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        setLocation(true);
        setUserPosition(-33.957503, 18.462007);
        if (savedInstanceState != null) {
            Toast.makeText(this, savedInstanceState.getString("message"), Toast.LENGTH_LONG).show();
            //set position based on bundle
        }
        else{
            enableBestUpdates();
        }
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        topX = Integer.parseInt(sharedPref.getString(getString(R.string.key_number_wifi),getString(R.string.pref_default_Number_Spots)));
        showOpenOnly =sharedPref.getBoolean(getString(R.string.key_open_switch), true);
    }

    public static int getTopX(){
        return topX;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (!marker.getTitle().equals("You")) {
                    Intent detailActivityIntent = new Intent(getApplicationContext(), DetialActivity.class);
                    detailActivityIntent.putExtra(Intent.EXTRA_TEXT, wifiLocations[findLocationId(Integer.parseInt(marker.getTitle()))].toStringLong());
                    startActivity(detailActivityIntent);
                }
                return true;
            }
        });
}

    public int findLocationId(Integer id){
        int pos = 0;
        for(int i = 0;i<wifiLocations.length;i++){
            if(wifiLocations[i].getId()== id){
                pos = i;
            }
        }
        return pos;
    }

    private void setLocation(boolean b) {
        locationOn = b;
    }

    private void enableBestUpdates() {
        if(isLocationEnabled()){
            if(checkLocationPermission()){
                if(locationOn){
                    Criteria criteria = new Criteria();
                    criteria.setAccuracy(Criteria.ACCURACY_FINE);
                    criteria.setAltitudeRequired(false);
                    criteria.setBearingRequired(false);
                    criteria.setCostAllowed(true);
                    criteria.setPowerRequirement(Criteria.POWER_LOW);
                    String provider = locationManager.getBestProvider(criteria, true);
                    if (provider != null) {
                        locationManager.requestLocationUpdates(provider, 2 * 60 * 1000, 10, locationListenerBest);
                        Toast.makeText(this, "Best Provider is " + provider, Toast.LENGTH_LONG).show();
                    }
                }
            }
            else{
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    //todo explanation
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                }
            }
        }
        else{
            final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("Enable Location")
                    .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " +
                            "use this app")
                    .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(myIntent);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        }
                    });
            dialog.show();
        }


    }

    private boolean isLocationEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void addYourLocation(LatLng position){
        mMap.addMarker(new MarkerOptions().position(position).title("You"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(14f));
    }

    private final LocationListener locationListenerBest = new LocationListener() {
        public void onLocationChanged(final Location location) {
            final double longitudeBest = location.getLongitude();
            Log.d("maps", "lng: " + longitudeBest);
            final double latitudeBest = location.getLatitude();
            Log.d("maps", "lat: " + latitudeBest);
            setLocation(false);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setUserPosition(latitudeBest, longitudeBest);
                    Toast.makeText(MainActivity.this, "Best Provider update", Toast.LENGTH_SHORT).show();
                    LatLng pos = getUserPosition();
                    mMap.clear();
                    addYourLocation(pos);
                    fetchWifi();
                }
            });

        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    private void fetchWifi(){
        FindWifiTask wifiTask = new FindWifiTask();
        wifiTask.execute(getUserPosition());
    }

    public static void updateWifi(PlaceLocation[] result) {
        wifiLocations = result;
        for (PlaceLocation wifiLocation : wifiLocations) {
            if (wifiLocation.isOpen()) {
                mMap.addMarker(new MarkerOptions().position(wifiLocation.getCoords()).title(Integer.toString(wifiLocation.getId())).icon(BitmapDescriptorFactory.fromResource(R.mipmap.wifi_green)));
            } else {
                if (!showOpenOnly) {
                    mMap.addMarker(new MarkerOptions().position(wifiLocation.getCoords()).title(Integer.toString(wifiLocation.getId())).icon(BitmapDescriptorFactory.fromResource(R.mipmap.wifi_red)));
                }
            }
        }
    }
}
