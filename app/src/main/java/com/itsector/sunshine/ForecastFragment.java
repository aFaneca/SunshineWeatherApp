/*
 * Copyright (c) 2019 ITSector Software. All rights reserved.
 * ITSector Software Confidential and Proprietary information. It is strictly forbidden for 3rd parties to modify, decompile, disassemble, defeat, disable or circumvent any protection mechanism; to sell, license, lease, rent, redistribute or make accessible to any third party, whether for profit or without charge.
 */

package com.itsector.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;


import com.itsector.sunshine.Data.WeatherContract;
import com.itsector.sunshine.sync.SunshineService;

/**
 * Fragment containing the list view of the forecasts
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private ForecastAdapter mForecastAdapter;

    private ListView mListView;
    private int mPosition = ListView.INVALID_POSITION; // current position of the selected item
    private boolean mUseTodayLayout; // should a specific item use the "today" layout?

    private static final String SELECTED_KEY = "selected_position";

    private static final int FORECAST_LOADER = 0;

    /* Columns associated with each forecast item; to allow projection*/
    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

    /* Indices associated with each of the columns referred above */
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(Uri dateUri);
    }

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* So that the fragment can handle menu events */
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);
        /* Get a reference to the ListView, and attach this adapter to it. */
        mListView = (ListView) rootView.findViewById(R.id.forecast_listview);
        mListView.setAdapter(mForecastAdapter);

        /* In order to be able to highlight the currently selected item */
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        /* Handling item clicks in the list*/
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                if (cursor != null) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());

                    /* Trigger the onItemSelected callback */
                    ((Callback) getActivity())
                            .onItemSelected(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                                    locationSetting, cursor.getLong(COL_WEATHER_DATE)
                            ));
                }
                mPosition = position;
            }
        });

        /* Check if info was persisted regarding a potencial already selected item
        (e.g. before a screen rotation) */
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            /* This change will have an effect once onLoadFinished checks for this variable's value */
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        /* Initialize the loader */
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    /* since we read the location when we create the loader, all we need to do is restart things */
    public void onLocationChanged() {
        updateWeather();
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        /* Check if a position is selected and, if so, persist that information */
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        /* Sort order:  Ascending, by date. */
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";

        String locationSetting = Utility.getPreferredLocation(getActivity());

        /* Only get dates from today onwards*/
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());


        return new CursorLoader(this.getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        /* Replace the list with the new cursor of data */
        mForecastAdapter.swapCursor(data);

        /* If there's already an item previously selected (e.g. before a screen rotation) */
        if (mPosition != ListView.INVALID_POSITION) {
           /* Automatically scroll the list view to that position, if needed */
            mListView.smoothScrollToPosition(mPosition);
        }else{
            /* If there isn't already a position defined and we're displaying a two-paned layout */
            if (isTwoPaneLayout()) {
                /* Automatically select the first item in the list (which represents today's forecast */
                autoSelectFirstItem();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mForecastAdapter.swapCursor(null);
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
        if (mForecastAdapter != null) {
            mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }

    /**
     * Starts the intent service that will update the weather information,
     * refetching it from the API
     */
    private void updateWeather() {
        Intent intent = new Intent(getActivity(), SunshineService.class);
        intent.putExtra(SunshineService.LOCATION_QUERY_EXTRA,
                Utility.getPreferredLocation(getActivity()));
        getActivity().startService(intent);
    }

    /**
     * Automatically selects the first item in the listView
     * Make sure to only call this method after the cursor has fully loaded (e.g. onLoadFinihed)
     */
    private void autoSelectFirstItem() {
        /* If there's no items in this list, do nothing */
        if(mListView.getAdapter().getCount() == 0) return;

        /* Perform a click in the first item */
        mListView.performItemClick(mListView.getAdapter().getView(0, null, null),
                0,
                mListView.getAdapter().getItemId(0));
    }

    /**
     * Checks whether or not we're dealing with a two-paned layout
     * @return boolean
     */
    private boolean isTwoPaneLayout() {
        if (getActivity().findViewById(R.id.weather_detail_container) != null) {
            /* If this view exists in the layout, it means we're dealing with a tablet
             *  which means we'll have a two pane layout*/
            return true;

        } else {
            return false;
        }
    }
}