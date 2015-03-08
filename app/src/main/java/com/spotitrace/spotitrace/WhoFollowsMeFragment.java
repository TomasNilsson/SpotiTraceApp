package com.spotitrace.spotitrace;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class WhoFollowsMeFragment extends Fragment {
    protected final String TAG = "WhoFollowsMeFragment";

    public WhoFollowsMeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_who_follows_me, container, false);

        return rootView;
    }

}
