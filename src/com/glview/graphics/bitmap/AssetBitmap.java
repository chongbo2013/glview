package com.glview.graphics.bitmap;

import java.io.IOException;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.util.TypedValue;

import com.glview.content.GLContext;
import com.glview.graphics.Bitmap;

public class AssetBitmap extends Bitmap {
	
	final Resources mResources;
	final TypedValue mValue;
	final String mFile;
	final BitmapFactory.Options mOptions;
	final AssetManager mAsset;
	android.graphics.Rect mPad = new android.graphics.Rect();
	
	public AssetBitmap(Resources resources, TypedValue value, String file, BitmapFactory.Options options) {
		mResources = resources;
		mValue = value;
		mFile = file;
		mOptions = options;
		mAsset = GLContext.get().getApplicationContext().getAssets();
	}
	
	public AssetBitmap(android.graphics.Bitmap bitmap, Resources resources, TypedValue value, String file, BitmapFactory.Options options) {
		this(resources, value, file, options);
		setBitmap(bitmap);
	}
	
	@Override
	protected android.graphics.Bitmap onGotBitmap() {
		try {
			return BitmapFactory.decodeResourceStream(mResources, mValue, mAsset.openNonAssetFd(mFile).createInputStream(), mPad, mOptions);
		} catch (IOException e) {
		};
		return null;
	}
	
	@Override
	protected boolean desireFreeBitmap() {
		return true;
	}

}
