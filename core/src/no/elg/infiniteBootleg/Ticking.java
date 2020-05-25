package no.elg.infiniteBootleg;

import no.elg.infiniteBootleg.util.Ticker;

/**
 * @author Elg
 */
public interface Ticking {

    /**
     * Tick an object. This will be called in sync with the current {@link Ticker}
     */
    void tick();

    /**
     * Update rarely (for expensive methods) this should be called every {@link Ticker#getTickRareRate()} ticks
     */
    default void tickRare() {}
}
