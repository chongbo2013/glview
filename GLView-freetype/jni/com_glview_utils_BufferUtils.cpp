/*
 * com_glview_utils_BufferUtils.cpp
 *
 *  Created on: 2015年10月22日
 *      Author: lijing
 */
#include <com_glview_utils_BufferUtils.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

JNIEXPORT void JNICALL Java_com_glview_utils_BufferUtils_copyJni
  (JNIEnv * env, jclass clazz, jobject obj_src, jint srcOffset, jobject obj_dst, jint dstOffset, jint numBytes) {

	unsigned char* src = (unsigned char*)(obj_src?env->GetDirectBufferAddress(obj_src):0);
	unsigned char* dst = (unsigned char*)(obj_dst?env->GetDirectBufferAddress(obj_dst):0);

	memcpy(dst + dstOffset, src + srcOffset, numBytes);
}


