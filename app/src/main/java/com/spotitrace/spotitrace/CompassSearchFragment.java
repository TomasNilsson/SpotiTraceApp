package com.spotitrace.spotitrace;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class CompassSearchFragment extends Fragment implements SensorEventListener {
    protected final String TAG = "CompassSearchFragment";
    private Button findUserButton;
    private SensorManager mSensorManager;
    private MainActivity ma;
    private Sensor mCompass;
    private double bearing;
    private boolean findUser;
    private double deltaBearing;
    private ListView listView;
    private ArrayList<User> userSingleList;

    public CompassSearchFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_compass_search, container, false);

        return rootView;
    }

    @Override
    public void onStart(){
        ma = (MainActivity) getActivity();
        super.onStart();
        findUserButton = (Button)getView().findViewById(R.id.compass_button);
        mSensorManager = (SensorManager) ma.getSystemService(ma.SENSOR_SERVICE);
        mCompass = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        listView = (ListView)getView().findViewById(R.id.list);
        userSingleList = new ArrayList<User>();

        findUserButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                findUser = true;
            }
        });
    }

    @Override
    public void onResume(){
        super.onResume();
        mSensorManager.registerListener(this, mCompass, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause(){
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event){
        if(findUser) {
            bearing = event.values[0];
            setNewMasterUser();
            findUser = false;
            ImageListAdapter adapter = new ImageListAdapter(ma, userSingleList);
            listView.setAdapter(adapter);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){

    }

    public void setNewMasterUser(){
        if(!userSingleList.isEmpty()){
            userSingleList.remove(0);
        }
        List<User> users = ma.getUsers();
        User bestUser = users.get(0);
        double deltaBearing = Math.abs(bearing-bestUser.bearing);
        double bestScore = calculateScore(bestUser);
        for( User u : users){
            if(bestScore>calculateScore(u)){
                bestScore=calculateScore(u);
                bestUser=u;
                deltaBearing = Math.abs(bearing-bestUser.bearing);
            }
        }
        if(deltaBearing < 20) {
            userSingleList.add(bestUser);
            ma.mMasterUser = bestUser;
            ma.startSong();
        }else{
            Toast toast = Toast.makeText(ma, "Could not find any users, try again!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private double calculateScore(User user){
        return Math.abs(bearing-user.bearing)*Math.sqrt(user.distance);
    }



}
