package io.github.danhjalmberg.dronephotoservice.models.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.Point;
import java.awt.geom.Point2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link Vector2D} class.
 * The tests cover representative normal cases, boundary cases, conversions,
 * validation, and the value semantics of the immutable vector type without
 * attempting exhaustive coverage of every possible numeric input.
 *
 * @author Dan Hjälmberg
 */
class Vector2DTest {

    /**
     * Tolerance used when comparing floating-point results.
     */
    private static final double DELTA = 1.0e-9;

    /**
     * Tests construction of vectors and conversion to and from AWT point types.
     */
    @Nested
    class ConstructionAndConversionTests {

        /**
         * Tests that the constructor stores the supplied x- and y-components.
         */
        @Test
        void constructorStoresComponents() {
            Vector2D vector = new Vector2D(2.5, -4.25);

            assertEquals(2.5, vector.getX(), DELTA);
            assertEquals(-4.25, vector.getY(), DELTA);
        }

        /**
         * Tests that an AWT Point is converted to a vector with equal coordinates.
         */
        @Test
        void fromPointCopiesIntegerCoordinates() {
            Vector2D vector = Vector2D.fromPoint(new Point(4, -7));

            assertVectorEquals(4.0, -7.0, vector);
        }

        /**
         * Tests that a Point2D is converted without losing fractional coordinates.
         */
        @Test
        void fromPoint2DCopiesFractionalCoordinates() {
            Vector2D vector = Vector2D.fromPoint2D(new Point2D.Double(3.75, -8.5));

            assertVectorEquals(3.75, -8.5, vector);
        }

        /**
         * Tests conversion to AWT point types and the intended rounding behavior.
         */
        @Test
        void pointConversionsPreserveOrRoundCoordinatesAsSpecified() {
            Vector2D vector = new Vector2D(2.6, -3.6);

            assertEquals(new Point(3, -4), vector.toPoint());

            Point2D.Double point2D = vector.toPoint2D();
            assertEquals(2.6, point2D.getX(), DELTA);
            assertEquals(-3.6, point2D.getY(), DELTA);
        }

        /**
         * Tests nearest-integer, floor, and ceiling conversions for both components.
         */
        @Test
        void integerConversionMethodsUseExpectedRoundingRules() {
            Vector2D vector = new Vector2D(2.6, -3.4);

            assertEquals(3, vector.toIntX());
            assertEquals(-3, vector.toIntY());
            assertEquals(2, vector.floorX());
            assertEquals(-4, vector.floorY());
            assertEquals(3, vector.ceilX());
            assertEquals(-3, vector.ceilY());
        }
    }

    /**
     * Tests arithmetic operations performed on vectors.
     */
    @Nested
    class ArithmeticTests {

        /**
         * Tests component-wise vector addition.
         */
        @Test
        void addReturnsComponentWiseSum() {
            Vector2D first = new Vector2D(3.0, -2.0);
            Vector2D second = new Vector2D(-1.5, 4.0);

            assertVectorEquals(1.5, 2.0, first.add(second));
        }

        /**
         * Tests component-wise vector subtraction.
         */
        @Test
        void subtractReturnsComponentWiseDifference() {
            Vector2D first = new Vector2D(3.0, -2.0);
            Vector2D second = new Vector2D(-1.5, 4.0);

            assertVectorEquals(4.5, -6.0, first.subtract(second));
        }

        /**
         * Tests scalar division.
         */
        @Test
        void divideReturnsScaledVector() {
            Vector2D vector = new Vector2D(6.0, -9.0);

            assertVectorEquals(3.0, -4.5, vector.divide(2.0));
        }

        /**
         * Tests scalar multiplication.
         */
        @Test
        void multiplyReturnsScaledVector() {
            Vector2D vector = new Vector2D(6.0, -9.0);

            assertVectorEquals(-12.0, 18.0, vector.multiply(-2.0));
        }

        /**
         * Tests vector negation.
         */
        @Test
        void negateReturnsVectorWithNegatedComponents() {
            Vector2D vector = new Vector2D(6.0, -9.0);

            assertVectorEquals(-6.0, 9.0, vector.negate());
        }

        /**
         * Tests calculation of the dot product of two vectors.
         */
        @Test
        void dotReturnsSumOfComponentProducts() {
            Vector2D first = new Vector2D(2.0, 3.0);
            Vector2D second = new Vector2D(4.0, -5.0);

            assertEquals(-7.0, first.dot(second), DELTA);
        }

        /**
         * Tests that vector operations return new values and leave the source unchanged.
         */
        @Test
        void arithmeticOperationsDoNotModifyOriginalVector() {
            Vector2D original = new Vector2D(2.0, 3.0);

            original.add(new Vector2D(4.0, 5.0));
            original.multiply(10.0);
            original.negate();

            assertVectorEquals(2.0, 3.0, original);
        }
    }

    /**
     * Tests vector length, distance, direction, and zero-vector operations.
     */
    @Nested
    class LengthAndDirectionTests {

        /**
         * Tests squared magnitude and magnitude using a three-four-five vector.
         */
        @Test
        void magnitudeMethodsReturnExpectedLength() {
            Vector2D vector = new Vector2D(3.0, 4.0);

            assertEquals(25.0, vector.magnitudeSquared(), DELTA);
            assertEquals(5.0, vector.magnitude(), DELTA);
        }

        /**
         * Tests squared distance and distance between two points.
         */
        @Test
        void distanceMethodsReturnExpectedDistance() {
            Vector2D first = new Vector2D(1.0, 2.0);
            Vector2D second = new Vector2D(4.0, 6.0);

            assertEquals(25.0, first.distanceSquaredTo(second), DELTA);
            assertEquals(5.0, first.distanceTo(second), DELTA);
        }

        /**
         * Tests exact and tolerance-based zero-vector checks.
         */
        @Test
        void zeroChecksDistinguishExactAndNearZeroVectors() {
            assertTrue(Vector2D.ZERO.isZero());
            assertTrue(new Vector2D(0.0003, 0.0004).isNearZero(0.0005));
            assertFalse(new Vector2D(0.0003, 0.0004).isNearZero(0.0004));
            assertFalse(new Vector2D(0.0, 0.0001).isZero());
        }

        /**
         * Tests that normalization returns a unit vector in the same direction.
         */
        @Test
        void normalizedReturnsUnitVectorInSameDirection() {
            Vector2D normalized = new Vector2D(3.0, 4.0).normalized();

            assertVectorEquals(0.6, 0.8, normalized);
            assertEquals(1.0, normalized.magnitude(), DELTA);
        }

        /**
         * Tests that normalization of the zero vector returns the shared zero constant.
         */
        @Test
        void normalizedZeroReturnsSharedZeroVector() {
            assertSame(Vector2D.ZERO, new Vector2D(0.0, 0.0).normalized());
        }

        /**
         * Tests direction and normalized direction from one point to another.
         */
        @Test
        void directionMethodsPointFromSourceToTarget() {
            Vector2D source = new Vector2D(1.0, 1.0);
            Vector2D target = new Vector2D(4.0, 5.0);

            assertVectorEquals(3.0, 4.0, source.directionTo(target));
            assertVectorEquals(0.6, 0.8, source.normalizedDirectionTo(target));
        }
    }

    /**
     * Tests movement, interpolation, and perpendicular-vector operations.
     */
    @Nested
    class MovementAndInterpolationTests {

        /**
         * Tests that movement toward a distant target advances by the maximum distance.
         */
        @Test
        void moveTowardAdvancesWithoutOvershooting() {
            Vector2D result = new Vector2D(0.0, 0.0)
                    .moveToward(new Vector2D(3.0, 4.0), 2.0);

            assertVectorEquals(1.2, 1.6, result);
        }

        /**
         * Tests that movement returns the target when it lies within the maximum distance.
         */
        @Test
        void moveTowardReturnsTargetWhenTargetIsWithinRange() {
            Vector2D target = new Vector2D(3.0, 4.0);

            Vector2D result = new Vector2D(0.0, 0.0).moveToward(target, 5.0);

            assertSame(target, result);
        }

        /**
         * Tests interpolation and extrapolation using representative factors.
         *
         * @param factor interpolation factor.
         * @param expectedX expected x-component.
         * @param expectedY expected y-component.
         */
        @ParameterizedTest
        @CsvSource({
                "0.0, 2.0, 4.0",
                "0.5, 4.0, 8.0",
                "1.0, 6.0, 12.0",
                "1.5, 8.0, 16.0"
        })
        void lerpInterpolatesAndExtrapolatesUsingFactor(
                double factor,
                double expectedX,
                double expectedY) {

            Vector2D start = new Vector2D(2.0, 4.0);
            Vector2D end = new Vector2D(6.0, 12.0);

            assertVectorEquals(
                    expectedX,
                    expectedY,
                    start.lerp(end, factor));
        }

        /**
         * Tests clockwise and counter-clockwise perpendicular rotations.
         */
        @Test
        void perpendicularMethodsRotateVectorNinetyDegrees() {
            Vector2D vector = new Vector2D(2.0, 3.0);

            Vector2D left = vector.perpendicularLeft();
            Vector2D right = vector.perpendicularRight();

            assertVectorEquals(-3.0, 2.0, left);
            assertVectorEquals(3.0, -2.0, right);
            assertEquals(0.0, vector.dot(left), DELTA);
            assertEquals(0.0, vector.dot(right), DELTA);
        }
    }

    /**
     * Tests invalid arguments and documented exceptional behavior.
     */
    @Nested
    class ValidationTests {

        /**
         * Tests that creation from null AWT Point references is rejected.
         */
        @Test
        void fromPointRejectsNull() {
            assertThrows(NullPointerException.class, () -> Vector2D.fromPoint(null));
        }

        /**
         * Tests that creation from null AWT Point2D references is rejected.
         */
        @Test
        void fromPoint2DRejectsNull() {
            assertThrows(NullPointerException.class, () -> Vector2D.fromPoint2D(null));
        }

        /**
         * Tests that addition rejects a null vector.
         */
        @Test
        void addRejectsNullVector() {
            Vector2D vector = new Vector2D(1.0, 2.0);

            assertThrows(
                    NullPointerException.class,
                    () -> vector.add(null));
        }

        /**
         * Tests that subtraction rejects a null vector.
         */
        @Test
        void subtractRejectsNullVector() {
            Vector2D vector = new Vector2D(1.0, 2.0);

            assertThrows(
                    NullPointerException.class,
                    () -> vector.subtract(null));
        }

        /**
         * Tests that dot product calculation rejects a null vector.
         */
        @Test
        void dotRejectsNullVector() {
            Vector2D vector = new Vector2D(1.0, 2.0);

            assertThrows(
                    NullPointerException.class,
                    () -> vector.dot(null));
        }

        /**
         * Tests that distance calculation rejects a null vector.
         */
        @Test
        void distanceToRejectsNullVector() {
            Vector2D vector = new Vector2D(1.0, 2.0);

            assertThrows(
                    NullPointerException.class,
                    () -> vector.distanceTo(null));
        }

        /**
         * Tests that squared distance calculation rejects a null vector.
         */
        @Test
        void distanceSquaredToRejectsNullVector() {
            Vector2D vector = new Vector2D(1.0, 2.0);

            assertThrows(
                    NullPointerException.class,
                    () -> vector.distanceSquaredTo(null));
        }

        /**
         * Tests that direction calculation rejects a null target.
         */
        @Test
        void directionToRejectsNullTarget() {
            Vector2D vector = new Vector2D(1.0, 2.0);

            assertThrows(
                    NullPointerException.class,
                    () -> vector.directionTo(null));
        }

        /**
         * Tests that normalized direction calculation rejects a null target.
         */
        @Test
        void normalizedDirectionToRejectsNullTarget() {
            Vector2D vector = new Vector2D(1.0, 2.0);

            assertThrows(
                    NullPointerException.class,
                    () -> vector.normalizedDirectionTo(null));
        }

        /**
         * Tests that movement rejects a null target.
         */
        @Test
        void moveTowardRejectsNullTarget() {
            Vector2D vector = new Vector2D(1.0, 2.0);

            assertThrows(
                    NullPointerException.class,
                    () -> vector.moveToward(null, 1.0));
        }

        /**
         * Tests that interpolation rejects a null target.
         */
        @Test
        void lerpRejectsNullTarget() {
            Vector2D vector = new Vector2D(1.0, 2.0);

            assertThrows(
                    NullPointerException.class,
                    () -> vector.lerp(null, 0.5));
        }

        /**
         * Tests that division by either representation of zero is rejected.
         *
         * @param scalar zero scalar.
         */
        @ParameterizedTest
        @ValueSource(doubles = {0.0, -0.0})
        void divideRejectsZeroScalar(double scalar) {
            Vector2D vector = new Vector2D(1.0, 2.0);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> vector.divide(scalar));
        }

        @Test
        void isNearZeroRejectsNegativeEpsilon() {
            Vector2D vector = new Vector2D(1.0, 2.0);

            assertThrows(IllegalArgumentException.class, () -> vector.isNearZero(-0.1));
        }

        @Test
        void moveTowardRejectsNegativeMaximumDistance() {
            Vector2D vector = new Vector2D(1.0, 2.0);

            assertThrows(IllegalArgumentException.class, () -> vector.moveToward(Vector2D.ZERO, -0.1));

        }
    }

    /**
     * Tests equality, hash-code, and string value semantics.
     */
    @Nested
    class EqualityTests {

        /**
         * Tests the principal equality contract for equal and unequal vectors.
         */
        @Test
        void equalsRecognizesEqualVectorsAndRejectsDifferentObjects() {
            Vector2D vector = new Vector2D(2.0, -3.0);
            Vector2D equalVector = new Vector2D(2.0, -3.0);

            assertEquals(vector, vector);
            assertEquals(vector, equalVector);
            assertNotEquals(vector, new Vector2D(2.0, -4.0));
            assertNotEquals(vector, null);
            assertNotEquals(vector, new Point(2, -3));
        }

        /**
         * Tests that equal vectors produce equal hash codes.
         */
        @Test
        void equalVectorsHaveEqualHashCodes() {
            Vector2D first = new Vector2D(2.0, -3.0);
            Vector2D second = new Vector2D(2.0, -3.0);

            assertEquals(first.hashCode(), second.hashCode());
        }

        /**
         * Tests the documented string representation of a vector.
         */
        @Test
        void toStringReturnsCoordinatePair() {
            assertEquals("(2.5, -3.75)", new Vector2D(2.5, -3.75).toString());
        }
    }

    // ########################################################################
    // Helper methods
    // ########################################################################

    /**
     * Asserts that a vector contains the expected x- and y-components.
     *
     * @param expectedX expected x-component
     * @param expectedY expected y-component
     * @param actual actual vector to inspect
     */
    private static void assertVectorEquals(
            double expectedX,
            double expectedY,
            Vector2D actual) {

        assertEquals(expectedX, actual.getX(), DELTA);
        assertEquals(expectedY, actual.getY(), DELTA);
    }
}
