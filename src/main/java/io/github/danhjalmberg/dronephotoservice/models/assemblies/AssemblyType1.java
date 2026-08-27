package io.github.danhjalmberg.dronephotoservice.models.assemblies;

import io.github.danhjalmberg.dronephotoservice.models.components.Battery;
import io.github.danhjalmberg.dronephotoservice.models.components.BatteryLowCapacity;
import io.github.danhjalmberg.dronephotoservice.models.components.Camera;
import io.github.danhjalmberg.dronephotoservice.models.components.CameraNegative;
import io.github.danhjalmberg.dronephotoservice.models.components.Motor;
import io.github.danhjalmberg.dronephotoservice.models.components.MotorLowPower;

/**
 * Creates the type 1 component family: a low-capacity battery, negative
 * camera, and low-power motor.
 *
 * @author Dan Hjälmberg
 */
public class AssemblyType1 implements Assembly {

    /**
     * Creates a type 1 component assembly.
     */
    public AssemblyType1() {
    }

    /**
     * {@inheritDoc}
     */
    public Battery createBattery() {
        return new BatteryLowCapacity();
    }

    /**
     * {@inheritDoc}
     */
    public Camera createCamera() {
        return new CameraNegative();
    }

    /**
     * {@inheritDoc}
     */
    public Motor createMotor() {
        return new MotorLowPower();
    }
}
