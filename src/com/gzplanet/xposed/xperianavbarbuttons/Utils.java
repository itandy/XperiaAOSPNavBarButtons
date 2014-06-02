package com.gzplanet.xposed.xperianavbarbuttons;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

public class Utils {

	public static int getScreenWidth(Context context) {
		final Display defaultDisplay = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		final Point point = new Point();
		defaultDisplay.getSize(point);
		return point.x;

	}
	
	public static int getNavBarHeight(Resources res) {
		int resId = res.getIdentifier("navigation_bar_height", "dimen", "android");
		if (resId > 0)
			return res.getDimensionPixelSize(resId);
		else
			return -1;
	}

	public static Bitmap decodeImageFromResource(Resources resources, int resId, int reqWidth, int reqHeight) {
		Bitmap bitmap = null;

		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(resources, resId, options);
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight, false);
		options.inJustDecodeBounds = false;
		// options.inMutable = true;

		// decode image file to estimated dimension
		bitmap = BitmapFactory.decodeResource(resources, resId, options);

		// scale to requried dimension
		bitmap = Bitmap.createScaledBitmap(bitmap, reqWidth, reqHeight, true);

		return bitmap;
	}

	private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight,
			boolean cropToFit) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			if (cropToFit)
				inSampleSize = Math.min((int) Math.floor((double) height / (double) reqHeight),
						(int) Math.floor((double) width / (double) reqWidth));
			else
				inSampleSize = Math.max((int) Math.floor((double) height / (double) reqHeight),
						(int) Math.floor((double) width / (double) reqWidth));

			// adjust if final image will be smaller than required image
			if (inSampleSize > 0)
				if (width / inSampleSize < reqWidth || height / inSampleSize < reqHeight)
					inSampleSize--;

			// adjust to avoid out of memory error
			final float totalPixels = width * height;
			final float totalReqPixelsCap = reqWidth * reqHeight * 2;
			while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap)
				inSampleSize++;
		}
		return inSampleSize;
	}

	public static void saveBitmapAsFile(File dir, String fileName, Bitmap bitmap) {
		File file = new File(dir, fileName);
		if (file.exists() || !dir.canWrite())
			return;

		FileOutputStream fo = null;
		try {
			fo = new FileOutputStream(file);
			bitmap.compress(CompressFormat.PNG, 100, fo);
		} catch (FileNotFoundException e) {
		} finally {
			if (fo != null)
				try {
					fo.close();
				} catch (IOException e) {
				}
		}
	}

	public static Bitmap getDrawableBitmap(Resources res, String resName) {
		Bitmap bitmap = BitmapFactory.decodeResource(res,
				res.getIdentifier(resName, "drawable", XperiaNavBarButtons.CLASSNAME_SYSTEMUI));
		if (bitmap != null)
			bitmap.setHasAlpha(true);

		return bitmap;
	}
	
	public static String getSystemUICacheFolder(Context context) {
		String cacheFolder = null;
		try {
			cacheFolder = context.getExternalCacheDir().getAbsolutePath()
					.replace(context.getPackageName(), XperiaNavBarButtons.CLASSNAME_SYSTEMUI);
		} catch (Exception e) {
		}
		
		return cacheFolder;
	}
	
	public static int getDensityDpi(Resources res) {
		return res.getDisplayMetrics().densityDpi;
	}
	
	public static void copyFile(String src, String target) throws IOException {
	    InputStream in = new FileInputStream(src);
	    OutputStream out = new FileOutputStream(target);

	    // Transfer bytes from in to out
	    byte[] buf = new byte[1024];
	    int len;
	    while ((len = in.read(buf)) > 0) {
	        out.write(buf, 0, len);
	    }
	    in.close();
	    out.close();
	}
}
