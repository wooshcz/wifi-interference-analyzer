package com.woosh.wirelesscoverage.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatRatingBar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.woosh.wirelesscoverage.MainActivity;
import com.woosh.wirelesscoverage.R;
import com.woosh.wirelesscoverage.helpers.Channel;
import com.woosh.wirelesscoverage.services.Recorder;
import com.woosh.wirelesscoverage.utils.Constants;
import com.woosh.wirelesscoverage.utils.WifiUtils;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Locale;

/**
 * Created by woosh on 17.7.16.
 * Recording Fragment
 */

public class RecordingFragment extends Fragment {

    private CustomListAdapter listAdapter;
    private Recorder recorder;

    private ProgressBar pbar;
    private Button bClr;
    private ToggleButton bRec;
    private TextView bestChan;
    private CoordinatorLayout coordinatorLayout;

    private int recMaxItems;
    private int recCurItem;

    public void onBandChange(String newband) {
        if (!newband.equals(recorder.getBand())) {
            recorder = new Recorder();
            listAdapter.notifyDataSetChanged();
        }
    }

    public void onScanReady() {
        if (!MainActivity.REC_RUNNING) return;
        recCurItem = recorder.getScanCount();
        WifiUtils.addToDebugLog("recItem/recMax: " + recCurItem + "/" + recMaxItems);
        if (recCurItem < recMaxItems) {
            recCurItem++;
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            recorder.addScan(MainActivity.wm.getScanResults());
            recorder.updateRatings();
            listAdapter.notifyDataSetChanged();
            bestChan.setText(getResources().getQuantityString(R.plurals.frag_rec_best, recCurItem, recorder.getBestChannel().getChannelId(), recCurItem));
            WifiUtils.addToDebugLog("recording item added. Recorded items: " + recCurItem);
        } else {
            MainActivity.setRecording(false);
            if (getView() != null) getView().setKeepScreenOn(false);
            pbar.setVisibility(View.INVISIBLE);
            bRec.setChecked(false);
            bClr.setEnabled(true);
            Snackbar.make(coordinatorLayout, R.string.snack_rec_stopped, Snackbar.LENGTH_LONG).show();
            WifiUtils.addToDebugLog("recording stopped");
            //rec.loadResultsFromFile(getContext(), "test.xml");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        WifiUtils.addToDebugLog("RecordingFragment:onCreateView()");
        return inflater.inflate(R.layout.fragment_recording, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        WifiUtils.addToDebugLog("RecordingFragment:onActivityCreated()");
        if (recorder == null || !recorder.getBand().equals(Constants.PREFS.get(Constants.PREF_BAND))) {
            recorder = new Recorder();
        }
        recMaxItems = Integer.parseInt(Constants.PREFS.get(Constants.PREF_REC_SAMPLES));
        recCurItem = recorder.getScanCount();
        final View v = getView();
        if (v != null) {
            listAdapter = new CustomListAdapter(getActivity());
            ListView scoreList = v.findViewById(R.id.listView);
            scoreList.setDividerHeight(0);
            scoreList.setAdapter(listAdapter);
            coordinatorLayout = v.findViewById(R.id.coord);

            bestChan = v.findViewById(R.id.bestchannel);
            if (recCurItem > 0)
                bestChan.setText(getResources().getQuantityString(R.plurals.frag_rec_best, recCurItem, recorder.getBestChannel().getChannelId(), recCurItem));
            pbar = v.findViewById(R.id.progressBar);
            pbar.setVisibility(View.INVISIBLE);
            bRec = v.findViewById(R.id.butt_rec);
            bRec.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                if (!isChecked && MainActivity.REC_RUNNING) {
                    MainActivity.setRecording(false);
                    getView().setKeepScreenOn(false);
                    pbar.setVisibility(View.INVISIBLE);
                    Snackbar.make(coordinatorLayout, R.string.snack_rec_stopped, Snackbar.LENGTH_LONG).show();
                } else if (!MainActivity.REC_RUNNING) {
                    MainActivity.setRecording(true);
                    getView().setKeepScreenOn(true);
                    pbar.setVisibility(View.VISIBLE);
                    Snackbar.make(coordinatorLayout, R.string.snack_rec_started, Snackbar.LENGTH_LONG).show();
                }
            });

            ToggleButton bFilter = v.findViewById(R.id.butt_autofilter);
            bFilter.setEnabled(false);
            ToggleButton bAutoreload = v.findViewById(R.id.butt_autoreload);
            bAutoreload.setEnabled(false);

            bClr = v.findViewById(R.id.butt_clr);
            bClr.setOnClickListener(view -> {
                recorder = new Recorder();
                recCurItem = recorder.getScanCount();
                listAdapter.notifyDataSetChanged();
                bestChan.setText(null);
                Snackbar.make(coordinatorLayout, R.string.snack_clear_results, Snackbar.LENGTH_LONG).show();
            });

            if (MainActivity.REC_RUNNING) {
                pbar.setVisibility(View.VISIBLE);
                bRec.setChecked(true);
                bClr.setEnabled(false);
                v.setKeepScreenOn(true);
            }
        }
    }

    private class CustomListAdapter extends BaseAdapter {

        private final LayoutInflater layoutInflater;

        CustomListAdapter(Context ctx) {
            layoutInflater = LayoutInflater.from(ctx);
        }

        @Override
        public int getCount() {
            return recorder.getChannelCount();
        }

        @Override
        public Object getItem(int position) {
            return recorder.getChannel(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder;

            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.list_rec_row, parent, false);
                holder = new ViewHolder();
                holder.chan = convertView.findViewById(R.id.ch_channel);
                holder.rbar = convertView.findViewById(R.id.ch_rbar);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (holder != null) {
                DecimalFormat df = new DecimalFormat("#.#");
                df.setRoundingMode(RoundingMode.HALF_UP);
                Channel ch = recorder.getChannel(position);
                holder.chan.setText(String.format(Locale.getDefault(), getString(R.string.frag_rec_channel), ch.getChannelId()));
                holder.rbar.setRating(ch.getRating());
            }
            return convertView;
        }

        class ViewHolder {
            TextView chan;
            AppCompatRatingBar rbar;
        }
    }
}
