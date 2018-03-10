package com.gzplanet.xposed.xperianavbarbuttons;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManagerPolicy.WindowState;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.crossbowffs.remotepreferences.RemotePreferences;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LayoutInflated.LayoutInflatedParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.callbacks.XCallback;

public class XperiaNavBarButtons implements IXposedHookZygoteInit, IXposedHookInitPackageResources,
        IXposedHookLoadPackage {
    final static String PKGNAME_SYSTEM_SERVICE = "android";
    final static String CLASSNAME_SYSTEMUI = "com.android.systemui";
    final static String CLASSNAME_NAVIGATIONBARVIEW = "com.android.systemui.statusbar.phone.NavigationBarView";
    final static String CLASSNAME_PHONEWINDOWMANAGER = "com.android.internal.policy.impl.PhoneWindowManager";
    final static String CLASSNAME_PHONEWINDOWMANAGER_MM = "com.android.server.policy.PhoneWindowManager";
    final static String CLASSNAME_NAVIGATIONBUTTONTRANSITIONS = "com.android.systemui.statusbar.phone.NavigationButtonTransitions";

    // Nougat settings
    final static String CLASSNAME_NAVIGATIONBARINFLATERVIEW = "com.android.systemui.statusbar.phone.NavigationBarInflaterView";

    final static String DEF_THEMEID = "Stock";
    final static String DEF_THEMECOLOR = "White";

    final static int KEYCODE_NONE = -1;
    final static int KEYCODE_SWITCH_LAST_APP = -2;
    final static int KEYCODE_TAKE_SCREENSHOT = -3;
    final static int KEYCODE_SCREEN_OFF = -4;
    final static int KEYCODE_POWER_MENU = -5;
    final static int KEYCODE_NOTIFICATION_PANEL = -6;
    final static int KEYCODE_QUICK_SETTINGS_PANEL = -7;
    final static int KEYCODE_LAUNCH_APP = -8;
    final static int KEYCODE_KILL_APP = -9;
    final static int KEYCODE_LAUNCH_SHORTCUT = -10;

    final static int NAVIGATION_HINT_IME_SHOWN = 1 << 1;
    final static String DEF_BUTTONS_ORDER_LIST = "Home,Menu,Recent,Back,Search";
    final static String DEF_STOCK_BUTTONS_ORDER_LIST = "Back,Home,Recent";

    // Nougat settings
    final static String GRAVITY_SEPARATOR = ";";
    final static String BUTTON_SEPARATOR = ",";
    final static int RES_ID_SEARCH = 0;
    final static String KEY_MENU_N = "menu_ime";
    final static String KEY_HOME_N = "home";
    final static String KEY_BACK_N = "back";
    final static String KEY_RECENT_N = "recent";
    final static String KEY_SEARCH_N = "search";
    final static String KEY_SEPARATOR_N = "separator";
    final static String KEY_LEFT_MARGIN_N = "left_margin";
    final static String KEY_RIGHT_MARGIN_N = "right_margin";
    final static String KEY_LEFT_IME_PADDING_N = "left_ime_padding";
    final static String ACTION_NAVBAR_CHANGED = "com.gzplanet.xposed.xperianavbarbuttons.CHANGED";

    static String MODULE_PATH = null;
    static Context mContext;
    static int mDisabledFlags;
    static XModuleResources modRes;
    static boolean mShowSearch;
    static boolean mShowMenu;
    static boolean mShowRecent;
    static XSharedPreferences pref;

    static int mKeyCodeLongPress;
    static String mSearchFuncApp = null;
    static String mSearchLongPressFuncApp = null;
    static boolean mSearchKeyLongPressed = false;
    static boolean mInjectedKeyCode = false;
    static String mSearchFuncShortcut = null;
    static String mSearchLongPressFuncShortcut = null;

    // theme settings
    static CustomButtons mCustomButtons;
    static ThemeIcons mThemeIcons;
    static boolean mUseTheme = false;
    static boolean mUseAltMenu = false;
    static String mThemeId;
    static String mThemeColor;
    static String mCacheFolder;
    static int mDensityDpi;

    // Nougat settings
    static int mScreenWidth;
    static int mScreenWidthLand;
    static int mButtonWidth;
    static int mButtonWidthLand;
    static View mNavigationBarView;
    static View mNavigationBarInflaterView;
    static int mRotation;
    static boolean mSystemBootCompleted = false;

    Map<String, Bitmap> mStockButtons = new HashMap<String, Bitmap>();

    private static BroadcastReceiver mNavBarUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_NAVBAR_CHANGED.equals(intent.getAction())) {
                if (intent.hasExtra("show_menu") && intent.hasExtra("show_menu") && mNavigationBarView != null) {
                    mSystemBootCompleted = true;

                    String orderList = intent.getStringExtra("order_list");
                    boolean showMenu = intent.getBooleanExtra("show_menu", true);
                    boolean showToast = intent.getBooleanExtra("show_toast", false);

                    Configuration mConfiguration = (Configuration) XposedHelpers.getObjectField(mNavigationBarView, "mConfiguration");
                    XposedHelpers.callMethod(mNavigationBarView, "updateIcons", context, Configuration.EMPTY, mConfiguration);
                    XposedHelpers.callMethod(mNavigationBarView, "setMenuVisibility", showMenu, true);
                    if (mNavigationBarInflaterView != null) {
                        XposedHelpers.callMethod(mNavigationBarInflaterView, "clearViews");
                        XposedHelpers.callMethod(mNavigationBarInflaterView, "inflateLayout", getOrderListForN(orderList, showMenu));
                        if (showToast)
                            Toast.makeText(context, "Customized NavBar applied", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "mNavigationBarInflaterView is NULL", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;

        pref = new XSharedPreferences(XperiaNavBarButtons.class.getPackage().getName());
        // just in case the preference file permission is reset by
        // recovery/script
        pref.makeWorldReadable();

        pref.reload();
        mKeyCodeLongPress = Integer.parseInt(pref.getString("pref_search_longpress_function",
                String.valueOf(KEYCODE_NONE)));
        mSearchFuncApp = pref.getString("pref_search_function_apps", null);
        mSearchLongPressFuncApp = pref.getString("pref_search_longpress_function_apps", null);
        mSearchFuncShortcut = pref.getString("pref_search_function_shortcut", null);
        mSearchLongPressFuncShortcut = pref.getString("pref_search_longpress_function_shortcut", null);
        mShowMenu = pref.getBoolean("pref_show_menu", true);
        mUseTheme = pref.getBoolean("pref_usetheme", false);

        // force navbar on Samsung devices
        try {
            XResources.setSystemWideReplacement("android", "bool", "config_showNavigationBar", true);
            XposedBridge.log("Set config_showNavigationBar");
        } catch (Throwable t) {
            XposedBridge.log("Resource config_showNavigationBar not found");
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1)
            handlePhoneWindowManager(null, CLASSNAME_PHONEWINDOWMANAGER);
    }

    private void handlePhoneWindowManager(ClassLoader classLoader, String classPhoneWindowManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // handle custom navbar height
            try {
                final Class<?> phoneWindowManager = XposedHelpers.findClass(classPhoneWindowManager, classLoader);

                XposedHelpers.findMethodExact(phoneWindowManager, "onConfigurationChanged");
                XposedHelpers.findAndHookMethod(phoneWindowManager, "onConfigurationChanged", new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (mContext == null)
                            mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        SharedPreferences remotePref = new RemotePreferences(mContext, Utils.PREF_AUTHORITY, Utils.PREF_NAME);

                        int navbarHeight = remotePref.getInt("pref_navbar_height", 100);
                        int navbarHeightLand = remotePref.getInt("pref_navbar_height_land", 100);
                        int navbarWidth = remotePref.getInt("pref_navbar_width", 100);

                        Resources res = mContext.getResources();
                        int[] navigationBarHeightForRotation = new int[4];
                        int[] navigationBarWidthForRotation = new int[4];

                        int portraitRotation = XposedHelpers.getIntField(param.thisObject, "mPortraitRotation");
                        int upsideDownRotation = XposedHelpers.getIntField(param.thisObject, "mUpsideDownRotation");
                        int landscapeRotation = XposedHelpers.getIntField(param.thisObject, "mLandscapeRotation");
                        int seascapeRotation = XposedHelpers.getIntField(param.thisObject, "mSeascapeRotation");

                        navigationBarHeightForRotation[portraitRotation] = navigationBarHeightForRotation[upsideDownRotation] =
                                res.getDimensionPixelSize(res.getIdentifier("navigation_bar_height", "dimen", "android"))
                                        * navbarHeight / 100;
                        navigationBarHeightForRotation[landscapeRotation] = navigationBarHeightForRotation[seascapeRotation] =
                                res.getDimensionPixelSize(res.getIdentifier("navigation_bar_height_landscape", "dimen", "android"))
                                        * navbarHeightLand / 100;
                        navigationBarWidthForRotation[portraitRotation] = navigationBarWidthForRotation[upsideDownRotation]
                                = navigationBarWidthForRotation[landscapeRotation] = navigationBarWidthForRotation[seascapeRotation]
                                = res.getDimensionPixelSize(res.getIdentifier("navigation_bar_width", "dimen", "android"))
                                * navbarWidth / 100;

                        XposedHelpers.setObjectField(param.thisObject, "mNavigationBarHeightForRotationDefault",
                                navigationBarHeightForRotation);
                        XposedHelpers.setObjectField(param.thisObject, "mNavigationBarWidthForRotationDefault",
                                navigationBarWidthForRotation);
                    }

                    ;
                });
            } catch (ClassNotFoundError e) {
                XposedBridge.log("Class PhoneWindowManager not found");
            } catch (NoSuchMethodError e2) {
                XposedBridge.log("Method onConfigurationChanged not found");
            }
        } else {
            // handle custom navbar height
            try {
                final Class<?> phoneWindowManager = XposedHelpers.findClass(classPhoneWindowManager, classLoader);

                XposedHelpers.findMethodExact(phoneWindowManager, "setInitialDisplaySize", Display.class, int.class,
                        int.class, int.class);
                XposedHelpers.findAndHookMethod(phoneWindowManager, "setInitialDisplaySize", Display.class, int.class,
                        int.class, int.class, new XC_MethodHook() {
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                pref.reload();
                                int navbarHeight = pref.getInt("pref_navbar_height", 100);
                                int navbarHeightLand = pref.getInt("pref_navbar_height_land", 100);
                                int navbarWidth = pref.getInt("pref_navbar_width", 100);

                                Display display = (Display) param.args[0];
                                Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                                if (context == null || display.getDisplayId() != Display.DEFAULT_DISPLAY) {
                                    return;
                                }
                                Resources res = context.getResources();
                                int[] navigationBarHeightForRotation = new int[4];
                                int[] navigationBarWidthForRotation = new int[4];

                                int portraitRotation = XposedHelpers.getIntField(param.thisObject, "mPortraitRotation");
                                int upsideDownRotation = XposedHelpers.getIntField(param.thisObject, "mUpsideDownRotation");
                                int landscapeRotation = XposedHelpers.getIntField(param.thisObject, "mLandscapeRotation");
                                int seascapeRotation = XposedHelpers.getIntField(param.thisObject, "mSeascapeRotation");

                                navigationBarHeightForRotation[portraitRotation] = navigationBarHeightForRotation[upsideDownRotation] = (int) res
                                        .getDimensionPixelSize(res.getIdentifier("navigation_bar_height", "dimen",
                                                "android"))
                                        * navbarHeight / 100;
                                navigationBarHeightForRotation[landscapeRotation] = navigationBarHeightForRotation[seascapeRotation] = (int) res
                                        .getDimensionPixelSize(res.getIdentifier("navigation_bar_height_landscape",
                                                "dimen", "android"))
                                        * navbarHeightLand / 100;
                                navigationBarWidthForRotation[portraitRotation] = navigationBarWidthForRotation[upsideDownRotation] = navigationBarWidthForRotation[landscapeRotation] = navigationBarWidthForRotation[seascapeRotation] = (int) res
                                        .getDimensionPixelSize(res
                                                .getIdentifier("navigation_bar_width", "dimen", "android"))
                                        * navbarWidth / 100;

                                XposedHelpers.setObjectField(param.thisObject, "mNavigationBarHeightForRotation",
                                        navigationBarHeightForRotation);
                                XposedHelpers.setObjectField(param.thisObject, "mNavigationBarWidthForRotation",
                                        navigationBarWidthForRotation);
                            }

                            ;
                        });
            } catch (ClassNotFoundError e) {
                XposedBridge.log("Class PhoneWindowManager not found");
            } catch (NoSuchMethodError e2) {
                XposedBridge.log("Method setInitialDisplaySize not found");
            }

            // handle custom search button functions
            try {
                final Class<?> phoneWindowManager = XposedHelpers.findClass(classPhoneWindowManager, classLoader);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    XposedHelpers.findMethodExact(phoneWindowManager, "interceptKeyBeforeQueueing", KeyEvent.class,
                            int.class);
                    XposedHelpers.findAndHookMethod(phoneWindowManager, "interceptKeyBeforeQueueing", KeyEvent.class,
                            int.class, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    interceptKeyBeforeQueueing(param);
                                }

                                ;
                            });
                } else {
                    XposedHelpers.findMethodExact(phoneWindowManager, "interceptKeyBeforeQueueing", KeyEvent.class,
                            int.class, boolean.class);
                    XposedHelpers.findAndHookMethod(phoneWindowManager, "interceptKeyBeforeQueueing", KeyEvent.class,
                            int.class, boolean.class, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    interceptKeyBeforeQueueing(param);
                                }

                                ;
                            });
                }
            } catch (ClassNotFoundError e) {
                XposedBridge.log("Class PhoneWindowManager not found");
            } catch (NoSuchMethodError e2) {
                XposedBridge.log("Method interceptKeyBeforeQueueing not found");
            }
        }
    }

    private void interceptKeyBeforeQueueing(MethodHookParam param) {
        boolean systemBooted = XposedHelpers.getBooleanField(param.thisObject, "mSystemBooted");
        if (!systemBooted) {
            param.setResult(0);
            return;
        }

        KeyEvent event = (KeyEvent) param.args[0];
        boolean handled = false;
        // XposedBridge.log(String.format("mKeyCodeLongPress:%d, mSearchFuncApp:%s, mSearchLongPressFuncApp:%s",
        // mKeyCodeLongPress, mSearchFuncApp,
        // mSearchLongPressFuncApp));

        final boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        final int keyCode = event.getKeyCode();

        if (keyCode != KeyEvent.KEYCODE_MENU && keyCode != KeyEvent.KEYCODE_BACK) {
            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            Handler handler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");

            if (keyCode < 0 || mKeyCodeLongPress != KEYCODE_NONE) {
                if (down) {
                    if (event.getRepeatCount() > 0) {
                        // handle long pressed
                        mSearchKeyLongPressed = true;
                        if (mKeyCodeLongPress != KEYCODE_NONE) {
                            performAction(context, handler, mKeyCodeLongPress, true, param.thisObject,
                                    mSearchLongPressFuncApp, mSearchLongPressFuncShortcut);
                            handled = true;
                        }
                    }
                } else {
                    // handle single tap
                    if (!mSearchKeyLongPressed && keyCode < KEYCODE_NONE) {
                        performAction(context, handler, keyCode, false, param.thisObject, mSearchFuncApp,
                                mSearchFuncShortcut);
                        handled = true;
                    }
                    if (!mInjectedKeyCode)
                        mSearchKeyLongPressed = false;
                    mInjectedKeyCode = false;
                }
                if (handled)
                    param.setResult(0);
                return;
            }
        }
    }

    private void performAction(Context context, Handler handler, int keyCode, boolean longPressed, Object thisObject,
                               String app, String shortcut) {
        if (longPressed && keyCode > 0) {
            injectKey(context, handler, keyCode);
        } else {
            switch (keyCode) {
                case KEYCODE_SWITCH_LAST_APP:
                    Utils.switchToLastApp(context, handler);
                    break;
                case KEYCODE_TAKE_SCREENSHOT:
                    Utils.takeScreenshot(context, handler);
                    break;
                case KEYCODE_SCREEN_OFF:
                    Utils.screenOff(context);
                    break;
                case KEYCODE_POWER_MENU:
                    Utils.powerMenu(thisObject, handler);
                    break;
                case KEYCODE_NOTIFICATION_PANEL:
                    Utils.expandNotificationsPanel(thisObject);
                    break;
                case KEYCODE_QUICK_SETTINGS_PANEL:
                    Utils.expandSettingsPanel(thisObject);
                    break;
                case KEYCODE_LAUNCH_APP:
                    Utils.launchApp(context, handler, app);
                    break;
                case KEYCODE_KILL_APP:
                    Utils.killForegroundApp(context, handler, thisObject);
                    break;
                case KEYCODE_LAUNCH_SHORTCUT:
                    Utils.launchShortcut(context, handler, shortcut);
                    break;
            }
        }

        if (longPressed && keyCode != KEYCODE_NONE)
            XposedHelpers.callMethod(thisObject, "performHapticFeedbackLw", new Class<?>[]{WindowState.class,
                    int.class, boolean.class}, null, HapticFeedbackConstants.LONG_PRESS, false);
    }

    /*
     * handle stock functions for long pressed event by injecting keycode
     * directly into input service
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void injectKey(final Context context, Handler handler, final int keyCode) {
        if (handler == null)
            return;

        mInjectedKeyCode = true;
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    final long eventTime = SystemClock.uptimeMillis();
                    final InputManager inputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
                    XposedHelpers.callMethod(inputManager, "injectInputEvent", new KeyEvent(eventTime - 50,
                            eventTime - 50, KeyEvent.ACTION_DOWN, keyCode, 0), 0);
                    XposedHelpers.callMethod(inputManager, "injectInputEvent", new KeyEvent(eventTime - 50,
                            eventTime - 25, KeyEvent.ACTION_UP, keyCode, 0), 0);
                } catch (Throwable t) {
                    XposedBridge.log(t);
                }
            }
        });
    }

    @Override
    public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
        if (!resparam.packageName.equals(CLASSNAME_SYSTEMUI))
            return;

        // module resources
        modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);

        mThemeIcons = new ThemeIcons();

        // load preferences from module package
        pref.reload();

        mCacheFolder = pref.getString("pref_cache_folder", null);
        mDensityDpi = pref.getInt("pref_density_dpi", -1);
        if (mCacheFolder != null && mDensityDpi > 0)
            mCustomButtons = new CustomButtons(mCacheFolder, mDensityDpi, CustomButtons.FILENAME_PREFIX);

        // backup stock button drawables
        mStockButtons.put("ic_sysbar_back", Utils.getDrawableBitmap(resparam.res, "ic_sysbar_back"));
        mStockButtons.put("ic_sysbar_home", Utils.getDrawableBitmap(resparam.res, "ic_sysbar_home"));
        mStockButtons.put("ic_sysbar_menu", Utils.getDrawableBitmap(resparam.res, "ic_sysbar_menu"));
        mStockButtons.put("ic_sysbar_recent", Utils.getDrawableBitmap(resparam.res, "ic_sysbar_recent"));

        // replace NavBar icons drawables, for theme only
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {

            boolean useTheme = pref.getBoolean("pref_usetheme", false);
            boolean useAltMenu = pref.getBoolean("pref_use_alt_menu", false);
            String themeId = pref.getString("pref_themeid", DEF_THEMEID);
            String themeColor = pref.getString("pref_themecolor", DEF_THEMECOLOR);

            if (useTheme) {
                if (mThemeIcons.getThemeExample(themeId) == -1) {
                    // user define theme
                    if (mCustomButtons != null) {
                        replaceCustomButton(resparam.res, "ic_sysbar_back", "Back", false, false);
                        replaceCustomButton(resparam.res, "ic_sysbar_back_ime", "Back", true, false);
                        replaceCustomButton(resparam.res, "ic_sysbar_back_land", "Back", false, true);
                        replaceCustomButton(resparam.res, "ic_sysbar_home", "Home", false, false);
                        replaceCustomButton(resparam.res, "ic_sysbar_home_land", "Home", false, true);
                        replaceCustomButton(resparam.res, "ic_sysbar_menu", "Menu", useAltMenu, false);
                        replaceCustomButton(resparam.res, "ic_sysbar_menu_land", "Menu", useAltMenu, true);
                        replaceCustomButton(resparam.res, "ic_sysbar_recent", "Recent", false, false);
                        replaceCustomButton(resparam.res, "ic_sysbar_recent_land", "Recent", false, true);

                        // HTC specific resources
                        try {
                            replaceCustomButton(resparam.res, "navigation_icon_back", "Back", false, false);
                            replaceCustomButton(resparam.res, "navigation_icon_home", "Home", false, false);
                            replaceCustomButton(resparam.res, "navigation_icon_menu", "Menu", useAltMenu, false);
                            replaceCustomButton(resparam.res, "navigation_icon_recent_apps", "Recent", false, false);
                        } catch (Exception e) {
                        }
                    }
                } else {
                    try {
                        resparam.res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", "ic_sysbar_back",
                                modRes.fwd(mThemeIcons.getIconResId(themeId, themeColor, "Back", false, false)));
                    } catch (Exception e) {
                        XposedBridge.log("Resource systemui:drawable/ic_sysbar_back not found");
                    }
                    try {
                        resparam.res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", "ic_sysbar_back_ime",
                                modRes.fwd(mThemeIcons.getIconResId(themeId, themeColor, "Back", true, false)));
                    } catch (Exception e) {
                        XposedBridge.log("Resource systemui:drawable/ic_sysbar_back_ime not found");
                    }
                    try {
                        resparam.res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", "ic_sysbar_back_land",
                                modRes.fwd(mThemeIcons.getIconResId(themeId, themeColor, "Back", false, true)));
                    } catch (Exception e) {
                        XposedBridge.log("Resource systemui:drawable/ic_sysbar_back_land not found");
                    }
                    try {
                        resparam.res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", "ic_sysbar_home",
                                modRes.fwd(mThemeIcons.getIconResId(themeId, themeColor, "Home", false, false)));
                    } catch (Exception e) {
                        XposedBridge.log("Resource systemui:drawable/ic_sysbar_home not found");
                    }
                    try {
                        resparam.res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", "ic_sysbar_home_land",
                                modRes.fwd(mThemeIcons.getIconResId(themeId, themeColor, "Home", false, true)));
                    } catch (Exception e) {
                        XposedBridge.log("Resource systemui:drawable/ic_sysbar_home_land not found");
                    }
                    try {
                        resparam.res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", "ic_sysbar_menu",
                                modRes.fwd(mThemeIcons.getIconResId(themeId, themeColor, "Menu", useAltMenu, false)));
                    } catch (Exception e) {
                        XposedBridge.log("Resource systemui:drawable/ic_sysbar_menu not found");
                    }
                    try {
                        resparam.res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", "ic_sysbar_menu_land",
                                modRes.fwd(mThemeIcons.getIconResId(themeId, themeColor, "Menu", useAltMenu, true)));
                    } catch (Exception e) {
                        XposedBridge.log("Resource systemui:drawable/ic_sysbar_menu_land not found");
                    }
                    try {
                        resparam.res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", "ic_sysbar_recent",
                                modRes.fwd(mThemeIcons.getIconResId(themeId, themeColor, "Recent", false, false)));
                    } catch (Exception e) {
                        XposedBridge.log("Resource systemui:drawable/ic_sysbar_recent not found");
                    }
                    try {
                        resparam.res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", "ic_sysbar_recent_land",
                                modRes.fwd(mThemeIcons.getIconResId(themeId, themeColor, "Recent", false, true)));
                    } catch (Exception e) {
                        XposedBridge.log("Resource systemui:drawable/ic_sysbar_recent_land not found");
                    }

                    // HTC specific resources
                    try {
                        resparam.res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", "navigation_icon_back",
                                modRes.fwd(mThemeIcons.getIconResId(themeId, themeColor, "Back", false, false)));
                        resparam.res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", "navigation_icon_home",
                                modRes.fwd(mThemeIcons.getIconResId(themeId, themeColor, "Home", false, false)));
                        resparam.res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", "navigation_icon_menu",
                                modRes.fwd(mThemeIcons.getIconResId(themeId, themeColor, "Menu", useAltMenu, false)));
                        resparam.res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", "navigation_icon_recent_apps",
                                modRes.fwd(mThemeIcons.getIconResId(themeId, themeColor, "Recent", false, false)));
                    } catch (Exception e) {
                    }
                }
            }

            // replace NavBar layout
            resparam.res.hookLayout(CLASSNAME_SYSTEMUI, "layout", "navigation_bar", new XC_LayoutInflated() {
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
                @SuppressLint("DefaultLocale")
                @Override
                public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                    int buttonsCount;
//                    int screenWidth = 0;
//                    int screenWidthLand = 0;
                    int buttonWidth0;
                    int buttonWidth90;

                    pref.reload();
                    boolean useTheme = pref.getBoolean("pref_usetheme", false);
                    String themeId = pref.getString("pref_themeid", DEF_THEMEID);
                    String themeColor = pref.getString("pref_themecolor", DEF_THEMECOLOR);
                    String[] orderList = pref.getString("pref_order", DEF_BUTTONS_ORDER_LIST).split(",");
                    buttonsCount = orderList.length;
                    int leftMargin = pref.getInt("pref_left_margin", 0);
                    int rightMargin = pref.getInt("pref_right_margin", 0);
                    int leftMargin90 = leftMargin;
                    int rightMargin90 = rightMargin;
                    int searchKeycode = Integer.parseInt(pref.getString("pref_search_function",
                            String.valueOf(KeyEvent.KEYCODE_SEARCH)));

                    mShowSearch = pref.getBoolean("pref_show_search", true);
                    mShowMenu = pref.getBoolean("pref_show_menu", true);
                    mShowRecent = pref.getBoolean("pref_show_recent", true);
                    mContext = liparam.view.getContext();

                    initScreenConfig(mContext);

                    // adjust left and right margins for alternate orientation
                    leftMargin90 = Math.round((float) leftMargin * (float) mScreenWidthLand / (float) mScreenWidth);
                    rightMargin90 = Math.round((float) rightMargin * (float) mScreenWidthLand / (float) mScreenWidth);

                    int extraKeyWidth = 0;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        extraKeyWidth = mContext.getResources().getDimensionPixelSize(
                                liparam.res.getIdentifier("navigation_extra_key_width", "dimen", CLASSNAME_SYSTEMUI));

                    FrameLayout rot0 = (FrameLayout) liparam.view.findViewById(liparam.res.getIdentifier("rot0", "id",
                            CLASSNAME_SYSTEMUI));
                    FrameLayout rot90 = (FrameLayout) liparam.view.findViewById(liparam.res.getIdentifier("rot90", "id",
                            CLASSNAME_SYSTEMUI));
                    LinearLayout rot0NavButtons = null;
                    LinearLayout rot0LightsOut = null;
                    LinearLayout rot0LightsOutHigh = null;
                    LinearLayout rot90NavButtons = null;
                    LinearLayout rot90LightsOut = null;
                    LinearLayout rot90LightsOutHigh = null;

                    if (rot0 != null) {
                        rot0NavButtons = (LinearLayout) rot0.findViewById(liparam.res.getIdentifier("nav_buttons", "id",
                                CLASSNAME_SYSTEMUI));
                        rot0LightsOut = (LinearLayout) rot0.findViewById(liparam.res.getIdentifier("lights_out", "id",
                                CLASSNAME_SYSTEMUI));
                        rot0LightsOutHigh = (LinearLayout) rot0.findViewById(liparam.res.getIdentifier("lights_out_high",
                                "id", CLASSNAME_SYSTEMUI));
                    }

                    if (rot90 != null) {
                        rot90NavButtons = (LinearLayout) rot90.findViewById(liparam.res.getIdentifier("nav_buttons", "id",
                                CLASSNAME_SYSTEMUI));
                        rot90LightsOut = (LinearLayout) rot90.findViewById(liparam.res.getIdentifier("lights_out", "id",
                                CLASSNAME_SYSTEMUI));
                        rot90LightsOutHigh = (LinearLayout) rot90.findViewById(liparam.res.getIdentifier("lights_out_high",
                                "id", CLASSNAME_SYSTEMUI));
                    }

                    // determine if a tablet is in use
                    boolean tabletMode = false;
                    if (rot90NavButtons != null)
                        tabletMode = rot90NavButtons.getOrientation() == LinearLayout.HORIZONTAL;

                    // separator definition
                    boolean showSeparator = pref.getBoolean("pref_show_separator", false);
                    int separatorWidthFactor = showSeparator ? pref.getInt("pref_separator_width", 0) : 0;
                    int separatorWidth0 = Math.round((float) mScreenWidth * (float) separatorWidthFactor / 100);
                    int separatorWidth90 = Math.round((float) mScreenWidthLand * (float) separatorWidthFactor / 100);
                    int actualButtonsCount = showSeparator ? buttonsCount - 1 : buttonsCount;

                    if (!mShowMenu && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        buttonWidth0 = Math
                                .round((float) (mScreenWidth - leftMargin - rightMargin - extraKeyWidth * 2 - separatorWidth0)
                                        / (float) actualButtonsCount);
                        buttonWidth90 = tabletMode ? Math.round((float) (mScreenWidthLand - leftMargin90 - rightMargin90
                                - extraKeyWidth * 2 - separatorWidth90)
                                / (float) actualButtonsCount) : buttonWidth0;
                    } else {
                        buttonWidth0 = Math.round((float) (mScreenWidth - leftMargin - rightMargin - separatorWidth0)
                                / (float) actualButtonsCount);
                        buttonWidth90 = tabletMode ? Math
                                .round((float) (mScreenWidthLand - leftMargin90 - rightMargin90 - separatorWidth90)
                                        / (float) actualButtonsCount) : buttonWidth0;
                    }

                    // reset margin if value larger than screen width
                    int maxWidth = (int) (mScreenWidth * 0.75);
                    if (leftMargin > maxWidth || rightMargin > maxWidth) {
                        leftMargin = 0;
                        rightMargin = 0;
                    }
                    XposedBridge.log(String
                            .format("screenWidth:%d, screenWidthLand:%d, buttonWidth0:%d, buttonWidth90:%d, leftMargin:%d, rightMargin:%d, leftMargin90:%d, rightMargin90:%d, extraKeyWidth:%d, separatorWidthFactor:%d",
                                    mScreenWidth, mScreenWidthLand, buttonWidth0, buttonWidth90, leftMargin, rightMargin,
                                    leftMargin90, rightMargin90, extraKeyWidth, separatorWidthFactor));

                    // write stock button images to cache folder
                    XposedBridge.log(String.format("Stock button count:%d, ExternalCacheDir:%s", mStockButtons.size(),
                            mContext.getExternalCacheDir()));
                    try {
                        if (mStockButtons.size() > 0) {
                            Iterator<Entry<String, Bitmap>> it = mStockButtons.entrySet().iterator();
                            while (it.hasNext()) {
                                Map.Entry<String, Bitmap> vp = it.next();
                                Bitmap bitmap = vp.getValue();
                                if (bitmap != null) {
                                    Utils.saveBitmapAsFile(mContext.getExternalCacheDir(), vp.getKey() + ".png", bitmap);
                                    bitmap.recycle();
                                    bitmap = null;
                                }
                                it.remove();
                            }
                        }
                    } catch (Throwable t) {
                        if (t.getMessage() == null)
                            XposedBridge.log(t);
                        else
                            XposedBridge.log(t.getMessage());
                    }

                    // portrait views
                    if (rot0 != null) {

                        // handle nav buttions
                        if (rot0NavButtons != null) {
                            rot0NavButtons.setGravity(Gravity.CENTER);
                            Map<String, ImageView> viewList = new HashMap<String, ImageView>();

                            // collection of existing button objects
                            viewList.put("Home", (ImageView) rot0NavButtons.findViewById(liparam.res.getIdentifier("home",
                                    "id", CLASSNAME_SYSTEMUI)));
                            viewList.put("Back", (ImageView) rot0NavButtons.findViewById(liparam.res.getIdentifier("back",
                                    "id", CLASSNAME_SYSTEMUI)));
                            viewList.put("Recent", (ImageView) rot0NavButtons.findViewById(liparam.res.getIdentifier(
                                    "recent_apps", "id", CLASSNAME_SYSTEMUI)));

                            ImageView menuView = (ImageView) rot0NavButtons.findViewById(liparam.res.getIdentifier("menu",
                                    "id", CLASSNAME_SYSTEMUI));
                            viewList.put("Menu", menuView);
                            // for Lollipop, container for menu and ime switcher
                            FrameLayout menuParent = null;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                // detach menu button from parent frame layout
                                FrameLayout fl = (FrameLayout) menuView.getParent();
                                if (fl != null)
                                    fl.removeView(menuView);

                                menuParent = new FrameLayout(mContext);
                                menuParent.setLayoutParams(new FrameLayout.LayoutParams(mShowMenu ? buttonWidth0
                                        : extraKeyWidth, FrameLayout.LayoutParams.MATCH_PARENT));

                                if (mShowMenu)
                                    menuParent.addView(menuView);

                                // add IME switcher button as well
                                ImageView imeView = (ImageView) rot0NavButtons.findViewById(liparam.res.getIdentifier(
                                        "ime_switcher", "id", CLASSNAME_SYSTEMUI));
                                // detach IME button from parent frame layout
                                fl = (FrameLayout) imeView.getParent();
                                if (fl != null)
                                    fl.removeView(imeView);

                                imeView.setVisibility(View.INVISIBLE);
                                imeView.setLayoutParams(new FrameLayout.LayoutParams(mShowMenu ? buttonWidth0
                                        : extraKeyWidth, FrameLayout.LayoutParams.MATCH_PARENT));
                                imeView.setPadding(0, 0, 0, 0);
                                // imeView.setScaleType(ScaleType.FIT_CENTER);
                                imeView.setScaleType(Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP ? ScaleType.FIT_CENTER
                                        : ScaleType.CENTER);
                                menuParent.addView(imeView);
                            }

                            if (mShowSearch) {
                                Drawable searchButtonDrawable = getButtonDrawable(mThemeIcons, mCustomButtons, useTheme, themeId, themeColor, false, false, "Search");
                                viewList.put(
                                        "Search",
                                        createButtonView(mContext, liparam, buttonWidth0, LinearLayout.LayoutParams.MATCH_PARENT,
                                                "ic_sysbar_highlight", searchButtonDrawable, searchKeycode, "Search", -1, true));
                            }

                            rot0NavButtons.removeAllViews();
                            // left margin
                            if (leftMargin > 0)
                                rot0NavButtons.addView(getPlaceholderView(mContext, leftMargin, LinearLayout.LayoutParams.MATCH_PARENT));

                            // for Lollipop, add extra padding if menu button is
                            // disabled
                            if (!mShowMenu && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                                rot0NavButtons.addView(getPlaceholderView(mContext, extraKeyWidth, LinearLayout.LayoutParams.MATCH_PARENT));

                            // add selected buttons
                            for (int i = 0; i < buttonsCount; i++) {

                                if (orderList[i].equals("Separator")) {
                                    rot0NavButtons.addView(getPlaceholderView(mContext, separatorWidth0, LinearLayout.LayoutParams.MATCH_PARENT));
                                } else {
                                    ImageView view = viewList.remove(orderList[i]);
                                    if (view != null) {
                                        view.setVisibility(View.VISIBLE);
                                        view.setPadding(0, 0, 0, 0);
                                        // view.setScaleType(ScaleType.FIT_CENTER);
                                        view.setScaleType(Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP ? ScaleType.FIT_CENTER
                                                : ScaleType.CENTER);

                                        if (orderList[i].equals("Menu")
                                                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            view.setLayoutParams(new FrameLayout.LayoutParams(buttonWidth0,
                                                    FrameLayout.LayoutParams.MATCH_PARENT));
                                            rot0NavButtons.addView(menuParent);
                                        } else {
                                            view.setLayoutParams(new LinearLayout.LayoutParams(buttonWidth0,
                                                    LinearLayout.LayoutParams.MATCH_PARENT, 0.0f));
                                            rot0NavButtons.addView(view);
                                        }
                                    }
                                }
                            }
                            // for Lollipop, add IME switcher to original position
                            // if menu button is disabled
                            if (!mShowMenu && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                                rot0NavButtons.addView(menuParent);

                            // right margin
                            if (rightMargin > 0)
                                rot0NavButtons.addView(getPlaceholderView(mContext, rightMargin, LinearLayout.LayoutParams.MATCH_PARENT));

                            // add unselected buttons and make them invisible
                            for (ImageView view : viewList.values()) {
                                view.setVisibility(View.INVISIBLE);
                                view.setLayoutParams(new LinearLayout.LayoutParams(0,
                                        LinearLayout.LayoutParams.MATCH_PARENT, 0.0f));
                                view.setPadding(0, 0, 0, 0);
                                view.setScaleType(ScaleType.CENTER);
                                rot0NavButtons.addView(view);
                            }
                        }

                        // handle lights out
                        if (rot0LightsOut != null) {
                            rot0LightsOut.setGravity(Gravity.CENTER);
                            rot0LightsOut.removeAllViews();

                            // left margin
                            if (leftMargin > 0)
                                rot0LightsOut.addView(getPlaceholderView(mContext, leftMargin, LinearLayout.LayoutParams.MATCH_PARENT));

                            int i = 0;
                            while (i < actualButtonsCount) {
                                rot0LightsOut.addView(createLightsOutView(liparam, buttonWidth0,
                                        LinearLayout.LayoutParams.MATCH_PARENT, "ic_sysbar_lights_out_dot_small"));
                                i++;
                            }

                            // right margin
                            if (rightMargin > 0)
                                rot0LightsOut.addView(getPlaceholderView(mContext, rightMargin, LinearLayout.LayoutParams.MATCH_PARENT));
                        }

                        // handle lights out high
                        if (rot0LightsOutHigh != null) {
                            rot0LightsOutHigh.setGravity(Gravity.CENTER);
                            rot0LightsOutHigh.removeAllViews();

                            // left margin
                            if (leftMargin > 0)
                                rot0LightsOutHigh.addView(getPlaceholderView(mContext, leftMargin, LinearLayout.LayoutParams.MATCH_PARENT));

                            int i = 0;
                            while (i < actualButtonsCount) {
                                rot0LightsOutHigh.addView(createLightsOutView(liparam, buttonWidth0,
                                        LinearLayout.LayoutParams.MATCH_PARENT, "ic_sysbar_lights_out_dot_small_high"));
                                i++;
                            }

                            // right margin
                            if (rightMargin > 0)
                                rot0LightsOutHigh.addView(getPlaceholderView(mContext, rightMargin, LinearLayout.LayoutParams.MATCH_PARENT));
                        }
                    }

                    // landscape views
                    if (rot90 != null) {

                        // handle nav buttions
                        if (rot90NavButtons != null) {

                            Map<String, ImageView> viewList = new HashMap<String, ImageView>();

                            // collection of existing button objects
                            viewList.put("Back", (ImageView) rot90NavButtons.findViewById(liparam.res.getIdentifier("back",
                                    "id", CLASSNAME_SYSTEMUI)));
                            viewList.put("Home", (ImageView) rot90NavButtons.findViewById(liparam.res.getIdentifier("home",
                                    "id", CLASSNAME_SYSTEMUI)));
                            viewList.put("Recent", (ImageView) rot90NavButtons.findViewById(liparam.res.getIdentifier(
                                    "recent_apps", "id", CLASSNAME_SYSTEMUI)));

                            ImageView menuView = (ImageView) rot90NavButtons.findViewById(liparam.res.getIdentifier("menu",
                                    "id", CLASSNAME_SYSTEMUI));
                            viewList.put("Menu", menuView);
                            // for Lollipop, container for menu and ime switcher
                            FrameLayout menuParent = null;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                // detach menu button from parent frame layout
                                FrameLayout fl = (FrameLayout) menuView.getParent();
                                if (fl != null)
                                    fl.removeView(menuView);

                                menuParent = new FrameLayout(mContext);
                                if (tabletMode)
                                    menuParent.setLayoutParams(new FrameLayout.LayoutParams(mShowMenu ? buttonWidth90
                                            : extraKeyWidth, FrameLayout.LayoutParams.MATCH_PARENT));
                                else
                                    menuParent.setLayoutParams(new FrameLayout.LayoutParams(
                                            FrameLayout.LayoutParams.MATCH_PARENT, mShowMenu ? buttonWidth90
                                            : extraKeyWidth));

                                if (mShowMenu)
                                    menuParent.addView(menuView);

                                // add IME switcher button as well
                                ImageView imeView = (ImageView) rot90NavButtons.findViewById(liparam.res.getIdentifier(
                                        "ime_switcher", "id", CLASSNAME_SYSTEMUI));
                                // detach IME button from parent frame layout
                                fl = (FrameLayout) imeView.getParent();
                                if (fl != null)
                                    fl.removeView(imeView);

                                imeView.setVisibility(View.INVISIBLE);
                                if (tabletMode)
                                    imeView.setLayoutParams(new FrameLayout.LayoutParams(mShowMenu ? buttonWidth90
                                            : extraKeyWidth, FrameLayout.LayoutParams.MATCH_PARENT));
                                else
                                    imeView.setLayoutParams(new FrameLayout.LayoutParams(
                                            FrameLayout.LayoutParams.MATCH_PARENT, mShowMenu ? buttonWidth90
                                            : extraKeyWidth));
                                imeView.setPadding(0, 0, 0, 0);
                                // imeView.setScaleType(ScaleType.FIT_CENTER);
                                imeView.setScaleType(Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP ? ScaleType.FIT_CENTER
                                        : ScaleType.CENTER);
                                menuParent.addView(imeView);
                            }

                            if (mShowSearch) {
                                Drawable searchButtonDrawable = getButtonDrawable(mThemeIcons, mCustomButtons, useTheme, themeId, themeColor, !tabletMode, false, "Search");
                                viewList.put(
                                        "Search",
                                        createButtonView(mContext, liparam, buttonWidth90, LinearLayout.LayoutParams.MATCH_PARENT,
                                                tabletMode ? "ic_sysbar_highlight" : "ic_sysbar_highlight_land",
                                                searchButtonDrawable, searchKeycode, "Search", -1, true));
                            }

                            rot90NavButtons.removeAllViews();
                            // add selected buttons
                            if (tabletMode) { // navbar is positioned horizontally
                                final int extraPadding = Math
                                        .round((float) (mScreenWidthLand - leftMargin90 - rightMargin90 - buttonWidth90
                                                * actualButtonsCount - (mShowMenu ? 0 : extraKeyWidth * 2) - separatorWidth90) / 2f);
                                // for Lollipop, add extra padding if menu button is
                                // disabled
                                if (!mShowMenu && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                                    rot90NavButtons.addView(getPlaceholderView(mContext, extraKeyWidth, LinearLayout.LayoutParams.MATCH_PARENT));

                                // left margin + extra padding
                                if (leftMargin90 + extraPadding > 0)
                                    rot90NavButtons.addView(getPlaceholderView(mContext, leftMargin90 + extraPadding, LinearLayout.LayoutParams.MATCH_PARENT));

                                for (int i = 0; i < buttonsCount; i++) {
                                    if (orderList[i].equals("Separator")) {
                                        rot90NavButtons.addView(getPlaceholderView(mContext, separatorWidth90, LinearLayout.LayoutParams.MATCH_PARENT));
                                    } else {
                                        ImageView view = viewList.remove(orderList[i]);

                                        if (view != null) {
                                            view.setVisibility(View.VISIBLE);
                                            view.setPadding(0, 0, 0, 0);
                                            // view.setScaleType(ScaleType.FIT_CENTER);
                                            view.setScaleType(Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP ? ScaleType.FIT_CENTER
                                                    : ScaleType.CENTER);

                                            if (orderList[i].equals("Menu")
                                                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                                view.setLayoutParams(new FrameLayout.LayoutParams(buttonWidth90,
                                                        FrameLayout.LayoutParams.MATCH_PARENT));
                                                rot90NavButtons.addView(menuParent);
                                            } else {
                                                view.setLayoutParams(new LinearLayout.LayoutParams(buttonWidth90,
                                                        LinearLayout.LayoutParams.MATCH_PARENT, 0.0f));
                                                rot90NavButtons.addView(view);
                                            }
                                        }
                                    }
                                }

                                // right margin + extra padding
                                if (rightMargin90 + extraPadding > 0)
                                    rot90NavButtons.addView(getPlaceholderView(mContext, rightMargin90 + extraPadding,
                                            LinearLayout.LayoutParams.MATCH_PARENT));

                                // for Lollipop, add IME switcher to original
                                // position if menu button is disabled
                                if (!mShowMenu && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                                    rot90NavButtons.addView(menuParent);
                            } else { // navbar is positioned vertically
                                // for Lollipop, add IME switcher to original
                                // position if menu button is disabled
                                if (!mShowMenu && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                                    rot90NavButtons.addView(menuParent);

                                // top margin
                                if (leftMargin > 0)
                                    rot90NavButtons.addView(getPlaceholderView(mContext, LinearLayout.LayoutParams.MATCH_PARENT, leftMargin));

                                for (int i = buttonsCount - 1; i >= 0; i--) {
                                    if (orderList[i].equals("Separator")) {
                                        rot90NavButtons.addView(getPlaceholderView(mContext, LinearLayout.LayoutParams.MATCH_PARENT, separatorWidth0));
                                    } else {
                                        ImageView view = viewList.remove(orderList[i]);
                                        if (view != null) {
                                            view.setVisibility(View.VISIBLE);
                                            view.setPadding(0, 0, 0, 0);
                                            // view.setScaleType(ScaleType.FIT_CENTER);
                                            view.setScaleType(Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP ? ScaleType.FIT_CENTER
                                                    : ScaleType.CENTER);

                                            if (orderList[i].equals("Menu")
                                                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                                view.setLayoutParams(new FrameLayout.LayoutParams(
                                                        FrameLayout.LayoutParams.MATCH_PARENT, buttonWidth90));
                                                rot90NavButtons.addView(menuParent);
                                            } else {
                                                view.setLayoutParams(new LinearLayout.LayoutParams(
                                                        LinearLayout.LayoutParams.MATCH_PARENT, buttonWidth90, 0.0f));
                                                rot90NavButtons.addView(view);
                                            }
                                        }
                                    }
                                }

                                // bottom margin
                                if (rightMargin > 0)
                                    rot90NavButtons.addView(getPlaceholderView(mContext, LinearLayout.LayoutParams.MATCH_PARENT, rightMargin));

                                // for Lollipop, add extra padding if menu button is
                                // disabled
                                if (!mShowMenu && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                                    rot90NavButtons.addView(getPlaceholderView(mContext, LinearLayout.LayoutParams.MATCH_PARENT, extraKeyWidth));
                            }

                            // add unselected buttons and make them invisible
                            for (ImageView view : viewList.values()) {
                                view.setVisibility(View.INVISIBLE);
                                if (tabletMode)
                                    view.setLayoutParams(new LinearLayout.LayoutParams(0,
                                            LinearLayout.LayoutParams.MATCH_PARENT, 0.0f));
                                else
                                    view.setLayoutParams(new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.0f));
                                view.setPadding(0, 0, 0, 0);
                                view.setScaleType(ScaleType.CENTER);
                                rot90NavButtons.addView(view);
                            }
                        }

                        // handle lights out
                        if (rot90LightsOut != null) {
                            rot90LightsOut.setGravity(Gravity.CENTER);
                            rot90LightsOut.removeAllViews();

                            // left margin
                            if (leftMargin > 0)
                                rot90LightsOut.addView(getPlaceholderView(mContext, LinearLayout.LayoutParams.MATCH_PARENT, leftMargin));

                            int i = 0;
                            while (i < actualButtonsCount) {
                                rot90LightsOut.addView(createLightsOutView(liparam, LinearLayout.LayoutParams.MATCH_PARENT,
                                        buttonWidth90, "ic_sysbar_lights_out_dot_small"));
                                i++;
                            }

                            // right margin
                            if (rightMargin > 0)
                                rot90LightsOut.addView(getPlaceholderView(mContext, LinearLayout.LayoutParams.MATCH_PARENT, rightMargin));
                        }

                        // handle lights out high
                        if (rot90LightsOutHigh != null) {
                            rot90LightsOutHigh.setGravity(Gravity.CENTER);
                            rot90LightsOutHigh.removeAllViews();

                            // left margin
                            if (leftMargin > 0)
                                rot90LightsOutHigh.addView(getPlaceholderView(mContext, LinearLayout.LayoutParams.MATCH_PARENT, leftMargin));

                            int i = 0;
                            while (i < actualButtonsCount) {
                                rot90LightsOutHigh.addView(createLightsOutView(liparam,
                                        LinearLayout.LayoutParams.MATCH_PARENT, buttonWidth90,
                                        "ic_sysbar_lights_out_dot_small_high"));
                                i++;
                            }

                            // right margin
                            if (rightMargin > 0)
                                rot90LightsOutHigh.addView(getPlaceholderView(mContext, LinearLayout.LayoutParams.MATCH_PARENT, rightMargin));
                        }
                    }

                    // XposedBridge.log(String.format("rot0:%b, rot0NavButtons:%b, rot0LightsOut:%b, rot0LightsOutHigh:%b",
                    // rot0 != null, rot0NavButtons != null,
                    // rot0LightsOut != null, rot0LightsOutHigh != null));
                    // XposedBridge.log(String.format("rot90:%b, rot90NavButtons:%b, rot90LightsOut:%b, rot90LightsOutHigh:%b",
                    // rot90 != null,
                    // rot90NavButtons != null, rot90LightsOut != null,
                    // rot90LightsOutHigh != null));
                }
            });
        }
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(CLASSNAME_SYSTEMUI) && !lpparam.packageName.equals(PKGNAME_SYSTEM_SERVICE))
            return;

        if (lpparam.packageName.equals(CLASSNAME_SYSTEMUI)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    XposedHelpers.findAndHookConstructor(CLASSNAME_NAVIGATIONBARINFLATERVIEW, lpparam.classLoader,
                            Context.class, AttributeSet.class, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if (mNavigationBarInflaterView == null)
                                        mNavigationBarInflaterView = (View) param.thisObject;
                                }
                            });
                } catch (NoSuchMethodError e) {
                    XposedBridge.log("NavigationBarInflaterView constructor not found");
                    return;
                }

                try {
                    XposedHelpers.findAndHookMethod(CLASSNAME_NAVIGATIONBARINFLATERVIEW, lpparam.classLoader, "getDefaultLayout", new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            if (!mSystemBootCompleted)
                                return getOrderListForN(DEF_STOCK_BUTTONS_ORDER_LIST, false);

                            if (mContext == null)
                                mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                            SharedPreferences remotePref = new RemotePreferences(mContext, Utils.PREF_AUTHORITY, Utils.PREF_NAME);
                            boolean showMenu = remotePref.getBoolean("pref_show_menu", true);
                            String orderList = getOrderListForN(remotePref.getString("pref_order", DEF_BUTTONS_ORDER_LIST), showMenu);
                            XposedBridge.log("orderList: " + orderList);
                            return orderList;
                        }
                    });
                } catch (NoSuchMethodError e) {
                    XposedBridge.log("getDefaultLayout not found");
                    return;
                }

                try {
                    XposedHelpers.findAndHookMethod(CLASSNAME_NAVIGATIONBARINFLATERVIEW, lpparam.classLoader, "inflateLayout", String.class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            if (mContext == null)
                                mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                            SharedPreferences remotePref = new RemotePreferences(mContext, Utils.PREF_AUTHORITY, Utils.PREF_NAME);
                            boolean showMenu = remotePref.getBoolean("pref_show_menu", true);
                            boolean showSeparator = remotePref.getBoolean("pref_show_separator", false);
                            int separatorWidthFactor = showSeparator ? remotePref.getInt("pref_separator_width", 0) : 0;
                            int placeholderCount = showSeparator ? 5 : 4;   // left and right margins & IME padding are counted by default
                            int leftMargin = remotePref.getInt("pref_left_margin", 0);
                            int rightMargin = remotePref.getInt("pref_right_margin", 0);
                            int leftMarginLand = leftMargin;
                            int rightMarginLand = rightMargin;
                            int imePadding = showMenu ? 0 :
                                    mContext.getResources().getDimensionPixelSize(
                                            mContext.getResources().getIdentifier("navigation_extra_key_width", "dimen", CLASSNAME_SYSTEMUI));

                            initScreenConfig(mContext);

                            FrameLayout mRot0 = (FrameLayout) XposedHelpers.getObjectField(param.thisObject, "mRot0");
                            FrameLayout mRot90 = (FrameLayout) XposedHelpers.getObjectField(param.thisObject, "mRot90");
                            int endsGroupId = mContext.getResources().getIdentifier("ends_group", "id", CLASSNAME_SYSTEMUI);
                            int centerGroupId = mContext.getResources().getIdentifier("center_group", "id", CLASSNAME_SYSTEMUI);
                            ViewGroup endsGroup0 = (ViewGroup) mRot0.findViewById(endsGroupId);
                            ViewGroup endsGroup90 = (ViewGroup) mRot90.findViewById(endsGroupId);
                            ViewGroup centerGroup0 = (ViewGroup) mRot0.findViewById(centerGroupId);
                            ViewGroup centerGroup90 = (ViewGroup) mRot90.findViewById(centerGroupId);

                            int orientation0 = ((LinearLayout) endsGroup0).getOrientation();
                            int orientation90 = ((LinearLayout) endsGroup90).getOrientation();
                            boolean tableMode = orientation90 == LinearLayout.HORIZONTAL;
//                            XposedBridge.log(String.format("orientation0:%d, orientation90:%d", orientation0, orientation90));

                            // adjust left and right margins for alternate orientation
                            if (tableMode) {
                                leftMarginLand = Math.round((float) leftMargin * (float) mScreenWidthLand / (float) mScreenWidth);
                                rightMarginLand = Math.round((float) rightMargin * (float) mScreenWidthLand / (float) mScreenWidth);
                            }

                            // reset margin if value larger than screen width
                            int maxWidth = (int) (mScreenWidth * 0.75);
                            if (leftMargin > maxWidth || rightMargin > maxWidth) {
                                leftMargin = 0;
                                rightMargin = 0;
                            }

                            XposedBridge.log(String
                                    .format("rotation:%d, screenWidth:%d, screenWidthLand:%d, leftMargin:%d, rightMargin:%d, leftMarginLand:%d, rightMarginLand:%d, separatorWidthFactor:%d, imePadding:%d",
                                            mRotation, mScreenWidth, mScreenWidthLand, leftMargin, rightMargin,
                                            leftMarginLand, rightMarginLand, separatorWidthFactor, imePadding));

                            String newLayout = (String) param.args[0];
                            XposedHelpers.setObjectField(param.thisObject, "mCurrentLayout", newLayout);
                            if (newLayout == null)
                                newLayout = (String) XposedHelpers.callMethod(param.thisObject, "getDefaultLayout");

                            String[] sets = newLayout.split(GRAVITY_SEPARATOR);
//                            XposedBridge.log("newLayout: " + newLayout);
//                            XposedBridge.log(String.format("endsGroup0.getChildCount: %d endsGroup90.getChildCount:%d", endsGroup0.getChildCount(), endsGroup90.getChildCount()));
                            for (int i = 0; i < sets.length; i++) {
                                String[] section = sets[i].split(BUTTON_SEPARATOR);

                                XposedHelpers.callMethod(param.thisObject, "inflateButtons", section, endsGroup0, false);
                                XposedHelpers.callMethod(param.thisObject, "inflateButtons", section, endsGroup90, true);
                            }
                            final int realButtonCount = endsGroup0.getChildCount() - placeholderCount;
//                            XposedBridge.log(String.format("endsGroup0.getChildCount: %d realButtonCount:%d", endsGroup0.getChildCount(), realButtonCount));

                            final int separatorWidth = Math.round((float) mScreenWidth * (float) separatorWidthFactor / 100);
                            final int separatorWidthLand = Math.round((tableMode ? (float) mScreenWidthLand : (float) mScreenWidth) * (float) separatorWidthFactor / 100);
                            mButtonWidth = Math.round((mScreenWidth - separatorWidth - leftMargin - rightMargin - imePadding * 2) / realButtonCount);
                            mButtonWidthLand = Math.round((tableMode
                                    ? (mScreenWidthLand - separatorWidthLand - leftMarginLand - rightMarginLand - imePadding * 2)
                                    : (mScreenWidth - separatorWidth - leftMargin - rightMargin - imePadding * 2)) / realButtonCount);
//                            XposedBridge.log(String.format("separatorWidth:%d, mButtonWidth:%d", separatorWidth, mButtonWidth));
//                            XposedBridge.log(String.format("separatorWidthLand:%d, mButtonWidthLand:%d", separatorWidthLand, mButtonWidthLand));

                            // resize buttons
                            for (int i = 0; i < endsGroup0.getChildCount(); i++) {
                                View view = endsGroup0.getChildAt(i);
                                View view90 = endsGroup90.getChildAt(i);

//                                XposedBridge.log(String.format("view i:%d id:%d tag:%s", i, view.getId(), view.getTag()));
//                                XposedBridge.log(String.format("view90 i:%d id:%d tag:%s", i, view90.getId(), view90.getTag()));
                                if (KEY_LEFT_IME_PADDING_N.equals(view.getTag())) {
                                    view.getLayoutParams().width = imePadding;
                                    if (tableMode)
                                        view90.getLayoutParams().width = imePadding;
                                    else
                                        view90.getLayoutParams().height = imePadding;
                                } else if (KEY_LEFT_MARGIN_N.equals(view.getTag())) {
                                    view.getLayoutParams().width = leftMargin;
                                    if (tableMode)
                                        view90.getLayoutParams().width = leftMarginLand;
                                    else
                                        view90.getLayoutParams().height = leftMarginLand;
                                } else if (KEY_RIGHT_MARGIN_N.equals(view.getTag())) {
                                    view.getLayoutParams().width = rightMargin;
                                    if (tableMode)
                                        view90.getLayoutParams().width = rightMarginLand;
                                    else
                                        view90.getLayoutParams().height = rightMarginLand;
                                } else if (KEY_SEPARATOR_N.equals(view.getTag())) {
                                    view.getLayoutParams().width = separatorWidth;
                                    if (tableMode)
                                        view90.getLayoutParams().width = separatorWidthLand;
                                    else
                                        view90.getLayoutParams().height = separatorWidthLand;
                                } else if (KEY_MENU_N.equals(view.getTag())) {
                                    int menuWidth = showMenu ? mButtonWidth : imePadding;
                                    int menuWidthLand = showMenu ? mButtonWidthLand : imePadding;

                                    view.getLayoutParams().width = menuWidth;
                                    if (tableMode) {
                                        view90.getLayoutParams().width = menuWidthLand;
                                        view90.getLayoutParams().height = LinearLayout.LayoutParams.MATCH_PARENT;
                                    } else {
                                        view90.getLayoutParams().height = menuWidthLand;
                                        view90.getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
                                    }

                                    for (int j = 0; j < ((FrameLayout) view).getChildCount(); j++) {
                                        ImageView iv = (ImageView) ((FrameLayout) view).getChildAt(j);
                                        iv.getLayoutParams().width = menuWidth;
                                        ImageView iv90 = (ImageView) ((FrameLayout) view90).getChildAt(j);
                                        if (tableMode) {
                                            iv90.getLayoutParams().width = menuWidthLand;
                                            iv90.getLayoutParams().height = LinearLayout.LayoutParams.MATCH_PARENT;
                                        } else {
                                            iv90.getLayoutParams().height = menuWidthLand;
                                            iv90.getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
                                        }
                                    }
                                } else {
                                    view.getLayoutParams().width = mButtonWidth;
                                    if (tableMode) {
                                        view90.getLayoutParams().width = mButtonWidthLand;
                                        view90.getLayoutParams().height = LinearLayout.LayoutParams.MATCH_PARENT;
                                    } else {
                                        view90.getLayoutParams().height = mButtonWidthLand;
                                        view90.getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
                                    }
                                }
                            }

                            return null;
                        }
                    });
                } catch (NoSuchMethodError e) {
                    XposedBridge.log("inflateLayout not found");
                    return;
                }

                try {
                    XposedHelpers.findAndHookMethod(CLASSNAME_NAVIGATIONBARINFLATERVIEW, lpparam.classLoader, "inflateButton",
                            String.class, ViewGroup.class, boolean.class, int.class, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    inflateButtonN(param);
                                }
                            });
                } catch (NoSuchMethodError e) {
                    try {
                        XposedHelpers.findAndHookMethod(CLASSNAME_NAVIGATIONBARINFLATERVIEW, lpparam.classLoader, "inflateButton",
                                String.class, ViewGroup.class, boolean.class, new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        inflateButtonN(param);
                                    }
                                });
                    } catch (NoSuchMethodError e1) {
                        XposedBridge.log("inflateButton not found");
                        return;
                    }
                }

                try {
                    XposedHelpers.findAndHookConstructor(CLASSNAME_NAVIGATIONBARVIEW, lpparam.classLoader,
                            Context.class, AttributeSet.class, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if (mContext == null)
                                        mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");

                                    if (mNavigationBarView == null)
                                        mNavigationBarView = (View) param.thisObject;

                                    IntentFilter intentFilter = new IntentFilter();
                                    intentFilter.addAction(ACTION_NAVBAR_CHANGED);
                                    mContext.registerReceiver(mNavBarUpdateReceiver, intentFilter);
                                }
                            });
                } catch (NoSuchMethodError e) {
                    XposedBridge.log("NavigationBarView constructor not found");
                    return;
                }

                try {
                    XposedHelpers.findAndHookMethod(CLASSNAME_NAVIGATIONBARVIEW, lpparam.classLoader, "setMenuVisibility",
                            boolean.class, boolean.class, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if (mContext == null)
                                        mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                                    SharedPreferences remotePref = new RemotePreferences(mContext, Utils.PREF_AUTHORITY, Utils.PREF_NAME);
                                    mShowMenu = remotePref.getBoolean("pref_show_menu", true);

                                    if (mShowMenu) {
                                        XposedHelpers.setBooleanField(param.thisObject, "mShowMenu", true);

                                        int mNavigationIconHints = XposedHelpers.getIntField(param.thisObject, "mNavigationIconHints");
                                        boolean shouldShow = (mNavigationIconHints & NAVIGATION_HINT_IME_SHOWN) == 0;

                                        Object menuView = XposedHelpers.callMethod(param.thisObject, "getMenuButton");
                                        XposedHelpers.callMethod(menuView, "setVisibility", shouldShow ? View.VISIBLE : View.INVISIBLE);
                                    }
                                }
                            });
                } catch (NoSuchMethodError e) {
                    XposedBridge.log("setMenuVisibility not found");
                    return;
                }

                try {
                    XposedHelpers.findAndHookMethod(CLASSNAME_NAVIGATIONBARVIEW, lpparam.classLoader, "updateIcons",
                            Context.class, Configuration.class, Configuration.class, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    Context context = (Context) param.args[0];
                                    SharedPreferences remotePref = new RemotePreferences(context, Utils.PREF_AUTHORITY, Utils.PREF_NAME);
                                    mUseTheme = remotePref.getBoolean("pref_usetheme", false);

                                    if (mUseTheme) {
                                        // theme settings
                                        prepareForTheme(remotePref);

                                        XposedHelpers.setObjectField(param.thisObject, "mBackIcon",
                                                getButtonDrawable(mThemeIcons, mCustomButtons, mUseTheme, mThemeId, mThemeColor, false, false, "Back"));
                                        XposedHelpers.setObjectField(param.thisObject, "mBackLandIcon",
                                                getButtonDrawable(mThemeIcons, mCustomButtons, mUseTheme, mThemeId, mThemeColor, true, false, "Back"));
                                        XposedHelpers.setObjectField(param.thisObject, "mBackAltIcon",
                                                getButtonDrawable(mThemeIcons, mCustomButtons, mUseTheme, mThemeId, mThemeColor, false, false, "Back"));
                                        XposedHelpers.setObjectField(param.thisObject, "mBackAltLandIcon",
                                                getButtonDrawable(mThemeIcons, mCustomButtons, mUseTheme, mThemeId, mThemeColor, true, false, "Back"));

                                        XposedHelpers.setObjectField(param.thisObject, "mHomeDefaultIcon",
                                                getButtonDrawable(mThemeIcons, mCustomButtons, mUseTheme, mThemeId, mThemeColor, false, false, "Home"));
                                        XposedHelpers.setObjectField(param.thisObject, "mRecentIcon",
                                                getButtonDrawable(mThemeIcons, mCustomButtons, mUseTheme, mThemeId, mThemeColor, false, false, "Recent"));
                                        XposedHelpers.setObjectField(param.thisObject, "mMenuIcon",
                                                getButtonDrawable(mThemeIcons, mCustomButtons, mUseTheme, mThemeId, mThemeColor, false, mUseAltMenu, "Menu"));
                                    }
                                }
                            });
                } catch (NoSuchMethodError e) {
                    XposedBridge.log("setMenuVisibility not found");
                    return;
                }
            } else {
                // hook setDisabledFlags method
                try {
                    XposedHelpers.findMethodExact(CLASSNAME_NAVIGATIONBARVIEW, lpparam.classLoader, "setDisabledFlags",
                            int.class, boolean.class);

                    XposedHelpers.findAndHookMethod(CLASSNAME_NAVIGATIONBARVIEW, lpparam.classLoader, "setDisabledFlags",
                            int.class, boolean.class, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    // keep original value for afterHookedMethod
                                    mDisabledFlags = XposedHelpers.getIntField(param.thisObject, "mDisabledFlags");
                                }

                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    final int disabledFlags = (Integer) param.args[0];
                                    final boolean force = (Boolean) param.args[1];

                                    final View mCurrentView = (View) XposedHelpers.getObjectField(param.thisObject,
                                            "mCurrentView");

                                    final View recentButton = mCurrentView.findViewById(mCurrentView.getResources()
                                            .getIdentifier("recent_apps", "id", CLASSNAME_SYSTEMUI));
                                    final View searchButton = mCurrentView.findViewWithTag("Search");

                                    if (!force && mDisabledFlags == disabledFlags)
                                        return;

                                    pref.reload();

                                    final boolean disableRecent = (disabledFlags & View.STATUS_BAR_DISABLE_RECENT) != 0;
                                    final boolean disableHome = (disabledFlags & View.STATUS_BAR_DISABLE_HOME) != 0;

                                    if (searchButton != null)
                                        searchButton.setVisibility(mShowSearch ? (disableHome ? View.INVISIBLE
                                                : View.VISIBLE) : View.GONE);
                                    if (recentButton != null)
                                        recentButton.setVisibility(mShowRecent ? (disableRecent ? View.INVISIBLE
                                                : View.VISIBLE) : View.INVISIBLE);

                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                                        final View menuButton = (View) XposedHelpers.callMethod(param.thisObject,
                                                "getMenuButton");
                                        if (menuButton != null)
                                            menuButton.setVisibility(mShowMenu ? (disableHome ? View.INVISIBLE
                                                    : View.VISIBLE) : View.GONE);
                                    }
                                }
                            });
                } catch (NoSuchMethodError e2) {
                    XposedBridge.log("setDisabledFlags not found");
                    return;
                }

                // replace setMenuVisibility(boolean, boolean) method
                try {
                    XposedHelpers.findMethodExact(CLASSNAME_NAVIGATIONBARVIEW, lpparam.classLoader, "setMenuVisibility",
                            boolean.class, boolean.class);

                    XposedHelpers.findAndHookMethod(CLASSNAME_NAVIGATIONBARVIEW, lpparam.classLoader, "setMenuVisibility",
                            boolean.class, boolean.class, new XC_MethodReplacement() {
                                @Override
                                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        final boolean shouldShow;

                                        View menuView = (View) XposedHelpers.callMethod(param.thisObject, "getMenuButton");
                                        if (menuView != null) {
                                            final int mNavigationIconHints = (Integer) XposedHelpers.getIntField(
                                                    param.thisObject, "mNavigationIconHints");

                                            shouldShow = mShowMenu
                                                    && ((mNavigationIconHints & NAVIGATION_HINT_IME_SHOWN) == 0);
                                            menuView.setVisibility(shouldShow ? View.VISIBLE : View.INVISIBLE);
                                        }
                                        XposedHelpers.setBooleanField(param.thisObject, "mShowMenu", mShowMenu);
                                    }
                                    return null;
                                }
                            });
                } catch (NoSuchMethodError e2) {
                    XposedBridge.log("setMenuVisibility(boolean, boolean) not found");
                    return;
                }

                pref.reload();
                boolean useTheme = pref.getBoolean("pref_usetheme", false);
                if (useTheme) {
                    // disable Xperia button transition methods
                    try {
                        XposedHelpers.findMethodExact(CLASSNAME_NAVIGATIONBUTTONTRANSITIONS, lpparam.classLoader,
                                "updateBackTransitions", ImageView.class, boolean.class, boolean.class);

                        XposedBridge.log("Xperia updateBackTransitions(ImageView, boolean, boolean) found");
                        XposedHelpers.findAndHookMethod(CLASSNAME_NAVIGATIONBUTTONTRANSITIONS, lpparam.classLoader,
                                "updateBackTransitions", ImageView.class, boolean.class, boolean.class,
                                new XC_MethodReplacement() {
                                    @Override
                                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                        return null;
                                    }
                                });
                    } catch (ClassNotFoundError e1) {
                    } catch (NoSuchMethodError e2) {
                    }

                    try {
                        XposedHelpers.findMethodExact(CLASSNAME_NAVIGATIONBUTTONTRANSITIONS, lpparam.classLoader,
                                "updateHomeTransitions", ImageView.class, boolean.class);

                        XposedBridge.log("Xperia updateHomeTransitions(ImageView, boolean) found");
                        XposedHelpers.findAndHookMethod(CLASSNAME_NAVIGATIONBUTTONTRANSITIONS, lpparam.classLoader,
                                "updateHomeTransitions", ImageView.class, boolean.class, new XC_MethodReplacement() {
                                    @Override
                                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                        return null;
                                    }
                                });
                    } catch (ClassNotFoundError e1) {
                    } catch (NoSuchMethodError e2) {
                    }

                    try {
                        XposedHelpers.findMethodExact(CLASSNAME_NAVIGATIONBUTTONTRANSITIONS, lpparam.classLoader,
                                "updateImeSwitcherTransitions", ImageView.class, boolean.class);

                        XposedBridge.log("Xperia updateImeSwitcherTransitions(ImageView, boolean) found");
                        XposedHelpers.findAndHookMethod(CLASSNAME_NAVIGATIONBUTTONTRANSITIONS, lpparam.classLoader,
                                "updateImeSwitcherTransitions", ImageView.class, boolean.class, new XC_MethodReplacement() {
                                    @Override
                                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                        return null;
                                    }
                                });
                    } catch (ClassNotFoundError e1) {
                    } catch (NoSuchMethodError e2) {
                    }

                    try {
                        XposedHelpers.findMethodExact(CLASSNAME_NAVIGATIONBUTTONTRANSITIONS, lpparam.classLoader,
                                "updateMenuTransitions", ImageView.class, boolean.class);

                        XposedBridge.log("Xperia updateMenuTransitions(ImageView, boolean) found");
                        XposedHelpers.findAndHookMethod(CLASSNAME_NAVIGATIONBUTTONTRANSITIONS, lpparam.classLoader,
                                "updateMenuTransitions", ImageView.class, boolean.class, new XC_MethodReplacement() {
                                    @Override
                                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                        return null;
                                    }
                                });
                    } catch (ClassNotFoundError e1) {
                    } catch (NoSuchMethodError e2) {
                    }

                    try {
                        XposedHelpers.findMethodExact(CLASSNAME_NAVIGATIONBUTTONTRANSITIONS, lpparam.classLoader,
                                "updateRecentTransitions", ImageView.class, boolean.class);

                        XposedBridge.log("Xperia updateRecentTransitions(ImageView, boolean) found");
                        XposedHelpers.findAndHookMethod(CLASSNAME_NAVIGATIONBUTTONTRANSITIONS, lpparam.classLoader,
                                "updateRecentTransitions", ImageView.class, boolean.class, new XC_MethodReplacement() {
                                    @Override
                                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                        return null;
                                    }
                                });
                    } catch (ClassNotFoundError e1) {
                    } catch (NoSuchMethodError e2) {
                    }
                }
            }
        }

        if (lpparam.packageName.equals(PKGNAME_SYSTEM_SERVICE)) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                handlePhoneWindowManager(lpparam.classLoader, CLASSNAME_PHONEWINDOWMANAGER_MM);
            }
        }
    }

    ImageView createLightsOutView(LayoutInflatedParam liparam, int width, int height, String imgResName) {
        ImageView iv = new ImageView(mContext);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height, 0.0f);
        iv.setLayoutParams(lp);
        iv.setImageDrawable(liparam.res.getDrawable(liparam.res.getIdentifier(imgResName, "drawable",
                CLASSNAME_SYSTEMUI)));
        iv.setScaleType(ScaleType.CENTER);

        return iv;
    }

    ImageView createButtonView(Context context, LayoutInflatedParam liparam, int width, int height, String glowBgResName, Drawable img,
                               int code, String tag, int resId, boolean supportLongpress) {
        final ImageView view;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            view = new KeyButtonViewN(context, code, supportLongpress);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            view = new KeyButtonViewL(context, code, supportLongpress);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            view = new KeyButtonView(context, code, supportLongpress, liparam.res.getDrawable(liparam.res
                    .getIdentifier(glowBgResName, "drawable", CLASSNAME_SYSTEMUI)));
        else
            view = new KeyButtonViewICS(context, code, supportLongpress, liparam.res.getDrawable(liparam.res
                    .getIdentifier(glowBgResName, "drawable", CLASSNAME_SYSTEMUI)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height, 0.0f);
        view.setLayoutParams(lp);
        view.setImageDrawable(img);
        view.setScaleType(ScaleType.CENTER);
        view.setTag(tag);
        if (resId >= 0)
            view.setId(resId);
        return view;
    }

    void replaceCustomButton(XResources res, String resName, String type, boolean isAlt, boolean landscape) {
        try {
            Bitmap bitmap = mCustomButtons.getBitmap(type, isAlt, landscape);
            if (bitmap != null) {
                final Drawable drawable = new BitmapDrawable(res, bitmap);
                res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", resName, new XResources.DrawableLoader() {
                    @Override
                    public Drawable newDrawable(XResources res, int id) throws Throwable {
                        return drawable;
                    }
                });
            }
        } catch (Exception e) {
            XposedBridge.log("Resource systemui:drawable/" + resName + " not found");
        }
    }

    Drawable getButtonDrawable(ThemeIcons themeIcons, CustomButtons customButtons, boolean useTheme, String themeId, String themeColor, boolean landscape, boolean isAlt, String key) {
        Drawable drawable = null;
        if (useTheme) {
            if (themeIcons.getThemeExample(themeId) == -1) {
                if (customButtons != null) {
                    Bitmap bitmap = customButtons.getBitmap(key, isAlt, landscape);
                    if (bitmap != null) {
                        drawable = new BitmapDrawable(modRes, bitmap);
                    }
                }
            }
            if (drawable == null)
                drawable = modRes
                        .getDrawable(themeIcons.getIconResId(themeId, themeColor, key, isAlt, landscape));
        } else if ("Search".equals(key)) {
            drawable = modRes.getDrawable(landscape ? R.drawable.ic_sysbar_search_land : R.drawable.ic_sysbar_search);
        }

        return drawable;
    }

    View getPlaceholderView(Context context, int width, int height) {
        ImageView iv = new ImageView(context);
        iv.setLayoutParams(new LinearLayout.LayoutParams(width, height, 0.0f));
        return iv;
    }

    void initScreenConfig(Context context) {
        // get screen dimension
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display d = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        d.getMetrics(metrics);

        mRotation = d.getRotation();

        // includes window decorations (statusbar bar/menu bar)
        if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17)
            try {
                mScreenWidth = (Integer) Display.class.getMethod("getRawWidth").invoke(d);
                mScreenWidthLand = (Integer) Display.class.getMethod("getRawHeight").invoke(d);
            } catch (Exception ignored) {
            }

        // includes window decorations (statusbar bar/menu bar)
        if (Build.VERSION.SDK_INT >= 17)
            try {
                Point realSize = new Point();
                Display.class.getMethod("getRealSize", Point.class).invoke(d, realSize);
                mScreenWidth = realSize.x;
                mScreenWidthLand = realSize.y;
            } catch (Exception ignored) {
            }
    }

    String nKey2OldKey(String key) {
        if ("home".equals(key))
            return "Home";
        if ("back".equals(key))
            return "Back";
        if ("recent".equals(key))
            return "Recent";
        if (KEY_MENU_N.equals(key))
            return "Menu";
        return null;
    }

    void prepareForTheme(SharedPreferences remotePref) {
        if (mThemeIcons == null)
            mThemeIcons = new ThemeIcons();

        mUseTheme = remotePref.getBoolean("pref_usetheme", false);
        mUseAltMenu = remotePref.getBoolean("pref_use_alt_menu", false);
        mThemeId = remotePref.getString("pref_themeid", DEF_THEMEID);
        mThemeColor = remotePref.getString("pref_themecolor", DEF_THEMECOLOR);
        mCacheFolder = remotePref.getString("pref_cache_folder", null);
        mDensityDpi = remotePref.getInt("pref_density_dpi", -1);

        if (mCacheFolder != null && mDensityDpi > 0)
            mCustomButtons = new CustomButtons(mCacheFolder, mDensityDpi, CustomButtons.FILENAME_PREFIX);
    }

    public static String getOrderListForN(String orderList, boolean showMenu) {
        return KEY_LEFT_MARGIN_N + GRAVITY_SEPARATOR + KEY_LEFT_IME_PADDING_N + GRAVITY_SEPARATOR
                + orderList.replace(",", GRAVITY_SEPARATOR).replace("Menu", KEY_MENU_N).toLowerCase()
                + GRAVITY_SEPARATOR + (showMenu ? KEY_LEFT_IME_PADDING_N : KEY_MENU_N)
                + GRAVITY_SEPARATOR + KEY_RIGHT_MARGIN_N;
    }

    void inflateButtonN(MethodHookParam param) {
        if (mContext == null)
            mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
        SharedPreferences remotePref = new RemotePreferences(mContext, Utils.PREF_AUTHORITY, Utils.PREF_NAME);

        String buttonSpec = (String) param.args[0];
        ViewGroup parent = (ViewGroup) param.args[1];
        boolean landscape = (boolean) param.args[2];

        // theme settings
        prepareForTheme(remotePref);

        if (param.getResult() == null) {
//                                        XposedBridge.log(String.format("buttonSpec:%s landscape:%b", buttonSpec, landscape));

            Drawable drawable;
            ImageView view;

            if (KEY_SEARCH_N.equals(buttonSpec)) {
                int keyCode = Integer.parseInt(remotePref.getString("pref_search_function", String.valueOf(KeyEvent.KEYCODE_SEARCH)));
                int keyCodeLongPress = Integer.parseInt(remotePref.getString("pref_search_longpress_function", String.valueOf(KEYCODE_NONE)));
                String funcApp = remotePref.getString("pref_search_function_apps", null);
                String funcAppLongPress = remotePref.getString("pref_search_longpress_function_apps", null);
                String funcShortcut = remotePref.getString("pref_search_function_shortcut", null);
                String funcShortcutLongPress = remotePref.getString("pref_search_longpress_function_shortcut", null);

                int layoutWidth = landscape ? LinearLayout.LayoutParams.MATCH_PARENT : LinearLayout.LayoutParams.WRAP_CONTENT;
                int layoutHeight = landscape ? LinearLayout.LayoutParams.WRAP_CONTENT : LinearLayout.LayoutParams.MATCH_PARENT;
                drawable = getButtonDrawable(mThemeIcons, mCustomButtons, mUseTheme, mThemeId, mThemeColor, landscape, false, "Search");
                view = createButtonView(mContext, null, layoutWidth, layoutHeight, null,
                        drawable, keyCode, "search", RES_ID_SEARCH, true);

                ((KeyButtonViewN) view).setCodeLongPress(keyCodeLongPress);
                ((KeyButtonViewN) view).setFuncApp(funcApp);
                ((KeyButtonViewN) view).setFuncAppLongPress(funcAppLongPress);
                ((KeyButtonViewN) view).setFuncShortcut(funcShortcut);
                ((KeyButtonViewN) view).setFuncShortcutLongPress(funcShortcutLongPress);

//                                            XposedBridge.log(String.format("keyCode:%d, keyCodeLongPress:%d", keyCode, keyCodeLongPress));

            } else if (KEY_SEPARATOR_N.equals(buttonSpec) || KEY_LEFT_MARGIN_N.equals(buttonSpec)
                    || KEY_RIGHT_MARGIN_N.equals(buttonSpec) || KEY_LEFT_IME_PADDING_N.equals(buttonSpec)) {
                int layoutWidth = landscape ? LinearLayout.LayoutParams.MATCH_PARENT : 1;
                int layoutHeight = landscape ? 1 : LinearLayout.LayoutParams.MATCH_PARENT;
                view = new ImageView(mContext);
                view.setLayoutParams(new LinearLayout.LayoutParams(layoutWidth, layoutHeight, 0.0f));
                view.setTag(buttonSpec);
            } else {
                param.setResult(null);
                return;
            }
            parent.addView(view);

            String lastLandFieldName;
            String lastPortFieldName;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                lastLandFieldName = "mLastLandscape";
                lastPortFieldName = "mLastPortrait";
            } else {
                lastLandFieldName = "mLastRot90";
                lastPortFieldName = "mLastRot0";
            }
            if (landscape)
                XposedHelpers.setObjectField(param.thisObject, lastLandFieldName, view);
            else
                XposedHelpers.setObjectField(param.thisObject, lastPortFieldName, view);
        } else if (KEY_HOME_N.equals(buttonSpec) || KEY_MENU_N.equals(buttonSpec)
                || KEY_BACK_N.equals(buttonSpec) || KEY_RECENT_N.equals(buttonSpec)) {
            View view = (View) param.getResult();
            if (mUseTheme) {
                String key = nKey2OldKey(buttonSpec);
                Drawable drawable = getButtonDrawable(mThemeIcons, mCustomButtons, mUseTheme, mThemeId, mThemeColor,
                        landscape, KEY_MENU_N.equals(buttonSpec) && mUseAltMenu, key);
                if (KEY_MENU_N.equals(buttonSpec)) {
                    int menuId = mContext.getResources().getIdentifier("menu", "id", CLASSNAME_SYSTEMUI);
                    for (int i = 0; i < ((FrameLayout) view).getChildCount(); i++)
                        if (((FrameLayout) view).getChildAt(i).getId() == menuId)
                            ((ImageView) ((FrameLayout) view).getChildAt(i)).setImageDrawable(drawable);
                } else
                    ((ImageView) view).setImageDrawable(drawable);
            }
            view.setTag(buttonSpec);
        }
    }
}
