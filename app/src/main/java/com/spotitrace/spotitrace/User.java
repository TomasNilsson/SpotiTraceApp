package com.spotitrace.spotitrace;

import com.google.gson.annotations.Expose;

public class User {
    public long id;
    @Expose
    public String username;
    @Expose
    public String token;

    public User (String username, String token) {
        this.username = username;
        this.token = token;
    }
}
