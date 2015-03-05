package com.spotitrace.spotitrace;

import com.google.gson.annotations.Expose;

/**
 * Created by Johannes on 3/3/2015.
 */
public class SpotiTraceLocation {
    @Expose
    public double latitude;

    @Expose
    public double longitude;

    public SpotiTraceLocation(double longitude, double latitude){
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
