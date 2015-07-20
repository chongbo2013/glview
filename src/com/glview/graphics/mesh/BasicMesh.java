package com.glview.graphics.mesh;

import com.glview.libgdx.graphics.VertexAttribute;
import com.glview.libgdx.graphics.VertexAttributes;
import com.glview.libgdx.graphics.VertexAttributes.Usage;
import com.glview.libgdx.graphics.opengl.GL20;

public abstract class BasicMesh {
	
	private final static int INVALIDATE_COUNT = -1;
	
	public final static int DEFAULT_VERTEX_COUNT = 4;
	public final static int MIN_VERTEX_COUNT = 2;
	public final static int MAX_VERTEX_COUNT = 5000;
	public final static int DEFAULT_INDEX_COUNT = 0;
	public final static int MIN_INDEX_COUNT = 0;
	public final static int MAX_INDEX_COUNT = 7500;
	
	private int vertexCount = INVALIDATE_COUNT;
	private int indexCount = INVALIDATE_COUNT;
	
	VertexAttributes attributes;
	
	private boolean mHasTexcoordsAttr = false;
	private boolean mHasColorAttr = false;
	
	/**
	 * {@link GL20#GL_LINES}
	 * {@link GL20#GL_LINE_LOOP}
	 * {@link GL20#GL_LINE_STRIP}
	 * {@link GL20#GL_TRIANGLES}
	 * {@link GL20#GL_TRIANGLE_STRIP}
	 * {@link GL20#GL_TRIANGLE_FAN}
	 */
	int drawMode = GL20.GL_TRIANGLES;
	
	protected BasicMesh() {
	}
	
	public BasicMesh(VertexAttribute... attributes) {
		this(GL20.GL_TRIANGLES, attributes);
	}
	
	public BasicMesh(int drawMode, VertexAttribute... attributes) {
		setVertexAttributes(attributes);
		this.drawMode = drawMode;
	}
	
	public void setVertexAttributes(VertexAttribute[] attributes) {
		for (VertexAttribute a : attributes) {
			if (a.usage == Usage.TextureCoordinates) {
				mHasTexcoordsAttr = true;
			}
			if (a.usage == Usage.Color || a.usage == Usage.ColorPacked) {
				mHasColorAttr = true;
			}
		}
		this.attributes = new VertexAttributes(attributes);
	}
	
	public final int getVertexCount() {
		if (this.vertexCount == INVALIDATE_COUNT) {
			throw new IllegalStateException("vertexCount is invalidate");
		}
		return this.vertexCount;
	}
	
	public final void setVertexCount(int vertexCount) {
		if (this.vertexCount == INVALIDATE_COUNT) {
			if (vertexCount > MAX_VERTEX_COUNT) {
				throw new IllegalStateException("vertexCount larger than " + MAX_VERTEX_COUNT);
			}
			if (vertexCount < MIN_VERTEX_COUNT) {
				throw new IllegalStateException("vertexCount less than " + MIN_VERTEX_COUNT);
			}
			this.vertexCount = vertexCount;
		} else {
			throw new IllegalStateException("Can not change the constant value vertexCount");
		}
	}
	
	public final int getIndexCount() {
		return indexCount;
	}
	
	public final void setIndexCount(int indexCount) {
		if (this.indexCount == INVALIDATE_COUNT) {
			if (indexCount > MAX_INDEX_COUNT) {
				throw new IllegalStateException("indexCount larger than " + MAX_INDEX_COUNT);
			}
			if (indexCount < MIN_INDEX_COUNT) {
				throw new IllegalStateException("indexCount less than " + MIN_INDEX_COUNT);
			}
			this.indexCount = indexCount;
		} else {
			throw new IllegalStateException("Can not change the constant value indexCount");
		}
	}
	
	public VertexAttributes getVertexAttributes() {
		return attributes;
	}
	
	public final int getDrawMode() {
		return drawMode;
	}
	
	public final float[] getVertices() {
		float[] vertices = generateVertices();
		if (vertices.length * 4 > getVertexCount() * attributes.vertexSize) {
			throw new IllegalArgumentException("vertices too large!");
		}
		return vertices;
	}
	
	public final short[] getIndices() {
		short[] indices = generateIndices();
		if (indices.length > getIndexCount()) {
			throw new IllegalArgumentException("indices too large!");
		}
		return indices;
	}
	
	public float[] generateVertices() {
		return new float[]{};
	}
	
	public short[] generateIndices() {
		return new short[]{};
	}
	
	public final Object getKey() {
		return generateKey();
	}
	
	Object generateKey() {
		return this;
	}
	
	public final boolean hasTexCoordsAttr() {
		return mHasTexcoordsAttr;
	}
	
	public final boolean hasColorAttr() {
		return mHasColorAttr;
	}
	
	public boolean needReload() {
		return true;
	}

}
