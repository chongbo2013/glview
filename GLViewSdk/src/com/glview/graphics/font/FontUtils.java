package com.glview.graphics.font;

import com.badlogic.gdx.graphics.g2d.freetype.FreeType;
import com.glview.hwui.GLPaint;
import com.glview.hwui.GLPaint.FontMetricsInt;

public class FontUtils {


	public static float measureText(GLPaint paint, CharSequence text, int start, int end) {
		if (text == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        if ((start | end | (end - start) | (text.length() - end)) < 0) {
            throw new IndexOutOfBoundsException();
        }

        if (text.length() == 0 || start == end) {
            return 0f;
        }
        FreeType.Face face = paint.getTypeface().face();
        face.setPixelSizes(0, paint.getTextSize());
        float r = 0f;
        for (int i = start; i < end; i ++) {
        	char c = text.charAt(i);
        	
        	r += measureChar(c, face);
        }
		return r;
	}
	
	public static float measureText(GLPaint paint, char[] text, int index, int count) {
		if (text == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        if ((index | count) < 0 || index + count > text.length) {
            throw new ArrayIndexOutOfBoundsException();
        }

        if (text.length == 0 || count == 0) {
            return 0f;
        }
        FreeType.Face face = paint.getTypeface().face();
        face.setPixelSizes(0, paint.getTextSize());
        float r = 0f;
        for (int i = index; i < index + count; i ++) {
        	char c = text[i];
        	
			r += measureChar(c, face);
        }
		return r;
	}
	
	private static float measureChar(char c, FreeType.Face face) {
		int charIndex = face.getCharIndex(c);
		if (charIndex == 0) {
			c = 0;
			charIndex = face.getCharIndex(c);
		}
		if (charIndex == 0) return 0f;
		if (!face.loadChar(c, FreeType.FT_LOAD_DEFAULT)) {
			return 0f;
		}
		FreeType.GlyphSlot slot = face.getGlyph();
		return FreeType.toInt(slot.getAdvanceX());
	}
	
	public static int getFontMetricsInt(GLPaint paint, FontMetricsInt fmi) {
		FreeType.Face face = paint.getTypeface().face();
        face.setPixelSizes(0, paint.getTextSize());
        FreeType.SizeMetrics fontMetrics = face.getSize().getMetrics();
        int lineHeight = FreeType.toInt(fontMetrics.getHeight());
        if (fmi != null) {
        	fmi.ascent = - FreeType.toInt(fontMetrics.getAscender()); // To Android Coordinate
        	fmi.top = fmi.ascent;
        	fmi.descent = - FreeType.toInt(fontMetrics.getDescender());
        	fmi.bottom = fmi.descent;
        	fmi.leading = 0;
//        	Log.d("lijing", "h=" + FreeType.toInt(fontMetrics.getHeight()));
        }
		return lineHeight;
	}
	
	public static float getTextRunAdvances(GLPaint paint, char[] chars, int index, int count, float[] advances, int advanceIndex) {
		FreeType.Face face = paint.getTypeface().face();
        face.setPixelSizes(0, paint.getTextSize());
        float totalAdvance = 0f;
        for (int i = 0; i < count; i ++) {
        	float advance = measureChar(chars[i + index], face);
        	advances[i + advanceIndex] = advance;
        	totalAdvance += advance;
        	
        }
        return totalAdvance;
	}
	
	/**
	 * For the purpose of layout, a word break is a boundary with no
	 * kerning or complex script processing. This is necessarily a
	 * heuristic, but should be accurate most of the time.
	 */
	public static boolean isWordBreak(char c) {
	    if (c == ' ' || (c >= 0x2000 && c <= 0x200a) || c == 0x3000) {
	        // spaces
	        return true;
	    }
	    if ((c >= 0x3400 && c <= 0x9fff)) {
	        // CJK ideographs (and yijing hexagram symbols)
	        return true;
	    }
	    // Note: kana is not included, as sophisticated fonts may kern kana
	    return false;
	}
}
