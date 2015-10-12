package com.glview.hwui.font;

import com.glview.graphics.font.GlyphSlot;
import com.glview.hwui.packer.PackerRect;

class FontRect {

	public final CacheTexture mTexture;
	public final PackerRect mRect;
	public final GlyphSlot mGlyphSlot;
	public final int mLeft;
	public final int mTop;
	
	public FontRect(CacheTexture texture, PackerRect rect, GlyphSlot slot, int left, int top) {
		mTexture = texture;
		mRect = rect;
		mGlyphSlot = slot;
		mLeft = left;
		mTop = top;
	}
	
}
