package com.spotitrace.spotitrace;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class LocationSearchFragment extends Fragment {
    protected final String TAG = "LocationSearchFragment";

    public LocationSearchFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_location_search, container, false);

        return rootView;
    }

}
