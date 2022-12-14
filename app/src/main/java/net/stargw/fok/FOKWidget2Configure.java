package net.stargw.fok;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static net.stargw.fok.Global.getContext;

public class FOKWidget2Configure extends Activity implements ActivityMainListener {

    private AppInfoAdapterApps adapter;
    private ArrayList<AppInfo> appInfoSource;
    private ListView listView;
    private Context myContext;

    private int appWidgetId;
    private static long currentProgress = 0;

    private Dialog appLoad = null;

    private BroadcastListener mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myContext = this;

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }


        setContentView(R.layout.fok_widget2_configure);

        ImageView btn = (ImageView) findViewById(R.id.activity_main_menu_icon);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showOptionsMenu(v);
            }
        });

        // When app starts show user apps
        Global.settingsEnableExpert = false;

        EditText myFilter = (EditText) findViewById(R.id.activity_main_filter_text);
        myFilter.setVisibility(View.GONE);

        ImageView mySearch = (ImageView) findViewById(R.id.activity_main_filter_icon);

        mySearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                //v.getId() will give you the image id

                EditText myFilter = (EditText) findViewById(R.id.activity_main_filter_text);

                if (myFilter.getVisibility() == View.GONE) {

                    myFilter.setVisibility(View.VISIBLE);
                    myFilter.setFocusableInTouchMode(true);
                    myFilter.requestFocus();

                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    // imm.showSoftInput(myFilter, InputMethodManager.SHOW_IMPLICIT);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

                    myFilter.addTextChangedListener(new TextWatcher() {

                        public void afterTextChanged(Editable s) {
                        }

                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                            // Logs.myLog("Filter on text: " + s , 3);
                            adapter.getFilter().filter(s.toString()); // CRASH REPORTED HERE
                        }
                    });
                } else {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(myFilter.getWindowToken(), 0);

                    myFilter.setVisibility(View.GONE);
                    myFilter.setText("");
                    // adapter.getFilter().filter(null);
                }

            }
        });


    }


    public void appRefresh()
    {
        // rebuild the app list
        Iterator<Integer> it = Global.appListFW.keySet().iterator();
        appInfoSource = new ArrayList<AppInfo>();

        while (it.hasNext())
        {
            int key = it.next();
            AppInfo thisApp = Global.appListFW.get(key);
            if (thisApp.system == Global.settingsEnableExpert) {
                appInfoSource.add(thisApp);
                // Log.w("FWWidget2", "Add GUI app: " + thisApp.name);
            }
        }


        mySort0(appInfoSource);

        adapter = new AppInfoAdapterApps(myContext, appInfoSource);
        adapter.updateFull();
        // notify?
        // adapter.notifyDataSetChanged();
        listView = (ListView) findViewById(R.id.listViewApps);
        listView.setAdapter(adapter);

        // setListViewFocus();
    }


    // Sort all alphabetically
    void mySort0( ArrayList<AppInfo> appInfo2)
    {
        Collections.sort(appInfo2, new Comparator<AppInfo>() {
            public int compare(AppInfo appInfoA, AppInfo appInfoB) {
                return appInfoA.name.compareToIgnoreCase(appInfoB.name);
            }
        });
    }

    public void changeSelectedItem(int pos)
    {
        AppInfo thisApp = adapter.getItem(pos);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(myContext);

        RemoteViews views = new RemoteViews(myContext.getPackageName(),
                R.layout.fok_widget2_layout);
        appWidgetManager.updateAppWidget(appWidgetId, views);


        Intent i = new Intent(myContext, FOKWidget2Provider.class);
        i.setAction(Global.TOGGLEAPP);
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(myContext, 0, i, 0);

        // Log.w("FWWidget2 Config","App Widget ID = " + appWidgetId + " - UID = " + thisApp.UID2);

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getContext());

        p.edit().putInt("W-" + appWidgetId, thisApp.UID2).apply();

        // boolean state = p.getBoolean("FW-" + thisApp.UID2,false);

         //   views.setImageViewBitmap(R.id.widgit2_icon,thisApp.icon);

        // views.setImageViewResource(R.id.widgit2_icon, R.drawable.fw_w_app);

        views.setImageViewBitmap(R.id.widgit2_icon, Global.drawableToBitmap(thisApp.icon));

        if (thisApp.fw == true) {
            views.setViewVisibility(R.id.widgit2_allow, View.INVISIBLE);
            views.setViewVisibility(R.id.widgit2_deny, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.widgit2_deny, View.INVISIBLE);
            views.setViewVisibility(R.id.widgit2_allow, View.VISIBLE);
        }

        if (Global.getFirewallState() == true) {
            views.setViewVisibility(R.id.widgit2_wall, View.VISIBLE);
            views.setViewVisibility(R.id.widgit2_disabled, View.INVISIBLE);
        } else {
            views.setViewVisibility(R.id.widgit2_disabled, View.VISIBLE);
            views.setViewVisibility(R.id.widgit2_wall, View.INVISIBLE);
        }


        // set the onclick again cos - onsuper and onupdate do not always work :-(
        views.setOnClickPendingIntent(R.id.widgit2_icon, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetId, views);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();

    }

    public void setListViewFocus()
    {

    }




    void displayAppDialog()
    {
        // Display a loading dialog box until the app list is prepared by the service.
        appLoad = new Dialog(myContext);
        // appLoad = new ProgressDialog(myContext);

        currentProgress = 0;

        // appLoad.setContentView(R.layout.dialog_load);
        appLoad.setContentView(R.layout.dialog_progress);
        appLoad.setTitle(R.string.LoadingApps);

        TextView text = (TextView) appLoad.findViewById(R.id.infoMessage);
        text.setText(R.string.BuildingApps);

        text = (TextView) appLoad.findViewById(R.id.infoMessage2);
        text.setText(R.string.InfoLoadingApps);
        // text.setGravity(i);

        // Runs on GUI thread (should I use a service)
        Thread thread = new Thread() {
            @Override
            public void run() {
                Global.getAppList();
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(Global.APPS_REFRESH_INTENT );
                myContext.sendBroadcast(broadcastIntent);
            }
        };

        thread.start();

        appLoad.show();
    }

    private class BroadcastListener extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            // Logs.myLog("App Received intent", 2);

            // Log.w("FWMain", "Got Action = " + intent.getAction());

            if (Global.APPS_LOADING_INTENT.equals(intent.getAction())) {
                // Logs.myLog("App Loading intent received", 2);
                // Global.myLog("App Received intent to update saving", 2);
                // close dialog just in case we are stil showing it
                if (appLoad != null) {
                    ProgressBar progBar = (ProgressBar) appLoad.findViewById(R.id.progBar);
                    currentProgress = currentProgress + 1;
                    int x1 = (int) (currentProgress /(float)Global.packageMax*100);
                    // Global.myLog("Progress = " + x1, 2);
                    // Global.myLog("Progress = " + currentProgress + "/" +Global.packageMax, 2);
                    progBar.setProgress(x1);
                }
            }

            if (Global.APPS_REFRESH_INTENT.equals(intent.getAction()))
            {
                Logs.myLog("App Received intent to update apps", 2);
                if (Global.appListFW != null)
                {
                    Logs.myLog("Redisplay Apps", 2);

                    // reset adapter to app list built by service

                    Logs.myLog("Rebuilt app list", 2);

                    // close the dialog box if we had one open
                    if (appLoad != null) {
                        Logs.myLog("Close dialog", 2);

                        appLoad.cancel();  // can I take progress input from service?
                        appLoad.dismiss();
                        appLoad = null;

                        AppWidgetManager man = AppWidgetManager.getInstance(myContext);
                        int[] ids = man.getAppWidgetIds(
                                new ComponentName(myContext,FOKWidget2Provider.class));
                        for (int i=0; i<ids.length; i++) {
                            int appWidgetId = ids[i];
                            Intent updateIntent = new Intent();
                            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                            // updateIntent.putExtra(MyWidgetProvider.WIDGET_DATA_KEY, data);
                            myContext.sendBroadcast(updateIntent);
                        }
                    }
                    appRefresh();
                    // createGUI(); // recreate..?

                }
            }

        }
    };


    @Override
    protected void onResume() {
        super.onResume();

        // register receiver
        mReceiver = new FOKWidget2Configure.BroadcastListener();
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Global.FIREWALL_STATE_CHANGE);
        mIntentFilter.addAction(Global.FIREWALL_STATE_OFF);
        mIntentFilter.addAction(Global.FIREWALL_STATE_ON);
        mIntentFilter.addAction(Global.SCREEN_REFRESH_INTENT);
        mIntentFilter.addAction(Global.APPS_REFRESH_INTENT);
        mIntentFilter.addAction(Global.APPS_LOADING_INTENT);
        mIntentFilter.addAction(Global.TOGGLEAPP_REFRESH);

        registerReceiver(mReceiver, mIntentFilter);

        // Count packages - a quick and dirty way to see if there have been any changes
        // Rather than using a package broadcast receiver
        List<PackageInfo> packageInfoList = Global.getContext().getPackageManager().getInstalledPackages(0);
        int packages = packageInfoList.size();

        // App build list should be underway
        if ( (Global.packageDone == false) || (Global.packageMax != packages) ) {
            displayAppDialog();
        } else {
            appRefresh();
        }

    }


    public void showOptionsMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.inflate(R.menu.menu_widget);
        // getMenuInflater().inflate(R.menu.menu_main, menu);

        EditText myFilter = (EditText) findViewById(R.id.activity_main_filter_text);
        if (myFilter.getVisibility() == View.VISIBLE) {
            myFilter.setVisibility(View.GONE);
            myFilter.setText("");
        }


        Menu m = popup.getMenu();
        MenuItem item = m.findItem(R.id.action_apps_system);


        if (Global.settingsEnableExpert == true) {
            item.setTitle(R.string.activity_main_menu_apps_user);
        } else {
            item.setTitle(R.string.activity_main_menu_apps_system);
        }


        // popup.getMenu().findItem(R.id.menu_name).setTitle(text);
        invalidateOptionsMenu();


        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {


            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_logs:
                        Intent intent = new Intent(myContext, ActivityLogs.class);
                        startActivity(intent);
                        return true;
                    case R.id.action_apps_system:
                        if (Global.settingsEnableExpert == true) {
                            Global.settingsEnableExpert = false;
                        } else {
                            Global.settingsEnableExpert = true;
                        }
                        Global.saveSetings();
                        appRefresh();
                        return true;

                    case R.id.action_refresh:
                        displayAppDialog();
                        return true;

                    default:
                        return false;
                }
            }

        });

        popup.show();

    }

}
