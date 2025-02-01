package no.elg.infiniteBootleg.world.chunks

/**
 * A chunk that know when it was last viewed
 */
interface ViewableChunk : Chunk {

  val lastViewedTick: Long

  /** Mark this chunk as viewed during the current tick  */
  fun view()
}
