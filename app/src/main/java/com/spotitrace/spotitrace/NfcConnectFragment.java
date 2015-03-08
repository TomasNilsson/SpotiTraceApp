package com.spotitrace.spotitrace;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class NfcConnectFragment extends Fragment {
    protected final String TAG = "NfcConnectFragment";
    private MainActivity activity;

    public NfcConnectFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_nfc_connect, container, false);

        return rootView;
    }

}
