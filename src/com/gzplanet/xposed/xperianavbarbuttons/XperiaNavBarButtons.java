package com.gzplanet.xposed.xperianavbarbuttons;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Display;
import android.view.Gravity;
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

	final static String DEF_THEMEID = "Stock";
	final static String DEF_THEMECOLOR = "White";

	private static String MODULE_PATH = null;
	private final static String DEF_BUTTONS_ORDER_LIST = "Home,Menu,Recent,Back,Search";

	Context mContext;
	int mDisabledFlags;
	String mCacheFolder;
	int mDensityDpi;
	CustomButtons mCustomButtons;
	ThemeIcons mThemeIcons;
	XModuleResources modRes;

	static XSharedPreferences pref;

	Map<String, Bitmap> mStockButtons = new HashMap<String, Bitmap>();

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

		// module resources
		modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);

		mThemeIcons = new ThemeIcons();

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
				}
			} else {
				resparam.res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", "ic_sysbar_back",
						modRes.fwd(mThemeIcons.getIconResId(themeId, themeColor, "Back", false, false)));
				resparam.res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", "ic_sysbar_back_ime",
						modRes.fwd(mThemeIcons.getIconResId(themeId, themeColor, "Back", true, false)));
				resparam.res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", "ic_sysbar_back_land",
						modRes.fwd(mThemeIcons.getIconResId(themeId, themeColor, "Back", false, true)));
				resparam.res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", "ic_sysbar_home",
						modRes.fwd(mThemeIcons.getIconResId(themeId, themeColor, "Home", false, false)));
				resparam.res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", "ic_sysbar_home_land",
						modRes.fwd(mThemeIcons.getIconResId(themeId, themeColor, "Home", false, true)));
				resparam.res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", "ic_sysbar_menu",
						modRes.fwd(mThemeIcons.getIconResId(themeId, themeColor, "Menu", useAltMenu, false)));
				resparam.res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", "ic_sysbar_menu_land",
						modRes.fwd(mThemeIcons.getIconResId(themeId, themeColor, "Menu", useAltMenu, true)));
				resparam.res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", "ic_sysbar_recent",
						modRes.fwd(mThemeIcons.getIconResId(themeId, themeColor, "Recent", false, false)));
				resparam.res.setReplacement(CLASSNAME_SYSTEMUI, "drawable", "ic_sysbar_recent_land",
						modRes.fwd(mThemeIcons.getIconResId(themeId, themeColor, "Recent", false, true)));
			}
		}

		// replace NavBar layout
		resparam.res.hookLayout(CLASSNAME_SYSTEMUI, "layout", "navigation_bar", new XC_LayoutInflated() {
			@Override
			public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
				int buttonsCount;
				int screenWidth;
				int buttonWidth;

				pref.reload();
				boolean useTheme = pref.getBoolean("pref_usetheme", false);
				String themeId = pref.getString("pref_themeid", DEF_THEMEID);
				String themeColor = pref.getString("pref_themecolor", DEF_THEMECOLOR);
				String[] orderList = pref.getString("pref_order", DEF_BUTTONS_ORDER_LIST).split(",");
				boolean showSearch = pref.getBoolean("pref_show_search", false);
				buttonsCount = orderList.length;

				mContext = liparam.view.getContext();
				final Display defaultDisplay = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
				final Point point = new Point();
				defaultDisplay.getSize(point);
				screenWidth = point.x;
				buttonWidth = Math.round((float) screenWidth / (float) buttonsCount);
				XposedBridge.log(String.format("screenWidth:%d, buttonWidth:%d", screenWidth, buttonWidth));

				// write stock button images to cache folder
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
				} catch (Exception e) {
					XposedBridge.log(e.getMessage());
				}

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
						Map<String, ImageView> viewList = new HashMap<String, ImageView>();

						viewList.put("Home", (ImageView) rot0NavButtons.findViewById(liparam.res.getIdentifier("home", "id", CLASSNAME_SYSTEMUI)));
						viewList.put("Menu", (ImageView) rot0NavButtons.findViewById(liparam.res.getIdentifier("menu", "id", CLASSNAME_SYSTEMUI)));
						viewList.put("Back", (ImageView) rot0NavButtons.findViewById(liparam.res.getIdentifier("back", "id", CLASSNAME_SYSTEMUI)));
						viewList.put("Recent", (ImageView) rot0NavButtons.findViewById(liparam.res.getIdentifier("recent_apps", "id", CLASSNAME_SYSTEMUI)));

						if (showSearch) {
							Drawable searchButtonDrawable = getSearchButton(useTheme, themeId, themeColor, false);
							viewList.put(
									"Search",
									createButtonView(liparam, buttonWidth, LinearLayout.LayoutParams.FILL_PARENT, "ic_sysbar_highlight", searchButtonDrawable,
											KeyEvent.KEYCODE_SEARCH, "Search", true));
						}

						rot0NavButtons.removeAllViews();
						// add selected buttons
						for (int i = 0; i < buttonsCount; i++) {
							ImageView view = viewList.remove(orderList[i]);
							if (view != null) {
								view.setVisibility(View.VISIBLE);
								view.setLayoutParams(new LinearLayout.LayoutParams(buttonWidth, LinearLayout.LayoutParams.FILL_PARENT, 0.0f));
								view.setPadding(0, 0, 0, 0);
								view.setScaleType(ScaleType.FIT_CENTER);
								rot0NavButtons.addView(view);
							}
						}

						// add unselected buttons and make them invisible
						for (ImageView view : viewList.values()) {
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
						// determine if a tablet is in use
						boolean tabletMode = rot90NavButtons.getOrientation() == LinearLayout.HORIZONTAL;

						if (tabletMode)
							rot90NavButtons.setGravity(Gravity.CENTER_HORIZONTAL);

						Map<String, ImageView> viewList = new HashMap<String, ImageView>();

						viewList.put("Back", (ImageView) rot90NavButtons.findViewById(liparam.res.getIdentifier("back", "id", CLASSNAME_SYSTEMUI)));
						viewList.put("Home", (ImageView) rot90NavButtons.findViewById(liparam.res.getIdentifier("home", "id", CLASSNAME_SYSTEMUI)));
						viewList.put("Recent", (ImageView) rot90NavButtons.findViewById(liparam.res.getIdentifier("recent_apps", "id", CLASSNAME_SYSTEMUI)));
						viewList.put("Menu", (ImageView) rot90NavButtons.findViewById(liparam.res.getIdentifier("menu", "id", CLASSNAME_SYSTEMUI)));
						if (showSearch) {
							Drawable searchButtonDrawable = getSearchButton(useTheme, themeId, themeColor, !tabletMode);
							viewList.put(
									"Search",
									createButtonView(liparam, buttonWidth, LinearLayout.LayoutParams.FILL_PARENT, tabletMode ? "ic_sysbar_highlight"
											: "ic_sysbar_highlight_land", searchButtonDrawable, KeyEvent.KEYCODE_SEARCH, "Search", true));
						}

						rot90NavButtons.removeAllViews();
						// add selected buttons
						if (tabletMode)
							for (int i = 0; i < buttonsCount; i++) {
								ImageView view = viewList.remove(orderList[i]);
								if (view != null) {
									view.setVisibility(View.VISIBLE);
									view.setLayoutParams(new LinearLayout.LayoutParams(buttonWidth, LinearLayout.LayoutParams.FILL_PARENT, 0.0f));
									view.setPadding(0, 0, 0, 0);
									view.setScaleType(ScaleType.FIT_CENTER);
									rot90NavButtons.addView(view);
								}
							}
						else
							for (int i = buttonsCount - 1; i >= 0; i--) {
								ImageView view = viewList.remove(orderList[i]);
								if (view != null) {
									view.setVisibility(View.VISIBLE);
									view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, buttonWidth, 0.0f));
									view.setPadding(0, 0, 0, 0);
									view.setScaleType(ScaleType.FIT_CENTER);
									rot90NavButtons.addView(view);
								}
							}

						// add unselected buttons and make them invisible
						for (ImageView view : viewList.values()) {
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

		// hook setDisabledFlags method
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
							final View searchButton = mCurrentView.findViewWithTag("Search");
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

	KeyButtonView createButtonView(LayoutInflatedParam liparam, int width, int height, String glowBgResName, Drawable img, int code, String tag,
			boolean supportLongpress) {
		KeyButtonView view = new KeyButtonView(mContext, code, supportLongpress, liparam.res.getDrawable(liparam.res.getIdentifier(glowBgResName, "drawable",
				CLASSNAME_SYSTEMUI)));
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height, 0.0f);
		view.setLayoutParams(lp);
		view.setImageDrawable(img);
		view.setScaleType(ScaleType.CENTER);
		view.setTag(tag);
		return view;
	}

	void replaceCustomButton(XResources res, String resName, String type, boolean isAlt, boolean landscape) {
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
				drawable = modRes.getDrawable(mThemeIcons.getIconResId(themeId, themeColor, "Search", false, landscape));
		} else {
			drawable = modRes.getDrawable(landscape ? R.drawable.ic_sysbar_search_land : R.drawable.ic_sysbar_search);
		}

		return drawable;
	}
}
