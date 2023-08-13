package no.elg.infiniteBootleg.world.render

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.utils.OrderedMap
import ktx.graphics.use
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.api.Renderer
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.WorldCoordNumber
import no.elg.infiniteBootleg.world.BOX2D_LOCK
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.chunks.ChunkColumn.Companion.FeatureFlag.TOP_MOST_FLAG
import no.elg.infiniteBootleg.world.render.ChunksInView.Companion.iterator
import no.elg.infiniteBootleg.world.render.debug.BlockLightDebugRenderer
import no.elg.infiniteBootleg.world.render.debug.DebugChunkRenderer
import no.elg.infiniteBootleg.world.world.ClientWorld
import kotlin.math.abs

/**
 * @author Elg
 */
class ClientWorldRender(override val world: ClientWorld) : WorldRender {

  private val viewBound: Rectangle = Rectangle()
  private val m4 = Matrix4()
  private val renderers: List<Renderer> = listOf(
    EntityRenderer(this),
    HoveringBlockRenderer(this)
  )
  private val chunkDebugRenderer: DebugChunkRenderer = DebugChunkRenderer(this)
  private val blockLightDebugRenderer: BlockLightDebugRenderer = BlockLightDebugRenderer(this)
  private var lastZoom = 0f

  val draw: OrderedMap<Chunk, TextureRegion> = OrderedMap<Chunk, TextureRegion>().apply {
    orderedKeys().ordered = false
  }
  val chunksInView: ClientChunksInView = ClientChunksInView()
  val batch: SpriteBatch = SpriteBatch()
  val camera: OrthographicCamera = OrthographicCamera().also {
    it.setToOrtho(false)
    it.zoom = 1f
    it.position.x = 0f
    it.position.y = 0f
  }
  val chunkRenderer: ChunkRenderer = ChunkRenderer(this)
  val box2DDebugRenderer: Box2DDebugRenderer = Box2DDebugRenderer(true, true, true, true, true, true)

  fun lookAt(worldX: WorldCoordNumber, worldY: WorldCoordNumber) {
    camera.position.set(worldX.toFloat(), worldY.toFloat(), 0f)
    update()
  }

  override fun render() {
    batch.projectionMatrix = camera.combined
    chunkRenderer.renderMultiple()
    draw.clear(chunksInView.size)
    draw.ensureCapacity(chunksInView.size)
    val worldBody = world.worldBody
    val worldOffsetX = worldBody.worldOffsetX * Block.BLOCK_SIZE
    val worldOffsetY = worldBody.worldOffsetY * Block.BLOCK_SIZE
    val verticalStart = chunksInView.verticalStart
    val verticalEnd = chunksInView.verticalEnd
    for (chunkY in verticalStart until verticalEnd) {
      val horizontalStart = chunksInView.horizontalStart
      val horizontalEnd = chunksInView.horizontalEnd
      for (chunkX in horizontalStart until horizontalEnd) {
        val chunk = world.getChunk(chunkX, chunkY, false)
        if (chunk == null) {
          Main.inst().scheduler.executeAsync { world.loadChunk(chunkX, chunkY) }
          continue
        }
        chunk.view()

        // No need to update texture when out of view, but in loaded zone
        if (chunkY == verticalEnd - 1 ||
          chunkY == verticalStart ||
          chunkX == horizontalStart ||
          chunkX == horizontalEnd - 1 ||
          (chunk.isAllAir && chunk.chunkColumn.isChunkAboveTopBlock(chunk.chunkY, TOP_MOST_FLAG))
        ) {
          continue
        }

        // get texture here to update last viewed in chunk
        val textureRegion = chunk.textureRegion
        if (textureRegion == null) {
          chunkRenderer.queueRendering(chunk, false)
          continue
        }
        draw.put(chunk, textureRegion)
      }
    }
    batch.use {
      for (entry in draw.entries()) {
        val dx = entry.key.chunkX * Chunk.CHUNK_TEXTURE_SIZE + worldOffsetX
        val dy = entry.key.chunkY * Chunk.CHUNK_TEXTURE_SIZE + worldOffsetY
        batch.draw(entry.value, dx, dy, Chunk.CHUNK_TEXTURE_SIZE.toFloat(), Chunk.CHUNK_TEXTURE_SIZE.toFloat())
      }
      for (renderer in renderers) {
        renderer.render()
      }
    }
    if (Settings.renderChunkUpdates || Settings.renderChunkBounds) {
      chunkDebugRenderer.render()
    }
    if (Settings.renderBox2dDebug) {
      synchronized(BOX2D_LOCK) {
        box2DDebugRenderer.render(worldBody.box2dWorld, m4)
      }
    }
    if (Settings.renderLight) {
      blockLightDebugRenderer.render()
    }
  }

  override fun update() {
    camera.update()
    m4.set(camera.combined).scl(Block.BLOCK_SIZE.toFloat())
    val width = camera.viewportWidth * camera.zoom
    val height = camera.viewportHeight * camera.zoom
    val worldBody = world.worldBody
    val worldOffsetX = worldBody.worldOffsetX * Block.BLOCK_SIZE
    val worldOffsetY = worldBody.worldOffsetY * Block.BLOCK_SIZE
    val w = width * abs(camera.up.y) + height * abs(camera.up.x)
    val h = height * abs(camera.up.y) + width * abs(camera.up.x)
    viewBound[camera.position.x - worldOffsetX - w / 2, camera.position.y - worldOffsetY - h / 2, w] = h
    chunksInView.horizontalStart = MathUtils.floor(viewBound.x / Chunk.CHUNK_TEXTURE_SIZE) - WorldRender.CHUNKS_IN_VIEW_HORIZONTAL_PHYSICS
    chunksInView.horizontalEnd =
      (MathUtils.floor((viewBound.x + viewBound.width + Chunk.CHUNK_TEXTURE_SIZE) / Chunk.CHUNK_TEXTURE_SIZE) + WorldRender.CHUNKS_IN_VIEW_HORIZONTAL_PHYSICS)
    chunksInView.verticalStart = MathUtils.floor(viewBound.y / Chunk.CHUNK_TEXTURE_SIZE) - WorldRender.CHUNKS_IN_VIEW_PADDING_RENDER
    chunksInView.verticalEnd =
      (MathUtils.floor((viewBound.y + viewBound.height + Chunk.CHUNK_TEXTURE_SIZE) / Chunk.CHUNK_TEXTURE_SIZE) + WorldRender.CHUNKS_IN_VIEW_PADDING_RENDER)
    if (abs(lastZoom - camera.zoom) > WorldRender.SKYLIGHT_ZOOM_THRESHOLD) {
      lastZoom = camera.zoom
    }
  }

  override fun resize(width: Int, height: Int) {
    val old = camera.position.cpy()
    camera.setToOrtho(false, width.toFloat(), height.toFloat())
    camera.position.set(old)
    update()
  }

  override fun dispose() {
    batch.dispose()
    chunkRenderer.dispose()
    box2DDebugRenderer.dispose()
    chunkDebugRenderer.dispose()
  }

  override fun isOutOfView(chunk: Chunk): Boolean = chunksInView.isOutOfView(chunk.chunkX, chunk.chunkY)
  override fun isOutOfView(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean = chunksInView.isOutOfView(chunkX, chunkY)
  override fun isInView(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean = chunksInView.isInView(chunkX, chunkY)

  override val chunkLocationsInView get() = chunksInView.iterator()
}
