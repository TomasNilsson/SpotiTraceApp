package com.spotitrace.spotitrace;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Random;

public class MyFriendsFragment extends Fragment implements SensorEventListener, ListHandler {
    protected final String TAG = "MyFriendsFragment";
    MainActivity ma;
    ListView listView;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private float mAccel; // Acceleration apart from gravity
    private float mAccelCurrent; // Current acceleration including gravity
    private float mAccelLast; // Last acceleration including gravity
    private TextView infoView;

    public MyFriendsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_my_friends, container, false);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        ma = (MainActivity) getActivity();
        infoView = (TextView) getView().findViewById(R.id.friends_info);
        ma.onlyFriends = true;
        listView = (ListView) getView().findViewById(R.id.list);
        if (ma.loggedIn) {
            ma.update();
        }
        // Use accelerometer to detect shaking
        mSensorManager = (SensorManager) ma.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

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
                        ma.update();
                        setInfoText();
                    }
                }, 3000);
            }
        });

    }

    public void setInfoText(){
        if (ma.getUsers().isEmpty()){
            infoView.setText("You have no friends");
        }else{
            infoView.setText("");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register sensor event listener
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        String provider = Settings.Secure.getString(ma.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if (!provider.contains("gps")) {
            Toast.makeText(ma, "Please turn High Accuracy GPS on!", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister sensor event listener
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
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

    private void startRandomSong() {
        Log.d(TAG, "Start random song");
        // Start random song in Spotify
        Random rng = new Random(); // Generate random numbers
        List<User> users = ma.getUsers();
        int position = rng.nextInt(users.size());
        ma.mMasterUser = users.get(position);
        ma.startSong();
        TextView textView = (TextView) listView.getChildAt(position).findViewById(R.id.user_name);
        if (ma.mMasterUserTextView != null) {
            ma.mMasterUserTextView.setTextColor(textView.getTextColors().getDefaultColor());
        }
        ma.mMasterUserTextView = textView;
        ma.mMasterUserTextView.setTextColor(getResources().getColor(R.color.text_color));
    }

    public void handleUsersList(List<User> users) {
        ma.setUsers(users);
        if (ma.mMasterUser != null) {
            for (User user : users) {
                if (user.id == ma.mMasterUser.id) {
                    if (!user.song.uri.equals(ma.mMasterUser.song.uri)) {
                        ma.mPlayer.clearQueue();
                        ma.mPlayer.queue(user.song.uri);
                        Log.d(TAG, user.song.name + " added to queue.");
                    }
                    ma.mMasterUser = user;
                    break;
                }
            }
        }

        ma.runOnUiThread(new Runnable() {
            @Override
            public void run() {


                setInfoText();

                ImageListAdapter adapter = new ImageListAdapter(ma, ma.getUsers());
                listView.setAdapter(adapter);

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
                        // Start song in Spotify
                        ma.mMasterUser = ma.getUsers().get(position);
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

}
