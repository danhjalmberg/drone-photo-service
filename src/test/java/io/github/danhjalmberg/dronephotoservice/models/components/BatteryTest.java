package io.github.danhjalmberg.dronephotoservice.models.components;

import io.github.danhjalmberg.dronephotoservice.settings.ModelSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link Battery} implementations.
 * The tests cover representative construction, charging, consumption,
 * capacity clamping, and validation behavior for low-, medium-, and
 * high-capacity batteries without duplicating the same tests for each class.
 *
 * @author Dan Hjälmberg
 */
class BatteryTest {

    /**
     * Tolerance used when comparing floating-point results.
     */
    private static final double DELTA = 1.0e-9;

    /**
     * Tests that each battery implementation starts fully charged with the
     * expected type and configured capacity.
     *
     * @param battery battery implementation to test.
     * @param expectedType expected battery type.
     * @param expectedCapacity expected capacity in seconds.
     */
    @ParameterizedTest
    @MethodSource("batteryImplementations")
    void constructorCreatesFullyChargedBattery(
            Battery battery,
            String expectedType,
            double expectedCapacity) {

        assertEquals(expectedType, battery.getType());
        assertEquals(expectedCapacity, battery.getCapacitySeconds(), DELTA);
        assertEquals(expectedCapacity, battery.getCurrentChargeSeconds(), DELTA);
    }

    /**
     * Tests that consuming charge reduces the current charge by the supplied amount.
     *
     * @param battery battery implementation to test.
     * @param expectedType expected battery type.
     * @param expectedCapacity expected capacity in seconds.
     */
    @ParameterizedTest
    @MethodSource("batteryImplementations")
    void consumeReducesCurrentCharge(
            Battery battery,
            String expectedType,
            double expectedCapacity) {

        battery.consume(30.0);

        assertEquals(expectedCapacity - 30.0, battery.getCurrentChargeSeconds(), DELTA);
    }

    /**
     * Tests that consuming more than the remaining charge clamps the battery at zero.
     *
     * @param battery battery implementation to test.
     * @param expectedType expected battery type.
     * @param expectedCapacity expected capacity in seconds.
     */
    @ParameterizedTest
    @MethodSource("batteryImplementations")
    void consumeDoesNotReduceChargeBelowZero(
            Battery battery,
            String expectedType,
            double expectedCapacity) {

        battery.consume(expectedCapacity + 100.0);

        assertEquals(0.0, battery.getCurrentChargeSeconds(), DELTA);
    }

    /**
     * Tests that charging increases the current charge after consumption.
     *
     * @param battery battery implementation to test.
     * @param expectedType expected battery type.
     * @param expectedCapacity expected capacity in seconds.
     */
    @ParameterizedTest
    @MethodSource("batteryImplementations")
    void chargeIncreasesCurrentCharge(
            Battery battery,
            String expectedType,
            double expectedCapacity) {

        battery.consume(60.0);
        battery.charge(20.0);

        assertEquals(expectedCapacity - 40.0, battery.getCurrentChargeSeconds(), DELTA);
    }

    /**
     * Tests that charging beyond capacity clamps the battery at its maximum capacity.
     *
     * @param battery battery implementation to test.
     * @param expectedType expected battery type.
     * @param expectedCapacity expected capacity in seconds.
     */
    @ParameterizedTest
    @MethodSource("batteryImplementations")
    void chargeDoesNotExceedCapacity(
            Battery battery,
            String expectedType,
            double expectedCapacity) {

        battery.consume(10.0);
        battery.charge(100.0);

        assertEquals(expectedCapacity, battery.getCurrentChargeSeconds(), DELTA);
    }

    /**
     * Tests that charging rejects a negative duration.
     *
     * @param battery battery implementation to test.
     * @param expectedType expected battery type.
     * @param expectedCapacity expected battery capacity.
     */
    @ParameterizedTest
    @MethodSource("batteryImplementations")
    void chargeRejectsNegativeDuration(
            Battery battery,
            String expectedType,
            double expectedCapacity) {

        assertThrows(
                IllegalArgumentException.class,
                () -> battery.charge(-0.1));
    }

    /**
     * Tests that consumption rejects a negative duration.
     *
     * @param battery battery implementation to test.
     * @param expectedType expected battery type.
     * @param expectedCapacity expected battery capacity.
     */
    @ParameterizedTest
    @MethodSource("batteryImplementations")
    void consumeRejectsNegativeDuration(
            Battery battery,
            String expectedType,
            double expectedCapacity) {

        assertThrows(
                IllegalArgumentException.class,
                () -> battery.consume(-0.1));
    }

    /**
     * Tests the formatted string representation for one representative battery.
     */
    @Test
    void toStringContainsTypeAndChargeInformation() {
        Battery battery = new BatteryLowCapacity();
        battery.consume(10.0);

        assertEquals(
                "Battery type: Low Capacity\n"
                        + "Battery level: 350 of 360\n",
                battery.toString());
    }

    // ########################################################################
    // Test data
    // ########################################################################

    /**
     * Provides all battery implementations and their expected metadata.
     *
     * @return stream of battery test arguments.
     */
    private static Stream<Arguments> batteryImplementations() {
        return Stream.of(
                Arguments.of(
                        new BatteryLowCapacity(),
                        "Low Capacity",
                        ModelSettings.BATTERY_LOW_CAPACITY_OPERATION_SECONDS),
                Arguments.of(
                        new BatteryMediumCapacity(),
                        "Medium Capacity",
                        ModelSettings.BATTERY_MEDIUM_CAPACITY_OPERATION_SECONDS),
                Arguments.of(
                        new BatteryHighCapacity(),
                        "High Capacity",
                        ModelSettings.BATTERY_HIGH_CAPACITY_OPERATION_SECONDS)
        );
    }
}
