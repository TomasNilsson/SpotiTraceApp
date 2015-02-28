package com.spotitrace.spotitrace;

import android.os.AsyncTask;
import android.util.Log;

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

/**
 * Created by Johannes on 2/27/2015.
 */
public class SpotifySongFetcher extends AsyncTask<Void, Void, String> {
    private static final String TAG = "SpotifySongFetcher";
    private String SERVER_URL;
    private UploadService mUploadService;

    public SpotifySongFetcher(String id, UploadService US){
        String songUri[] = id.split(":");
        SERVER_URL = "https://api.spotify.com/v1/tracks/" +songUri[2];
        mUploadService = US;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            //Create an HTTP client
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(SERVER_URL);

            //Perform the request and check the status code
            HttpResponse response = client.execute(request);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();

                try {
                    //Read the server response and attempt to parse it as JSON
                    Reader reader = new InputStreamReader(content);

                    GsonBuilder gsonBuilder = new GsonBuilder();
                    Gson gson = gsonBuilder.create();
                    SpotifySong song = gson.fromJson(reader, SpotifySong.class);
                    mUploadService.setUrl(song.album.images.get(song.album.images.size()-1).url);

                    content.close();

                } catch (Exception ex) {
                    Log.e(TAG, "Failed to parse JSON due to: " + ex);
                }
            } else {
                Log.e(TAG, "Server responded with status code: " + statusLine.getStatusCode());
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to send HTTP GET request due to: " + ex);
        }
        return null;
    }
}
