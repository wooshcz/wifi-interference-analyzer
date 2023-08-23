package com.woosh.wirelesscoverage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.woosh.wirelesscoverage.helpers.BSSID;
import com.woosh.wirelesscoverage.helpers.WifiNetwork;
import com.woosh.wirelesscoverage.services.Scanner;

import java.util.Locale;

/**
 * Created by woosh on 14.10.16.
 * Expandable list adapter for list view
 */

public class ExpandableListAdapter extends BaseExpandableListAdapter {

    private final LayoutInflater layoutInflater;
    private final Context context;

    public ExpandableListAdapter(Context ctx) {
        this.context = ctx;
        this.layoutInflater = LayoutInflater.from(ctx);
    }

    @Override
    public Object getChild(int groupPosition, int childPosititon) {
        return MainActivity.sw.getNetworkChild(groupPosition, childPosititon);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        ViewHolderChild holder;

        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.list_scan_exp_row, parent, false);
            holder = new ViewHolderChild();
            holder.flag = convertView.findViewById(R.id.flag);
            holder.bssid = convertView.findViewById(R.id.ap_bssid);
            holder.freq = convertView.findViewById(R.id.ap_frequency);
            holder.level = convertView.findViewById(R.id.ap_level);
            holder.capab = convertView.findViewById(R.id.ap_capab);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolderChild) convertView.getTag();
        }

        if (holder != null) {
            BSSID bssid = MainActivity.sw.getNetworkChild(groupPosition, childPosition);
            //int absLev = WifiManager.calculateSignalLevel(network.level, 20);
            holder.bssid.setText(String.format(Locale.getDefault(), context.getString(R.string.frag_scan_bssid), bssid.getBSSID()));
            if (bssid.getFreq1() > 0 && bssid.getBw1() > 0) {
                holder.freq.setText(
                        String.format(Locale.getDefault(),
                                context.getString(R.string.frag_scan_chanfr_long),
                                bssid.getChan0(),
                                bssid.getFreq0(),
                                bssid.getBw0(),
                                bssid.getFreq1(),
                                bssid.getBw1()
                        )
                );
            } else {
                holder.freq.setText(
                        String.format(Locale.getDefault(),
                                context.getString(R.string.frag_scan_chanfr),
                                bssid.getChan0(),
                                bssid.getFreq0(),
                                bssid.getBw0()
                        )
                );
            }
            holder.level.setText(String.format(Locale.getDefault(), context.getString(R.string.frag_scan_strength), bssid.getLevel()));
            StringBuilder captext = new StringBuilder();
            for (int i = 0; i < bssid.getCapabilitiesList().size(); i++) {
                String item = bssid.getCapabilitiesList().get(i);
                if (item.length() > 0) {
                    if (i > 0) captext.append("\n");
                    captext.append(item);
                }
            }
            holder.capab.setText(captext.toString());
            boolean isIgnored = Scanner.getIgnoredNetworksSet().contains(bssid.getBSSID());
            boolean isHome = Scanner.getHomeNetworksSet().contains(bssid.getBSSID());
            boolean isOld = (System.currentTimeMillis() / 1000.0 - bssid.getSeenAt()) > 1;
            if (isIgnored)
                holder.flag.setBackgroundColor(ContextCompat.getColor(context, R.color.colorIgnore));
            else if (isHome)
                holder.flag.setBackgroundColor(ContextCompat.getColor(context, R.color.colorHome));
            else if (isOld)
                holder.flag.setBackgroundColor(ContextCompat.getColor(context, R.color.colorOld));
            else holder.flag.setBackgroundColor(0);
        }
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return MainActivity.sw.getNetworkChildSize(groupPosition);
    }

    @Override
    public Object getGroup(int groupPosition) {
        return MainActivity.sw.getNetworkGroup(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return MainActivity.sw.getNetworkGroupSize();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

        ViewHolderGroup holder;

        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.list_scan_main_row, parent, false);
            holder = new ViewHolderGroup();
            //holder.flag = (LinearLayout) convertView.findViewById(R.id.flag);
            holder.ssid = convertView.findViewById(R.id.ap_ssid);
            holder.count = convertView.findViewById(R.id.ap_count);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolderGroup) convertView.getTag();
        }

        if (holder != null) {
            WifiNetwork network = MainActivity.sw.getNetworkGroup(groupPosition);
            String name = network.getSSID();
            holder.ssid.setText(String.format(Locale.getDefault(), context.getString(R.string.frag_scan_ssid), name));
            ExpandableListView listview = (ExpandableListView) parent;
            if (network.getBSSIDList().size() > 1) {
                holder.count.setVisibility(View.VISIBLE);
                holder.count.setText(String.format(Locale.getDefault(), context.getString(R.string.frag_scan_count), network.getBSSIDList().size()));
            } else {
                holder.count.setVisibility(View.GONE);
                listview.expandGroup(groupPosition);
            }
        }
        return convertView;

    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    private static class ViewHolderGroup {
        TextView ssid, count;
        //LinearLayout flag;
    }

    private static class ViewHolderChild {
        TextView freq, level, capab, bssid;
        LinearLayout flag;
    }
}