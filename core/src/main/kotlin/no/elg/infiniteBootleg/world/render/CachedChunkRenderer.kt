package no.elg.infiniteBootleg.world.render

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.OrderedMap
import ktx.collections.component1
import ktx.collections.component2
import no.elg.infiniteBootleg.api.Renderer
import no.elg.infiniteBootleg.util.launchOnAsync
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.chunks.ChunkColumn

class CachedChunkRenderer(private val worldRender: ClientWorldRender) : Renderer {

  private val chunksToDraw: OrderedMap<Chunk, TextureRegion> = OrderedMap<Chunk, TextureRegion>().apply {
    orderedKeys().ordered = false
  }

  private fun prepareChunks() {
    val chunksInView = worldRender.chunksInView
    chunksToDraw.clear(chunksInView.size)
    val verticalStart = chunksInView.verticalStart
    val verticalEnd = chunksInView.verticalEnd
    for (chunkY in verticalStart until verticalEnd) {
      val horizontalStart = chunksInView.horizontalStart
      val horizontalEnd = chunksInView.horizontalEnd
      for (chunkX in horizontalStart until horizontalEnd) {
        val chunk = worldRender.world.getChunk(chunkX, chunkY, false)
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
        val textureRegion = chunk.textureRegion
        if (textureRegion == null) {
          chunk.queueForRendering(true)
          continue
        }
        chunksToDraw.put(chunk, textureRegion)
      }
    }
  }

  override fun render() {
    prepareChunks()
    for ((chunk, textureRegion) in chunksToDraw.entries()) {
      val dx = chunk.chunkX * Chunk.CHUNK_TEXTURE_SIZE
      val dy = chunk.chunkY * Chunk.CHUNK_TEXTURE_SIZE
      worldRender.batch.draw(textureRegion, dx.toFloat(), dy.toFloat(), Chunk.CHUNK_TEXTURE_SIZE.toFloat(), Chunk.CHUNK_TEXTURE_SIZE.toFloat())
    }
  }
}
