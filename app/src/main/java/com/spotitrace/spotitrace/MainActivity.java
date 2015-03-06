package com.spotitrace.spotitrace;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.playback.Config;
import com.spotify.sdk.android.playback.ConnectionStateCallback;


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

import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.spotify.sdk.android.playback.Player;
import com.spotify.sdk.android.playback.PlayerNotificationCallback;
import com.spotify.sdk.android.playback.PlayerState;

public class MainActivity extends ActionBarActivity implements ConnectionCallbacks, OnConnectionFailedListener, PlayerNotificationCallback, ConnectionStateCallback {
    private List<Song> songs;
    private List<User> users;
    
    private static final int REQUEST_CODE = 1337;
    private static final String CLIENT_ID = "d3d6beb8d2a04634bd0eeec107c11e18";
    private static final String REDIRECT_URI = "spotitrace-login://callback";
    public static final String EXTRA_ARTIST = "com.spotitrace.spotitrace.ARTIST";
    public static final String EXTRA_ALBUM = "com.spotitrace.spotitrace.ALBUM";
    public static final String EXTRA_NAME = "com.spotitrace.spotitrace.NAME";
    public static final String EXTRA_URI = "com.spotitrace.spotitrace.URI";
    private static String accessToken;
    private String username;
    private Song currentSong;
    private SpotifyReceiver spotifyReceiver;
    private User mMasterUser;
    private GoogleApiClient mApiClient;
    private Location mLastLocation;
    private Player mPlayer;
    protected final String TAG="MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment(),"Fragment1")
                    .commit();
        }
        songs = new ArrayList<Song>();
        users = new ArrayList<User>();

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        // For non-Premium users:
        //builder.setScopes(new String[]{"user-read-private"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                accessToken = response.getAccessToken();
                Log.d("MainActivity", "User logged in");
                SpotifyUserFetcher userFetcher = new SpotifyUserFetcher();
                userFetcher.execute();
                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                    @Override
                    public void onInitialized(Player player) {
                        mPlayer.addConnectionStateCallback(MainActivity.this);
                        mPlayer.addPlayerNotificationCallback(MainActivity.this);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public List<Song> getSongs(){
        return songs;
    }

    public List<User> getUsers(){
        return users;
    }

    public void update(){
        Log.d(TAG, "update()");
        UserFetcher fetcher = new UserFetcher();
        fetcher.execute();
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mApiClient);
        LocationUploader uploader = new LocationUploader();
        uploader.execute();

    }

    protected synchronized void buildGoogleApiClient(){
        mApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onStart(){
        super.onStart();
        buildGoogleApiClient();
        mApiClient.connect();
    }

    @Override
    public void onStop(){
        super.onStop();
        if(mApiClient.isConnected()){
            mApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint){
        // TODO: Check if location is activated
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mApiClient);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result){
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause){
        Log.i(TAG, "Connection suspended");
        mApiClient.connect();
    }

    private void registerSpotifyReceiver() {
        IntentFilter filter = new IntentFilter("com.spotify.music.metadatachanged");
        spotifyReceiver = new SpotifyReceiver();
        registerReceiver(spotifyReceiver, filter);
    }

    public static String getAccessToken() {
        return accessToken;
    }

    private void startSong() {
        MasterUserUploader masterUserUploader = new MasterUserUploader();
        masterUserUploader.execute();
        currentSong = mMasterUser.song; // The file path of the clicked image
        mPlayer.play(currentSong.uri);
        // Start in Spotify app
        //Intent launcher = new Intent( Intent.ACTION_VIEW, Uri.parse(song.uri));
        //startActivity(launcher);
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Throwable error) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d("MainActivity", "Playback event received: " + eventType.name());
        if (eventType == EventType.TRACK_END) {
            update();
        } else if (eventType == EventType.TRACK_START) {
            currentSong = mMasterUser.song;
            Intent i = new Intent(this, UploadService.class);
            // potentially add data to the intent
            i.putExtra(EXTRA_ARTIST, currentSong.artist);
            i.putExtra(EXTRA_NAME, currentSong.name);
            i.putExtra(EXTRA_URI, currentSong.uri);
            this.startService(i);
        }
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String errorDetails) {
        Log.d("MainActivity", "Playback error received: " + errorType.name());
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
        unregisterReceiver(spotifyReceiver);
    }


    public void setSongs(List<Song> songs){
        this.songs = songs;
    }

    public void setUsers(List<User> users){
        this.users= users;
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment implements SensorEventListener {
        protected final String TAG="PlaceholderFragment";
        MainActivity ma;
        ListView listView;
        private SensorManager mSensorManager;
        private Sensor mAccelerometer;

        private float mAccel; // Acceleration apart from gravity
        private float mAccelCurrent; // Current acceleration including gravity
        private float mAccelLast; // Last acceleration including gravity

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);


            return rootView;
        }

        @Override
        public void onStart(){
            super.onStart();
            ma = (MainActivity)getActivity();
            listView = (ListView)getView().findViewById(R.id.list);
            // Use accelerometer to detect shaking
            mSensorManager = (SensorManager) ma.getSystemService(Context.SENSOR_SERVICE);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mAccel = 0.00f;
            mAccelCurrent = SensorManager.GRAVITY_EARTH;
            mAccelLast = SensorManager.GRAVITY_EARTH;

            final SwipeRefreshLayout swipeView = (SwipeRefreshLayout)getView().findViewById(R.id.swipe_container);
            swipeView.setColorScheme(android.R.color.holo_blue_dark, android.R.color.holo_blue_light, android.R.color.holo_green_light, android.R.color.holo_green_light);
            swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener(){
                @Override
                public void onRefresh(){
                    swipeView.setRefreshing(true);
                    ( new Handler()).postDelayed( new Runnable(){
                        @Override
                        public void run(){
                            swipeView.setRefreshing(false);
                            ma.update();
                        }
                    }, 3000);
                }
            });

        }

        @Override
        public void onResume() {
            super.onResume();
            // Register sensor event listener
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            String provider = Settings.Secure.getString(ma.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            if(!provider.contains("gps")){
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
            mAccelCurrent = (float) Math.sqrt((double) (x*x + y*y + z*z));
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta; // Perform low-cut filter
            if (mAccel > 10) {
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
        }

       /* private void handleSongsList(List<Song> songs) {
            ma.setSongs(songs);

            ma.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    ImageListAdapter adapter = new ImageListAdapter(ma, ma.getSongs());
                    listView.setAdapter(adapter);

                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
                            // Start song in Spotify
                            Song song = ma.getSongs().get(position); // The file path of the clicked image
                            String uri = song.uri;
                            Intent launcher = new Intent( Intent.ACTION_VIEW, Uri.parse(uri));
                            startActivity(launcher);
                        }
                    });
                }
            });
        }*/

        private void handleUsersList(List<User> users){
            ma.setUsers(users);
            if (ma.mMasterUser != null) {
                for (User user:users) {
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

                    ImageListAdapter adapter = new ImageListAdapter(ma, ma.getUsers());
                    listView.setAdapter(adapter);

                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
                            // Start song in Spotify
                            ma.mMasterUser = ma.getUsers().get(position);
                            ma.startSong();
                        }
                    });
                }
            });
        }
    }



    private void failedLoadingSongs() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Failed to load Songs. Have a look at LogCat.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class UserFetcher extends AsyncTask<Void, Void, String>{
        private static final String TAG = "UserFetcher";
        public static final String SERVER_URL = "http://spotitrace.herokuapp.com/api/users/nearby";
        @Override
        protected String doInBackground(Void... params) {
            try {
                //Create an HTTP client
                HttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet(SERVER_URL);
                request.setHeader("Authorization", "Token token=\"" + getAccessToken() + "\"");

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
                        List<User> users = new ArrayList<User>();

                        users = Arrays.asList(gson.fromJson(reader, User[].class));
                        Log.d(TAG,""+users.size()+" User="+users.get(0).username+" Bearing= "+users.get(0).bearing);
                        content.close();

                        PlaceholderFragment fragment = (PlaceholderFragment)getSupportFragmentManager().findFragmentByTag("Fragment1");
                        fragment.handleUsersList(users);
                    } catch (Exception ex) {
                        Log.e(TAG, "Failed to parse JSON due to: " + ex);
                        failedLoadingSongs();
                    }
                } else {
                    Log.e(TAG, "Server responded with status code: " + statusLine.getStatusCode());
                    failedLoadingSongs();
                }
            } catch(Exception ex) {
                Log.e(TAG, "Failed to send HTTP GET request due to: " + ex);
                failedLoadingSongs();
            }
            return null;
        }

    }

    private class SongFetcher extends AsyncTask<Void, Void, String> {
        private static final String TAG = "SongFetcher";
        public static final String SERVER_URL = "http://spotitrace.herokuapp.com/api/songs";

        @Override
        protected String doInBackground(Void... params) {
            try {
                //Create an HTTP client
                HttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet(SERVER_URL);
                request.setHeader("Authorization", "Token token=\"" + getAccessToken() + "\"");

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
                        List<Song> songs = new ArrayList<Song>();
                        songs = Arrays.asList(gson.fromJson(reader, Song[].class));
                        content.close();

                        //PlaceholderFragment fragment = (PlaceholderFragment)getSupportFragmentManager().findFragmentByTag("Fragment1");
                        //fragment.handleSongsList(songs);
                    } catch (Exception ex) {
                        Log.e(TAG, "Failed to parse JSON due to: " + ex);
                        failedLoadingSongs();
                    }
                } else {
                    Log.e(TAG, "Server responded with status code: " + statusLine.getStatusCode());
                    failedLoadingSongs();
                }
            } catch(Exception ex) {
                Log.e(TAG, "Failed to send HTTP GET request due to: " + ex);
                failedLoadingSongs();
            }
            return null;
        }
    }

    private class SpotifyUserFetcher extends AsyncTask<Void, Void, String> {
        private static final String TAG = "SpotifyUserFetcher";
        public static final String SERVER_URL = "https://api.spotify.com/v1/me";

        @Override
        protected String doInBackground(Void... params) {
            try {
                //Create an HTTP client
                HttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet(SERVER_URL);
                request.setHeader("Authorization", "Bearer "+ accessToken);

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
                        SpotifyUser user = gson.fromJson(reader, SpotifyUser.class);
                        username = user.id;
                        Log.d(TAG, "User: " + username);
                        content.close();
                        UserUpload userUpload = new UserUpload();
                        userUpload.execute();
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

    private class UserUpload extends AsyncTask<Void, Void, String> {
        private static final String TAG = "User Upload";
        public static final String SERVER_URL = "http://spotitrace.herokuapp.com/api/users";

        @Override
        protected String doInBackground(Void... params) {
            try {
                User user = new User(username, accessToken);
                Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
                String jsonString = gson.toJson(user);

                //Create an HTTP client
                HttpClient client = new DefaultHttpClient();
                HttpPost post = new HttpPost(SERVER_URL);
                post.setHeader("Content-Type", "application/json; charset=utf-8");
                post.setEntity(new StringEntity(jsonString, HTTP.UTF_8));
                //Perform the request and check the status code
                HttpResponse response = client.execute(post);
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == 201) {
                    Log.d(TAG, "Upload completed");
                    registerSpotifyReceiver();
                    LocationUploader uploader = new LocationUploader();
                    uploader.execute();
                } else {
                    Log.e(TAG, "Server responded with status code: " + statusLine.getStatusCode());
                }
            } catch(Exception ex) {
                Log.e(TAG, "Failed to send HTTP POST request due to: " + ex);
            }
            return null;
        }
    }

    private class LocationUploader extends AsyncTask<Void, Void, String>{
        private static final String TAG = "Location Upload";
        public static final String SERVER_URL = "http://spotitrace.herokuapp.com/api/locations";

        @Override
        protected String doInBackground(Void... params) {
            try {
                SpotiTraceLocation location = new SpotiTraceLocation(mLastLocation.getLongitude(), mLastLocation.getLatitude());
                Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
                String jsonString = gson.toJson(location);

                //Create an HTTP client
                HttpClient client = new DefaultHttpClient();
                HttpPost post = new HttpPost(SERVER_URL);
                post.setHeader("Content-Type", "application/json; charset=utf-8");
                post.setHeader("Authorization", "Token token=\"" + MainActivity.getAccessToken() + "\"");
                post.setEntity(new StringEntity(jsonString, HTTP.UTF_8));
                //Perform the request and check the status code
                HttpResponse response = client.execute(post);
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == 201) {
                    Log.d(TAG, "Upload completed");
                    UserFetcher fetcher = new UserFetcher();
                    fetcher.execute();
                } else {
                    Log.e(TAG, "Server responded with status code: " + statusLine.getStatusCode());
                }
            } catch(Exception ex) {
                Log.e(TAG, "Failed to send HTTP POST request due to: " + ex);
            }
            return null;
        }
    }

    private class MasterUserUploader extends AsyncTask<Void, Void, String>{
        private static final String TAG = "MasterUser Upload";
        public static final String SERVER_URL = "http://spotitrace.herokuapp.com/api/users/master_user";

        @Override
        protected String doInBackground(Void... params) {
            try {
                JsonObject jsonObject = new JsonObject();
                jsonObject.add("id", new JsonPrimitive(mMasterUser.id));
                Gson gson = new GsonBuilder().create();
                String jsonString = gson.toJson(jsonObject);
                Log.d(TAG, jsonString);

                //Create an HTTP client
                HttpClient client = new DefaultHttpClient();
                HttpPost post = new HttpPost(SERVER_URL);
                post.setHeader("Content-Type", "application/json; charset=utf-8");
                post.setHeader("Authorization", "Token token=\"" + MainActivity.getAccessToken() + "\"");
                post.setEntity(new StringEntity(jsonString, HTTP.UTF_8));
                //Perform the request and check the status code
                HttpResponse response = client.execute(post);
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == 200) {
                    Log.d(TAG, "Upload completed");
                } else {
                    Log.e(TAG, "Server responded with status code: " + statusLine.getStatusCode());
                }
            } catch(Exception ex) {
                Log.e(TAG, "Failed to send HTTP POST request due to: " + ex);
            }
            return null;
        }
    }

}
