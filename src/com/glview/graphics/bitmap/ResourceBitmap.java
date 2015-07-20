package com.glview.graphics.bitmap;

import com.glview.graphics.Bitmap;

import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

public class ResourceBitmap extends Bitmap {
	
	final int mResourceId;
	final Resources mResources;
	final Options mOptions;

	public ResourceBitmap(Resources resources, int resourceId) {
		this(resources, resourceId, null);
	}
	
	public ResourceBitmap(Resources resources, int resourceId, Options options) {
		mResources = resources;
		mResourceId = resourceId;
		mOptions = options;
		setBitmap(onGotBitmap());
	}
	
	@Override
	protected android.graphics.Bitmap onGotBitmap() {
		return BitmapFactory.decodeResource(mResources, mResourceId, mOptions);
	}
	
	@Override
	protected boolean desireFreeBitmap() {
		return true;
	}

}
