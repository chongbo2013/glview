package com.rendernode.test.mesh;

import com.glview.graphics.mesh.BasicMesh;
import com.glview.libgdx.graphics.VertexAttribute;
import com.glview.libgdx.graphics.VertexAttributes.Usage;
import com.glview.libgdx.graphics.glutils.ShaderProgram;
import com.glview.libgdx.graphics.opengl.GL20;

public class TestMesh1 extends BasicMesh {

	float[] vertices;
	short[] indices;
	
	public TestMesh1() {
		super(GL20.GL_TRIANGLES, new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE));
		
		vertices = new float[18];
		
		vertices[0] = 0;
		vertices[1] = 0;
		vertices[2] = 100;
		vertices[3] = 0;
		vertices[4] = 200;
		vertices[5] = 0;
		
		vertices[6] = 0;
		vertices[7] = 100;
		vertices[8] = 100;
		vertices[9] = 100;
		vertices[10] = 200;
		vertices[11] = 100;
		
		vertices[12] = 0;
		vertices[13] = 200;
		vertices[14] = 100;
		vertices[15] = 200;
		vertices[16] = 200;
		vertices[17] = 200;
		
		indices = new short[6];
		indices[0] = 0;
		indices[1] =  1;
		indices[2] = 3;
		indices[3] = 3;
		indices[4] = 6;
		indices[5] = 4;
		
		setVertexCount(9);
		setIndexCount(indices.length);
	}
	
	@Override
	public float[] generateVertices() {
		return vertices;
	}
	
	@Override
	public short[] generateIndices() {
		return indices;
	}
}
