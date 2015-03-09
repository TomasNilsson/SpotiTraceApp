package com.spotitrace.spotitrace;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class LocationSearchFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {
    protected final String TAG = "LocationSearchFragment";
    private SupportMapFragment mMapFragment;
    private static GoogleMap mMap;
    private MainActivity activity;
    private GoogleMapOptions options;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private static LatLng location;

    public LocationSearchFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_location_search, container, false);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        activity = (MainActivity) getActivity();
        Button button = (Button) getView().findViewById(R.id.map_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                options = new GoogleMapOptions();
                Location myLocation = activity.mLastLocation;
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(myLocation.getLatitude(),myLocation.getLongitude()))
                        .zoom(13)
                        .build();
                options.mapType(GoogleMap.MAP_TYPE_NORMAL)
                        .camera(cameraPosition)
                        .compassEnabled(false)
                        .rotateGesturesEnabled(false)
                        .zoomControlsEnabled(true)
                        .zoomGesturesEnabled(true)
                        .tiltGesturesEnabled(false);
                mMapFragment = SupportMapFragment.newInstance(options);
                mMapFragment.getMapAsync(LocationSearchFragment.this);
                FragmentManager fragmentManager = activity.getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.frame_container, mMapFragment, "SpotiTraceFragment").commit();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap map) {
        map.setMyLocationEnabled(true);
        map.setOnMapLongClickListener(this);
        mMap = map;
    }

    @Override
    public void onMapLongClick(LatLng point) {
        location = point;
        mMap.addMarker(new MarkerOptions()
                .position(point));
        // Create an instance of the dialog fragment and show it
        DialogFragment dialog = new ConfirmLocationFragment();
        dialog.show(activity.getSupportFragmentManager(), "ConfirmLocationFragment");
    }

    public static class ConfirmLocationFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.map_dialog_message)
                    .setTitle(R.string.map_dialog_title)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                            fragmentManager.beginTransaction()
                                    .replace(R.id.frame_container, new LocationSearchFragment(), "SpotiTraceFragment").commit();
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            mMap.clear();
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

}
