package no.elg.infiniteBootleg;

import no.elg.infiniteBootleg.world.WorldTicker;

/**
 * @author Elg
 */
public interface Ticking {

    /**
     * How many ticks between each rare update. Currently each rare tick is the same as one second
     */
    long TICK_RARE_RATE = WorldTicker.TICKS_PER_SECOND;

    /**
     * Tick an object. This will be called in sync with the current {@link WorldTicker}
     */
    void tick();

    /**
     * Update rarely (for expensive methods) this should be called every {@link #TICK_RARE_RATE} ticks
     */
    default void tickRare() {}
}
