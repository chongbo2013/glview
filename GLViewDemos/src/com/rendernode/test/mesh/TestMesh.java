package com.rendernode.test.mesh;

import java.util.Arrays;

import android.util.Log;

import com.glview.graphics.mesh.BasicMesh;
import com.glview.libgdx.graphics.VertexAttribute;
import com.glview.libgdx.graphics.VertexAttributes.Usage;
import com.glview.libgdx.graphics.glutils.ShaderProgram;
import com.glview.libgdx.graphics.opengl.GL20;

public class TestMesh extends BasicMesh {

	private final static int WIDTH_MESH_COUNT = 4;
	private final static int HEIGHT_MESH_COUNT = 4;
	
	float mWidth, mHeight;
	float mRadius;
	float mPos1, mPos2;
	
	float[] vertices;
	short[] indices;
	
	public TestMesh(float w, float h) {
		super(GL20.GL_TRIANGLES, new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE));
		mWidth = w;
		mHeight = h;
		mRadius = mWidth / 10;
		mPos1 = (float) (Math.PI * mRadius / 2 - mRadius);
		mPos2 = (float) (Math.PI * mRadius);
		
		vertices = new float[(WIDTH_MESH_COUNT + 1) * (HEIGHT_MESH_COUNT + 1) * 2];
		indices = new short[WIDTH_MESH_COUNT * HEIGHT_MESH_COUNT * 2 * 3];
		
		initialMeshXYUV();
//		setCurrentPosition(mWidth / 2);
		initialIndices();

		setVertexCount(vertices.length / 2);
		setIndexCount(indices.length);
	}
	
	@Override
	public short[] generateIndices() {
		return indices;
	}
	
	@Override
	public float[] generateVertices() {
		return vertices;
	}
	
	private void initialMeshXYUV() {
		float dx, dy;
		int loop = 0;
		
		for (int y = 0; y <= HEIGHT_MESH_COUNT; y ++) {
			dy = y * mHeight / HEIGHT_MESH_COUNT;
			for (int x = 0; x <= WIDTH_MESH_COUNT; x ++) {
				dx = x * mWidth / WIDTH_MESH_COUNT;
				vertices[loop] = dx;
				vertices[loop + 1] = dy;
//				vertices[loop + 2] = dx /mWidth;
//				vertices[loop + 3] = dy / mHeight;
				loop += 2;
			}
		}
		Log.d("lijing", "initialMeshXYUV=" + Arrays.toString(vertices));
	}
	
	public void setCurrentPosition(float position) {
		float p = mWidth - position;
		float tiltPosition = 0;
		
		if (p < mPos1) {
			// w - tp - p 
			// TODO
		} else if (p < mPos2) {
			// TODO
		} else {
			// w - p + (w - tp - PI * R) = tp
			tiltPosition = (float) ((mWidth - p + mWidth - Math.PI * mRadius) / 2);
		}
		
		float dx, dy;
		int loop = 0;
		for (int x = 0; x <= WIDTH_MESH_COUNT; x ++) {
			dx = x * mWidth / WIDTH_MESH_COUNT;
			if (dx > tiltPosition) {
				float d = dx - tiltPosition;
				if (d > Math.PI * mRadius) {
					dx = (float) (tiltPosition - (d - Math.PI * mRadius));
				} else {
					dx = (float) (tiltPosition + Math.sin(d / mRadius) * mRadius);
				}
			}
			for (int y = 0; y <= HEIGHT_MESH_COUNT; y ++) {
				dy = y * mHeight / HEIGHT_MESH_COUNT;
				vertices[loop] = dx;
				vertices[loop + 1] = dy;
				loop += 4;
			}
		}
	}
	
	private void initialIndices() {
		int loop = 0;
		for (int y = 0; y < HEIGHT_MESH_COUNT; y ++) {
			for (int x = 0; x < WIDTH_MESH_COUNT; x ++) {
				indices[loop] = (short) (y * (WIDTH_MESH_COUNT + 1) + x);
				indices[loop + 1] = (short) (y * (WIDTH_MESH_COUNT + 1) + x + WIDTH_MESH_COUNT + 1);
				indices[loop + 2] = (short) (y * (WIDTH_MESH_COUNT + 1) + x + 1);
				indices[loop + 3] = (short) (y * (WIDTH_MESH_COUNT + 1) + x + 1);
				indices[loop + 4] = (short) (y * (WIDTH_MESH_COUNT + 1) + x + WIDTH_MESH_COUNT + 1);
				indices[loop + 5] = (short) (y * (WIDTH_MESH_COUNT + 1) + x + 1 + WIDTH_MESH_COUNT + 1);
				loop += 6;
			}
		}
		Log.d("lijing", "initialIndices=" + Arrays.toString(indices));
	}

}
