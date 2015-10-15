/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.glview.text;

import com.glview.graphics.font.FontUtils;
import com.glview.hwui.GLPaint;

/**
 * @hide
 */
class MeasuredText {
    CharSequence mText;
    int mTextStart;
    float[] mWidths;
    char[] mChars;
    int mDir;
    int mLen;

    private int mPos;

    private MeasuredText() {
    }

    private static final Object[] sLock = new Object[0];
    private static final MeasuredText[] sCached = new MeasuredText[3];

    static MeasuredText obtain() {
        MeasuredText mt;
        synchronized (sLock) {
            for (int i = sCached.length; --i >= 0;) {
                if (sCached[i] != null) {
                    mt = sCached[i];
                    sCached[i] = null;
                    return mt;
                }
            }
        }
        mt = new MeasuredText();
        return mt;
    }

    static MeasuredText recycle(MeasuredText mt) {
        mt.mText = null;
        if (mt.mLen < 1000) {
            synchronized(sLock) {
                for (int i = 0; i < sCached.length; ++i) {
                    if (sCached[i] == null) {
                        sCached[i] = mt;
                        mt.mText = null;
                        break;
                    }
                }
            }
        }
        return null;
    }

    void setPos(int pos) {
        mPos = pos - mTextStart;
    }

    /**
     * Analyzes text for bidirectional runs.  Allocates working buffers.
     */
    void setPara(CharSequence text, int start, int end, TextDirectionHeuristic textDir) {
        mText = text;
        mTextStart = start;

        int len = end - start;
        mLen = len;
        mPos = 0;

        if (mWidths == null || mWidths.length < len) {
            mWidths = new float[len];
        }
        if (mChars == null || mChars.length < len) {
            mChars = new char[len];
        }
        TextUtils.getChars(text, start, end, mChars, 0);

        mDir = Layout.DIR_LEFT_TO_RIGHT;
    }

    float addStyleRun(GLPaint paint, int len, GLPaint.FontMetricsInt fm) {
        if (fm != null) {
            paint.getFontMetricsInt(fm);
        }

        int p = mPos;
        mPos = p + len;

        return FontUtils.getTextRunAdvances(paint, mChars, p, len, mWidths, p);
    }

    int breakText(int limit, boolean forwards, float width) {
        float[] w = mWidths;
        if (forwards) {
            int i = 0;
            while (i < limit) {
                width -= w[i];
                if (width < 0.0f) break;
                i++;
            }
            while (i > 0 && mChars[i - 1] == ' ') i--;
            return i;
        } else {
            int i = limit - 1;
            while (i >= 0) {
                width -= w[i];
                if (width < 0.0f) break;
                i--;
            }
            while (i < limit - 1 && mChars[i + 1] == ' ') i++;
            return limit - i - 1;
        }
    }

    float measure(int start, int limit) {
        float width = 0;
        float[] w = mWidths;
        for (int i = start; i < limit; ++i) {
            width += w[i];
        }
        return width;
    }
}
