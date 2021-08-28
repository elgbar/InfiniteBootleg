package no.elg.infiniteBootleg.util;

/**
 * @author Elg
 */
public interface Assemble {

    /**
     * Assemble the given bytes to restore an object
     */
    void assemble(byte[] bytes);

}
