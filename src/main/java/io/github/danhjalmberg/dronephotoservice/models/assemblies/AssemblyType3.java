package io.github.danhjalmberg.dronephotoservice.models.assemblies;

import io.github.danhjalmberg.dronephotoservice.models.components.Battery;
import io.github.danhjalmberg.dronephotoservice.models.components.BatteryHighCapacity;
import io.github.danhjalmberg.dronephotoservice.models.components.Camera;
import io.github.danhjalmberg.dronephotoservice.models.components.CameraColor;
import io.github.danhjalmberg.dronephotoservice.models.components.Motor;
import io.github.danhjalmberg.dronephotoservice.models.components.MotorHighPower;

/**
 * Creates the type 3 component family: a high-capacity battery, color camera,
 * and high-power motor.
 *
 * @author Dan Hjälmberg
 */
public class AssemblyType3 implements Assembly {

    /**
     * Creates a type 3 component assembly.
     */
    public AssemblyType3() {
    }

    /**
     * {@inheritDoc}
     */
    public Battery createBattery() {
        return new BatteryHighCapacity();
    }

    /**
     * {@inheritDoc}
     */
    public Camera createCamera() {
        return new CameraColor();
    }

    /**
     * {@inheritDoc}
     */
    public Motor createMotor() {
        return new MotorHighPower();
    }
}
