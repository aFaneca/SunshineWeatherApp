/*
 * Copyright (c) 2019 ITSector Software. All rights reserved.
 * ITSector Software Confidential and Proprietary information. It is strictly forbidden for 3rd parties to modify, decompile, disassemble, defeat, disable or circumvent any protection mechanism; to sell, license, lease, rent, redistribute or make accessible to any third party, whether for profit or without charge.
 */

package com.itsector.sunshine.sync;

import android.app.IntentService;
import android.content.Intent;

import com.itsector.sunshine.Utility;

/**
 * Created by E936 on 4/9/2019.
 */
public class SunshineService extends IntentService {

    public static final String LOCATION_QUERY_EXTRA = "lqe";
    private final String LOG_TAG = SunshineService.class.getSimpleName();

    public SunshineService() {
        super("Sunshine");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String locationQuery = Utility.getPreferredLocation(this);
        SunshineSyncTask.syncWeather(this, locationQuery);

    }


}
