package com.gzplanet.xposed.xperianavbarbuttons;


public class ButtonAttr {
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
		if (getClass() != obj.getClass())
			return false;
		ButtonAttr other = (ButtonAttr) obj;
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

}
