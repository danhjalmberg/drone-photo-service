package io.github.danhjalmberg.dronephotoservice.models.drones;

import io.github.danhjalmberg.dronephotoservice.models.assemblies.AssemblyType1;
import io.github.danhjalmberg.dronephotoservice.models.components.BatteryHighCapacity;
import io.github.danhjalmberg.dronephotoservice.models.components.BatteryLowCapacity;
import io.github.danhjalmberg.dronephotoservice.models.components.BatteryMediumCapacity;
import io.github.danhjalmberg.dronephotoservice.models.components.CameraColor;
import io.github.danhjalmberg.dronephotoservice.models.components.CameraGrayscale;
import io.github.danhjalmberg.dronephotoservice.models.components.CameraNegative;
import io.github.danhjalmberg.dronephotoservice.models.components.MotorHighPower;
import io.github.danhjalmberg.dronephotoservice.models.components.MotorLowPower;
import io.github.danhjalmberg.dronephotoservice.models.components.MotorMediumPower;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests complete drone construction for every supported fixed configuration.
 */
class DroneFactoryTest {

    private final DroneFactory factory = new DroneFactory();

    /** Verifies that type 1 uses its configured component family. */
    @Test
    void type1CreatesExpectedComponentFamily() {
        Drone drone = factory.createDrone(DroneType.TYPE_1);
        assertInstanceOf(BatteryLowCapacity.class, drone.getBattery());
        assertInstanceOf(CameraNegative.class, drone.getCamera());
        assertInstanceOf(MotorLowPower.class, drone.getMotor());
    }

    /** Verifies that type 2 uses its configured component family. */
    @Test
    void type2CreatesExpectedComponentFamily() {
        Drone drone = factory.createDrone(DroneType.TYPE_2);
        assertInstanceOf(BatteryMediumCapacity.class, drone.getBattery());
        assertInstanceOf(CameraGrayscale.class, drone.getCamera());
        assertInstanceOf(MotorMediumPower.class, drone.getMotor());
    }

    /** Verifies that type 3 uses its configured component family. */
    @Test
    void type3CreatesExpectedComponentFamily() {
        Drone drone = factory.createDrone(DroneType.TYPE_3);
        assertInstanceOf(BatteryHighCapacity.class, drone.getBattery());
        assertInstanceOf(CameraColor.class, drone.getCamera());
        assertInstanceOf(MotorHighPower.class, drone.getMotor());
    }

    /** Verifies that every supported type creates a fully assembled drone. */
    @Test
    void everyDroneTypeCreatesFullyAssembledDrone() {
        for (DroneType droneType : DroneType.values()) {
            Drone drone = factory.createDrone(droneType);
            assertNotNull(drone.getBattery());
            assertNotNull(drone.getCamera());
            assertNotNull(drone.getMotor());
        }
    }

    /** Verifies that a null drone type is rejected. */
    @Test
    void createDroneRejectsNullType() {
        assertThrows(NullPointerException.class, () -> factory.createDrone(null));
    }

    /** Verifies that direct package-level construction requires every component. */
    @Test
    void droneConstructorRejectsMissingComponents() {
        AssemblyType1 assembly = new AssemblyType1();
        assertThrows(NullPointerException.class, () -> new Drone(
                null, assembly.createCamera(), assembly.createMotor()));
        assertThrows(NullPointerException.class, () -> new Drone(
                assembly.createBattery(), null, assembly.createMotor()));
        assertThrows(NullPointerException.class, () -> new Drone(
                assembly.createBattery(), assembly.createCamera(), null));
    }
}
