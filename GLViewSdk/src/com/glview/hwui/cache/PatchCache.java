package com.glview.hwui.cache;

import java.util.Arrays;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.glview.graphics.drawable.ninepatch.NinePatch;
import com.glview.graphics.drawable.ninepatch.NinePatchChunk;
import com.glview.libgdx.graphics.Mesh;
import com.glview.libgdx.graphics.Mesh.VertexDataType;
import com.glview.libgdx.graphics.VertexAttribute;
import com.glview.libgdx.graphics.VertexAttributes.Usage;
import com.glview.libgdx.graphics.glutils.ShaderProgram;

public class PatchCache {
	
	final static int DEFAULT_PATCH_CACHE_SIZE = 128 * 1024; //128KB
	
	// We need 16 vertices for a normal nine-patch image (the 4x4 vertices)
	private static final int VERTEX_BUFFER_SIZE = 36 * 2;//16 * 2;

	// We need 22 indices for a normal nine-patch image, plus 2 for each
	// transparent region. Current there are at most 1 transparent region.
	private static final int INDEX_BUFFER_SIZE = 56 + 2;//22 + 2;
	
    final float mTmpVertices[] = new float[4*VERTEX_BUFFER_SIZE/2];
    final short mTmpIndices[]  = new short[INDEX_BUFFER_SIZE];
    private int mTmpIndicesCount = -1;
    private int mTmpVerticesCount = -1;
	
	final float mDivX[] = new float[8];
	final float mDivY[] = new float[8];
	final float mDivU[] = new float[8];
	final float mDivV[] = new float[8];
    
	PatchLruCache mCache;
	
	public PatchCache() {
		mCache = new PatchLruCache(DEFAULT_PATCH_CACHE_SIZE);
	}
	
	public void clear() {
		mCache.evictAll();
	}
	
	public Mesh get(int drawWidth, int drawHeight, NinePatch ninePatch) {
		String key = "" + drawWidth + "*" + drawHeight + "+" + ninePatch.getBitmap();
		Mesh mesh = mCache.get(key);
		if (mesh == null) {
			mesh = create(drawWidth, drawHeight, ninePatch);
			if (mesh != null) {
				mCache.put(key, mesh);
			}
		}
		return mesh;
	}
	
	private Mesh create(int width, int height, NinePatch patch) {
		
		NinePatchChunk chunk = patch.getChunk();
		
		int drawWidth = width;
		int drawHeight = height;
		if (drawWidth < 0 || drawHeight < 0) return null;
		
	     int nx = stretch(mDivX, mDivU, chunk.mDivX, patch.getBitmap().getWidth(), drawWidth, patch.getBitmap().getWidth());
	     int ny = stretch(mDivY, mDivV, chunk.mDivY, patch.getBitmap().getHeight(), drawHeight, patch.getBitmap().getHeight());
	
	     prepareVertexData(mDivX, mDivY, mDivU, mDivV, nx, ny, chunk.mColor);
	     
	     Mesh mesh = new Mesh(VertexDataType.VertexArray, false, mTmpVerticesCount, mTmpIndicesCount,
	        		new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE), 
					new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE));
		
		mesh.setIndices(mTmpIndices, 0, mTmpIndicesCount);
		mesh.setVertices(mTmpVertices, 0, mTmpVerticesCount * 4);
		return mesh;
	}
	
	private void prepareVertexData(float x[], float y[], float u[], float v[],
			int nx, int ny, int[] color) {
		/*
		 * Given a 3x3 nine-patch image, the vertex order is defined as the
		 * following graph:
		 * 
		 * (0) (1) (2) (3) | /| /| /| | / | / | / | (4) (5) (6) (7) | \ | \ | \
		 * | | \| \| \| (8) (9) (A) (B) | /| /| /| | / | / | / | (C) (D) (E) (F)
		 * 
		 * And we draw the triangle strip in the following index order:
		 * 
		 * index: 04152637B6A5948C9DAEBF
		 */
		int pntCount = 0;

		for (int j = 0; j < ny; ++j) {
			for (int i = 0; i < nx; ++i) {
				mTmpVertices[pntCount * 4 + 0] = x[i];
				mTmpVertices[pntCount * 4 + 1] = y[j];


				mTmpVertices[pntCount * 4 + 2] = u[i];
				mTmpVertices[pntCount * 4 + 3] = v[j];

				pntCount++;
			}
		}

		mTmpVerticesCount = pntCount;

		Arrays.fill(mTmpIndices, (short) -1);
		int idxCount = 1;
		boolean isForward = false;
		for (int row = 0; row < ny - 1; row++) {
			--idxCount;
			isForward = !isForward;

			int start, end, inc;
			if (isForward) {
				start = 0;
				end = nx;
				inc = 1;
			} else {
				start = nx - 1;
				end = -1;
				inc = -1;
			}
			for (int col = start; col != end; col += inc) {
				int k = row * nx + col;
				mTmpIndices[idxCount++] = (byte) k;
				mTmpIndices[idxCount++] = (byte) (k + nx);
			}
		}
		mTmpIndicesCount = idxCount;
	}
	 
	/**
     * Stretches the texture according to the nine-patch rules. It will
     * linearly distribute the strechy parts defined in the nine-patch chunk to
     * the target area.
     *
     * <pre>
     *                      source
     *          /--------------^---------------\
     *         u0    u1       u2  u3     u4   u5
     * div ---> |fffff|ssssssss|fff|ssssss|ffff| ---> u
     *          |    div0    div1 div2   div3  |
     *          |     |       /   /      /    /
     *          |     |      /   /     /    /
     *          |     |     /   /    /    /
     *          |fffff|ssss|fff|sss|ffff| ---> x
     *         x0    x1   x2  x3  x4   x5
     *          \----------v------------/
     *                  target
     *
     * f: fixed segment
     * s: stretchy segment
     * </pre>
     *
     * @param div the stretch parts defined in nine-patch chunk
     * @param source the length of the texture
     * @param target the length on the drawing plan
     * @param u output, the positions of these dividers in the texture
     *        coordinate
     * @param x output, the corresponding position of these dividers on the
     *        drawing plan
     * @return the number of these dividers.
     */
    private static int stretch(
            float x[], float u[], int div[], int source, int target, int sourceTextureSize) {
        int textureSize = sourceTextureSize;
        float textureBound = (float) source / textureSize;

        float stretch = 0;
        for (int i = 0, n = div.length; i < n; i += 2) {
            stretch += div[i + 1] - div[i];
        }

        float remaining = target - source + stretch;

        float lastX = 0;
        float lastU = 0;

        x[0] = 0;
        u[0] = 0;
        for (int i = 0, n = div.length; i < n; i += 2) {
            // Make the stretchy segment a little smaller to prevent sampling
            // on neighboring fixed segments.
            // fixed segment
            x[i + 1] = lastX + (div[i] - lastU) + 0.5f;
            u[i + 1] = Math.min((div[i] + 0.5f) / textureSize, textureBound);

            // stretchy segment
            float partU = div[i + 1] - div[i];
            float partX = remaining * partU / stretch;
            remaining -= partX;
            stretch -= partU;

            lastX = x[i + 1] + partX;
            lastU = div[i + 1];
            x[i + 2] = lastX - 0.5f;
            u[i + 2] = Math.min((lastU - 0.5f)/ textureSize, textureBound);
        }
        // the last fixed segment
        x[div.length + 1] = target;
        u[div.length + 1] = textureBound;

        // remove segments with length 0.
        int last = 0;
        for (int i = 1, n = div.length + 2; i < n; ++i) {
            if ((x[i] - x[last]) < 1f) continue;
            x[++last] = x[i];
            u[last] = u[i];
        }
        return last + 1;
    }
	class PatchLruCacheKey {
		int mDrawWidth, mDrawHeight;
		Bitmap mBitmap;
	}

	class PatchLruCache extends LruCache<Object, Mesh> {

		public PatchLruCache(int maxSize) {
			super(maxSize);
		}
		
		/**
		 * indices byte count + vertices byte count
		 */
		@Override
		protected int sizeOf(Object key, Mesh value) {
			// Short type index, Multiplied by 2., 
			return value.getNumIndices() * 2 + value.getNumVertices() * value.getVertexSize();
		}
		
		@Override
		protected void entryRemoved(boolean evicted, Object key,
				Mesh oldValue, Mesh newValue) {
			oldValue.dispose();
		}
		
		@Override
		protected Mesh create(Object key) {
			return null;
		}
		
		@Override
		protected void finalize() throws Throwable {
			super.finalize();
			evictAll();
		}
	}
}
