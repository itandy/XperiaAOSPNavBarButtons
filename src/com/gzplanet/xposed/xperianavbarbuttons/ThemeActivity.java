package com.gzplanet.xposed.xperianavbarbuttons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

public class ThemeActivity extends Activity {
	static final int MAX_COLOR = 7;
	private String mThemeId;
	private String mThemeColor;
	private boolean mUseTheme;
	private int mColorViewWidth;

	ThemeIcons mThemeIcons = new ThemeIcons();
	ArrayList<String> mThemeList;
	Map<String, View> mColorList = new HashMap<String, View>();
	Bitmap mOverlay;

	ImageView mExample;
	ListView mList;
	ViewGroup mColorPanel;
	TextView mThemeLabel;
	Switch mSwitch;
	ViewGroup mRoot;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.theme);

		mColorViewWidth = Math.round((float) Utils.getScreenWidth(this) / (float) MAX_COLOR);

		mOverlay = Utils.decodeImageFromResource(getResources(), R.drawable.ic_imageview_checked, mColorViewWidth, mColorViewWidth);

		Intent intent = getIntent();
		mUseTheme = intent.getBooleanExtra("usetheme", false);
		mThemeId = intent.getStringExtra("themeid");
		mThemeColor = intent.getStringExtra("themecolor");

		mRoot = (ViewGroup) findViewById(R.id.theme_root);
		mSwitch = (Switch) findViewById(R.id.use_theme);
		mList = (ListView) findViewById(R.id.theme_list);
		mExample = (ImageView) findViewById(R.id.theme_example);
		mColorPanel = (ViewGroup) findViewById(R.id.color_panel);
		mThemeLabel = (TextView) findViewById(R.id.example_label);

		mThemeList = new ArrayList<String>(mThemeIcons.getThemeList());
		Collections.sort(mThemeList);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.theme_item, R.id.theme_name, mThemeList);
		mList.setAdapter(adapter);
		mList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
				mThemeId = mThemeList.get(position);
				mThemeColor = XperiaNavBarButtons.DEF_THEMECOLOR;
				refreshSelectedTheme();
			}
		});

		mSwitch.setChecked(mUseTheme);
		mSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mUseTheme = isChecked;
				refreshSelectedTheme();
			}

		});

		refreshSelectedTheme();
	}

	@Override
	public void onBackPressed() {
		Intent intent = new Intent();
		intent.putExtra("usetheme", mUseTheme);
		intent.putExtra("themeid", mThemeId);
		intent.putExtra("themecolor", mThemeColor);
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	public void onStop() {
		if (mOverlay != null) {
			mOverlay.recycle();
			mOverlay = null;
		}
		super.onStop();
	}

	private void refreshSelectedTheme() {
		toggleViewsEnable(mUseTheme);

		if (mUseTheme) {
			mThemeLabel.setText(getSelectedThemeLabel());

			int resId = mThemeIcons.getThemeExample(mThemeId);
			if (resId != -1) {
				Drawable img = getResources().getDrawable(resId);
				if (img != null)
					mExample.setImageDrawable(img);
			}

			ArrayList<String> colorList = mThemeIcons.getThemeColorList(mThemeId);
			mColorPanel.removeAllViews();
			mColorList.clear();
			for (int i = 0; i < colorList.size(); i++) {
				CheckedImageView iv = new CheckedImageView(this);
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(mColorViewWidth, mColorViewWidth, 0.0f);
				iv.setLayoutParams(lp);
				iv.setTag(colorList.get(i));
				iv.setBackgroundColor(mThemeIcons.getColorId(colorList.get(i)));
				iv.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						mThemeColor = (String) v.getTag();
						setSelectedColor();
					}

				});
				if (colorList.get(i).equals(mThemeColor))
					iv.setChecked(true, mOverlay);
				mColorPanel.addView(iv);
				mColorList.put(colorList.get(i), iv);
			}
		}
	}

	private void toggleViewsEnable(boolean enable) {
		for (int i = 0; i < mRoot.getChildCount(); i++) {
			View view = mRoot.getChildAt(i);
			if (view.getId() != R.id.use_theme)
				view.setEnabled(enable);
		}
	}

	private String getSelectedThemeLabel() {
		return String.format("%s [%s]", getResources().getText(R.string.selected_theme), mThemeId);
	}

	private void setSelectedColor() {
		for (int i = 0; i < mColorPanel.getChildCount(); i++) {
			CheckedImageView iv = (CheckedImageView) mColorPanel.getChildAt(i);
			iv.setChecked(mThemeColor.equals(iv.getTag()) ? true : false, mOverlay);
		}
	}

}
