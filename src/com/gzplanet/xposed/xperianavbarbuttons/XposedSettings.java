package com.gzplanet.xposed.xperianavbarbuttons;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import sheetrock.panda.changelog.ChangeLog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.robobunny.SeekBarPreference;

public class XposedSettings extends PreferenceActivity {
    public final static String DELIMITER_LAUNCHKEY = ",";

    private final static int RESULT_REORDER = 1;
    private final static int RESULT_THEME = 2;
    private final static int RESULT_CREATE_SHORTCUT = 3;
    private final static int RESULT_CREATE_LP_SHORTCUT = 4;
    private final static int RESULT_PICK_SHORTCUT = 5;
    private final static int RESULT_PICK_LP_SHORTCUT = 6;

    int mScreenWidth;
    int mButtonWidth;
    int mButtonsCount = 2;
    boolean mShowMenu;
    boolean mShowSearch;
    boolean mShowRecent;
    boolean mShowSeparator;
    String mThemeId;
    String mThemeColor;
    boolean mUseTheme;
    boolean mUseAltMenu;
    String mCacheFolder;
    int mDensityDpi;
    int mLeftMargin;
    int mRightMargin;
    int mNavBarHeight;
    int mExtraPadding;
    int mSeparatorWidth;

    String mSearchKeycode;
    String mSearchLongPressKeycode;
    String mSearchFuncApp;
    String mSearchLongPressFuncApp;
    String mSearchFuncShortcut;
    String mSearchLongPressFuncShortcut;
    String mSearchFuncShortcutApp;
    String mSearchLongPressFuncShortcutApp;

    private ArrayList<String> mSearchFuncArray = new ArrayList<String>();
    private ArrayList<String> mSearchKeycodeValues = new ArrayList<String>();

    // Installed activities
    private ArrayList<String> mAppActivity = new ArrayList<String>();
    private ArrayList<String> mAppPackageName = new ArrayList<String>();
    private ArrayList<String> mAppComponentName = new ArrayList<String>();
    private ArrayList<String> mAppLaunchKey = new ArrayList<String>();
    private ArrayList<Drawable> mAppActivityIcon = new ArrayList<Drawable>();

    CheckBoxPreference mPrefShowRecent;
    CheckBoxPreference mPrefShowMenu;
    CheckBoxPreference mPrefShowSearch;
    CheckBoxPreference mPrefShowSeparator;
    Preference mPrefRestartSystemUI;
    Preference mPrefReorder;
    Preference mPrefTheme;
    CheckBoxPreference mPrefUseAltMenu;
    Preference mPrefHints;
    Preference mPrefSeparatorWidth;
    Preference mPrefLeftMargin;
    Preference mPrefRightMargin;
    Preference mPrefNavBarHeight;
    ListPreference mPrefSearchButtonFunc;
    ListPreference mPrefSearchButtonLongPressFunc;
    AppListPreference mPrefSearchButtonFuncApps;
    AppListPreference mPrefSearchLongPressButtonFuncApps;

    ButtonSettings mSettings;
    ThemeIcons mThemeIcons = new ThemeIcons();
    CustomButtons mCustomButtons;

    static int[] mIconId = {R.id.iv1, R.id.iv2, R.id.iv3, R.id.iv4, R.id.iv5, R.id.iv6};

    // async task to load installed app list
    private class LoadInstalledAppTask extends AsyncTask<Void, Void, Void> {
        final Context mContext;

        public LoadInstalledAppTask(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            final Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            final PackageManager pm = mContext.getPackageManager();

            List<ResolveInfo> apps = pm.queryIntentActivities(intent, 0);

            Comparator<ResolveInfo> icc = new Comparator<ResolveInfo>() {
                @Override
                public int compare(ResolveInfo lhs, ResolveInfo rhs) {
                    final String name1 = lhs.loadLabel(pm).toString();
                    final String name2 = rhs.loadLabel(pm).toString();

                    return name1.compareToIgnoreCase(name2);
                }

            };
            Collections.sort(apps, icc);

            // retrieve standard icon size from this package
            Drawable appIcon = mContext.getResources().getDrawable(R.drawable.ic_launcher);
            int width = ((BitmapDrawable) appIcon).getIntrinsicWidth();

            for (int i = 0; i < apps.size(); i++) {
                mAppActivity.add(apps.get(i).loadLabel(pm).toString());
                mAppPackageName.add(apps.get(i).activityInfo.packageName);
                mAppComponentName.add(apps.get(i).activityInfo.name);
                mAppLaunchKey.add(apps.get(i).activityInfo.packageName + DELIMITER_LAUNCHKEY
                        + apps.get(i).activityInfo.name);
                // resize app icon because some apps are loaded with over-sized
                // icons
                mAppActivityIcon.add(Utils.resizeDrawable(mContext.getResources(),
                        apps.get(i).activityInfo.applicationInfo.loadIcon(pm), width, width));
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // populate app listpreferences
            mPrefSearchButtonFuncApps.setEntries(mAppActivity.toArray(new CharSequence[mAppActivity.size()]));
            mPrefSearchButtonFuncApps.setEntryValues(mAppLaunchKey.toArray(new CharSequence[mAppLaunchKey.size()]));
            mPrefSearchButtonFuncApps.setEntryIcons(mAppActivityIcon.toArray(new Drawable[mAppActivityIcon.size()]));
            mPrefSearchLongPressButtonFuncApps.setEntries(mAppActivity.toArray(new CharSequence[mAppActivity.size()]));
            mPrefSearchLongPressButtonFuncApps.setEntryValues(mAppLaunchKey.toArray(new CharSequence[mAppLaunchKey
                    .size()]));
            mPrefSearchLongPressButtonFuncApps.setEntryIcons(mAppActivityIcon.toArray(new Drawable[mAppActivityIcon
                    .size()]));

            // display preference summary
            mPrefSearchButtonFunc.setSummary(getSearchFuncSummary(mSearchKeycode, false));
            mPrefSearchButtonLongPressFunc.setSummary(getSearchFuncSummary(mSearchLongPressKeycode, true));

            // enable search button action preferences
            mPrefSearchButtonFunc.setEnabled(mShowSearch);
            mPrefSearchButtonLongPressFunc.setEnabled(mShowSearch);

            super.onPostExecute(result);
        }

    }

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTitle(R.string.app_name);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // this is important because although the handler classes that read
        // these settings
        // are in the same package, they are executed in the context of the
        // hooked package
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        addPreferencesFromResource(R.xml.preferences);

        // get screen width
        mScreenWidth = Utils.getScreenWidth(this);
        int maxWidth = (int) (mScreenWidth * 0.75);

        // for Lollipop, add extra padding for IME switcher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            mExtraPadding = Utils.getPackageResDimension(this, "navigation_extra_key_width", "dimen",
                    XperiaNavBarButtons.CLASSNAME_SYSTEMUI);
        else
            mExtraPadding = 0;

        mPrefShowRecent = (CheckBoxPreference) findPreference("pref_show_recent");
        mPrefShowMenu = (CheckBoxPreference) findPreference("pref_show_menu");
        mPrefShowSearch = (CheckBoxPreference) findPreference("pref_show_search");
        mPrefShowSeparator = (CheckBoxPreference) findPreference("pref_show_separator");
        mPrefReorder = (Preference) findPreference("pref_reorder");
        mPrefTheme = (Preference) findPreference("pref_theme");
        mPrefRestartSystemUI = (Preference) findPreference("pref_restart_systemui");
        mPrefUseAltMenu = (CheckBoxPreference) findPreference("pref_use_alt_menu");
        mPrefHints = (Preference) findPreference("pref_hints");
        mPrefSeparatorWidth = (Preference) findPreference("pref_separator_width");
        mPrefLeftMargin = (Preference) findPreference("pref_left_margin");
        mPrefRightMargin = (Preference) findPreference("pref_right_margin");
        mPrefNavBarHeight = (Preference) findPreference("pref_navbar_height");
        mPrefSearchButtonFunc = (ListPreference) findPreference("pref_search_function");
        mPrefSearchButtonLongPressFunc = (ListPreference) findPreference("pref_search_longpress_function");
        mPrefSearchButtonFuncApps = (AppListPreference) findPreference("pref_search_function_apps");
        mPrefSearchLongPressButtonFuncApps = (AppListPreference) findPreference("pref_search_longpress_function_apps");

        reloadPreferences();

        mShowMenu = mSettings.isShowMenu();
        if (mShowMenu)
            mButtonsCount++;
        mShowSearch = mSettings.isShowSearch();
        if (mShowSearch)
            mButtonsCount++;
        mShowRecent = mSettings.isShowRecent();
        if (mShowRecent)
            mButtonsCount++;
        mShowSeparator = mSettings.isShowSeparator();
        if (mShowSeparator)
            mButtonsCount++;

        mCacheFolder = Utils.getSystemUICacheFolder(this);
        mDensityDpi = Utils.getDensityDpi(getResources());
        mCustomButtons = new CustomButtons(mCacheFolder, mDensityDpi, "custom_");

        // load available search button functions
        Collections.addAll(mSearchFuncArray, getResources().getStringArray(
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? R.array.array_search_func_name_n : R.array.array_search_func_name));
        Collections.addAll(mSearchKeycodeValues, getResources().getStringArray(
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? R.array.array_search_func_keycode_n : R.array.array_search_func_keycode));

        storePreferences();

        updatePreviewPanel();

        mPrefShowRecent.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue)
                    mButtonsCount++;
                else
                    mButtonsCount--;
                mShowRecent = (Boolean) newValue;
                mSettings.setShowRecent(mShowRecent);
                getPreferenceManager().getSharedPreferences().edit()
                        .putString("pref_order", mSettings.getOrderListString()).commit();
                updatePreviewPanel();
                return true;
            }
        });

        mPrefShowMenu.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue)
                    mButtonsCount++;
                else
                    mButtonsCount--;
                mShowMenu = (Boolean) newValue;
                mSettings.setShowMenu(mShowMenu);
                getPreferenceManager().getSharedPreferences().edit()
                        .putString("pref_order", mSettings.getOrderListString()).commit();
                updatePreviewPanel();
                return true;
            }
        });

        mPrefShowSearch.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue)
                    mButtonsCount++;
                else
                    mButtonsCount--;
                mShowSearch = (Boolean) newValue;
                mSettings.setShowSearch(mShowSearch);
                getPreferenceManager().getSharedPreferences().edit()
                        .putString("pref_order", mSettings.getOrderListString()).commit();
                updatePreviewPanel();
                mPrefSearchButtonFunc.setEnabled(mShowSearch);
                mPrefSearchButtonLongPressFunc.setEnabled(mShowSearch);
                return true;
            }
        });

        mPrefShowSeparator.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue)
                    mButtonsCount++;
                else
                    mButtonsCount--;
                mShowSeparator = (Boolean) newValue;
                mSettings.setShowSeparator(mShowSeparator);
                getPreferenceManager().getSharedPreferences().edit()
                        .putString("pref_order", mSettings.getOrderListString()).commit();
                updatePreviewPanel();
                return true;
            }
        });

        mPrefReorder.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(XposedSettings.this, ReorderActivity.class);
                intent.putExtra("order_list", mSettings.getOrderListString());
                intent.putExtra("left_margin", mLeftMargin);
                intent.putExtra("right_margin", mRightMargin);
                startActivityForResult(intent, RESULT_REORDER);
                return true;
            }
        });

        mPrefTheme.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(XposedSettings.this, ThemeActivity.class);
                intent.putExtra("usetheme", mUseTheme);
                intent.putExtra("themeid", mThemeId);
                intent.putExtra("themecolor", mThemeColor);
                startActivityForResult(intent, RESULT_THEME);
                return true;
            }
        });

        mPrefUseAltMenu.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mUseAltMenu = (Boolean) newValue;
                getPreferenceManager().getSharedPreferences().edit().putBoolean("pref_use_alt_menu", mUseAltMenu)
                        .commit();
                updatePreviewPanel();
                return true;
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mPrefSearchButtonFunc.setEntries(R.array.array_search_func_name_n);
            mPrefSearchButtonFunc.setEntryValues(R.array.array_search_func_keycode_n);
        }
        mPrefSearchButtonFunc.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (Integer.valueOf(newValue.toString()) == XperiaNavBarButtons.KEYCODE_LAUNCH_APP) {
                    mPrefSearchButtonFuncApps.show();
                } else if (Integer.valueOf(newValue.toString()) == XperiaNavBarButtons.KEYCODE_LAUNCH_SHORTCUT) {
                    Intent intent = new Intent(Intent.ACTION_PICK_ACTIVITY);
                    intent.putExtra(Intent.EXTRA_INTENT, new Intent(Intent.ACTION_CREATE_SHORTCUT));
                    startActivityForResult(intent, RESULT_PICK_SHORTCUT);
                } else {
                    preference.setSummary(getSearchFuncSummary(newValue.toString(), false));
                }
                return true;
            }
        });
        mPrefSearchButtonFunc.setEnabled(false);

        mPrefSearchButtonFuncApps.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mSearchFuncApp = newValue.toString();
                mPrefSearchButtonFunc.setSummary(getSearchFuncSummary(
                        String.valueOf(XperiaNavBarButtons.KEYCODE_LAUNCH_APP), false));
                return true;
            }
        });
        getPreferenceScreen().removePreference(mPrefSearchButtonFuncApps);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mPrefSearchButtonLongPressFunc.setEntries(R.array.array_search_func_name_n);
            mPrefSearchButtonLongPressFunc.setEntryValues(R.array.array_search_func_keycode_n);
        }
        mPrefSearchButtonLongPressFunc.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (Integer.valueOf(newValue.toString()) == XperiaNavBarButtons.KEYCODE_LAUNCH_APP) {
                    mPrefSearchLongPressButtonFuncApps.show();
                } else if (Integer.valueOf(newValue.toString()) == XperiaNavBarButtons.KEYCODE_LAUNCH_SHORTCUT) {
                    Intent intent = new Intent(Intent.ACTION_PICK_ACTIVITY);
                    intent.putExtra(Intent.EXTRA_INTENT, new Intent(Intent.ACTION_CREATE_SHORTCUT));
                    startActivityForResult(intent, RESULT_PICK_LP_SHORTCUT);
                } else {
                    preference.setSummary(getSearchFuncSummary(newValue.toString(), true));
                }
                return true;
            }
        });
        mPrefSearchButtonLongPressFunc.setEnabled(false);

        mPrefSearchLongPressButtonFuncApps.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mSearchLongPressFuncApp = newValue.toString();
                mPrefSearchButtonLongPressFunc.setSummary(getSearchFuncSummary(
                        String.valueOf(XperiaNavBarButtons.KEYCODE_LAUNCH_APP), true));
                return true;
            }
        });
        getPreferenceScreen().removePreference(mPrefSearchLongPressButtonFuncApps);

        mPrefSeparatorWidth.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mSeparatorWidth = (Integer) newValue;
                updatePreviewPanel();
                return true;
            }

        });

        mPrefLeftMargin.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mLeftMargin = (Integer) newValue;
                updatePreviewPanel();
                return true;
            }

        });
        ((SeekBarPreference) mPrefLeftMargin).setMaxValue(maxWidth);

        mPrefRightMargin.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mRightMargin = (Integer) newValue;
                updatePreviewPanel();
                return true;
            }

        });
        ((SeekBarPreference) mPrefRightMargin).setMaxValue(maxWidth);

        mPrefNavBarHeight.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mNavBarHeight = (Integer) newValue;
                updatePreviewPanel();
                return true;
            }

        });

        mPrefRestartSystemUI.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Intent intent = new Intent();
                        intent.setAction(XperiaNavBarButtons.ACTION_NAVBAR_CHANGED);
                        intent.putExtra("order_list", mSettings.getOrderListString());
                        intent.putExtra("show_menu", mShowMenu);
                        intent.putExtra("show_toast", true);
                        sendBroadcast(intent);
                    } else {
                        Toast.makeText(XposedSettings.this, "Restarting SystemUI...", Toast.LENGTH_SHORT).show();

                        final String pkgName = XposedSettings.this.getPackageName();
                        final String pkgFilename = pkgName + "_preferences";
                        final File prefFile = new File(Environment.getDataDirectory(), "data/" + pkgName + "/shared_prefs/"
                                + pkgFilename + ".xml");
                        Log.d("XposedSettings", prefFile.getAbsolutePath());

                        // make shared preference world-readable
                        Process sh = Runtime.getRuntime().exec("su", null, null);
                        OutputStream os = sh.getOutputStream();
                        os.write(("chmod 664 " + prefFile.getAbsolutePath()).getBytes("ASCII"));
                        os.flush();
                        os.close();
                        try {
                            sh.waitFor();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // restart SystemUI process
                        sh = Runtime.getRuntime().exec("su", null, null);
                        os = sh.getOutputStream();
                        os.write(("pkill com.android.systemui").getBytes("ASCII"));
                        os.flush();
                        os.close();
                        try {
                            sh.waitFor();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });

        mPrefHints.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                (new ChangeLog(XposedSettings.this)).getFullLogDialog().show();
                return true;
            }
        });

        // load installed app by async task
        (new LoadInstalledAppTask(this)).execute();

        // display change log
        ChangeLog cl = new ChangeLog(this);
        if (cl.firstRun())
            cl.getFullLogDialog().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Intent intent;
        String name;
        ResolveInfo ri;

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case RESULT_REORDER:
                    String order = data.getStringExtra("order_list");
                    getPreferenceManager().getSharedPreferences().edit().putString("pref_order", order).commit();
                    mSettings = new ButtonSettings(this, order);
                    updatePreviewPanel();
                    break;
                case RESULT_THEME:
                    mUseTheme = data.getBooleanExtra("usetheme", false);
                    mThemeId = data.getStringExtra("themeid");
                    mThemeColor = data.getStringExtra("themecolor");
                    Editor editor = getPreferenceManager().getSharedPreferences().edit();
                    editor.putBoolean("pref_usetheme", mUseTheme);
                    editor.putString("pref_themeid", mThemeId);
                    editor.putString("pref_themecolor", mThemeColor);
                    editor.commit();
                    mCustomButtons.refresh();
                    updatePreviewPanel();
                    break;
                case RESULT_CREATE_SHORTCUT:
                    intent = (Intent) data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
                    name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
                    intent.putExtra("shortcutName", name);
                    intent.putExtra("shortcutApp", mSearchFuncShortcutApp);
                    mSearchFuncShortcut = intent.toUri(0);
                    getPreferenceManager().getSharedPreferences().edit()
                            .putString("pref_search_function_shortcut", mSearchFuncShortcut).commit();
                    mPrefSearchButtonFunc.setSummary(getSearchFuncSummary(
                            String.valueOf(XperiaNavBarButtons.KEYCODE_LAUNCH_SHORTCUT), false));
                    break;
                case RESULT_CREATE_LP_SHORTCUT:
                    intent = (Intent) data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
                    name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
                    intent.putExtra("shortcutName", name);
                    intent.putExtra("shortcutApp", mSearchLongPressFuncShortcutApp);
                    mSearchLongPressFuncShortcut = intent.toUri(0);
                    getPreferenceManager().getSharedPreferences().edit()
                            .putString("pref_search_longpress_function_shortcut", mSearchLongPressFuncShortcut).commit();
                    mPrefSearchButtonLongPressFunc.setSummary(getSearchFuncSummary(
                            String.valueOf(XperiaNavBarButtons.KEYCODE_LAUNCH_SHORTCUT), true));
                    break;
                case RESULT_PICK_SHORTCUT:
                    ri = getPackageManager().resolveActivity(data, 0);
                    mSearchFuncShortcutApp = ri.loadLabel(getPackageManager()).toString();
                    startActivityForResult(data, RESULT_CREATE_SHORTCUT);
                    break;
                case RESULT_PICK_LP_SHORTCUT:
                    ri = getPackageManager().resolveActivity(data, 0);
                    mSearchLongPressFuncShortcutApp = ri.loadLabel(getPackageManager()).toString();
                    startActivityForResult(data, RESULT_CREATE_LP_SHORTCUT);
                    break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updatePreviewPanel() {
        int extraPadding = mShowMenu ? 0 : mExtraPadding;
        int separatorWidth = mShowSeparator ? Math.round((float) mScreenWidth * (float) mSeparatorWidth / 100) : 0;
        int actualButtonsCount = mShowSeparator ? mButtonsCount - 1 : mButtonsCount;

        mButtonWidth = Math
                .round((float) (mScreenWidth - mLeftMargin - mRightMargin - extraPadding * 2 - separatorWidth)
                        / (float) actualButtonsCount);

        // get default navbar height
        int navBarHeight = (int) Utils.getNavBarHeight(getResources()) * mNavBarHeight / 100;

        LinearLayout panel = (LinearLayout) findViewById(R.id.previewPanel);
        if (navBarHeight > 0)
            panel.getLayoutParams().height = navBarHeight;

        ImageView leftMargin = (ImageView) panel.findViewById(R.id.left_margin);
        leftMargin.setLayoutParams(new LinearLayout.LayoutParams(mLeftMargin + extraPadding,
                LinearLayout.LayoutParams.MATCH_PARENT, 0.0f));

        ImageView rightMargin = (ImageView) panel.findViewById(R.id.right_margin);
        rightMargin.setLayoutParams(new LinearLayout.LayoutParams(mRightMargin + extraPadding,
                LinearLayout.LayoutParams.MATCH_PARENT, 0.0f));

        for (int i = 0; i < mIconId.length; i++) {
            ImageView iv = (ImageView) panel.findViewById(mIconId[i]);
            if (i < mButtonsCount) {
                if ("Separator".equals(mSettings.getButtonName(i))) {
                    iv.setLayoutParams(new LinearLayout.LayoutParams(separatorWidth,
                            LinearLayout.LayoutParams.MATCH_PARENT, 0.0f));
                    iv.setImageDrawable(null);
                } else {
                    boolean useAlt = false;

                    iv.setLayoutParams(new LinearLayout.LayoutParams(mButtonWidth,
                            LinearLayout.LayoutParams.MATCH_PARENT, 0.0f));
                    if ("Menu".equals(mSettings.getButtonName(i)))
                        useAlt = mUseAltMenu;

                    Drawable drawable = null;
                    if (mUseTheme) {
                        int buttonResId = mThemeIcons.getIconResId(mThemeId, mThemeColor, mSettings.getButtonName(i),
                                useAlt, false);
                        if (buttonResId != -1)
                            drawable = getResources().getDrawable(buttonResId);
                        else {
                            Bitmap bitmap = mCustomButtons.getBitmap(mSettings.getButtonName(i), useAlt, false);
                            if (bitmap == null)
                                drawable = mSettings.getButtonDrawable(i);
                            else
                                drawable = new BitmapDrawable(getResources(), bitmap);
                        }
                    } else {
                        drawable = mSettings.getButtonDrawable(i);
                    }
                    iv.setImageDrawable(drawable);
                }
                iv.setVisibility(View.VISIBLE);
            } else {
                iv.setVisibility(View.GONE);
            }
        }

        mPrefUseAltMenu.setEnabled(mUseTheme);
    }

    private void reloadPreferences() {
        SharedPreferences pref = getPreferenceManager().getSharedPreferences();

        mThemeId = pref.getString("pref_themeid", XperiaNavBarButtons.DEF_THEMEID);
        mThemeColor = pref.getString("pref_themecolor", XperiaNavBarButtons.DEF_THEMECOLOR);

        mSettings = new ButtonSettings(this, pref.getString("pref_order", null));

        mUseTheme = pref.getBoolean("pref_usetheme", false);
        mUseAltMenu = pref.getBoolean("pref_use_alt_menu", false);

        mLeftMargin = pref.getInt("pref_left_margin", 0);
        mRightMargin = pref.getInt("pref_right_margin", 0);

        mNavBarHeight = pref.getInt("pref_navbar_height", 100);

        mSearchFuncShortcut = pref.getString("pref_search_function_shortcut", null);
        mSearchLongPressFuncShortcut = pref.getString("pref_search_longpress_function_shortcut", null);

        mSearchKeycode = pref.getString("pref_search_function", String.valueOf(
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? -1 : KeyEvent.KEYCODE_SEARCH));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mSearchKeycode.equals(String.valueOf(KeyEvent.KEYCODE_SEARCH)))
            mSearchKeycode = "-1";
        mSearchLongPressKeycode = pref.getString("pref_search_longpress_function", "-1");
        mSearchFuncApp = pref.getString("pref_search_function_apps", null);
        mSearchLongPressFuncApp = pref.getString("pref_search_longpress_function_apps", null);

        mSeparatorWidth = pref.getInt("pref_separator_width", 0);
    }

    // store necesary info for user define theme to work
    private void storePreferences() {
        Editor editor = getPreferenceManager().getSharedPreferences().edit();

        editor.putString("pref_cache_folder", mCacheFolder);
        editor.putInt("pref_density_dpi", mDensityDpi);
        editor.commit();
    }

    private String getSearchFuncSummary(String keyCode, boolean longPress) {
        String keys[];
        String component = null;

        if (Integer.valueOf(keyCode) == XperiaNavBarButtons.KEYCODE_LAUNCH_APP) {
            String key = longPress ? mSearchLongPressFuncApp : mSearchFuncApp;
            if (key != null) {
                keys = key.split(DELIMITER_LAUNCHKEY);
                component = keys[1];
            }

            int pos = mAppComponentName.indexOf(component);
            String activityName = (pos >= 0) ? mAppActivity.get(pos) : getResources().getString(R.string.text_unknown);
            return String.format(getResources().getString(R.string.summ_search_function2), mSearchFuncArray
                            .get(mSearchKeycodeValues.indexOf(String.valueOf(XperiaNavBarButtons.KEYCODE_LAUNCH_APP))),
                    activityName);
        } else if (Integer.valueOf(keyCode) == XperiaNavBarButtons.KEYCODE_LAUNCH_SHORTCUT) {
            String name = null;
            String app = null;
            Intent intent;
            try {
                String uri = longPress ? mSearchLongPressFuncShortcut : mSearchFuncShortcut;
                if (uri != null) {
                    intent = Intent.parseUri(uri, 0);
                    name = intent.getStringExtra("shortcutName");
                    app = intent.getStringExtra("shortcutApp");
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            return String.format(getResources().getString(R.string.summ_search_function3), mSearchFuncArray
                            .get(mSearchKeycodeValues.indexOf(String.valueOf(XperiaNavBarButtons.KEYCODE_LAUNCH_SHORTCUT))),
                    app, name);
        } else
            return String.format(getResources().getString(R.string.summ_search_function),
                    mSearchFuncArray.get(mSearchKeycodeValues.indexOf(keyCode)));
    }

}