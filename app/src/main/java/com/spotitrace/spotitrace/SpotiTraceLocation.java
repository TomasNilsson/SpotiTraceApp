package com.spotitrace.spotitrace;

import com.google.gson.annotations.Expose;


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
