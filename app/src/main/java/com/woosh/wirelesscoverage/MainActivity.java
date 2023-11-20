package com.woosh.wirelesscoverage;

import static android.view.View.TEXT_ALIGNMENT_CENTER;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.android.material.navigation.NavigationView;
import com.woosh.wirelesscoverage.fragments.RecordingFragment;
import com.woosh.wirelesscoverage.fragments.ScanFragment;
import com.woosh.wirelesscoverage.fragments.SpectrumFragment;
import com.woosh.wirelesscoverage.services.Scanner;
import com.woosh.wirelesscoverage.utils.Constants;
import com.woosh.wirelesscoverage.utils.WifiUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    public static Scanner sw;
    public static WifiManager wm;
    public static boolean RELOAD_RUNNING = false;
    public static boolean REC_RUNNING = false;
    public static boolean MANUAL_SCAN;
    public static boolean receiverRegistered = false;
    public static boolean showDonateButton = true;
    private static long DELAY;
    private static long DELAY_REC;
    private static Timer timer;
    private final AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = billingResult -> WifiUtils.addToDebugLog("billingFlow onAcknowledgePurchaseResponse: " + billingResult);
    private final SharedPreferences.OnSharedPreferenceChangeListener spChanged = (sharedPreferences, key) -> {
        Map<String, ?> all = sharedPreferences.getAll();
        String val = Objects.requireNonNull(all.get(key)).toString();
        if (val.length() > 0) {
            Constants.PREFS.put(key, val);
        }
        WifiUtils.addToDebugLog("pref changed: " + key);
        WifiUtils.addToDebugLog("pref changed: " + Constants.PREFS.get(key));
    };
    private final BroadcastReceiver scanReady = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            WifiUtils.addToDebugLog("MainActivity:ScanResults received");
            //Util.addToDebugLog(wm.getScanResults().toString());
            if (MANUAL_SCAN || RELOAD_RUNNING || REC_RUNNING) {
                if (ActivityCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                sw.addScanResults(wm.getScanResults());
                FragmentManager manager = getSupportFragmentManager();
                Fragment currentFragment = manager.findFragmentById(R.id.root_frame);
                if (currentFragment instanceof ScanFragment) {
                    ((ScanFragment) currentFragment).onScanReady();
                } else if (currentFragment instanceof SpectrumFragment) {
                    ((SpectrumFragment) currentFragment).onScanReady();
                } else if (currentFragment instanceof RecordingFragment) {
                    ((RecordingFragment) currentFragment).onScanReady();
                }
                // Immediate autorefresh
                if (DELAY < 0 || DELAY_REC < 0) wm.startScan();
                if (MANUAL_SCAN) MANUAL_SCAN = false;
            }
        }
    };
    private SharedPreferences sp;
    private BillingClient billingClient;
    private final PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                WifiUtils.addToDebugLog("billingFlow onPurchasesUpdated: BillingClient.BillingResponseCode.OK");
                for (Purchase purchase : purchases) {
                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        Toast.makeText(getApplicationContext(), R.string.feedback_donation_success, Toast.LENGTH_LONG).show();
                        if (!purchase.isAcknowledged()) {
                            AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();
                            billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
                        }
                        View donateButton = findViewById(R.id.action_donate);
                        donateButton.setVisibility(View.INVISIBLE);
                    }
                }
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                Toast.makeText(getApplicationContext(), R.string.feedback_donation_cancelled, Toast.LENGTH_LONG).show();
            } else {
                WifiUtils.addToDebugLog("billingFlow onPurchasesUpdated: BillingResponseCode is not OK or list of Purchases is null");
            }
        }
    };
    private ProductDetails productDetails;
    private NavigationView navigationView;

    public static void runOneTimeScan() {
        if (null != wm && wm.isWifiEnabled()) {
            MANUAL_SCAN = true;
            wm.startScan();
        }
    }

    public static void setRecording(boolean set) {
        if (null == wm || !wm.isWifiEnabled()) return;
        DELAY_REC = Integer.parseInt(Objects.requireNonNull(Constants.PREFS.get(Constants.PREF_REC_DELAY))) * 1000L;
        if (set) {
            REC_RUNNING = true;
            wm.startScan();
            if (timer != null) cancelTimer();
            if (DELAY_REC >= 1000) rescheduleTask(DELAY_REC);
        } else {
            REC_RUNNING = false;
            cancelTimer();
        }
    }

    private static void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    public static void setAutoreload(boolean set) {
        if (null == wm || !wm.isWifiEnabled()) return;
        DELAY = Integer.parseInt(Objects.requireNonNull(Constants.PREFS.get(Constants.PREF_DELAY))) * 1000L;
        if (set) {
            RELOAD_RUNNING = true;
            wm.startScan();
            if (timer != null) cancelTimer();
            if (DELAY >= 1000) rescheduleTask(DELAY);
        } else {
            RELOAD_RUNNING = false;
            cancelTimer();
        }
    }

    private static void rescheduleTask(long delay) {
        if (timer == null) {
            timer = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    WifiUtils.addToDebugLog("TimerTask tick");
                    wm.startScan();
                }
            };
            timer.schedule(task, 100, delay);
        }
    }

    private void parsePreferences(SharedPreferences sp) {
        Map<String, ?> prefs = sp.getAll();
        for (int i = 0; i < Constants.PREF_STRING_KEYS.length; i++) {
            String key = Constants.PREF_STRING_KEYS[i];
            String val;
            if (prefs.get(key) != null) {
                val = Objects.requireNonNull(prefs.get(key)).toString();
            } else {
                val = getString(Constants.PREF_STRING_DEFS[i]);
            }
            Constants.PREFS.put(key, val);
            WifiUtils.addToDebugLog("pref " + key + ": " + val);
        }
        for (int i = 0; i < Constants.PREF_SET_KEYS.length; i++) {
            String key = Constants.PREF_SET_KEYS[i];
            Set<String> val = sp.getStringSet(key, new HashSet<>());
            if (key.equals(Constants.PREF_LIST_HOME)) {
                Scanner.setHomeNetworksSet(val);
            } else if (key.equals(Constants.PREF_LIST_IGNORE)) {
                Scanner.setIgnoredNetworksSet(val);
            }
            WifiUtils.addToDebugLog("pref " + key + ": " + val);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WifiUtils.addToDebugLog("MainActivity:onCreate()");
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        parsePreferences(sp);
        if (!Objects.equals(Constants.PREFS.get(Constants.PREF_THEME), getString(R.string.pref_theme_def)))
            setTheme(R.style.DarkTheme);
        else setTheme(R.style.LightTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (!Objects.equals(Constants.PREFS.get(Constants.PREF_THEME), getString(R.string.pref_theme_def))) {
            toolbar.setPopupTheme(R.style.DarkTheme_PopupOverlay);
        }
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Spinner sBandSwitch = findViewById(R.id.spin_band);
        ActionBar actbar = getSupportActionBar();
        if (actbar != null) {
            ArrayAdapter<CharSequence> spinneradapter = ArrayAdapter.createFromResource(actbar.getThemedContext(), R.array.band_entries, android.R.layout.simple_spinner_item);
            spinneradapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sBandSwitch.setAdapter(spinneradapter);
            if (Objects.equals(Constants.PREFS.get(Constants.PREF_BAND), Constants.BAND_5GHZ))
                sBandSwitch.setSelection(1);
            sBandSwitch.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    WifiUtils.addToDebugLog("Spinner: " + i + "/" + adapterView.getItemAtPosition(i));
                    String newband;
                    if (i == 0) newband = Constants.BAND_2GHZ;
                    else newband = Constants.BAND_5GHZ;
                    if (!newband.equals(Constants.PREFS.get(Constants.PREF_BAND))) {
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor edit = sp.edit();
                        edit.putString(Constants.PREF_BAND, newband);
                        edit.apply();
                        MainActivity.sw.filterNetworks();
                        FragmentManager manager = getSupportFragmentManager();
                        Fragment currentFragment = manager.findFragmentById(R.id.root_frame);
                        if (currentFragment instanceof ScanFragment) {
                            ((ScanFragment) currentFragment).onBandChange();
                        } else if (currentFragment instanceof SpectrumFragment) {
                            ((SpectrumFragment) currentFragment).onBandChange();
                        } else if (currentFragment instanceof RecordingFragment) {
                            ((RecordingFragment) currentFragment).onBandChange(newband);
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
        }

        if (savedInstanceState == null) {
            ScanFragment fragment = new ScanFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.root_frame, fragment, "scan").commit();
            navigationView.setCheckedItem(R.id.nav_scan);

            wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                if (!wm.isWifiEnabled()) {
                    Toast.makeText(getApplicationContext(), R.string.toast_enabling_wifi, Toast.LENGTH_LONG).show();
                    wm.setWifiEnabled(true);
                }
                MANUAL_SCAN = true;
                wm.startScan();
            }
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_WIFI_STATE}, 0x1000);
        } else {
            registerReceiver(scanReady, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            receiverRegistered = true;
        }

        if (sw == null) sw = new Scanner();
        sp.registerOnSharedPreferenceChangeListener(spChanged);

        billingClient = BillingClient.newBuilder(this).setListener(purchasesUpdatedListener).enablePendingPurchases().build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {

                WifiUtils.addToDebugLog("billingResult response code: " + billingResult.getResponseCode() + " | " + billingResult.getDebugMessage());
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here
                    billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(), (billingResult1, list) -> {
                        WifiUtils.addToDebugLog("billingResult purchase list fetched: " + list);
                        if (list.size() > 0) {
                            Purchase purchase = list.get(0);
                            WifiUtils.addToDebugLog("billingResult productId from the purchase list: " + purchase.getProducts().get(0));
                            if (Constants.SKU_DONATE.equals(purchase.getProducts().get(0))) {
                                // let's hide the donate button since the user already purchased the SKU_DONATE
                                showDonateButton = false;
                            }
                        }
                    });
                    List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
                    productList.add(QueryProductDetailsParams.Product.newBuilder().setProductId(Constants.SKU_DONATE).setProductType(BillingClient.ProductType.INAPP).build());
                    QueryProductDetailsParams queryProductDetailsParams = QueryProductDetailsParams.newBuilder().setProductList(productList).build();
                    billingClient.queryProductDetailsAsync(queryProductDetailsParams, (billingResult12, list) -> {
                        WifiUtils.addToDebugLog("billingResult queryProductDetailsAsync ResponseCode: " + billingResult12.getResponseCode());
                        WifiUtils.addToDebugLog("billingResult queryProductDetailsAsync DebugMessage: " + billingResult12.getDebugMessage());
                        WifiUtils.addToDebugLog("billingResult queryProductDetailsAsync skuDetailsList: " + list);
                        for (ProductDetails details : list) {
                            if (details.getProductId().equals(Constants.SKU_DONATE))
                                productDetails = details;
                        }
                        if (productDetails == null) {
                            WifiUtils.addToDebugLog("billingResult setting showDonateButton=false");
                            showDonateButton = false;
                        }
                    });
                } else {
                    showDonateButton = false;
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                billingClient.startConnection(this);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int reqCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(reqCode, permissions, grantResults);
        int res = -1;
        for (int i = 0; i < permissions.length; i++) {
            if (permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION))
                res = grantResults[i];
        }
        if (res == PackageManager.PERMISSION_GRANTED) {
            registerReceiver(scanReady, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            receiverRegistered = true;
            if (null != wm) {
                wm.startScan();
            }
        } else {
            receiverRegistered = false;
        }
    }

    @Override
    public void onDestroy() {
        WifiUtils.addToDebugLog("MainActivity:onDestroy()");
        sp.unregisterOnSharedPreferenceChangeListener(spChanged);
        if (receiverRegistered) {
            unregisterReceiver(scanReady);
        }
        if (billingClient.isReady()) {
            WifiUtils.addToDebugLog("BillingClient can only be used once -- closing connection");
            billingClient.endConnection();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
            FragmentManager manager = getSupportFragmentManager();
            Fragment currentFragment = manager.findFragmentById(R.id.root_frame);
            WifiUtils.addToDebugLog(String.format(Locale.getDefault(), "backstackEntryCount: %d", manager.getBackStackEntryCount()));
            if (currentFragment instanceof ScanFragment) {
                navigationView.setCheckedItem(R.id.nav_scan);
            }
            if (currentFragment instanceof SpectrumFragment) {
                navigationView.setCheckedItem(R.id.nav_spectrum);
            }
            if (currentFragment instanceof RecordingFragment) {
                navigationView.setCheckedItem(R.id.nav_recording);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem donateMenuItem = menu.findItem(R.id.action_donate);
        WifiUtils.addToDebugLog("showDonateButton: " + showDonateButton);
        if (!showDonateButton) donateMenuItem.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_donate) {
            if (productDetails != null) {
                ArrayList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
                productDetailsParamsList.add(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails).build());
                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder().setProductDetailsParamsList(productDetailsParamsList).build();
                int responseCode = billingClient.launchBillingFlow(this, billingFlowParams).getResponseCode();
                if (responseCode == BillingClient.BillingResponseCode.OK) {
                    WifiUtils.addToDebugLog("billingFlow launchBillingFlow: BillingClient.BillingResponseCode.OK");
                } else {
                    WifiUtils.addToDebugLog(String.format(Locale.getDefault(), "billingFlow launchBillingFlow: %d", responseCode));
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Fragment fr = null;
        String tag = null;
        if (id == R.id.nav_scan) {
            fr = new ScanFragment();
            tag = "scan";
        } else if (id == R.id.nav_spectrum) {
            fr = new SpectrumFragment();
            tag = "spectrum";
        } else if (id == R.id.nav_recording) {
            fr = new RecordingFragment();
            tag = "recording";
        } else if (id == R.id.nav_about) {
            String versionName = BuildConfig.VERSION_NAME;
            long versionCode = BuildConfig.VERSION_CODE;
            TextView textView = new TextView(getApplicationContext());
            textView.setAutoLinkMask(Linkify.WEB_URLS);
            textView.setText(R.string.app_privacy_policy);
            textView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.app_name);
            builder.setView(textView);
            builder.setMessage(String.format(Locale.getDefault(), getString(R.string.app_about), versionCode, versionName));
            builder.setPositiveButton(R.string.action_close, (dialog, which) -> dialog.cancel());
            builder.show();
        }
        if (fr != null) {
            FragmentManager fm = getSupportFragmentManager();
            Fragment currentFragment = fm.findFragmentById(R.id.root_frame);
            if (null != currentFragment && null != currentFragment.getTag()) {
                if (!currentFragment.getTag().equals(tag)) {
                    WifiUtils.addToDebugLog(String.format(Locale.getDefault(), "%d", fm.getBackStackEntryCount()));
                    FragmentTransaction fmtrans = fm.beginTransaction();
                    fmtrans.replace(R.id.root_frame, fr, tag);
                    if (fm.getBackStackEntryCount() > 0) {
                        if (!Objects.equals(fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1).getName(), tag))
                            fmtrans.addToBackStack(tag);
                    } else {
                        fmtrans.addToBackStack(tag);
                    }
                    fmtrans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                    fmtrans.commit();
                }
            }
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
