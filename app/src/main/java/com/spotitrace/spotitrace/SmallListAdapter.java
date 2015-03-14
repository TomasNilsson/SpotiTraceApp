package com.spotitrace.spotitrace;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;


public class SmallListAdapter extends ArrayAdapter<User> {
    private LayoutInflater inflater;
    private final MainActivity activity;
    private final List<User> users;

    public SmallListAdapter(Activity context, List<User> users) {
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
            rowView= inflater.inflate(R.layout.small_list_item, null, true);
        }

        TextView userTxtTitle = (TextView) rowView.findViewById(R.id.user_name);
        TextView distanceTextTile = (TextView) rowView.findViewById(R.id.distance);
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
                activity.handleFriend(user.id, user.friend);
                if(!user.friend) {
                    friendView.setImageResource(R.drawable.ic_star_friend);
                }else{
                    friendView.setImageResource(R.drawable.ic_star_no_friend);
                }
                user.friend=!user.friend;
            }
        });

        // Set text to the song and artist.
        userTxtTitle.setText(user.username);
        distanceTextTile.setText((double)Math.round(user.distance*100)/100+" km away");
        return rowView;
    }
}
