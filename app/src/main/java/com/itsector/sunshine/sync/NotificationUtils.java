/*
 * Copyright (c) 2019 ITSector Software. All rights reserved.
 * ITSector Software Confidential and Proprietary information. It is strictly forbidden for 3rd parties to modify, decompile, disassemble, defeat, disable or circumvent any protection mechanism; to sell, license, lease, rent, redistribute or make accessible to any third party, whether for profit or without charge.
 */

package com.itsector.sunshine.sync;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;

import com.itsector.sunshine.Data.SunshinePreferences;
import com.itsector.sunshine.Data.WeatherContract;
import com.itsector.sunshine.DetailActivity;
import com.itsector.sunshine.R;
import com.itsector.sunshine.Utility;

public class NotificationUtils {

    /*
     * The columns of data that we are interested in displaying within our notification to let
     * the user know there is new weather data available.
     */
    public static final String[] WEATHER_NOTIFICATION_PROJECTION = {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
    };

    /* Index of each of the columns referred above */
    public static final int INDEX_WEATHER_ID = 0;
    public static final int INDEX_MAX_TEMP = 1;
    public static final int INDEX_MIN_TEMP = 2;

    /* Constant int value to identify the notification */
    private static final int WEATHER_NOTIFICATION_ID = 3004;

    /**
     * Constructs and displays a notification for the newly updated weather for today.
     *
     * @param context Context used to query our ContentProvider and use various Utility methods
     */
    public static void notifyUserOfNewWeather(Context context) {

        /* Build the URI for today's weather in order to show updated data in notification */
        Uri todaysWeatherUri = WeatherContract.WeatherEntry
                .buildWeatherLocationWithDate(Utility.getPreferredLocation(context), System.currentTimeMillis());

        /* Gets a cursor with the data */
        Cursor todayWeatherCursor = context.getContentResolver().query(
                todaysWeatherUri,
                WEATHER_NOTIFICATION_PROJECTION,
                null,
                null,
                null);

        /*
         * If todayWeatherCursor is empty, moveToFirst will return false. If our cursor is not
         * empty, we want to show the notification.
         */
        if (todayWeatherCursor.moveToFirst()) {

            /* Weather ID as returned by API, used to identify the icon to be used */
            int weatherId = todayWeatherCursor.getInt(INDEX_WEATHER_ID);
            double high = todayWeatherCursor.getDouble(INDEX_MAX_TEMP);
            double low = todayWeatherCursor.getDouble(INDEX_MIN_TEMP);


            /* Associate an image resource with this item, according to its weather ID*/
            Resources resources = context.getResources();
            int largeArtResourceId = Utility.
                    getArtResourceForWeatherCondition(weatherId);

            Bitmap largeIcon = BitmapFactory.decodeResource(
                    resources,
                    largeArtResourceId);

            String notificationTitle = context.getString(R.string.app_name);

            String notificationText = getNotificationText(context, weatherId, high, low);

            /* getSmallArtResourceIdForWeatherCondition returns the proper art to show given an ID */
            int smallArtResourceId = Utility
                    .getIconResourceForWeatherCondition(weatherId);

            /* Get a reference to the NotificationManager */
            NotificationManager notificationManager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationCompat.Builder notificationBuilder;

            /* Android Oreo requires the creation of a notification channel
            before we can send out notifications */
            if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                String CHANNEL_ID = "" + (R.string.notif_main_channel_id);
                String channelName = "" + R.string.notif_main_channel_name;
                String channelDescription = "" + R.string.notif_main_channel_description;
                int importance = NotificationManager.IMPORTANCE_HIGH;

                /* Create the notification channel*/
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, channelName, importance);
                mChannel.setDescription(channelDescription);
                mChannel.enableLights(true);
                mChannel.setLightColor(Color.GREEN);
                mChannel.enableVibration(true);
                mChannel.setShowBadge(false);

                notificationManager.createNotificationChannel(mChannel);

                /* Build the notification instance */
                notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                        .setSmallIcon(smallArtResourceId)
                        .setLargeIcon(largeIcon)
                        .setContentTitle(notificationTitle)
                        .setContentText(notificationText)
                        .setAutoCancel(true);
            }else{
                /* Build the notification instance */
                notificationBuilder = new NotificationCompat.Builder(context)
                        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                        .setSmallIcon(smallArtResourceId)
                        .setLargeIcon(largeIcon)
                        .setContentTitle(notificationTitle)
                        .setContentText(notificationText)
                        .setAutoCancel(true);
            }
            /*
             * This Intent will be triggered when the user clicks the notification. In this case,
             * we want to open Sunshine to the DetailActivity to display the newly updated weather.
             */
            Intent detailIntentForToday = new Intent(context, DetailActivity.class);
            /* We have to pass the URI for today's weather,
             * so that the activity can recover and display that data */
            detailIntentForToday.setData(todaysWeatherUri);

            /* Create the pending intent */
            TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
            taskStackBuilder.addNextIntentWithParentStack(detailIntentForToday);
            PendingIntent resultPendingIntent = taskStackBuilder
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            /* Set the content Intent of the NotificationBuilder
            * The notificationBuilder will use this intent to direct the user back into the app
            * once he clicks in the notification. */
            notificationBuilder.setContentIntent(resultPendingIntent);

            /* Send the notification */
            notificationManager.notify(WEATHER_NOTIFICATION_ID, notificationBuilder.build());

            /*
             * Since we just showed a notification, save the current time. That way, we can check
             * next time the weather is refreshed if we should show another notification.
             */
            SunshinePreferences.saveLastNotificationTime(context, System.currentTimeMillis());
        }

        todayWeatherCursor.close();
    }

    /**
     * Constructs and returns the summary of a particular day's forecast using various utility
     * methods and resources for formatting. This method is only used to create the text for the
     * notification that appears when the weather is refreshed.
     *
     * The String returned from this method will look something like this:
     *                              Forecast: Sunny - High: 14°C Low 7°C
     *
     * @param context   Used to access utility methods and resources
     * @param weatherId ID as determined by Open Weather Map
     * @param high      High temperature (either celsius or fahrenheit depending on preferences)
     * @param low       Low temperature (either celsius or fahrenheit depending on preferences)
     * @return Summary of a particular day's forecast
     */
    private static String getNotificationText(Context context, int weatherId, double high, double low) {

        /*
         * Short description of the weather, as provided by the API.
         * e.g "clear" vs "sky is clear".
         */
        String shortDescription = Utility
                .getStringForWeatherCondition(context, weatherId);

        String notificationFormat = context.getString(R.string.format_notification);

        /* Using String's format method, we create the forecast summary */
        String notificationText = String.format(notificationFormat,
                shortDescription,
                Utility.formatTemperature(high, Utility.isMetric(context)),
                Utility.formatTemperature(low, Utility.isMetric(context)));

        return notificationText;
    }
}
