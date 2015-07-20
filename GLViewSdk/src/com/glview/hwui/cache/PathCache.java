package com.glview.hwui.cache;

import android.support.v4.util.LruCache;

import com.glview.graphics.mesh.BasicMesh;
import com.glview.hwui.GLPaint;

public class PathCache {
	
	final static int DEFAULT_VERTEX_CACHE_SIZE = 10 * 1024 * 1024; //10MB
	
	PathLruCache mCache = new PathLruCache(DEFAULT_VERTEX_CACHE_SIZE);

	public BasicMesh getCircle(float xradius, float yradius, GLPaint paint) {
		
		return null;
	}
	
	class PathLruCache extends LruCache<Object, BasicMesh> {

		public PathLruCache(int maxSize) {
			super(maxSize);
		}
		
	}
}
