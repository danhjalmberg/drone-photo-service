package io.github.danhjalmberg.dronephotoservice.models.assemblies;

import io.github.danhjalmberg.dronephotoservice.models.components.Battery;
import io.github.danhjalmberg.dronephotoservice.models.components.BatteryMediumCapacity;
import io.github.danhjalmberg.dronephotoservice.models.components.Camera;
import io.github.danhjalmberg.dronephotoservice.models.components.CameraGrayscale;
import io.github.danhjalmberg.dronephotoservice.models.components.Motor;
import io.github.danhjalmberg.dronephotoservice.models.components.MotorMediumPower;

/**
 * Creates the type 2 component family: a medium-capacity battery, grayscale
 * camera, and medium-power motor.
 *
 * @author Dan Hjälmberg
 */
public class AssemblyType2 implements Assembly {

    /**
     * Creates a type 2 component assembly.
     */
    public AssemblyType2() {
    }

    /**
     * {@inheritDoc}
     */
    public Battery createBattery() {
        return new BatteryMediumCapacity();
    }

    /**
     * {@inheritDoc}
     */
    public Camera createCamera() {
        return new CameraGrayscale();
    }

    /**
     * {@inheritDoc}
     */
    public Motor createMotor() {
        return new MotorMediumPower();
    }
}
