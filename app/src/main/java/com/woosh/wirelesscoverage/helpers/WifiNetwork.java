package com.woosh.wirelesscoverage.helpers;

import java.util.ArrayList;
import java.util.List;

public class WifiNetwork {

    final String SSID;
    List<BSSID> BSSIDList;

    public WifiNetwork (String SSID) {
        this.SSID = SSID;
        this.BSSIDList = new ArrayList<>();
    }

    public int getMaxLevel() {
        int maxlev = -200;
        for (BSSID item: BSSIDList) {
            if (item.level > maxlev) maxlev = item.level;
        }
        return maxlev;
    }

    public String getSSID() {
        if (SSID.length() > 0) {
            return SSID;
        } else {
            return "<empty>";
        }
    }

    public List<BSSID> getBSSIDList() {
        return BSSIDList;
    }

    public void setBSSIDList(List<BSSID> BSSIDList) {
        this.BSSIDList = BSSIDList;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof WifiNetwork && ((WifiNetwork) obj).SSID.equals(this.SSID);
    }

}
