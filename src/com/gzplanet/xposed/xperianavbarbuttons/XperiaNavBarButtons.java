package com.gzplanet.xposed.xperianavbarbuttons;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
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
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManagerPolicy.WindowState;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
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

	static String MODULE_PATH = null;
	static Context mContext;
	static int mDisabledFlags;
	static String mCacheFolder;
	static int mDensityDpi;
	static CustomButtons mCustomButtons;
	static ThemeIcons mThemeIcons;
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

	Map<String, Bitmap> mStockButtons = new HashMap<String, Bitmap>();

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

		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1)
			handlePhoneWindowManager(null, CLASSNAME_PHONEWINDOWMANAGER);
	}

	private void handlePhoneWindowManager(ClassLoader classLoader, String classPhoneWindowManager) {
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
						};
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
							};
						});
			} else {
				XposedHelpers.findMethodExact(phoneWindowManager, "interceptKeyBeforeQueueing", KeyEvent.class,
						int.class, boolean.class);
				XposedHelpers.findAndHookMethod(phoneWindowManager, "interceptKeyBeforeQueueing", KeyEvent.class,
						int.class, boolean.class, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
							protected void afterHookedMethod(MethodHookParam param) throws Throwable {
								interceptKeyBeforeQueueing(param);
							};
						});
			}
		} catch (ClassNotFoundError e) {
			XposedBridge.log("Class PhoneWindowManager not found");
		} catch (NoSuchMethodError e2) {
			XposedBridge.log("Method interceptKeyBeforeQueueing not found");
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
			XposedHelpers.callMethod(thisObject, "performHapticFeedbackLw", new Class<?>[] { WindowState.class,
					int.class, boolean.class }, null, HapticFeedbackConstants.LONG_PRESS, false);
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

		boolean useTheme = pref.getBoolean("pref_usetheme", false);
		boolean useAltMenu = pref.getBoolean("pref_use_alt_menu", false);
		String themeId = pref.getString("pref_themeid", DEF_THEMEID);
		String themeColor = pref.getString("pref_themecolor", DEF_THEMECOLOR);

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
				int screenWidth = 0;
				int screenWidthLand = 0;
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

				// get screen dimension
				WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
				Display d = wm.getDefaultDisplay();
				DisplayMetrics metrics = new DisplayMetrics();
				d.getMetrics(metrics);

				// includes window decorations (statusbar bar/menu bar)
				if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17)
					try {
						screenWidth = (Integer) Display.class.getMethod("getRawWidth").invoke(d);
						screenWidthLand = (Integer) Display.class.getMethod("getRawHeight").invoke(d);
					} catch (Exception ignored) {
					}
				// includes window decorations (statusbar bar/menu bar)
				if (Build.VERSION.SDK_INT >= 17)
					try {
						Point realSize = new Point();
						Display.class.getMethod("getRealSize", Point.class).invoke(d, realSize);
						screenWidth = realSize.x;
						screenWidthLand = realSize.y;
					} catch (Exception ignored) {
					}

				// adjust left and right margins for alternate orientation
				leftMargin90 = Math.round((float) leftMargin * (float) screenWidthLand / (float) screenWidth);
				rightMargin90 = Math.round((float) rightMargin * (float) screenWidthLand / (float) screenWidth);

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
				int separatorWidth0 = Math.round((float) screenWidth * (float) separatorWidthFactor / 100);
				int separatorWidth90 = Math.round((float) screenWidthLand * (float) separatorWidthFactor / 100);
				int actualButtonsCount = showSeparator ? buttonsCount - 1 : buttonsCount;

				if (!mShowMenu && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					buttonWidth0 = Math
							.round((float) (screenWidth - leftMargin - rightMargin - extraKeyWidth * 2 - separatorWidth0)
									/ (float) actualButtonsCount);
					buttonWidth90 = tabletMode ? Math.round((float) (screenWidthLand - leftMargin90 - rightMargin90
							- extraKeyWidth * 2 - separatorWidth90)
							/ (float) actualButtonsCount) : buttonWidth0;
				} else {
					buttonWidth0 = Math.round((float) (screenWidth - leftMargin - rightMargin - separatorWidth0)
							/ (float) actualButtonsCount);
					buttonWidth90 = tabletMode ? Math
							.round((float) (screenWidthLand - leftMargin90 - rightMargin90 - separatorWidth90)
									/ (float) actualButtonsCount) : buttonWidth0;
				}

				// reset margin if value larger than screen width
				int maxWidth = (int) (screenWidth * 0.75);
				if (leftMargin > maxWidth || rightMargin > maxWidth) {
					leftMargin = 0;
					rightMargin = 0;
				}
				XposedBridge.log(String
						.format("screenWidth:%d, screenWidthLand:%d, buttonWidth0:%d, buttonWidth90:%d, leftMargin:%d, rightMargin:%d, leftMargin90:%d, rightMargin90:%d, extraKeyWidth:%d, separatorWidthFactor:%d",
								screenWidth, screenWidthLand, buttonWidth0, buttonWidth90, leftMargin, rightMargin,
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
							Drawable searchButtonDrawable = getSearchButton(useTheme, themeId, themeColor, false);
							viewList.put(
									"Search",
									createButtonView(liparam, buttonWidth0, LinearLayout.LayoutParams.MATCH_PARENT,
											"ic_sysbar_highlight", searchButtonDrawable, searchKeycode, "Search", true));
						}

						rot0NavButtons.removeAllViews();
						// left margin
						if (leftMargin > 0)
							addPlaceHolder(mContext, rot0NavButtons, leftMargin, LinearLayout.LayoutParams.MATCH_PARENT);

						// for Lollipop, add extra padding if menu button is
						// disabled
						if (!mShowMenu && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
							addPlaceHolder(mContext, rot0NavButtons, extraKeyWidth,
									LinearLayout.LayoutParams.MATCH_PARENT);

						// add selected buttons
						for (int i = 0; i < buttonsCount; i++) {

							if (orderList[i].equals("Separator")) {
								addPlaceHolder(mContext, rot0NavButtons, separatorWidth0,
										LinearLayout.LayoutParams.MATCH_PARENT);
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
							addPlaceHolder(mContext, rot0NavButtons, rightMargin,
									LinearLayout.LayoutParams.MATCH_PARENT);

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
							addPlaceHolder(mContext, rot0LightsOut, leftMargin, LinearLayout.LayoutParams.MATCH_PARENT);

						int i = 0;
						while (i < actualButtonsCount) {
							rot0LightsOut.addView(createLightsOutView(liparam, buttonWidth0,
									LinearLayout.LayoutParams.MATCH_PARENT, "ic_sysbar_lights_out_dot_small"));
							i++;
						}

						// right margin
						if (rightMargin > 0)
							addPlaceHolder(mContext, rot0LightsOut, rightMargin, LinearLayout.LayoutParams.MATCH_PARENT);
					}

					// handle lights out high
					if (rot0LightsOutHigh != null) {
						rot0LightsOutHigh.setGravity(Gravity.CENTER);
						rot0LightsOutHigh.removeAllViews();

						// left margin
						if (leftMargin > 0)
							addPlaceHolder(mContext, rot0LightsOutHigh, leftMargin,
									LinearLayout.LayoutParams.MATCH_PARENT);

						int i = 0;
						while (i < actualButtonsCount) {
							rot0LightsOutHigh.addView(createLightsOutView(liparam, buttonWidth0,
									LinearLayout.LayoutParams.MATCH_PARENT, "ic_sysbar_lights_out_dot_small_high"));
							i++;
						}

						// right margin
						if (rightMargin > 0)
							addPlaceHolder(mContext, rot0LightsOutHigh, rightMargin,
									LinearLayout.LayoutParams.MATCH_PARENT);
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
							Drawable searchButtonDrawable = getSearchButton(useTheme, themeId, themeColor, !tabletMode);
							viewList.put(
									"Search",
									createButtonView(liparam, buttonWidth90, LinearLayout.LayoutParams.MATCH_PARENT,
											tabletMode ? "ic_sysbar_highlight" : "ic_sysbar_highlight_land",
											searchButtonDrawable, searchKeycode, "Search", true));
						}

						rot90NavButtons.removeAllViews();
						// add selected buttons
						if (tabletMode) { // navbar is positioned horizontally
							final int extraPadding = Math
									.round((float) (screenWidthLand - leftMargin90 - rightMargin90 - buttonWidth90
											* actualButtonsCount - (mShowMenu ? 0 : extraKeyWidth * 2) - separatorWidth90) / 2f);
							// for Lollipop, add extra padding if menu button is
							// disabled
							if (!mShowMenu && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
								addPlaceHolder(mContext, rot90NavButtons, extraKeyWidth,
										LinearLayout.LayoutParams.MATCH_PARENT);

							// left margin + extra padding
							if (leftMargin90 + extraPadding > 0)
								addPlaceHolder(mContext, rot90NavButtons, leftMargin90 + extraPadding,
										LinearLayout.LayoutParams.MATCH_PARENT);

							for (int i = 0; i < buttonsCount; i++) {
								if (orderList[i].equals("Separator")) {
									addPlaceHolder(mContext, rot90NavButtons, separatorWidth90,
											LinearLayout.LayoutParams.MATCH_PARENT);
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
								addPlaceHolder(mContext, rot90NavButtons, rightMargin90 + extraPadding,
										LinearLayout.LayoutParams.MATCH_PARENT);

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
								addPlaceHolder(mContext, rot90NavButtons, LinearLayout.LayoutParams.MATCH_PARENT,
										leftMargin);

							for (int i = buttonsCount - 1; i >= 0; i--) {
								if (orderList[i].equals("Separator")) {
									addPlaceHolder(mContext, rot90NavButtons, LinearLayout.LayoutParams.MATCH_PARENT,
											separatorWidth0);
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
								addPlaceHolder(mContext, rot90NavButtons, LinearLayout.LayoutParams.MATCH_PARENT,
										rightMargin);

							// for Lollipop, add extra padding if menu button is
							// disabled
							if (!mShowMenu && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
								addPlaceHolder(mContext, rot90NavButtons, LinearLayout.LayoutParams.MATCH_PARENT,
										extraKeyWidth);
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
							addPlaceHolder(mContext, rot90LightsOut, LinearLayout.LayoutParams.MATCH_PARENT, leftMargin);

						int i = 0;
						while (i < actualButtonsCount) {
							rot90LightsOut.addView(createLightsOutView(liparam, LinearLayout.LayoutParams.MATCH_PARENT,
									buttonWidth90, "ic_sysbar_lights_out_dot_small"));
							i++;
						}

						// right margin
						if (rightMargin > 0)
							addPlaceHolder(mContext, rot90LightsOut, LinearLayout.LayoutParams.MATCH_PARENT,
									rightMargin);
					}

					// handle lights out high
					if (rot90LightsOutHigh != null) {
						rot90LightsOutHigh.setGravity(Gravity.CENTER);
						rot90LightsOutHigh.removeAllViews();

						// left margin
						if (leftMargin > 0)
							addPlaceHolder(mContext, rot90LightsOutHigh, LinearLayout.LayoutParams.MATCH_PARENT,
									leftMargin);

						int i = 0;
						while (i < actualButtonsCount) {
							rot90LightsOutHigh.addView(createLightsOutView(liparam,
									LinearLayout.LayoutParams.MATCH_PARENT, buttonWidth90,
									"ic_sysbar_lights_out_dot_small_high"));
							i++;
						}

						// right margin
						if (rightMargin > 0)
							addPlaceHolder(mContext, rot90LightsOutHigh, LinearLayout.LayoutParams.MATCH_PARENT,
									rightMargin);
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

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(CLASSNAME_SYSTEMUI) && !lpparam.packageName.equals(PKGNAME_SYSTEM_SERVICE))
			return;

		if (lpparam.packageName.equals(CLASSNAME_SYSTEMUI)) {
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
		} else if (lpparam.packageName.equals(PKGNAME_SYSTEM_SERVICE)) {
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

	ImageView createButtonView(LayoutInflatedParam liparam, int width, int height, String glowBgResName, Drawable img,
			int code, String tag, boolean supportLongpress) {
		final ImageView view;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			view = new KeyButtonViewL(mContext, code, supportLongpress);
		else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
			view = new KeyButtonView(mContext, code, supportLongpress, liparam.res.getDrawable(liparam.res
					.getIdentifier(glowBgResName, "drawable", CLASSNAME_SYSTEMUI)));
		else
			view = new KeyButtonViewICS(mContext, code, supportLongpress, liparam.res.getDrawable(liparam.res
					.getIdentifier(glowBgResName, "drawable", CLASSNAME_SYSTEMUI)));
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height, 0.0f);
		view.setLayoutParams(lp);
		view.setImageDrawable(img);
		view.setScaleType(ScaleType.CENTER);
		view.setTag(tag);
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

	Drawable getSearchButton(boolean useTheme, String themeId, String themeColor, boolean landscape) {
		Drawable drawable = null;
		if (useTheme) {
			if (mThemeIcons.getThemeExample(themeId) == -1) {
				if (mCustomButtons != null) {
					Bitmap bitmap = mCustomButtons.getBitmap("Search", false, landscape);
					if (bitmap != null) {
						drawable = new BitmapDrawable(modRes, bitmap);
					}
				}
			}
			if (drawable == null)
				drawable = modRes
						.getDrawable(mThemeIcons.getIconResId(themeId, themeColor, "Search", false, landscape));
		} else {
			drawable = modRes.getDrawable(landscape ? R.drawable.ic_sysbar_search_land : R.drawable.ic_sysbar_search);
		}

		return drawable;
	}

	void addPlaceHolder(Context context, LinearLayout parent, int width, int height) {
		ImageView iv = new ImageView(context);
		iv.setLayoutParams(new LinearLayout.LayoutParams(width, height, 0.0f));
		parent.addView(iv);
	}
}
