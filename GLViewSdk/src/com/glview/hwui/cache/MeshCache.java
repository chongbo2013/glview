package com.glview.hwui.cache;

import android.support.v4.util.LruCache;

import com.glview.graphics.mesh.BasicMesh;
import com.glview.libgdx.graphics.Mesh;

public class MeshCache {
	
	final static int DEFAULT_VERTEX_CACHE_SIZE = 2 * 1024 * 1024; //10MB
	
	MeshLruCache mCache;
	
	public MeshCache() {
		mCache = new MeshLruCache(DEFAULT_VERTEX_CACHE_SIZE);
	}
	
	public void clear() {
		mCache.evictAll();
	}
	
	public Mesh get(BasicMesh basicMesh) {
		MeshDescription entry = mCache.get(basicMesh.getKey());
		if (entry == null) {
			entry = new MeshDescription();
			entry.mesh = new Mesh(false, basicMesh.getVertexCount(), basicMesh.getIndexCount(), basicMesh.getVertexAttributes());
			mCache.put(basicMesh.getKey(), entry);
		}
		entry.basicMesh = basicMesh;
		if (!entry.valid || basicMesh.needReload()) {
			entry.mesh.setVertices(basicMesh.getVertices());
			entry.mesh.setIndices(basicMesh.getIndices());
			entry.valid = true;
		}
		return entry.mesh;
	}
	
	static class MeshDescription {
		Mesh mesh;
		BasicMesh basicMesh;
		boolean valid = false;
	}
	
	class MeshLruCache extends LruCache<Object, MeshDescription> {

		public MeshLruCache(int maxSize) {
			super(maxSize);
		}
		
		@Override
		protected int sizeOf(Object key, MeshDescription value) {
			Mesh mesh = value.mesh;
			return mesh.getVertexSize() * mesh.getMaxVertices() + mesh.getMaxIndices() * 2;
		}
		
		@Override
		protected void entryRemoved(boolean evicted, Object key, MeshDescription oldValue,
				MeshDescription newValue) {
			oldValue.mesh.dispose();
		}
	}

}
