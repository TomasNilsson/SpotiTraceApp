package com.spotitrace.spotitrace;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

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
        // Print all intent extras to log
        /*Bundle bundle = intent.getExtras();
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            Log.d("SpotifyReceiver", String.format("%s %s (%s)", key,
                    value.toString(), value.getClass().getName()));
        }*/
        //Toast.makeText(context, uri, Toast.LENGTH_LONG).show();
        Log.d("SpotifyReceiver", name);
        if (!uri.equals(lastSongUri)) {
            ma.removeMaster();
            lastSongUri = uri;
            // use this to start and trigger a service
            Intent i = new Intent(context, UploadService.class);
            // potentially add data to the intent
            i.putExtra(EXTRA_ARTIST, artist);
            i.putExtra(EXTRA_NAME, name);
            i.putExtra(EXTRA_URI, uri);
            context.startService(i);
        }
    }
}
