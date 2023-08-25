package com.woosh.wirelesscoverage.helpers;

import java.util.ArrayList;

public class Channel {

    final int channelId, channelFrequency;
    final ArrayList<String> accessPointList = new ArrayList<>();
    int accesPointCount;
    float score, rating;

    public Channel(int id, int freq) {
        this.channelId = id;
        this.channelFrequency = freq;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getChannelId() {
        return channelId;
    }

    public int getChannelFrequency() {
        return channelFrequency;
    }

    public int getAccesPointCount() {
        return accesPointCount;
    }

    public void setAccesPointCount(int accesPointCount) {
        this.accesPointCount = accesPointCount;
    }

    public ArrayList<String> getAccessPointList() {
        return accessPointList;
    }

}
