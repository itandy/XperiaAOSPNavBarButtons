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
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
	private int mScreenWidth;

	ThemeIcons mThemeIcons = new ThemeIcons();
	ArrayList<String> mThemeList;
	Map<String, View> mColorList = new HashMap<String, View>();
	Bitmap mOverlay;
	ArrayList<Bitmap> mCustomImages = new ArrayList<Bitmap>();
	CustomButtons mCustomButtons;

	ImageView mExample;
	ListView mList;
	TextView mColorLabel;
	ViewGroup mColorPanel;
	TextView mThemeLabel;
	Switch mSwitch;
	ViewGroup mRoot;
	ViewGroup mCustomThemePanel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.theme);

		// get screen width
		mScreenWidth = Utils.getScreenWidth(this);
		mColorViewWidth = Math.round((float) mScreenWidth / (float) MAX_COLOR);

		Intent intent = getIntent();
		mUseTheme = intent.getBooleanExtra("usetheme", false);
		mThemeId = intent.getStringExtra("themeid");
		mThemeColor = intent.getStringExtra("themecolor");

		mCustomButtons = new CustomButtons(Utils.getSystemUICacheFolder(this), Utils.getDensityDpi(getResources()),
				CustomButtons.FILENAME_PREFIX);

		mRoot = (ViewGroup) findViewById(R.id.theme_root);
		mSwitch = (Switch) findViewById(R.id.use_theme);
		mList = (ListView) findViewById(R.id.theme_list);
		mExample = (ImageView) findViewById(R.id.theme_example);
		mColorLabel = (TextView) findViewById(R.id.color_label);
		mColorPanel = (ViewGroup) findViewById(R.id.color_panel);
		mThemeLabel = (TextView) findViewById(R.id.example_label);
		mCustomThemePanel = (ViewGroup) findViewById(R.id.custom_theme_panel);

		mThemeList = new ArrayList<String>(mThemeIcons.getThemeList());
		Collections.sort(mThemeList);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.theme_item, R.id.theme_name, mThemeList);
		mList.setAdapter(adapter);
		mList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
				mThemeId = mThemeList.get(position);
				boolean userDefine = mThemeIcons.getThemeExample(mThemeId) == -1;
				mThemeColor = userDefine ? "N/A" : XperiaNavBarButtons.DEF_THEMECOLOR;

				// allow user to select custom theme, if used for the first time
				if (userDefine && mCustomButtons.getCount() == 0) {
					Intent intent = new Intent(ThemeActivity.this, DirectoryPicker.class);
					intent.putExtra("usetheme", mUseTheme);
					intent.putExtra("themeid", mThemeId);
					intent.putExtra("themecolor", mThemeColor);
					intent.putExtra("currentpath", Environment.getExternalStorageDirectory().getAbsolutePath());
					startActivity(intent);
				} else
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
	}

	@Override
	protected void onStart() {
		mOverlay = Utils.decodeImageFromResource(getResources(), R.drawable.ic_imageview_checked, mColorViewWidth,
				mColorViewWidth);

		refreshSelectedTheme();

		super.onStart();
	}

	@Override
	public void onStop() {
		if (mOverlay != null) {
			mOverlay.recycle();
			mOverlay = null;
		}
		recycleBitmaps();

		super.onStop();
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
	protected void onNewIntent(Intent intent) {
		mCustomButtons.refresh();
		refreshSelectedTheme();

		super.onNewIntent(intent);
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
				mCustomThemePanel.setVisibility(View.GONE);
				mExample.setVisibility(View.VISIBLE);
				mColorLabel.setVisibility(View.VISIBLE);

				ArrayList<String> colorList = mThemeIcons.getThemeColorList(mThemeId);
				mColorPanel.removeAllViews();
				mColorList.clear();
				for (int i = 0; i < colorList.size(); i++) {
					if (!"N/A".equals(colorList.get(i))) {
						CheckedImageView iv = new CheckedImageView(this);
						LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(mColorViewWidth, mColorViewWidth,
								0.0f);
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
			} else {
				// display custom button images (main buttons only)
				if (mCustomButtons.getMainButtonsCount() > 0) {
					// recycle previously loaded bitmap
					recycleBitmaps();

					mCustomThemePanel.removeAllViews();
					int buttonWidth = Math.round((float) mScreenWidth / (float) mCustomButtons.getMainButtonsCount());
					ArrayList<Bitmap> mainButtons = mCustomButtons.getMainButtonBitmap();
					for (Bitmap bitmap : mainButtons) {
						mCustomImages.add(bitmap);
						ImageView iv = new ImageView(this);
						iv.setLayoutParams(new LinearLayout.LayoutParams(buttonWidth,
								LinearLayout.LayoutParams.FILL_PARENT, 0.0f));
						iv.setImageBitmap(bitmap);
						mCustomThemePanel.addView(iv);
					}
				}

				mCustomThemePanel.setVisibility(View.VISIBLE);
				mExample.setVisibility(View.GONE);
				mColorLabel.setVisibility(View.GONE);
				mColorPanel.removeAllViews();
				Button button = new Button(this);
				button.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT, 0.0f));
				button.setText(R.string.import_button_iamges);
				button.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(ThemeActivity.this, DirectoryPicker.class);
						intent.putExtra("usetheme", mUseTheme);
						intent.putExtra("themeid", mThemeId);
						intent.putExtra("themecolor", mThemeColor);
						intent.putExtra("currentpath", Environment.getExternalStorageDirectory().getAbsolutePath());
						startActivity(intent);
					}
				});
				mColorPanel.addView(button);
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

	private void recycleBitmaps() {
		if (mCustomImages != null) {
			for (Bitmap bitmap : mCustomImages) {
				if (bitmap != null) {
					bitmap.recycle();
					bitmap = null;
				}
			}
			mCustomImages.clear();
		}
	}
}
