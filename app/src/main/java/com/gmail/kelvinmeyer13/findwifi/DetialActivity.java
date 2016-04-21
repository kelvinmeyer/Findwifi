package com.gmail.kelvinmeyer13.findwifi;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class DetialActivity extends AppCompatActivity {

    private PlaceLocation wifiLocation;

    @Override
    public void onBackPressed() {
        Intent mainActivityIntent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(mainActivityIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detial);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Uri gmmIntentUri = Uri.parse("http://maps.google.com/maps?daddr=" + wifiLocation.getLat() + ",+" + wifiLocation.getLng());
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);
                }
            });
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        Intent intent = this.getIntent();
        if(intent != null && intent.hasExtra(intent.EXTRA_TEXT)) {
            wifiLocation = new PlaceLocation(intent.getStringExtra(intent.EXTRA_TEXT));
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            TextView mTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
            mTitle.setText(wifiLocation.getName());
            ((TextView) this.findViewById(R.id.textView_times)).setText(getOpenClosed(wifiLocation) + wifiLocation.getTimes());
            ((TextView) this.findViewById(R.id.textView_service)).setText(wifiLocation.getService());
            ((TextView) this.findViewById(R.id.textView_direction)).setText(wifiLocation.getDirDist());
            ((TextView) this.findViewById(R.id.textView_password)).setText(wifiLocation.getPasswordInfo());
            ((TextView) this.findViewById(R.id.textView_best_Spot)).setText(wifiLocation.getBestSpot());
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.action_help_feedback:
                // User chose the "Favorite" action, mark the current item
                // as a favorite...
                return true;

            case R.id.action_share:
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                //todo this whole sharing thing
                sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out this cool wifi hot spot");
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.detail_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private String getOpenClosed(PlaceLocation location) {
        if(location.isOpen()){
            return "\nOpen\n";
        }
        else{
            return "\nClosed\n";
        }
    }
}
