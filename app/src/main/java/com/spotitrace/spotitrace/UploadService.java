package com.spotitrace.spotitrace;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
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

public class UploadService extends IntentService {
    private static final String TAG = "UploadService";
    private static final String SERVER_URL = "http://spotitrace.herokuapp.com/api/songs";

    private String songImgUrl;
    private String name;
    private String artist;
    private String uri;
    /**
     * An IntentService must always have a constructor that calls the super constructor. The
     * string supplied to the super constructor is used to give a name to the IntentService's
     * background thread.
     */
    public UploadService() {

        super("UploadService");
    }

    public void setUrl(String url){
        songImgUrl = url;
        Log.d(TAG,"ImgUrl Found");

        //TODO: Fix fulhack.

        Song song = new Song(name, artist, uri, songImgUrl);
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        String jsonString = gson.toJson(song);

        try {
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
            } else {
                Log.e(TAG, "Server responded with status code: " + statusLine.getStatusCode());
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to send HTTP POST request due to: " + ex);
        }
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        uri = intent.getStringExtra(SpotifyReceiver.EXTRA_URI);
        name = intent.getStringExtra(SpotifyReceiver.EXTRA_NAME);
        artist = intent.getStringExtra(SpotifyReceiver.EXTRA_ARTIST);
        SpotifySongFetcher SF = new SpotifySongFetcher(uri, this);
        SF.execute();
        // Request image_url from Spotify? Or fix this server side?

        /*Song song = new Song(name, artist, album, uri, songImgUrl);
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        String jsonString = gson.toJson(song);

        try {
            //Create an HTTP client
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(SERVER_URL);
            post.setHeader("Content-Type", "application/json; charset=utf-8");
            post.setEntity(new StringEntity(jsonString));
            //Perform the request and check the status code
            HttpResponse response = client.execute(post);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == 201) {
                Log.d(TAG, "Upload completed");
            } else {
                Log.e(TAG, "Server responded with status code: " + statusLine.getStatusCode());
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to send HTTP POST request due to: " + ex);
        }*/
    }
}
