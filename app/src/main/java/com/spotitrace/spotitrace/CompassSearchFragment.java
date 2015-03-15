package com.spotitrace.spotitrace;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class CompassSearchFragment extends Fragment implements SensorEventListener, ListHandler, UserListUpdater {
    protected final String TAG = "CompassSearchFragment";
    private Button findUserButton;
    private SensorManager mSensorManager;
    private MainActivity ma;
    private Sensor mCompass;
    private double bearing;
    private boolean findUser;
    private ListView listView;
    private static List<User> userSingleList;
    private TextView tv;

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
        super.onStart();
        ma = (MainActivity) getActivity();
        tv = (TextView) getView().findViewById(R.id.info_box);
        findUserButton = (Button)getView().findViewById(R.id.compass_button);
        mSensorManager = (SensorManager) ma.getSystemService(ma.SENSOR_SERVICE);
        mCompass = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        listView = (ListView)getView().findViewById(R.id.list);
        if(userSingleList != null){
            tv.setText("");
            ma.userSingleList = true;
            handleUsersList(userSingleList);
        }else{
            userSingleList = new ArrayList<User>();
            tv.setText(R.string.compass_info);
        }

        findUserButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                findUser = true;
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
                        if(userSingleList.isEmpty()) {
                            ma.update();
                        }else{
                            updateUserList();
                        }
                    }
                }, 3000);
            }
        });
    }

    @Override
    public void onResume(){
        super.onResume();
        mSensorManager.registerListener(this, mCompass, SensorManager.SENSOR_DELAY_NORMAL);
        if(userSingleList.isEmpty()){
            tv.setText(R.string.compass_info);
        }else{
            tv.setText("");
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        ma.userSingleList = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event){
        if(findUser) {
            bearing = event.values[0];
            setNewMasterUser();
            findUser = false;
            handleUsersList(userSingleList);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){

    }

    @Override
    public void handleUsersList(List<User> users){
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

                ImageListAdapter adapter = new ImageListAdapter(ma, userSingleList);
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

    public void setNewMasterUser(){
        userSingleList.clear();
        List<User> users = ma.getUsers();
        if(!users.isEmpty()) {
            User bestUser = users.get(0);
            double deltaBearing = Math.abs(bearing - bestUser.bearing);
            double bestScore = calculateScore(bestUser);
            for (User u : users) {
                if (bestScore > calculateScore(u)) {
                    bestScore = calculateScore(u);
                    bestUser = u;
                    deltaBearing = Math.abs(bearing - bestUser.bearing);
                }
            }
            if (deltaBearing < 20) {
                userSingleList.add(bestUser);
                ma.setRecentUsers(userSingleList);
                ma.userSingleList = true;
                ma.mMasterUser = bestUser;
                ma.startSong();
                tv.setText("");
            } else {
                Toast toast = Toast.makeText(ma, "Could not find any users, try again!", Toast.LENGTH_SHORT);
                toast.show();
                tv.setText(R.string.compass_info);
            }
        }else{
            ma.update();
            Toast toast = Toast.makeText(ma, "No nearby users found, try again!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private double calculateScore(User user){
        return Math.abs(bearing-user.bearing)*Math.sqrt(user.distance);
    }

    public void updateUserList(){
        UserFetcher userFetcher = new UserFetcher();
        userFetcher.execute();
    }

    private class UserFetcher extends AsyncTask<Void, Void, String> {
        private static final String TAG = "UserFetcher Compass";
        public final String SERVER_URL = "http://spotitrace.herokuapp.com/api/users/"+userSingleList.get(0).id;

        @Override
        protected String doInBackground(Void... params) {
            try {
                //Create an HTTP client
                HttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet(SERVER_URL);
                request.setHeader("Authorization", "Token token=\"" + ma.getAccessToken() + "\"");

                //Perform the request and check the status code
                HttpResponse response = client.execute(request);
                StatusLine statusLine = response.getStatusLine();
                if(statusLine.getStatusCode() == 200) {
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();

                    try {
                        //Read the server response and attempt to parse it as JSON
                        Reader reader = new InputStreamReader(content);

                        GsonBuilder gsonBuilder = new GsonBuilder();
                        Gson gson = gsonBuilder.create();
                        User user;

                        user =gson.fromJson(reader, User.class);
                        content.close();

                        if(user != null){
                            userSingleList.clear();
                            userSingleList.add(user);
                            Log.d(TAG, "User list updated");
                            handleUsersList(userSingleList);
                        }

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





}
