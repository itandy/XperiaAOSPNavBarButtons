package com.gzplanet.xposed.xperianavbarbuttons;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.res.XModuleResources;
import android.graphics.Point;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LayoutInflated.LayoutInflatedParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XperiaNavBarButtons implements IXposedHookZygoteInit, IXposedHookInitPackageResources, IXposedHookLoadPackage {
	final static String CLASSNAME_SYSTEMUI = "com.android.systemui";
	final static String CLASSNAME_NAVIGATIONBARVIEW = "com.android.systemui.statusbar.phone.NavigationBarView";

	private static String MODULE_PATH = null;
	private final static String DEF_BUTTONS_ORDER_LIST = "Home,Menu,Recent,Back,Search";

	Context mContext;
	XModuleResources modRes;
	private static XSharedPreferences pref;
	int mDisabledFlags;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		MODULE_PATH = startupParam.modulePath;

		pref = new XSharedPreferences(XperiaNavBarButtons.class.getPackage().getName());
		// just in case the preference file permission is reset by
		// recovery/script
		pref.makeWorldReadable();
	}

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
		if (!resparam.packageName.equals(CLASSNAME_SYSTEMUI))
			return;

		modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);

		resparam.res.hookLayout(CLASSNAME_SYSTEMUI, "layout", "navigation_bar", new XC_LayoutInflated() {
			@Override
			public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
				int buttonsCount;
				int screenWidth;
				int buttonWidth;

				pref.reload();

				String[] orderList = pref.getString("pref_order", DEF_BUTTONS_ORDER_LIST).split(",");
				buttonsCount = orderList.length;

				mContext = liparam.view.getContext();
				final Display defaultDisplay = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
				final Point point = new Point();
				defaultDisplay.getSize(point);
				screenWidth = point.x;
				buttonWidth = Math.round((float) screenWidth / (float) buttonsCount);
				XposedBridge.log(String.format("screenWidth:%d, buttonWidth:%d", screenWidth, buttonWidth));

				FrameLayout rot0 = (FrameLayout) liparam.view.findViewById(liparam.res.getIdentifier("rot0", "id", CLASSNAME_SYSTEMUI));
				FrameLayout rot90 = (FrameLayout) liparam.view.findViewById(liparam.res.getIdentifier("rot90", "id", CLASSNAME_SYSTEMUI));
				LinearLayout rot0NavButtons = null;
				LinearLayout rot0LightsOut = null;
				LinearLayout rot0LightsOutHigh = null;
				LinearLayout rot90NavButtons = null;
				LinearLayout rot90LightsOut = null;
				LinearLayout rot90LightsOutHigh = null;

				// portrait views
				if (rot0 != null) {
					rot0NavButtons = (LinearLayout) rot0.findViewById(liparam.res.getIdentifier("nav_buttons", "id", CLASSNAME_SYSTEMUI));
					rot0LightsOut = (LinearLayout) rot0.findViewById(liparam.res.getIdentifier("lights_out", "id", CLASSNAME_SYSTEMUI));
					rot0LightsOutHigh = (LinearLayout) rot0.findViewById(liparam.res.getIdentifier("lights_out_high", "id", CLASSNAME_SYSTEMUI));

					// handle nav buttions
					if (rot0NavButtons != null) {
						Map<String, View> viewList = new HashMap<String, View>();
						viewList.put("Back", (View) rot0NavButtons.findViewById(liparam.res.getIdentifier("back", "id", CLASSNAME_SYSTEMUI)));
						viewList.put("Home", (View) rot0NavButtons.findViewById(liparam.res.getIdentifier("home", "id", CLASSNAME_SYSTEMUI)));
						viewList.put("Recent", (View) rot0NavButtons.findViewById(liparam.res.getIdentifier("recent_apps", "id", CLASSNAME_SYSTEMUI)));
						viewList.put("Menu", (View) rot0NavButtons.findViewById(liparam.res.getIdentifier("menu", "id", CLASSNAME_SYSTEMUI)));
						viewList.put(
								"Search",
								createButtonView(liparam, buttonWidth, LinearLayout.LayoutParams.FILL_PARENT, "ic_sysbar_highlight",
										R.drawable.ic_sysbar_search, KeyEvent.KEYCODE_SEARCH, "search"));

						rot0NavButtons.removeAllViews();
						// add selected buttons
						for (int i = 0; i < buttonsCount; i++) {
							View view = viewList.remove(orderList[i]);
							if (view != null) {
								view.setVisibility(View.VISIBLE);
								view.setLayoutParams(new LinearLayout.LayoutParams(buttonWidth, LinearLayout.LayoutParams.FILL_PARENT, 0.0f));
								rot0NavButtons.addView(view);
							}
						}

						// add unselected buttons and make them invisible
						for (View view : viewList.values()) {
							view.setVisibility(View.GONE);
							rot0NavButtons.addView(view);
						}
					}

					// handle lights out
					if (rot0LightsOut != null) {
						rot0LightsOut.removeAllViews();
						int i = 0;
						while (i < buttonsCount) {
							rot0LightsOut.addView(createLightsOutView(liparam, buttonWidth, LinearLayout.LayoutParams.FILL_PARENT,
									"ic_sysbar_lights_out_dot_small"));
							i++;
						}
					}

					// handle lights out high
					if (rot0LightsOutHigh != null) {
						rot0LightsOutHigh.removeAllViews();
						int i = 0;
						while (i < buttonsCount) {
							rot0LightsOutHigh.addView(createLightsOutView(liparam, buttonWidth, LinearLayout.LayoutParams.FILL_PARENT,
									"ic_sysbar_lights_out_dot_small_high"));
							i++;
						}
					}
				}

				// landscape views
				if (rot90 != null) {
					rot90NavButtons = (LinearLayout) rot90.findViewById(liparam.res.getIdentifier("nav_buttons", "id", CLASSNAME_SYSTEMUI));
					rot90LightsOut = (LinearLayout) rot90.findViewById(liparam.res.getIdentifier("lights_out", "id", CLASSNAME_SYSTEMUI));
					rot90LightsOutHigh = (LinearLayout) rot90.findViewById(liparam.res.getIdentifier("lights_out_high", "id", CLASSNAME_SYSTEMUI));

					// handle nav buttions
					if (rot90NavButtons != null) {
						Map<String, View> viewList = new HashMap<String, View>();
						viewList.put("Back", (View) rot90NavButtons.findViewById(liparam.res.getIdentifier("back", "id", CLASSNAME_SYSTEMUI)));
						viewList.put("Home", (View) rot90NavButtons.findViewById(liparam.res.getIdentifier("home", "id", CLASSNAME_SYSTEMUI)));
						viewList.put("Recent", (View) rot90NavButtons.findViewById(liparam.res.getIdentifier("recent_apps", "id", CLASSNAME_SYSTEMUI)));
						viewList.put("Menu", (View) rot90NavButtons.findViewById(liparam.res.getIdentifier("menu", "id", CLASSNAME_SYSTEMUI)));
						viewList.put(
								"Search",
								createButtonView(liparam, buttonWidth, LinearLayout.LayoutParams.FILL_PARENT, "ic_sysbar_highlight_land",
										R.drawable.ic_sysbar_search_land, KeyEvent.KEYCODE_SEARCH, "search"));

						rot90NavButtons.removeAllViews();
						// add selected buttons
						for (int i = buttonsCount - 1; i >= 0; i--) {
							View view = viewList.remove(orderList[i]);
							if (view != null) {
								view.setVisibility(View.VISIBLE);
								view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, buttonWidth, 0.0f));
								rot90NavButtons.addView(view);
							}
						}

						// add unselected buttons and make them invisible
						for (View view : viewList.values()) {
							view.setVisibility(View.GONE);
							rot90NavButtons.addView(view);
						}
					}

					// handle lights out
					if (rot90LightsOut != null) {
						rot90LightsOut.removeAllViews();
						int i = 0;
						while (i < buttonsCount) {
							rot90LightsOut.addView(createLightsOutView(liparam, LinearLayout.LayoutParams.FILL_PARENT, buttonWidth,
									"ic_sysbar_lights_out_dot_small"));
							i++;
						}
					}

					// handle lights out high
					if (rot90LightsOutHigh != null) {
						rot90LightsOutHigh.removeAllViews();
						int i = 0;
						while (i < buttonsCount) {
							rot90LightsOutHigh.addView(createLightsOutView(liparam, LinearLayout.LayoutParams.FILL_PARENT, buttonWidth,
									"ic_sysbar_lights_out_dot_small_high"));
							i++;
						}
					}
				}

				XposedBridge.log(String.format("rot0:%b, rot0NavButtons:%b, rot0LightsOut:%b, rot0LightsOutHigh:%b", rot0 != null, rot0NavButtons != null,
						rot0LightsOut != null, rot0LightsOutHigh != null));
				XposedBridge.log(String.format("rot90:%b, rot90NavButtons:%b, rot90LightsOut:%b, rot90LightsOutHigh:%b", rot90 != null,
						rot90NavButtons != null, rot90LightsOut != null, rot90LightsOutHigh != null));
			}
		});
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(CLASSNAME_SYSTEMUI))
			return;

		// replace setDisabledFlags method
		try {
			XposedHelpers.findMethodExact(CLASSNAME_NAVIGATIONBARVIEW, lpparam.classLoader, "setDisabledFlags", int.class, boolean.class);

			XposedHelpers.findAndHookMethod(CLASSNAME_NAVIGATIONBARVIEW, lpparam.classLoader, "setDisabledFlags", int.class, boolean.class,
					new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
							// keep original value for afterHookedMethod
							mDisabledFlags = XposedHelpers.getIntField(param.thisObject, "mDisabledFlags");
						}

						@Override
						protected void afterHookedMethod(MethodHookParam param) throws Throwable {
							final int disabledFlags = (Integer) param.args[0];
							final boolean force = (Boolean) param.args[1];

							final View mCurrentView = (View) XposedHelpers.getObjectField(param.thisObject, "mCurrentView");

							final View recentButton = mCurrentView.findViewById(mCurrentView.getResources().getIdentifier("recent_apps", "id",
									CLASSNAME_SYSTEMUI));
							final View searchButton = mCurrentView.findViewWithTag("search");
							final View menuButton = mCurrentView.findViewById(mCurrentView.getResources().getIdentifier("menu", "id", CLASSNAME_SYSTEMUI));

							if (!force && mDisabledFlags == disabledFlags)
								return;

							pref.reload();

							final boolean disableRecent = (disabledFlags & View.STATUS_BAR_DISABLE_RECENT) != 0;
							final boolean disableHome = (disabledFlags & View.STATUS_BAR_DISABLE_HOME) != 0;

							if (searchButton != null)
								searchButton.setVisibility(pref.getBoolean("pref_show_search", true) ? (disableHome ? View.INVISIBLE : View.VISIBLE)
										: View.GONE);
							if (menuButton != null)
								menuButton.setVisibility(pref.getBoolean("pref_show_menu", true) ? (disableHome ? View.INVISIBLE : View.VISIBLE) : View.GONE);
							if (recentButton != null)
								recentButton.setVisibility(pref.getBoolean("pref_show_recent", true) ? (disableRecent ? View.INVISIBLE : View.VISIBLE)
										: View.GONE);
						}
					});
		} catch (NoSuchMethodError e2) {
			XposedBridge.log("setDisabledFlags not found");
			return;
		}

		// replace setMenuVisibility(boolean) method
		try {
			XposedHelpers.findMethodExact(CLASSNAME_NAVIGATIONBARVIEW, lpparam.classLoader, "setMenuVisibility", boolean.class);

			XposedHelpers.findAndHookMethod(CLASSNAME_NAVIGATIONBARVIEW, lpparam.classLoader, "setMenuVisibility", boolean.class, new XC_MethodReplacement() {
				@Override
				protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
					return null;
				}
			});
		} catch (NoSuchMethodError e2) {
			XposedBridge.log("setMenuVisibility(boolean) not found");
			return;
		}

		// replace setMenuVisibility(boolean, boolean) method
		try {
			XposedHelpers.findMethodExact(CLASSNAME_NAVIGATIONBARVIEW, lpparam.classLoader, "setMenuVisibility", boolean.class, boolean.class);

			XposedHelpers.findAndHookMethod(CLASSNAME_NAVIGATIONBARVIEW, lpparam.classLoader, "setMenuVisibility", boolean.class, boolean.class,
					new XC_MethodReplacement() {
						@Override
						protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
							return null;
						}
					});
		} catch (NoSuchMethodError e2) {
			XposedBridge.log("setMenuVisibility(boolean, boolean) not found");
			return;
		}
	}

	ImageView createLightsOutView(LayoutInflatedParam liparam, int width, int height, String imgResName) {
		ImageView iv = new ImageView(mContext);

		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height, 0.0f);
		iv.setLayoutParams(lp);
		iv.setImageDrawable(liparam.res.getDrawable(liparam.res.getIdentifier(imgResName, "drawable", CLASSNAME_SYSTEMUI)));
		iv.setScaleType(ScaleType.CENTER);

		return iv;
	}

	KeyButtonView createButtonView(LayoutInflatedParam liparam, int width, int height, String glowBgResName, int imgResId, int code, String tag) {
		KeyButtonView view = new KeyButtonView(mContext, code, true, liparam.res.getDrawable(liparam.res.getIdentifier(glowBgResName, "drawable",
				CLASSNAME_SYSTEMUI)));
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height, 0.0f);
		view.setLayoutParams(lp);
		view.setImageDrawable(modRes.getDrawable(imgResId));
		view.setScaleType(ScaleType.CENTER);
		view.setTag(tag);
		return view;
	}
}
