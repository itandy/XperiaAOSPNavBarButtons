package com.gzplanet.xposed.xperianavbarbuttons;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class DirectoryPicker extends Activity {
	private String mThemeId;
	private String mThemeColor;
	private boolean mUseTheme;
	private String mCurrentPath;
	private ArrayList<String> mFolderList;
	private Map<String, Bitmap> mCustomButtonFiles = new HashMap<String, Bitmap>();
	private CustomButtons mCustomButtons;

	private ListView mList;

	private class CustomButtonImageFilesOnly implements FileFilter {
		@Override
		public boolean accept(File pathname) {
			return pathname.getName().startsWith(CustomButtons.FILENAME_PREFIX);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.directory);

		Intent intent = getIntent();
		mCurrentPath = intent.getStringExtra("currentpath");
		mUseTheme = intent.getBooleanExtra("usetheme", false);
		mThemeId = intent.getStringExtra("themeid");
		mThemeColor = intent.getStringExtra("themecolor");

		// populate directory list
		File currDir = new File(mCurrentPath);
		mFolderList = populateDataModel(currDir.listFiles());
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.directory_item, R.id.folder, mFolderList);

		mList = (ListView) findViewById(R.id.dir_list);
		TextView tvCurrDir = (TextView) findViewById(R.id.current_folder);
		tvCurrDir.setText(mCurrentPath);

		mList.setAdapter(adapter);
		mList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
				Intent intent = new Intent(DirectoryPicker.this, DirectoryPicker.class);
				intent.putExtra("usetheme", mUseTheme);
				intent.putExtra("themeid", mThemeId);
				intent.putExtra("themecolor", mThemeColor);
				intent.putExtra("currentpath", mCurrentPath + File.separator + mFolderList.get(position));
				startActivity(intent);
			}
		});

		// search for custom button image files
		mCustomButtons = new CustomButtons(mCurrentPath, Utils.getDensityDpi(getResources()), "");
		if (mCustomButtons.getMainButtonsCount() > 0) {
			final int buttonWidth = Math.round((float) Utils.getScreenWidth(this) / 5f);
			final int navBarHeight = Utils.getNavBarHeight(getResources());
			
			ViewGroup vgRoot = (ViewGroup) findViewById(R.id.preview_panel);
			ViewGroup vg = (ViewGroup) findViewById(R.id.preview_buttons_container);
			TextView tv = (TextView) findViewById(R.id.custom_buttons_found);
			Button btn = (Button) findViewById(R.id.btn_confirm);

			ArrayList<String> examples = mCustomButtons.getMainButtonNames();
			for (String type: examples) {
				Bitmap bitmap = mCustomButtons.getBitmap(type, false, false);
				if (bitmap != null) {
					mCustomButtonFiles.put(type, bitmap);
					ImageView iv = new ImageView(this);
					iv.setLayoutParams(new LinearLayout.LayoutParams(buttonWidth, navBarHeight > 0 ? navBarHeight
							: LinearLayout.LayoutParams.FILL_PARENT, 0.0f));
					iv.setScaleType(ScaleType.FIT_CENTER);
					iv.setImageBitmap(bitmap);
					vg.addView(iv);
				}
			}

			if (vg.getChildCount() > 0) {
				vgRoot.setVisibility(View.VISIBLE);
				tv.setText(String.format("%d custom button images found", mCustomButtons.getCount()));
				btn.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						confirmFolder();
					}

				});
			}
		}
	}

	private ArrayList<String> populateDataModel(File[] file_list) {

		ArrayList<String> files = new ArrayList<String>();
		if (file_list != null) {
			for (File file : file_list)
				if (file.isDirectory() && !file.isHidden())
					files.add(file.getName());

			Comparator<String> icc = new Comparator<String>() {

				@Override
				public int compare(String lhs, String rhs) {
					return lhs.compareToIgnoreCase(rhs);
				}

			};
			Collections.sort(files, icc);
		}
		return files;
	}

	private void confirmFolder() {
		// get SystemUI cache folder
		String cacheFolder = null;
		File cacheDir;
		try {
			cacheFolder = getExternalCacheDir().getAbsolutePath().replace(getPackageName(),
					XperiaNavBarButtons.CLASSNAME_SYSTEMUI);
		} catch (Exception e) {
		} finally {
			cacheDir = new File(cacheFolder);
			if (!cacheDir.exists())
				cacheDir.mkdirs();
		}

		// clear existing files
		File[] oldFiles = cacheDir.listFiles(new CustomButtonImageFilesOnly());
		for (File file : oldFiles) {
			if (file.exists())
				file.delete();
		}

		// write custom button images to cache folder
		mCustomButtons.importIntoCacheFolder(cacheFolder);
//		try {
//			if (mCustomButtonFiles.size() > 0) {
//				Iterator<Entry<String, Bitmap>> it = mCustomButtonFiles.entrySet().iterator();
//				while (it.hasNext()) {
//					Map.Entry<String, Bitmap> vp = it.next();
//					Bitmap bitmap = vp.getValue();
//					if (bitmap != null) {
//						Utils.saveBitmapAsFile(cacheDir, CustomButtons.FILENAME_PREFIX + vp.getKey(), bitmap);
//						bitmap.recycle();
//						bitmap = null;
//					}
//					it.remove();
//				}
//			}
//		} catch (Exception e) {
//			XposedBridge.log(e.getMessage());
//		}

		Intent intent = new Intent(this, ThemeActivity.class);
		intent.putExtra("usetheme", mUseTheme);
		intent.putExtra("themeid", mThemeId);
		intent.putExtra("themecolor", mThemeColor);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		startActivity(intent);
	}
}
