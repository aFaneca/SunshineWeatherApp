/*
 * Copyright (c) 2019 ITSector Software. All rights reserved.
 * ITSector Software Confidential and Proprietary information. It is strictly forbidden for 3rd parties to modify, decompile, disassemble, defeat, disable or circumvent any protection mechanism; to sell, license, lease, rent, redistribute or make accessible to any third party, whether for profit or without charge.
 */

package com.itsector.sunshine;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.itsector.sunshine.sync.SunshineSyncUtils;

public class MainActivity extends AppCompatActivity implements ForecastFragment.Callback {
    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String DETAILFRAGMENT_TAG = "DFTAG";

    private Boolean mIsTwoPaneLayout;
    private String mLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocation = Utility.getPreferredLocation(this);

        setContentView(R.layout.activity_main);

        if (isTwoPaneLayout()) {
            /* If this view exists in the layout, it means we're dealing with a tablet
                which means we'll have a two pane layout*/
            mIsTwoPaneLayout = true;

        } else {
            mIsTwoPaneLayout = false;

            /* Removes unnecessary shadows below the action bar*/
            getSupportActionBar().setElevation(0f);
        }

        ForecastFragment forecastFragment = ((ForecastFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_forecast));
        /* Only use the today layout if we're not in the two pane layout */
        forecastFragment.setUseTodayLayout(!mIsTwoPaneLayout);
        SunshineSyncUtils.initialize(this);
    }

    /**
     * Handles options menu item selection
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        if (id == R.id.action_show_on_map) {
            openPreferredLocationInMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Makes an implicit intent for an external app that can show the
     * location on a map (e.g. Google Maps)
     * <p>
     * If none is found, nothing happens
     */
    private void openPreferredLocationInMap() {
        String location = Utility.getPreferredLocation(this);

        /* Building the URI */
        Uri geoLocation = Uri.parse("geo:0,0?").buildUpon()
                .appendQueryParameter("q", location)
                .build();

        /* Creating the intent */
        Intent intent = new Intent(Intent.ACTION_VIEW);
        /* Associate the URI with the intent */
        intent.setData(geoLocation);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(LOG_TAG, "Couldn't call " + location + ", no receiving apps installed!");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String location = Utility.getPreferredLocation(this);

        /* If the location changed, update it in both fragments */
        if (location != null && !location.equals(mLocation)) {
            ForecastFragment ff = (ForecastFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);
            if (null != ff) {
                ff.onLocationChanged();
            }
            DetailActivityFragment df = (DetailActivityFragment) getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
            if (null != df) {
                df.onLocationChanged(location);
            }
            mLocation = location;
        }
    }

    @Override
    public void onItemSelected(Uri contentUri) {
        if (isTwoPaneLayout()) {
            /* Send the required info in a bundle to the fragment */
            Bundle args = new Bundle();
            args.putParcelable(DetailActivityFragment.DETAIL_URI, contentUri);

            DetailActivityFragment fragment = new DetailActivityFragment();
            fragment.setArguments(args);

            /* Replace the current fragment for the new one selected */
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, fragment, DETAILFRAGMENT_TAG)
                    .commit();
        } else {
            /* Send the required info as extra data to the intent */
            Intent intent = new Intent(this, DetailActivity.class)
                    .setData(contentUri);
            startActivity(intent);
        }
    }

    /**
     * Checks wether the device is using the two-paned layout or not.
     * Will try to get that info from the member var mIsTwoPaneLayout (to avoid extra work)
     * If not defined, will look up the answer itself and return it
     *
     * @return boolean
     */
    private boolean isTwoPaneLayout() {
        /* if the var is already defined, just return it*/
        if (mIsTwoPaneLayout == null) {
            mIsTwoPaneLayout = (findViewById(R.id.weather_detail_container) != null);
        }

        return mIsTwoPaneLayout;
    }
}