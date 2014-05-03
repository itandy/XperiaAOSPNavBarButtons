package com.gzplanet.xposed.xperianavbarbuttons;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CheckedImageView extends ImageView {
	private boolean mChecked = false;
	private Bitmap mOverlay;

	public CheckedImageView(Context context) {
		super(context);
	}

	public CheckedImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CheckedImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public boolean isChecked() {
		return mChecked;
	}

	public void setChecked(boolean checked, Bitmap overlay) {
		mChecked = checked;
		mOverlay = overlay;
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mChecked && mOverlay != null && !mOverlay.isRecycled()) {
			canvas.drawBitmap(mOverlay, 0, 0, null);
		}
	}
}
