package no.elg.infiniteBootleg.client.world.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import com.google.errorprone.annotations.concurrent.GuardedBy
import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2LongSortedMap
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectSortedSet
import ktx.graphics.use
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.world.render.texture.TextureNeighbor
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.api.Renderer
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.events.chunks.ChunkAddedToChunkRendererEvent
import no.elg.infiniteBootleg.core.events.chunks.ChunkTextureChangeRejectedEvent
import no.elg.infiniteBootleg.core.events.chunks.ChunkTextureChangedEvent
import no.elg.infiniteBootleg.core.util.ChunkCompactLoc
import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.util.chunkToWorld
import no.elg.infiniteBootleg.core.util.getNoise
import no.elg.infiniteBootleg.core.util.isMarkerBlock
import no.elg.infiniteBootleg.core.util.launchOnMultithreadedAsync
import no.elg.infiniteBootleg.core.util.safeUse
import no.elg.infiniteBootleg.core.world.Direction
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.getRawRelative
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.materialOrAir
import no.elg.infiniteBootleg.core.world.blocks.BlockLight
import no.elg.infiniteBootleg.core.world.blocks.Brightness
import no.elg.infiniteBootleg.core.world.blocks.BrightnessArray
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.chunks.ChunkColumn
import no.elg.infiniteBootleg.core.world.chunks.TexturedChunk
import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite
import no.elg.infiniteBootleg.core.world.render.WorldRender
import no.elg.infiniteBootleg.core.world.render.texture.RotatableTextureRegion
import java.util.EnumMap

typealias SystemTimeMillis = Long

/**
 * @author Elg
 */
class ChunkRenderer(private val worldRender: WorldRender) : Renderer, Disposable {

  private val batch: SpriteBatch = SpriteBatch().also {
    it.projectionMatrix =
      Matrix4().setToOrtho2D(0f, 0f, Chunk.Companion.CHUNK_TEXTURE_SIZE.toFloat(), Chunk.Companion.CHUNK_TEXTURE_SIZE.toFloat())
  }

  // long = SystemTimeMillis
  // map:  ChunkCompactLoc -> SystemTimeMillis
  @GuardedBy("QUEUE_LOCK")
  private val chunkLocToTimeAdded: Long2LongSortedMap = Long2LongLinkedOpenHashMap()

  // When the chunk was added to the queue
  // map: SystemTimeMillis -> Chunk[]
  @GuardedBy("QUEUE_LOCK")
  private val renderTimeAdded: Long2ObjectLinkedOpenHashMap<ObjectLinkedOpenHashSet<TexturedChunk>> =
    Long2ObjectLinkedOpenHashMap()

  // current rendering chunk
  @GuardedBy("QUEUE_LOCK")
  @Volatile
  private var curr: TexturedChunk? = null
    set(value) = synchronized(QUEUE_LOCK) {
      field = value
    }
  private val splitCache: MutableMap<TextureRegion, Array<Array<TextureRegion>>> = HashMap()

  private val rotationNoise: FastNoiseLite =
    FastNoiseLite(worldRender.world.seed.toInt()).also {
      it.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2)
      it.SetFrequency(1f)
    }

  private fun nextChunk(): TexturedChunk? {
    return synchronized(QUEUE_LOCK) {
      val (time: SystemTimeMillis, chunkTimeBucket: ObjectSortedSet<TexturedChunk>) = renderTimeAdded.firstEntry() ?: let {
        chunksInRenderQueue = 0
        return null
      }
      val chunk = chunkTimeBucket.removeFirst()
      if (chunkTimeBucket.isEmpty()) {
        renderTimeAdded.remove(time)
      }
      chunkLocToTimeAdded.remove(chunk.compactLocation)
      chunksInRenderQueue = chunkLocToTimeAdded.size
      chunk
    }
  }

  /**
   * Queue rendering of a chunk. If the chunk is already in the queue to be rendered and `prioritize` is `true` then the chunk will be moved to the front of the queue
   *
   * @param chunk The chunk to render
   * @param prioritize If the chunk should be placed at the front of the queue being rendered
   */
  fun queueRendering(chunk: TexturedChunk, prioritize: Boolean) {
    launchOnMultithreadedAsync {
      val pos: ChunkCompactLoc = chunk.compactLocation
      synchronized(QUEUE_LOCK) {
        if (chunk === curr) {
          return@launchOnMultithreadedAsync
        }

        // Time used to prioritize the chunk, a chunk added a while a go should be prioritized over a chunk added just now with prioritize = true to not get stale chunk textures
        val newTime: SystemTimeMillis =
          System.currentTimeMillis() + if (prioritize) -PRIORITIZATION_ADVANTAGE_ADD_TIME else 0
        val existingTime: SystemTimeMillis = chunkLocToTimeAdded[pos]

        // Remove existing chunk from time bucket when the new time is less than the existing time to push it forward in the queue
        if (existingTime != NOT_IN_COLLECTION && newTime < existingTime) {
          val chunks =
            renderTimeAdded[existingTime] ?: error("Chunk $chunk is in the queue but not in the renderTimeAdded map")
          chunks.remove(chunk)
          if (chunks.isEmpty()) {
            renderTimeAdded.remove(existingTime)
          }
        }
        // Add the chunk to the queue
        if (existingTime == NOT_IN_COLLECTION || newTime < existingTime) {
          chunkLocToTimeAdded[pos] = newTime
          val timeSet =
            renderTimeAdded.computeIfAbsent(newTime) { ObjectLinkedOpenHashSet() }
          timeSet.addAndMoveToLast(chunk)
        }
      }
      chunksInRenderQueue = chunkLocToTimeAdded.size
      EventManager.dispatchEvent(ChunkAddedToChunkRendererEvent(chunk.compactLocation, prioritize))
    }
  }

  fun renderMultiple() {
    repeat(Settings.chunksToRenderEachFrame) {
      render()
    }
  }

  override fun render() {
    // fast return if there is nothing to render
    if (chunkLocToTimeAdded.isEmpty()) {
      chunksInRenderQueue = 0
      return
    }
    // get the first valid chunk to render
    val chunk: TexturedChunk = synchronized(QUEUE_LOCK) {
      do {
        if (chunkLocToTimeAdded.isEmpty()) {
          chunksInRenderQueue = 0
          // nothing to render
          return
        }
        val candidateChunk = nextChunk() ?: return
        if (candidateChunk.isInvalid) {
          EventManager.dispatchEvent(
            ChunkTextureChangeRejectedEvent(
              candidateChunk.compactLocation,
              ChunkTextureChangeRejectedEvent.Companion.CHUNK_INVALID_REASON
            )
          )
          continue
        }
        if (worldRender.isOutOfView(candidateChunk)) {
          EventManager.dispatchEvent(
            ChunkTextureChangeRejectedEvent(
              candidateChunk.compactLocation,
              ChunkTextureChangeRejectedEvent.Companion.CHUNK_OUT_OF_VIEW_REASON
            )
          )
          continue
        }
        if (candidateChunk.isAllAir && candidateChunk.chunkColumn.isChunkAboveTopBlock(
            candidateChunk.chunkY,
            ChunkColumn.Companion.FeatureFlag.TOP_MOST_FLAG
          )
        ) {
          EventManager.dispatchEvent(
            ChunkTextureChangeRejectedEvent(
              candidateChunk.compactLocation,
              ChunkTextureChangeRejectedEvent.Companion.CHUNK_ABOVE_TOP_BLOCK_REASON
            )
          )
          continue
        }
        curr = candidateChunk
        return@synchronized candidateChunk
      } while (true)
      error("Should never reach here")
    }
    doRenderChunk(chunk)
    if (Settings.renderChunkUpdates) {
      EventManager.dispatchEvent(ChunkTextureChangedEvent(chunk.compactLocation))
    }
    curr = null
  }

  private fun doRenderChunk(chunk: TexturedChunk) {
    val fbo = chunk.frameBuffer ?: return
    val chunkColumn = chunk.chunkColumn
    val assets = ClientMain.inst().assets

    // this is the main render function
    fbo.use { _ ->
      Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
      Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
      batch.safeUse { _ ->
        for (localX in 0 until Chunk.Companion.CHUNK_SIZE) {
          val topLightBlockHeight = chunkColumn.topBlockHeight(
            localX,
            ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG
          )
          for (localY in 0 until Chunk.Companion.CHUNK_SIZE) {
            batch.color = Color.WHITE
            val block = chunk.getRawBlock(localX, localY)
            val material = block.materialOrAir()
            val texture: RotatableTextureRegion
            val secondaryTexture: RotatableTextureRegion?

            val worldY = chunk.chunkY.chunkToWorld(localY)
            val dx = localX * Block.Companion.BLOCK_SIZE_F
            val dy = localY * Block.Companion.BLOCK_SIZE_F

            val isMarker = block.isMarkerBlock()
            if (material.invisibleBlock || isMarker) {
              if (isMarker && topLightBlockHeight == worldY) {
                // Draw half the texture as sky and lower half as cave.
                // This will remove visual artifact when a marker block is falling and updating the top light block height
                drawHalfwayTexture(assets.caveTexture, assets.skyTexture, dx, dy)
                continue
              }
              texture = if (topLightBlockHeight > worldY) assets.caveTexture else assets.skyTexture
              secondaryTexture = null
            } else {
              texture = block?.texture ?: continue
              secondaryTexture = if (material.hasTransparentTexture) {
                if (topLightBlockHeight > worldY) assets.caveTexture else assets.skyTexture
              } else {
                null
              }
            }

            val blockLight = chunk.getBlockLight(localX, localY)
            if (Settings.renderLight) {
              if (blockLight.isLit && (!blockLight.isSkylight || texture.rotationAllowed)) {
                val rotation = calculateRotation(chunk, localX, localY)
                if (secondaryTexture != null) {
                  // Optimization: If the block is emitting light there is no point in drawing it shaded
                  if (material.emitsLight) {
                    drawRotatedTexture(secondaryTexture, dx, dy, rotation)
                  } else {
                    drawShadedBlock(secondaryTexture, blockLight.lightMap, dx, dy, rotation)
                  }
                }
                drawShadedBlock(texture, blockLight.lightMap, dx, dy, rotation)
              } else {
                if (blockLight.isLit) {
                  val rotation = calculateRotation(chunk, localX, localY)
                  if (secondaryTexture != null) {
                    drawRotatedTexture(secondaryTexture, dx, dy, rotation)
                  }
                  drawRotatedTexture(texture, dx, dy, rotation)
                } else {
                  // Optimization: If the draw anything if the block is not lit or in the sky, the background is already cleared to black
                  continue
                }
              }
            } else {
              // No light, no problem
              val rotation = calculateRotation(chunk, localX, localY)
              if (secondaryTexture != null) {
                drawRotatedTexture(secondaryTexture, dx, dy, rotation)
              }
              drawRotatedTexture(texture, dx, dy, rotation)
            }
          }
        }
      }
    }
  }

  private fun calculateRotation(chunk: Chunk, localX: LocalCoord, localY: LocalCoord): Int {
    val noise = rotationNoise.getNoise(chunk.chunkX.chunkToWorld(localX), chunk.chunkY.chunkToWorld(localY))
    val cardinalDirections = 4
    val cardinalDirectionDegrees = 90
    return (noise * cardinalDirections).toInt() * cardinalDirectionDegrees
  }

  private fun drawHalfwayTexture(lowerHalf: RotatableTextureRegion, upperHalf: RotatableTextureRegion, dx: Float, dy: Float) {
    batch.draw(
      upperHalf.textureRegion,
      dx,
      dy + Block.Companion.HALF_BLOCK_SIZE_F,
      Block.Companion.BLOCK_SIZE_F,
      Block.Companion.HALF_BLOCK_SIZE_F
    )
    batch.draw(lowerHalf.textureRegion, dx, dy, Block.Companion.BLOCK_SIZE_F, Block.Companion.HALF_BLOCK_SIZE_F)
  }

  private fun drawRotatedTexture(texture: RotatableTextureRegion, dx: Float, dy: Float, rotation: Int) {
    if (rotation == NO_ROTATION || !texture.rotationAllowed) {
      batch.draw(texture.textureRegion, dx, dy, Block.Companion.BLOCK_SIZE_F, Block.Companion.BLOCK_SIZE_F)
    } else {
      batch.draw(
        texture.textureRegion,
        dx,
        dy,
        Block.Companion.HALF_BLOCK_SIZE_F,
        Block.Companion.HALF_BLOCK_SIZE_F,
        Block.Companion.BLOCK_SIZE_F,
        Block.Companion.BLOCK_SIZE_F,
        1f,
        1f,
        rotation.toFloat()
      )
    }
  }

  private fun drawShadedBlock(
    textureRegion: RotatableTextureRegion,
    lights: BrightnessArray,
    dx: Float,
    dy: Float,
    rotation: Int
  ) {
    val texture = textureRegion.textureRegion
    val tileWidth = texture.regionWidth / BlockLight.Companion.LIGHT_RESOLUTION
    val tileHeight = texture.regionHeight / BlockLight.Companion.LIGHT_RESOLUTION
    val split = splitCache.computeIfAbsent(texture) { t: TextureRegion -> t.split(tileWidth, tileHeight) }
    var ry = 0
    val splitLength = split.size
    while (ry < splitLength) {
      val regions = split[BlockLight.Companion.LIGHT_RESOLUTION - ry - 1]
      var rx = 0
      val regionsLength = regions.size
      while (rx < regionsLength) {
        val region = regions[rx]
        val brightness: Brightness = lights[BlockLight.Companion.lightMapIndex(rx, ry)]
        batch.setColor(brightness, brightness, brightness, 1f)
        if (textureRegion.rotationAllowed || rotation == 0) {
          batch.draw(
            region,
            dx + rx * LIGHT_SUBBLOCK_SIZE,
            dy + ry * LIGHT_SUBBLOCK_SIZE,
            HALF_LIGHT_SUBBLOCK_SIZE,
            HALF_LIGHT_SUBBLOCK_SIZE,
            LIGHT_SUBBLOCK_SIZE,
            LIGHT_SUBBLOCK_SIZE,
            1f,
            1f,
            rotation.toFloat()
          )
        } else {
          batch.draw(
            region,
            dx + rx * LIGHT_SUBBLOCK_SIZE,
            dy + ry * LIGHT_SUBBLOCK_SIZE,
            LIGHT_SUBBLOCK_SIZE,
            LIGHT_SUBBLOCK_SIZE
          )
        }
        rx++
      }
      ry++
    }
  }

  val Block.texture: RotatableTextureRegion?
    get() {
      val map = EnumMap<Direction, Material>(Direction::class.java)
      for (direction in Direction.CARDINAL) {
        val relMat = this.getRawRelative(direction, false).materialOrAir()
        map[direction] = relMat
      }
      return TextureNeighbor.getTexture(material, map)
    }

  override fun dispose() {
    batch.dispose()
    synchronized(QUEUE_LOCK) { chunkLocToTimeAdded.clear() }
  }

  companion object {

    const val LIGHT_SUBBLOCK_SIZE = Block.Companion.BLOCK_SIZE_F / BlockLight.Companion.LIGHT_RESOLUTION
    const val HALF_LIGHT_SUBBLOCK_SIZE = LIGHT_SUBBLOCK_SIZE * 0.5f

    const val CAVE_CLEAR_COLOR_R = 0.408824f
    const val CAVE_CLEAR_COLOR_G = 0.202941f
    const val CAVE_CLEAR_COLOR_B = 0.055882f

    const val NO_ROTATION = 0

    private val QUEUE_LOCK = Any()

    private const val PRIORITIZATION_ADVANTAGE_ADD_TIME = 1000L
    private const val NOT_IN_COLLECTION = 0L

    var chunksInRenderQueue: Int = 0
  }
}
