/*
 * com_glview_font_FontUtils.cpp
 *
 *  Created on: 2015年10月22日
 *      Author: lijing
 */
#include <com_glview_font_FontUtils.h>

#include <stdio.h>
#include <stdlib.h>
#include <android/log.h>

#define LOG_TAG "FontUtils"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

JNIEXPORT void JNICALL Java_com_glview_font_FontUtils_loadGlyphBitmap__Ljava_nio_ByteBuffer_2IIIILjava_nio_ByteBuffer_2IIII
  (JNIEnv * env, jclass clazz, jobject obj_src, jint srcWidth, jint srcHeight, jint pitch, jint border, jobject obj_dst, jint dstWidth, jint dstHeight, jint xOffset, jint yOffset) {
	unsigned char* src = (unsigned char*)(obj_src?env->GetDirectBufferAddress(obj_src):0);
	unsigned char* dst = (unsigned char*)(obj_dst?env->GetDirectBufferAddress(obj_dst):0);

	if (border == 0) {
		for (int i = 0 ; i < srcHeight; i ++) {
			memcpy(dst + dstWidth * (yOffset + i) + xOffset, src + i * pitch, srcWidth);
		}
	} else if (border == 1) {
		memset(dst + dstWidth * yOffset + xOffset, 0, srcWidth + 2);
		for (int i = 0 ; i < srcHeight; i ++) {
			dst[dstWidth * (yOffset + i) + xOffset] = 0;
			memcpy(dst + dstWidth * (yOffset + i) + xOffset + 1, src + i * pitch, srcWidth);
			dst[dstWidth * (yOffset + i) + xOffset + srcWidth + 1] = 0;
		}
		memset(dst + dstWidth * (yOffset + srcHeight) + xOffset, 0, srcWidth + 2);
	} else {
		for (int j = 0; j < border; j ++) {
			memset(dst + dstWidth * (yOffset + j) + xOffset, 0, srcWidth + border * 2);
		}
		for (int i = border ; i < srcHeight + border; i ++) {
			memset(dst + dstWidth * (yOffset + i) + xOffset, 0, border);
			memcpy(dst + dstWidth * (yOffset + i) + xOffset + border, src + i * pitch, srcWidth);
			memset(dst + dstWidth * (yOffset + i) + xOffset + srcWidth + border, 0, border);
		}
		for (int j = 0; j < border; j ++) {
			memset(dst + dstWidth * (yOffset + srcHeight + j) + xOffset, 0, srcWidth + border * 2);
		}
	}
}

JNIEXPORT void JNICALL Java_com_glview_font_FontUtils_loadGlyphBitmap___3BIIIILjava_nio_ByteBuffer_2IIII
  (JNIEnv * env, jclass clazz, jbyteArray obj_src, jint srcWidth, jint srcHeight, jint pitch, jint border, jobject obj_dst, jint dstWidth, jint dstHeight, jint xOffset, jint yOffset) {
	unsigned char* dst = (unsigned char*)(obj_dst?env->GetDirectBufferAddress(obj_dst):0);
	unsigned char* src = (unsigned char*)env->GetPrimitiveArrayCritical(obj_src, 0);
	if (border == 0) {
		for (int i = 0 ; i < srcHeight; i ++) {
			memcpy(dst + dstWidth * (yOffset + i) + xOffset, src + i * pitch, srcWidth);
		}
	} else if (border == 1) {
		memset(dst + dstWidth * yOffset + xOffset, 0, srcWidth + 2);
		for (int i = 0 ; i < srcHeight; i ++) {
			dst[dstWidth * (yOffset + i) + xOffset] = 0;
			memcpy(dst + dstWidth * (yOffset + i) + xOffset + 1, src + i * pitch, srcWidth);
			dst[dstWidth * (yOffset + i) + xOffset + srcWidth + 1] = 0;
		}
		memset(dst + dstWidth * (yOffset + srcHeight) + xOffset, 0, srcWidth + 2);
	} else {
		for (int j = 0; j < border; j ++) {
			memset(dst + dstWidth * (yOffset + j) + xOffset, 0, srcWidth + border * 2);
		}
		for (int i = border ; i < srcHeight + border; i ++) {
			memset(dst + dstWidth * (yOffset + i) + xOffset, 0, border);
			memcpy(dst + dstWidth * (yOffset + i) + xOffset + border, src + i * pitch, srcWidth);
			memset(dst + dstWidth * (yOffset + i) + xOffset + srcWidth + border, 0, border);
		}
		for (int j = 0; j < border; j ++) {
			memset(dst + dstWidth * (yOffset + srcHeight + j) + xOffset, 0, srcWidth + border * 2);
		}
	}
	env->ReleasePrimitiveArrayCritical(obj_src, src, 0);
}

JNIEXPORT void JNICALL Java_com_glview_font_FontUtils_loadGlyphBlurBitmap__Ljava_nio_ByteBuffer_2IIILjava_nio_ByteBuffer_2I
  (JNIEnv * env, jclass clazz, jobject obj_src, jint width, jint height, jint pitch, jobject obj_dst, jint radius) {
	unsigned char* src = (unsigned char*)(obj_src?env->GetDirectBufferAddress(obj_src):0);
	unsigned char* dst = (unsigned char*)(obj_dst?env->GetDirectBufferAddress(obj_dst):0);

	int dstWidth = width + radius * 2;
	int dstHeight = height + radius * 2;
	memset(dst, 0, dstWidth * dstHeight);
	for (int i = 0 ; i < height; i ++) {
		memcpy(dst + dstWidth * i + radius, src + i * pitch, width);
	}
}

JNIEXPORT void JNICALL Java_com_glview_font_FontUtils_loadGlyphBlurBitmap__Ljava_nio_ByteBuffer_2III_3BI
  (JNIEnv * env, jclass clazz, jobject obj_src, jint width, jint height, jint pitch, jbyteArray obj_dst, jint radius) {
	unsigned char* src = (unsigned char*)(obj_src?env->GetDirectBufferAddress(obj_src):0);
	unsigned char* dst = (unsigned char*)env->GetPrimitiveArrayCritical(obj_dst, 0);

	int dstWidth = width + radius * 2;
	int dstHeight = height + radius * 2;
	memset(dst, 0, dstWidth * dstHeight);
	for (int i = 0 ; i < height; i ++) {
		memcpy(dst + dstWidth * (i + radius) + radius, src + i * pitch, width);
	}
	env->ReleasePrimitiveArrayCritical(obj_dst, dst, 0);
}


