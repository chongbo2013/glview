package com.glview.hwui;

import static javax.microedition.khronos.egl.EGL10.EGL_BAD_NATIVE_WINDOW;
import static javax.microedition.khronos.egl.EGL10.EGL_DEFAULT_DISPLAY;
import static javax.microedition.khronos.egl.EGL10.EGL_DRAW;
import static javax.microedition.khronos.egl.EGL10.EGL_NONE;
import static javax.microedition.khronos.egl.EGL10.EGL_NO_CONTEXT;
import static javax.microedition.khronos.egl.EGL10.EGL_NO_DISPLAY;
import static javax.microedition.khronos.egl.EGL10.EGL_NO_SURFACE;
import static javax.microedition.khronos.egl.EGL10.EGL_SUCCESS;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;

import android.opengl.EGL14;
import android.opengl.GLUtils;
import android.util.Log;

/**
 * Learn from Android5.0 hwui library.
 * @author lijing.lj
 */
class EglManager {
	
	final static String TAG = "EglManager";
	final static boolean DEBUG = true;
	
	int mEGLContextClientVersion = 2;

	static EGL10 sEgl;
    static EGLDisplay sEglDisplay;
    static EGLConfig sEglConfig;
    
    EGLContext mEglContext;
    
    EglManager() {
    	initializeEgl();
	}
    
	void initializeEgl() {
		if (hasEglContext()) return;
		
        if (sEgl == null && sEglConfig == null) {
            sEgl = (EGL10) EGLContext.getEGL();

            // Get to the default display.
            sEglDisplay = sEgl.eglGetDisplay(EGL_DEFAULT_DISPLAY);

            if (sEglDisplay == EGL_NO_DISPLAY) {
                throw new RuntimeException("eglGetDisplay failed "
                        + GLUtils.getEGLErrorString(sEgl.eglGetError()));
            }

            // We can now initialize EGL for that display
            int[] version = new int[2];
            if (!sEgl.eglInitialize(sEglDisplay, version)) {
                throw new RuntimeException("eglInitialize failed " +
                        GLUtils.getEGLErrorString(sEgl.eglGetError()));
            }

            checkEglErrorsForced();

            sEglConfig = loadEglConfig();
        }

        if (mEglContext == null) {
        	// Create the EGLContext
            mEglContext = createContext(sEgl, sEglDisplay, sEglConfig);
        }

    }
	
	boolean hasEglContext() {
		return mEglContext != null;
	}
	
	private boolean checkEglErrorsForced() {
        int error = sEgl.eglGetError();
        if (error != EGL_SUCCESS) {
            // Something bad has happened
            Log.w(TAG, "EGL error: " + GLUtils.getEGLErrorString(error));
            destroy();
            return false;
        }
        return true;
    }
	
	private EGLConfig loadEglConfig() {
		return new GLViewEGLConfigChooser(8, 8, 8, 8, 8, 0, 0, mEGLContextClientVersion).chooseConfig(sEgl, sEglDisplay);
//		int[] attribs;
//		attribs = new int[] {
//				EGL10.EGL_RED_SIZE, 8,
//				EGL10.EGL_GREEN_SIZE, 8,
//				EGL10.EGL_BLUE_SIZE, 8,
//				EGL10.EGL_ALPHA_SIZE, 8,
//				EGL10.EGL_DEPTH_SIZE, 0,
//				EGL10.EGL_CONFIG_CAVEAT, EGL_NONE,
//				EGL10.EGL_STENCIL_SIZE, 0,
//				EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT,
//				EGL10.EGL_NONE
//	    };
//			
//		attribs = filterConfigSpec(attribs);
//		int num_configs[] = new int[]{1};
//		EGLConfig[] configs = new EGLConfig[num_configs.length];
//		if (!sEgl.eglChooseConfig(sEglDisplay, attribs, configs, num_configs.length, num_configs) || num_configs[0] != 1) {
//			throw new RuntimeException("eglConfig choose failed");
//		}
//        EGLConfig eglConfig = configs[0];
//        if (eglConfig == null) {
//            throw new RuntimeException("eglConfig not initialized");
//        }
//        return eglConfig;
    }
	
//	private int[] filterConfigSpec(int[] configSpec) {
//        if (mEGLContextClientVersion != 2 && mEGLContextClientVersion != 3) {
//            return configSpec;
//        }
//        /* We know none of the subclasses define EGL_RENDERABLE_TYPE.
//         * And we know the configSpec is well formed.
//         */
//        int len = configSpec.length;
//        int[] newConfigSpec = new int[len + 2];
//        System.arraycopy(configSpec, 0, newConfigSpec, 0, len-1);
//        newConfigSpec[len-1] = EGL10.EGL_RENDERABLE_TYPE;
//        if (mEGLContextClientVersion == 2) {
//            newConfigSpec[len] = EGL14.EGL_OPENGL_ES2_BIT;  /* EGL_OPENGL_ES2_BIT */
//        } else {
//            newConfigSpec[len] = EGLExt.EGL_OPENGL_ES3_BIT_KHR; /* EGL_OPENGL_ES3_BIT_KHR */
//        }
//        newConfigSpec[len+1] = EGL10.EGL_NONE;
//        return newConfigSpec;
//    }
	
	EGLContext createContext(EGL10 egl, EGLDisplay eglDisplay, EGLConfig eglConfig) {
        int[] attribs = { EGL14.EGL_CONTEXT_CLIENT_VERSION, mEGLContextClientVersion, EGL_NONE };

        EGLContext context = egl.eglCreateContext(eglDisplay, eglConfig, EGL_NO_CONTEXT,
        		mEGLContextClientVersion != 0 ? attribs : null);
        if (context == null || context == EGL_NO_CONTEXT) {
            //noinspection ConstantConditions
            throw new IllegalStateException(
                    "Could not create an EGL context. eglCreateContext failed with error: " +
                    GLUtils.getEGLErrorString(sEgl.eglGetError()));
        }

        return context;
    }
	
	EGLSurface createSurface(Object surface) {
		initializeEgl();
        EGLSurface eglSurface = null;
        try {
        	eglSurface = sEgl.eglCreateWindowSurface(sEglDisplay, sEglConfig, surface, null);
        } catch (IllegalArgumentException e) {
            // This exception indicates that the surface flinger surface
            // is not valid. This can happen if the surface flinger surface has
            // been torn down, but the application has not yet been
            // notified via SurfaceHolder.Callback.surfaceDestroyed.
            // In theory the application should be notified first,
            // but in practice sometimes it is not. See b/4588890
            Log.e(TAG, "eglCreateWindowSurface", e);
        }
        if (eglSurface == null || eglSurface == EGL_NO_SURFACE) {
            int error = sEgl.eglGetError();
            Log.e(TAG, "createWindowSurface failed "
                    + GLUtils.getEGLErrorString(error));
            if (error == EGL_BAD_NATIVE_WINDOW) {
                Log.e(TAG, "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
            }
            return null;
        }
        if (!makeCurrent(eglSurface)) {
        	Log.e(TAG, "eglMakeCurrent failed " +
                    GLUtils.getEGLErrorString(sEgl.eglGetError()));
        }
        return eglSurface;
    }
	
	boolean makeCurrent(EGLSurface surface) {
		if (surface == null || surface == EGL10.EGL_NO_SURFACE) {
    		return false;
    	}
    	if (!surface.equals(sEgl.eglGetCurrentSurface(EGL_DRAW)) && !sEgl.eglMakeCurrent(sEglDisplay, surface, surface, mEglContext)) {
    		return false;
    	}
    	return true;
	}
	
	void destroySurface(EGLSurface surface) {
		if (surface != null && surface != EGL_NO_SURFACE) {
            if (surface.equals(sEgl.eglGetCurrentSurface(EGL_DRAW))) {
                sEgl.eglMakeCurrent(sEglDisplay,
                        EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
            }
            sEgl.eglDestroySurface(sEglDisplay, surface);
        }
	}
	
	GL getGL() {
		initializeEgl();
		return mEglContext.getGL();
	}
	
	void destroy() {
		if (mEglContext != null) {
			sEgl.eglDestroyContext(sEglDisplay, mEglContext);
			mEglContext = null;
		}
	}
	
	boolean swapBuffers(EGLSurface surface) {
		sEgl.eglSwapBuffers(sEglDisplay, surface);
		int error = sEgl.eglGetError();
		if (error == EGL10.EGL_SUCCESS) {
			return true;
		}
		Log.w(TAG, "EGL error: " + GLUtils.getEGLErrorString(error));
		if (error == EGL10.EGL_BAD_SURFACE) {
			// Tell them to recreate the surface.
			return false;
		}
		// Shouldn't reach here. Maybe we lost our egl context?
		// Check this error and fix it.
		throw new RuntimeException(String.format("Encountered EGL error %d %s during rendering", error, GLUtils.getEGLErrorString(error)));
//		destroy();
//		return false;
	}
}
