package com.gzplanet.xposed.xperianavbarbuttons;

import java.util.ArrayList;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;


public class AppListPreference extends ListPreference {
	private int mClickedDialogEntryIndex;
	private Drawable[] mEntryIcons;
	private ArrayList<Item> mItems = new ArrayList<Item>();
	private ListAdapter mAdapter;

	class Item {
		private final String mDisplayText;
		private final String mValueText;
		private final Drawable mIcon;

		public Item(String displayText, String valueText, Drawable icon) {
			mDisplayText = displayText;
			mValueText = valueText;
			mIcon = icon;
		}

		public String getDisplayText() {
			return mDisplayText;
		}

		public String getValueText() {
			return mValueText;
		}

		public Drawable getIcon() {
			return mIcon;
		}
	}

	public AppListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void setEntryIcons(Drawable[] icons) {
		mEntryIcons = icons;
	}

	public void show() {
		mItems.clear();
		for (int i = 0; i < getEntries().length; i++) {
			mItems.add(new Item(getEntries()[i].toString(), getEntryValues()[i].toString(), mEntryIcons[i]));
		}
		mAdapter = new ArrayAdapter<Item>(getContext(), android.R.layout.select_dialog_singlechoice, android.R.id.text1, mItems) {
			public View getView(int position, View convertView, ViewGroup parent) {
				// User super class to create the View
				View v = super.getView(position, convertView, parent);
				TextView tv = (TextView) v.findViewById(android.R.id.text1);
				
				tv.setText(mItems.get(position).getDisplayText());

				// Put the image on the TextView
				tv.setCompoundDrawablesWithIntrinsicBounds(mItems.get(position).getIcon(), null, null, null);

				// Add margin between image and text (support various screen
				// densities)
				int dp5 = (int) (5 * getContext().getResources().getDisplayMetrics().density + 0.5f);
				tv.setCompoundDrawablePadding(dp5);

				return v;
			}
		};
		showDialog(null);
	}

	@Override
	protected void onPrepareDialogBuilder(Builder builder) {
		super.onPrepareDialogBuilder(builder);
		
		if (getEntries() == null || getEntryValues() == null) {
			throw new IllegalStateException("ListPreference requires an entries array and an entryValues array.");
		}

		mClickedDialogEntryIndex = findIndexOfValue(getValue());
		builder.setSingleChoiceItems(mAdapter, mClickedDialogEntryIndex, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mClickedDialogEntryIndex = which;

				/*
				 * Clicking on an item simulates the positive button click, and
				 * dismisses the dialog.
				 */
				AppListPreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
				dialog.dismiss();
			}
		});

		/*
		 * The typical interaction for list-based dialogs is to have
		 * click-on-an-item dismiss the dialog instead of the user having to
		 * press 'Ok'.
		 */
		builder.setPositiveButton(null, null);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		
		if (positiveResult && mClickedDialogEntryIndex >= 0 && getEntryValues() != null) {
			String value = getEntryValues()[mClickedDialogEntryIndex].toString();
			if (callChangeListener(value)) {
				setValue(value);
			}
		}
	}
}
