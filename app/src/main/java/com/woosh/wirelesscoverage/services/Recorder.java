package com.woosh.wirelesscoverage.services;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import com.woosh.wirelesscoverage.helpers.Channel;
import com.woosh.wirelesscoverage.utils.Constants;
import com.woosh.wirelesscoverage.utils.WifiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by woosh on 31.7.16.
 * Recorder class
 */

public class Recorder {

    final String band;
    private final ArrayList<Channel> list = new ArrayList<>();
    private final int maxchan;
    private final int minchan;
    private int scancount;

    public Recorder() {
        this.band = Constants.PREFS.getOrDefault(Constants.PREF_BAND, Constants.BAND_2GHZ);
        int chanstep;
        if (Constants.BAND_5GHZ.equals(this.band)) {
            this.maxchan = Integer.parseInt(Objects.requireNonNull(Constants.PREFS.get(Constants.PREF_MAX_CHANNEL_5G)));
            this.minchan = Constants.WIFI_5G_MINCHAN;
            chanstep = 2;
        } else {
            this.maxchan = Integer.parseInt(Objects.requireNonNull(Constants.PREFS.get(Constants.PREF_MAX_CHANNEL_2G)));
            this.minchan = Constants.WIFI_2G_MINCHAN;
            chanstep = 1;
        }
        for (int i = minchan; i <= maxchan; i += chanstep) {
            this.list.add(new Channel(i, WifiUtils.ieee80211_channel_to_frequency(i)));
            //Util.addToDebugLog(String.format(Locale.getDefault(), "Channel: %d, %d", i, Util.ieee80211_channel_to_frequency(i)));
        }
        this.scancount = 0;
    }

    public void addScan(List<ScanResult> scan) {
        scancount++;
        for (ScanResult res : scan) {
            boolean isIgnored = Scanner.getIgnoredNetworksSet().contains(res.BSSID);
            boolean isHome = Scanner.getHomeNetworksSet().contains(res.BSSID);
            if (isHome || isIgnored) continue;
            int[] chanfreqbw = WifiUtils.chanFreqWidth(res);
            int ch = WifiUtils.ieee80211_frequency_to_channel(chanfreqbw[0]);
            if (ch < minchan || ch > maxchan) continue;
            int listch = -1;
            for (int k = list.size() - 1; k >= 0; k--) {
                if (list.get(k).getChannelId() == ch) {
                    listch = k;
                    break;
                }
            }
            //Util.addToDebugLog(String.format(Locale.getDefault(), res.SSID + ": %d, %d", ch, listch));
            Channel chan = list.get(listch);
            String bssid = res.BSSID;
            boolean inList = false;
            for (int k = 0; k < chan.getAccessPointList().size(); k++) {
                if (bssid.equals(chan.getAccessPointList().get(k))) {
                    inList = true;
                    break;
                }
            }
            if (!inList) {
                chan.getAccessPointList().add(bssid);
                chan.setAccesPointCount(chan.getAccesPointCount() + 1);
            }
            list.set(listch, chan);
            if (chanfreqbw[1] > 0 && chanfreqbw[3] > 0) {
                updateChanScores(res.level, chanfreqbw[0], chanfreqbw[2]);
                updateChanScores(res.level, chanfreqbw[1], chanfreqbw[3]);
            } else {
                updateChanScores(res.level, chanfreqbw[0], chanfreqbw[2]);
            }
        }
    }

    private void updateChanScores(int lev, int freq0, int bw0) {
        int levdiv = 20;
        int minbw = 20; // 20 MHz minimal channel bandwidth
        for (int i = 0; i < list.size(); i++) {
            int freq = list.get(i).getChannelFrequency();
            int dist = Math.abs(freq0 - freq); // Distance in MHz
            int reach = minbw / 2 + bw0 / 2; // Maximum reach between two networks
            if (dist <= reach) {
                double overlap = 1 - 0.8 * dist / reach;
                double pen = Math.pow(overlap, 2);
                Channel ch = list.get(i);
                ch.setScore((float) (ch.getScore() + WifiManager.calculateSignalLevel(lev, levdiv) * pen));
                //Util.addToDebugLog(String.format(Locale.getDefault(), "Channel %3$d score calculation - distance: %1$d, penalty: %2$f", dist, pen/maxpen, cId));
                list.set(i, ch);
            }
        }
    }

    public String getBand() {
        return band;
    }

    public int getScanCount() {
        return scancount;
    }

    public Channel getChannel(int pos) {
        return list.get(pos);
    }

    public void updateRatings() {
        float[] stat = getChannelsStats();
        WifiUtils.addToDebugLog("Stats: tot/max/min: " + stat[0] + "/" + stat[1] + "/" + stat[2]);
        //float avg = stat[0]/list.size();
        for (int i = 0; i < list.size(); i++) {
            Channel ch = list.get(i);
            if (stat[0] > 0) {
                ch.setRating(10 * (1 - (ch.getScore() - stat[2]) / (stat[1] - stat[2])));
            } else {
                ch.setRating(10f);
            }
        }
    }

    public Channel getBestChannel() {
        float min = -1;
        Channel bestchan = null;
        for (int i = 0; i < list.size(); i++) {
            Channel ch = list.get(i);
            if (min == -1 || min > ch.getScore()) {
                min = ch.getScore();
                bestchan = ch;
            }
        }
        return bestchan;
    }

    public int getChannelCount() {
        return list.size();
    }

    private float[] getChannelsStats() {
        float tot = 0;
        float max = 0;
        float min = -1;
        for (Channel ch : list) {
            //Log.d(Constants.DEBUG_TAG, "!cScore: " + ch.cScore);
            tot += ch.getScore();
            if (min == -1 || min > ch.getScore()) min = ch.getScore();
            if (max < ch.getScore()) max = ch.getScore();
        }
        return new float[]{tot, max, min};
    }

//    boolean saveCurrentResults(Context ctx) {
//        try {
//            String filename = "test.xml";
//            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//            DocumentBuilder db = dbf.newDocumentBuilder();
//            Document doc = db.newDocument();
//            Element root = doc.createElement("RecorderData");
//            doc.appendChild(root);
//            Element details = doc.createElement("Channels");
//            root.appendChild(details);
//            for (int i = 0; i < list.size(); i++) {
//                Element chan = doc.createElement("Chan");
//                details.appendChild(chan);
//                chan.appendChild(doc.createTextNode(list.get(i).toString()));
//            }
//            Transformer transformer = TransformerFactory.newInstance().newTransformer();
//            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//            File file = ctx.getFilesDir();
//            StreamResult result = new StreamResult(new File(file + "/" + filename));
//            DOMSource source = new DOMSource(doc);
//            transformer.transform(source, result);
//            WifiUtils.addToDebugLog(file.toString() + "/" + filename);
//            return true;
//        } catch (ParserConfigurationException | TransformerException e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    void loadResultsFromFile(Context ctx, String filename) {
//        try {
//            File source = new File(ctx.getFilesDir().getAbsolutePath() + "/" + filename);
//            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//            dbf.setNamespaceAware(false);
//            dbf.setValidating(false);
//            DocumentBuilder db = dbf.newDocumentBuilder();
//            Document doc = db.parse(source);
//            NodeList nl = doc.getElementsByTagName("Chan");
//            for (int i = 0; i < nl.getLength(); i++) {
//                Node n = nl.item(i);
//                n.getTextContent();
//                WifiUtils.addToDebugLog(n.getTextContent());
//            }
//        } catch (ParserConfigurationException | SAXException | IOException e) {
//            e.printStackTrace();
//        }
//    }
}
