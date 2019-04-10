/*
 * Copyright (c) 2019 ITSector Software. All rights reserved.
 * ITSector Software Confidential and Proprietary information. It is strictly forbidden for 3rd parties to modify, decompile, disassemble, defeat, disable or circumvent any protection mechanism; to sell, license, lease, rent, redistribute or make accessible to any third party, whether for profit or without charge.
 */

package com.itsector.sunshine.sync;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.Time;
import android.util.Log;

import com.itsector.sunshine.BuildConfig;
import com.itsector.sunshine.Data.WeatherContract;
import com.itsector.sunshine.ForecastAdapter;
import com.itsector.sunshine.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

/**
 * Created by E936 on 4/9/2019.
 */
public class SunshineService extends IntentService {
    private ForecastAdapter mForecastAdapter;
    public static final String LOCATION_QUERY_EXTRA = "lqe";
    private final String LOG_TAG = SunshineService.class.getSimpleName();

    public SunshineService() {
        super("Sunshine");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        /*String locationQuery = intent.getStringExtra(LOCATION_QUERY_EXTRA);*/
        String locationQuery = Utility.getPreferredLocation(this);
        SunshineSyncTask.syncWeather(this, locationQuery);

    }


}
