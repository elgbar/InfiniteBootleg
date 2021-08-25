package no.elg.infiniteBootleg;

import no.elg.infiniteBootleg.util.Ticker;

/**
 * @author Elg
 */
public interface Ticking {

    /**
     * Tick an object. This will be called in sync with the current {@link Ticker}.
     * <p>
     * You might want to use synchronization either on the whole method or parts of the method to ensure correctness
     */
    void tick();

    /**
     * Update rarely (for expensive methods) this should be called every {@link Ticker#getTickRareRate()} ticks.
     * <p>
     * You might want to use synchronization either on the whole method or parts of the method to ensure correctness
     */
    default void tickRare() { }
}
