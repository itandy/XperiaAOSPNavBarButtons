package com.gzplanet.xposed.xperianavbarbuttons;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import de.robv.android.xposed.XposedHelpers;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class KeyButtonViewL extends ImageView {
	private static final String TAG = "StatusBar.KeyButtonView";
	private static final boolean DEBUG = false;
	private static final String CLASSNAME_KEYBUTTONRIPPLE = "com.android.systemui.statusbar.policy.KeyButtonRipple";

	// TODO: Get rid of this
	public static final float DEFAULT_QUIESCENT_ALPHA = 1f;

	private long mDownTime;
	private int mCode;
	private int mTouchSlop;
	private float mDrawingAlpha = 1f;
	private float mQuiescentAlpha = DEFAULT_QUIESCENT_ALPHA;
	private boolean mSupportsLongpress = true;
	private AudioManager mAudioManager;
	private Animator mAnimateToQuiescent = new ObjectAnimator();

	private final Runnable mCheckLongPress = new Runnable() {
		public void run() {
			if (isPressed()) {
				// Log.d("KeyButtonView", "longpressed: " + this);
				if (isLongClickable()) {
					// Just an old-fashioned ImageView
					performLongClick();
				} else {
					sendEvent(KeyEvent.ACTION_DOWN, KeyEvent.FLAG_LONG_PRESS);
					sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);
				}
			}
		}
	};

	public KeyButtonViewL(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public KeyButtonViewL(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.KeyButtonView, defStyle, 0);

		mCode = a.getInteger(R.styleable.KeyButtonView_keyCode, 0);

		mSupportsLongpress = a.getBoolean(R.styleable.KeyButtonView_keyRepeat, true);

		setDrawingAlpha(mQuiescentAlpha);

		a.recycle();

		setClickable(true);
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

		Class<?> classKeyButtonRipple = XposedHelpers.findClass(CLASSNAME_KEYBUTTONRIPPLE, context.getClassLoader());
		setBackground((Drawable) XposedHelpers.newInstance(classKeyButtonRipple, context, this));
	}

	public KeyButtonViewL(Context context, final int code, final boolean supportsLongpress) {
		super(context);

		mCode = code;

		mSupportsLongpress = supportsLongpress;

		setDrawingAlpha(mQuiescentAlpha);

		setClickable(true);
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

		Class<?> classKeyButtonRipple = XposedHelpers.findClass(CLASSNAME_KEYBUTTONRIPPLE, context.getClassLoader());
		setBackground((Drawable) XposedHelpers.newInstance(classKeyButtonRipple, context, this));
	}

	@Override
	public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
		super.onInitializeAccessibilityNodeInfo(info);
		if (mCode != 0) {
			info.addAction(new AccessibilityNodeInfo.AccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK, null));
			if (mSupportsLongpress) {
				info.addAction(new AccessibilityNodeInfo.AccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK, null));
			}
		}
	}

	@Override
	public boolean performAccessibilityAction(int action, Bundle arguments) {
		if (action == android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK && mCode != 0) {
			sendEvent(KeyEvent.ACTION_DOWN, 0, SystemClock.uptimeMillis());
			sendEvent(KeyEvent.ACTION_UP, 0);
			sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
			playSoundEffect(SoundEffectConstants.CLICK);
			return true;
		} else if (action == android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK && mCode != 0 && mSupportsLongpress) {
			sendEvent(KeyEvent.ACTION_DOWN, KeyEvent.FLAG_LONG_PRESS);
			sendEvent(KeyEvent.ACTION_UP, 0);
			sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);
			return true;
		}
		return super.performAccessibilityAction(action, arguments);
	}

	public void setQuiescentAlpha(float alpha, boolean animate) {
		mAnimateToQuiescent.cancel();
		alpha = Math.min(Math.max(alpha, 0), 1);
		if (alpha == mQuiescentAlpha && alpha == mDrawingAlpha)
			return;
		mQuiescentAlpha = alpha;
		if (DEBUG)
			Log.d(TAG, "New quiescent alpha = " + mQuiescentAlpha);
		if (animate) {
			mAnimateToQuiescent = animateToQuiescent();
			mAnimateToQuiescent.start();
		} else {
			setDrawingAlpha(mQuiescentAlpha);
		}
	}

	private ObjectAnimator animateToQuiescent() {
		return ObjectAnimator.ofFloat(this, "drawingAlpha", mQuiescentAlpha);
	}

	public float getQuiescentAlpha() {
		return mQuiescentAlpha;
	}

	public float getDrawingAlpha() {
		return mDrawingAlpha;
	}

	public void setDrawingAlpha(float x) {
		setImageAlpha((int) (x * 255));
		mDrawingAlpha = x;
	}

	public boolean onTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		int x, y;

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			// Log.d("KeyButtonView", "press");
			mDownTime = SystemClock.uptimeMillis();
			setPressed(true);
			if (mCode != 0) {
				sendEvent(KeyEvent.ACTION_DOWN, 0, mDownTime);
			} else {
				// Provide the same haptic feedback that the system offers for
				// virtual keys.
				performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
			}
			if (mSupportsLongpress) {
				removeCallbacks(mCheckLongPress);
				postDelayed(mCheckLongPress, ViewConfiguration.getLongPressTimeout());
			}
			break;
		case MotionEvent.ACTION_MOVE:
			x = (int) ev.getX();
			y = (int) ev.getY();
			setPressed(x >= -mTouchSlop && x < getWidth() + mTouchSlop && y >= -mTouchSlop && y < getHeight() + mTouchSlop);
			break;
		case MotionEvent.ACTION_CANCEL:
			setPressed(false);
			if (mCode != 0) {
				sendEvent(KeyEvent.ACTION_UP, KeyEvent.FLAG_CANCELED);
			}
			if (mSupportsLongpress) {
				removeCallbacks(mCheckLongPress);
			}
			break;
		case MotionEvent.ACTION_UP:
			final boolean doIt = isPressed();
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
			if (mSupportsLongpress) {
				removeCallbacks(mCheckLongPress);
			}
			break;
		}

		return true;
	}

	public void playSoundEffect(int soundConstant) {
		mAudioManager.playSoundEffect(soundConstant, (Integer) XposedHelpers.callStaticMethod(ActivityManager.class, "getCurrentUser"));
	};

	public void sendEvent(int action, int flags) {
		sendEvent(action, flags, SystemClock.uptimeMillis());
	}

	void sendEvent(int action, int flags, long when) {
		final int repeatCount = (flags & KeyEvent.FLAG_LONG_PRESS) != 0 ? 1 : 0;
		final KeyEvent ev = new KeyEvent(mDownTime, when, action, mCode, repeatCount, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags | KeyEvent.FLAG_FROM_SYSTEM
				| KeyEvent.FLAG_VIRTUAL_HARD_KEY, InputDevice.SOURCE_KEYBOARD);
		InputManager.getInstance().injectInputEvent(ev, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
	}
}
