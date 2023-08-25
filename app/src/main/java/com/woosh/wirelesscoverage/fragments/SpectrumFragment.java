package com.woosh.wirelesscoverage.fragments;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.woosh.wirelesscoverage.MainActivity;
import com.woosh.wirelesscoverage.R;
import com.woosh.wirelesscoverage.SpectrumView;
import com.woosh.wirelesscoverage.utils.Constants;
import com.woosh.wirelesscoverage.utils.WifiUtils;

/**
 * Created by woosh on 17.7.16.
 * Spectrum Fragment
 */

public class SpectrumFragment extends Fragment {

    private SpectrumView spectrumView;
    private SwipeRefreshLayout mSwipeLayout;
    private CoordinatorLayout coordinatorLayout;

    public void onBandChange() {
        if (spectrumView != null) spectrumView.invalidate();
    }

    public void onScanReady() {
        if (!MainActivity.MANUAL_SCAN && !MainActivity.RELOAD_RUNNING) return;
        if (mSwipeLayout != null) mSwipeLayout.setRefreshing(false);
        if (spectrumView != null) spectrumView.invalidate();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        WifiUtils.addToDebugLog("SpectrumFragment:onCreateView()");
        return inflater.inflate(R.layout.fragment_spectrum, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        WifiUtils.addToDebugLog("SpectrumFragment:onViewCreated()");
        spectrumView = view.findViewById(R.id.custom_view);
        spectrumView.setNetworks(MainActivity.sw.getNetworks());
        coordinatorLayout = view.findViewById(R.id.coord);
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            ToggleButton bFilter = view.findViewById(R.id.butt_autofilter);
            if (Constants.PREFS.get(Constants.PREF_FILTER_SPEC).equals("true")) {
                bFilter.setChecked(true);
            }
            bFilter.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor edit = sp.edit();
                edit.putBoolean(Constants.PREF_FILTER_SPEC, isChecked);
                edit.apply();
                spectrumView.invalidate();
            });

            ToggleButton bRec = view.findViewById(R.id.butt_rec);
            bRec.setEnabled(false);
            Button bClr = view.findViewById(R.id.butt_clr);
            bClr.setEnabled(false);

            ToggleButton bAutoreload = view.findViewById(R.id.butt_autoreload);
            if (MainActivity.RELOAD_RUNNING) {
                view.setKeepScreenOn(true);
                bAutoreload.setChecked(true);
            }
            bAutoreload.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                if (!isChecked) {
                    MainActivity.setAutoreload(false);
                    getView().setKeepScreenOn(false);
                } else if (!MainActivity.RELOAD_RUNNING) {
                    MainActivity.setAutoreload(true);
                    getView().setKeepScreenOn(true);
                    Snackbar.make(coordinatorLayout, R.string.snack_autoreload_started, Snackbar.LENGTH_LONG).show();
                }
            });

            mSwipeLayout = view.findViewById(R.id.swipe_refresh_layout);
            mSwipeLayout.setOnRefreshListener(() -> {
                if (MainActivity.receiverRegistered) {
                    WifiUtils.addToDebugLog("SpectrumFragment:onRefresh()");
                    MainActivity.runOneTimeScan();
                } else {
                    mSwipeLayout.setRefreshing(false);
                }
            });
        }
    }
}
