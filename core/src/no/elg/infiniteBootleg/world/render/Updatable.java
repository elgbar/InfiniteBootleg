package no.elg.infiniteBootleg.world.render;

import no.elg.infiniteBootleg.world.WorldTicker;

/**
 * @author Elg
 */
public interface Updatable {

    /**
     * How many ticks between each rare update
     */
    long UPDATE_RARE_RATE = WorldTicker.TICKS_PER_SECOND / 2;

    /**
     * Update the state, might be called every frame
     */
    void update();

    /**
     * Update rarely (for expensive methods) this should be called every {@link #UPDATE_RARE_RATE} ticks
     */
    default void updateRare() {}
}
