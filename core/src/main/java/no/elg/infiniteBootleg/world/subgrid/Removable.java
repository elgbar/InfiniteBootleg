package no.elg.infiniteBootleg.world.subgrid;

/** Marks this class as removable from f.eks the world */
@Deprecated
public interface Removable {

  default void onRemove() {}
}
