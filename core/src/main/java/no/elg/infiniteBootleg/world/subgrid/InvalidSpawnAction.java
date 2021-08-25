package no.elg.infiniteBootleg.world.subgrid;

/**
 * @author Elg
 */
public enum InvalidSpawnAction {
    /**
     * Delete the entity when the spawn location is invalid
     */
    DELETE,
    /**
     * Push the spawn location up til it is valid
     */
    PUSH_UP,
}
