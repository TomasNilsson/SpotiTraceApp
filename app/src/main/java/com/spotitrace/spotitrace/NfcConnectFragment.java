package com.spotitrace.spotitrace;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static android.nfc.NdefRecord.createMime;

public class NfcConnectFragment extends Fragment
        implements NfcAdapter.CreateNdefMessageCallback, NfcAdapter.OnNdefPushCompleteCallback, ListHandler {
    protected final String TAG = "NfcConnectFragment";
    private NfcAdapter mNfcAdapter;
    private MainActivity activity;
    private static final int MESSAGE_SENT = 1;
    private ListView listView;
    private static List<User> userSingleList;

    public NfcConnectFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_nfc_connect, container, false);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        activity = (MainActivity) getActivity();
        listView = (ListView)getView().findViewById(R.id.list);
        if(userSingleList != null){
            handleUsersList(userSingleList);
        }else{
            userSingleList = new ArrayList<User>();
        }

        final SwipeRefreshLayout swipeView = (SwipeRefreshLayout) getView().findViewById(R.id.swipe_container);
        swipeView.setColorScheme(android.R.color.holo_blue_dark, android.R.color.holo_blue_light, android.R.color.holo_green_light, android.R.color.holo_green_light);
        swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeView.setRefreshing(true);
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeView.setRefreshing(false);
                        if(userSingleList.isEmpty()) {
                            activity.update();
                        }else{
                            updateUser();
                        }
                    }
                }, 3000);
            }
        });

        if (activity.nfcMasterUserId != 0) {
            Toast.makeText(activity, "New user received", Toast.LENGTH_LONG).show();
            Log.d(TAG, "User received: " + activity.nfcMasterUserId);
            updateUser();
        }

        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (mNfcAdapter == null) {
            Toast.makeText(activity, "NFC is not available on this device.", Toast.LENGTH_LONG).show();
        } else {
            // Register callback to set NDEF message
            mNfcAdapter.setNdefPushMessageCallback(this, activity);
            // Register callback to listen for message-sent success
            mNfcAdapter.setOnNdefPushCompleteCallback(this, activity);
        }
    }

    /**
     * Implementation for the CreateNdefMessageCallback interface
     */
    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        // Include the userId of the master user in the NdefMessage
        NdefMessage msg = new NdefMessage(NdefRecord.createMime(
                "application/com.spotitrace.spotitrace", ByteBuffer.allocate(8).putLong(activity.userId).array())
                /**
                 * The Android Application Record (AAR) is commented out. When a device
                 * receives a push with an AAR in it, the application specified in the AAR
                 * is guaranteed to run. The AAR overrides the tag dispatch system.
                 * You can add it back in to guarantee that this
                 * activity starts when receiving a beamed message. For now, this code
                 * uses the tag dispatch system.
                 */
                //,NdefRecord.createApplicationRecord("com.example.android.beam")
        );
        return msg;
    }

    /**
     * Implementation for the OnNdefPushCompleteCallback interface
     */
    @Override
    public void onNdefPushComplete(NfcEvent arg0) {
        // A handler is needed to send messages to the activity when this
        // callback occurs, because it happens from a binder thread
        mHandler.obtainMessage(MESSAGE_SENT).sendToTarget();
    }

    /** This handler receives a message from onNdefPushComplete */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SENT:
                    Toast.makeText(activity, "Sent my user id: " + activity.userId, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    @Override
    public void handleUsersList(List<User> users){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageListAdapter adapter = new ImageListAdapter(activity, userSingleList);
                listView.setAdapter(adapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
                        activity.startSong();
                        TextView textView = (TextView)v.findViewById(R.id.user_name);
                        if (activity.mMasterUserTextView != null) {
                            activity.mMasterUserTextView.setTextColor(textView.getTextColors().getDefaultColor());
                        }
                        activity.mMasterUserTextView = textView;
                        activity.mMasterUserTextView.setTextColor(getResources().getColor(R.color.text_color));
                    }
                });
            }
        });
    }

    public void updateUser(){
        UserFetcher userFetcher = new UserFetcher();
        userFetcher.execute();
    }

    private class UserFetcher extends AsyncTask<Void, Void, String> {
        private static final String TAG = "UserFetcher";
        public final String SERVER_URL = "http://spotitrace.herokuapp.com/api/users/"+activity.nfcMasterUserId;

        @Override
        protected String doInBackground(Void... params) {
            try {
                //Create an HTTP client
                HttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet(SERVER_URL);
                request.setHeader("Authorization", "Token token=\"" + activity.getAccessToken() + "\"");

                //Perform the request and check the status code
                HttpResponse response = client.execute(request);
                StatusLine statusLine = response.getStatusLine();
                if(statusLine.getStatusCode() == 200) {
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();

                    try {
                        //Read the server response and attempt to parse it as JSON
                        Reader reader = new InputStreamReader(content);

                        GsonBuilder gsonBuilder = new GsonBuilder();
                        Gson gson = gsonBuilder.create();
                        User user = gson.fromJson(reader, User.class);
                        content.close();

                        if(user != null) {
                            Log.d(TAG, "New user: " + user.username);
                            userSingleList.clear();
                            userSingleList.add(user);
                            activity.mMasterUser = user;
                            activity.startSong();
                            handleUsersList(userSingleList);
                        }

                    } catch (Exception ex) {
                        Log.e(TAG, "Failed to parse JSON due to: " + ex);

                    }
                } else {
                    Log.e(TAG, "Server responded with status code: " + statusLine.getStatusCode());

                }
            } catch(Exception ex) {
                Log.e(TAG, "Failed to send HTTP GET request due to: " + ex);

            }
            return null;
        }

    }


}
