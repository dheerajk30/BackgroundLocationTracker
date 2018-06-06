package com.example.dheeraj.backgroundlocationtracker;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.dheeraj.backgroundlocationtracker.MapsScreen;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;

import java.util.ArrayList;


public class LocationTrackerService extends Service {


    ArrayList<LatLng> points;
    Context context;
    long update_interval=30000,fast_update_interval=30000;
    public IBinder binder=new LocalBinder();


    @Override
    public void onCreate() {
        super.onCreate();
        points=new ArrayList<LatLng>();
        getDeviceLocation();
        getLocationUpdates();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent i=new Intent("Location_Update");
        i.putParcelableArrayListExtra("points",points);
        sendBroadcast(i);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        points=new ArrayList<LatLng>();
        return START_STICKY;
    }


    public LocationTrackerService(){}


    LocationCallback mLocationCallback=new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            points.add(new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude()));
            Log.d("main", "onLocationResult: " + points.toString());
        }
    };


    public void getDeviceLocation() {

        points=new ArrayList<LatLng>();


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }



        final com.google.android.gms.tasks.Task<Location> location = ((MapsScreen) context).fusedLocationProviderClient.getLastLocation();


        location.addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull com.google.android.gms.tasks.Task<Location> task) {
                if (task.isSuccessful()) {
                      points.add(new LatLng(task.getResult().getLatitude(), task.getResult().getLongitude()));
                }
                else{
                    Toast.makeText(getApplicationContext(), "Could not fetch location!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void getLocationUpdates() {
        final LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(update_interval);
        mLocationRequest.setFastestInterval(fast_update_interval);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ((MapsScreen) context).fusedLocationProviderClient.requestLocationUpdates(mLocationRequest,mLocationCallback, Looper.myLooper());
    }

    public class LocalBinder extends Binder{
        public LocationTrackerService getService(Context ctx){
            context=ctx;
            return LocationTrackerService.this;
        }
    }




}
