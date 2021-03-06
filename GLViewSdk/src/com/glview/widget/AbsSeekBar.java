/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.glview.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.glview.animation.ObjectAnimator;
import com.glview.content.GLContext;
import com.glview.graphics.Rect;
import com.glview.graphics.drawable.Drawable;
import com.glview.graphics.drawable.Insets;
import com.glview.hwui.GLCanvas;

public abstract class AbsSeekBar extends ProgressBar {
    private final Rect mTempRect = new Rect();

    private Drawable mThumb;
    private ColorStateList mThumbTintList = null;
    private PorterDuff.Mode mThumbTintMode = null;
    private boolean mHasThumbTint = false;
    private boolean mHasThumbTintMode = false;

    private int mThumbOffset;
    private boolean mSplitTrack;

    /**
     * On touch, this offset plus the scaled value from the position of the
     * touch will form the progress value. Usually 0.
     */
    float mTouchProgressOffset;

    /**
     * Whether this is user seekable.
     */
    boolean mIsUserSeekable = true;

    /**
     * On key presses (right or left), the amount to increment/decrement the
     * progress.
     */
    private int mKeyProgressIncrement = 1;
    private ObjectAnimator mPositionAnimator;
    private static final int PROGRESS_ANIMATION_DURATION = 250;


    private static final int NO_ALPHA = 0xFF;
    private float mDisabledAlpha;

    private int mScaledTouchSlop;
    private float mTouchDownX;
    private boolean mIsDragging;

    public AbsSeekBar(Context context) {
        super(context);
    }

    public AbsSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AbsSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AbsSeekBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray a = context.obtainStyledAttributes(
                attrs, com.glview.R.styleable.SeekBar, defStyleAttr, defStyleRes);

        final Drawable thumb = GLContext.get().getResources().getDrawable(a.getResourceId(com.glview.R.styleable.SeekBar_thumb, 0));
        setThumb(thumb);

        /*if (a.hasValue(com.glview.R.styleable.SeekBar_thumbTintMode)) {
            mThumbTintMode = Drawable.parseTintMode(a.getInt(
            		com.glview.R.styleable.SeekBar_thumbTintMode, -1), mThumbTintMode);
            mHasThumbTintMode = true;
        }

        if (a.hasValue(com.glview.R.styleable.SeekBar_thumbTint)) {
            mThumbTintList = a.getColorStateList(com.glview.R.styleable.SeekBar_thumbTint);
            mHasThumbTint = true;
        }*/

        // Guess thumb offset if thumb != null, but allow layout to override.
        final int thumbOffset = a.getDimensionPixelOffset(
        		com.glview.R.styleable.SeekBar_thumbOffset, getThumbOffset());
        setThumbOffset(thumbOffset);

//        mSplitTrack = a.getBoolean(com.glview.R.styleable.SeekBar_splitTrack, false);
        a.recycle();

        a = context.obtainStyledAttributes(attrs,
        		com.glview.R.styleable.Theme, 0, 0);
        mDisabledAlpha = a.getFloat(com.glview.R.styleable.Theme_disabledAlpha, 0.5f);
        a.recycle();

        applyThumbTint();

        mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    /**
     * Sets the thumb that will be drawn at the end of the progress meter within the SeekBar.
     * <p>
     * If the thumb is a valid drawable (i.e. not null), half its width will be
     * used as the new thumb offset (@see #setThumbOffset(int)).
     *
     * @param thumb Drawable representing the thumb
     */
    public void setThumb(Drawable thumb) {
        final boolean needUpdate;
        // This way, calling setThumb again with the same bitmap will result in
        // it recalcuating mThumbOffset (if for example it the bounds of the
        // drawable changed)
        if (mThumb != null && thumb != mThumb) {
            mThumb.setCallback(null);
            needUpdate = true;
        } else {
            needUpdate = false;
        }

        if (thumb != null) {
            thumb.setCallback(this);
            if (canResolveLayoutDirection()) {
                thumb.setLayoutDirection(getLayoutDirection());
            }

            // Assuming the thumb drawable is symmetric, set the thumb offset
            // such that the thumb will hang halfway off either edge of the
            // progress bar.
            mThumbOffset = thumb.getIntrinsicWidth() / 2;

            // If we're updating get the new states
            if (needUpdate &&
                    (thumb.getIntrinsicWidth() != mThumb.getIntrinsicWidth()
                        || thumb.getIntrinsicHeight() != mThumb.getIntrinsicHeight())) {
                requestLayout();
            }
        }

        mThumb = thumb;

        applyThumbTint();
        invalidate();

        if (needUpdate) {
            updateThumbAndTrackPos(getWidth(), getHeight());
            if (thumb != null && thumb.isStateful()) {
                // Note that if the states are different this won't work.
                // For now, let's consider that an app bug.
                int[] state = getDrawableState();
                thumb.setState(state);
            }
        }
    }

    /**
     * Return the drawable used to represent the scroll thumb - the component that
     * the user can drag back and forth indicating the current value by its position.
     *
     * @return The current thumb drawable
     */
    public Drawable getThumb() {
        return mThumb;
    }

    /**
     * Applies a tint to the thumb drawable. Does not modify the current tint
     * mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link #setThumb(Drawable)} will automatically
     * mutate the drawable and apply the specified tint and tint mode using
     * {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     *
     * @attr ref android.R.styleable#SeekBar_thumbTint
     * @see #getThumbTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setThumbTintList(ColorStateList tint) {
        mThumbTintList = tint;
        mHasThumbTint = true;

        applyThumbTint();
    }

    /**
     * Returns the tint applied to the thumb drawable, if specified.
     *
     * @return the tint applied to the thumb drawable
     * @attr ref android.R.styleable#SeekBar_thumbTint
     * @see #setThumbTintList(ColorStateList)
     */
    public ColorStateList getThumbTintList() {
        return mThumbTintList;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setThumbTintList(ColorStateList)}} to the thumb drawable. The
     * default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     *
     * @attr ref android.R.styleable#SeekBar_thumbTintMode
     * @see #getThumbTintMode()
     * @see Drawable#setTintMode(PorterDuff.Mode)
     */
    public void setThumbTintMode(PorterDuff.Mode tintMode) {
        mThumbTintMode = tintMode;
        mHasThumbTintMode = true;

        applyThumbTint();
    }

    /**
     * Returns the blending mode used to apply the tint to the thumb drawable,
     * if specified.
     *
     * @return the blending mode used to apply the tint to the thumb drawable
     * @attr ref android.R.styleable#SeekBar_thumbTintMode
     * @see #setThumbTintMode(PorterDuff.Mode)
     */
    public PorterDuff.Mode getThumbTintMode() {
        return mThumbTintMode;
    }

    private void applyThumbTint() {
        if (mThumb != null && (mHasThumbTint || mHasThumbTintMode)) {
            mThumb = mThumb.mutate();

//            if (mHasThumbTint) {
//                mThumb.setTintList(mThumbTintList);
//            }
//
//            if (mHasThumbTintMode) {
//                mThumb.setTintMode(mThumbTintMode);
//            }
        }
    }

    /**
     * @see #setThumbOffset(int)
     */
    public int getThumbOffset() {
        return mThumbOffset;
    }

    /**
     * Sets the thumb offset that allows the thumb to extend out of the range of
     * the track.
     *
     * @param thumbOffset The offset amount in pixels.
     */
    public void setThumbOffset(int thumbOffset) {
        mThumbOffset = thumbOffset;
        invalidate();
    }

    /**
     * Specifies whether the track should be split by the thumb. When true,
     * the thumb's optical bounds will be clipped out of the track drawable,
     * then the thumb will be drawn into the resulting gap.
     *
     * @param splitTrack Whether the track should be split by the thumb
     */
    public void setSplitTrack(boolean splitTrack) {
        mSplitTrack = splitTrack;
        invalidate();
    }

    /**
     * Returns whether the track should be split by the thumb.
     */
    public boolean getSplitTrack() {
        return mSplitTrack;
    }

    /**
     * Sets the amount of progress changed via the arrow keys.
     *
     * @param increment The amount to increment or decrement when the user
     *            presses the arrow keys.
     */
    public void setKeyProgressIncrement(int increment) {
        mKeyProgressIncrement = increment < 0 ? -increment : increment;
    }

    /**
     * Returns the amount of progress changed via the arrow keys.
     * <p>
     * By default, this will be a value that is derived from the max progress.
     *
     * @return The amount to increment or decrement when the user presses the
     *         arrow keys. This will be positive.
     */
    public int getKeyProgressIncrement() {
        return mKeyProgressIncrement;
    }

    @Override
    public synchronized void setMax(int max) {
        super.setMax(max);

        if ((mKeyProgressIncrement == 0) || (getMax() / mKeyProgressIncrement > 20)) {
            // It will take the user too long to change this via keys, change it
            // to something more reasonable
            setKeyProgressIncrement(Math.max(1, Math.round((float) getMax() / 20)));
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mThumb || super.verifyDrawable(who);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();

        if (mThumb != null) {
            mThumb.jumpToCurrentState();
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        final Drawable progressDrawable = getProgressDrawable();
        if (progressDrawable != null) {
            progressDrawable.setAlpha(isEnabled() ? NO_ALPHA : (int) (NO_ALPHA * mDisabledAlpha));
        }

        final Drawable thumb = mThumb;
        if (thumb != null && thumb.isStateful()) {
            thumb.setState(getDrawableState());
        }
    }

    @Override
    void onProgressRefresh(float scale, boolean fromUser) {
        super.onProgressRefresh(scale, fromUser);

        if (!isAnimationRunning()) {
            setThumbPos(scale);
        }
    }

    @Override
    void onAnimatePosition(float scale, boolean fromUser) {
        setThumbPos(scale);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        updateThumbAndTrackPos(w, h);
    }

    private void updateThumbAndTrackPos(int w, int h) {
        final Drawable track = getCurrentDrawable();
        final Drawable thumb = mThumb;

        // The max height does not incorporate padding, whereas the height
        // parameter does.
        final int trackHeight = Math.min(mMaxHeight, h - mPaddingTop - mPaddingBottom);
        final int thumbHeight = thumb == null ? 0 : thumb.getIntrinsicHeight();

        // Apply offset to whichever item is taller.
        final int trackOffset;
        final int thumbOffset;
        if (thumbHeight > trackHeight) {
            trackOffset = (thumbHeight - trackHeight) / 2;
            thumbOffset = 0;
        } else {
            trackOffset = 0;
            thumbOffset = (trackHeight - thumbHeight) / 2;
        }

        if (track != null) {
            track.setBounds(0, trackOffset, w - mPaddingRight - mPaddingLeft,
                    h - mPaddingBottom - trackOffset - mPaddingTop);
        }

        if (thumb != null) {
            setThumbPos(w, thumb, getScale(), thumbOffset);
        }
    }

    private float getScale() {
        final int max = getMax();
        return max > 0 ? getProgress() / (float) max : 0;
    }

    private void setThumbPos(float scale) {
        final Drawable thumb = mThumb;
        if (thumb != null) {
            setThumbPos(getWidth(), thumb, scale, Integer.MIN_VALUE);
            // Since we draw translated, the drawable's bounds that it signals
            // for invalidation won't be the actual bounds we want invalidated,
            // so just invalidate this whole view.
            invalidate();

        }
    }

    /**
     * Updates the thumb drawable bounds.
     *
     * @param w Width of the view, including padding
     * @param thumb Drawable used for the thumb
     * @param scale Current progress between 0 and 1
     * @param offset Vertical offset for centering. If set to
     *            {@link Integer#MIN_VALUE}, the current offset will be used.
     */
    private void setThumbPos(int w, Drawable thumb, float scale, int offset) {
        int available = w - mPaddingLeft - mPaddingRight;
        final int thumbWidth = thumb.getIntrinsicWidth();
        final int thumbHeight = thumb.getIntrinsicHeight();
        available -= thumbWidth;

        // The extra space for the thumb to move on the track
        available += mThumbOffset * 2;

        final int thumbPos = (int) (scale * available + 0.5f);

        final int top, bottom;
        if (offset == Integer.MIN_VALUE) {
            final Rect oldBounds = thumb.getBounds();
            top = oldBounds.top;
            bottom = oldBounds.bottom;
        } else {
            top = offset;
            bottom = offset + thumbHeight;
        }

        final int left = (isLayoutRtl() && mMirrorForRtl) ? available - thumbPos : thumbPos;
        final int right = left + thumbWidth;

        final Drawable background = getBackground();
        if (background != null) {
            final Rect bounds = thumb.getBounds();
            final int offsetX = mPaddingLeft - mThumbOffset;
            final int offsetY = mPaddingTop;
//            background.setHotspotBounds(left + offsetX, top + offsetY,
//                    right + offsetX, bottom + offsetY);
        }

        // Canvas will be translated, so 0,0 is where we start drawing
        thumb.setBounds(left, top, right, bottom);
    }

    /**
     * @hide
     */
    @Override
    public void onResolveDrawables(int layoutDirection) {
        super.onResolveDrawables(layoutDirection);

        if (mThumb != null) {
            mThumb.setLayoutDirection(layoutDirection);
        }
    }

    @Override
    protected synchronized void onDraw(GLCanvas canvas) {
        super.onDraw(canvas);
        drawThumb(canvas);

    }

    @Override
    void drawTrack(GLCanvas canvas) {
        final Drawable thumbDrawable = mThumb;
        if (thumbDrawable != null && mSplitTrack) {
            final Insets insets = thumbDrawable.getOpticalInsets();
            final Rect tempRect = mTempRect;
            thumbDrawable.copyBounds(tempRect);
            tempRect.offset(mPaddingLeft - mThumbOffset, mPaddingTop);
            tempRect.left += insets.left;
            tempRect.right -= insets.right;

            canvas.save();
            canvas.clipRect(tempRect/*, Op.DIFFERENCE*/);
            super.drawTrack(canvas);
            canvas.restore();
        } else {
            super.drawTrack(canvas);
        }
    }

    /**
     * Draw the thumb.
     */
    void drawThumb(GLCanvas canvas) {
        if (mThumb != null) {
            canvas.save();
            // Translate the padding. For the x, we need to allow the thumb to
            // draw in its extra space
            canvas.translate(mPaddingLeft - mThumbOffset, mPaddingTop);
            mThumb.draw(canvas);
            canvas.restore();
        }
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable d = getCurrentDrawable();

        int thumbHeight = mThumb == null ? 0 : mThumb.getIntrinsicHeight();
        int dw = 0;
        int dh = 0;
        if (d != null) {
            dw = Math.max(mMinWidth, Math.min(mMaxWidth, d.getIntrinsicWidth()));
            dh = Math.max(mMinHeight, Math.min(mMaxHeight, d.getIntrinsicHeight()));
            dh = Math.max(thumbHeight, dh);
        }
        dw += mPaddingLeft + mPaddingRight;
        dh += mPaddingTop + mPaddingBottom;

        setMeasuredDimension(resolveSizeAndState(dw, widthMeasureSpec, 0),
                resolveSizeAndState(dh, heightMeasureSpec, 0));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mIsUserSeekable || !isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isInScrollingContainer()) {
                    mTouchDownX = event.getX();
                } else {
                    setPressed(true);
                    if (mThumb != null) {
                        invalidate(mThumb.getBounds()); // This may be within the padding region
                    }
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    attemptClaimDrag();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mIsDragging) {
                    trackTouchEvent(event);
                } else {
                    final float x = event.getX();
                    if (Math.abs(x - mTouchDownX) > mScaledTouchSlop) {
                        setPressed(true);
                        if (mThumb != null) {
                            invalidate(mThumb.getBounds()); // This may be within the padding region
                        }
                        onStartTrackingTouch();
                        trackTouchEvent(event);
                        attemptClaimDrag();
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    // Touch up when we never crossed the touch slop threshold should
                    // be interpreted as a tap-seek to that location.
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                }
                // ProgressBar doesn't know to repaint the thumb drawable
                // in its inactive state when the touch stops (because the
                // value has not apparently changed)
                invalidate();
                break;

            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate(); // see above explanation
                break;
        }
        return true;
    }

    private void setHotspot(float x, float y) {
        final Drawable bg = getBackground();
        if (bg != null) {
//            bg.setHotspot(x, y);
        }
    }

    private void trackTouchEvent(MotionEvent event) {
        final int width = getWidth();
        final int available = width - mPaddingLeft - mPaddingRight;
        final int x = (int) event.getX();
        float scale;
        float progress = 0;
        if (isLayoutRtl() && mMirrorForRtl) {
            if (x > width - mPaddingRight) {
                scale = 0.0f;
            } else if (x < mPaddingLeft) {
                scale = 1.0f;
            } else {
                scale = (float)(available - x + mPaddingLeft) / (float)available;
                progress = mTouchProgressOffset;
            }
        } else {
            if (x < mPaddingLeft) {
                scale = 0.0f;
            } else if (x > width - mPaddingRight) {
                scale = 1.0f;
            } else {
                scale = (float)(x - mPaddingLeft) / (float)available;
                progress = mTouchProgressOffset;
            }
        }
        final int max = getMax();
        progress += scale * max;

        setHotspot(x, (int) event.getY());
        setProgress((int) progress, true);
    }

    /**
     * Tries to claim the user's drag motion, and requests disallowing any
     * ancestors from stealing events in the drag.
     */
    private void attemptClaimDrag() {
        if (mParent != null) {
            mParent.requestDisallowInterceptTouchEvent(true);
        }
    }

    /**
     * This is called when the user has started touching this widget.
     */
    void onStartTrackingTouch() {
        mIsDragging = true;
    }

    /**
     * This is called when the user either releases his touch or the touch is
     * canceled.
     */
    void onStopTrackingTouch() {
        mIsDragging = false;
    }

    /**
     * Called when the user changes the seekbar's progress by using a key event.
     */
    void onKeyChange() {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isEnabled()) {
            int progress = getProgress();
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (progress <= 0) break;
                    animateSetProgress(progress - mKeyProgressIncrement);
                    onKeyChange();
                    return true;

                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (progress >= getMax()) break;
                    animateSetProgress(progress + mKeyProgressIncrement);
                    onKeyChange();
                    return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    boolean isAnimationRunning() {
        return mPositionAnimator != null && mPositionAnimator.isRunning();
    }

    /**
     * @hide
     */
    @Override
    public void setProgress(int progress, boolean fromUser) {
        if (isAnimationRunning()) {
            mPositionAnimator.cancel();
        }
        super.setProgress(progress, fromUser);
    }

    void animateSetProgress(int progress) {
        float curProgress = isAnimationRunning() ? getAnimationPosition() : getProgress();

        if (progress < 0) {
            progress = 0;
        } else if (progress > getMax()) {
            progress = getMax();
        }
        setProgressValueOnly(progress);

        mPositionAnimator = ObjectAnimator.ofFloat(this, "animationPosition", curProgress,
                progress);
        mPositionAnimator.setDuration(PROGRESS_ANIMATION_DURATION);
        mPositionAnimator.setAutoCancel(true);
        mPositionAnimator.start();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);

        final Drawable thumb = mThumb;
        if (thumb != null) {
            setThumbPos(getWidth(), thumb, getScale(), Integer.MIN_VALUE);

            // Since we draw translated, the drawable's bounds that it signals
            // for invalidation won't be the actual bounds we want invalidated,
            // so just invalidate this whole view.
            invalidate();
        }
    }
}
