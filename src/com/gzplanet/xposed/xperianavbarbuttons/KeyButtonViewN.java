package com.gzplanet.xposed.xperianavbarbuttons;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;

import static android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK;

@TargetApi(Build.VERSION_CODES.N)
public class KeyButtonViewN extends ImageView {
    final long[] PATTERN_KEYPRESS = new long[]{0, 70, 0, 0};
    final long[] PATTERN_LONGPRESS = new long[]{0, 50, 0, 0};

    private int mContentDescriptionRes;
    private long mDownTime;
    private int mCode;
    private int mTouchSlop;
    private boolean mSupportsLongpress = true;
    private AudioManager mAudioManager;
    private boolean mGestureAborted;
    private boolean mLongClicked;
    private Context mContext;
    private Handler mHandler = new Handler();
    private int mCodeLongPress;
    private String mFuncApp;
    private String mFuncAppLongPress;
    private String mFuncShortcut;
    private String mFuncShortcutLongPress;

    public void setCode(int code) {
        mCode = code;
    }

    public void setCodeLongPress(int codeLongPress) {
        mCodeLongPress = codeLongPress;
    }

    public void setFuncApp(String funcApp) {
        mFuncApp = funcApp;
    }

    public void setFuncAppLongPress(String funcAppLongPress) {
        mFuncAppLongPress = funcAppLongPress;
    }

    public void setFuncShortcut(String funcShortcut) {
        mFuncShortcut = funcShortcut;
    }

    public void setFuncShortcutLongPress(String funcShortcutLongPress) {
        mFuncShortcutLongPress = funcShortcutLongPress;
    }

    private final Runnable mCheckLongPress = new Runnable() {
        public void run() {
            if (isPressed()) {
                // Log.d("KeyButtonView", "longpressed: " + this);
                if (isLongClickable()) {
                    // Just an old-fashioned ImageView
                    performLongClick();
                    mLongClicked = true;
                } else if (mSupportsLongpress) {
                    sendEvent(KeyEvent.ACTION_DOWN, KeyEvent.FLAG_LONG_PRESS);
                    sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);
                    mLongClicked = true;
                }
            }
        }
    };

    public KeyButtonViewN(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

        mContext = context;
    }

    public KeyButtonViewN(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

        mContext = context;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.KeyButtonView,
                defStyle, 0);

        mCode = a.getInteger(R.styleable.KeyButtonView_keyCode, 0);

        mSupportsLongpress = a.getBoolean(R.styleable.KeyButtonView_keyRepeat, true);

        TypedValue value = new TypedValue();
        if (a.getValue(R.styleable.KeyButtonView_android_contentDescription, value)) {
            mContentDescriptionRes = value.resourceId;
        }

        a.recycle();


        setClickable(true);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        setBackground(new KeyButtonRipple(context, this));
    }

    public KeyButtonViewN(Context context, int code, boolean supportsLongpress) {
        super(context);

        mContext = context;

        mCode = code;

        mSupportsLongpress = supportsLongpress;

        setClickable(true);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        setBackground(new KeyButtonRipple(context, this));
    }

    public void loadAsync(String uri) {
        new AsyncTask<String, Void, Drawable>() {
            @Override
            protected Drawable doInBackground(String... params) {
                return Icon.createWithContentUri(params[0]).loadDrawable(mContext);
            }

            @Override
            protected void onPostExecute(Drawable drawable) {
                setImageDrawable(drawable);
            }
        }.execute(uri);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mContentDescriptionRes != 0) {
            setContentDescription(mContext.getString(mContentDescriptionRes));
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (mCode != 0) {
            info.addAction(new AccessibilityNodeInfo.AccessibilityAction(ACTION_CLICK, null));
            if (mSupportsLongpress || isLongClickable()) {
                info.addAction(
                        new AccessibilityNodeInfo.AccessibilityAction(ACTION_LONG_CLICK, null));
            }
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != View.VISIBLE) {
            jumpDrawablesToCurrentState();
        }
    }

    @Override
    public boolean performAccessibilityActionInternal(int action, Bundle arguments) {
        if (action == ACTION_CLICK && mCode != 0) {
            sendEvent(KeyEvent.ACTION_DOWN, 0, SystemClock.uptimeMillis());
            sendEvent(KeyEvent.ACTION_UP, 0);
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
            playSoundEffect(SoundEffectConstants.CLICK);
            return true;
        } else if (action == ACTION_LONG_CLICK && mCode != 0) {
            sendEvent(KeyEvent.ACTION_DOWN, KeyEvent.FLAG_LONG_PRESS);
            sendEvent(KeyEvent.ACTION_UP, 0);
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);
            return true;
        }
        return super.performAccessibilityActionInternal(action, arguments);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        int x, y;
        if (action == MotionEvent.ACTION_DOWN) {
            mGestureAborted = false;
        }
        if (mGestureAborted) {
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownTime = SystemClock.uptimeMillis();
                mLongClicked = false;
                setPressed(true);
                if (mCode != 0) {
                    sendEvent(KeyEvent.ACTION_DOWN, 0, mDownTime);
                } else {
                    // Provide the same haptic feedback that the system offers for virtual keys.
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                }
                removeCallbacks(mCheckLongPress);
                postDelayed(mCheckLongPress, ViewConfiguration.getLongPressTimeout());
                break;
            case MotionEvent.ACTION_MOVE:
                x = (int) ev.getX();
                y = (int) ev.getY();
                setPressed(x >= -mTouchSlop
                        && x < getWidth() + mTouchSlop
                        && y >= -mTouchSlop
                        && y < getHeight() + mTouchSlop);
                break;
            case MotionEvent.ACTION_CANCEL:
                setPressed(false);
                if (mCode != 0) {
                    sendEvent(KeyEvent.ACTION_UP, KeyEvent.FLAG_CANCELED);
                }
                removeCallbacks(mCheckLongPress);
                break;
            case MotionEvent.ACTION_UP:
                final boolean doIt = isPressed() && !mLongClicked;
                setPressed(false);
                if (mCode != 0) {
                    if (doIt) {
                        sendEvent(KeyEvent.ACTION_UP, 0);
                        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
                        playSoundEffect(SoundEffectConstants.CLICK);
                    } else {
                        sendEvent(KeyEvent.ACTION_UP, KeyEvent.FLAG_CANCELED);
                    }
                } else {
                    // no key code, just a regular ImageView
                    if (doIt) {
                        performClick();
                    }
                }
                removeCallbacks(mCheckLongPress);
                break;
        }

        return true;
    }

    public void playSoundEffect(int soundConstant) {
        mAudioManager.playSoundEffect(soundConstant, ActivityManager.getCurrentUser());
    }

    ;

    public void sendEvent(int action, int flags) {
        sendEvent(action, flags, SystemClock.uptimeMillis());
    }

    void sendEvent(int action, int flags, long when) {
        final int repeatCount = (flags & KeyEvent.FLAG_LONG_PRESS) != 0 ? 1 : 0;
        if (action == KeyEvent.ACTION_DOWN) {
            if (repeatCount > 0) {
                performAction(mCodeLongPress, mFuncAppLongPress, mFuncShortcutLongPress);
                vibrate(true);
            } else
                vibrate(false);
        } else {
            if (!mLongClicked)
                performAction(mCode, mFuncApp, mFuncShortcut);
        }
    }

    public void abortCurrentGesture() {
        setPressed(false);
        mGestureAborted = true;
    }

    private void performAction(int keyCode, String funcApp, String funcShortcut) {
        switch (keyCode) {
            case XperiaNavBarButtons.KEYCODE_SWITCH_LAST_APP:
                Utils.switchToLastApp(mContext, mHandler);
                break;
            case XperiaNavBarButtons.KEYCODE_TAKE_SCREENSHOT:
                Utils.takeScreenshot(mContext, mHandler);
                break;
            case XperiaNavBarButtons.KEYCODE_SCREEN_OFF:
                Utils.screenOff(mContext);
                break;
            case XperiaNavBarButtons.KEYCODE_LAUNCH_APP:
                Utils.launchApp(mContext, mHandler, funcApp);
                break;
            case XperiaNavBarButtons.KEYCODE_LAUNCH_SHORTCUT:
                Utils.launchShortcut(mContext, mHandler, funcShortcut);
                break;
        }
    }

    private void vibrate(boolean longPress) {

        Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(longPress ? PATTERN_LONGPRESS : PATTERN_KEYPRESS, -1);

    }

    private long[] getLongIntArray(Resources r, int resid) {
        int[] ar = r.getIntArray(resid);
        if (ar == null) {
            return null;
        }
        long[] out = new long[ar.length];
        for (int i = 0; i < ar.length; i++) {
            out[i] = ar[i];
        }
        return out;
    }

}
