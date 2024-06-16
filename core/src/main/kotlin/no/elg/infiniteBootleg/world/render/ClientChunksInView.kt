package no.elg.infiniteBootleg.world.render

/** @author Elg
 */
data class ClientChunksInView(
  override var horizontalStart: Int = 0,
  override var horizontalEnd: Int = 0,
  override var verticalStart: Int = 0,
  override var verticalEnd: Int = 0
) : ChunksInView
