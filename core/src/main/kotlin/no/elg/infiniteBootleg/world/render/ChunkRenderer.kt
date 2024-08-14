package no.elg.infiniteBootleg.world.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import com.google.errorprone.annotations.concurrent.GuardedBy
import ktx.graphics.use
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.api.Renderer
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.events.chunks.ChunkTextureChangedEvent
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.util.chunkToWorld
import no.elg.infiniteBootleg.util.getNoise
import no.elg.infiniteBootleg.util.isMarkerBlock
import no.elg.infiniteBootleg.util.launchOnMultithreadedAsync
import no.elg.infiniteBootleg.util.safeUse
import no.elg.infiniteBootleg.world.blocks.Block.Companion.BLOCK_SIZE
import no.elg.infiniteBootleg.world.blocks.Block.Companion.materialOrAir
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.chunks.ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG
import no.elg.infiniteBootleg.world.chunks.ChunkColumn.Companion.FeatureFlag.TOP_MOST_FLAG
import no.elg.infiniteBootleg.world.generator.noise.FastNoiseLite
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion
import java.util.LinkedList

/**
 * @author Elg
 */
class ChunkRenderer(private val worldRender: WorldRender) : Renderer, Disposable {

  private val batch: SpriteBatch = SpriteBatch().also {
    it.projectionMatrix =
      Matrix4().setToOrtho2D(0f, 0f, Chunk.CHUNK_TEXTURE_SIZE.toFloat(), Chunk.CHUNK_TEXTURE_SIZE.toFloat())
  }

  // use linked list for fast adding to end and beginning
  @GuardedBy("QUEUE_LOCK")
  private val renderQueue: MutableList<Chunk> = LinkedList()

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

  /**
   * Queue rendering of a chunk. If the chunk is already in the queue to be rendered and `prioritize` is `true` then the chunk will be moved to the front of the queue
   *
   * @param chunk The chunk to render
   * @param prioritize If the chunk should be placed at the front of the queue being rendered
   */
  fun queueRendering(chunk: Chunk, prioritize: Boolean) {
    launchOnMultithreadedAsync {
      synchronized(QUEUE_LOCK) {
        if (chunk === curr) {
          return@launchOnMultithreadedAsync
        }
        val chunkIndex = renderQueue.indexOf(chunk)
        // Place the chunk at the front of the queue

        if (prioritize) {
          if (chunkIndex > 0) {
            // Chunk is in the queue, so we must remove it first
            renderQueue.removeAt(chunkIndex)
          }
          renderQueue.addFirst(chunk)
        } else if (chunkIndex == CHUNK_NOT_IN_QUEUE_INDEX) {
          // Only add if not already in queue to avoid duplicates
          renderQueue.addLast(chunk)
        }
      }
    }
  }

  fun renderMultiple() {
    render()
    if (Gdx.graphics.framesPerSecond > FPS_FAST_CHUNK_RENDER_THRESHOLD) {
      // only render more chunks when the computer isn't struggling with the rendering
      for (i in 0 until EXTRA_CHUNKS_TO_RENDER_EACH_FRAME) {
        render()
      }
    }
  }

  override fun render() {
    // get the first valid chunk to render
    var chunk: Chunk
    var aboveGround: Boolean
    synchronized(QUEUE_LOCK) {
      do {
        if (renderQueue.isEmpty()) {
          // nothing to render
          return
        }
        chunk = renderQueue.removeAt(0)
        aboveGround = chunk.chunkColumn.isChunkAboveTopBlock(chunk.chunkY, TOP_MOST_FLAG)
      } while ((chunk.isAllAir && aboveGround) || !chunk.isNotDisposed || worldRender.isOutOfView(chunk))
      curr = chunk
    }
    doRenderChunk(chunk)
    if (Settings.renderChunkUpdates) {
      EventManager.dispatchEvent(ChunkTextureChangedEvent(chunk))
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
    lights: Array<FloatArray>,
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
        val lightIntensity = lights[rx][ry]
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
    synchronized(QUEUE_LOCK) { renderQueue.clear() }
  }

  companion object {
    /** How many [Graphics.getFramesPerSecond] should there be when rendering multiple chunks  */
    const val FPS_FAST_CHUNK_RENDER_THRESHOLD = 10

    const val EXTRA_CHUNKS_TO_RENDER_EACH_FRAME = 4
    const val LIGHT_RESOLUTION = 2

    const val LIGHT_SUBBLOCK_SIZE = BLOCK_SIZE / LIGHT_RESOLUTION.toFloat()

    const val CAVE_CLEAR_COLOR_R = 0.408824f
    const val CAVE_CLEAR_COLOR_G = 0.202941f
    const val CAVE_CLEAR_COLOR_B = 0.055882f

    private const val CHUNK_NOT_IN_QUEUE_INDEX = -1

    private val QUEUE_LOCK = Any()
  }
}
