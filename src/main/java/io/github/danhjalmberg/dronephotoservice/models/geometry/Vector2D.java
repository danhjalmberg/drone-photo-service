package io.github.danhjalmberg.dronephotoservice.models.geometry;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Objects;

/**
 * Represents an immutable two-dimensional position or mathematical vector.
 *
 * <p>Components are stored as double-precision values so calculations retain
 * fractional precision. Simulation-domain positions are normally expressed in
 * world meters; conversion to integer pixel coordinates should occur only at
 * rendering or image-processing boundaries.</p>
 *
 * <p>All vector operations return new values and do not modify this
 * instance.</p>
 */
public final class Vector2D {

    /**
     * Shared vector whose x- and y-components are both zero.
     */
    public static final Vector2D ZERO = new Vector2D(0.0, 0.0);

    private final double x;
    private final double y;

    /**
     * Creates a vector with the specified components.
     *
     * @param x x-component
     * @param y y-component
     */
    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Creates a vector with the coordinates of an AWT integer point.
     *
     * @param point source point
     * @return vector with the same coordinates
     * @throws NullPointerException if {@code point} is {@code null}
     */
    public static Vector2D fromPoint(Point point) {
        Objects.requireNonNull(point, "point must not be null");
        return new Vector2D(point.x, point.y);
    }

    /**
     * Creates a vector with the coordinates of an AWT point.
     *
     * @param point source point
     * @return vector with the same coordinates
     * @throws NullPointerException if {@code point} is {@code null}
     */
    public static Vector2D fromPoint2D(Point2D point) {
        Objects.requireNonNull(point, "point must not be null");
        return new Vector2D(point.getX(), point.getY());
    }

    /**
     * Returns the x-component.
     *
     * @return x-component
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the y-component.
     *
     * @return y-component
     */
    public double getY() {
        return y;
    }

    /**
     * Returns the component-wise sum of this vector and another vector.
     *
     * @param other vector to add
     * @return component-wise sum
     * @throws NullPointerException if {@code other} is {@code null}
     */
    public Vector2D add(Vector2D other) {
        Objects.requireNonNull(other, "other must not be null");
        return new Vector2D(x + other.x, y + other.y);
    }

    /**
     * Returns the component-wise difference between this vector and another
     * vector.
     *
     * @param other vector to subtract from this vector
     * @return component-wise difference
     * @throws NullPointerException if {@code other} is {@code null}
     */
    public Vector2D subtract(Vector2D other) {
        Objects.requireNonNull(other, "other must not be null");
        return new Vector2D(x - other.x, y - other.y);
    }

    /**
     * Returns this vector multiplied by a scalar.
     *
     * @param scalar scalar multiplier
     * @return scaled vector
     */
    public Vector2D multiply(double scalar) {
        return new Vector2D(x * scalar, y * scalar);
    }

    /**
     * Returns this vector divided by a scalar.
     *
     * @param scalar scalar divisor
     * @return divided vector
     * @throws IllegalArgumentException if {@code scalar} is zero
     */
    public Vector2D divide(double scalar) {
        if (scalar == 0.0) {
            throw new IllegalArgumentException("Cannot divide vector by zero.");
        }
        return new Vector2D(x / scalar, y / scalar);
    }

    /**
     * Returns the additive inverse of this vector.
     *
     * @return vector with both components negated
     */
    public Vector2D negate() {
        return new Vector2D(-x, -y);
    }

    /**
     * Returns the dot product of this vector and another vector.
     *
     * @param other other vector
     * @return dot product
     * @throws NullPointerException if {@code other} is {@code null}
     */
    public double dot(Vector2D other) {
        Objects.requireNonNull(other, "other must not be null");
        return x * other.x + y * other.y;
    }

    /**
     * Returns the squared magnitude of this vector.
     *
     * <p>This value can be used to compare vector lengths without calculating a
     * square root.</p>
     *
     * @return squared magnitude
     */
    public double magnitudeSquared() {
        return dot(this);
    }

    /**
     * Returns the magnitude, or Euclidean length, of this vector.
     *
     * @return magnitude of this vector
     */
    public double magnitude() {
        return Math.sqrt(magnitudeSquared());
    }

    /**
     * Returns the Euclidean distance from this position to another position.
     *
     * @param other other position
     * @return distance between the positions
     * @throws NullPointerException if {@code other} is {@code null}
     */
    public double distanceTo(Vector2D other) {
        Objects.requireNonNull(other, "other must not be null");
        return subtract(other).magnitude();
    }

    /**
     * Returns the squared Euclidean distance from this position to another
     * position.
     *
     * <p>This value can be used to compare distances without calculating a square
     * root.</p>
     *
     * @param other other position
     * @return squared distance between the positions
     * @throws NullPointerException if {@code other} is {@code null}
     */
    public double distanceSquaredTo(Vector2D other) {
        Objects.requireNonNull(other, "other must not be null");
        return subtract(other).magnitudeSquared();
    }

    /**
     * Reports whether both components are exactly zero.
     *
     * @return {@code true} if both components are exactly zero; otherwise
     *         {@code false}
     */
    public boolean isZero() {
        return x == 0.0 && y == 0.0;
    }

    /**
     * Reports whether this vector's magnitude is within the specified tolerance
     * of zero.
     *
     * @param epsilon non-negative tolerance
     * @return {@code true} if the magnitude is less than or equal to
     *         {@code epsilon}; otherwise {@code false}
     * @throws IllegalArgumentException if {@code epsilon} is negative
     */
    public boolean isNearZero(double epsilon) {
        if (epsilon < 0.0) {
            throw new IllegalArgumentException("epsilon must not be negative.");
        }
        return magnitudeSquared() <= epsilon * epsilon;
    }

    /**
     * Returns a unit vector in the same direction as this vector.
     *
     * <p>If this is the zero vector, {@link #ZERO} is returned.</p>
     *
     * @return normalized vector, or {@link #ZERO} for the zero vector
     */
    public Vector2D normalized() {
        double magnitude = magnitude();
        if (magnitude == 0.0) {
            return ZERO;
        }
        return divide(magnitude);
    }

    /**
     * Returns the displacement vector from this position to a target position.
     *
     * @param target target position
     * @return vector equivalent to {@code target - this}
     * @throws NullPointerException if {@code target} is {@code null}
     */
    public Vector2D directionTo(Vector2D target) {
        Objects.requireNonNull(target, "target must not be null");
        return target.subtract(this);
    }

    /**
     * Returns a unit vector pointing from this position toward a target position.
     *
     * <p>If the positions are equal, {@link #ZERO} is returned.</p>
     *
     * @param target target position
     * @return normalized direction to {@code target}, or {@link #ZERO} if the
     *         positions are equal
     * @throws NullPointerException if {@code target} is {@code null}
     */
    public Vector2D normalizedDirectionTo(Vector2D target) {
        return directionTo(target).normalized();
    }

    /**
     * Returns a position moved toward a target by at most a specified distance.
     *
     * <p>If the target is no farther away than {@code maxDistance}, the target
     * position is returned without overshooting it.</p>
     *
     * @param target target position
     * @param maxDistance maximum non-negative movement distance
     * @return moved position, or {@code target} if it is within range
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws IllegalArgumentException if {@code maxDistance} is negative
     */
    public Vector2D moveToward(Vector2D target, double maxDistance) {

        Objects.requireNonNull(target, "target must not be null");

        if (maxDistance < 0.0) {
            throw new IllegalArgumentException("maxDistance must not be negative.");
        }

        Vector2D offset = directionTo(target);
        double distance = offset.magnitude();

        if (distance == 0.0 || distance <= maxDistance) {
            return target;
        }

        return add(offset.divide(distance).multiply(maxDistance));
    }

    /**
     * Returns the linear interpolation from this vector to another vector.
     *
     * <p>A factor of {@code 0.0} returns a value equal to this vector, and a
     * factor of {@code 1.0} returns a value equal to {@code other}. Values outside
     * the range {@code [0.0, 1.0]} perform linear extrapolation.</p>
     *
     * @param other target vector
     * @param t interpolation factor
     * @return interpolated or extrapolated vector
     * @throws NullPointerException if {@code other} is {@code null}
     */
    public Vector2D lerp(Vector2D other, double t) {
        Objects.requireNonNull(other, "other must not be null");
        return new Vector2D(
                x + (other.x - x) * t,
                y + (other.y - y) * t);
    }

    /**
     * Returns this vector rotated 90 degrees counterclockwise.
     *
     * @return counterclockwise perpendicular vector
     */
    public Vector2D perpendicularLeft() {
        return new Vector2D(-y, x);
    }

    /**
     * Returns this vector rotated 90 degrees clockwise.
     *
     * @return clockwise perpendicular vector
     */
    public Vector2D perpendicularRight() {
        return new Vector2D(y, -x);
    }

    /**
     * Returns an AWT point created by rounding both components to the nearest
     * integers.
     *
     * @return point containing the rounded components
     */
    public Point toPoint() {
        return new Point(toIntX(), toIntY());
    }

    /**
     * Returns an AWT double-precision point with the same coordinates.
     *
     * @return point with the same coordinates
     */
    public Point2D.Double toPoint2D() {
        return new Point2D.Double(x, y);
    }

    /**
     * Returns the x-component rounded to the nearest integer.
     *
     * @return rounded x-component
     */
    public int toIntX() {
        return (int) Math.round(x);
    }

    /**
     * Returns the y-component rounded to the nearest integer.
     *
     * @return rounded y-component
     */
    public int toIntY() {
        return (int) Math.round(y);
    }

    /**
     * Returns the floor of the x-component as an integer.
     *
     * @return floor of the x-component
     */
    public int floorX() {
        return (int) Math.floor(x);
    }

    /**
     * Returns the floor of the y-component as an integer.
     *
     * @return floor of the y-component
     */
    public int floorY() {
        return (int) Math.floor(y);
    }

    /**
     * Returns the ceiling of the x-component as an integer.
     *
     * @return ceiling of the x-component
     */
    public int ceilX() {
        return (int) Math.ceil(x);
    }

    /**
     * Returns the ceiling of the y-component as an integer.
     *
     * @return ceiling of the y-component
     */
    public int ceilY() {
        return (int) Math.ceil(y);
    }

    /**
     * Returns the components formatted as {@code (x, y)}.
     *
     * @return string representation of this vector
     */
    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    /**
     * Reports whether another object is a vector whose components compare as
     * equal using {@link Double#compare(double, double)}.
     *
     * @param object object to compare with this vector
     * @return {@code true} if {@code object} is a vector with equal components;
     *         otherwise {@code false}
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Vector2D)) {
            return false;
        }
        Vector2D vector2D = (Vector2D) object;

        return Double.compare(vector2D.x, x) == 0 && Double.compare(vector2D.y, y) == 0;
    }

    /**
     * Returns a hash code consistent with component-based equality.
     *
     * @return hash code for this vector
     */
    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
