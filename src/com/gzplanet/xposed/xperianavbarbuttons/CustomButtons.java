package com.gzplanet.xposed.xperianavbarbuttons;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class CustomButtons {
	public final static String FILENAME_PREFIX = "custom_";
	private final static String[] MAIN_BUTTONS = { "Home", "Back", "Recent", "Menu", "Search" };
	private String mFolder;
	private String mFilenamePrefix;
	private Map<ButtonAttr, String> mCustomButtons = new HashMap<ButtonAttr, String>();
	BitmapFactory.Options mOptions;

	public CustomButtons(String folder, int densityDpi, String filenamePrefix) {
		mFolder = folder;
		mFilenamePrefix = filenamePrefix;

		// create cache folder if not exists
		File dir = new File(mFolder);
		if (!dir.exists())
			dir.mkdirs();

		mOptions = new BitmapFactory.Options();
		mOptions.inDensity = 240;
		mOptions.inTargetDensity = densityDpi;

		refresh();
	}

	private void add(String type, boolean isAlt, boolean landscape, String filename) {
		File file = new File(mFolder + File.separator + mFilenamePrefix + filename);
		if (file.exists())
			mCustomButtons.put(new ButtonAttr(type, isAlt, landscape), filename);
	}
	
	public void refresh() {
		mCustomButtons.clear();
		add("Back", false, false, "ic_sysbar_back.png");
		add("Back", true, false, "ic_sysbar_back_ime.png");
		add("Back", false, true, "ic_sysbar_back_land.png");
		add("Back", true, true, "ic_sysbar_back_ime_land.png");
		add("Home", false, false, "ic_sysbar_home.png");
		add("Home", false, true, "ic_sysbar_home_land.png");
		add("Menu", false, false, "ic_sysbar_menu.png");
		add("Menu", true, false, "ic_sysbar_menu_alt.png");
		add("Menu", false, true, "ic_sysbar_menu_land.png");
		add("Menu", true, true, "ic_sysbar_menu_alt_land.png");
		add("Recent", false, false, "ic_sysbar_recent.png");
		add("Recent", false, true, "ic_sysbar_recent_land.png");
		add("Search", false, false, "ic_sysbar_search.png");
		add("Search", false, true, "ic_sysbar_search_land.png");
	}

	public int getCount() {
		return mCustomButtons.size();
	}

	public Bitmap getBitmap(String type, boolean isAlt, boolean landscape) {
		ButtonAttr buttonAttr = new ButtonAttr(type, isAlt, landscape);
		if (!mCustomButtons.containsKey(buttonAttr))
			return null;

		File file = new File(mFolder + File.separator + mFilenamePrefix + mCustomButtons.get(buttonAttr));
		if (!file.exists())
			return null;

		return BitmapFactory.decodeFile(file.getAbsolutePath(), mOptions);
	}

	public ArrayList<String> getMainButtonNames() {
		ArrayList<String> list = new ArrayList<String>();

		for (String type : MAIN_BUTTONS) {
			if (mCustomButtons.containsKey(new ButtonAttr(type, false, false)))
				list.add(type);
		}

		return list;
	}

	public ArrayList<Bitmap> getMainButtonBitmap() {
		ArrayList<Bitmap> list = new ArrayList<Bitmap>();

		for (String type : MAIN_BUTTONS) {
			if (mCustomButtons.containsKey(new ButtonAttr(type, false, false)))
				list.add(getBitmap(type, false, false));
		}

		return list;
	}

	public int getMainButtonsCount() {
		int count = 0;
		for (String type : MAIN_BUTTONS) {
			if (mCustomButtons.containsKey(new ButtonAttr(type, false, false)))
				count++;
		}
		return count;
	}

	public int importIntoCacheFolder(String cacheFolder) {
		int count = 0;
		for (String filename : mCustomButtons.values()) {
			try {
				Utils.copyFile(mFolder + File.separator + mFilenamePrefix + filename, cacheFolder + File.separator
						+ FILENAME_PREFIX + filename);
				count++;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return count;
	}
}
