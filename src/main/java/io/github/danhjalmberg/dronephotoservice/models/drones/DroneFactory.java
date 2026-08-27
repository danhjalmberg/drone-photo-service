package io.github.danhjalmberg.dronephotoservice.models.drones;

import io.github.danhjalmberg.dronephotoservice.models.assemblies.AssemblyType1;
import io.github.danhjalmberg.dronephotoservice.models.assemblies.AssemblyType2;
import io.github.danhjalmberg.dronephotoservice.models.assemblies.AssemblyType3;
import io.github.danhjalmberg.dronephotoservice.models.assemblies.Assembly;

/**
 * Creates fully assembled drones for the supported fixed configurations.
 *
 * <p>Each {@link DroneType} selects a concrete {@link Assembly} Abstract
 * Factory. The selected assembly creates a compatible battery, camera, and
 * motor family used to construct the drone.</p>
 *
 * @author Dan Hjälmberg
 */
public final class DroneFactory {

    /**
     * Creates a factory for the supported drone assembly configurations.
     */
    public DroneFactory() {
    }

    /**
     * Creates a fully assembled drone of the requested type.
     *
     * @param droneType supported drone configuration
     * @return fully assembled drone
     * @throws NullPointerException if {@code droneType} is {@code null}
     */
    public Drone createDrone(DroneType droneType) {

        Assembly assembly = switch (droneType) {
            case TYPE_1 -> new AssemblyType1();
            case TYPE_2 -> new AssemblyType2();
            case TYPE_3 -> new AssemblyType3();
        };

        return createDrone(assembly);
    }

    /**
     * Creates a drone from the compatible component family produced by an
     * assembly.
     *
     * @param assembly provider of the drone's required components
     * @return fully assembled drone
     * @throws NullPointerException if a supplied component is {@code null}
     */
    private Drone createDrone(Assembly assembly) {
        return new Drone(
                assembly.createBattery(),
                assembly.createCamera(),
                assembly.createMotor());
    }
}
