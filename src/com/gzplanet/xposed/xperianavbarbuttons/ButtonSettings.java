package com.gzplanet.xposed.xperianavbarbuttons;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

public class ButtonSettings {
	private boolean mShowMenu = false;
	private boolean mShowSearch = false;
	private boolean mShowRecent = false;
	private boolean mShowSeparator = false;
	private ArrayList<String> mOrder = new ArrayList<String>();
	private Map<String, Drawable> mStockButtons = new HashMap<String, Drawable>();

	public ButtonSettings(Context context, String orderList) {
		// prepare preview panel
		PackageManager pm = context.getPackageManager();
		String cacheFolder = null;
		try {
			cacheFolder = context.getExternalCacheDir().getAbsolutePath()
					.replace(context.getPackageName(), XperiaNavBarButtons.CLASSNAME_SYSTEMUI);
		} catch (Exception e) {
		}

		try {
			Resources resSystemUI = pm.getResourcesForApplication(XperiaNavBarButtons.CLASSNAME_SYSTEMUI);

			BitmapFactory.Options options = new BitmapFactory.Options();
			DisplayMetrics metrics = resSystemUI.getDisplayMetrics();
//			options.inDensity = 240;
			options.inDensity = metrics.densityDpi;
			options.inTargetDensity = metrics.densityDpi;

			mStockButtons.put("Home", getStockButtonDrawable(resSystemUI, "ic_sysbar_home", cacheFolder, options));
			mStockButtons.put("Back", getStockButtonDrawable(resSystemUI, "ic_sysbar_back", cacheFolder, options));
			mStockButtons.put("Recent", getStockButtonDrawable(resSystemUI, "ic_sysbar_recent", cacheFolder, options));
			mStockButtons.put("Menu", getStockButtonDrawable(resSystemUI, "ic_sysbar_menu", cacheFolder, options));
			mStockButtons.put("Search", context.getResources().getDrawable(R.drawable.ic_sysbar_search));
		} catch (NameNotFoundException e1) {
		}

		if (orderList == null) {
			mOrder.add("Home");
			mOrder.add("Menu");
			mOrder.add("Recent");
			mOrder.add("Back");
			mOrder.add("Search");
			mShowMenu = true;
			mShowSearch = true;
			mShowRecent = true;
			mShowSeparator = false;
		} else {
			String[] array = orderList.split(",");
			for (int i = 0; i < array.length; i++) {
				mOrder.add(array[i]);

				if ("Menu".equals(array[i]))
					mShowMenu = true;
				if ("Search".equals(array[i]))
					mShowSearch = true;
				if ("Recent".equals(array[i]))
					mShowRecent = true;
				if ("Separator".equals(array[i]))
					mShowSeparator = true;
			}
		}
	}

	public boolean isShowRecent() {
		return mShowRecent;
	}

	public void setShowRecent(boolean showRecent) {
		mShowRecent = showRecent;
		if (mShowRecent)
			addButton("Recent");
		else
			removeButton("Recent");
	}

	public boolean isShowMenu() {
		return mShowMenu;
	}

	public void setShowMenu(boolean showMenu) {
		mShowMenu = showMenu;
		if (mShowMenu)
			addButton("Menu");
		else
			removeButton("Menu");
	}

	public boolean isShowSearch() {
		return mShowSearch;
	}

	public void setShowSearch(boolean showSearch) {
		mShowSearch = showSearch;
		if (mShowSearch)
			addButton("Search");
		else
			removeButton("Search");
	}

	public boolean isShowSeparator() {
		return mShowSeparator;
	}

	public void setShowSeparator(boolean showSeparator) {
		mShowSeparator = showSeparator;
		if (mShowSeparator)
			addButton("Separator");
		else
			removeButton("Separator");
	}

	private void removeButton(String button) {
		int pos = mOrder.indexOf(button);
		if (pos >= 0)
			mOrder.remove(pos);
	}

	private void addButton(String button) {
		int pos = mOrder.indexOf(button);
		if (pos == -1)
			mOrder.add(button);
	}

	public String getOrderListString() {
		StringBuilder list = new StringBuilder();

		for (int i = 0; i < mOrder.size(); i++) {
			list.append(mOrder.get(i));
			if (i != mOrder.size() - 1)
				list.append(",");
		}

		return list.toString();
	}

	public Drawable getButtonDrawable(int index) {
		if (index >= mOrder.size())
			return null;

		return mStockButtons.get(mOrder.get(index));
	}

	public String getButtonName(int index) {
		if (index >= mOrder.size())
			return null;

		return mOrder.get(index);
	}

	/*
	 * get stock button drawable either from 1. cache folder 2. SystemUI
	 * resources
	 */
	private Drawable getStockButtonDrawable(Resources res, String resName, String cacheFolder,
			BitmapFactory.Options options) {

		if (cacheFolder != null) {
			Bitmap bitmap = BitmapFactory.decodeFile(cacheFolder + File.separator + resName + ".png", options);
			if (bitmap != null) {
				Drawable drawable = new BitmapDrawable(res, bitmap);

				if (drawable.getIntrinsicHeight() > 0 && drawable.getIntrinsicWidth() > 0)
					return drawable;
			}
		}

		return res.getDrawable(res.getIdentifier(resName, "drawable", XperiaNavBarButtons.CLASSNAME_SYSTEMUI));
	}
}
