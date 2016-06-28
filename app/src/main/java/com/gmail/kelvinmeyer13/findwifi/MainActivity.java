package com.gmail.kelvinmeyer13.findwifi;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    /**
     * variables and such
     **/
    private static final String TAG = MainActivity.class.getSimpleName();
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    //location
    private LatLng userPosition;
    private GoogleApiClient mGoogleApiClient;
    private boolean locationOn = false;
    private LocationManager locationManager;
    //map
    private static GoogleMap mMap;
    private static PlaceLocation[] wifiLocations;
    //preferences
    private static int topX;
    private static boolean showOpenOnly;

    //setters
    public void setUserPosition(double lat,double lng){
        userPosition = new LatLng(lat,lng);
    }

    private void setLocation(boolean b) {
        locationOn = b;
    }

    //getters
    public LatLng getUserPosition(){
        return userPosition;
    }

    public static int getTopX(){
        return topX;
    }

    /**
     * android lifecycle methods
     */
    /*@Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putDouble("lat", getUserPosition().latitude );
        outState.putDouble("lng", getUserPosition().longitude );
        super.onSaveInstanceState(outState);
    }*/

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
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
        // First we need to check availability of play services
        if (checkPlayServices()) {

            // Building the GoogleApi client
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API).build();
        }
        //if (savedInstanceState != null) {
          //  Toast.makeText(this, savedInstanceState.getString("message"), Toast.LENGTH_LONG).show();
            //set position based on bundle
        //}
        //else{
            //enableBestUpdates();
        //}
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        topX = Integer.parseInt(sharedPref.getString(getString(R.string.key_number_wifi),getString(R.string.pref_default_Number_Spots)));
        showOpenOnly =sharedPref.getBoolean(getString(R.string.key_open_switch), true);
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
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

    /**
     *  location methods new
     */

    public int findLocationId(Integer id){
        int pos = 0;
        for(int i = 0;i<wifiLocations.length;i++){
            if(wifiLocations[i].getId()== id){
                pos = i;
            }
        }
        return pos;
    }

    private void addYourLocation(LatLng position){
        mMap.addMarker(new MarkerOptions().position(position).title("You"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(14f));
    }

    /**
     * Method to verify google play services on the device
     * */
    private boolean checkPlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            setUserPosition(mLastLocation.getLatitude(),mLastLocation.getLongitude());
            addYourLocation(getUserPosition());
        }
        fetchWifi();
    }


    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }



    /**
     *  wifi methods
     */

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

    /**
     * location methods old
     *

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
     ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_COURSE_LOCATION);
     } else {
     // No explanation needed, we can request the permission.
     ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_COURSE_LOCATION);
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
     **/
}
