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
import android.net.Uri;
import android.os.AsyncTask;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private LatLng userPosition;
    private boolean locationOn = false;
    private LocationManager locationManager;
    private GoogleMap mMap;
    private PlaceLocation[] wifiLocations;
    //preferences
    private int topX;
    private boolean showOpenOnly;

    public void setUserPosition(double lat,double lng){
        userPosition = new LatLng(lat,lng);
    }

    public LatLng getUserPosition(){
        return userPosition;
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
        enableBestUpdates();
        setUserPosition(-33.957503, 18.462007);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        topX = Integer.parseInt(sharedPref.getString(getString(R.string.key_number_wifi),getString(R.string.pref_default_Number_Spots)));
        showOpenOnly =sharedPref.getBoolean(getString(R.string.key_open_switch), true);
    }

    public int getTopX(){
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
                    detailActivityIntent.putExtra(detailActivityIntent.EXTRA_TEXT, wifiLocations[findLocationId(Integer.parseInt(marker.getTitle()))].toStringLong());
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
                if(locationOn == true){
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
                    //todo explaination
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                return true;
        }
        else {
            return false;
        }
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

    private void updateWifi() {
        for (int i = 0; i < wifiLocations.length; i++) {
            if(wifiLocations[i].isOpen()) {
                mMap.addMarker(new MarkerOptions().position(wifiLocations[i].getCoords()).title(Integer.toString(wifiLocations[i].getId())).icon(BitmapDescriptorFactory.fromResource(R.mipmap.wifi_green)));
            }
            else{
                if(!showOpenOnly) {
                    mMap.addMarker(new MarkerOptions().position(wifiLocations[i].getCoords()).title(Integer.toString(wifiLocations[i].getId())).icon(BitmapDescriptorFactory.fromResource(R.mipmap.wifi_red)));
                }
            }
        }
    }

    public class FindWifiTask extends AsyncTask<LatLng, Void, PlaceLocation[]> {

        private final String LOG_TAG = FindWifiTask.class.getSimpleName();

        //JSON formatting
        private PlaceLocation[] getWifiDataFromJson(String rawJsonStr, int numHotSpots) throws JSONException {
            // These are the names of the JSON objects that need to be extracted.
            final String FFW_DATA = "data";
            final String FFW_OTIME = "OpeningTime";
            final String FFW_NAME = "Name";
            final String FFW_CTIME = "ClosingTime";
            final String FFW_SERVICE = "CoLocatedService";
            final String FFW_PASSWORD_REQUEST = "CredentialRequest";
            final String FFW_BESTRECEPTION = "BestReceptionSpot";
            final String FFW_LAT = "Lat";
            final String FFW_PASSWORD_CONTROL = "PasswordControl";
            final String FFW_LNG = "Long";
            final String FFW_DIST = "DistanceFromMe";
            final String FFW_DIR = "FullDirection";
            final String FFW_ID = "ID";

            PlaceLocation[] resultLocations = new PlaceLocation[numHotSpots];

            JSONObject JsonRootObject = new JSONObject(rawJsonStr);
            JSONArray wifiArray = JsonRootObject.getJSONArray(FFW_DATA);

            String name;
            String oTime;
            String cTime;
            String bestReception;
            String passwordInfo;
            String passwordControl;
            String service;
            int distance;
            String direction;
            double lat;
            double lng;
            int id;

            try {
                //Iterate the jsonArray and print the info of JSONObjects
                for (int i = 0; i < wifiArray.length(); i++) {
                    JSONObject jsonObject = wifiArray.getJSONObject(i);
                    id = jsonObject.optInt(FFW_ID);
                    oTime = jsonObject.optString(FFW_OTIME);
                    name = jsonObject.optString(FFW_NAME);
                    cTime = jsonObject.optString(FFW_CTIME);
                    service = jsonObject.optString(FFW_SERVICE);
                    passwordInfo = jsonObject.optString(FFW_PASSWORD_REQUEST);
                    bestReception = jsonObject.optString(FFW_BESTRECEPTION);
                    lat = jsonObject.optDouble(FFW_LAT);
                    passwordControl = jsonObject.optString(FFW_PASSWORD_CONTROL);
                    lng = jsonObject.optDouble(FFW_LNG);
                    distance = Integer.parseInt(Math.round(jsonObject.optDouble(FFW_DIST) * 1000) + "");
                    direction = jsonObject.optString(FFW_DIR);
                    resultLocations[i] = new PlaceLocation(name, oTime, cTime, bestReception, lat, lng, passwordInfo, passwordControl, service, distance, direction, id);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return resultLocations;

        }

        //get data from the server
        @Override
        protected PlaceLocation[] doInBackground(LatLng... params) {
            /**
             *  http://www.findfreewifi.co.za/publicjson/locationsnear?lat=-33.957503&lng=18.462007&topX=1
             */
            // If there's no zip code, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            double lat = params[0].latitude;
            double lng = params[0].longitude;

            // Will contain the raw JSON response as a string.
            String wifiJsonStr = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                final String FORECAST_BASE_URL = "http://www.findfreewifi.co.za/publicjson/locationsnear?";
                final String LAT_PARAM = "lat";
                final String LNG_PARAM = "lng";
                final String TOPX_PARAM = "topX";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(LAT_PARAM, Double.toString(lat))
                        .appendQueryParameter(LNG_PARAM, Double.toString(lng))
                        .appendQueryParameter(TOPX_PARAM, Integer.toString(getTopX()))
                        .build();

                URL url = new URL(builtUri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.1
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                wifiJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getWifiDataFromJson(wifiJsonStr, topX);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }

        //after complete do this.v
        @Override
        protected void onPostExecute(PlaceLocation[] result) {
            if (result != null) {
                wifiLocations = new PlaceLocation[result.length];
                for (int i = 0; i<result.length; i++) {
                    wifiLocations[i] = result[i];
                }
                updateWifi();
            }
        }
    }

}
