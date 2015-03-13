package com.spotitrace.spotitrace;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WhoFollowsMeFragment extends Fragment implements ListHandler {
    protected final String TAG = "WhoFollowsMeFragment";
    private MainActivity ma;
    private List<User> followers;
    private ListView listView;
    private TextView textView;

    public WhoFollowsMeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_who_follows_me, container, false);

        return rootView;
    }

    @Override
    public void onStart(){
        super.onStart();
        ma = (MainActivity) getActivity();
        followers = new ArrayList<User>();
        listView = (ListView)getView().findViewById(R.id.list);
        textView = (TextView)getView().findViewById(R.id.info_box);

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
                        UserFetcher userFetcher = new UserFetcher();
                        userFetcher.execute();
                    }
                }, 3000);
            }
        });
    }

    @Override
    public void onResume(){
        super.onResume();
        UserFetcher userFetcher = new UserFetcher();
        userFetcher.execute();
    }

    private class UserFetcher extends AsyncTask<Void, Void, String> {
        private static final String TAG = "UserFetcher";
        public final String SERVER_URL  = "http://spotitrace.herokuapp.com/api/users/followers";

        @Override
        protected String doInBackground(Void... params) {
            try {
                //Create an HTTP client
                HttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet(SERVER_URL);
                request.setHeader("Authorization", "Token token=\"" + ma.getAccessToken() + "\"");

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

                        followers = Arrays.asList(gson.fromJson(reader, User[].class));
                        content.close();
                        handleUsersList(followers);


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
    public void handleUsersList(List<User> users){
        ma.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(followers.isEmpty()){
                    textView.setText("No one is following you right now.");
                    ma.setFollowerCounter(0);
                }else {
                    textView.setText("");
                    SmallListAdapter adapter = new SmallListAdapter(ma, followers);
                    listView.setAdapter(adapter);
                    ma.setFollowerCounter(followers.size());
                }
            }
        });
    }
}
