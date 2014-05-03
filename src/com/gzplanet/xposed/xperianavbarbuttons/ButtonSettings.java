package com.gzplanet.xposed.xperianavbarbuttons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

public class ButtonSettings {
	private boolean mShowMenu = false;
	private boolean mShowSearch = false;
	private boolean mShowRecent = false;
	private ArrayList<String> mOrder = new ArrayList<String>();
	private Map<String, Drawable> mStockButtons = new HashMap<String, Drawable>();

	public ButtonSettings(Context context, String orderList) {
		// prepare preview panel
		PackageManager pm = context.getPackageManager();
		try {
			Resources resSystemUI = pm.getResourcesForApplication(XperiaNavBarButtons.CLASSNAME_SYSTEMUI);

			mStockButtons.put("Home", resSystemUI.getDrawable(resSystemUI.getIdentifier("ic_sysbar_home", "drawable", XperiaNavBarButtons.CLASSNAME_SYSTEMUI)));
			mStockButtons.put("Back", resSystemUI.getDrawable(resSystemUI.getIdentifier("ic_sysbar_back", "drawable", XperiaNavBarButtons.CLASSNAME_SYSTEMUI)));
			mStockButtons.put("Recent",
					resSystemUI.getDrawable(resSystemUI.getIdentifier("ic_sysbar_recent", "drawable", XperiaNavBarButtons.CLASSNAME_SYSTEMUI)));
			mStockButtons.put("Menu", resSystemUI.getDrawable(resSystemUI.getIdentifier("ic_sysbar_menu", "drawable", XperiaNavBarButtons.CLASSNAME_SYSTEMUI)));
			mStockButtons.put("Search", context.getResources().getDrawable(R.drawable.ic_sysbar_search));
		} catch (NameNotFoundException e1) {
			e1.printStackTrace();
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
}
