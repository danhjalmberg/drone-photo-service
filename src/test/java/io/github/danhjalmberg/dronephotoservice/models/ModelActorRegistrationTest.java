package io.github.danhjalmberg.dronephotoservice.models;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests atomic actor registration and executor submission in {@link Model}.
 */
class ModelActorRegistrationTest {

    private Model model;

    /**
     * Creates an empty model before each test.
     */
    @BeforeEach
    void setUp() {
        model = new Model();
        model.resetSimulation();
    }

    /**
     * Stops any actors and executor pools left by a test.
     *
     * <p>This failure-safe cleanup also runs when an actor-registration
     * assertion fails.</p>
     *
     * @throws InterruptedException if actor-pool shutdown is interrupted
     */
    @AfterEach
    void tearDown() throws InterruptedException {
        model.stopSimulationActors();
        model.shutdownActorPools();
    }

    /**
     * Tests that a missing executor leaves photo-agency state and identity
     * allocation unchanged.
     *
     */
    @Test
    void failedPhotoAgencyAdditionLeavesModelUnchanged() {

        assertThrows(IllegalStateException.class, model::addPhotoAgency);
        assertEquals(0, model.getPhotoAgencyCount());

        model.createPhotoAgencyPool(1);

        model.addPhotoAgency();
        assertEquals(1, model.getPhotoAgencyCount());
        assertEquals(1, model.getPhotoAgencySnapshots().get(0).getId());
    }

    /**
     * Tests that a missing executor leaves drone state and identity allocation
     * unchanged.
     *
     */
    @Test
    void failedDroneAdditionLeavesModelUnchanged() {

        assertThrows(IllegalStateException.class, model::addDrone);
        assertEquals(0, model.getDroneCount());

        model.createDronePool(1);

        model.addDrone();
        assertEquals(1, model.getDroneCount());
        assertEquals("drone_1", model.getDroneSnapshots().get(0).getName());
    }
}
