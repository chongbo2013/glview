package com.glview.text;

import com.glview.R;
import com.glview.content.GLContext;
import com.glview.hwui.GLPaint;

public class TextUtils {

	private TextUtils() { /* cannot be instantiated */ }

    public static void getChars(CharSequence s, int start, int end,
                                char[] dest, int destoff) {
        Class<? extends CharSequence> c = s.getClass();

        if (c == String.class)
            ((String) s).getChars(start, end, dest, destoff);
        else if (c == StringBuffer.class)
            ((StringBuffer) s).getChars(start, end, dest, destoff);
        else if (c == StringBuilder.class)
            ((StringBuilder) s).getChars(start, end, dest, destoff);
        else {
            for (int i = start; i < end; i++)
                dest[destoff++] = s.charAt(i);
        }
    }

	public enum TruncateAt {
        START,
        MIDDLE,
        END,
        MARQUEE,
        /**
         * @hide
         */
        END_SMALL
    }
	
	public interface EllipsizeCallback {
        /**
         * This method is called to report that the specified region of
         * text was ellipsized away by a call to {@link #ellipsize}.
         */
        public void ellipsized(int start, int end);
    }

    /**
     * Returns the original text if it fits in the specified width
     * given the properties of the specified Paint,
     * or, if it does not fit, a truncated
     * copy with ellipsis character added at the specified edge or center.
     */
    public static CharSequence ellipsize(CharSequence text,
                                         GLPaint p,
                                         float avail, TruncateAt where) {
        return ellipsize(text, p, avail, where, false, null);
    }

    /**
     * Returns the original text if it fits in the specified width
     * given the properties of the specified Paint,
     * or, if it does not fit, a copy with ellipsis character added
     * at the specified edge or center.
     * If <code>preserveLength</code> is specified, the returned copy
     * will be padded with zero-width spaces to preserve the original
     * length and offsets instead of truncating.
     * If <code>callback</code> is non-null, it will be called to
     * report the start and end of the ellipsized range.  TextDirection
     * is determined by the first strong directional character.
     */
    public static CharSequence ellipsize(CharSequence text,
                                         GLPaint paint,
                                         float avail, TruncateAt where,
                                         boolean preserveLength,
                                         EllipsizeCallback callback) {

        final String ellipsis = (where == TruncateAt.END_SMALL) ?
                GLContext.get().getApplicationContext().getString(R.string.ellipsis_two_dots) :
                	GLContext.get().getApplicationContext().getString(R.string.ellipsis);

        return ellipsize(text, paint, avail, where, preserveLength, callback,
                ellipsis);
    }

    /**
     * Returns the original text if it fits in the specified width
     * given the properties of the specified Paint,
     * or, if it does not fit, a copy with ellipsis character added
     * at the specified edge or center.
     * If <code>preserveLength</code> is specified, the returned copy
     * will be padded with zero-width spaces to preserve the original
     * length and offsets instead of truncating.
     * If <code>callback</code> is non-null, it will be called to
     * report the start and end of the ellipsized range.
     *
     * @hide
     */
    public static CharSequence ellipsize(CharSequence text,
            GLPaint paint,
            float avail, TruncateAt where,
            boolean preserveLength,
            EllipsizeCallback callback, String ellipsis) {

        int len = text.length();

        MeasuredText mt = MeasuredText.obtain();
        try {
            mt.setPara(text, 0, len);
            float width = mt.addStyleRun(paint, len, null);
            if (width <= avail) {
                if (callback != null) {
                    callback.ellipsized(0, 0);
                }

                return text;
            }

            // XXX assumes ellipsis string does not require shaping and
            // is unaffected by style
            float ellipsiswid = paint.measureText(ellipsis);
            avail -= ellipsiswid;

            int left = 0;
            int right = len;
            if (avail < 0) {
                // it all goes
            } else if (where == TruncateAt.START) {
                right = len - mt.breakText(len, false, avail);
            } else if (where == TruncateAt.END || where == TruncateAt.END_SMALL) {
                left = mt.breakText(len, true, avail);
            } else {
                right = len - mt.breakText(len, false, avail / 2);
                avail -= mt.measure(right, len);
                left = mt.breakText(right, true, avail);
            }

            if (callback != null) {
                callback.ellipsized(left, right);
            }

            char[] buf = mt.mChars;

            int remaining = len - (right - left);
            if (preserveLength) {
                if (remaining > 0) { // else eliminate the ellipsis too
                    buf[left++] = ellipsis.charAt(0);
                }
                for (int i = left; i < right; i++) {
                    buf[i] = ZWNBS_CHAR;
                }
                String s = new String(buf, 0, len);
                return s;
            }

            if (remaining == 0) {
                return "";
            }

            StringBuilder sb = new StringBuilder(remaining + ellipsis.length());
            sb.append(buf, 0, left);
            sb.append(ellipsis);
            sb.append(buf, right, len - right);
            return sb.toString();

        } finally {
            MeasuredText.recycle(mt);
        }
    }
    
    /* package */ static char[] obtain(int len) {
        char[] buf;

        synchronized (sLock) {
            buf = sTemp;
            sTemp = null;
        }

        if (buf == null || buf.length < len)
            buf = new char[len];

        return buf;
    }

    /* package */ static void recycle(char[] temp) {
        if (temp.length > 1000)
            return;

        synchronized (sLock) {
            sTemp = temp;
        }
    }
    
    private static Object sLock = new Object();

    private static char[] sTemp = null;
    
    private static final char ZWNBS_CHAR = '\uFEFF';
}
