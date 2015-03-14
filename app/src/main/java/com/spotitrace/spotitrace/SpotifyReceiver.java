package com.spotitrace.spotitrace;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SpotifyReceiver extends BroadcastReceiver{
    public static final String EXTRA_ARTIST = "com.spotitrace.spotitrace.ARTIST";
    public static final String EXTRA_NAME = "com.spotitrace.spotitrace.NAME";
    public static final String EXTRA_URI = "com.spotitrace.spotitrace.URI";
    // Used to prevent duplicates (the song is detected twice).
    private String lastSongUri = "";
    private MainActivity ma;
    public SpotifyReceiver(MainActivity ma){
        this.ma=ma;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String artist = intent.getStringExtra("artist");
        String name = intent.getStringExtra("track");
        String uri = intent.getStringExtra("id");

        if (!uri.equals(lastSongUri) && uri != null) {
            Log.d("SpotifyReceiver", name);
            ma.removeMaster();
            lastSongUri = uri;
            // Upload song to SpotiTrace server
            Intent i = new Intent(context, UploadService.class);
            // Add data to the intent
            i.putExtra(EXTRA_ARTIST, artist);
            i.putExtra(EXTRA_NAME, name);
            i.putExtra(EXTRA_URI, uri);
            context.startService(i);
        }
    }
}
