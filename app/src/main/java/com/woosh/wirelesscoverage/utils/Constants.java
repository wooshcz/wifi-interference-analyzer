package com.woosh.wirelesscoverage.utils;

import com.woosh.wirelesscoverage.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by woosh on 4.7.16.
 * Constants used by multiple classes in this package
 */

public class Constants {

    public static final Boolean DEBUG = true;
    public static final String DEBUG_TAG = "woosh";
    public static final ArrayList<String> DEBUG_LIST = new ArrayList<>();
    public static final String SKU_DONATE = "com.woosh.wirelesscoverage.donated";

    public static final int WIFI_2G_MINCHAN = 1;
    public static final int WIFI_5G_MINCHAN = 36;
    public static final int FILTER_SPEC_COUNT = 10;
    public static final String BAND_5GHZ = "5";
    public static final String BAND_2GHZ = "24";

    public static final String PREF_THEME = "theme";
    public static final String PREF_BAND = "band";
    public static final String PREF_FREQCHAN = "freqchan";
    public static final String PREF_DELAY = "autorefresh_delay";
    public static final String PREF_FILTER_SPEC = "filter_spectrum";
    public static final String PREF_REC_SAMPLES = "rec_samples";
    public static final String PREF_REC_DELAY = "rec_delay";
    public static final String PREF_MAX_CHANNEL_2G = "max_channel_2G";
    public static final String PREF_MAX_CHANNEL_5G = "max_channel_5G";

    public static final String[] PREF_STRING_KEYS = {PREF_THEME, PREF_BAND, PREF_FREQCHAN, PREF_DELAY, PREF_FILTER_SPEC, PREF_REC_SAMPLES, PREF_REC_DELAY, PREF_MAX_CHANNEL_2G, PREF_MAX_CHANNEL_5G};
    public static final int[] PREF_STRING_DEFS = {R.string.pref_theme_def, R.string.pref_band_def, R.string.pref_freqchan_def, R.string.pref_autorefresh_delay_def, R.string.pref_filter_spectrum_def, R.string.pref_rec_samples_def, R.string.pref_rec_delay_def, R.string.pref_max_channel_2G_def, R.string.pref_max_channel_5G_def};

    public static final String PREF_LIST_HOME = "list_home";
    public static final String PREF_LIST_IGNORE = "list_ignore";

    public static final String[] PREF_SET_KEYS = {PREF_LIST_HOME, PREF_LIST_IGNORE};

    public static final Map<String, String> PREFS = new HashMap<>();

}
