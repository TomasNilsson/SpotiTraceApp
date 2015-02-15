package com.spotitrace.spotitrace;

import com.google.gson.annotations.SerializedName;

public class Song {
    public long id;
    public String name;
    public String artist;
    public String album;
    public String uri;
    @SerializedName("image_url") // Used when the field name does not match the JSON response
    public String imageUrl;
}
