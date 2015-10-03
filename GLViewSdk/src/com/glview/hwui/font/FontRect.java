package com.glview.hwui.font;

import com.glview.hwui.packer.PackerRect;

class FontRect {

	public final CacheTexture mTexture;
	public final PackerRect mRect;
	public final int mLeft;
	public final int mTop;
	
	public FontRect(CacheTexture texture, PackerRect rect, int left, int top) {
		mTexture = texture;
		mRect = rect;
		mLeft = left;
		mTop = top;
	}
	
}
