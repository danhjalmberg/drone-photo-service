package io.github.danhjalmberg.dronephotoservice.views.viewdata;

import io.github.danhjalmberg.dronephotoservice.models.geometry.Vector2D;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the collection-ownership contract of
 * {@link MapDroneViewData}.
 *
 * @author Dan Hjälmberg
 */
class MapDroneViewDataTest {

    /**
     * Tests that later changes to the source list do not change view data.
     */
    @Test
    void constructorCopiesCompletedTasks() {
        MapTaskViewData firstTask = createTask("task_1");
        MapTaskViewData secondTask = createTask("task_2");
        List<MapTaskViewData> sourceTasks = new ArrayList<>();
        sourceTasks.add(firstTask);

        MapDroneViewData viewData = createViewData(sourceTasks);
        sourceTasks.add(secondTask);

        assertEquals(List.of(firstTask), viewData.getCompletedTasks());
    }

    /**
     * Tests that callers cannot mutate completed tasks through the getter.
     */
    @Test
    void getCompletedTasksReturnsUnmodifiableList() {
        MapDroneViewData viewData = createViewData(
                List.of(createTask("task_1")));

        assertThrows(
                UnsupportedOperationException.class,
                () -> viewData.getCompletedTasks().clear());
    }

    /**
     * Creates representative drone view data with the supplied completed tasks.
     *
     * @param completedTasks completed tasks to retain
     * @return constructed drone view data
     */
    private static MapDroneViewData createViewData(
            List<MapTaskViewData> completedTasks) {

        return new MapDroneViewData(
                Vector2D.ZERO,
                Vector2D.ZERO,
                "drone_1",
                null,
                completedTasks);
    }

    /**
     * Creates representative map task data.
     *
     * @param name task name
     * @return constructed task view data
     */
    private static MapTaskViewData createTask(String name) {
        return new MapTaskViewData(Vector2D.ZERO, name);
    }
}
