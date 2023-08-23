package com.woosh.wirelesscoverage.helpers;

import java.util.ArrayList;
import java.util.Comparator;

public class Channel {

    final int channelId, channelFrequency;
    int accesPointCount;
    float score, rating;
    final ArrayList<String> accessPointList = new ArrayList<>();

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

    private static Comparator<Channel> compByScore() {
        return new Comparator<Channel>() {
            @Override
            public int compare(Channel c1, Channel c2) {
                return Math.round(c1.score - c2.score);
            }
        };
    }
}
