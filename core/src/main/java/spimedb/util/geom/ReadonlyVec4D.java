/*
 *   __               .__       .__  ._____.           
 * _/  |_  _______  __|__| ____ |  | |__\_ |__   ______
 * \   __\/  _ \  \/  /  |/ ___\|  | |  || __ \ /  ___/
 *  |  | (  <_> >    <|  \  \___|  |_|  || \_\ \\___ \ 
 *  |__|  \____/__/\_ \__|\___  >____/__||___  /____  >
 *                   \/       \/             \/     \/ 
 *
 * Copyright (c) 2006-2011 Karsten Schmidt
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * http://creativecommons.org/licenses/LGPL/2.1/
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 */

package spimedb.util.geom;

import spimedb.util.math.InterpolateStrategy;
import spimedb.util.math.MathUtils;
import spimedb.util.math.ScaleMap;

/**
 * Readonly, immutable interface wrapper for Vec3D instances. Used throughout
 * the library for safety purposes.
 */
public interface ReadonlyVec4D {

    /**
     * Add vector v and returns result as new vector.
     * 
     * @param v
     *            vector to add
     * 
     * @return result as new vector
     */
    Vec4D add(ReadonlyVec4D v);

    Vec4D addScaled(ReadonlyVec4D t, float s);

    /**
     * Adds vector {a,b,c} and returns result as new vector.
     * 
     * @param a
     *            X coordinate
     * @param b
     *            Y coordinate
     * @param c
     *            Z coordinate
     * 
     * @return result as new vector
     */
    Vec4D addXYZ(float a, float b, float c);

    /**
     * Returns the (4-space) angle in radians between this vector and the vector
     * parameter; the return value is constrained to the range [0,PI].
     * 
     * @param v
     *            the other vector
     * @return the angle in radians in the range [0,PI]
     */
    float angleBetween(ReadonlyVec4D v);

    /**
     * Compares the length of the vector with another one.
     * 
     * @param v
     *            vector to compare with
     * 
     * @return -1 if other vector is longer, 0 if both are equal or else +1
     */
    int compareTo(ReadonlyVec4D v);

    /**
     * Copy.
     * 
     * @return a new independent instance/copy of a given vector
     */
    Vec4D copy();

    /**
     * Calculates distance to another vector.
     * 
     * @param v
     *            non-null vector
     * 
     * @return distance or Float.NaN if v=null
     */
    float distanceTo(ReadonlyVec4D v);

    /**
     * Calculates the squared distance to another vector.
     * 
     * @param v
     *            non-null vector
     * 
     * @return distance or NaN if v=null
     * 
     * @see #magSquared()
     */
    float distanceToSquared(ReadonlyVec4D v);

    /**
     * Computes the scalar product (dot product) with the given vector.
     * 
     * @param v
     *            the v
     * 
     * @return dot product
     * 
     * @see <a href="http://en.wikipedia.org/wiki/Dot_product">Wikipedia
     *      entry</a>
     */
    float dot(ReadonlyVec4D v);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    boolean equals(Object obj);

    /**
     * Compares this vector with the one given. The vectors are deemed equal if
     * the individual differences of all component values are within the given
     * tolerance.
     * 
     * @param v
     *            the v
     * @param tolerance
     *            the tolerance
     * 
     * @return true, if equal
     */
    boolean equalsWithTolerance(ReadonlyVec4D v, float tolerance);

    /**
     * Gets the abs.
     * 
     * @return the abs
     */
    Vec4D getAbs();

    /**
     * Scales vector uniformly by factor -1 ( v = -v ).
     * 
     * @return result as new vector
     */
    Vec4D getInvertedXYZ();

    /**
     * Produces a new vector with all of its coordinates passed through the
     * given {@link ScaleMap}. This version also maps the w coordinate. For
     * mapping only XYZ use the {@link #getMappedXYZ(ScaleMap)} version.
     * 
     * @param map
     * @return mapped vector
     */
    Vec4D getMapped(ScaleMap map);

    /**
     * Produces a new vector with only its XYZ coordinates passed through the
     * given {@link ScaleMap}. This version keeps the original w coordinate. For
     * mapping all XYZW, use the {@link #getMapped(ScaleMap)} version.
     * 
     * @param map
     * @return mapped vector
     */
    Vec4D getMappedXYZ(ScaleMap map);

    /**
     * Produces the normalized version as a new vector.
     * 
     * @return new vector
     */
    Vec4D getNormalized();

    /**
     * Produces a new vector normalized to the given length.
     * 
     * @param len
     *            new desired length
     * 
     * @return new vector
     */
    Vec4D getNormalizedTo(float len);

    /**
     * Gets the rotated around axis.
     * 
     * @param axis
     *            the axis
     * @param theta
     *            the theta
     * 
     * @return new result vector
     */
    Vec4D getRotatedAroundAxis(roVec3D axis, float theta);

    /**
     * Creates a new vector rotated by the given angle around the X axis.
     * 
     * @param theta
     *            the theta
     * 
     * @return rotated vector
     */
    Vec4D getRotatedX(float theta);

    /**
     * Creates a new vector rotated by the given angle around the Y axis.
     * 
     * @param theta
     *            the theta
     * 
     * @return rotated vector
     */
    Vec4D getRotatedY(float theta);

    /**
     * Creates a new vector rotated by the given angle around the Z axis.
     * 
     * @param theta
     *            the theta
     * 
     * @return rotated vector
     */
    Vec4D getRotatedZ(float theta);

    /**
     * Creates a new vector with its XYZ coordinates rounded to the given
     * precision (grid alignment). The weight component remains unmodified.
     * 
     * @param prec
     * @return grid aligned vector
     */
    Vec4D getRoundedXYZTo(float prec);

    Vec4D getUnweighted();

    Vec4D getWeighted();

    /**
     * Interpolates the vector towards the given target vector, using linear
     * interpolation.
     * 
     * @param v
     *            target vector
     * @param f
     *            interpolation factor (should be in the range 0..1)
     * 
     * @return result as new vector
     */
    Vec4D interpolateTo(ReadonlyVec4D v, float f);

    /**
     * Interpolates the vector towards the given target vector, using the given
     * {@link InterpolateStrategy}.
     * 
     * @param v
     *            target vector
     * @param f
     *            interpolation factor (should be in the range 0..1)
     * @param s
     *            InterpolateStrategy instance
     * 
     * @return result as new vector
     */
    Vec4D interpolateTo(ReadonlyVec4D v, float f, InterpolateStrategy s);

    /**
     * Checks if vector has a magnitude equals or close to zero (tolerance used
     * is {@link MathUtils#EPS}).
     * 
     * @return true, if zero vector
     */
    boolean isZeroVector();

    /**
     * Calculates the magnitude/eucledian length of the vector.
     * 
     * @return vector length
     */
    float magnitude();

    /**
     * Calculates only the squared magnitude/length of the vector. Useful for
     * inverse square law applications and/or for speed reasons or if the real
     * eucledian distance is not required (e.g. sorting).
     * 
     * @return squared magnitude (x^2 + y^2 + z^2)
     */
    float magSquared();

    /**
     * Scales vector uniformly and returns result as new vector.
     * 
     * @param s
     *            scale factor
     * 
     * @return new vector
     */
    Vec4D scale(float s);

    /**
     * Scales vector non-uniformly and returns result as new vector.
     * 
     * @param x
     *            scale factor for X coordinate
     * @param y
     *            scale factor for Y coordinate
     * @param z
     *            scale factor for Z coordinate
     * 
     * @return new vector
     */
    Vec4D scale(float x, float y, float z, float w);

    /**
     * Scales vector non-uniformly by vector v and returns result as new vector.
     * 
     * @param s
     *            scale vector
     * 
     * @return new vector
     */
    Vec4D scale(ReadonlyVec4D s);

    /**
     * Subtracts vector v and returns result as new vector.
     * 
     * @param v
     *            vector to be subtracted
     * 
     * @return result as new vector
     */
    Vec4D sub(ReadonlyVec4D v);

    /**
     * Subtracts vector {a,b,c} and returns result as new vector.
     * 
     * @param a
     *            X coordinate
     * @param b
     *            Y coordinate
     * @param c
     *            Z coordinate
     * 
     * @return result as new vector
     */
    Vec4D subXYZ(float a, float b, float c);

    float[] toArray();

    Vec3D unweightInto(Vec3D out);

    float w();

    XYZ weightInto(Vec3D out);

    float x();

    float y();

    float z();
}