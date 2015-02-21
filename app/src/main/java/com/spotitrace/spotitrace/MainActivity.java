package com.spotitrace.spotitrace;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
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
import java.util.Arrays;
import java.util.List;
import android.widget.ListView;


public class MainActivity extends ActionBarActivity {
    private List<Song> songs;

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

        // Move to fragment?
        //SongFetcher fetcher = new SongFetcher();
        //fetcher.execute();
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
}
