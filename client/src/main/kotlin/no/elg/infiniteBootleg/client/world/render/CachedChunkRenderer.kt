package no.elg.infiniteBootleg.client.world.render

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.OrderedMap
import ktx.collections.component1
import ktx.collections.component2
import no.elg.infiniteBootleg.client.world.chunks.TexturedChunkImpl
import no.elg.infiniteBootleg.core.api.Renderer
import no.elg.infiniteBootleg.core.util.launchOnAsync
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.chunks.ChunkColumn

class CachedChunkRenderer(private val worldRender: ClientWorldRender) : Renderer {

  private val batch get() = worldRender.batch
  private val region = TextureRegion()

  private val chunksToDraw: OrderedMap<Chunk, Texture> = OrderedMap<Chunk, Texture>().apply {
    orderedKeys().ordered = false
  }
  val chunksToDrawIterator: OrderedMap.OrderedMapEntries<Chunk, Texture> = OrderedMap.OrderedMapEntries(chunksToDraw)

  private fun prepareChunks() {
    val chunksInView = worldRender.chunksInView
    chunksToDraw.clear(chunksInView.size)
    val verticalStart = chunksInView.verticalStart
    val verticalEnd = chunksInView.verticalEnd
    for (chunkY in verticalStart until verticalEnd) {
      val horizontalStart = chunksInView.horizontalStart
      val horizontalEnd = chunksInView.horizontalEnd
      for (chunkX in horizontalStart until horizontalEnd) {
        val chunk = worldRender.world.getChunk(chunkX, chunkY, false) as? TexturedChunkImpl?
        if (chunk == null) {
          launchOnAsync { worldRender.world.loadChunk(chunkX, chunkY) }
          continue
        }
        chunk.view()

        // No need to update texture when out of view, but in loaded zone
        if (chunkY == verticalEnd - 1 ||
          chunkY == verticalStart ||
          chunkX == horizontalStart ||
          chunkX == horizontalEnd - 1 ||
          (chunk.isAllAir && chunk.chunkColumn.isChunkAboveTopBlock(chunk.chunkY, ChunkColumn.Companion.FeatureFlag.TOP_MOST_FLAG))
        ) {
          continue
        }

        // get texture here to update last viewed in chunk
        val textureRegion = chunk.texture ?: continue
        chunksToDraw.put(chunk, textureRegion)
      }
    }
  }

  override fun render() {
    prepareChunks()
    batch.disableBlending()
    chunksToDrawIterator.reset()
    for ((chunk, texture) in chunksToDrawIterator) {
      if (region.texture == null) {
        region.setRegion(texture)
        region.flip(false, true)
      } else {
        region.texture = texture
      }
      val dx = chunk.chunkX * Chunk.Companion.CHUNK_TEXTURE_SIZE
      val dy = chunk.chunkY * Chunk.Companion.CHUNK_TEXTURE_SIZE
      batch.draw(region, dx.toFloat(), dy.toFloat(), Chunk.Companion.CHUNK_TEXTURE_SIZE.toFloat(), Chunk.Companion.CHUNK_TEXTURE_SIZE.toFloat())
    }
    region.texture = null
    batch.enableBlending()
  }
}
