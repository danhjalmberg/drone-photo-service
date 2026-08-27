package io.github.danhjalmberg.dronephotoservice.views.viewdata;

/**
 * Describes the result of map hit testing.
 *
 * <p>A selection identifies either a drone or a completed task by name.
 * {@link Type#NONE} carries a {@code null} name and represents a click or hover
 * that did not hit a selectable entity.</p>
 */
public final class MapSelection {

    /**
     * Identifies the kind of selectable object represented by a map selection.
     */
    public enum Type {
        /**
         * No selectable map entity was hit.
         */
        NONE,
        /**
         * A drone was hit.
         */
        DRONE,
        /**
         * A completed task marker was hit.
         */
        COMPLETED_TASK
    }

    private final Type type;
    private final String name;

    /**
     * Creates a typed map-selection value.
     *
     * @param type the type of the selection
     * @param name selected entity name, or {@code null} for {@link Type#NONE}
     */
    private MapSelection(Type type, String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * Creates a selection representing a map hit with no selectable entity.
     *
     * @return selection representing no map entity
     */
    public static MapSelection none() {
        return new MapSelection(Type.NONE, null);
    }

    /**
     * Creates a drone selection.
     *
     * @param name selected drone name
     * @return drone selection carrying {@code name}
     */
    public static MapSelection drone(String name) {
        return new MapSelection(Type.DRONE, name);
    }

    /**
     * Creates a completed-task selection.
     *
     * @param name selected completed-task name
     * @return completed-task selection carrying {@code name}
     */
    public static MapSelection completedTask(String name) {
        return new MapSelection(Type.COMPLETED_TASK, name);
    }

    /**
     * Returns the kind of entity represented by this selection.
     *
     * @return kind of map-selection result
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the name of the selected map entity.
     *
     * @return selected entity name, or {@code null} for no selection
     */
    public String getName() {
        return name;
    }

    /**
     * Reports whether this selection represents no map entity.
     *
     * @return {@code true} when no selectable entity was hit
     */
    public boolean isNone() {
        return type == Type.NONE;
    }
}
