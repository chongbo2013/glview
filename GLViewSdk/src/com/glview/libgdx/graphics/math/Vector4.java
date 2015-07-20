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

package com.glview.libgdx.graphics.math;

import java.io.Serializable;

/** Encapsulates a 3D vector. Allows chaining operations by returning a reference to itself in all modification methods.
 * @author badlogicgames@gmail.com */
public class Vector4 implements Serializable, Vector<Vector4> {
	private static final long serialVersionUID = 3840054589595372522L;

	/** the x-component of this vector **/
	public float x;
	/** the y-component of this vector **/
	public float y;
	/** the z-component of this vector **/
	public float z;
	
	public float w;

	/** @deprecated
	 * Static temporary vector. Use with care! Use only when sure other code will not also use this.
	 * @see #tmp() **/
	public final static Vector4 tmp = new Vector4();
	/** @deprecated
	 * Static temporary vector. Use with care! Use only when sure other code will not also use this.
	 * @see #tmp() **/
	public final static Vector4 tmp2 = new Vector4();
	/** @deprecated
	 * Static temporary vector. Use with care! Use only when sure other code will not also use this.
	 * @see #tmp() **/
	public final static Vector4 tmp3 = new Vector4();

	public final static Vector4 X    = new Vector4(1, 0, 0, 0);
	public final static Vector4 Y    = new Vector4(0, 1, 0, 0);
	public final static Vector4 Z    = new Vector4(0, 0, 1, 0);
	public final static Vector4 W    = new Vector4(0, 0, 0, 1);
	public final static Vector4 Zero = new Vector4(0, 0, 0, 0);
	
	private final static Matrix4 tmpMat = new Matrix4();

	/** Constructs a vector at (0,0,0) */
	public Vector4 () {
	}

	/** Creates a vector with the given components
	 * @param x The x-component
	 * @param y The y-component
	 * @param z The z-component */
	public Vector4 (float x, float y, float z, float w) {
		this.set(x, y, z, w);
	}

	/** Creates a vector from the given vector
	 * @param vector The vector */
	public Vector4 (final Vector4 vector) {
		this.set(vector);
	}

	/** Creates a vector from the given array. The array must have at least 3 elements.
	 * 
	 * @param values The array */
	public Vector4 (final float[] values) {
		this.set(values[0], values[1], values[2], values[3]);
	}

	/** Creates a vector from the given vector and z-component
	 *
	 * @param vector The vector
	 * @param z The z-component */
	public Vector4 (final Vector3 vector, float w) {
		this.set(vector.x, vector.y, vector.z, w);
	}

	/** Sets the vector to the given components
	 * 
	 * @param x The x-component
	 * @param y The y-component
	 * @param z The z-component
	 * @return this vector for chaining */
	public Vector4 set (float x, float y, float z, float w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
		return this;
	}

	/** Sets the components of the given vector
	 * 
	 * @param vector The vector
	 * @return This vector for chaining */
	@Override
	public Vector4 set (final Vector4 vector) {
		return this.set(vector.x, vector.y, vector.z, vector.w);
	}

	/** Sets the components from the array. The array must have at least 3 elements
	 * 
	 * @param values The array
	 * @return this vector for chaining */
	public Vector4 set (final float[] values) {
		return this.set(values[0], values[1], values[2], values[3]);
	}

	/** Sets the components of the given vector and z-component
	 *
	 * @param vector The vector
	 * @param z The z-component
	 * @return This vector for chaining */
	public Vector4 set (final Vector3 vector, float w) {
		return this.set(vector.x, vector.y, vector.z, w);
	}

	/** @return a copy of this vector */
	@Override
	public Vector4 cpy () {
		return new Vector4(this);
	}

	/** @deprecated
	 * NEVER EVER SAVE THIS REFERENCE! Do not use this unless you are aware of the side-effects, e.g. other methods might call this
	 * as well.
	 * 
	 * @return a temporary copy of this vector */
	
	public Vector4 tmp () {
		return tmp.set(this);
	}

	/** @deprecated
	 * NEVER EVER SAVE THIS REFERENCE! Do not use this unless you are aware of the side-effects, e.g. other methods might call this
	 * as well.
	 * 
	 * @return a temporary copy of this vector */
	public Vector4 tmp2 () {
		return tmp2.set(this);
	}

	/** @deprecated
	 * NEVER EVER SAVE THIS REFERENCE! Do not use this unless you are aware of the side-effects, e.g. other methods might call this
	 * as well.
	 * 
	 * @return a temporary copy of this vector */
	Vector4 tmp3 () {
		return tmp3.set(this);
	}

	/** Adds the given vector to this vector
	 * 
	 * @param vector The other vector
	 * @return This vector for chaining */
	public Vector4 add (final Vector4 vector) {
		return this.add(vector.x, vector.y, vector.z, vector.w);
	}

	/** Adds the given vector to this component
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return This vector for chaining. */
	public Vector4 add (float x, float y, float z, float w) {
		return this.set(this.x + x, this.y + y, this.z + z, this.w + w);
	}

	/** Adds the given value to all three components of the vector.
	 * 
	 * @param values The value
	 * @return This vector for chaining */
	public Vector4 add (float values) {
		return this.set(this.x + values, this.y + values, this.z + values, this.w + values);
	}

	/** Subtracts the given vector from this vector
	 * @param a_vec The other vector
	 * @return This vector for chaining */
	public Vector4 sub (final Vector4 a_vec) {
		return this.sub(a_vec.x, a_vec.y, a_vec.z, a_vec.w);
	}

	/** Subtracts the other vector from this vector.
	 * 
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return This vector for chaining */
	public Vector4 sub (float x, float y, float z, float w) {
		return this.set(this.x - x, this.y - y, this.z - z, this.w - w);
	}

	/** Subtracts the given value from all components of this vector
	 * 
	 * @param value The value
	 * @return This vector for chaining */
	public Vector4 sub (float value) {
		return this.set(this.x - value, this.y - value, this.z - value, this.w - value);
	}

	/** Scales this vector by the given value
	 * @param value The value
	 * @return This vector for chaining */
	public Vector4 scl (float value) {
		return this.set(this.x * value, this.y * value, this.z * value, this.w * value);
	}
	
	/** @deprecated Use {@link #scl(float)} instead. */
	public Vector4 mul (float value) {
		return scl(value);
	}

	/** Scales this vector by the given vector3's values
	 * @param other The vector3 to multiply by
	 * @return This vector for chaining */
	public Vector4 scl (final Vector4 other) {
		return this.set(x * other.x, y * other.y, z * other.z, w * other.w);
	}
	
	/** @deprecated Use {@link #scl(Vector4)} instead. */
	public Vector4 mul (final Vector4 other) {
		return scl(other);
	}

	/** Scales this vector by the given values
	 * @param vx X value
	 * @param vy Y value
	 * @param vz Z value
	 * @return This vector for chaining */
	public Vector4 scl (float vx, float vy, float vz, float vw) {
		return this.set(this.x * vx, this.y * vy, this.z * vz, this.w * vw);
	}
	
	/** @deprecated Use {@link #scl(float, float, float)} instead. */
	public Vector4 mul (float vx, float vy, float vz, float vw) {
		return scl(vx, vy, vz, vw);
	}

	/** @deprecated Use {@link #scl(float, float, float)} instead. */
	public Vector4 scale (float scalarX, float scalarY, float scalarZ, float scalarW) {
		return scl(scalarX, scalarY, scalarZ, scalarW);
	}
	
	/** @deprecated Use {@link #scl(float)} instead.
	 * Divides all components of this vector by the given value
	 * @param value The value
	 * @return This vector for chaining */
	public Vector4 div (float value) {
		return this.scl(1f/value);
	}

	/** @deprecated Use {@link #scl(float, float, float)} instead.
	 * Divides this vector by the given vector */
	public Vector4 div (float vx, float vy, float vz, float vw) {
		return this.set(x/vx, y/vy, z/vz, w/vw);
	}

	/** @deprecated Use {@link #scl(Vector4)} instead. 
	 * Divides this vector by the given vector */
	public Vector4 div (final Vector4 other) {
		return this.set(x/other.x, y/other.y, z/other.z, w/other.w);
	}
	
	/** @return The euclidian length */
	public static float len (final float x, final float y, final float z, final float w) {
		return (float)Math.sqrt(x * x + y * y + z * z + w * w);
	}

	/** @return The euclidian length */
	public float len () {
		return (float)Math.sqrt(x * x + y * y + z * z + w * w);
	}

	/** @return The squared euclidian length */
	public static float len2 (final float x, final float y, final float z, final float w) {
		return x * x + y * y + z * z + w * w;
	}
	
	/** @return The squared euclidian length */
	public float len2 () {
		return x * x + y * y + z * z + w * w;
	}

	/** @param vector The other vector
	 * @return Wether this and the other vector are equal */
	public boolean idt (final Vector4 vector) {
		return x == vector.x && y == vector.y && z == vector.z && w == vector.w;
	}
	
	/** @return The euclidian distance between the two specified vectors */
	public static float dst (final float x1, final float y1, final float z1, final float w1, 
			final float x2, final float y2, final float z2, final float w2) {
		final float a = x2 - x1;
		final float b = y2 - y1;
		final float c = z2 - z1;
		final float d = w2 - w1;
		return (float)Math.sqrt(a * a + b * b + c * c + d * d); 
	}

	/** @param vector The other vector
	 * @return The euclidian distance between this and the other vector */
	public float dst (final Vector4 vector) {
		final float a = vector.x - x;
		final float b = vector.y - y;
		final float c = vector.z - z;
		final float d = vector.w - w;
		return (float)Math.sqrt(a * a + b * b + c * c + d * d);
	}

	/** @return the distance between this point and the given point */
	public float dst (float x, float y, float z, float w) {
		final float a = x - this.x;
		final float b = y - this.y;
		final float c = z - this.z;
		final float d = w - this.w;
		return (float)Math.sqrt(a * a + b * b + c * c + d * d);
	}
	
	/** @return the squared distance between the given points */
	public static float dst2(final float x1, final float y1, final float z1, final float w1,
			                 final float x2, final float y2, final float z2, final float w2) {
		final float a = x2 - x1;
		final float b = y2 - y1;
		final float c = z2 - z1;
		final float d = w2 - w2;
		return a * a + b * b + c * c + d * d; 
	}
	
	/** Returns the squared distance between this point and the given point
	 * @param point The other point
	 * @return The squared distance */
	public float dst2 (Vector4 point) {
		final float a = point.x - x;
		final float b = point.y - y;
		final float c = point.z - z;
		final float d = point.w - w;
		return a * a + b * b + c * c + d * d;
	}
	
	/** Returns the squared distance between this point and the given point
	 * @param x The x-component of the other point
	 * @param y The y-component of the other point
	 * @param z The z-component of the other point
	 * @return The squared distance */
	public float dst2 (float x, float y, float z, float w) {
		final float a = x - this.x;
		final float b = y - this.y;
		final float c = z - this.z;
		final float d = w - this.w;
		return a * a + b * b + c * c + d * d;
	}

	/** Normalizes this vector to unit length. Does nothing if it is zero.
	 * @return This vector for chaining */
	public Vector4 nor () {
		final float len2 = this.len2();
		if (len2 == 0f || len2 == 1f)
			return this;
		return this.scl(1f/(float)Math.sqrt(len2));
	}
	
	/** @return The dot product between the two vectors */
	public static float dot(float x1, float y1, float z1, float w1, 
			                float x2, float y2, float z2, float w2) {
		return x1 * x2 + y1 * y2 + z1 * z2 + w1 * w2;
	}

	/** @param vector The other vector
	 * @return The dot product between this and the other vector */
	public float dot (final Vector4 vector) {
		return x * vector.x + y * vector.y + z * vector.z + w * vector.w;
	}

	/** Returns the dot product between this and the given vector.
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return The dot product */
	public float dot (float x, float y, float z, float w) {
		return this.x * x + this.y * y + this.z * z + this.w * w;
	}
	
	/** @return Whether this vector is a unit length vector */
	public boolean isUnit () {
		return isUnit(0.000000001f);
	}
	
	/** @return Whether this vector is a unit length vector within the given margin */
	public boolean isUnit(final float margin) {
		return Math.abs(len2() - 1f) < margin * margin;
	}

	/** @return Whether this vector is a zero vector */
	public boolean isZero () {
		return x == 0 && y == 0 && z == 0 && w == 0;
	}
	
	/** @return Whether the length of this vector is smaller than the given margin */
	public boolean isZero (final float margin) {
		return len2() < margin * margin;
	}

	/** Linearly interpolates between this vector and the target vector by alpha which is in the range [0,1]. The result is stored
	 * in this vector.
	 * 
	 * @param target The target vector
	 * @param alpha The interpolation coefficient
	 * @return This vector for chaining. */
	public Vector4 lerp (final Vector4 target, float alpha) {
		scl(1.0f - alpha);
		add(target.x * alpha, target.y * alpha, target.z * alpha, target.w * alpha);
		return this;
	}

	
	public String toString () {
		return x + "," + y + "," + z;
	}
	
	/** Limits this vector's length to given value
	 * @param limit Max length
	 * @return This vector for chaining */
	public Vector4 limit (float limit) {
		if (len2() > limit * limit)
			nor().scl(limit);
		return this;
	}
	
	/** Clamps this vector's length to given value
	 * @param min Min length
	 * @param max Max length
	 * @return This vector for chaining */
	public Vector4 clamp (float min, float max) {
		final float l2 = len2();
		if (l2 == 0f)
			return this;
		if (l2 > max * max)
			return nor().scl(max);
		if (l2 < min * min)
			return nor().scl(min);
		return this;
	}

	@Override
	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + NumberUtils.floatToIntBits(x);
		result = prime * result + NumberUtils.floatToIntBits(y);
		result = prime * result + NumberUtils.floatToIntBits(z);
		result = prime * result + NumberUtils.floatToIntBits(w);
		return result;
	}

	@Override
	public boolean equals (Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Vector4 other = (Vector4)obj;
		if (NumberUtils.floatToIntBits(x) != NumberUtils.floatToIntBits(other.x)) return false;
		if (NumberUtils.floatToIntBits(y) != NumberUtils.floatToIntBits(other.y)) return false;
		if (NumberUtils.floatToIntBits(z) != NumberUtils.floatToIntBits(other.z)) return false;
		if (NumberUtils.floatToIntBits(w) != NumberUtils.floatToIntBits(other.w)) return false;
		return true;
	}

	/** Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
	 * @return whether the vectors are the same. */
	public boolean epsilonEquals (float x, float y, float z, float w, float epsilon) {
		if (Math.abs(x - this.x) > epsilon) return false;
		if (Math.abs(y - this.y) > epsilon) return false;
		if (Math.abs(z - this.z) > epsilon) return false;
		if (Math.abs(w - this.w) > epsilon) return false;
		return true;
	}
	
	public Vector4 zero(){
		return this.Zero;
	}
}
