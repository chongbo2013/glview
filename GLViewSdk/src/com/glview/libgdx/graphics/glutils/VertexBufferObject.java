/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.glview.libgdx.graphics.glutils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.glview.App;
import com.glview.exception.GLViewRuntimeException;
import com.glview.hwui.GLId;
import com.glview.libgdx.graphics.VertexAttribute;
import com.glview.libgdx.graphics.VertexAttributes;
import com.glview.libgdx.graphics.VertexAttributes.Usage;
import com.glview.libgdx.graphics.opengl.GL20;
import com.glview.utils.BufferUtils;

/** <p>
 * A {@link VertexData} implementation based on OpenGL vertex buffer objects.
 * </p>
 * 
 * <p>
 * If the OpenGL ES context was lost you can call {@link #invalidate()} to recreate a new OpenGL vertex buffer object. This class
 * can be used seamlessly with OpenGL ES 1.x and 2.0.
 * </p>
 * 
 * <p>
 * In case OpenGL ES 2.0 is used in the application the data is bound via glVertexAttribPointer() according to the attribute
 * aliases specified via {@link VertexAttributes} in the constructor.
 * </p>
 * 
 * <p>
 * Uses indirect Buffers on Android 1.5/1.6 to fix GC invocation due to leaking PlatformAddress instances.
 * </p>
 * 
 * <p>
 * VertexBufferObjects must be disposed via the {@link #dispose()} method when no longer needed
 * </p>
 * 
 * @author mzechner, Dave Clayton <contact@redskyforge.com> */
public class VertexBufferObject implements VertexData {
	final static IntBuffer tmpHandle = BufferUtils.newIntBuffer(1);

	final VertexAttributes attributes;
	final FloatBuffer buffer;
	final ByteBuffer byteBuffer;
	int bufferHandle;
	final boolean isStatic;
	final int usage;
	boolean isDirty = false;
	boolean isBound = false;

	/** Constructs a new interleaved VertexBufferObject.
	 * 
	 * @param isStatic whether the vertex data is static.
	 * @param numVertices the maximum number of vertices
	 * @param attributes the {@link VertexAttribute}s. */
	public VertexBufferObject (boolean isStatic, int numVertices, VertexAttribute... attributes) {
		this(isStatic, numVertices, new VertexAttributes(attributes));
	}

	/** Constructs a new interleaved VertexBufferObject.
	 * 
	 * @param isStatic whether the vertex data is static.
	 * @param numVertices the maximum number of vertices
	 * @param attributes the {@link VertexAttributes}. */
	public VertexBufferObject (boolean isStatic, int numVertices, VertexAttributes attributes) {
		this.isStatic = isStatic;
		this.attributes = attributes;

		byteBuffer = BufferUtils.newByteBuffer(this.attributes.vertexSize * numVertices);
		buffer = byteBuffer.asFloatBuffer();
		buffer.flip();
		byteBuffer.flip();
		bufferHandle = createBufferObject();
		usage = isStatic ? GL20.GL_STATIC_DRAW : GL20.GL_DYNAMIC_DRAW;
	}

	private int createBufferObject () {
		int array[] = new int[1];
		GLId.glGenBuffers(1, array, 0);
		return array[0];
	}

	/** {@inheritDoc} */
	@Override
	public VertexAttributes getAttributes () {
		return attributes;
	}

	/** {@inheritDoc} */
	@Override
	public int getNumVertices () {
		return buffer.limit() * 4 / attributes.vertexSize;
	}

	/** {@inheritDoc} */
	public int getNumMaxVertices () {
		return byteBuffer.capacity() / attributes.vertexSize;
	}

	/** {@inheritDoc} */
	@Override
	public FloatBuffer getBuffer () {
		isDirty = true;
		return buffer;
	}

	/** {@inheritDoc} */
	@Override
	public void setVertices (float[] vertices, int offset, int count) {
		isDirty = true;
		BufferUtils.copy(vertices, buffer, count, offset);
		buffer.position(0);
		buffer.limit(count);

		if (isBound) {
			if (App.getGL20() != null) {
				GL20 gl = App.getGL20();
				gl.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage);
			}
			isDirty = false;
		}
	}

	/** Binds this VertexBufferObject for rendering via glDrawArrays or glDrawElements
	 * 
	 * @param shader the shader */
	@Override
	public void bind (ShaderProgram shader) {
		bind(shader, null);
	}
	
	@Override
	public void bind (ShaderProgram shader, int[] locations) {
		
		if (bufferHandle == 0) throw new GLViewRuntimeException("No buffer allocated!");
		
		final GL20 gl = App.getGL20();

		gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, bufferHandle);
		if (isDirty) {
			byteBuffer.limit(buffer.limit() * 4);
			gl.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage);
			isDirty = false;
		}

		final int numAttributes = attributes.size();
		if (locations == null) {
			for (int i = 0; i < numAttributes; i++) {
				final VertexAttribute attribute = attributes.get(i);
				final int location = shader.getAttributeLocation(attribute.alias);
				if (location < 0)
					continue;
				shader.enableVertexAttribute(location);
	
				if (attribute.usage == Usage.ColorPacked)
					shader.setVertexAttribute(location, attribute.numComponents, GL20.GL_UNSIGNED_BYTE, true, attributes.vertexSize,
						attribute.offset);
				else
					shader.setVertexAttribute(location, attribute.numComponents, GL20.GL_FLOAT, false, attributes.vertexSize,
						attribute.offset);
			}
		} else {
			for (int i = 0; i < numAttributes; i++) {
				final VertexAttribute attribute = attributes.get(i);
				final int location = locations[i];
				if (location < 0)
					continue;
				shader.enableVertexAttribute(location);
	
				if (attribute.usage == Usage.ColorPacked)
					shader.setVertexAttribute(location, attribute.numComponents, GL20.GL_UNSIGNED_BYTE, true, attributes.vertexSize,
						attribute.offset);
				else
					shader.setVertexAttribute(location, attribute.numComponents, GL20.GL_FLOAT, false, attributes.vertexSize,
						attribute.offset);
			}
		}
		isBound = true;
	}

	/** Unbinds this VertexBufferObject.
	 * 
	 * @param shader the shader */
	@Override
	public void unbind (final ShaderProgram shader) {
		unbind(shader, null);
	}
	
	@Override
	public void unbind (final ShaderProgram shader, final int[] locations) {
		final GL20 gl = App.getGL20();
		final int numAttributes = attributes.size();
		if (locations == null) {
			for (int i = 0; i < numAttributes; i++) {
				shader.disableVertexAttribute(attributes.get(i).alias);
			}
		} else {
			for (int i = 0; i < numAttributes; i++) {
				final int location = locations[i];
				if (location >= 0)
					shader.disableVertexAttribute(location);
			}
		}
		gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
		isBound = false;
	}

	/** Invalidates the VertexBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss. */
	public void invalidate () {
		bufferHandle = createBufferObject();
		isDirty = true;
	}

	/** Disposes of all resources this VertexBufferObject uses. */
	@Override
	public void dispose () {
		if (App.getGL20() != null) {
			tmpHandle.clear();
			tmpHandle.put(bufferHandle);
			tmpHandle.flip();
			
			GL20 gl = App.getGL20();
			gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
			gl.glDeleteBuffers(1, tmpHandle);
			bufferHandle = 0;
		}
//		BufferUtils.disposeUnsafeByteBuffer(byteBuffer);
	}
}
