package com.spotitrace.spotitrace;

import com.google.gson.annotations.Expose;

public class User {
    public long id;
    @Expose
    public String username;
    @Expose
    public String token;

    public double distance;

    public double bearing;

    public Song song;

    public User (String username, String token) {
        this.username = username;
        this.token = token;
    }
}
