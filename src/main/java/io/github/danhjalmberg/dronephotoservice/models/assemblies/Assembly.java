package io.github.danhjalmberg.dronephotoservice.models.assemblies;

import io.github.danhjalmberg.dronephotoservice.models.components.Battery;
import io.github.danhjalmberg.dronephotoservice.models.components.Camera;
import io.github.danhjalmberg.dronephotoservice.models.components.Motor;

/**
 * Creates a compatible family of components used to construct a drone.
 *
 * <p>Each assembly acts as an abstract factory for one battery, camera, and
 * motor configuration. Every factory method creates a new component
 * instance.</p>
 *
 * @author Dan Hjälmberg
 */

public interface Assembly {

    /**
     * Creates the battery for this component family.
     *
     * @return new battery instance
     */
    Battery createBattery();

    /**
     * Creates the camera for this component family.
     *
     * @return new camera instance
     */
    Camera createCamera();

    /**
     * Creates the motor for this component family.
     *
     * @return new motor instance
     */
    Motor createMotor();
}
