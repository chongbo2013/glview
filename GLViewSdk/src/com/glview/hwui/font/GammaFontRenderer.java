package com.glview.hwui.font;

class GammaFontRenderer {

	static ThreadLocal<GammaFontRenderer> sThreadLocal = new ThreadLocal<GammaFontRenderer>() {
		@Override
		protected GammaFontRenderer initialValue() {
			return new GammaFontRenderer();
		}
	};
	
	public static GammaFontRenderer instance() {
		return sThreadLocal.get();
	}
	
	byte[] mGammaTable = new byte[256];
	float mGamma = FontUtils.DEFAULT_TEXT_GAMMA;
	
	FontRenderer mFontRenderer;
	
	public GammaFontRenderer() {
		// Compute the gamma tables
	    final float gamma = 1.0f / mGamma;

	    for (int i = 0; i <= 255; i++) {
	        mGammaTable[i] = (byte) Math.floor(Math.pow(i / 255.0f, gamma) * 255.0f + 0.5f);
	    }
	}
	
	public FontRenderer getFontRenderer() {
		if (mFontRenderer == null) {
			FontRenderer renderer = new FontRenderer();
			renderer.setGammaTable(mGammaTable);
			mFontRenderer = renderer;
		}
		return mFontRenderer;
	}
}
