package com.glview.graphics.font;

public class GlyphSlot {

	int advanceX;
	
	int advanceY;
	
	public GlyphSlot() {
	}
	
	public GlyphSlot(int advanceX, int advanceY) {
		this.advanceX = advanceX;
		this.advanceY = advanceY;
	}

	public int getAdvanceX() {
		return advanceX;
	}

	public void setAdvanceX(int advanceX) {
		this.advanceX = advanceX;
	}

	public int getAdvanceY() {
		return advanceY;
	}

	public void setAdvanceY(int advanceY) {
		this.advanceY = advanceY;
	}
	
}
