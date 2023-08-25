package com.woosh.wirelesscoverage;

import static com.woosh.wirelesscoverage.utils.Constants.BAND_5GHZ;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.woosh.wirelesscoverage.helpers.BSSID;
import com.woosh.wirelesscoverage.helpers.WifiNetwork;
import com.woosh.wirelesscoverage.services.Scanner;
import com.woosh.wirelesscoverage.utils.Constants;
import com.woosh.wirelesscoverage.utils.WifiUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by woosh on 17.7.16.
 * SpectrumView Class
 */

public class SpectrumView extends View {

    private final Paint paintText = new Paint();
    private final Paint paintLabels = new Paint();
    private final Paint paintBars = new Paint();
    private final Paint paintGrid = new Paint();
    private final int colorText;
    private final int colorBars;
    private final int colorGrid;
    private int fontsize;
    private int labelsize;
    private int axisgap;
    private float viewHeight;
    private float viewWidth;
    private float graphHeight;
    private float graphWidth;
    private int chanstep;
    private int maxchan;
    private int minchan;
    private float fr2pix;
    private float lev2pix;
    private int maxlev;
    private int minlev;
    private int levs;
    private int frstart;
    private String xaxis;

    private List<WifiNetwork> networks;
    private List<Map<String, Object>> networkCanvasObjects;
    private int[] levels;
    private int SIZE = 0;

    public SpectrumView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SpectrumView, 0, 0);
        try {
            colorText = a.getColor(R.styleable.SpectrumView_textColor, 0);
            colorBars = a.getColor(R.styleable.SpectrumView_barsColor, 0);
            colorGrid = a.getColor(R.styleable.SpectrumView_gridColor, 0);
        } finally {
            a.recycle();
        }
    }

    private void initializeView() {
        WifiUtils.addToDebugLog("SpectrumView:initializeView()");
        fontsize = getResources().getDimensionPixelSize(R.dimen.spectrum_textsize);
        labelsize = getResources().getDimensionPixelSize(R.dimen.spectrum_labelsize);
        axisgap = getResources().getDimensionPixelSize(R.dimen.spectrum_axisgap);
        xaxis = Constants.PREFS.get(Constants.PREF_FREQCHAN);
        String band = Constants.PREFS.get(Constants.PREF_BAND);

        viewHeight = getHeight();
        viewWidth = getWidth();
        graphHeight = viewHeight - axisgap;
        graphWidth = viewWidth - axisgap;

        if (BAND_5GHZ.equals(band)) {
            maxchan = Integer.parseInt(Constants.PREFS.get(Constants.PREF_MAX_CHANNEL_5G));
            minchan = Constants.WIFI_5G_MINCHAN;
            chanstep = 2;
        } else {
            maxchan = Integer.parseInt(Constants.PREFS.get(Constants.PREF_MAX_CHANNEL_2G));
            minchan = Constants.WIFI_2G_MINCHAN;
            chanstep = 1;
        }
        int chanbw = 20;
        frstart = WifiUtils.ieee80211_channel_to_frequency(minchan) - chanbw / 2;
        int frstop = WifiUtils.ieee80211_channel_to_frequency(maxchan) + chanbw / 2;
        float frwidth = frstop - frstart;
        fr2pix = (graphWidth / frwidth);

        maxlev = getMaxValue() + 10;
        minlev = getMinValue() - 5;
        levs = maxlev - minlev;
        lev2pix = (graphHeight / levs);
    }

    private int getMaxValue() {
        if (networks != null) {
            this.SIZE = 0;
            for (WifiNetwork net : networks) {
                SIZE += net.getBSSIDList().size();
            }
            levels = new int[SIZE];
            int max = -100;
            int iter = 0;
            for (WifiNetwork network : networks) {
                for (BSSID bssid : network.getBSSIDList()) {
                    int cur = bssid.getLevel();
                    levels[iter++] = cur;
                    if (cur > max) max = cur;
                }
            }
            return max;
        } else {
            return 0;
        }
    }

    private int getMinValue() {
        if (networks != null) {
            int min = -95;
            for (WifiNetwork network : networks) {
                for (BSSID bssid : network.getBSSIDList()) {
                    int cur = bssid.getLevel();
                    if (cur < min) min = cur;
                }
            }
            return min;
        } else {
            return 0;
        }
    }

    public void setNetworks(List<WifiNetwork> networks) {
        this.networks = networks;
    }

    private void buildCanvasObjects() {
        WifiUtils.addToDebugLog("SpectrumView:buildCanvasObjects()");
        // Drawing the bars
        if (networks != null && networks.size() > 0) {
            Arrays.sort(levels);
            int treshold = minlev;
            if (SIZE >= Constants.FILTER_SPEC_COUNT) {
                treshold = levels[SIZE - Constants.FILTER_SPEC_COUNT];
            }
            WifiUtils.addToDebugLog("Treshold: " + treshold);
            List<Map<String, Object>> listMap = new ArrayList<>();
            for (WifiNetwork network : networks) {
                for (BSSID bssid : network.getBSSIDList()) {
                    Map<String, Object> map = new HashMap<>();
                    int level = bssid.getLevel();
                    if (level < treshold && Constants.PREFS.get(Constants.PREF_FILTER_SPEC).equals("true"))
                        break;
                    int freq = bssid.getFreq0();
                    int ch = bssid.getChan0();
                    int bw = bssid.getBw0();
                    if (ch > maxchan || ch < minchan) continue;
                    float baseWidth = (bw) * fr2pix;
                    float topWidth = (bw - 5) * fr2pix;
                    String ssid = network.getSSID();
                    Path path = new Path();
                    path.moveTo(axisgap + (freq - frstart) * fr2pix - baseWidth / 2, graphHeight);
                    path.lineTo(axisgap + (freq - frstart) * fr2pix - topWidth / 2, graphHeight - lev2pix * (level - minlev));
                    path.lineTo(axisgap + (freq - frstart) * fr2pix + topWidth / 2, graphHeight - lev2pix * (level - minlev));
                    path.lineTo(axisgap + (freq - frstart) * fr2pix + baseWidth / 2, graphHeight);
                    map.put("bar", path);
                    map.put("freq", freq);
                    map.put("bssid", bssid.getBSSID());
                    map.put("text", ssid);
                    map.put("level", level);
                    listMap.add(map);
                }
            }
            this.networkCanvasObjects = listMap;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        initializeView();
        buildCanvasObjects();

        paintText.setTextAlign(Paint.Align.RIGHT);
        paintText.setColor(colorText);
        paintText.setTextSize(fontsize);
        paintText.setAntiAlias(true);

        paintLabels.setTextAlign(Paint.Align.CENTER);
        paintLabels.setColor(colorText);
        paintLabels.setTextSize(labelsize);
        paintLabels.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paintLabels.setAntiAlias(true);

        paintGrid.setColor(colorGrid);
        paintGrid.setAntiAlias(true);

        paintBars.setColor(colorBars);
        paintBars.setAlpha(0x50);
        paintBars.setAntiAlias(true);
        paintBars.setStrokeWidth(0.6f * fr2pix);
        paintBars.setStyle(Paint.Style.STROKE);

        // Drawing the y-axis
        canvas.drawLine(axisgap, 0, axisgap, graphHeight, paintText);
        int points = 10;
        for (int i = 0; i < points; i++) {
            int val = maxlev - i * levs / (points - 1);
            if (i > 0) {
                canvas.drawText(
                        String.format(Locale.getDefault(), "%d", Math.round(val)),
                        (float) (axisgap * 3.0 / 4.0),
                        (maxlev - val) * lev2pix + paintText.getTextSize() / 3,
                        paintText
                );
            }
            canvas.drawLine((float) (axisgap - axisgap / 10.0), (maxlev - val) * lev2pix, axisgap, (maxlev - val) * lev2pix, paintText);
            canvas.drawLine(axisgap, (maxlev - val) * lev2pix, viewWidth, (maxlev - val) * lev2pix, paintGrid);
        }

        // Drawing the x-axis
        canvas.drawLine(axisgap, viewHeight, graphWidth, viewHeight, paintText);
        paintText.setTextAlign(Paint.Align.CENTER);
        int shift = axisgap * 6 / 10;
        for (int i = minchan; i <= maxchan; i += chanstep) {
            int val = WifiUtils.ieee80211_channel_to_frequency(i);
            switch (xaxis) {
                case "Chan":
                    canvas.drawText(
                            String.format(Locale.getDefault(), "%d", WifiUtils.ieee80211_frequency_to_channel(val)),
                            axisgap + (val - frstart) * fr2pix,
                            viewHeight - shift,
                            paintText
                    );
                    break;
                case "MHz":
                    if (shift != axisgap / 6) shift = axisgap / 6;
                    else shift = axisgap * 6 / 10;
                    canvas.drawText(
                            String.format(Locale.getDefault(), "%d", val),
                            axisgap + (val - frstart) * fr2pix,
                            viewHeight - shift,
                            paintText
                    );
                    break;
                case "GHz":
                    if (shift != axisgap / 6) shift = axisgap / 6;
                    else shift = axisgap * 6 / 10;
                    canvas.drawText(
                            String.format(Locale.getDefault(), "%1.3f", (float) val / 1000),
                            axisgap + (val - frstart) * fr2pix,
                            viewHeight - shift,
                            paintText
                    );
                    break;
            }
            canvas.drawLine(
                    axisgap + (val - frstart) * fr2pix,
                    (float) (graphHeight + axisgap / 10.0),
                    axisgap + (val - frstart) * fr2pix,
                    graphHeight,
                    paintText
            );
        }

        // Drawing the bars
        int displayed = 0;
        if (null != networkCanvasObjects && networkCanvasObjects.size() > 0) {
            for (Map<String, ?> canvasMap : networkCanvasObjects) {
                boolean isIgnored = Scanner.getIgnoredNetworksSet().contains((String) canvasMap.get("bssid"));
                boolean isHome = Scanner.getHomeNetworksSet().contains((String) canvasMap.get("bssid"));
                if (isIgnored) {
                    paintBars.setColor(ContextCompat.getColor(getContext(), R.color.colorIgnore));
                    paintBars.setAlpha(0x20);
                } else if (isHome) {
                    paintBars.setColor(ContextCompat.getColor(getContext(), R.color.colorHome));
                    paintBars.setAlpha(0x50);
                } else {
                    paintBars.setColor(colorBars);
                    paintBars.setAlpha(0x50);
                }
                canvas.drawPath(
                        (Path) canvasMap.get("bar"),
                        paintBars);
                canvas.drawText(
                        (String) canvasMap.get("text"),
                        axisgap + ((int) canvasMap.get("freq") - frstart) * fr2pix,
                        graphHeight - lev2pix * ((int) canvasMap.get("level") - minlev) - 20,
                        paintLabels
                );
                displayed++;
            }
            WifiUtils.addToDebugLog("Displayed: " + displayed);
        }
    }
}