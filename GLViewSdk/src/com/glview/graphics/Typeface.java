package com.glview.graphics;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.WeakHashMap;

import com.glview.content.GLContext;
import com.glview.freetype.FreeType;
import com.glview.freetype.FreeType.Face;
import com.glview.freetype.FreeType.Library;

import android.content.res.AssetManager;
import android.util.Log;

/**
 * @author lijing.lj
 */
public class Typeface {
	
	private final static String TAG = "Typeface";
	
	static Library sLibrary;
	
	/** The default NORMAL typeface object */
    public static final Typeface DEFAULT;
    
    static int sCounter = 1;
    static WeakHashMap<Object, Typeface> sTypefaces = new WeakHashMap<Object, Typeface>();
	
	static {
		sLibrary = FreeType.initFreeType();
		
		Typeface typeface = createFromAsset(GLContext.get().getApplicationContext().getAssets(), "fonts/DroidSansFallback.ttf");
		if (typeface == null) {
			typeface = createFromFile(new File("/system/fonts/DroidSansFallback.ttf"));
		}
		DEFAULT = typeface;
	}
	
    public static Typeface createFromFile(File path) {
    	Typeface typeface = sTypefaces.get(path.getAbsolutePath());
		if (typeface != null) {
			return typeface;
		}
		InputStream is = null;
		try {
			is = new FileInputStream(path);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			int l = 0;
			byte[] buffer = new byte[1024];
			while ((l = is.read(buffer)) > 0) {
				bos.write(buffer, 0, l);
			}
			byte[] data = bos.toByteArray();
			Face face = sLibrary.newMemoryFace(data, data.length, 0);
			if (face != null) {
				typeface = new Typeface(face);
				sTypefaces.put(path, typeface);
				return typeface;
			}
		} catch (Throwable tr) {
			Log.w(TAG, "createFromAsset path=" + path, tr);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
		return null;
    }

	public static Typeface createFromAsset(AssetManager mgr, String path) {
		Typeface typeface = sTypefaces.get(path);
		if (typeface != null) {
			return typeface;
		}
		InputStream is = null;
		try {
			is = mgr.open(path);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			int l = 0;
			byte[] buffer = new byte[1024];
			while ((l = is.read(buffer)) > 0) {
				bos.write(buffer, 0, l);
			}
			byte[] data = bos.toByteArray();
			Face face = sLibrary.newMemoryFace(data, data.length, 0);
			if (face != null) {
				typeface = new Typeface(face);
				sTypefaces.put(path, typeface);
				return typeface;
			}
		} catch (Throwable tr) {
			Log.w(TAG, "createFromAsset path=" + path, tr);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
		return null;
	}
	
	private final Face mFace;
	private final int mIndex;
	
	public Typeface(Face face) {
		mFace = face;
		mIndex = sCounter++;
	}
	
	public Face face() {
		return mFace;
	}
	
	public int index() {
		return mIndex;
	}
	
}
