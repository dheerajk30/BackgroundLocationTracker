package com.example.dheeraj.backgroundlocationtracker;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class MapsScreen extends FragmentActivity implements OnMapReadyCallback {

    public boolean isStarted = false;
    public LocationTrackerService service;
    public Button infobtn;
    long update_interval=30000,fast_update_interval=30000;
    public TextView tvstatus;
    public BroadcastReceiver broadcastReceiver;
    public static FusedLocationProviderClient fusedLocationProviderClient;
    public long startTime, stopTime;
    private GoogleMap mMap;
    Polyline line;
    Vibrator vibrator;
    ArrayList<LatLng> points;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_screen);
        init();
        checkProviderDisabled();
        checkPermissions();
    }

    public void init(){
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        isStarted = false;
        tvstatus=(TextView) findViewById(R.id.status_text);
        tvstatus.setVisibility(View.GONE);
        infobtn=(Button) findViewById(R.id.infobtn);
        infobtn.setVisibility(View.GONE);
        points = new ArrayList<LatLng>();
    }




    LocationCallback mLocationCallback=new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            mMap.clear();

            points.add(new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude()));

            PolylineOptions options = new PolylineOptions().width(5).color(Color.RED).geodesic(true);
            for (int i = 0; i < points.size(); i++) {
                LatLng point = points.get(i);
                options.add(point);
            }

            line = mMap.addPolyline(options);

            Log.d("main", "onLocationResult: " + points.toString());
        }
    };


    public void checkPermissions() {
        int coarse = ActivityCompat.checkSelfPermission(MapsScreen.this, Manifest.permission.ACCESS_COARSE_LOCATION);
        int fine = ActivityCompat.checkSelfPermission(MapsScreen.this, Manifest.permission.ACCESS_FINE_LOCATION);


        if (coarse == PackageManager.PERMISSION_GRANTED && fine == PackageManager.PERMISSION_GRANTED) {
            initMap();
        } else {
            askPermissions();
        }

    }

    public void askPermissions() {
        ActivityCompat.requestPermissions(MapsScreen.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
    }

    public void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }


    public ServiceConnection connection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            LocationTrackerService.LocalBinder binder=(LocationTrackerService.LocalBinder) iBinder;
            service=binder.getService(MapsScreen.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };



    @Override
    protected void onResume() {
        super.onResume();
        if(broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    points=intent.getParcelableArrayListExtra("points");
                    PolylineOptions options = new PolylineOptions().width(5).color(Color.RED).geodesic(true);
                    for (int i = 0; i < points.size(); i++) {
                        LatLng point = points.get(i);
                        options.add(point);
                    }
                    line = mMap.addPolyline(options);
                }
            };
        }
        registerReceiver(broadcastReceiver,new IntentFilter("Location_Update"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(broadcastReceiver!=null)
            unregisterReceiver(broadcastReceiver);
    }

    public void getDeviceLocation() {


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        final com.google.android.gms.tasks.Task<Location> location = fusedLocationProviderClient.getLastLocation();


        location.addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull com.google.android.gms.tasks.Task<Location> task) {

                if (task.isSuccessful() && task.getResult()!=null) {
                    points.add(new LatLng(task.getResult().getLatitude(), task.getResult().getLongitude()));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(task.getResult().getLatitude(), task.getResult().getLongitude()), 15f));
                }
                else {
                    Toast.makeText(MapsScreen.this, "Could not fetch location!", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));


        getDeviceLocation();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==101){
            checkPermissions();
        }
    }

    public void startShift(View view) {
        if(!isStarted){
            isStarted=true;
            vibrateNotification();
            tvstatus.setVisibility(View.VISIBLE);
            mMap.clear();
            infobtn.setVisibility(View.GONE);
            startTime=System.currentTimeMillis();
            Toast.makeText(MapsScreen.this,"Shift Started! ",Toast.LENGTH_SHORT).show();
            getDeviceLocation();

            Intent it=new Intent(MapsScreen.this,LocationTrackerService.class);
            bindService(it,connection,Context.BIND_AUTO_CREATE);
            Log.d("main", "startShift: started at"+startTime);
        }
        else{
            Toast.makeText(MapsScreen.this,"A shift is running currently! Please wait",Toast.LENGTH_SHORT).show();
        }
    }

    public void stopShift(View view) {


        if(isStarted){

            isStarted=false;
            stopTime=System.currentTimeMillis();
            infobtn.setVisibility(View.VISIBLE);
            tvstatus.setVisibility(View.GONE);
            unbindService(connection);
            vibrateNotification();
            long difftime=(stopTime-startTime)/1000;
            Toast.makeText(MapsScreen.this,"Shift Stopped, Now you Track its Information now",Toast.LENGTH_LONG).show();
            stopService(new Intent(MapsScreen.this,LocationTrackerService.class));
            Log.d("main", "stopShift: starttime"+startTime+"stoptime"+stopTime+"diff"+difftime);

        }
        else{
            Toast.makeText(MapsScreen.this,"No running shift was found!",Toast.LENGTH_SHORT).show();
        }
    }


    public void showinfo(View view) {

        final AlertDialog dialog=new AlertDialog.Builder(MapsScreen.this).create();
        dialog.setTitle("Detailed Shift Information");
        View v=LayoutInflater.from(MapsScreen.this).inflate(R.layout.infolayout,null,false);

        TextView tvtotaltime=(TextView)v.findViewById(R.id.totaltime);
        TextView tvstarttime=(TextView)v.findViewById(R.id.starttime);
        TextView tvendtime=(TextView)v.findViewById(R.id.endtime);
        TextView tvnopoints=(TextView) v.findViewById(R.id.nopoints);
        TextView tvuniquenopoints=(TextView) v.findViewById(R.id.uniquenopoints);
        final TextView tvcontentpoints=(TextView) v.findViewById(R.id.contentallpts);

        tvstarttime.setText("Start Time : "+formathhmmss(startTime/1000));
        tvendtime.setText("End Time : "+formathhmmss(stopTime/1000));
        tvstarttime.setVisibility(View.GONE);
        tvendtime.setVisibility(View.GONE);
        tvtotaltime.setText(""+formathhmmss((stopTime-startTime)/1000));
        tvnopoints.setText(""+points.size());


        ArrayList<LatLng> unique=new ArrayList<LatLng>();

        final StringBuilder s=new StringBuilder();
        for(LatLng l : points){
           s.append("Lat:"+l.latitude+" Lon:"+l.longitude+"\n");
           if(!unique.contains(l))
                unique.add(l);
        }

        final StringBuilder sunique=new StringBuilder();
        for(int i=0;i<unique.size();i++){
            sunique.append("Lat:"+(unique.toArray(new LatLng[unique.size()]))[i].latitude+" Lon:"+(unique.toArray(new LatLng[unique.size()]))[i].longitude+"\n");

        }




        CheckBox cbunique=(CheckBox) v.findViewById(R.id.cbunique);
        tvcontentpoints.setText(s.toString());

        cbunique.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    tvcontentpoints.setText(sunique.toString());
                }
                else{
                    tvcontentpoints.setText(s.toString());
                }
            }
        });


        tvuniquenopoints.setText(""+unique.size());


        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Okay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialog.dismiss();
            }
        });
        dialog.setView(v);
        dialog.show();
    }



    public String formathhmmss(long time){

        long nsecs=time%60;
        long nmins=(time/60) % 60 ;
        long nhrs=((time/60*60))%24;
        String b=nhrs+" hrs, "+nmins+" mins, "+nsecs+" secs,";
        return b;

    }


    public void vibrateNotification(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100,VibrationEffect.DEFAULT_AMPLITUDE));
        }else{
            vibrator.vibrate(100);
        }
    }


    public void checkProviderDisabled(){
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}


        if(!gps_enabled && !network_enabled)
            providerDisabled();

    }


    public void providerDisabled() {




        final AlertDialog.Builder dialog = new AlertDialog.Builder(MapsScreen.this);
        dialog.setMessage("The location services are turned off, please enable them to proceed");
        dialog.setPositiveButton("Enable Location", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(myIntent);
            }
        });
        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
            }
        });
        dialog.show();
    }
}

