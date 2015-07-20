/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.glview.view;

import java.util.ArrayList;
import java.util.List;

import android.util.SparseArray;
import android.util.SparseIntArray;

import com.glview.animation.Animator;
import com.glview.animation.ObjectAnimator;
import com.glview.animation.TimeInterpolator;
import com.glview.view.ViewPropertyAnimator.NameValuesHolder;
import com.glview.view.animation.Interpolator;
import com.glview.view.animation.LinearInterpolator;


/**
 * This is a RenderThread driven backend for ViewPropertyAnimator.
 */
class ViewPropertyAnimatorRT {
	
	// Keep in sync with enum RenderProperty in Animator.h
    public static final int TRANSLATION_X = 0;
    public static final int TRANSLATION_Y = 1;
    public static final int TRANSLATION_Z = 2;
    public static final int SCALE_X = 3;
    public static final int SCALE_Y = 4;
    public static final int ROTATION = 5;
    public static final int ROTATION_X = 6;
    public static final int ROTATION_Y = 7;
    public static final int X = 8;
    public static final int Y = 9;
    public static final int Z = 10;
    public static final int ALPHA = 11;
    // The last value in the enum, used for array size initialization
    public static final int LAST_VALUE = ALPHA;
    
    // ViewPropertyAnimator uses a mask for its values, we need to remap them
    // to the enum values here. RenderPropertyAnimator can't use the mask values
    // directly as internally it uses a lookup table so it needs the values to
    // be sequential starting from 0
    private static final SparseIntArray sViewPropertyAnimatorMap = new SparseIntArray(15) {{
        put(ViewPropertyAnimator.TRANSLATION_X, TRANSLATION_X);
        put(ViewPropertyAnimator.TRANSLATION_Y, TRANSLATION_Y);
        put(ViewPropertyAnimator.TRANSLATION_Z, TRANSLATION_Z);
        put(ViewPropertyAnimator.SCALE_X, SCALE_X);
        put(ViewPropertyAnimator.SCALE_Y, SCALE_Y);
        put(ViewPropertyAnimator.ROTATION, ROTATION);
        put(ViewPropertyAnimator.ROTATION_X, ROTATION_X);
        put(ViewPropertyAnimator.ROTATION_Y, ROTATION_Y);
        put(ViewPropertyAnimator.X, X);
        put(ViewPropertyAnimator.Y, Y);
        put(ViewPropertyAnimator.Z, Z);
        put(ViewPropertyAnimator.ALPHA, ALPHA);
    }};
    
    private static final SparseArray<String> sViewPropertyNameAnimatorMap = new SparseArray<String>() {{
    	put(ViewPropertyAnimator.TRANSLATION_X, "translationX");
        put(ViewPropertyAnimator.TRANSLATION_Y, "translationY");
        put(ViewPropertyAnimator.TRANSLATION_Z, "translationZ");
        put(ViewPropertyAnimator.SCALE_X, "scaleX");
        put(ViewPropertyAnimator.SCALE_Y, "scaleY");
        put(ViewPropertyAnimator.ROTATION, "rotation");
        put(ViewPropertyAnimator.ROTATION_X, "rotationX");
        put(ViewPropertyAnimator.ROTATION_Y, "rotationY");
        put(ViewPropertyAnimator.X, "x");
        put(ViewPropertyAnimator.Y, "y");
        put(ViewPropertyAnimator.Z, "z");
        put(ViewPropertyAnimator.ALPHA, "alpha");
    }};

    private static final Interpolator sLinearInterpolator = new LinearInterpolator();

    private final View mView;

    private Animator mAnimators[] = new Animator[LAST_VALUE + 1];

    ViewPropertyAnimatorRT(View view) {
        mView = view;
    }

    /**
     * @return true if ViewPropertyAnimatorRT handled the animation,
     *         false if ViewPropertyAnimator needs to handle it
     */
    public boolean startAnimation(ViewPropertyAnimator parent) {
        cancelAnimators(parent.mPendingAnimations);
        if (!canHandleAnimator(parent)) {
            return false;
        }
        doStartAnimation(parent);
        return true;
    }

    public void cancelAll() {
    	if (!mView.isAttachedToWindow()) {
        	return;
        }
    	List<Animator> animators = new ArrayList<Animator>();
		for (int i = 0; i < mAnimators.length; i++) {
			if (mAnimators[i] != null && mAnimators[i].isStarted()) {
            	animators.add(mAnimators[i]);
            }
            mAnimators[i] = null;
        }
		if (animators.size() > 0) {
			mView.getViewRootImpl().stopRTAnimation(animators);
		}
    }

    private void doStartAnimation(final ViewPropertyAnimator parent) {
		int size = parent.mPendingAnimations.size();

        long startDelay = parent.getStartDelay();
        long duration = parent.getDuration();
        TimeInterpolator interpolator = parent.getInterpolator();
        if (interpolator == null) {
            // Documented to be LinearInterpolator in ValueAnimator.setInterpolator
            interpolator = sLinearInterpolator;
        }
        List<Animator> animators = new ArrayList<Animator>();
        for (int i = 0; i < size; i++) {
            NameValuesHolder holder = parent.mPendingAnimations.get(i);
            int property = sViewPropertyAnimatorMap.get(holder.mNameConstant);
            
            final float finalValue = holder.mFromValue + holder.mDeltaValue;
            Animator animator = ObjectAnimator.ofFloat(mView.mRenderNode, sViewPropertyNameAnimatorMap.get(holder.mNameConstant), finalValue);
            animator.setStartDelay(startDelay);
            animator.setDuration(duration);
            animator.setInterpolator(interpolator);
            mAnimators[property] = animator;
            animators.add(animator);
        }
        if (animators.size() > 0) {
			mView.getViewRootImpl().startRTAnimation(animators);
		}
        parent.mPendingAnimations.clear();
    }

    private boolean canHandleAnimator(ViewPropertyAnimator parent) {
        // TODO: Can we eliminate this entirely?
        // If RenderNode.animatorProperties() can be toggled to point at staging
        // instead then RNA can be used as the animators for software as well
        // as the updateListener fallback paths. If this can be toggled
        // at the top level somehow, combined with requiresUiRedraw, we could
        // ensure that RT does not self-animate, allowing for safe driving of
        // the animators from the UI thread using the same mechanisms
        // ViewPropertyAnimator does, just with everything sitting on a single
        // animator subsystem instead of multiple.

        if (parent.getUpdateListener() != null) {
            return false;
        }
        if (parent.getListener() != null) {
            // TODO support
            return false;
        }
        /*if (!mView.isHardwareAccelerated()) {
            // TODO handle this maybe?
            return false;
        }*/
        if (parent.hasActions()) {
            return false;
        }
        if (!mView.isAttachedToWindow()) {
        	return false;
        }
        // Here goes nothing...
        return true;
    }

    private void cancelAnimators(final ArrayList<NameValuesHolder> mPendingAnimations) {
    	if (!mView.isAttachedToWindow()) {
        	return;
        }
    	final int size = mPendingAnimations.size();
    	List<Animator> animators = new ArrayList<Animator>();
		for (int i = 0; i < size; i++) {
            NameValuesHolder holder = mPendingAnimations.get(i);
            int property = sViewPropertyAnimatorMap.get(holder.mNameConstant);
            if (mAnimators[property] != null && mAnimators[property].isStarted()) {
            	animators.add(mAnimators[property]);
            }
            mAnimators[property] = null;
        }
		if (animators.size() > 0) {
			mView.getViewRootImpl().stopRTAnimation(animators);
		}
    }

}
