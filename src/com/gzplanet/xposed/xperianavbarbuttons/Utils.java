package com.gzplanet.xposed.xperianavbarbuttons;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class Utils {
	private final static String PKG_DEFAULT_HOME = "com.android.launcher";
	private final static String PKG_SYSTEMUI = "com.android.systemui";
	private final static String CLASSNAME_TAKESCREENSHOTSERVICE = "com.android.systemui.screenshot.TakeScreenshotService";
	private final static int MAX_LAST_APPS = 5;
	private final static String SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS = "globalactions";

	private static Context mContext;
	private static Handler mHandler;
	private static Object mPhoneWindowManager;
	private static final Object mScreenshotLock = new Object();
	private static ServiceConnection mScreenshotConnection;

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

	public static Drawable resizeDrawable(Resources res, Drawable source, int width, int height) {
		if ((source == null) || !(source instanceof BitmapDrawable)) {
			return source;
		}

		if (source.getIntrinsicWidth() == width && source.getIntrinsicHeight() == height)
			return source;

		Bitmap bitmap = ((BitmapDrawable) source).getBitmap();

		Bitmap bitmapResized = Bitmap.createScaledBitmap(bitmap, width, height, false);

		source = new BitmapDrawable(res, bitmapResized);

		if (bitmap != bitmapResized) {
			bitmap.recycle();
			bitmap = null;
		}

		return source;
	}

	public static void switchToLastApp(Context context, Handler handler) {
		mHandler = handler;
		mContext = context;
		if (mHandler != null)
			mHandler.post(new Runnable() {

				@Override
				public void run() {
					final Intent intent = new Intent(Intent.ACTION_MAIN);
					intent.addCategory(Intent.CATEGORY_HOME);

					String pkgNameHome = PKG_DEFAULT_HOME;
					final ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
					final ResolveInfo res = mContext.getPackageManager().resolveActivity(intent, 0);
					if (res.activityInfo != null)
						if (!res.activityInfo.packageName.equals("android"))
							pkgNameHome = res.activityInfo.packageName;
					List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(MAX_LAST_APPS);
					int i = 1;
					int lastAppId = 0;
					while (lastAppId == 0 && i < tasks.size()) {
						String pkgName = tasks.get(i).topActivity.getPackageName();
						if (!pkgNameHome.equals(pkgName) && !PKG_SYSTEMUI.equals(pkgName))
							lastAppId = tasks.get(i).id;
						i++;
					}
					if (lastAppId != 0)
						am.moveTaskToFront(lastAppId, ActivityManager.MOVE_TASK_NO_USER_ACTION);
				}

			});
	}

	private static final Runnable mScreenshotTimeout = new Runnable() {
		@Override
		public void run() {
			synchronized (mScreenshotLock) {
				if (mScreenshotConnection != null) {
					mContext.unbindService(mScreenshotConnection);
					mScreenshotConnection = null;
				}
			}
		}
	};

	public static void takeScreenshot(Context context, Handler handler) {
		mContext = context;
		mHandler = handler;

		if (mHandler != null)
			synchronized (mScreenshotLock) {
				if (mScreenshotConnection != null) {
					return;
				}
				ComponentName cn = new ComponentName(PKG_SYSTEMUI, CLASSNAME_TAKESCREENSHOTSERVICE);
				Intent intent = new Intent();
				intent.setComponent(cn);
				ServiceConnection conn = new ServiceConnection() {
					@Override
					public void onServiceConnected(ComponentName name, IBinder service) {
						synchronized (mScreenshotLock) {
							if (mScreenshotConnection != this) {
								return;
							}
							final Messenger messenger = new Messenger(service);
							final Message msg = Message.obtain(null, 1);
							final ServiceConnection myConn = this;

							Handler h = new Handler(mHandler.getLooper()) {
								@Override
								public void handleMessage(Message msg) {
									synchronized (mScreenshotLock) {
										if (mScreenshotConnection == myConn) {
											mContext.unbindService(mScreenshotConnection);
											mScreenshotConnection = null;
											mHandler.removeCallbacks(mScreenshotTimeout);
										}
									}
								}
							};
							msg.replyTo = new Messenger(h);
							msg.arg1 = msg.arg2 = 0;
							h.postDelayed(new Runnable() {
								@Override
								public void run() {
									try {
										messenger.send(msg);
									} catch (RemoteException e) {
										XposedBridge.log(e);
									}
								}
							}, 1000);
						}
					}

					@Override
					public void onServiceDisconnected(ComponentName name) {
					}
				};
				if (mContext.bindService(intent, conn, Context.BIND_AUTO_CREATE)) {
					mScreenshotConnection = conn;
					mHandler.postDelayed(mScreenshotTimeout, 10000);
				}
			}
	}

	public static void screenOff(Context context) {
		try {
			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			pm.goToSleep(SystemClock.uptimeMillis());
		} catch (Exception e) {
			XposedBridge.log(e);
		}
	}

	public static void powerMenu(Object phoneWindowManager, Handler handler) {
		mPhoneWindowManager = phoneWindowManager;
		if (handler != null)
			try {
				handler.post(new Runnable() {
					@Override
					public void run() {
						XposedHelpers.callMethod(mPhoneWindowManager, "sendCloseSystemWindows",
								SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS);
						XposedHelpers.callMethod(mPhoneWindowManager, "showGlobalActionsDialog");
					}
				});
			} catch (Throwable t) {
				XposedBridge.log("Error executing PhoneWindowManager.showGlobalActionsDialog(): " + t.getMessage());
			}
	}

	public static void launchApp(Context context, Handler handler, final String launchKey) {
		mContext = context;
		if (handler != null)
			handler.post(new Runnable() {
				@Override
				public void run() {
					try {
						String keys[] = launchKey.split(XposedSettings.DELIMITER_LAUNCHKEY);

						Intent intent = new Intent();
						intent.setComponent(new ComponentName(keys[0], keys[1]));
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
						mContext.startActivity(intent);
					} catch (ActivityNotFoundException e) {
						Toast.makeText(mContext, R.string.msg_unknown_app, Toast.LENGTH_SHORT).show();
					}
				}
			});
	}

	public static void expandNotificationsPanel(Object phoneWindowManager) {
		mPhoneWindowManager = phoneWindowManager;
		try {
			final Object sbService = XposedHelpers.callMethod(mPhoneWindowManager, "getStatusBarService");
			XposedHelpers.callMethod(sbService, "expandNotificationsPanel");
		} catch (Throwable t) {
			XposedBridge.log("Error executing expandNotificationsPanel(): " + t.getMessage());
		}
	}

	public static void expandSettingsPanel(Object phoneWindowManager) {
		mPhoneWindowManager = phoneWindowManager;
		try {
			final Object sbService = XposedHelpers.callMethod(mPhoneWindowManager, "getStatusBarService");
			XposedHelpers.callMethod(sbService, "expandSettingsPanel");
		} catch (Throwable t) {
			XposedBridge.log("Error executing expandSettingsPanel(): " + t.getMessage());
		}
	}

	public static List<ResolveInfo> getAppList(Context context) {
		List<ResolveInfo> apps;

		final Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		final PackageManager pm = context.getPackageManager();

		apps = pm.queryIntentActivities(intent, 0);

		Comparator<ResolveInfo> icc = new Comparator<ResolveInfo>() {
			@Override
			public int compare(ResolveInfo lhs, ResolveInfo rhs) {
				final String name1 = lhs.loadLabel(pm).toString();
				final String name2 = rhs.loadLabel(pm).toString();

				return name1.compareToIgnoreCase(name2);
			}

		};
		Collections.sort(apps, icc);

		return apps;
	}
}
