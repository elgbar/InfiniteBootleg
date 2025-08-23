package no.elg.infiniteBootleg.client.world.render

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.OrderedMap
import ktx.collections.component1
import ktx.collections.component2
import no.elg.infiniteBootleg.client.world.chunks.TexturedChunkImpl
import no.elg.infiniteBootleg.core.api.Renderer
import no.elg.infiniteBootleg.core.util.launchOnAsyncSuspendable
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.chunks.Chunk.Companion.CHUNK_TEXTURE_SIZE
import no.elg.infiniteBootleg.core.world.chunks.Chunk.Companion.CHUNK_TEXTURE_SIZE_F
import no.elg.infiniteBootleg.core.world.chunks.ChunkColumn

class CachedChunkRenderer(private val worldRender: ClientWorldRender) : Renderer {

  private val batch get() = worldRender.batch
  private val region = TextureRegion()

  private val chunksToDraw: OrderedMap<Chunk, Texture> = OrderedMap<Chunk, Texture>().apply {
    orderedKeys().ordered = false
  }
  val chunksToDrawIterator: OrderedMap.OrderedMapEntries<Chunk, Texture> = OrderedMap.OrderedMapEntries(chunksToDraw)

  private var regionHasBeenSetup = false
  private fun setupRegion() {
    if (!regionHasBeenSetup) {
      regionHasBeenSetup = true

      region.setRegion(0, 0, CHUNK_TEXTURE_SIZE, CHUNK_TEXTURE_SIZE)
      region.flip(false, true)
    }
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
        val maybeLoadedChunk = worldRender.world.getChunk(chunkX, chunkY, false)
        if (maybeLoadedChunk == null) {
          launchOnAsyncSuspendable { worldRender.world.loadChunk(chunkX, chunkY) }
          continue
        }
        val chunk = maybeLoadedChunk as? TexturedChunkImpl ?: error("Chunk is not a TexturedChunkImpl: $maybeLoadedChunk")
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
      region.texture = texture
      setupRegion() // must be called after setting texture
      val dx = chunk.chunkX * CHUNK_TEXTURE_SIZE_F
      val dy = chunk.chunkY * CHUNK_TEXTURE_SIZE_F
      batch.draw(region, dx, dy, CHUNK_TEXTURE_SIZE_F, CHUNK_TEXTURE_SIZE_F)
    }
    batch.enableBlending()
  }
}
