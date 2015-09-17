package com.glview.hwui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

import com.glview.App;
import com.glview.libgdx.graphics.opengl.GL20;

public class Extensions {
	
	private final static String TAG = "Extensions";
	
	final GL20 mGL;
	
	final ArrayList<String> mGlExtensionList = new ArrayList<String>();
	Pattern mVersionPattern = Pattern.compile(".*OpenGL ES (\\d*)\\.(\\d*).*");
	
	boolean mHasNPot;
    boolean mHasFramebufferFetch;
    boolean mHasDiscardFramebuffer;
    boolean mHasDebugMarker;
    boolean mHasDebugLabel;
    boolean mHasTiledRendering;
    boolean mHas1BitStencil;
    boolean mHas4BitStencil;
    boolean mHasNvSystemTime;

    int mVersionMajor;
    int mVersionMinor;
	
	Extensions() {
		mGL = App.getGL20();
		Log.d(TAG, "mGL=" + mGL);
		String extensions = mGL != null ? mGL.glGetString(GL20.GL_EXTENSIONS) : null;
		Log.i(TAG, "GL_EXTENSIONS=" + extensions);
		findExtensions(extensions, mGlExtensionList);
		mHasNPot = hasGlExtension("GL_OES_texture_npot");
	    mHasFramebufferFetch = hasGlExtension("GL_NV_shader_framebuffer_fetch");
	    mHasDiscardFramebuffer = hasGlExtension("GL_EXT_discard_framebuffer");
	    mHasDebugMarker = hasGlExtension("GL_EXT_debug_marker");
	    mHasDebugLabel = hasGlExtension("GL_EXT_debug_label");
	    mHasTiledRendering = hasGlExtension("GL_QCOM_tiled_rendering");
	    mHas1BitStencil = hasGlExtension("GL_OES_stencil1");
	    mHas4BitStencil = hasGlExtension("GL_OES_stencil4");
	    
	    String version = mGL != null ? mGL.glGetString(GL20.GL_VERSION) : null;
	    Log.i(TAG, "GL_VERSION=" + version);
	    //"OpenGL ES %d.%d"
	    try {
	    	Matcher m = mVersionPattern.matcher(version);
			if (m.matches()) {
				mVersionMajor = Integer.valueOf(m.group(1));
				mVersionMinor = Integer.valueOf(m.group(2));
			} else {
				mVersionMajor = 2;
		        mVersionMinor = 0;
			}
	    } catch (Throwable throwable) {
	    	Log.w(TAG, "GL_VERSION=" + version, throwable);
	    	mVersionMajor = 2;
	        mVersionMinor = 0;
	    }
	}
	
	private void findExtensions(String extensions, List<String> list) {
		if (extensions != null) {
			String[] arr = extensions.split(" ");
			if (arr != null) {
				for (String extension : arr) {
					extension = extension != null ? extension.trim() : null;
					if (extension != null && extension.length() > 0) {
						list.add(extension);
					}
				}
			}
		}
	}
	
	public boolean hasGlExtension(String extension) {
		return mGlExtensionList.indexOf(extension) >= 0;
	}
	
	public boolean hasNPot() { return mHasNPot; }
	public boolean hasFramebufferFetch() { return mHasFramebufferFetch; }
	public boolean hasDiscardFramebuffer() { return mHasDiscardFramebuffer; }
	public boolean hasDebugMarker() { return mHasDebugMarker; }
	public boolean hasDebugLabel() { return mHasDebugLabel; }
	public boolean hasTiledRendering() { return mHasTiledRendering; }
	public boolean has1BitStencil() { return mHas1BitStencil; }
	public boolean has4BitStencil() { return mHas4BitStencil; }
	public boolean hasNvSystemTime() { return mHasNvSystemTime; }
	public boolean hasUnpackRowLength() { return mVersionMajor >= 3; }
	public boolean hasPixelBufferObjects() { return mVersionMajor >= 3; }
	public boolean hasOcclusionQueries() { return mVersionMajor >= 3; }
	public boolean hasFloatTextures() { return mVersionMajor >= 3; }

	public int getMajorGlVersion() { return mVersionMajor; }
	public int getMinorGlVersion() { return mVersionMinor; }
	
}
