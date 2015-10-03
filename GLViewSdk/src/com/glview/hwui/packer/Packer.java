package com.glview.hwui.packer;

public interface Packer {
	
	public PackerRect insert(int width, int height);
	
	public void reset();
	
	public String dump();

}
