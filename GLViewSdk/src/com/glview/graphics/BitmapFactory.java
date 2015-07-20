/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.glview.graphics;

import java.io.FileDescriptor;
import java.io.InputStream;

import com.glview.graphics.bitmap.AssetBitmap;
import com.glview.graphics.bitmap.FileBitmap;
import com.glview.graphics.bitmap.ResourceBitmap;

import android.content.res.Resources;
import android.graphics.BitmapFactory.Options;
import android.util.TypedValue;

/**
 * Creates Bitmap objects from various sources, including files, streams,
 * and byte-arrays.
 */
public class BitmapFactory {

	static ThreadLocal<android.graphics.Rect> sThreadLocalRect = new ThreadLocal<android.graphics.Rect>() {
		protected android.graphics.Rect initialValue() {
			return new android.graphics.Rect();
		};
	};
	
    /**
     * Decode a file path into a bitmap. If the specified file name is null,
     * or cannot be decoded into a bitmap, the function returns null.
     *
     * @param pathName complete path name for the file to be decoded.
     * @param opts null-ok; Options that control downsampling and whether the
     *             image should be completely decoded, or just is size returned.
     * @return The decoded bitmap, or null if the image data could not be
     *         decoded, or, if opts is non-null, if opts requested only the
     *         size be returned (in opts.outWidth and opts.outHeight)
     */
    public static Bitmap decodeFile(String pathName, Options opts) {
    	android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeFile(pathName, opts);
    	if (bm != null) return new FileBitmap(bm, pathName, opts);
    	else return null;
    }

    /**
     * Decode a file path into a bitmap. If the specified file name is null,
     * or cannot be decoded into a bitmap, the function returns null.
     *
     * @param pathName complete path name for the file to be decoded.
     * @return the resulting decoded bitmap, or null if it could not be decoded.
     */
    public static Bitmap decodeFile(String pathName) {
        return decodeFile(pathName, null);
    }
    
    /**
     * Decode a new Bitmap from an InputStream. This InputStream was obtained from
     * resources, which we pass to be able to scale the bitmap accordingly.
     */
    public static Bitmap decodeResourceStream(Resources res, TypedValue value,
            InputStream is, Rect pad, Options opts) {
    	android.graphics.Rect apad = sThreadLocalRect.get();
    	android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeResourceStream(res, value, is, apad, opts);
    	if (pad != null) {
    		pad.set(apad.left, apad.top, apad.right, apad.bottom);
    	}
    	if (bm != null) return new Bitmap(bm);
    	else return null;
    }

    /**
     * Decode a new Bitmap from an InputStream. This InputStream was obtained from
     * resources, which we pass to be able to scale the bitmap accordingly.
     */
    public static Bitmap decodeResourceStream(Resources res, TypedValue value,
            InputStream is, Rect pad, String srcName, Options opts) {
    	android.graphics.Rect apad = sThreadLocalRect.get();
    	android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeResourceStream(res, value, is, apad, opts);
    	if (pad != null) {
    		pad.set(apad.left, apad.top, apad.right, apad.bottom);
    	}
    	if (bm != null) return srcName != null ? new AssetBitmap(bm, res, value, srcName, opts) : new Bitmap(bm);
    	else return null;
    }

    /**
     * Synonym for opening the given resource and calling
     * {@link #decodeResourceStream}.
     *
     * @param res   The resources object containing the image data
     * @param id The resource id of the image data
     * @param opts null-ok; Options that control downsampling and whether the
     *             image should be completely decoded, or just is size returned.
     * @return The decoded bitmap, or null if the image data could not be
     *         decoded, or, if opts is non-null, if opts requested only the
     *         size be returned (in opts.outWidth and opts.outHeight)
     */
    public static Bitmap decodeResource(Resources res, int id, Options opts) {
    	android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeResource(res, id, opts);
    	if (bm != null) return new ResourceBitmap(res, id, opts);
    	else return null;
    }

    /**
     * Synonym for {@link #decodeResource(Resources, int, android.graphics.BitmapFactory.Options)}
     * with null Options.
     *
     * @param res The resources object containing the image data
     * @param id The resource id of the image data
     * @return The decoded bitmap, or null if the image could not be decoded.
     */
    public static Bitmap decodeResource(Resources res, int id) {
        return decodeResource(res, id, null);
    }

    /**
     * Decode an immutable bitmap from the specified byte array.
     *
     * @param data byte array of compressed image data
     * @param offset offset into imageData for where the decoder should begin
     *               parsing.
     * @param length the number of bytes, beginning at offset, to parse
     * @param opts null-ok; Options that control downsampling and whether the
     *             image should be completely decoded, or just is size returned.
     * @return The decoded bitmap, or null if the image data could not be
     *         decoded, or, if opts is non-null, if opts requested only the
     *         size be returned (in opts.outWidth and opts.outHeight)
     */
    public static Bitmap decodeByteArray(byte[] data, int offset, int length, Options opts) {
    	android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeByteArray(data, offset, length, opts);
    	if (bm != null) return new Bitmap(bm);
    	else return null;
    }

    /**
     * Decode an immutable bitmap from the specified byte array.
     *
     * @param data byte array of compressed image data
     * @param offset offset into imageData for where the decoder should begin
     *               parsing.
     * @param length the number of bytes, beginning at offset, to parse
     * @return The decoded bitmap, or null if the image could not be decoded.
     */
    public static Bitmap decodeByteArray(byte[] data, int offset, int length) {
        return decodeByteArray(data, offset, length, null);
    }

    /**
     * Decode an input stream into a bitmap. If the input stream is null, or
     * cannot be used to decode a bitmap, the function returns null.
     * The stream's position will be where ever it was after the encoded data
     * was read.
     *
     * @param is The input stream that holds the raw data to be decoded into a
     *           bitmap.
     * @param outPadding If not null, return the padding rect for the bitmap if
     *                   it exists, otherwise set padding to [-1,-1,-1,-1]. If
     *                   no bitmap is returned (null) then padding is
     *                   unchanged.
     * @param opts null-ok; Options that control downsampling and whether the
     *             image should be completely decoded, or just is size returned.
     * @return The decoded bitmap, or null if the image data could not be
     *         decoded, or, if opts is non-null, if opts requested only the
     *         size be returned (in opts.outWidth and opts.outHeight)
     *
     * <p class="note">Prior to {@link android.os.Build.VERSION_CODES#KITKAT},
     * if {@link InputStream#markSupported is.markSupported()} returns true,
     * <code>is.mark(1024)</code> would be called. As of
     * {@link android.os.Build.VERSION_CODES#KITKAT}, this is no longer the case.</p>
     */
    public static Bitmap decodeStream(InputStream is, Rect pad, Options opts) {
    	android.graphics.Rect apad = sThreadLocalRect.get();
    	android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeStream(is, apad, opts);
    	if (pad != null) {
    		pad.set(apad.left, apad.top, apad.right, apad.bottom);
    	}
    	if (bm != null) return new Bitmap(bm);
    	else return null;
    }

    /**
     * Decode an input stream into a bitmap. If the input stream is null, or
     * cannot be used to decode a bitmap, the function returns null.
     * The stream's position will be where ever it was after the encoded data
     * was read.
     *
     * @param is The input stream that holds the raw data to be decoded into a
     *           bitmap.
     * @return The decoded bitmap, or null if the image data could not be decoded.
     */
    public static Bitmap decodeStream(InputStream is) {
        return decodeStream(is, null, null);
    }

    /**
     * Decode a bitmap from the file descriptor. If the bitmap cannot be decoded
     * return null. The position within the descriptor will not be changed when
     * this returns, so the descriptor can be used again as-is.
     *
     * @param fd The file descriptor containing the bitmap data to decode
     * @param outPadding If not null, return the padding rect for the bitmap if
     *                   it exists, otherwise set padding to [-1,-1,-1,-1]. If
     *                   no bitmap is returned (null) then padding is
     *                   unchanged.
     * @param opts null-ok; Options that control downsampling and whether the
     *             image should be completely decoded, or just its size returned.
     * @return the decoded bitmap, or null
     */
    public static Bitmap decodeFileDescriptor(FileDescriptor fd, Rect pad, Options opts) {
    	android.graphics.Rect apad = sThreadLocalRect.get();
    	android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeFileDescriptor(fd, apad, opts);
    	if (pad != null) {
    		pad.set(apad.left, apad.top, apad.right, apad.bottom);
    	}
    	if (bm != null) return new Bitmap(bm);
    	else return null;
    }

    /**
     * Decode a bitmap from the file descriptor. If the bitmap cannot be decoded
     * return null. The position within the descriptor will not be changed when
     * this returns, so the descriptor can be used again as is.
     *
     * @param fd The file descriptor containing the bitmap data to decode
     * @return the decoded bitmap, or null
     */
    public static Bitmap decodeFileDescriptor(FileDescriptor fd) {
        return decodeFileDescriptor(fd, null, null);
    }
}
