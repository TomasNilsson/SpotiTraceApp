package com.spotitrace.spotitrace;

import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
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
import java.nio.ByteBuffer;
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
import com.spotitrace.spotitrace.adapter.NavDrawerListAdapter;
import com.spotitrace.spotitrace.model.NavDrawerItem;

public class MainActivity extends ActionBarActivity
        implements ConnectionCallbacks, OnConnectionFailedListener, PlayerNotificationCallback, ConnectionStateCallback, FriendAdder {
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    //To be able to change count
    private NavDrawerItem whoFollowsMe;

    // nav drawer title
    private CharSequence mDrawerTitle;

    // used to store app title
    private CharSequence mTitle;

    // slide menu items
    private String[] navMenuTitles;
    private TypedArray navMenuIcons;

    private ArrayList<NavDrawerItem> navDrawerItems;
    private NavDrawerListAdapter adapter;

    private List<User> users;
    private List<User> recentUsers;
    
    private static final int REQUEST_CODE = 1337;
    private static final String CLIENT_ID = "d3d6beb8d2a04634bd0eeec107c11e18";
    private static final String REDIRECT_URI = "spotitrace-login://callback";
    public static final String EXTRA_ARTIST = "com.spotitrace.spotitrace.ARTIST";
    public static final String EXTRA_NAME = "com.spotitrace.spotitrace.NAME";
    public static final String EXTRA_URI = "com.spotitrace.spotitrace.URI";
    private static String accessToken;
    protected String username;
    protected long userId;
    private Song currentSong;
    private SpotifyReceiver spotifyReceiver;
    protected long nfcMasterUserId;
    protected User mMasterUser;
    protected TextView mMasterUserTextView;
    protected boolean onlyFriends;
    protected boolean loggedIn;
    protected boolean isPlaying;
    private boolean newIntent = false;
    private GoogleApiClient mApiClient;
    protected Location mLastLocation;
    protected Player mPlayer;
    protected final String TAG="MainActivity";
    protected int shakeLimit = 13;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");

        // Action Bar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        LayoutInflater inflator = LayoutInflater.from(this);
        View v = inflator.inflate(R.layout.action_bar, null);

        actionBar.setCustomView(v);

        // Menu
        mTitle = mDrawerTitle = getTitle();

        // Load slide menu items
        navMenuTitles = getResources().getStringArray(R.array.nav_drawer_items);

        // Nav drawer icons from resources
        navMenuIcons = getResources()
                .obtainTypedArray(R.array.nav_drawer_icons);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.list_slidermenu);

        navDrawerItems = new ArrayList<NavDrawerItem>();

        // Add nav drawer items to array
        // Nearby Users
        navDrawerItems.add(new NavDrawerItem(navMenuTitles[0], navMenuIcons.getResourceId(0, -1)));
        // My Friends
        navDrawerItems.add(new NavDrawerItem(navMenuTitles[1], navMenuIcons.getResourceId(1, -1)));
        // Who Follows Me? Will add a counter here
        whoFollowsMe = new NavDrawerItem(navMenuTitles[2], navMenuIcons.getResourceId(2, -1), true, "0");
        navDrawerItems.add(whoFollowsMe);
        // Location Search
        navDrawerItems.add(new NavDrawerItem(navMenuTitles[3], navMenuIcons.getResourceId(3, -1)));
        // Compass Search
        navDrawerItems.add(new NavDrawerItem(navMenuTitles[4], navMenuIcons.getResourceId(4, -1)));
        // NFC Connect
        navDrawerItems.add(new NavDrawerItem(navMenuTitles[5], navMenuIcons.getResourceId(5, -1)));
        // Log Out
        navDrawerItems.add(new NavDrawerItem(navMenuTitles[6], navMenuIcons.getResourceId(6, -1)));

        // Recycle the typed array
        navMenuIcons.recycle();

        // setting the nav drawer list adapter
        adapter = new NavDrawerListAdapter(getApplicationContext(),
                navDrawerItems);
        mDrawerList.setAdapter(adapter);

        // enabling action bar app icon and behaving it as toggle button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.app_name, // nav drawer open - description for accessibility
                R.string.app_name // nav drawer close - description for accessibility
        ){
            public void onDrawerClosed(View view) {
                setTitle(mTitle);
                // calling onPrepareOptionsMenu() to show action bar icons
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                setTitle("");
                // calling onPrepareOptionsMenu() to hide action bar icons
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerList.setOnItemClickListener(new SlideMenuClickListener());

        // Display view for first nav item
        displayView(0);

        users = new ArrayList<User>();
        recentUsers = new ArrayList<User>();

        spotifyLogin();

        final ImageView playbackControl = (ImageView)findViewById(R.id.playback_control);
        playbackControl.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isPlaying) {
                    mPlayer.pause();
                    playbackControl.setImageResource(R.drawable.ic_play);
                    isPlaying = false;
                } else {
                    mPlayer.resume();
                    playbackControl.setImageResource(R.drawable.ic_pause);
                    isPlaying = true;
                }
            }
        });
    }

    public void setFollowerCounter(int nbrOfFollowers){
        whoFollowsMe.setCount(""+nbrOfFollowers);
        adapter.notifyDataSetChanged();
    }

    public void spotifyLogin() {
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
                //Log.d("MainActivity", "User logged in");
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
            } else {
                Log.d(TAG, "Back button pressed");
                spotifyLogin();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        // Check to see that the Activity started due to an Android Beam (NFC)
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction()) && newIntent) {
            processIntent(getIntent());
            newIntent = false;
        }
    }

    // Needed for NFC
    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        Log.d(TAG, "onNewIntent");
        newIntent = true;
        setIntent(intent);
    }

    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    void processIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put(msg.getRecords()[0].getPayload());
        buffer.flip();
        nfcMasterUserId = buffer.getLong();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.frame_container, new NfcConnectFragment(), "SpotiTraceFragment").commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // toggle nav drawer on selecting action bar app icon/title
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action bar actions click
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /***
     * Called when invalidateOptionsMenu() is triggered
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // if nav drawer is opened, hide the action items
        // boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        // menu.findItem(R.id.action_settings).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void setTitle(CharSequence title) {
        if (title.length() > 0) {
            mTitle = title;
        }
        //getSupportActionBar().setTitle(mTitle);
        ((TextView) getSupportActionBar().getCustomView().findViewById(R.id.title)).setText(title);
    }

    /**
     * When using the ActionBarDrawerToggle, it must be called during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Slide menu item click listener
     * */
    private class SlideMenuClickListener implements
            ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            // display view for selected nav drawer item
            displayView(position);
        }
    }

    /**
     * Diplaying fragment view for selected nav drawer list item
     * */
    private void displayView(int position) {
        // update the main content by replacing fragments
        Fragment fragment = null;
        switch (position) {
            case 0:
                fragment = new NearbyUsersFragment();
                break;
            case 1:
                fragment = new MyFriendsFragment();
                break;
            case 2:
                fragment = new WhoFollowsMeFragment();
                break;
            case 3:
                fragment = new LocationSearchFragment();
                break;
            case 4:
                fragment = new CompassSearchFragment();
                break;
            case 5:
                fragment = new NfcConnectFragment();
                break;
            case 6:
                mPlayer.logout();
                AuthenticationClient.logout(this);
                // Mark first menu item as active when activity is recreated
                mDrawerList.performItemClick(mDrawerList, 0, mDrawerList.getItemIdAtPosition(0));
                this.recreate();
                break;
            default:
                break;
        }

        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.frame_container, fragment, "SpotiTraceFragment").commit();

            // update selected item and title, then close the drawer
            mDrawerList.setItemChecked(position, true);
            mDrawerList.setSelection(position);
            setTitle(navMenuTitles[position]);
            mDrawerLayout.closeDrawer(mDrawerList);
        } else {
            // error in creating fragment
            Log.e("MainActivity", "Error in creating fragment");
        }
    }

    public List<User> getUsers(){
        return users;
    }
    public List<User> getRecentUsers() { return recentUsers; }

    public void update() {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mApiClient);
        LocationUploader uploader = new LocationUploader();
        uploader.execute();

    }

    public void updateUserList() {
        UserFetcher fetcher = new UserFetcher(onlyFriends);
        fetcher.execute();
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
        spotifyReceiver = new SpotifyReceiver(this);
        registerReceiver(spotifyReceiver, filter);
    }

    public static String getAccessToken() {
        return accessToken;
    }

    public void startSong() {
        MasterUserUploader masterUserUploader = new MasterUserUploader();
        masterUserUploader.execute();
        currentSong = mMasterUser.song; // The file path of the clicked image
        mPlayer.play(currentSong.uri);
        ImageView playbackControl = (ImageView)findViewById(R.id.playback_control);
        playbackControl.setImageResource(R.drawable.ic_pause);
        isPlaying = true;

        // Start in Spotify app
        //Intent launcher = new Intent( Intent.ACTION_VIEW, Uri.parse(song.uri));
        //startActivity(launcher);
    }

    public void removeMaster(){

        if(mMasterUser != null) {
            MasterUserRemover masterUserRemover = new MasterUserRemover();
            masterUserRemover.execute();
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
        loggedIn = true;
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
        loggedIn = false;
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
        if (eventType == EventType.TRACK_END || eventType == EventType.END_OF_CONTEXT) {
            isPlaying = false;
            update();
        } else if (eventType == EventType.TRACK_START) {
            isPlaying = true;
            View bottomBar = findViewById(R.id.bottom_bar);
            if (bottomBar.getVisibility() != View.VISIBLE) {
                bottomBar.setVisibility(View.VISIBLE);
            }
            currentSong = mMasterUser.song;
            TextView songTextView = (TextView)findViewById(R.id.bottom_bar_song);
            songTextView.setText(currentSong.name);
            TextView artistTextView = (TextView)findViewById(R.id.bottom_bar_artist);
            artistTextView.setText(currentSong.artist);

            Intent i = new Intent(this, UploadService.class);
            // Add data to intent
            i.putExtra(EXTRA_ARTIST, currentSong.artist);
            i.putExtra(EXTRA_NAME, currentSong.name);
            i.putExtra(EXTRA_URI, currentSong.uri);
            this.startService(i);
        } else if (eventType == EventType.LOST_PERMISSION) {
            Toast.makeText(MainActivity.this, "Your Spotify account is being used somewhere else.", Toast.LENGTH_LONG).show();
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

    public void setUsers(List<User> users){
        this.users= users;
    }

    public void setRecentUsers(List<User> users) { recentUsers = users; }

    @Override
    public void handleFriend(long id, boolean friend){
        FriendUserUploader FUU = new FriendUserUploader( id, friend);
        FUU.execute();
    }

    private class UserFetcher extends AsyncTask<Void, Void, String>{
        private static final String TAG = "UserFetcher";
        public final String SERVER_URL;
        boolean onlyFriends;

        private UserFetcher(boolean onlyFriends) {
            this.onlyFriends = onlyFriends;
            if (onlyFriends) {
                SERVER_URL = "http://spotitrace.herokuapp.com/api/friendships";
            } else {
                SERVER_URL = "http://spotitrace.herokuapp.com/api/users/nearby";
            }
        }

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
                        content.close();

                        ListHandler fragment = (ListHandler)getSupportFragmentManager().findFragmentByTag("SpotiTraceFragment");
                        fragment.handleUsersList(users);/*
                        if (onlyFriends) {
                            MyFriendsFragment fragment = (MyFriendsFragment)getSupportFragmentManager().findFragmentByTag("SpotiTraceFragment");
                            fragment.handleUsersList(users);
                        } else {
                            NearbyUsersFragment fragment = (NearbyUsersFragment) getSupportFragmentManager().findFragmentByTag("SpotiTraceFragment");
                            fragment.handleUsersList(users);
                        }*/
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

                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    try {
                        //Read the server response and attempt to parse it as JSON
                        Reader reader = new InputStreamReader(content);
                        Gson gsonRead = new GsonBuilder().create();
                        User spotiTraceUser = gsonRead.fromJson(reader, User.class);
                        userId = spotiTraceUser.id;
                        Log.d(TAG, "User ID: " + userId);
                        content.close();
                    } catch (Exception ex) {
                        Log.e(TAG, "Failed to parse JSON due to: " + ex);
                    }
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
                    Log.d(TAG, "Location Upload completed");
                    updateUserList();
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

    private class MasterUserRemover extends AsyncTask<Void, Void, String>{
        private static final String TAG = "MasterUser Remove";
        public static final String SERVER_URL = "http://spotitrace.herokuapp.com/api/users/master_user/remove";

        @Override
        protected String doInBackground(Void... params) {
            try {
                Log.d(TAG, "Removing ="+mMasterUser.username);
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
                    mMasterUser = null;
                } else {
                    Log.e(TAG, "Server responded with status code: " + statusLine.getStatusCode());
                }
            } catch(Exception ex) {
                Log.e(TAG, "Failed to send HTTP POST request due to: " + ex);
            }
            return null;
        }
    }

    private class FriendUserUploader extends AsyncTask<Void, Void, String>{
        private static final String TAG = "Friend User Uploader";
        public String SERVER_URL;
        private long id;
        public FriendUserUploader(long id, boolean friend){
            this.id =id;
            if(friend){
                SERVER_URL = "http://spotitrace.herokuapp.com/api/friendships/remove";
            }else{
                SERVER_URL = "http://spotitrace.herokuapp.com/api/friendships";
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                JsonObject jsonObject = new JsonObject();
                jsonObject.add("id", new JsonPrimitive(id));
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
                if (statusLine.getStatusCode() == 200 || statusLine.getStatusCode() == 201) {
                    Log.d(TAG, "Upload completed");
                } else {
                    Log.e(TAG, "Server responded with status code: " + statusLine.getStatusCode());
                }
            } catch (Exception ex) {
                Log.e(TAG, "Failed to send HTTP POST request due to: " + ex);
            }
            return null;
        }

    }

}
