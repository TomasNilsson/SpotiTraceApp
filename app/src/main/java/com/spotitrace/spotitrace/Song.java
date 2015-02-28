package com.spotitrace.spotitrace;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Song {
    public long id;
    @Expose
    public String name;
    @Expose
    public String artist;
    @Expose
    public String uri;
    @Expose
    @SerializedName("image_url") // Used when the field name does not match the JSON response
    public String imageUrl;

    public Song (String name, String artist, String album, String uri, String imageUrl) {
        this.name = name;
        this.artist = artist;
        this.uri = uri;
        this.imageUrl = imageUrl;
    }
}
