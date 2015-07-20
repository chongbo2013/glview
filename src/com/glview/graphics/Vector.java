package com.glview.graphics;


public class Vector extends PointF {

	public Vector() {
	}

	public Vector(float x, float y) {
		super(x, y);
	}

	public Vector(Vector v) {
		super(v);
	}
	
	/** @return a copy of this vector */
	public Vector cpy () {
		return new Vector(this);
	}

	/** @return The euclidian length */
	public float len () {
		return (float)Math.sqrt(x * x + y * y);
	}

	/** @return The squared euclidian length */
	public float len2 () {
		return x * x + y * y;
	}

	/** Sets this vector from the given vector
	 * @param v The vector
	 * @return This vector for chaining */
	public Vector set (Vector v) {
		x = v.x;
		y = v.y;
		return this;
	}

	/** Subtracts the given vector from this vector.
	 * @param v The vector
	 * @return This vector for chaining */
	public Vector sub (Vector v) {
		x -= v.x;
		y -= v.y;
		return this;
	}

	/** Normalizes this vector. Does nothing if it is zero.
	 * @return This vector for chaining */
	public Vector nor () {
		float len = len();
		if (len != 0) {
			x /= len;
			y /= len;
		}
		return this;
	}

	/** Adds the given vector to this vector
	 * @param v The vector
	 * @return This vector for chaining */
	public Vector add (Vector v) {
		x += v.x;
		y += v.y;
		return this;
	}

	/** Adds the given components to this vector
	 * @param x The x-component
	 * @param y The y-component
	 * @return This vector for chaining */
	public Vector add (float x, float y) {
		this.x += x;
		this.y += y;
		return this;
	}

	/** @param v The other vector
	 * @return The dot product between this and the other vector */
	public float dot (Vector v) {
		return x * v.x + y * v.y;
	}

	/** Multiplies this vector by a scalar
	 * @param scalar The scalar
	 * @return This vector for chaining */
	public Vector scl (float scalar) {
		x *= scalar;
		y *= scalar;
		return this;
	}

	/** @deprecated Use {@link #scl(float)} instead. */
	public Vector mul (float scalar) {
		return scl(scalar);
	}

	/** Multiplies this vector by a scalar
	 * @return This vector for chaining */
	public Vector scl (float x, float y) {
		this.x *= x;
		this.y *= y;
		return this;
	}

	/** @deprecated Use {@link #scl(float, float)} instead. */
	public Vector mul (float x, float y) {
		return scl(x, y);
	}

	/** Multiplies this vector by a vector
	 * @return This vector for chaining */
	public Vector scl (Vector v) {
		this.x *= v.x;
		this.y *= v.y;
		return this;
	}

	/** @deprecated Use {@link #scl(Vector)} instead. */
	public Vector mul (Vector v) {
		return scl(v);
	}

	public Vector div (float value) {
		return this.scl(1 / value);
	}

	public Vector div (float vx, float vy) {
		return this.scl(1 / vx, 1 / vy);
	}

	public Vector div (Vector other) {
		return this.scl(1 / other.x, 1 / other.y);
	}

	/** @param v The other vector
	 * @return the distance between this and the other vector */
	public float dst (Vector v) {
		final float x_d = v.x - x;
		final float y_d = v.y - y;
		return (float)Math.sqrt(x_d * x_d + y_d * y_d);
	}

	/** @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @return the distance between this and the other vector */
	public float dst (float x, float y) {
		final float x_d = x - this.x;
		final float y_d = y - this.y;
		return (float)Math.sqrt(x_d * x_d + y_d * y_d);
	}

	/** @param v The other vector
	 * @return the squared distance between this and the other vector */
	public float dst2 (Vector v) {
		final float x_d = v.x - x;
		final float y_d = v.y - y;
		return x_d * x_d + y_d * y_d;
	}

	/** @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @return the squared distance between this and the other vector */
	public float dst2 (float x, float y) {
		final float x_d = x - this.x;
		final float y_d = y - this.y;
		return x_d * x_d + y_d * y_d;
	}

	/** Limits this vector's length to given value
	 * @param limit Max length
	 * @return This vector for chaining */
	public Vector limit (float limit) {
		if (len2() > limit * limit) {
			nor();
			scl(limit);
		}
		return this;
	}

	/** Clamps this vector's length to given value
	 * @param min Min length
	 * @param max Max length
	 * @return This vector for chaining */
	public Vector clamp (float min, float max) {
		final float l2 = len2();
		if (l2 == 0f) return this;
		if (l2 > max * max) return nor().scl(max);
		if (l2 < min * min) return nor().scl(min);
		return this;
	}

	public String toString () {
		return "[" + x + ":" + y + "]";
	}

	/** Substracts the other vector from this vector.
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @return This vector for chaining */
	public Vector sub (float x, float y) {
		this.x -= x;
		this.y -= y;
		return this;
	}

	/** Calculates the 2D cross product between this and the given vector.
	 * @param v the other vector
	 * @return the cross product */
	public float crs (Vector v) {
		return this.x * v.y - this.y * v.x;
	}

	/** Calculates the 2D cross product between this and the given vector.
	 * @param x the x-coordinate of the other vector
	 * @param y the y-coordinate of the other vector
	 * @return the cross product */
	public float crs (float x, float y) {
		return this.x * y - this.y * x;
	}

	/** @return the angle in degrees of this vector (point) relative to the x-axis. Angles are towards the positive y-axis (typically
	 *         counter-clockwise) and between 0 and 360. */
	public float angle () {
		float angle = (float) (Math.atan2(y, x) * (180f / Math.PI));
		if (angle < 0) angle += 360;
		return angle;
	}

	/** Sets the angle of the vector in degrees relative to the x-axis, towards the positive y-axis (typically counter-clockwise).
	 * @param degrees The angle to set. */
	public Vector setAngle (float degrees) {
		this.set(len(), 0f);
		this.rotate(degrees);

		return this;
	}

	/** Rotates the Vector by the given angle, counter-clockwise assuming the y-axis points up.
	 * @param degrees the angle in degrees */
	public Vector rotate (float degrees) {
		float rad = (float) (degrees * (Math.PI / 180));
		float cos = (float)Math.cos(rad);
		float sin = (float)Math.sin(rad);

		float newX = this.x * cos - this.y * sin;
		float newY = this.x * sin + this.y * cos;

		this.x = newX;
		this.y = newY;

		return this;
	}

	/** Rotates the Vector by 90 degrees in the specified direction, where >= 0 is counter-clockwise and < 0 is clockwise. */
	public Vector rotateCCW () {
		float x = this.x;
	    this.x = this.y;
	    this.y = -x;
		return this;
	}
	
	/** Rotates the Vector by 90 degrees in the specified direction, where >= 0 is counter-clockwise and < 0 is clockwise. */
	public Vector rotateCW () {
	    float x = this.x;
	    this.x = -this.y;
	    this.y = x;
		return this;
	}

	/** Linearly interpolates between this vector and the target vector by alpha which is in the range [0,1]. The result is stored
	 * in this vector.
	 * 
	 * @param target The target vector
	 * @param alpha The interpolation coefficient
	 * @return This vector for chaining. */
	public Vector lerp (Vector target, float alpha) {
		final float invAlpha = 1.0f - alpha;
		this.x = (x * invAlpha) + (target.x * alpha);
		this.y = (y * invAlpha) + (target.y * alpha);
		return this;
	}

	/** Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
	 * @param obj
	 * @param epsilon
	 * @return whether the vectors are the same. */
	public boolean epsilonEquals (Vector obj, float epsilon) {
		if (obj == null) return false;
		if (Math.abs(obj.x - x) > epsilon) return false;
		if (Math.abs(obj.y - y) > epsilon) return false;
		return true;
	}

	/** Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
	 * @param x
	 * @param y
	 * @param epsilon
	 * @return whether the vectors are the same. */
	public boolean epsilonEquals (float x, float y, float epsilon) {
		if (Math.abs(x - this.x) > epsilon) return false;
		if (Math.abs(y - this.y) > epsilon) return false;
		return true;
	}

}
