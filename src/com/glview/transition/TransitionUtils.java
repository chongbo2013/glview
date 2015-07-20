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

package com.glview.transition;

import com.glview.animation.Animator;
import com.glview.animation.AnimatorSet;
import com.glview.graphics.Bitmap;
import com.glview.view.View;
import com.glview.view.ViewGroup;
import com.glview.widget.ImageView;

/**
 * Static utility methods for Transitions.
 *
 * @hide
 */
public class TransitionUtils {
    private static int MAX_IMAGE_SIZE = (1024 * 1024);

    static Animator mergeAnimators(Animator animator1, Animator animator2) {
        if (animator1 == null) {
            return animator2;
        } else if (animator2 == null) {
            return animator1;
        } else {
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(animator1, animator2);
            return animatorSet;
        }
    }

    public static Transition mergeTransitions(Transition... transitions) {
        int count = 0;
        int nonNullIndex = -1;
        for (int i = 0; i < transitions.length; i++) {
            if (transitions[i] != null) {
                count++;
                nonNullIndex = i;
            }
        }

        if (count == 0) {
            return null;
        }

        if (count == 1) {
            return transitions[nonNullIndex];
        }

        TransitionSet transitionSet = new TransitionSet();
        for (int i = 0; i < transitions.length; i++) {
            if (transitions[i] != null) {
                transitionSet.addTransition(transitions[i]);
            }
        }
        return transitionSet;
    }
    
    /**
     * Creates a View using the bitmap copy of <code>view</code>. If <code>view</code> is large,
     * the copy will use a scaled bitmap of the given view.
     *
     * @param sceneRoot The ViewGroup in which the view copy will be displayed.
     * @param view The view to create a copy of.
     * @param parent The parent of view.
     */
    public static View copyViewImage(ViewGroup sceneRoot, View view, View parent) {
        int left = view.getLeft();
        int top = view.getTop();
        int right = view.getRight();
        int bottom = view.getBottom();

        ImageView copy = new ImageView(view.getContext());
        copy.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Bitmap bitmap = view.getDrawingCache();
        if (bitmap != null) {
            copy.setImageBitmap(bitmap);
        }
        int widthSpec = View.MeasureSpec.makeMeasureSpec(right - left, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(bottom - top, View.MeasureSpec.EXACTLY);
        copy.measure(widthSpec, heightSpec);
        copy.layout(left, top, right, bottom);
        return copy;
    }

    /*public static class MatrixEvaluator implements TypeEvaluator<Matrix> {

        float[] mTempStartValues = new float[9];

        float[] mTempEndValues = new float[9];

        Matrix mTempMatrix = new Matrix();

        @Override
        public Matrix evaluate(float fraction, Matrix startValue, Matrix endValue) {
            startValue.getValues(mTempStartValues);
            endValue.getValues(mTempEndValues);
            for (int i = 0; i < 9; i++) {
                float diff = mTempEndValues[i] - mTempStartValues[i];
                mTempEndValues[i] = mTempStartValues[i] + (fraction * diff);
            }
            mTempMatrix.setValues(mTempEndValues);
            return mTempMatrix;
        }
    }*/
}
