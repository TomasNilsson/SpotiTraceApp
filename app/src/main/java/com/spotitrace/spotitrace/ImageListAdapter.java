package com.spotitrace.spotitrace;

/**
 * Created by Johannes on 2/20/2015.
 */
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.List;

public class ImageListAdapter extends ArrayAdapter<User> {
    private LayoutInflater inflater;
    private final MainActivity activity;
    private final List<User> users;

    public ImageListAdapter(Activity context, List<User> users) {
        super(context, R.layout.list_item, users);
        activity = (MainActivity)context;
        this.users = users;
        this.inflater = context.getLayoutInflater();
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {

        View rowView = view;
        // view is null when new row is needed
        if (rowView == null) {
            rowView= inflater.inflate(R.layout.list_item, null, true);
        }

        TextView userTxtTitle = (TextView) rowView.findViewById(R.id.user_name);
        TextView songTxtTile = (TextView) rowView.findViewById(R.id.song_title);
        TextView artistTextTile = (TextView) rowView.findViewById(R.id.song_artist);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.img);
        final ImageView friendView = (ImageView) rowView.findViewById(R.id.friend);


        User user = users.get(position);
        if(user.friend) {
            friendView.setImageResource(R.drawable.ic_star_friend);
        }else{
            friendView.setImageResource(R.drawable.ic_star_no_friend);
        }

        friendView.setOnClickListener(new ImageView.OnClickListener(){

            @Override
            public void onClick(View v){
                User user = users.get(position);
                activity.handleFriend(position);
                if(!user.friend) {
                    friendView.setImageResource(R.drawable.ic_star_friend);
                }else{
                    friendView.setImageResource(R.drawable.ic_star_no_friend);
                }
            }
        });

        // Set text to the song and artist.
        userTxtTitle.setText(user.username +", "+(double)Math.round(user.distance*100)/100+" km away");
        if (activity.mMasterUser != null && user.username.equals(activity.mMasterUser.username)) {
            userTxtTitle.setTextColor(activity.getResources().getColor(R.color.text_color));
            activity.mMasterUserTextView = userTxtTitle;
        }
        artistTextTile.setText(user.song.artist);
        songTxtTile.setText(user.song.name);

        // Load a scaled down version of the image.
        new ImageDownloader(imageView).execute(user.song.imageUrl);
        return rowView;
    }

}

class ImageDownloader extends AsyncTask<String, Void, Bitmap> {
    ImageView bmImage;

    public ImageDownloader(ImageView bmImage) {
        this.bmImage = bmImage;
    }

    protected Bitmap doInBackground(String... urls) {
        String url = urls[0];
        if( url == null){
            url = "http://djazz.mine.nu/apps/eqbeats/img/album-placeholder.png"; //Use local image instead.
        }
        Bitmap mIcon = null;
        try {
            InputStream in = new java.net.URL(url).openStream();
            mIcon = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
        }
        return mIcon;
    }

    protected void onPostExecute(Bitmap result) {
        bmImage.setImageBitmap(result);
    }
}




/*
package com.spotitrace.spotitrace;

*/
/**
 * Created by Johannes on 2/20/2015.
 *//*

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ImageListAdapter extends ArrayAdapter<Song> {
    private LayoutInflater inflater;
    private final Activity context;
    private final List<Song> songs;

    public ImageListAdapter(Activity context, List<Song> songs) {
        super(context, R.layout.list_item, songs);
        this.context = context;
        this.songs = songs;
        this.inflater = context.getLayoutInflater();
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        View rowView = view;
        // view is null when new row is needed
        if (rowView == null) {
            rowView= inflater.inflate(R.layout.list_item, null, true);
        }

        TextView userTxtTitle = (TextView) rowView.findViewById(R.id.userName);
        TextView songTxtTile = (TextView) rowView.findViewById(R.id.songTitle);
        TextView artistTextTile = (TextView) rowView.findViewById(R.id.songArtist);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.img);


        Song song = songs.get(position);
        // Set text to the song and artist.
        userTxtTitle.setText("UserName");
        artistTextTile.setText(song.artist);
        songTxtTile.setText(song.name);

        // Load a scaled down version of the image.
        new ImageDownloader(imageView).execute(song.imageUrl);
        return rowView;
    }
}

class ImageDownloader extends AsyncTask<String, Void, Bitmap> {
    ImageView bmImage;

    public ImageDownloader(ImageView bmImage) {
        this.bmImage = bmImage;
    }

    protected Bitmap doInBackground(String... urls) {
        String url = urls[0];
        if( url == null){
            url = "http://djazz.mine.nu/apps/eqbeats/img/album-placeholder.png"; //Use local image instead.
        }
        Bitmap mIcon = null;
        try {
            InputStream in = new java.net.URL(url).openStream();
            mIcon = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
        }
        return mIcon;
    }

    protected void onPostExecute(Bitmap result) {
        bmImage.setImageBitmap(result);
    }
}
*/
