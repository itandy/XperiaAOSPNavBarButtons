package com.gzplanet.xposed.xperianavbarbuttons;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CanvasProperty;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.DisplayListCanvas;
import android.view.RenderNodeAnimator;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

import java.util.ArrayList;
import java.util.HashSet;


@TargetApi(Build.VERSION_CODES.N)
public class KeyButtonRipple extends Drawable {

    private static final float GLOW_MAX_SCALE_FACTOR = 1.35f;
    private static final float GLOW_MAX_ALPHA = 0.2f;
    private static final int ANIMATION_DURATION_SCALE = 350;
    private static final int ANIMATION_DURATION_FADE = 450;

    private Paint mRipplePaint;
    private CanvasProperty<Float> mLeftProp;
    private CanvasProperty<Float> mTopProp;
    private CanvasProperty<Float> mRightProp;
    private CanvasProperty<Float> mBottomProp;
    private CanvasProperty<Float> mRxProp;
    private CanvasProperty<Float> mRyProp;
    private CanvasProperty<Paint> mPaintProp;
    private float mGlowAlpha = 0f;
    private float mGlowScale = 1f;
    private boolean mPressed;
    private boolean mDrawingHardwareGlow;
    private int mMaxWidth;

    private final Interpolator mInterpolator = new LogInterpolator();
    private boolean mSupportHardware;
    private final View mTargetView;

    private final HashSet<Animator> mRunningAnimations = new HashSet<>();
    private final ArrayList<Animator> mTmpArray = new ArrayList<>();

    public static final Interpolator ALPHA_OUT = new PathInterpolator(0f, 0f, 0.8f, 1f);

    public KeyButtonRipple(Context ctx, View targetView) {
        int id = ctx.getResources().getIdentifier("key_button_ripple_max_width", "dimen", "com.android.systemui");
        mMaxWidth =  ctx.getResources().getDimensionPixelSize(id);
        mTargetView = targetView;
    }

    private Paint getRipplePaint() {
        if (mRipplePaint == null) {
            mRipplePaint = new Paint();
            mRipplePaint.setAntiAlias(true);
            mRipplePaint.setColor(0xffffffff);
        }
        return mRipplePaint;
    }

    private void drawSoftware(Canvas canvas) {
        if (mGlowAlpha > 0f) {
            final Paint p = getRipplePaint();
            p.setAlpha((int)(mGlowAlpha * 255f));

            final float w = getBounds().width();
            final float h = getBounds().height();
            final boolean horizontal = w > h;
            final float diameter = getRippleSize() * mGlowScale;
            final float radius = diameter * .5f;
            final float cx = w * .5f;
            final float cy = h * .5f;
            final float rx = horizontal ? radius : cx;
            final float ry = horizontal ? cy : radius;
            final float corner = horizontal ? cy : cx;

            canvas.drawRoundRect(cx - rx, cy - ry,
                    cx + rx, cy + ry,
                    corner, corner, p);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        mSupportHardware = canvas.isHardwareAccelerated();
        if (mSupportHardware) {
            drawHardware((DisplayListCanvas) canvas);
        } else {
            drawSoftware(canvas);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        // Not supported.
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        // Not supported.
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    private boolean isHorizontal() {
        return getBounds().width() > getBounds().height();
    }

    private void drawHardware(DisplayListCanvas c) {
        if (mDrawingHardwareGlow) {
            c.drawRoundRect(mLeftProp, mTopProp, mRightProp, mBottomProp, mRxProp, mRyProp,
                    mPaintProp);
        }
    }

    public float getGlowAlpha() {
        return mGlowAlpha;
    }

    public void setGlowAlpha(float x) {
        mGlowAlpha = x;
        invalidateSelf();
    }

    public float getGlowScale() {
        return mGlowScale;
    }

    public void setGlowScale(float x) {
        mGlowScale = x;
        invalidateSelf();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        boolean pressed = false;
        for (int i = 0; i < state.length; i++) {
            if (state[i] == android.R.attr.state_pressed) {
                pressed = true;
                break;
            }
        }
        if (pressed != mPressed) {
            setPressed(pressed);
            mPressed = pressed;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void jumpToCurrentState() {
        cancelAnimations();
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    public void setPressed(boolean pressed) {
        if (mSupportHardware) {
            setPressedHardware(pressed);
        } else {
            setPressedSoftware(pressed);
        }
    }

    private void cancelAnimations() {
        mTmpArray.addAll(mRunningAnimations);
        int size = mTmpArray.size();
        for (int i = 0; i < size; i++) {
            Animator a = mTmpArray.get(i);
            a.cancel();
        }
        mTmpArray.clear();
        mRunningAnimations.clear();
    }

    private void setPressedSoftware(boolean pressed) {
        if (pressed) {
            enterSoftware();
        } else {
            exitSoftware();
        }
    }

    private void enterSoftware() {
        cancelAnimations();
        mGlowAlpha = GLOW_MAX_ALPHA;
        ObjectAnimator scaleAnimator = ObjectAnimator.ofFloat(this, "glowScale",
                0f, GLOW_MAX_SCALE_FACTOR);
        scaleAnimator.setInterpolator(mInterpolator);
        scaleAnimator.setDuration(ANIMATION_DURATION_SCALE);
        scaleAnimator.addListener(mAnimatorListener);
        scaleAnimator.start();
        mRunningAnimations.add(scaleAnimator);
    }

    private void exitSoftware() {
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(this, "glowAlpha", mGlowAlpha, 0f);
        alphaAnimator.setInterpolator(ALPHA_OUT);
        alphaAnimator.setDuration(ANIMATION_DURATION_FADE);
        alphaAnimator.addListener(mAnimatorListener);
        alphaAnimator.start();
        mRunningAnimations.add(alphaAnimator);
    }

    private void setPressedHardware(boolean pressed) {
        if (pressed) {
            enterHardware();
        } else {
            exitHardware();
        }
    }

    /**
     * Sets the left/top property for the round rect to {@code prop} depending on whether we are
     * horizontal or vertical mode.
     */
    private void setExtendStart(CanvasProperty<Float> prop) {
        if (isHorizontal()) {
            mLeftProp = prop;
        } else {
            mTopProp = prop;
        }
    }

    private CanvasProperty<Float> getExtendStart() {
        return isHorizontal() ? mLeftProp : mTopProp;
    }

    /**
     * Sets the right/bottom property for the round rect to {@code prop} depending on whether we are
     * horizontal or vertical mode.
     */
    private void setExtendEnd(CanvasProperty<Float> prop) {
        if (isHorizontal()) {
            mRightProp = prop;
        } else {
            mBottomProp = prop;
        }
    }

    private CanvasProperty<Float> getExtendEnd() {
        return isHorizontal() ? mRightProp : mBottomProp;
    }

    private int getExtendSize() {
        return isHorizontal() ? getBounds().width() : getBounds().height();
    }

    private int getRippleSize() {
        int size = isHorizontal() ? getBounds().width() : getBounds().height();
        return Math.min(size, mMaxWidth);
    }

    private void enterHardware() {
        cancelAnimations();
        mDrawingHardwareGlow = true;
        setExtendStart(CanvasProperty.createFloat(getExtendSize() / 2));
        final RenderNodeAnimator startAnim = new RenderNodeAnimator(getExtendStart(),
                getExtendSize()/2 - GLOW_MAX_SCALE_FACTOR * getRippleSize()/2);
        startAnim.setDuration(ANIMATION_DURATION_SCALE);
        startAnim.setInterpolator(mInterpolator);
        startAnim.addListener(mAnimatorListener);
        startAnim.setTarget(mTargetView);

        setExtendEnd(CanvasProperty.createFloat(getExtendSize() / 2));
        final RenderNodeAnimator endAnim = new RenderNodeAnimator(getExtendEnd(),
                getExtendSize()/2 + GLOW_MAX_SCALE_FACTOR * getRippleSize()/2);
        endAnim.setDuration(ANIMATION_DURATION_SCALE);
        endAnim.setInterpolator(mInterpolator);
        endAnim.addListener(mAnimatorListener);
        endAnim.setTarget(mTargetView);

        if (isHorizontal()) {
            mTopProp = CanvasProperty.createFloat(0f);
            mBottomProp = CanvasProperty.createFloat(getBounds().height());
            mRxProp = CanvasProperty.createFloat(getBounds().height()/2);
            mRyProp = CanvasProperty.createFloat(getBounds().height()/2);
        } else {
            mLeftProp = CanvasProperty.createFloat(0f);
            mRightProp = CanvasProperty.createFloat(getBounds().width());
            mRxProp = CanvasProperty.createFloat(getBounds().width()/2);
            mRyProp = CanvasProperty.createFloat(getBounds().width()/2);
        }

        mGlowScale = GLOW_MAX_SCALE_FACTOR;
        mGlowAlpha = GLOW_MAX_ALPHA;
        mRipplePaint = getRipplePaint();
        mRipplePaint.setAlpha((int) (mGlowAlpha * 255));
        mPaintProp = CanvasProperty.createPaint(mRipplePaint);

        startAnim.start();
        endAnim.start();
        mRunningAnimations.add(startAnim);
        mRunningAnimations.add(endAnim);

        invalidateSelf();
    }

    private void exitHardware() {
        mPaintProp = CanvasProperty.createPaint(getRipplePaint());
        final RenderNodeAnimator opacityAnim = new RenderNodeAnimator(mPaintProp,
                RenderNodeAnimator.PAINT_ALPHA, 0);
        opacityAnim.setDuration(ANIMATION_DURATION_FADE);
        opacityAnim.setInterpolator(ALPHA_OUT);
        opacityAnim.addListener(mAnimatorListener);
        opacityAnim.setTarget(mTargetView);

        opacityAnim.start();
        mRunningAnimations.add(opacityAnim);

        invalidateSelf();
    }

    private final AnimatorListenerAdapter mAnimatorListener =
            new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mRunningAnimations.remove(animation);
                    if (mRunningAnimations.isEmpty() && !mPressed) {
                        mDrawingHardwareGlow = false;
                        invalidateSelf();
                    }
                }
            };

    /**
     * Interpolator with a smooth log deceleration
     */
    private static final class LogInterpolator implements Interpolator {
        @Override
        public float getInterpolation(float input) {
            return 1 - (float) Math.pow(400, -input * 1.4);
        }
    }
}
