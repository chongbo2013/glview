package com.glview.graphics.mesh;

import com.glview.hwui.GLPaint;
import com.glview.hwui.GLPaint.Style;
import com.glview.libgdx.graphics.VertexAttribute;
import com.glview.libgdx.graphics.VertexAttributes;
import com.glview.libgdx.graphics.VertexAttributes.Usage;
import com.glview.libgdx.graphics.glutils.ShaderProgram;
import com.glview.libgdx.graphics.opengl.GL20;

public class CircleMesh extends BasicMesh {

	private float mXRadius;
	private float mYRadius;

	private float[] mVertex;

	public CircleMesh(float xRadius, float yRadius) {
		this(xRadius, yRadius, null);
	}

	public CircleMesh(float xRadius, float yRadius, GLPaint paint) {
		attributes = new VertexAttributes(new VertexAttribute(Usage.Position,
				2, ShaderProgram.POSITION_ATTRIBUTE));
		drawMode = paint == null || paint.getStyle() == Style.FILL ? GL20.GL_TRIANGLE_FAN : GL20.GL_LINE_LOOP;

		mXRadius = xRadius;
		mYRadius = yRadius;
		int count = (int) (mXRadius > mYRadius ? mXRadius : mYRadius);
		count *= 4;
		if (count < MIN_VERTEX_COUNT) {
			count = MIN_VERTEX_COUNT;
		} else if (count > 500) {
			count = 500;
		}
		
		setVertexCount(count);
		setIndexCount(0);

		mVertex = new float[count * 2];
		initialXYVertices(count / 4);
	}

	@Override
	public float[] generateVertices() {
		return mVertex;
	}

	protected void initialXYVertices(int count) {
		float xCenter = 0;
		float yCenter = 0;

		float sinValue;
		float cosValue;

		double radians;

		int i, j;
		int vertexSize = 2;

		float delatAng = 90.0f / (count - 1);
		int offset1 = count * 1 * vertexSize;
		int offset2 = count * 2 * vertexSize;
		int offset3 = count * 3 * vertexSize;

		// axis
		/*
		 * | | 2 1 ----------------------------------- 3 4 | |
		 */

		for (i = 0; i < count; i++) {
			radians = Math.toRadians(i * delatAng);

			cosValue = (float) (Math.cos(-radians));
			sinValue = (float) (Math.sin(-radians));

			// circle 1
			j = i * vertexSize;
			mVertex[j] = xCenter + mXRadius * cosValue;
			mVertex[j + 1] = yCenter + mYRadius * sinValue;

			// circle 2
			j = i * vertexSize + offset1;
			mVertex[j] = xCenter + mXRadius * sinValue;
			mVertex[j + 1] = yCenter - mYRadius * cosValue;

			// circle 3
			j = i * vertexSize + offset2;
			mVertex[j] = xCenter - mXRadius * cosValue;
			mVertex[j + 1] = yCenter - mYRadius * sinValue;

			// circle 4
			j = i * vertexSize + offset3;
			mVertex[j] = xCenter - mXRadius * sinValue;
			mVertex[j + 1] = yCenter + mYRadius * cosValue;
		}
	}
	
	String key;
	
	@Override
	Object generateKey() {
		if (key == null) {
			key = getClass().getName() + "_" + mXRadius + "_" + mYRadius + "_" + drawMode;
		}
		return key;
	}
	
	@Override
	public boolean needReload() {
		return false;
	}

}
