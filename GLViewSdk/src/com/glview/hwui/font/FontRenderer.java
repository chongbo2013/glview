package com.glview.hwui.font;

import java.nio.ByteBuffer;
import java.util.Vector;

import android.support.v4.util.LongSparseArray;

import com.badlogic.gdx.graphics.g2d.freetype.FreeType;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType.Face;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType.SizeMetrics;
import com.glview.graphics.Rect;
import com.glview.graphics.Typeface;
import com.glview.graphics.font.GlyphMetrics;
import com.glview.graphics.font.GlyphSlot;
import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;
import com.glview.hwui.packer.PackerRect;
import com.glview.libgdx.graphics.opengl.GL20;

public class FontRenderer {
	
	final static int FONT_BORDER_SIZE = 1;
	
	FontRenderer() {}
	
	public static FontRenderer instance() {
		return GammaFontRenderer.instance().getFontRenderer();
	}
	
	private boolean mInitialized;
	
	int mSmallCacheWidth = 512;
	int mSmallCacheHeight = 512;
	int mLargeCacheWidth = 1024;
	int mLargeCacheHeight = 1024;
	byte[] mGammaTable;

    Vector<CacheTexture> mACacheTextures = new Vector<CacheTexture>();
    Vector<CacheTexture> mRGBACacheTextures = new Vector<CacheTexture>();
    
    LongSparseArray<FontRect> mCacheRects = new LongSparseArray<FontRect>();
    
    LongSparseArray<FontData> mFontDatas = new LongSparseArray<FontData>();
    
    GLCanvas mCanvas = null;
    
    void setGammaTable(byte[] gammaTable) {
        mGammaTable = gammaTable;
    }
    
    public void release() {
    	clearCacheTextures(mACacheTextures);
    	clearCacheTextures(mRGBACacheTextures);
    	mCacheRects.clear();
    	mFontDatas.clear();
    	mInitialized = false;
    }
    
	// We don't want to allocate anything unless we actually draw text
	void checkInit() {
	    if (mInitialized) {
	        return;
	    }

	    initTextTexture();

	    mInitialized = true;
	}
	
	void initTextTexture() {
	    clearCacheTextures(mACacheTextures);
	    clearCacheTextures(mRGBACacheTextures);
//
//	    mUploadTexture = false;
	    mACacheTextures.add(createCacheTexture(mSmallCacheWidth, mSmallCacheHeight,
	    		GL20.GL_ALPHA, true));
	    mACacheTextures.add(createCacheTexture(mLargeCacheWidth, mLargeCacheHeight >> 1,
	    		GL20.GL_ALPHA, false));
	    mACacheTextures.add(createCacheTexture(mLargeCacheWidth, mLargeCacheHeight >> 1,
	            GL20.GL_ALPHA, false));
	    mACacheTextures.add(createCacheTexture(mLargeCacheWidth, mLargeCacheHeight,
	    		GL20.GL_ALPHA, false));
//	    mRGBACacheTextures.add(createCacheTexture(mSmallCacheWidth, mSmallCacheHeight,
//	    		GL20.GL_RGBA, false));
//	    mRGBACacheTextures.add(createCacheTexture(mLargeCacheWidth, mLargeCacheHeight >> 1,
//	    		GL20.GL_RGBA, false));
	}
	
	void clearCacheTextures(Vector<CacheTexture> cacheTextures) {
	    for (int i = 0; i < cacheTextures.size(); i++) {
	        cacheTextures.get(i).release();
	    }
	    cacheTextures.clear();
	}
	
	CacheTexture createCacheTexture(int width, int height, int format,
	        boolean allocate) {
	    CacheTexture cacheTexture = new CacheTexture(this, width, height, format);

	    if (allocate) {
	        cacheTexture.allocateTexture();
	        cacheTexture.allocateMesh();
	    }

	    return cacheTexture;
	}
	
	public void flushBatch() {
		for (CacheTexture cacheTexture : mACacheTextures) {
			if (cacheTexture.mFontBatch != null) {
				cacheTexture.mFontBatch.flush();
			}
		}
	}
	
	public void setGLCanvas(GLCanvas canvas) {
		mCanvas = canvas;
	}
	
	public GLCanvas getGLCanvas() {
		return mCanvas;
	}
 	
	public void renderText(GLCanvas canvas, CharSequence text, int start, int end, float x, float y,
			float alpha, GLPaint paint, Rect clip, float[] matrix, boolean forceFinish) {
		checkInit();
		Typeface typeface = paint.getTypeface();
		Face face = typeface.face();
		int textSize = paint.getTextSize();
		if (textSize < 5) return;
		face.setPixelSizes(0, textSize);
		long k = typeface.index() * 10000L | textSize;
		FontData fontData = mFontDatas.get(k);
		if (fontData == null) {
			fontData = new FontData();
			SizeMetrics fontMetrics = face.getSize().getMetrics();
			fontData.ascent = FreeType.toInt(fontMetrics.getAscender());
			fontData.descent = FreeType.toInt(fontMetrics.getDescender());
			fontData.lineHeight = FreeType.toInt(fontMetrics.getHeight());
			if (face.loadChar(' ', FreeType.FT_LOAD_DEFAULT)) {
				fontData.spaceWidth = FreeType.toInt(face.getGlyph().getMetrics().getHoriAdvance());
			} else {
				fontData.spaceWidth = FreeType.toInt(face.getMaxAdvanceWidth());
			}
		}
		
		float baseline = y;
		
		for (int index = 0; index < end - start; index ++) {
			char c = text.charAt(index + start);
			if (c == ' ') {
				x += fontData.spaceWidth;
				continue;
			}
			int charIndex = face.getCharIndex(c);
			if (charIndex == 0) {
				c = 0;
				charIndex = face.getCharIndex(c);
			}
			if (charIndex == 0) continue;
			long key = charIndex * 10000000L | typeface.index() * 10000L | textSize;
			FontRect r = mCacheRects.get(key);
			if (r == null) {
				if (!face.loadChar(c, FreeType.FT_LOAD_DEFAULT)) {
					continue;
				}
				FreeType.GlyphSlot slot = face.getGlyph();
				FreeType.Glyph mainGlyph = slot.getGlyph();
				try {
					mainGlyph.toBitmap(FreeType.FT_RENDER_MODE_NORMAL);
				} catch (RuntimeException e) {
					mainGlyph.dispose();
					continue;
				}
				FreeType.Bitmap mainBitmap = mainGlyph.getBitmap();
				FreeType.GlyphMetrics metrics = slot.getMetrics();
				int w = mainBitmap.getWidth();
				int h = mainBitmap.getRows();
				if (w <= 0 || h <= 0) {
					continue;
				}
				for (CacheTexture cacheTexture : mACacheTextures) {
					PackerRect rect = cacheTexture.mPacker.insert(w + FONT_BORDER_SIZE * 2, h + FONT_BORDER_SIZE * 2);
					if (rect != null) {
						r = new FontRect(cacheTexture, rect, new GlyphSlot(FreeType.toInt(slot.getAdvanceX()), FreeType.toInt(slot.getAdvanceY()), new GlyphMetrics(metrics.getWidth(), metrics.getHeight())), mainGlyph.getLeft(), mainGlyph.getTop());
						if (cacheTexture.getPixelBuffer() == null) {
							cacheTexture.allocateTexture();
						}
						ByteBuffer byteBuffer = cacheTexture.getPixelBuffer().map();
						ByteBuffer buffer = mainBitmap.getBuffer();
						int pitch = mainBitmap.getPitch();
//						byte[] tmp = new byte[rect.width() * rect.height()];
//						for (int i = 0; i < rect.height(); i ++) {
//							for (int j = 0; j < rect.width(); j ++) {
//								if (i < FONT_BORDER_SIZE || i >= rect.height() - FONT_BORDER_SIZE || j < FONT_BORDER_SIZE || j >= rect.width() - FONT_BORDER_SIZE) {
//									tmp[i * rect.width() + j] = 0;
//								} else {
//									tmp[i * rect.width() + j] = buffer.get((i - FONT_BORDER_SIZE) * pitch + j - FONT_BORDER_SIZE);
//								}
//							}
//						}
//						tmp = new JavaBlurProcess().blur(tmp, rect.width(), rect.height(), 15);
						if (mGammaTable != null) {
							for (int i = 0; i < rect.height(); i ++) {
								for (int j = 0; j < rect.width(); j ++) {
									if (i < FONT_BORDER_SIZE || i >= rect.height() - FONT_BORDER_SIZE || j < FONT_BORDER_SIZE || j >= rect.width() - FONT_BORDER_SIZE) {
										byteBuffer.put((i + rect.rect().top) * cacheTexture.mWidth + j + rect.rect().left, (byte) 0);
									} else {
										int t = buffer.get((i - FONT_BORDER_SIZE) * pitch + j - FONT_BORDER_SIZE) & 0xFF;
										byteBuffer.put((i + rect.rect().top) * cacheTexture.mWidth + j + rect.rect().left, mGammaTable[t]);
									}
								}
							}
						} else {
							for (int i = 0; i < rect.height(); i ++) {
								for (int j = 0; j < rect.width(); j ++) {
									if (i < FONT_BORDER_SIZE || i >= rect.height() - FONT_BORDER_SIZE || j < FONT_BORDER_SIZE || j >= rect.width() - FONT_BORDER_SIZE) {
										byteBuffer.put((i + rect.rect().top) * cacheTexture.mWidth + j + rect.rect().left, (byte) 0);
									} else {
										byteBuffer.put((i + rect.rect().top) * cacheTexture.mWidth + j + rect.rect().left, buffer.get((i - FONT_BORDER_SIZE) * pitch + j - FONT_BORDER_SIZE));
									}
								}
							}
						}
						cacheTexture.mDirtyRect.union(rect.rect());
						cacheTexture.setDirty(true);
						mCacheRects.put(key, r);
						break;
					}
				}
				mainGlyph.dispose();
			}
			if (r != null) {
				r.mTexture.allocateMesh();
				r.mTexture.mFontBatch.draw(x + r.mLeft, baseline/* - r.mRect.height()*/ - r.mTop, r.mRect.width(), r.mRect.height(), r.mRect.rect().left, r.mRect.rect().top, r.mRect.width(), r.mRect.height(), alpha, paint);
				x += r.mGlyphSlot.getAdvanceX();
			} else {
			}
		}
		if (forceFinish) {
			flushBatch();
		}
	}
	
	private static class FontData {
		int ascent;
		int descent;
		int lineHeight;
		int spaceWidth;
	}

}
