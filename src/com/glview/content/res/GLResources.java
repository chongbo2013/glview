package com.glview.content.res;

import java.io.InputStream;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.SparseArray;
import android.util.TypedValue;

import com.glview.graphics.drawable.ColorDrawable;
import com.glview.graphics.drawable.Drawable;
import com.glview.graphics.drawable.Drawable.ConstantState;


public class GLResources {

	private static final SparseArray<ConstantState> sCacheDrawables = new SparseArray<ConstantState>();
	
	private static final byte[] sAccessLock = new byte[0];
	
	AssetManager mAssets;
	Resources mAndroidResources;
	
	private TypedValue mTmpValue = new TypedValue();
	
	public GLResources(AssetManager assets, Resources androidResources) {
		mAssets = assets;
		mAndroidResources = androidResources;
	}
	
	public Drawable getDrawable(int id) {
		if (id <= 0) return null;
		synchronized (sAccessLock) {
			Drawable dr = null;
			ConstantState cs = sCacheDrawables.get(id);
			if (cs != null) {
				dr = cs.newDrawable(mAndroidResources);
				return dr;
			}
			TypedValue value = mTmpValue;
			mAndroidResources.getValue(id, value, true);
			dr = loadDrawable(value, id);
			return dr;
		}
	}
	
	/*package*/ Drawable loadDrawable(TypedValue value, int id) throws NotFoundException {

		final boolean isColorDrawable;
		if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            isColorDrawable = true;
        } else {
            isColorDrawable = false;
        }
		
		final Drawable dr;
		if (isColorDrawable) {
			dr = new ColorDrawable(value.data);
		} else {
			dr = loadDrawableInner(value, id);
		}
		if (dr != null) {
			dr.setChangingConfigurations(value.changingConfigurations);
			cacheDrawable(id, dr);
		}
		return dr;
	}
	
	private Drawable loadDrawableInner(TypedValue value, int id) throws NotFoundException {
		if (value.string == null) {
            throw new NotFoundException("Resource \"" + mAndroidResources.getResourceName(id) + "\" ("
                    + Integer.toHexString(id) + ")  is not a Drawable (color or path): " + value);
        }
		final String file = value.string.toString();
		
		final Drawable dr;

        try {
            if (file.endsWith(".xml")) {
                final XmlResourceParser rp = mAssets.openXmlResourceParser(file);
                dr = Drawable.createFromXml(mAndroidResources, rp);
                rp.close();
            } else {
                final InputStream is = mAssets.openNonAssetFd(file).createInputStream();
                dr = Drawable.createFromResourceStream(mAndroidResources, value, is, file, null, true);
                is.close();
            }
        } catch (Exception e) {
            final NotFoundException rnf = new NotFoundException(
                    "File " + file + " from drawable resource ID #0x" + Integer.toHexString(id));
            rnf.initCause(e);
            throw rnf;
        }

        return dr;
	}
	
	private void cacheDrawable(int id, Drawable dr) {
		ConstantState cs = dr.getConstantState();
		if (cs == null) {
			return;
		}
		sCacheDrawables.put(id, cs);
	}
	
	/**
     * This exception is thrown by the resource APIs when a requested resource
     * can not be found.
     */
    public static class NotFoundException extends RuntimeException {
        /**
		 * 
		 */
		private static final long serialVersionUID = -8449329176119403199L;

		public NotFoundException() {
        }

        public NotFoundException(String name) {
            super(name);
        }
    }
}
