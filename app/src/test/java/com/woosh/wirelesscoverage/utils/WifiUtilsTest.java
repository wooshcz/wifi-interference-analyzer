package com.woosh.wirelesscoverage.utils;

import static org.junit.Assert.assertEquals;

import android.net.wifi.ScanResult;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class WifiUtilsTest {

    @Parameterized.Parameter
    public String fInput;
    @Parameterized.Parameter(1)
    public List<String> fExpected;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS][WPS]", Arrays.asList("WPA2-PSK-CCMP", "RSN-PSK-CCMP", "ESS", "WPS")},
                {"[WPA-PSK-TKIP+CCMP][WPA2-PSK-TKIP+CCMP][RSN-PSK-TKIP+CCMP][ESS]", Arrays.asList("WPA-PSK-TKIP+CCMP", "WPA2-PSK-TKIP+CCMP", "RSN-PSK-TKIP+CCMP", "ESS")},
                {"[WPA2-PSK+FT/PSK-CCMP][RSN-PSK+FT/PSK-CCMP][WPA-PSK-CCMP][ESS]", Arrays.asList("WPA2-PSK+FT/PSK-CCMP", "RSN-PSK+FT/PSK-CCMP", "WPA-PSK-CCMP", "ESS")}
        });
    }

    @Test
    public void parseCapabilities() {
        assertEquals(fExpected, WifiUtils.parseCapabilities(fInput));
    }

    @Test
    public void ieee80211_frequency_to_channel() {
        int channel = WifiUtils.ieee80211_frequency_to_channel(5210);
        assertEquals(42, channel);
    }

    @Test
    public void ieee80211_channel_to_frequency() {
        int frequency = WifiUtils.ieee80211_channel_to_frequency(11);
        assertEquals(2462, frequency);
    }

    @Test
    public void chanFreqWidth() {
        var scanResult = new ScanResult();
        scanResult.BSSID = "de:ad:ff:fe:be:ef";
        scanResult.centerFreq0 = 2437;
        scanResult.channelWidth = ScanResult.CHANNEL_WIDTH_40MHZ;
        int[] out = WifiUtils.chanFreqWidth(scanResult);
        assertEquals(4, out.length);
        assertEquals(scanResult.centerFreq0, out[0]);
        assertEquals(0, out[1]);
        assertEquals(40, out[2]);
        assertEquals(0, out[3]);
    }
}