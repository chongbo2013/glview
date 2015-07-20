/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.glview.hwui;

import java.nio.IntBuffer;


// This mimics corresponding GL functions.
public class GLId {
    static int sNextId = 1;

    public synchronized static void glGenTextures(int n, IntBuffer buffers) {
    	buffers.clear();
        while (n-- > 0) {
        	buffers.put(sNextId++);
        }
        buffers.flip();
    }
    
    public synchronized static void glGenTextures(int n, int[] buffers, int offset) {
        while (n-- > 0) {
        	buffers[n] = sNextId++;
        }
    }

    public synchronized static void glGenBuffers(int n, IntBuffer buffers) {
    	buffers.clear();
        while (n-- > 0) {
        	buffers.put(sNextId++);
        }
        buffers.flip();
    }
    
    public synchronized static void glGenBuffers(int n, int[] buffers, int offset) {
    	while (n-- > 0) {
        	buffers[n] = sNextId++;
        }
    }
    
    /*public synchronized static void glDeleteTextures(GL11 gl, int n, int[] textures, int offset) {    	
    	if(Gdx.graphics.isGL20Available()){
    		GL20 gl20 = Gdx.graphics.getGL20();
    		
			ByteBuffer buffer = BufferUtils.newByteBuffer(n*Integer.SIZE/Byte.SIZE);		
			IntBuffer intBuffer = buffer.asIntBuffer();		
			
			intBuffer.put(textures, offset, n).position(0);
			
    		gl20.glDeleteTextures(n, intBuffer);
    	}else{
    		gl.glDeleteTextures(n, textures, offset);
    	}
    }

    public synchronized static void glDeleteBuffers(GL11 gl, int n, int[] buffers, int offset) {
    	if(Gdx.graphics.isGL20Available()){
    		GL20 gl20 = Gdx.graphics.getGL20();
    		
			ByteBuffer buffer = BufferUtils.newByteBuffer(n*Integer.SIZE/Byte.SIZE);		
			IntBuffer intBuffer = buffer.asIntBuffer();		
			
			intBuffer.put(buffers, offset, n).position(0);
			
    		gl20.glDeleteTextures(n, intBuffer);
    	}else{
    		gl.glDeleteBuffers(n, buffers, offset);
    	}
    }

    public synchronized static void glDeleteFramebuffers(
            GL11ExtensionPack gl11ep, int n, int[] buffers, int offset) {    	
        gl11ep.glDeleteFramebuffersOES(n, buffers, offset);
    }*/
}
