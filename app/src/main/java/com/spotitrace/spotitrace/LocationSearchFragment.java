package com.spotitrace.spotitrace;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class LocationSearchFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, ListHandler, SensorEventListener {
    protected final String TAG = "LocationSearchFragment";
    private SupportMapFragment mMapFragment;
    private static GoogleMap mMap;
    private MainActivity activity;
    private GoogleMapOptions options;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private static LatLng location;
    private MainActivity ma;
    private List<User> users;
    private ListView listView;
    private TextView textView;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private float mAccel; // Acceleration apart from gravity
    private float mAccelCurrent; // Current acceleration including gravity
    private float mAccelLast; // Last acceleration including gravity

    public LocationSearchFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_location_search, container, false);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        ma = (MainActivity) getActivity();
        listView = (ListView)getView().findViewById(R.id.list);
        textView = (TextView)getView().findViewById(R.id.info_box);
        if(location != null){
            UserFetcher userFetcher = new UserFetcher();
            userFetcher.execute();
        }
        activity = (MainActivity) getActivity();
        Button button = (Button) getView().findViewById(R.id.map_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                options = new GoogleMapOptions();
                Location myLocation = activity.mLastLocation;
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(myLocation.getLatitude(),myLocation.getLongitude()))
                        .zoom(13)
                        .build();
                options.mapType(GoogleMap.MAP_TYPE_NORMAL)
                        .camera(cameraPosition)
                        .compassEnabled(false)
                        .rotateGesturesEnabled(false)
                        .zoomControlsEnabled(true)
                        .zoomGesturesEnabled(true)
                        .tiltGesturesEnabled(false);
                mMapFragment = SupportMapFragment.newInstance(options);
                mMapFragment.getMapAsync(LocationSearchFragment.this);
                FragmentManager fragmentManager = activity.getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.frame_container, mMapFragment, "SpotiTraceFragment").commit();
            }
        });
        final SwipeRefreshLayout swipeView = (SwipeRefreshLayout) getView().findViewById(R.id.swipe_container);
        swipeView.setColorScheme(android.R.color.holo_blue_dark, android.R.color.holo_blue_light, android.R.color.holo_green_light, android.R.color.holo_green_light);
        swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeView.setRefreshing(true);
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeView.setRefreshing(false);
                        if(location != null){
                            UserFetcher userFetcher = new UserFetcher();
                            userFetcher.execute();
                        }
                    }
                }, 3000);
            }
        });
        // Use accelerometer to detect shaking
        mSensorManager = (SensorManager) ma.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
    }

    @Override
    public void onResume(){
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause(){
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        map.setMyLocationEnabled(true);
        map.setOnMapLongClickListener(this);
        mMap = map;
    }

    @Override
    public void onMapLongClick(LatLng point) {
        location = point;
        mMap.addMarker(new MarkerOptions()
                .position(point));
        // Create an instance of the dialog fragment and show it
        DialogFragment dialog = new ConfirmLocationFragment();
        dialog.show(activity.getSupportFragmentManager(), "ConfirmLocationFragment");
    }

    public static class ConfirmLocationFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.map_dialog_message)
                    .setTitle(R.string.map_dialog_title)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                            fragmentManager.beginTransaction()
                                    .replace(R.id.frame_container, new LocationSearchFragment(), "SpotiTraceFragment").commit();
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            mMap.clear();
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    private class UserFetcher extends AsyncTask<Void, Void, String> {
        private static final String TAG = "UserFetcher";
        public final String SERVER_URL = "http://spotitrace.herokuapp.com/api/users/location";


        @Override
        protected String doInBackground(Void... params) {
            try {

                SpotiTraceLocation loc = new SpotiTraceLocation(location.longitude,location.latitude);
                Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
                String jsonString = gson.toJson(loc);
                Log.d(TAG,"jsonString= "+jsonString);


                //Create an HTTP client
                HttpClient client = new DefaultHttpClient();
                HttpPost post = new HttpPost(SERVER_URL);
                post.setHeader("Content-Type", "application/json; charset=utf-8");
                post.setHeader("Authorization", "Token token=\"" + MainActivity.getAccessToken() + "\"");
                post.setEntity(new StringEntity(jsonString, HTTP.UTF_8));
                //Perform the request and check the status code
                HttpResponse response = client.execute(post);
                StatusLine statusLine = response.getStatusLine();

                if(statusLine.getStatusCode() == 200) {
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();

                    try {
                        //Read the server response and attempt to parse it as JSON
                        Reader reader = new InputStreamReader(content);

                        GsonBuilder gsonBuilder = new GsonBuilder();
                        gson = gsonBuilder.create();
                        users = new ArrayList<User>();

                        users = Arrays.asList(gson.fromJson(reader, User[].class));

                        ma.setRecentUsers( users);
                        content.close();
                        Log.d(TAG, "Found "+users.size()+" users");

                        handleUsersList(users);




                    } catch (Exception ex) {
                        Log.e(TAG, "Failed to parse JSON due to: " + ex);
                    }
                } else {
                    Log.e(TAG, "Server responded with status code: " + statusLine.getStatusCode());
                }
            } catch(Exception ex) {
                Log.e(TAG, "Failed to send HTTP GET request due to: " + ex);
            }
            return null;
        }

    }

    public void handleUsersList(final List<User> users){


        ma.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(users.isEmpty()){
                    textView.setText("Sorry, we were not able to find any SpotiTrace users close to the location you choose, please try again.");
                }else {
                    textView.setText("");
                }

                ImageListAdapter adapter = new ImageListAdapter(ma, users);
                listView.setAdapter(adapter);

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
                        ma.mMasterUser = ma.getRecentUsers().get(position);
                        ma.startSong();
                        TextView textView = (TextView)v.findViewById(R.id.user_name);
                        if (ma.mMasterUserTextView != null) {
                            ma.mMasterUserTextView.setTextColor(textView.getTextColors().getDefaultColor());
                        }
                        ma.mMasterUserTextView = textView;
                        ma.mMasterUserTextView.setTextColor(getResources().getColor(R.color.text_color));
                    }
                });
            }
        });

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Detect shaking
        // (code partially from http://stackoverflow.com/questions/2317428/android-i-want-to-shake-it)
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        mAccelLast = mAccelCurrent;
        mAccelCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
        float delta = mAccelCurrent - mAccelLast;
        mAccel = mAccel * 0.9f + delta; // Perform low-cut filter
        if (mAccel > ma.shakeLimit) {
            startRandomSong(); // Call rolling() method when phone is shaken.
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){

    }

    private void startRandomSong() {
        Log.d(TAG, "Start random song");
        // Start random song in Spotify
        Random rng = new Random(); // Generate random numbers
        List<User> users = ma.getRecentUsers();
        int position = rng.nextInt(users.size());
        ma.mMasterUser = users.get(position);
        ma.startSong();
    }

}
