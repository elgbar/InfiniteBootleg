package no.elg.infiniteBootleg.util;

/** @author Elg */
public interface Assemble {

  /**
   * Assemble the given bytes to restore an object
   *
   * @return If it was assembled without errors
   */
  boolean assemble(byte[] bytes);
}
