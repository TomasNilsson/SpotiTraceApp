package com.spotitrace.spotitrace;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class CompassSearchFragment extends Fragment {
    protected final String TAG = "CompassSearchFragment";

    public CompassSearchFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_compass_search, container, false);

        return rootView;
    }

}
