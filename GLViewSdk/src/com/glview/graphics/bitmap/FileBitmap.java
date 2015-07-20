package com.glview.graphics.bitmap;

import com.glview.graphics.Bitmap;

import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

public class FileBitmap extends Bitmap {

	final String mPathName;
	final Options mOptions;
	
	public FileBitmap(String pathName) {
		mPathName = pathName;
		mOptions = null;
	}
	
	public FileBitmap(android.graphics.Bitmap bitmap, String pathName) {
		setBitmap(bitmap);
		mPathName = pathName;
		mOptions = null;
	}
	
	public FileBitmap(android.graphics.Bitmap bitmap, String pathName, Options options) {
		setBitmap(bitmap);
		mPathName = pathName;
		mOptions = options;
	}
	
	@Override
	protected android.graphics.Bitmap onGotBitmap() {
		return BitmapFactory.decodeFile(mPathName, mOptions);
	}
	
	@Override
	protected boolean desireFreeBitmap() {
		return true;
	}

}
