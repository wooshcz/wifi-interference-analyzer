package com.woosh.wirelesscoverage.services;

import android.net.wifi.ScanResult;

import com.woosh.wirelesscoverage.helpers.BSSID;
import com.woosh.wirelesscoverage.helpers.WifiNetwork;
import com.woosh.wirelesscoverage.utils.Constants;
import com.woosh.wirelesscoverage.utils.WifiUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Created by woosh on 2.8.16.
 * ScanWrapper
 */

public class Scanner {

    private static Set<String> homeNetworksSet = new HashSet<>();
    private static Set<String> ignoredNetworksSet = new HashSet<>();
    private final List<WifiNetwork> wifiNetworks = new ArrayList<>();
    private final List<WifiNetwork> filteredNetworks = new ArrayList<>();

    public static Set<String> getHomeNetworksSet() {
        return homeNetworksSet;
    }

    public static void setHomeNetworksSet(Set<String> homeNetworksSet) {
        Scanner.homeNetworksSet = homeNetworksSet;
    }

    public static Set<String> getIgnoredNetworksSet() {
        return ignoredNetworksSet;
    }

    public static void setIgnoredNetworksSet(Set<String> ignoredNetworksSet) {
        Scanner.ignoredNetworksSet = ignoredNetworksSet;
    }

    private Comparator<WifiNetwork> compByMaxLevel() {
        return (n1, n2) -> Math.round(n2.getMaxLevel() - n1.getMaxLevel());
    }

    private Comparator<BSSID> compByLevel() {
        return (b1, b2) -> Math.round(b2.getLevel() - b1.getLevel());
    }

    public void addScanResults(List<ScanResult> scan) {
        for (ScanResult item : scan) {
            //Util.addToDebugLog(item.toString());
            WifiNetwork tmpnet = new WifiNetwork(item.SSID);
            WifiNetwork net;
            if (wifiNetworks.contains(tmpnet)) {
                //Util.addToDebugLog(item.SSID + " is in networks");
                net = wifiNetworks.get(wifiNetworks.indexOf(tmpnet));
            } else {
                //Util.addToDebugLog(item.SSID + " is new network");
                net = tmpnet;
            }
            BSSID tmpbssid = new BSSID(item.BSSID);
            BSSID bssid;
            if (net.getBSSIDList().contains(tmpbssid)) {
                bssid = net.getBSSIDList().get(net.getBSSIDList().indexOf(tmpbssid));
                //Util.addToDebugLog(item.BSSID + " is in the network");
            } else {
                bssid = tmpbssid;
                //Util.addToDebugLog(item.BSSID + " is new bssid");
            }
            int[] chanfreqbw = WifiUtils.chanFreqWidth(item);
            bssid.setFreq0(chanfreqbw[0]);
            bssid.setBw0(chanfreqbw[2]);
            if (chanfreqbw[1] != 0 && chanfreqbw[3] != 0) {
                bssid.setFreq1(chanfreqbw[1]);
                bssid.setBw1(chanfreqbw[3]);
            }
            bssid.setSeenAt(System.currentTimeMillis() / 1000.0);
            bssid.setCapabilitiesList(WifiUtils.parseCapabilities(item.capabilities));
            bssid.setLevel(item.level);
            if (net.getBSSIDList().contains(tmpbssid)) {
                net.getBSSIDList().set(net.getBSSIDList().indexOf(tmpbssid), bssid);
                //Util.addToDebugLog(item.BSSID + " is edited");
            } else {
                net.getBSSIDList().add(bssid);
                //Util.addToDebugLog(item.BSSID + " is added");
            }
            if (wifiNetworks.contains(tmpnet)) {
                wifiNetworks.set(wifiNetworks.indexOf(tmpnet), net);
                //Util.addToDebugLog(item.SSID + " is edited");
            } else {
                wifiNetworks.add(net);
                //Util.addToDebugLog(item.SSID + " is added");
            }
        }
        //Util.addToDebugLog("wifinetworks size: " + wifiNetworks.size());
        purgeOldNetworks();
        filterNetworks();
        //Util.addToDebugLog("wifinetworks size after purge: " + wifiNetworks.size());
    }

    public List<WifiNetwork> getNetworks() {
        return filteredNetworks;
    }

    public BSSID getNetworkChild(int group, int child) {
        return filteredNetworks.get(group).getBSSIDList().get(child);
    }

    public WifiNetwork getNetworkGroup(int group) {
        return filteredNetworks.get(group);
    }

    public void filterNetworks() {
        int band = Integer.parseInt(Objects.requireNonNull(Constants.PREFS.get(Constants.PREF_BAND)));
        WifiUtils.addToDebugLog("filterNetworks is called: " + band);
        filteredNetworks.clear();
        if (band > 0) {
            int maxchan, minchan;
            if (band == 5) {
                maxchan = Integer.parseInt(Objects.requireNonNull(Constants.PREFS.get(Constants.PREF_MAX_CHANNEL_5G)));
                minchan = Constants.WIFI_5G_MINCHAN;
            } else {
                maxchan = Integer.parseInt(Objects.requireNonNull(Constants.PREFS.get(Constants.PREF_MAX_CHANNEL_2G)));
                minchan = Constants.WIFI_2G_MINCHAN;
            }
            for (WifiNetwork net : wifiNetworks) {
                List<BSSID> outb = new ArrayList<>();
                for (BSSID bssid : net.getBSSIDList()) {
                    if (bssid.getChan0() >= minchan && bssid.getChan0() <= maxchan) {
                        outb.add(bssid);
                    }
                }
                if (outb.size() > 0) {
                    outb.sort(compByLevel());
                    net.setBSSIDList(outb);
                    filteredNetworks.add(net);
                }
            }
        }
        filteredNetworks.sort(compByMaxLevel());
    }

    public int getNetworkGroupSize() {
        return filteredNetworks.size();
    }

    public int getNetworkChildSize(int group) {
        return filteredNetworks.get(group).getBSSIDList().size();
    }

    private void purgeOldNetworks() {
        double curtime = System.currentTimeMillis() / 1000.0;
        for (Iterator<WifiNetwork> iterator = wifiNetworks.iterator(); iterator.hasNext(); ) {
            WifiNetwork network = iterator.next();
            network.getBSSIDList().removeIf(bssid -> bssid.getSeenAt() + 10 < curtime);
            if (network.getBSSIDList().size() == 0) {
                iterator.remove();
            }
        }
    }
}
