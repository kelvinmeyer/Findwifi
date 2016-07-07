package com.gmail.kelvinmeyer13.findwifi;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by hobbes on 2016/06/28.
 * Apparently i need an edit??
 */
public class FindWifiTask extends AsyncTask<LatLng, Void, PlaceLocation[]> {

    private final String LOG_TAG = FindWifiTask.class.getSimpleName();

    //JSON formatting
    private PlaceLocation[] getWifiDataFromJson(String rawJsonStr, int numHotSpots) throws JSONException {
        // These are the names of the JSON objects that need to be extracted.
        final String FFW_DATA = "data";
        final String FFW_O_TIME = "OpeningTime";
        final String FFW_NAME = "Name";
        final String FFW_C_TIME = "ClosingTime";
        final String FFW_SERVICE = "CoLocatedService";
        final String FFW_PASSWORD_REQUEST = "CredentialRequest";
        final String FFW_BEST_RECEPTION = "BestReceptionSpot";
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
                oTime = jsonObject.optString(FFW_O_TIME);
                name = jsonObject.optString(FFW_NAME);
                cTime = jsonObject.optString(FFW_C_TIME);
                service = jsonObject.optString(FFW_SERVICE);
                passwordInfo = jsonObject.optString(FFW_PASSWORD_REQUEST);
                bestReception = jsonObject.optString(FFW_BEST_RECEPTION);
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
            final String FORECAST_BASE_URL = "http://www.findfreewifi.co.za/publicjson/locationsnear?";
            final String LAT_PARAM = "lat";
            final String LNG_PARAM = "lng";
            final String TOP_X_PARAM = "topX";

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(LAT_PARAM, Double.toString(lat))
                    .appendQueryParameter(LNG_PARAM, Double.toString(lng))
                    .appendQueryParameter(TOP_X_PARAM, Integer.toString(MainActivity.getTopX()))
                    .build();

            URL url = new URL(builtUri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuilder buffer = new StringBuilder();
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
                buffer.append(line);
                buffer.append("\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            wifiJsonStr = buffer.toString();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attempting
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
            return getWifiDataFromJson(wifiJsonStr, MainActivity.getTopX());
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
            MainActivity.updateWifi(result);
        }
    }
}

