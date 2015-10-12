package com.glview.graphics.font;

public class GlyphSlot {

	int advanceX;
	
	int advanceY;
	
	GlyphMetrics metrics;
	
	public GlyphSlot() {
	}
	
	public GlyphSlot(int advanceX, int advanceY, GlyphMetrics metrics) {
		this.advanceX = advanceX;
		this.advanceY = advanceY;
		this.metrics = metrics;
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

	public GlyphMetrics getMetrics() {
		return metrics;
	}

	public void setMetrics(GlyphMetrics metrics) {
		this.metrics = metrics;
	}
	
}
