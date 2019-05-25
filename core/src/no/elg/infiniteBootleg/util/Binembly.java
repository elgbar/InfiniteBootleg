package no.elg.infiniteBootleg.util;

/**
 * @author Elg
 */
public interface Binembly {

    /**
     * @return This object as bytes
     */
    byte[] disassemble();

    /**
     * Assemble the given bytes to restore an object
     */
    void assemble(byte[] bytes);

}
