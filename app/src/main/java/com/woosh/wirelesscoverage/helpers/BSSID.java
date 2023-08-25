package com.woosh.wirelesscoverage.helpers;

import com.woosh.wirelesscoverage.utils.WifiUtils;

import java.util.List;

public class BSSID {

    final String BSSID;
    double seenAt;
    List<String> capabilitiesList;
    int freq0, freq1;
    int chan0;
    int bw0, bw1;
    int band;
    int level;

    public BSSID(String bssid) {
        this.BSSID = bssid;
        this.seenAt = System.currentTimeMillis();
    }

    public String getBSSID() {
        return BSSID;
    }

    public double getSeenAt() {
        return seenAt;
    }

    public void setSeenAt(double seenAt) {
        this.seenAt = seenAt;
    }

    public List<String> getCapabilitiesList() {
        return capabilitiesList;
    }

    public void setCapabilitiesList(List<String> capabilitiesList) {
        this.capabilitiesList = capabilitiesList;
    }

    public int getFreq0() {
        return freq0;
    }

    public void setFreq0(int freq0) {
        this.freq0 = freq0;
        this.chan0 = WifiUtils.ieee80211_frequency_to_channel(freq0);
        if (this.freq0 > 5000) this.band = 5;
        else if (this.freq0 < 5000) this.band = 2;
    }

    public int getFreq1() {
        return freq1;
    }

    public void setFreq1(int freq1) {
        this.freq1 = freq1;
    }

    public int getChan0() {
        return chan0;
    }

    public void setChan0(int chan0) {
        this.chan0 = chan0;
    }

    public int getBw0() {
        return bw0;
    }

    public void setBw0(int bw0) {
        this.bw0 = bw0;
    }

    public int getBw1() {
        return bw1;
    }

    public void setBw1(int bw1) {
        this.bw1 = bw1;
    }

    public int getBand() {
        return band;
    }

    public void setBand(int band) {
        this.band = band;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BSSID && ((BSSID) obj).BSSID.equals(this.BSSID);
    }
}
