package com.spotitrace.spotitrace;

import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
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
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends ActionBarActivity implements ConnectionCallbacks, OnConnectionFailedListener {
    private List<Song> songs;
    
    private static final int REQUEST_CODE = 1337;
    private static final String CLIENT_ID = "d3d6beb8d2a04634bd0eeec107c11e18";
    private static final String REDIRECT_URI = "spotitrace-login://callback";
    private String accessToken;
    private String username;

    private GoogleApiClient mApiClient;
    protected Location mLastLocation;
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

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        //builder.setScopes(new String[]{"user-read-private", "streaming"});
        builder.setScopes(new String[]{"user-read-private"});
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
                UserFetcher userFetcher = new UserFetcher();
                userFetcher.execute();
                SongFetcher fetcher = new SongFetcher();
                fetcher.execute();
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

    public SongFetcher getFetcher(){
        return new SongFetcher();
    }

    public void update(){
        getFetcher().execute();
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mApiClient);
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
        Toast.makeText(this, "Latitude ="+mLastLocation.getLatitude()+" Longitude= "+ mLastLocation.getLongitude(), Toast.LENGTH_LONG).show();
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




    public void setSongs(List<Song> songs){
        this.songs = songs;
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        MainActivity ma;
        ListView listView;

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

        private void handleSongsList(List<Song> songs) {
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

    private class SongFetcher extends AsyncTask<Void, Void, String> {
        private static final String TAG = "SongFetcher";
        public static final String SERVER_URL = "http://spotitrace.herokuapp.com/api/songs";

        @Override
        protected String doInBackground(Void... params) {
            try {
                //Create an HTTP client
                HttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet(SERVER_URL);

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

                        PlaceholderFragment fragment = (PlaceholderFragment)getSupportFragmentManager().findFragmentByTag("Fragment1");
                        fragment.handleSongsList(songs);
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

    private class UserFetcher extends AsyncTask<Void, Void, String> {
        private static final String TAG = "UserFetcher";
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
