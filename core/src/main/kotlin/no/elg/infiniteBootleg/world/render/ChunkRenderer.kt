package no.elg.infiniteBootleg.world.render

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
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.Settings.chunksToRenderEachFrame
import no.elg.infiniteBootleg.api.Renderer
import no.elg.infiniteBootleg.events.api.EventManager.dispatchEvent
import no.elg.infiniteBootleg.events.chunks.ChunkAddedToChunkRendererEvent
import no.elg.infiniteBootleg.events.chunks.ChunkTextureChangeRejectedEvent
import no.elg.infiniteBootleg.events.chunks.ChunkTextureChangeRejectedEvent.Companion.CHUNK_ABOVE_TOP_BLOCK_REASON
import no.elg.infiniteBootleg.events.chunks.ChunkTextureChangeRejectedEvent.Companion.CHUNK_INVALID_REASON
import no.elg.infiniteBootleg.events.chunks.ChunkTextureChangeRejectedEvent.Companion.CHUNK_OUT_OF_VIEW_REASON
import no.elg.infiniteBootleg.events.chunks.ChunkTextureChangedEvent
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.ChunkCompactLoc
import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.util.chunkToWorld
import no.elg.infiniteBootleg.util.getNoise
import no.elg.infiniteBootleg.util.isMarkerBlock
import no.elg.infiniteBootleg.util.launchOnMultithreadedAsync
import no.elg.infiniteBootleg.util.safeUse
import no.elg.infiniteBootleg.world.blocks.Block.Companion.BLOCK_SIZE
import no.elg.infiniteBootleg.world.blocks.Block.Companion.materialOrAir
import no.elg.infiniteBootleg.world.blocks.BlockLight.Companion.LIGHT_RESOLUTION
import no.elg.infiniteBootleg.world.blocks.BlockLight.Companion.lightMapIndex
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.chunks.ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG
import no.elg.infiniteBootleg.world.chunks.ChunkColumn.Companion.FeatureFlag.TOP_MOST_FLAG
import no.elg.infiniteBootleg.world.generator.noise.FastNoiseLite
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion

typealias SystemTimeMillis = Long

/**
 * @author Elg
 */
class ChunkRenderer(private val worldRender: WorldRender) : Renderer, Disposable {

  private val batch: SpriteBatch = SpriteBatch().also {
    it.projectionMatrix =
      Matrix4().setToOrtho2D(0f, 0f, Chunk.CHUNK_TEXTURE_SIZE.toFloat(), Chunk.CHUNK_TEXTURE_SIZE.toFloat())
  }

  // long = SystemTimeMillis
  // map:  ChunkCompactLoc -> SystemTimeMillis
  @GuardedBy("QUEUE_LOCK")
  private val chunkLocToTimeAdded: Long2LongSortedMap = Long2LongLinkedOpenHashMap()

  // When the chunk was added to the queue
  // map: SystemTimeMillis -> Chunk[]
  @GuardedBy("QUEUE_LOCK")
  private val renderTimeAdded: Long2ObjectLinkedOpenHashMap<ObjectLinkedOpenHashSet<Chunk>> = Long2ObjectLinkedOpenHashMap()

  // current rendering chunk
  @GuardedBy("QUEUE_LOCK")
  private var curr: Chunk? = null
    set(value) = synchronized(QUEUE_LOCK) {
      field = value
    }
  private val splitCache: MutableMap<TextureRegion, Array<Array<TextureRegion>>> = HashMap()

  private val rotationNoise: FastNoiseLite =
    FastNoiseLite(worldRender.world.seed.toInt()).also {
      it.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2)
      it.SetFrequency(1f)
    }

  private fun nextChunk(): Chunk? {
    return synchronized(QUEUE_LOCK) {
      val (time: SystemTimeMillis, chunkTimeBucket: ObjectSortedSet<Chunk>) = renderTimeAdded.firstEntry() ?: let {
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
  fun queueRendering(chunk: Chunk, prioritize: Boolean) {
    launchOnMultithreadedAsync {
      val pos: ChunkCompactLoc = chunk.compactLocation
      synchronized(QUEUE_LOCK) {
        if (chunk === curr) {
          return@launchOnMultithreadedAsync
        }

        // Time used to prioritize the chunk, a chunk added a while a go should be prioritized over a chunk added just now with prioritize = true to not get stale chunk textures
        val newTime: SystemTimeMillis = System.currentTimeMillis() + if (prioritize) -PRIORITIZATION_ADVANTAGE_ADD_TIME else 0
        val existingTime: SystemTimeMillis = chunkLocToTimeAdded[pos]

        // Remove existing chunk from time bucket when the new time is less than the existing time to push it forward in the queue
        if (existingTime != NOT_IN_COLLECTION && newTime < existingTime) {
          val chunks = renderTimeAdded[existingTime] ?: error("Chunk $chunk is in the queue but not in the renderTimeAdded map")
          chunks.remove(chunk)
          if (chunks.isEmpty) {
            renderTimeAdded.remove(existingTime)
          }
        }
        // Add the chunk to the queue
        if (existingTime == NOT_IN_COLLECTION || newTime < existingTime) {
          chunkLocToTimeAdded[pos] = newTime
          val timeSet = renderTimeAdded.computeIfAbsent(newTime) { ObjectLinkedOpenHashSet() }
          timeSet.addAndMoveToLast(chunk)
        }
      }
      chunksInRenderQueue = chunkLocToTimeAdded.size
      dispatchEvent(ChunkAddedToChunkRendererEvent(chunk.compactLocation, prioritize))
    }
  }

  fun renderMultiple() {
    repeat(chunksToRenderEachFrame) {
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
    val chunk: Chunk = synchronized(QUEUE_LOCK) {
      do {
        if (chunkLocToTimeAdded.isEmpty()) {
          chunksInRenderQueue = 0
          // nothing to render
          return
        }
        val candidateChunk: Chunk = nextChunk()
        if (candidateChunk.isInvalid) {
          dispatchEvent(ChunkTextureChangeRejectedEvent(candidateChunk.compactLocation, CHUNK_INVALID_REASON))
          continue
        }
        if (worldRender.isOutOfView(candidateChunk)) {
          dispatchEvent(ChunkTextureChangeRejectedEvent(candidateChunk.compactLocation, CHUNK_OUT_OF_VIEW_REASON))
          continue
        }
        if (candidateChunk.isAllAir && candidateChunk.chunkColumn.isChunkAboveTopBlock(candidateChunk.chunkY, TOP_MOST_FLAG)) {
          dispatchEvent(ChunkTextureChangeRejectedEvent(candidateChunk.compactLocation, CHUNK_ABOVE_TOP_BLOCK_REASON))
          continue
        }
        curr = candidateChunk
        return@synchronized candidateChunk
      } while (true)
      error("Should never reach here")
    }
    doRenderChunk(chunk)
    if (Settings.renderChunkUpdates) {
      dispatchEvent(ChunkTextureChangedEvent(chunk.compactLocation))
    }
    curr = null
  }

  private fun doRenderChunk(chunk: Chunk) {
    val fbo = chunk.frameBuffer ?: return
    val chunkColumn = chunk.chunkColumn

    // this is the main render function
    val blocks = chunk.blocks
    fbo.use {
      batch.safeUse {
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        for (localX in 0 until Chunk.CHUNK_SIZE) {
          val topBlockHeight = chunkColumn.topBlockHeight(localX, BLOCKS_LIGHT_FLAG)
          for (localY in 0 until Chunk.CHUNK_SIZE) {
            val block = blocks[localX][localY]
            val material = block.materialOrAir()
            var texture: RotatableTextureRegion
            var secondaryTexture: RotatableTextureRegion?

            val worldY = chunk.chunkY.chunkToWorld(localY)
            if (material.invisibleBlock || block.isMarkerBlock()) {
              texture = if (topBlockHeight > worldY) Main.inst().assets.caveTexture else Main.inst().assets.skyTexture
              secondaryTexture = null
            } else {
              texture = block?.texture ?: continue
              secondaryTexture = if (material.hasTransparentTexture) {
                if (topBlockHeight > worldY) Main.inst().assets.caveTexture else Main.inst().assets.skyTexture
              } else {
                null
              }
            }
            val dx = localX * BLOCK_SIZE
            val dy = localY * BLOCK_SIZE
            val rotation = calculateRotation(chunk, localX, localY)

            batch.color = Color.WHITE
            val blockLight = chunk.getBlockLight(localX, localY)
            if (Settings.renderLight && blockLight.isLit && (!blockLight.isSkylight || texture.rotationAllowed)) {
              if (secondaryTexture != null) {
                // If the block is emitting light there is no point in drawing it shaded
                if (material.emitsLight) {
                  drawRotatedTexture(secondaryTexture, dx, dy, rotation)
                } else {
                  drawShadedBlock(secondaryTexture, blockLight.lightMap, dx, dy, rotation)
                }
              }
              drawShadedBlock(texture, blockLight.lightMap, dx, dy, rotation)
            } else {
              if (Settings.renderLight && !blockLight.isSkylight) {
                batch.color = Color.BLACK
              }
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

  private fun drawRotatedTexture(texture: RotatableTextureRegion, dx: Int, dy: Int, rotation: Int) {
    if (rotation == 0 || !texture.rotationAllowed) {
      batch.draw(texture.textureRegion, dx.toFloat(), dy.toFloat(), BLOCK_SIZE.toFloat(), BLOCK_SIZE.toFloat())
    } else {
      batch.draw(
        texture.textureRegion,
        dx.toFloat(),
        dy.toFloat(),
        BLOCK_SIZE / 2f,
        BLOCK_SIZE / 2f,
        BLOCK_SIZE.toFloat(),
        BLOCK_SIZE.toFloat(),
        1f,
        1f,
        rotation.toFloat()
      )
    }
  }

  private fun drawShadedBlock(
    textureRegion: RotatableTextureRegion,
    lights: FloatArray,
    dx: Int,
    dy: Int,
    rotation: Int
  ) {
    val texture = textureRegion.textureRegion
    val tileWidth = texture.regionWidth / LIGHT_RESOLUTION
    val tileHeight = texture.regionHeight / LIGHT_RESOLUTION
    val split = splitCache.computeIfAbsent(texture) { t: TextureRegion -> t.split(tileWidth, tileHeight) }
    var ry = 0
    val splitLength = split.size
    while (ry < splitLength) {
      val regions = split[LIGHT_RESOLUTION - ry - 1]
      var rx = 0
      val regionsLength = regions.size
      while (rx < regionsLength) {
        val region = regions[rx]
        val lightIntensity = lights[lightMapIndex(rx, ry)]
        batch.setColor(lightIntensity, lightIntensity, lightIntensity, 1f)
        if (textureRegion.rotationAllowed || rotation == 0) {
          batch.draw(
            region,
            dx + rx * LIGHT_SUBBLOCK_SIZE,
            dy + ry * LIGHT_SUBBLOCK_SIZE,
            LIGHT_SUBBLOCK_SIZE / 2,
            LIGHT_SUBBLOCK_SIZE / 2,
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

  override fun dispose() {
    batch.dispose()
    synchronized(QUEUE_LOCK) { chunkLocToTimeAdded.clear() }
  }

  companion object {
    /** How many [Graphics.getFramesPerSecond] should there be when rendering multiple chunks  */
    const val FPS_FAST_CHUNK_RENDER_THRESHOLD = 10

    const val LIGHT_SUBBLOCK_SIZE = BLOCK_SIZE / LIGHT_RESOLUTION.toFloat()

    const val CAVE_CLEAR_COLOR_R = 0.408824f
    const val CAVE_CLEAR_COLOR_G = 0.202941f
    const val CAVE_CLEAR_COLOR_B = 0.055882f

    private val QUEUE_LOCK = Any()

    private const val PRIORITIZATION_ADVANTAGE_ADD_TIME = 1000L
    private const val NOT_IN_COLLECTION = 0L

    var chunksInRenderQueue: Int = 0
  }
}
