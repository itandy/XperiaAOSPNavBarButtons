package com.gzplanet.xposed.xperianavbarbuttons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ThemeIcons {
	private Map<String, ThemeAttr> mThemeList = new HashMap<String, ThemeAttr>();
	private Map<SetKey, IconSet> mThemeIcons = new HashMap<SetKey, IconSet>();
	private Map<String, Integer> mColorMap = new HashMap<String, Integer>();

	private class ThemeAttr {
		private int mExampleId;
		private ArrayList<String> mColorList = new ArrayList<String>();

		public ThemeAttr(int exampleId, String color) {
			mExampleId = exampleId;
			mColorList.add(color);
		}

		public int getExampleId() {
			return mExampleId;
		}

		public void addColor(String color) {
			if (mColorList.indexOf(color) == -1)
				mColorList.add(color);
		}

		public ArrayList<String> getColorList() {
			return mColorList;
		}
	}

	private class SetKey {
		private String mThemeId;
		private String mColor;

		public SetKey(String themeId, String color) {
			super();
			this.mThemeId = themeId;
			this.mColor = color;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((mColor == null) ? 0 : mColor.hashCode());
			result = prime * result + ((mThemeId == null) ? 0 : mThemeId.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof SetKey))
				return false;
			SetKey other = (SetKey) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (mColor == null) {
				if (other.mColor != null)
					return false;
			} else if (!mColor.equals(other.mColor))
				return false;
			if (mThemeId == null) {
				if (other.mThemeId != null)
					return false;
			} else if (!mThemeId.equals(other.mThemeId))
				return false;
			return true;
		}

		private ThemeIcons getOuterType() {
			return ThemeIcons.this;
		}
	}

	private class IconSet {
		private Map<ButtonAttr, Integer> mButtonRes = new HashMap<ButtonAttr, Integer>();

		private class ButtonAttr {
			private String mType;
			private boolean mIsAlt;
			private boolean mLandscape;

			public ButtonAttr(String type, boolean isAlt, boolean landscape) {
				mType = type;
				mIsAlt = isAlt;
				mLandscape = landscape;
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + getOuterType().hashCode();
				result = prime * result + (mIsAlt ? 1231 : 1237);
				result = prime * result + (mLandscape ? 1231 : 1237);
				result = prime * result + ((mType == null) ? 0 : mType.hashCode());
				return result;
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (!(obj instanceof ButtonAttr))
					return false;
				ButtonAttr other = (ButtonAttr) obj;
				if (!getOuterType().equals(other.getOuterType()))
					return false;
				if (mIsAlt != other.mIsAlt)
					return false;
				if (mLandscape != other.mLandscape)
					return false;
				if (mType == null) {
					if (other.mType != null)
						return false;
				} else if (!mType.equals(other.mType))
					return false;
				return true;
			}

			private IconSet getOuterType() {
				return IconSet.this;
			}

		}

		public IconSet(String themeId, String color, int back, int backIme, int backLand, int backImeLand, int home, int homeLand, int menu, int menuLand,
				int menuBig, int menuBigLand, int recent, int recentLand, int search, int searchLand) {
			super();

			mButtonRes.put(new ButtonAttr("Back", false, false), back);
			mButtonRes.put(new ButtonAttr("Back", true, false), backIme);
			mButtonRes.put(new ButtonAttr("Back", false, true), backLand);
			mButtonRes.put(new ButtonAttr("Back", true, true), backImeLand);
			mButtonRes.put(new ButtonAttr("Home", false, false), home);
			mButtonRes.put(new ButtonAttr("Home", false, true), homeLand);
			mButtonRes.put(new ButtonAttr("Menu", false, false), menu);
			mButtonRes.put(new ButtonAttr("Menu", false, true), menuLand);
			mButtonRes.put(new ButtonAttr("Menu", true, false), menuBig);
			mButtonRes.put(new ButtonAttr("Menu", true, true), menuBigLand);
			mButtonRes.put(new ButtonAttr("Recent", false, false), recent);
			mButtonRes.put(new ButtonAttr("Recent", false, true), recentLand);
			mButtonRes.put(new ButtonAttr("Search", false, false), search);
			mButtonRes.put(new ButtonAttr("Search", false, true), searchLand);
		}

		public int getIcon(String type, boolean isAlt, boolean landscape) {
			return mButtonRes.get(new ButtonAttr(type, isAlt, landscape));
		}
	}

	public ThemeIcons() {
		// fill up theme icons
		add("Stock", "White", R.drawable.stock_example, R.drawable.stock_ic_sysbar_back, R.drawable.stock_ic_sysbar_back_ime,
				R.drawable.stock_ic_sysbar_back_land, R.drawable.stock_ic_sysbar_back_ime, R.drawable.stock_ic_sysbar_home,
				R.drawable.stock_ic_sysbar_home_land, R.drawable.stock_ic_sysbar_menu_big, R.drawable.stock_ic_sysbar_menu_big_land,
				R.drawable.stock_ic_sysbar_menu, R.drawable.stock_ic_sysbar_menu_land, R.drawable.stock_ic_sysbar_recent,
				R.drawable.stock_ic_sysbar_recent_land, R.drawable.stock_ic_sysbar_search, R.drawable.stock_ic_sysbar_search_land);

		add("Stock", "Blue", R.drawable.stock_example, R.drawable.stockblue_ic_sysbar_back, R.drawable.stockblue_ic_sysbar_back_ime,
				R.drawable.stockblue_ic_sysbar_back_land, R.drawable.stockblue_ic_sysbar_back_ime, R.drawable.stockblue_ic_sysbar_home,
				R.drawable.stockblue_ic_sysbar_home_land, R.drawable.stockblue_ic_sysbar_menu_big, R.drawable.stockblue_ic_sysbar_menu_big_land,
				R.drawable.stockblue_ic_sysbar_menu, R.drawable.stockblue_ic_sysbar_menu_land, R.drawable.stockblue_ic_sysbar_recent,
				R.drawable.stockblue_ic_sysbar_recent_land, R.drawable.stockblue_ic_sysbar_search, R.drawable.stockblue_ic_sysbar_search_land);

		add("Stock", "Green", R.drawable.stock_example, R.drawable.stockgreen_ic_sysbar_back, R.drawable.stockgreen_ic_sysbar_back_ime,
				R.drawable.stockgreen_ic_sysbar_back_land, R.drawable.stockgreen_ic_sysbar_back_ime, R.drawable.stockgreen_ic_sysbar_home,
				R.drawable.stockgreen_ic_sysbar_home_land, R.drawable.stockgreen_ic_sysbar_menu_big, R.drawable.stockgreen_ic_sysbar_menu_big_land,
				R.drawable.stockgreen_ic_sysbar_menu, R.drawable.stockgreen_ic_sysbar_menu_land, R.drawable.stockgreen_ic_sysbar_recent,
				R.drawable.stockgreen_ic_sysbar_recent_land, R.drawable.stockgreen_ic_sysbar_search, R.drawable.stockgreen_ic_sysbar_search_land);

		add("Stock", "Pink", R.drawable.stock_example, R.drawable.stockpink_ic_sysbar_back, R.drawable.stockpink_ic_sysbar_back_ime,
				R.drawable.stockpink_ic_sysbar_back_land, R.drawable.stockpink_ic_sysbar_back_ime, R.drawable.stockpink_ic_sysbar_home,
				R.drawable.stockpink_ic_sysbar_home_land, R.drawable.stockpink_ic_sysbar_menu_big, R.drawable.stockpink_ic_sysbar_menu_big_land,
				R.drawable.stockpink_ic_sysbar_menu, R.drawable.stockpink_ic_sysbar_menu_land, R.drawable.stockpink_ic_sysbar_recent,
				R.drawable.stockpink_ic_sysbar_recent_land, R.drawable.stockpink_ic_sysbar_search, R.drawable.stockpink_ic_sysbar_search_land);

		add("Stock", "Red", R.drawable.stock_example, R.drawable.stockred_ic_sysbar_back, R.drawable.stockred_ic_sysbar_back_ime,
				R.drawable.stockred_ic_sysbar_back_land, R.drawable.stockred_ic_sysbar_back_ime, R.drawable.stockred_ic_sysbar_home,
				R.drawable.stockred_ic_sysbar_home_land, R.drawable.stockred_ic_sysbar_menu_big, R.drawable.stockred_ic_sysbar_menu_big_land,
				R.drawable.stockred_ic_sysbar_menu, R.drawable.stockred_ic_sysbar_menu_land, R.drawable.stockred_ic_sysbar_recent,
				R.drawable.stockred_ic_sysbar_recent_land, R.drawable.stockred_ic_sysbar_search, R.drawable.stockred_ic_sysbar_search_land);

		add("Stock", "Yellow", R.drawable.stock_example, R.drawable.stockyellow_ic_sysbar_back, R.drawable.stockyellow_ic_sysbar_back_ime,
				R.drawable.stockyellow_ic_sysbar_back_land, R.drawable.stockyellow_ic_sysbar_back_ime, R.drawable.stockyellow_ic_sysbar_home,
				R.drawable.stockyellow_ic_sysbar_home_land, R.drawable.stockyellow_ic_sysbar_menu_big, R.drawable.stockyellow_ic_sysbar_menu_big_land,
				R.drawable.stockyellow_ic_sysbar_menu, R.drawable.stockyellow_ic_sysbar_menu_land, R.drawable.stockyellow_ic_sysbar_recent,
				R.drawable.stockyellow_ic_sysbar_recent_land, R.drawable.stockyellow_ic_sysbar_search, R.drawable.stockyellow_ic_sysbar_search_land);

		add("Airbrush", "White", R.drawable.airbrush_example, R.drawable.airbrush_ic_sysbar_back, R.drawable.airbrush_ic_sysbar_back_ime,
				R.drawable.airbrush_ic_sysbar_back_land, R.drawable.airbrush_ic_sysbar_back_ime, R.drawable.airbrush_ic_sysbar_home,
				R.drawable.airbrush_ic_sysbar_home_land, R.drawable.airbrush_ic_sysbar_menu_big, R.drawable.airbrush_ic_sysbar_menu_big_land,
				R.drawable.airbrush_ic_sysbar_menu, R.drawable.airbrush_ic_sysbar_menu_land, R.drawable.airbrush_ic_sysbar_recent,
				R.drawable.airbrush_ic_sysbar_recent_land, R.drawable.airbrush_ic_sysbar_search, R.drawable.airbrush_ic_sysbar_search_land);

		add("Airbrush", "Blue", R.drawable.airbrush_example, R.drawable.airbrushblue_ic_sysbar_back, R.drawable.airbrushblue_ic_sysbar_back_ime,
				R.drawable.airbrushblue_ic_sysbar_back_land, R.drawable.airbrushblue_ic_sysbar_back_ime, R.drawable.airbrushblue_ic_sysbar_home,
				R.drawable.airbrushblue_ic_sysbar_home_land, R.drawable.airbrushblue_ic_sysbar_menu_big, R.drawable.airbrushblue_ic_sysbar_menu_big_land,
				R.drawable.airbrushblue_ic_sysbar_menu, R.drawable.airbrushblue_ic_sysbar_menu_land, R.drawable.airbrushblue_ic_sysbar_recent,
				R.drawable.airbrushblue_ic_sysbar_recent_land, R.drawable.airbrushblue_ic_sysbar_search, R.drawable.airbrushblue_ic_sysbar_search_land);

		add("College", "White", R.drawable.college_example, R.drawable.college_ic_sysbar_back, R.drawable.college_ic_sysbar_back_ime,
				R.drawable.college_ic_sysbar_back_land, R.drawable.college_ic_sysbar_back_ime, R.drawable.college_ic_sysbar_home,
				R.drawable.college_ic_sysbar_home_land, R.drawable.college_ic_sysbar_menu_big, R.drawable.college_ic_sysbar_menu_big_land,
				R.drawable.college_ic_sysbar_menu, R.drawable.college_ic_sysbar_menu_land, R.drawable.college_ic_sysbar_recent,
				R.drawable.college_ic_sysbar_recent_land, R.drawable.college_ic_sysbar_search, R.drawable.college_ic_sysbar_search_land);

		add("College", "Blue", R.drawable.college_example, R.drawable.collegeblue_ic_sysbar_back, R.drawable.collegeblue_ic_sysbar_back_ime,
				R.drawable.collegeblue_ic_sysbar_back_land, R.drawable.collegeblue_ic_sysbar_back_ime, R.drawable.collegeblue_ic_sysbar_home,
				R.drawable.collegeblue_ic_sysbar_home_land, R.drawable.collegeblue_ic_sysbar_menu_big, R.drawable.collegeblue_ic_sysbar_menu_big_land,
				R.drawable.collegeblue_ic_sysbar_menu, R.drawable.collegeblue_ic_sysbar_menu_land, R.drawable.collegeblue_ic_sysbar_recent,
				R.drawable.collegeblue_ic_sysbar_recent_land, R.drawable.collegeblue_ic_sysbar_search, R.drawable.collegeblue_ic_sysbar_search_land);

		add("Defused", "White", R.drawable.defused_example, R.drawable.defused_ic_sysbar_back, R.drawable.defused_ic_sysbar_back_ime,
				R.drawable.defused_ic_sysbar_back_land, R.drawable.defused_ic_sysbar_back_ime, R.drawable.defused_ic_sysbar_home,
				R.drawable.defused_ic_sysbar_home_land, R.drawable.defused_ic_sysbar_menu_big, R.drawable.defused_ic_sysbar_menu_big_land,
				R.drawable.defused_ic_sysbar_menu, R.drawable.defused_ic_sysbar_menu_land, R.drawable.defused_ic_sysbar_recent,
				R.drawable.defused_ic_sysbar_recent_land, R.drawable.defused_ic_sysbar_search, R.drawable.defused_ic_sysbar_search_land);

		add("Defused", "Blue", R.drawable.defused_example, R.drawable.defusedblue_ic_sysbar_back, R.drawable.defusedblue_ic_sysbar_back_ime,
				R.drawable.defusedblue_ic_sysbar_back_land, R.drawable.defusedblue_ic_sysbar_back_ime, R.drawable.defusedblue_ic_sysbar_home,
				R.drawable.defusedblue_ic_sysbar_home_land, R.drawable.defusedblue_ic_sysbar_menu_big, R.drawable.defusedblue_ic_sysbar_menu_big_land,
				R.drawable.defusedblue_ic_sysbar_menu, R.drawable.defusedblue_ic_sysbar_menu_land, R.drawable.defusedblue_ic_sysbar_recent,
				R.drawable.defusedblue_ic_sysbar_recent_land, R.drawable.defusedblue_ic_sysbar_search, R.drawable.defusedblue_ic_sysbar_search_land);

		add("Droid", "White", R.drawable.droid_example, R.drawable.droid_ic_sysbar_back, R.drawable.droid_ic_sysbar_back_ime,
				R.drawable.droid_ic_sysbar_back_land, R.drawable.droid_ic_sysbar_back_ime, R.drawable.droid_ic_sysbar_home,
				R.drawable.droid_ic_sysbar_home_land, R.drawable.droid_ic_sysbar_menu_big, R.drawable.droid_ic_sysbar_menu_big_land,
				R.drawable.droid_ic_sysbar_menu, R.drawable.droid_ic_sysbar_menu_land, R.drawable.droid_ic_sysbar_recent,
				R.drawable.droid_ic_sysbar_recent_land, R.drawable.droid_ic_sysbar_search, R.drawable.droid_ic_sysbar_search_land);

		add("Droid", "Blue", R.drawable.droid_example, R.drawable.droidblue_ic_sysbar_back, R.drawable.droidblue_ic_sysbar_back_ime,
				R.drawable.droidblue_ic_sysbar_back_land, R.drawable.droidblue_ic_sysbar_back_ime, R.drawable.droidblue_ic_sysbar_home,
				R.drawable.droidblue_ic_sysbar_home_land, R.drawable.droidblue_ic_sysbar_menu_big, R.drawable.droidblue_ic_sysbar_menu_big_land,
				R.drawable.droidblue_ic_sysbar_menu, R.drawable.droidblue_ic_sysbar_menu_land, R.drawable.droidblue_ic_sysbar_recent,
				R.drawable.droidblue_ic_sysbar_recent_land, R.drawable.droidblue_ic_sysbar_search, R.drawable.droidblue_ic_sysbar_search_land);

		add("Elvish", "White", R.drawable.elvish_example, R.drawable.elvish_ic_sysbar_back, R.drawable.elvish_ic_sysbar_back_ime,
				R.drawable.elvish_ic_sysbar_back_land, R.drawable.elvish_ic_sysbar_back_ime, R.drawable.elvish_ic_sysbar_home,
				R.drawable.elvish_ic_sysbar_home_land, R.drawable.elvish_ic_sysbar_menu_big, R.drawable.elvish_ic_sysbar_menu_big_land,
				R.drawable.elvish_ic_sysbar_menu, R.drawable.elvish_ic_sysbar_menu_land, R.drawable.elvish_ic_sysbar_recent,
				R.drawable.elvish_ic_sysbar_recent_land, R.drawable.elvish_ic_sysbar_search, R.drawable.elvish_ic_sysbar_search_land);

		add("Elvish", "Blue", R.drawable.elvish_example, R.drawable.elvishblue_ic_sysbar_back, R.drawable.elvishblue_ic_sysbar_back_ime,
				R.drawable.elvishblue_ic_sysbar_back_land, R.drawable.elvishblue_ic_sysbar_back_ime, R.drawable.elvishblue_ic_sysbar_home,
				R.drawable.elvishblue_ic_sysbar_home_land, R.drawable.elvishblue_ic_sysbar_menu_big, R.drawable.elvishblue_ic_sysbar_menu_big_land,
				R.drawable.elvishblue_ic_sysbar_menu, R.drawable.elvishblue_ic_sysbar_menu_land, R.drawable.elvishblue_ic_sysbar_recent,
				R.drawable.elvishblue_ic_sysbar_recent_land, R.drawable.elvishblue_ic_sysbar_search, R.drawable.elvishblue_ic_sysbar_search_land);

		add("Facebook", "White", R.drawable.facebook_example, R.drawable.facebook_ic_sysbar_back, R.drawable.facebook_ic_sysbar_back_ime,
				R.drawable.facebook_ic_sysbar_back_land, R.drawable.facebook_ic_sysbar_back_ime, R.drawable.facebook_ic_sysbar_home,
				R.drawable.facebook_ic_sysbar_home_land, R.drawable.facebook_ic_sysbar_menu_big, R.drawable.facebook_ic_sysbar_menu_big_land,
				R.drawable.facebook_ic_sysbar_menu, R.drawable.facebook_ic_sysbar_menu_land, R.drawable.facebook_ic_sysbar_recent,
				R.drawable.facebook_ic_sysbar_recent_land, R.drawable.facebook_ic_sysbar_search, R.drawable.facebook_ic_sysbar_search_land);

		add("Facebook", "Blue", R.drawable.facebook_example, R.drawable.facebookblue_ic_sysbar_back, R.drawable.facebookblue_ic_sysbar_back_ime,
				R.drawable.facebookblue_ic_sysbar_back_land, R.drawable.facebookblue_ic_sysbar_back_ime, R.drawable.facebookblue_ic_sysbar_home,
				R.drawable.facebookblue_ic_sysbar_home_land, R.drawable.facebookblue_ic_sysbar_menu_big, R.drawable.facebookblue_ic_sysbar_menu_big_land,
				R.drawable.facebookblue_ic_sysbar_menu, R.drawable.facebookblue_ic_sysbar_menu_land, R.drawable.facebookblue_ic_sysbar_recent,
				R.drawable.facebookblue_ic_sysbar_recent_land, R.drawable.facebookblue_ic_sysbar_search, R.drawable.facebookblue_ic_sysbar_search_land);

		add("Galaxy", "White", R.drawable.galaxy_example, R.drawable.galaxy_ic_sysbar_back, R.drawable.galaxy_ic_sysbar_back_ime,
				R.drawable.galaxy_ic_sysbar_back_land, R.drawable.galaxy_ic_sysbar_back_ime, R.drawable.galaxy_ic_sysbar_home,
				R.drawable.galaxy_ic_sysbar_home_land, R.drawable.galaxy_ic_sysbar_menu_big, R.drawable.galaxy_ic_sysbar_menu_big_land,
				R.drawable.galaxy_ic_sysbar_menu, R.drawable.galaxy_ic_sysbar_menu_land, R.drawable.galaxy_ic_sysbar_recent,
				R.drawable.galaxy_ic_sysbar_recent_land, R.drawable.galaxy_ic_sysbar_search, R.drawable.galaxy_ic_sysbar_search_land);

		add("Galaxy", "Blue", R.drawable.galaxy_example, R.drawable.galaxyblue_ic_sysbar_back, R.drawable.galaxyblue_ic_sysbar_back_ime,
				R.drawable.galaxyblue_ic_sysbar_back_land, R.drawable.galaxyblue_ic_sysbar_back_ime, R.drawable.galaxyblue_ic_sysbar_home,
				R.drawable.galaxyblue_ic_sysbar_home_land, R.drawable.galaxyblue_ic_sysbar_menu_big, R.drawable.galaxyblue_ic_sysbar_menu_big_land,
				R.drawable.galaxyblue_ic_sysbar_menu, R.drawable.galaxyblue_ic_sysbar_menu_land, R.drawable.galaxyblue_ic_sysbar_recent,
				R.drawable.galaxyblue_ic_sysbar_recent_land, R.drawable.galaxyblue_ic_sysbar_search, R.drawable.galaxyblue_ic_sysbar_search_land);

		add("Nexus", "White", R.drawable.nexus_example, R.drawable.nexus_ic_sysbar_back, R.drawable.nexus_ic_sysbar_back_ime,
				R.drawable.nexus_ic_sysbar_back_land, R.drawable.nexus_ic_sysbar_back_ime, R.drawable.nexus_ic_sysbar_home,
				R.drawable.nexus_ic_sysbar_home_land, R.drawable.nexus_ic_sysbar_menu_big, R.drawable.nexus_ic_sysbar_menu_big_land,
				R.drawable.nexus_ic_sysbar_menu, R.drawable.nexus_ic_sysbar_menu_land, R.drawable.nexus_ic_sysbar_recent,
				R.drawable.nexus_ic_sysbar_recent_land, R.drawable.nexus_ic_sysbar_search, R.drawable.nexus_ic_sysbar_search_land);

		add("Nexus", "Blue", R.drawable.nexus_example, R.drawable.nexusblue_ic_sysbar_back, R.drawable.nexusblue_ic_sysbar_back_ime,
				R.drawable.nexusblue_ic_sysbar_back_land, R.drawable.nexusblue_ic_sysbar_back_ime, R.drawable.nexusblue_ic_sysbar_home,
				R.drawable.nexusblue_ic_sysbar_home_land, R.drawable.nexusblue_ic_sysbar_menu_big, R.drawable.nexusblue_ic_sysbar_menu_big_land,
				R.drawable.nexusblue_ic_sysbar_menu, R.drawable.nexusblue_ic_sysbar_menu_land, R.drawable.nexusblue_ic_sysbar_recent,
				R.drawable.nexusblue_ic_sysbar_recent_land, R.drawable.nexusblue_ic_sysbar_search, R.drawable.nexusblue_ic_sysbar_search_land);

		add("Pixel", "White", R.drawable.pixel_example, R.drawable.pixel_ic_sysbar_back, R.drawable.pixel_ic_sysbar_back_ime,
				R.drawable.pixel_ic_sysbar_back_land, R.drawable.pixel_ic_sysbar_back_ime, R.drawable.pixel_ic_sysbar_home,
				R.drawable.pixel_ic_sysbar_home_land, R.drawable.pixel_ic_sysbar_menu_big, R.drawable.pixel_ic_sysbar_menu_big_land,
				R.drawable.pixel_ic_sysbar_menu, R.drawable.pixel_ic_sysbar_menu_land, R.drawable.pixel_ic_sysbar_recent,
				R.drawable.pixel_ic_sysbar_recent_land, R.drawable.pixel_ic_sysbar_search, R.drawable.pixel_ic_sysbar_search_land);

		add("Pixel", "Blue", R.drawable.pixel_example, R.drawable.pixelblue_ic_sysbar_back, R.drawable.pixelblue_ic_sysbar_back_ime,
				R.drawable.pixelblue_ic_sysbar_back_land, R.drawable.pixelblue_ic_sysbar_back_ime, R.drawable.pixelblue_ic_sysbar_home,
				R.drawable.pixelblue_ic_sysbar_home_land, R.drawable.pixelblue_ic_sysbar_menu_big, R.drawable.pixelblue_ic_sysbar_menu_big_land,
				R.drawable.pixelblue_ic_sysbar_menu, R.drawable.pixelblue_ic_sysbar_menu_land, R.drawable.pixelblue_ic_sysbar_recent,
				R.drawable.pixelblue_ic_sysbar_recent_land, R.drawable.pixelblue_ic_sysbar_search, R.drawable.pixelblue_ic_sysbar_search_land);

		add("RAZR HD", "White", R.drawable.razrhd_example, R.drawable.razrhd_ic_sysbar_back, R.drawable.razrhd_ic_sysbar_back_ime,
				R.drawable.razrhd_ic_sysbar_back_land, R.drawable.razrhd_ic_sysbar_back_ime, R.drawable.razrhd_ic_sysbar_home,
				R.drawable.razrhd_ic_sysbar_home_land, R.drawable.razrhd_ic_sysbar_menu_big, R.drawable.razrhd_ic_sysbar_menu_big_land,
				R.drawable.razrhd_ic_sysbar_menu, R.drawable.razrhd_ic_sysbar_menu_land, R.drawable.razrhd_ic_sysbar_recent,
				R.drawable.razrhd_ic_sysbar_recent_land, R.drawable.razrhd_ic_sysbar_search, R.drawable.razrhd_ic_sysbar_search_land);

		add("RAZR HD", "Blue", R.drawable.razrhd_example, R.drawable.razrhdblue_ic_sysbar_back, R.drawable.razrhdblue_ic_sysbar_back_ime,
				R.drawable.razrhdblue_ic_sysbar_back_land, R.drawable.razrhdblue_ic_sysbar_back_ime, R.drawable.razrhdblue_ic_sysbar_home,
				R.drawable.razrhdblue_ic_sysbar_home_land, R.drawable.razrhdblue_ic_sysbar_menu_big, R.drawable.razrhdblue_ic_sysbar_menu_big_land,
				R.drawable.razrhdblue_ic_sysbar_menu, R.drawable.razrhdblue_ic_sysbar_menu_land, R.drawable.razrhdblue_ic_sysbar_recent,
				R.drawable.razrhdblue_ic_sysbar_recent_land, R.drawable.razrhdblue_ic_sysbar_search, R.drawable.razrhdblue_ic_sysbar_search_land);

		add("Russia", "White", R.drawable.russia_example, R.drawable.russia_ic_sysbar_back, R.drawable.russia_ic_sysbar_back_ime,
				R.drawable.russia_ic_sysbar_back_land, R.drawable.russia_ic_sysbar_back_ime, R.drawable.russia_ic_sysbar_home,
				R.drawable.russia_ic_sysbar_home_land, R.drawable.russia_ic_sysbar_menu_big, R.drawable.russia_ic_sysbar_menu_big_land,
				R.drawable.russia_ic_sysbar_menu, R.drawable.russia_ic_sysbar_menu_land, R.drawable.russia_ic_sysbar_recent,
				R.drawable.russia_ic_sysbar_recent_land, R.drawable.russia_ic_sysbar_search, R.drawable.russia_ic_sysbar_search_land);

		add("Russia", "Blue", R.drawable.russia_example, R.drawable.russiablue_ic_sysbar_back, R.drawable.russiablue_ic_sysbar_back_ime,
				R.drawable.russiablue_ic_sysbar_back_land, R.drawable.russiablue_ic_sysbar_back_ime, R.drawable.russiablue_ic_sysbar_home,
				R.drawable.russiablue_ic_sysbar_home_land, R.drawable.russiablue_ic_sysbar_menu_big, R.drawable.russiablue_ic_sysbar_menu_big_land,
				R.drawable.russiablue_ic_sysbar_menu, R.drawable.russiablue_ic_sysbar_menu_land, R.drawable.russiablue_ic_sysbar_recent,
				R.drawable.russiablue_ic_sysbar_recent_land, R.drawable.russiablue_ic_sysbar_search, R.drawable.russiablue_ic_sysbar_search_land);

		add("Stock Reflect", "White", R.drawable.stockreflect_example, R.drawable.stockreflect_ic_sysbar_back, R.drawable.stockreflect_ic_sysbar_back_ime,
				R.drawable.stockreflect_ic_sysbar_back_land, R.drawable.stockreflect_ic_sysbar_back_ime, R.drawable.stockreflect_ic_sysbar_home,
				R.drawable.stockreflect_ic_sysbar_home_land, R.drawable.stockreflect_ic_sysbar_menu_big, R.drawable.stockreflect_ic_sysbar_menu_big_land,
				R.drawable.stockreflect_ic_sysbar_menu, R.drawable.stockreflect_ic_sysbar_menu_land, R.drawable.stockreflect_ic_sysbar_recent,
				R.drawable.stockreflect_ic_sysbar_recent_land, R.drawable.stockreflect_ic_sysbar_search, R.drawable.stockreflect_ic_sysbar_search_land);

		add("Stock Reflect", "Blue", R.drawable.stockreflect_example, R.drawable.stockreflectblue_ic_sysbar_back,
				R.drawable.stockreflectblue_ic_sysbar_back_ime, R.drawable.stockreflectblue_ic_sysbar_back_land,
				R.drawable.stockreflectblue_ic_sysbar_back_ime, R.drawable.stockreflectblue_ic_sysbar_home, R.drawable.stockreflectblue_ic_sysbar_home_land,
				R.drawable.stockreflectblue_ic_sysbar_menu_big, R.drawable.stockreflectblue_ic_sysbar_menu_big_land,
				R.drawable.stockreflectblue_ic_sysbar_menu, R.drawable.stockreflectblue_ic_sysbar_menu_land, R.drawable.stockreflectblue_ic_sysbar_recent,
				R.drawable.stockreflectblue_ic_sysbar_recent_land, R.drawable.stockreflectblue_ic_sysbar_search,
				R.drawable.stockreflectblue_ic_sysbar_search_land);

		add("Stock Small", "White", R.drawable.stocksmall_example, R.drawable.stocksmall_ic_sysbar_back, R.drawable.stocksmall_ic_sysbar_back_ime,
				R.drawable.stocksmall_ic_sysbar_back_land, R.drawable.stocksmall_ic_sysbar_back_ime, R.drawable.stocksmall_ic_sysbar_home,
				R.drawable.stocksmall_ic_sysbar_home_land, R.drawable.stocksmall_ic_sysbar_menu_big, R.drawable.stocksmall_ic_sysbar_menu_big_land,
				R.drawable.stocksmall_ic_sysbar_menu, R.drawable.stocksmall_ic_sysbar_menu_land, R.drawable.stocksmall_ic_sysbar_recent,
				R.drawable.stocksmall_ic_sysbar_recent_land, R.drawable.stocksmall_ic_sysbar_search, R.drawable.stocksmall_ic_sysbar_search_land);

		add("Stock Small", "Blue", R.drawable.stocksmall_example, R.drawable.stocksmallblue_ic_sysbar_back, R.drawable.stocksmallblue_ic_sysbar_back_ime,
				R.drawable.stocksmallblue_ic_sysbar_back_land, R.drawable.stocksmallblue_ic_sysbar_back_ime, R.drawable.stocksmallblue_ic_sysbar_home,
				R.drawable.stocksmallblue_ic_sysbar_home_land, R.drawable.stocksmallblue_ic_sysbar_menu_big, R.drawable.stocksmallblue_ic_sysbar_menu_big_land,
				R.drawable.stocksmallblue_ic_sysbar_menu, R.drawable.stocksmallblue_ic_sysbar_menu_land, R.drawable.stocksmallblue_ic_sysbar_recent,
				R.drawable.stocksmallblue_ic_sysbar_recent_land, R.drawable.stocksmallblue_ic_sysbar_search, R.drawable.stocksmallblue_ic_sysbar_search_land);

		add("Stock Small Reflect", "White", R.drawable.stocksmallreflect_example, R.drawable.stocksmallreflect_ic_sysbar_back,
				R.drawable.stocksmallreflect_ic_sysbar_back_ime, R.drawable.stocksmallreflect_ic_sysbar_back_land,
				R.drawable.stocksmallreflect_ic_sysbar_back_ime, R.drawable.stocksmallreflect_ic_sysbar_home, R.drawable.stocksmallreflect_ic_sysbar_home_land,
				R.drawable.stocksmallreflect_ic_sysbar_menu_big, R.drawable.stocksmallreflect_ic_sysbar_menu_big_land,
				R.drawable.stocksmallreflect_ic_sysbar_menu, R.drawable.stocksmallreflect_ic_sysbar_menu_land, R.drawable.stocksmallreflect_ic_sysbar_recent,
				R.drawable.stocksmallreflect_ic_sysbar_recent_land, R.drawable.stocksmallreflect_ic_sysbar_search,
				R.drawable.stocksmallreflect_ic_sysbar_search_land);

		add("Stock Small Reflect", "Blue", R.drawable.stocksmallreflect_example, R.drawable.stocksmallreflectblue_ic_sysbar_back,
				R.drawable.stocksmallreflectblue_ic_sysbar_back_ime, R.drawable.stocksmallreflectblue_ic_sysbar_back_land,
				R.drawable.stocksmallreflectblue_ic_sysbar_back_ime, R.drawable.stocksmallreflectblue_ic_sysbar_home,
				R.drawable.stocksmallreflectblue_ic_sysbar_home_land, R.drawable.stocksmallreflectblue_ic_sysbar_menu_big,
				R.drawable.stocksmallreflectblue_ic_sysbar_menu_big_land, R.drawable.stocksmallreflectblue_ic_sysbar_menu,
				R.drawable.stocksmallreflectblue_ic_sysbar_menu_land, R.drawable.stocksmallreflectblue_ic_sysbar_recent,
				R.drawable.stocksmallreflectblue_ic_sysbar_recent_land, R.drawable.stocksmallreflectblue_ic_sysbar_search,
				R.drawable.stocksmallreflectblue_ic_sysbar_search_land);

		add("Xperia", "White", R.drawable.xperia_example, R.drawable.xperia_ic_sysbar_back, R.drawable.xperia_ic_sysbar_back_ime,
				R.drawable.xperia_ic_sysbar_back_land, R.drawable.xperia_ic_sysbar_back_ime, R.drawable.xperia_ic_sysbar_home,
				R.drawable.xperia_ic_sysbar_home_land, R.drawable.xperia_ic_sysbar_menu_big, R.drawable.xperia_ic_sysbar_menu_big_land,
				R.drawable.xperia_ic_sysbar_menu, R.drawable.xperia_ic_sysbar_menu_land, R.drawable.xperia_ic_sysbar_recent,
				R.drawable.xperia_ic_sysbar_recent_land, R.drawable.xperia_ic_sysbar_search, R.drawable.xperia_ic_sysbar_search_land);

		add("Xperia", "Blue", R.drawable.xperia_example, R.drawable.xperiablue_ic_sysbar_back, R.drawable.xperiablue_ic_sysbar_back_ime,
				R.drawable.xperiablue_ic_sysbar_back_land, R.drawable.xperiablue_ic_sysbar_back_ime, R.drawable.xperiablue_ic_sysbar_home,
				R.drawable.xperiablue_ic_sysbar_home_land, R.drawable.xperiablue_ic_sysbar_menu_big, R.drawable.xperiablue_ic_sysbar_menu_big_land,
				R.drawable.xperiablue_ic_sysbar_menu, R.drawable.xperiablue_ic_sysbar_menu_land, R.drawable.xperiablue_ic_sysbar_recent,
				R.drawable.xperiablue_ic_sysbar_recent_land, R.drawable.xperiablue_ic_sysbar_search, R.drawable.xperiablue_ic_sysbar_search_land);

		add("ZTE Grand", "White", R.drawable.ztegrand_example, R.drawable.ztegrand_ic_sysbar_back, R.drawable.ztegrand_ic_sysbar_back_ime,
				R.drawable.ztegrand_ic_sysbar_back_land, R.drawable.ztegrand_ic_sysbar_back_ime, R.drawable.ztegrand_ic_sysbar_home,
				R.drawable.ztegrand_ic_sysbar_home_land, R.drawable.ztegrand_ic_sysbar_menu_big, R.drawable.ztegrand_ic_sysbar_menu_big_land,
				R.drawable.ztegrand_ic_sysbar_menu, R.drawable.ztegrand_ic_sysbar_menu_land, R.drawable.ztegrand_ic_sysbar_recent,
				R.drawable.ztegrand_ic_sysbar_recent_land, R.drawable.ztegrand_ic_sysbar_search, R.drawable.ztegrand_ic_sysbar_search_land);

		add("ZTE Grand", "Blue", R.drawable.ztegrand_example, R.drawable.ztegrandblue_ic_sysbar_back, R.drawable.ztegrandblue_ic_sysbar_back_ime,
				R.drawable.ztegrandblue_ic_sysbar_back_land, R.drawable.ztegrandblue_ic_sysbar_back_ime, R.drawable.ztegrandblue_ic_sysbar_home,
				R.drawable.ztegrandblue_ic_sysbar_home_land, R.drawable.ztegrandblue_ic_sysbar_menu_big, R.drawable.ztegrandblue_ic_sysbar_menu_big_land,
				R.drawable.ztegrandblue_ic_sysbar_menu, R.drawable.ztegrandblue_ic_sysbar_menu_land, R.drawable.ztegrandblue_ic_sysbar_recent,
				R.drawable.ztegrandblue_ic_sysbar_recent_land, R.drawable.ztegrandblue_ic_sysbar_search, R.drawable.ztegrandblue_ic_sysbar_search_land);

		// fill up color map
		mColorMap.put("White", -1);
		mColorMap.put("Blue", -16730123);
		mColorMap.put("Green", -10027226);
		mColorMap.put("Pink", -54338);
		mColorMap.put("Purple", -7864165);
		mColorMap.put("Red", -63480);
		mColorMap.put("Yellow", -4341);
	}

	private void add(String themeId, String color, int example, int back, int backIme, int backLand, int backImeLand, int home, int homeLand, int menu,
			int menuLand, int menuBig, int menuBigLand, int recent, int recentLand, int search, int searchLand) {
		SetKey key = new SetKey(themeId, color);

		if (!mThemeList.containsKey(themeId))
			mThemeList.put(themeId, new ThemeAttr(example, color));
		else
			mThemeList.get(themeId).addColor(color);

		if (!mThemeIcons.containsKey(key))
			mThemeIcons.put(key, new IconSet(themeId, color, back, backIme, backLand, backImeLand, home, homeLand, menu, menuLand, menuBig, menuBigLand,
					recent, recentLand, search, searchLand));
	}

	public int getIconResId(String themeId, String color, String type, boolean isAlt, boolean landscape) {
		return mThemeIcons.get(new SetKey(themeId, color)).getIcon(type, isAlt, landscape);
	}

	public int getThemeExample(String themeId) {
		return mThemeList.get(themeId).getExampleId();
	}

	public Set<String> getThemeList() {
		return mThemeList.keySet();
	}

	public ArrayList<String> getThemeColorList(String themeId) {
		return mThemeList.get(themeId).getColorList();
	}

	public int getColorId(String color) {
		return mColorMap.get(color);
	}
}
