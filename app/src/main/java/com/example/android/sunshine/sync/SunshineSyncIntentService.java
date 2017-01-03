/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine.sync;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.sunshine.data.WeatherContract;
import com.example.android.sunshine.utilities.SunshineDateUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class SunshineSyncIntentService extends IntentService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private GoogleApiClient mWearClient;
    public final String LOG_TAG = SunshineSyncIntentService.class.getSimpleName();

    public static final String WATCH_WEATHER_PATH = "/weather_watch";
    public static final String HIGH_TEMP = "HIGH_TEMP";
    public static final String LOW_TEMP = "LOW_TEMP";
    public static final String WEATHER_ID = "WEATHER_ID";

    double high_temp;
    double low_temp;
    int weather_id;

    public static final String[] MAIN_FORECAST_PROJECTION = {
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
    };

    /*
     * We store the indices of the values in the array of Strings above to more quickly be able to
     * access the data from our query. If the order of the Strings above changes, these indices
     * must be adjusted to match the order of the Strings.
     */
    public static final int INDEX_WEATHER_DATE = 0;
    public static final int INDEX_WEATHER_MAX_TEMP = 1;
    public static final int INDEX_WEATHER_MIN_TEMP = 2;
    public static final int INDEX_WEATHER_CONDITION_ID = 3;

    @Override
    public void onCreate() {
        super.onCreate();
        mWearClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mWearClient.connect();
        Log.d(LOG_TAG, "google api client connected");
    }

    @Override
    public void onDestroy() {
//        super.onDestroy();

    }

    public SunshineSyncIntentService() {

        super("SunshineSyncIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        SunshineSyncTask.syncWeather(this);

        long normalizedUtcNow = SunshineDateUtils.normalizeDate(System.currentTimeMillis());
        Uri uriForDateToday = WeatherContract.WeatherEntry.buildWeatherUriWithDate(normalizedUtcNow);

        Cursor cursor = getContentResolver().query(uriForDateToday, MAIN_FORECAST_PROJECTION, null, null, null);

        if(cursor!=null && cursor.moveToFirst()){
            weather_id = cursor.getInt(INDEX_WEATHER_CONDITION_ID);
            high_temp = cursor.getDouble(INDEX_WEATHER_MAX_TEMP);
            low_temp = cursor.getDouble(INDEX_WEATHER_MIN_TEMP);
            Log.d(LOG_TAG, "google api client cursor fetched weather");
            updateWearWeather(weather_id, high_temp, low_temp);
        }

    }

    private void updateWearWeather(int weather_id, double high, double low){
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WATCH_WEATHER_PATH).setUrgent();
        putDataMapRequest.getDataMap().putInt(WEATHER_ID, weather_id);
        putDataMapRequest.getDataMap().putDouble(HIGH_TEMP, high);
        putDataMapRequest.getDataMap().putDouble(LOW_TEMP, low);
        PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest().setUrgent();
        Wearable.DataApi.putDataItem(mWearClient, putDataRequest);
        Log.d(LOG_TAG, "in update weather task");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}