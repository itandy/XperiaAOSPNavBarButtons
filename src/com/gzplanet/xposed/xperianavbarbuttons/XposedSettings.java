package com.gzplanet.xposed.xperianavbarbuttons;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import sheetrock.panda.changelog.ChangeLog;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class XposedSettings extends PreferenceActivity {

	int mScreenWidth;
	int mButtonWidth;
	int mButtonsCount = 2;
	boolean mShowMenu;
	boolean mShowSearch;
	boolean mShowRecent;
	String mThemeId;
	String mThemeColor;
	boolean mUseTheme;
	boolean mUseAltMenu;
	String mCacheFolder;
	int mDensityDpi;

	CheckBoxPreference mPrefShowRecent;
	CheckBoxPreference mPrefShowMenu;
	CheckBoxPreference mPrefShowSearch;
	Preference mPrefRestartSystemUI;
	Preference mPrefReorder;
	Preference mPrefTheme;
	CheckBoxPreference mPrefUseAltMenu;
	Preference mPrefHints;

	ButtonSettings mSettings;
	ThemeIcons mThemeIcons = new ThemeIcons();
	CustomButtons mCustomButtons;

	static int[] mIconId = { R.id.iv1, R.id.iv2, R.id.iv3, R.id.iv4, R.id.iv5 };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTitle(R.string.app_name);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		addPreferencesFromResource(R.xml.preferences);

		// get screen width
		mScreenWidth = Utils.getScreenWidth(this);

		mPrefShowRecent = (CheckBoxPreference) findPreference("pref_show_recent");
		mPrefShowMenu = (CheckBoxPreference) findPreference("pref_show_menu");
		mPrefShowSearch = (CheckBoxPreference) findPreference("pref_show_search");
		mPrefReorder = (Preference) findPreference("pref_reorder");
		mPrefTheme = (Preference) findPreference("pref_theme");
		mPrefRestartSystemUI = (Preference) findPreference("pref_restart_systemui");
		mPrefUseAltMenu = (CheckBoxPreference) findPreference("pref_use_alt_menu");
		mPrefHints = (Preference) findPreference("pref_hints");

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

		mCacheFolder = Utils.getSystemUICacheFolder(this);
		mDensityDpi = Utils.getDensityDpi(getResources());
		mCustomButtons = new CustomButtons(mCacheFolder, mDensityDpi, "custom_");

		storePreferences();

		updatePreviewPanel();

		// this is important because although the handler classes that read
		// these settings
		// are in the same package, they are executed in the context of the
		// hooked package
		getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);

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
				return true;
			}
		});

		mPrefReorder.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent(XposedSettings.this, ReorderActivity.class);
				intent.putExtra("order_list", mSettings.getOrderListString());
				startActivityForResult(intent, 1);
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
				startActivityForResult(intent, 2);
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

		mPrefRestartSystemUI.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				try {
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

		// display change log
		ChangeLog cl = new ChangeLog(this);
		if (cl.firstRun())
	        cl.getFullLogDialog().show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
			case 1:
				String order = data.getStringExtra("order_list");
				getPreferenceManager().getSharedPreferences().edit().putString("pref_order", order).commit();
				mSettings = new ButtonSettings(this, order);
				updatePreviewPanel();
				break;
			case 2:
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
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void updatePreviewPanel() {
		mButtonWidth = Math.round((float) mScreenWidth / (float) mButtonsCount);

		// get default navbar height
		int navBarHeight = Utils.getNavBarHeight(getResources());

		LinearLayout panel = (LinearLayout) findViewById(R.id.previewPanel);
		if (navBarHeight > 0)
			panel.getLayoutParams().height = navBarHeight;
		for (int i = 0; i < 5; i++) {
			ImageView iv = (ImageView) panel.findViewById(mIconId[i]);
			if (i < mButtonsCount) {
				boolean useAlt = false;

				iv.setLayoutParams(new LinearLayout.LayoutParams(mButtonWidth, LinearLayout.LayoutParams.FILL_PARENT,
						0.0f));
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
	}

	// store necesary info for user define theme to work
	private void storePreferences() {
		Editor editor = getPreferenceManager().getSharedPreferences().edit();

		editor.putString("pref_cache_folder", mCacheFolder);
		editor.putInt("pref_density_dpi", mDensityDpi);
		editor.commit();
	}
}