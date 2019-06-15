package no.elg.infiniteBootleg.util;

import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public interface Binembly {

    /**
     * @return This object as bytes
     */
    @NotNull byte[] disassemble();

    /**
     * Assemble the given bytes to restore an object
     */
    void assemble(@NotNull byte[] bytes);

}
