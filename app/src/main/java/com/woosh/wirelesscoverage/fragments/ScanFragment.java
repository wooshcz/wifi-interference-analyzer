package com.woosh.wirelesscoverage.fragments;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.woosh.wirelesscoverage.ExpandableListAdapter;
import com.woosh.wirelesscoverage.MainActivity;
import com.woosh.wirelesscoverage.R;
import com.woosh.wirelesscoverage.helpers.BSSID;
import com.woosh.wirelesscoverage.helpers.WifiNetwork;
import com.woosh.wirelesscoverage.services.Scanner;
import com.woosh.wirelesscoverage.utils.Constants;
import com.woosh.wirelesscoverage.utils.WifiUtils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Created by woosh on 16.7.16.
 * ScanFragment
 */

public class ScanFragment extends Fragment {

    private ExpandableListAdapter listAdapter;
    private ExpandableListView listView;
    private SwipeRefreshLayout mSwipeLayout;
    private CoordinatorLayout coordinatorLayout;

    public void onBandChange() {
        if (listAdapter != null) listAdapter.notifyDataSetChanged();
        expandAllGroups();
    }

    public void onScanReady() {
        if (!MainActivity.MANUAL_SCAN && !MainActivity.RELOAD_RUNNING) return;
        if (mSwipeLayout != null) mSwipeLayout.setRefreshing(false);
        expandAllGroups();
    }

    private void expandAllGroups() {
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
            for (int i = 0; i < listAdapter.getGroupCount(); i++) {
                listView.expandGroup(i);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        WifiUtils.addToDebugLog("ScanFragment:onCreateView()");
        return inflater.inflate(R.layout.fragment_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        WifiUtils.addToDebugLog("ScanFragment:onViewCreated()");
        final Activity act = requireActivity();
        coordinatorLayout = v.findViewById(R.id.coord);
        listAdapter = new ExpandableListAdapter(act);
        listView = v.findViewById(R.id.expListView);
        TextView emptyElement = v.findViewById(R.id.emptyView);
        listView.setAdapter(listAdapter);
        listView.setEmptyView(emptyElement);
        expandAllGroups();
        listView.setOnGroupClickListener((parent, vg, groupPosition, id) -> {
            WifiUtils.addToDebugLog("groupPos: " + groupPosition);
            if (listView.isGroupExpanded(groupPosition)) {
                listView.collapseGroup(groupPosition);
            } else {
                listView.expandGroup(groupPosition);
            }
            return true;
        });
        listView.setOnChildClickListener((parent, v1, groupPosition, childPosition, id) -> {
            WifiUtils.addToDebugLog("groupPos: " + groupPosition + ", childPos: " + childPosition);
            final WifiNetwork network = (WifiNetwork) listAdapter.getGroup(groupPosition);
            final BSSID bssid = (BSSID) listAdapter.getChild(groupPosition, childPosition);
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setItems(R.array.dialog_options, (dialog, which) -> {
                String bssidString = bssid.getBSSID();
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(act);
                SharedPreferences.Editor edit = sp.edit();
                if (which == 0) {
                    if (Scanner.getHomeNetworksSet().contains(bssidString)) {
                        Scanner.getHomeNetworksSet().remove(bssidString);
                        Toast.makeText(act, String.format(Locale.getDefault(), getString(R.string.toast_home_removed), bssidString), Toast.LENGTH_SHORT).show();
                    } else {
                        Scanner.getHomeNetworksSet().add(bssidString);
                        Toast.makeText(act, String.format(Locale.getDefault(), getString(R.string.toast_home_added), bssidString), Toast.LENGTH_SHORT).show();
                    }
                    Set<String> set = new HashSet<>(Scanner.getHomeNetworksSet());
                    edit.putStringSet(Constants.PREF_LIST_HOME, set);
                }
                if (which == 1) {
                    if (Scanner.getIgnoredNetworksSet().contains(bssidString)) {
                        Scanner.getIgnoredNetworksSet().remove(bssidString);
                        Toast.makeText(act, String.format(Locale.getDefault(), getString(R.string.toast_ignore_removed), bssidString), Toast.LENGTH_SHORT).show();
                    } else {
                        Scanner.getIgnoredNetworksSet().add(bssidString);
                        Toast.makeText(act, String.format(Locale.getDefault(), getString(R.string.toast_ignore_added), bssidString), Toast.LENGTH_SHORT).show();
                    }
                    Set<String> set = new HashSet<>(Scanner.getIgnoredNetworksSet());
                    edit.putStringSet(Constants.PREF_LIST_IGNORE, set);
                }
                edit.apply();
                listAdapter.notifyDataSetChanged();
            });
            builder.setTitle(String.format(Locale.getDefault(), getString(R.string.dialog_options), network.getSSID()));
            final AlertDialog dialog = builder.create();
            dialog.show();
            return false;
        });

        ToggleButton bFilter = v.findViewById(R.id.butt_autofilter);
        bFilter.setEnabled(false);
        ToggleButton bRec = v.findViewById(R.id.butt_rec);
        bRec.setEnabled(false);
        Button bClr = v.findViewById(R.id.butt_clr);
        bClr.setEnabled(false);

        ToggleButton bAutoreload = v.findViewById(R.id.butt_autoreload);
        if (MainActivity.RELOAD_RUNNING) {
            v.setKeepScreenOn(true);
            bAutoreload.setChecked(true);
        }
        bAutoreload.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (!isChecked) {
                MainActivity.setAutoreload(false);
                v.setKeepScreenOn(false);
            } else if (!MainActivity.RELOAD_RUNNING) {
                MainActivity.setAutoreload(true);
                v.setKeepScreenOn(true);
                Snackbar.make(coordinatorLayout, R.string.snack_autoreload_started, Snackbar.LENGTH_LONG).show();
            }
        });

        mSwipeLayout = v.findViewById(R.id.swipe_refresh_layout);
        mSwipeLayout.setOnRefreshListener(() -> {
            if (MainActivity.receiverRegistered) {
                WifiUtils.addToDebugLog("ScanFragment:onRefresh()");
                MainActivity.runOneTimeScan();
            } else {
                mSwipeLayout.setRefreshing(false);
            }
        });
    }
}
