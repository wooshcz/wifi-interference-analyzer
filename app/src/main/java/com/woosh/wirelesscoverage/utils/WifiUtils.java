package com.woosh.wirelesscoverage.utils;

import android.net.wifi.ScanResult;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by woosh on 5.7.16.
 * Utilities
 */

public class WifiUtils {

    public static void addToDebugLog(String message) {
        long timeMillis = System.currentTimeMillis();
        long timeSeconds = System.currentTimeMillis() / 1000;
        if (Constants.DEBUG) {
            if (message.length() > 0) {
                Log.d(Constants.DEBUG_TAG, message);
                Constants.DEBUG_LIST.add(timeSeconds + "." + String.valueOf(timeMillis).substring(10) + " | " + message);
            }
        }
    }

    public static int ieee80211_frequency_to_channel(int freq) {
        if (freq == 2484) return 14;
        if (freq < 2484) return (freq - 2407) / 5;
        return freq / 5 - 1000;
    }

    public static int ieee80211_channel_to_frequency(int ch) {
        if (ch == 14) return 2484;
        if (ch < 14) return (ch * 5) + 2407;
        return 5 * (ch + 1000);
    }

    public static int[] chanFreqWidth(ScanResult result) {
        int[] ret = new int[4]; // fr0, fr1, bw0, bw1
        switch (result.channelWidth) {
            case ScanResult.CHANNEL_WIDTH_20MHZ:
                ret[0] = result.frequency;
                ret[1] = 0;
                ret[2] = 20;
                ret[3] = 0;
                break;
            case ScanResult.CHANNEL_WIDTH_40MHZ:
                ret[0] = result.centerFreq0;
                ret[1] = 0;
                ret[2] = 40;
                ret[3] = 0;
                break;
            case ScanResult.CHANNEL_WIDTH_80MHZ:
                ret[0] = result.centerFreq0;
                ret[1] = 0;
                ret[2] = 80;
                ret[3] = 0;
                break;
            case ScanResult.CHANNEL_WIDTH_160MHZ:
                ret[0] = result.centerFreq0;
                ret[1] = 0;
                ret[2] = 160;
                ret[3] = 0;
                break;
            case ScanResult.CHANNEL_WIDTH_320MHZ:
                ret[0] = result.centerFreq0;
                ret[1] = 0;
                ret[2] = 320;
                ret[3] = 0;
                break;
            case ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ:
                ret[0] = result.centerFreq0;
                ret[1] = result.centerFreq1;
                ret[2] = 80;
                ret[3] = 80;
                break;
        }
        return ret;
    }

    public static List<String> parseCapabilities(String cap) {
        List<String> out = new ArrayList<>();
        if (cap.contains("[") && cap.contains("]")) {
            String regex = "\\[([A-Z0-9-+/]+)]";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(cap);
            while (m.find()) {
                out.add(m.group(1));
            }
        }
        return out;
    }

}
