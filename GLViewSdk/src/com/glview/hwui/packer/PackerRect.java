package com.glview.hwui.packer;

import com.glview.graphics.Rect;

public class PackerRect {
	
	private Rect mRect = new Rect();
	
	public boolean rotation = false;
	
	public PackerRect() {
	}

	public PackerRect(int left, int top, int right, int bottom) {
		mRect.set(left, top, right, bottom);
	}

	public PackerRect(Rect r) {
		mRect.set(r);
	}
	
	public Rect rect() {
		return mRect;
	}
	
	public void set(int left, int top, int right, int bottom) {
		mRect.set(left, top, right, bottom);
	}
	
	public int width() {
		return mRect.width();
	}
	
	public int height() {
		return mRect.height();
	}
	
	@Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(mRect.toString());
        sb.append(", rotation=");
        sb.append(rotation);
        return sb.toString();
    }

}
