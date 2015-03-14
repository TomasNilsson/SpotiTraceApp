package com.spotitrace.spotitrace;

import java.util.ArrayList;

// Used for saving url to album cover
public class SpotifySong {
    Album album;
}

class Album{
    ArrayList<Image> images;
}

class Image{
    String url;
}