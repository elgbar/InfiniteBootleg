package no.elg.infiniteBootleg.client.world.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color.WHITE_FLOAT_BITS
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.badlogic.gdx.utils.Disposable
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.client.inventory.ui.SlotSource.Companion.DRAG_ICON_SIZE
import no.elg.infiniteBootleg.client.inventory.ui.createContainerActor
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.screens.StageScreen
import no.elg.infiniteBootleg.client.util.IBVisWindow
import no.elg.infiniteBootleg.client.world.managers.interfaces.InterfaceManager
import no.elg.infiniteBootleg.client.world.render.debug.AirBlockRenderer
import no.elg.infiniteBootleg.client.world.render.debug.BlockLightDebugRenderer
import no.elg.infiniteBootleg.client.world.render.debug.ClosestBlockToChunkDebugRenderer
import no.elg.infiniteBootleg.client.world.render.debug.DebugChunkAddedToChunkRenderer
import no.elg.infiniteBootleg.client.world.render.debug.DebugChunkRenderer
import no.elg.infiniteBootleg.client.world.render.debug.EntityMarkerBlockRenderer
import no.elg.infiniteBootleg.client.world.render.debug.GroundedTouchingArea
import no.elg.infiniteBootleg.client.world.render.debug.LeafDecayDebugRenderer
import no.elg.infiniteBootleg.client.world.render.debug.TopBlockChangeRenderer
import no.elg.infiniteBootleg.client.world.render.debug.TopBlockChunkColumnRenderer
import no.elg.infiniteBootleg.client.world.world.ClientWorld
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.api.Renderer
import no.elg.infiniteBootleg.core.api.render.OverlayRenderer.Companion.isInactive
import no.elg.infiniteBootleg.core.inventory.container.InterfaceId
import no.elg.infiniteBootleg.core.inventory.container.OwnedContainer
import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.util.WorldCompactLoc
import no.elg.infiniteBootleg.core.util.WorldCoordNumber
import no.elg.infiniteBootleg.core.util.decompactLocX
import no.elg.infiniteBootleg.core.util.decompactLocY
import no.elg.infiniteBootleg.core.util.safeUse
import no.elg.infiniteBootleg.core.world.BOX2D_LOCK
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.render.ChunksInView.Companion.chunkColumnsInView
import no.elg.infiniteBootleg.core.world.render.ChunksInView.Companion.sequence
import no.elg.infiniteBootleg.core.world.render.WorldRender
import kotlin.math.abs

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
class ClientWorldRender(override val world: ClientWorld) : WorldRender {

  private val viewBound: Rectangle = Rectangle()
  private val box2dDebugM4 = Matrix4()
  private val renderers: Array<Renderer> = arrayOf(
    CachedChunkRenderer(this),
    AirBlockRenderer(this),
    HoveringBlockRenderer(this),
    TopBlockChunkColumnRenderer(this),
    EntityRenderer(this),
    DebugChunkRenderer(this),
    DebugChunkAddedToChunkRenderer(this),
    BlockLightDebugRenderer(this),
    TopBlockChangeRenderer(this),
    LeafDecayDebugRenderer(this),
    FuturePositionRenderer(this),
    EntityMarkerBlockRenderer(this),
    ClosestBlockToChunkDebugRenderer(this),
    GroundedTouchingArea(this)
  )

  private var lastZoom = 0f

  private val maybeStage: Stage?
    get() {
      return (ClientMain.inst().screen as? StageScreen)?.stage.also { stage ->
        if (stage == null) {
          logger.warn { "Could not get stage from screen ${ClientMain.inst().screen}" }
        }
      }
    }

  val chunksInView: ClientChunksInView = ClientChunksInView()
  val batch: SpriteBatch = SpriteBatch().also {
    it.enableBlending()
  }
  val camera: OrthographicCamera = OrthographicCamera().also {
    it.setToOrtho(false)
    it.zoom = 1f
    it.position.x = 0f
    it.position.y = 0f
  }
  val chunkRenderer: ChunkRenderer = ChunkRenderer(this)
  val box2DDebugRenderer: Box2DDebugRenderer by lazy { Box2DDebugRenderer(true, false, false, false, true, false) }

  private val dad: DragAndDrop = DragAndDrop().apply {
    setButton(-1)
    setDragActorPosition(DRAG_ICON_SIZE / 2f, -DRAG_ICON_SIZE / 2f) // center on drag image
    dragTime = 50 // ms
  }

  val interfaceManager = InterfaceManager(world)

  fun openInterface(interfaceId: InterfaceId, createIfMissing: () -> IBVisWindow? = { null }) {
    interfaceManager.openInterface(interfaceId, maybeStage ?: return, createIfMissing)
  }

  fun closeInterface(interfaceId: InterfaceId) {
    interfaceManager.closeInterface(interfaceId)
  }

  fun toggleInterface(interfaceId: InterfaceId, createIfMissing: () -> IBVisWindow? = { null }) {
    interfaceManager.toggleInterface(interfaceId, maybeStage ?: return, createIfMissing)
  }

  fun createContainerActor(ownedContainer: OwnedContainer) = world.createContainerActor(ownedContainer, dad, batch)

  fun lookAt(loc: WorldCompactLoc) = lookAt(loc.decompactLocX(), loc.decompactLocY())
  fun lookAt(worldX: WorldCoordNumber, worldY: WorldCoordNumber) {
    camera.position.set(worldX.toFloat() * Block.Companion.BLOCK_TEXTURE_SIZE, worldY.toFloat() * Block.Companion.BLOCK_TEXTURE_SIZE, 0f)
    update()
  }

  override fun render() {
    chunkRenderer.renderMultiple()
    batch.safeUse(camera.combined) {
      for (renderer in renderers) {
        if (renderer.isInactive) continue

        if (!batch.projectionMatrix.values.contentEquals(camera.combined.values)) {
          batch.projectionMatrix = camera.combined
        }

        if (batch.isDrawing) {
          // Flush to make sure everything is drawn before the next renderer
          batch.flush()
        } else {
          batch.begin()
        }

        // reset between renderers
        batch.packedColor = WHITE_FLOAT_BITS
        Gdx.gl.glEnable(GL30.GL_BLEND)

        renderer.render()

        if (!batch.isDrawing) {
          logger.warn { "Batch is no longer drawing after ${renderer::class}" }
        }
      }
    }
    if (Settings.renderBox2dDebug) {
      synchronized(BOX2D_LOCK) {
        box2DDebugRenderer.render(world.worldBody.box2dWorld, box2dDebugM4)
      }
    }
  }

  override fun update() {
    camera.update()
    box2dDebugM4.set(camera.combined).scl(Block.Companion.BLOCK_TEXTURE_SIZE_F)
    val width = camera.viewportWidth * camera.zoom
    val height = camera.viewportHeight * camera.zoom
    val w = width * abs(camera.up.y) + height * abs(camera.up.x)
    val h = height * abs(camera.up.y) + width * abs(camera.up.x)
    viewBound[camera.position.x - w / 2, camera.position.y - h / 2, w] = h
    chunksInView.horizontalStart = MathUtils.floor(viewBound.x / Chunk.Companion.CHUNK_TEXTURE_SIZE) - WorldRender.CHUNKS_IN_VIEW_HORIZONTAL_PHYSICS
    chunksInView.horizontalEnd =
      (MathUtils.floor((viewBound.x + viewBound.width + Chunk.Companion.CHUNK_TEXTURE_SIZE) / Chunk.Companion.CHUNK_TEXTURE_SIZE) + WorldRender.CHUNKS_IN_VIEW_HORIZONTAL_PHYSICS)
    chunksInView.verticalStart = MathUtils.floor(viewBound.y / Chunk.Companion.CHUNK_TEXTURE_SIZE) - WorldRender.CHUNKS_IN_VIEW_PADDING_RENDER
    chunksInView.verticalEnd =
      (MathUtils.floor((viewBound.y + viewBound.height + Chunk.Companion.CHUNK_TEXTURE_SIZE) / Chunk.Companion.CHUNK_TEXTURE_SIZE) + WorldRender.CHUNKS_IN_VIEW_PADDING_RENDER)
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
    for (renderer in renderers.filterIsInstance<Disposable>()) {
      renderer.dispose()
    }
  }

  override fun isOutOfView(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean = chunksInView.isOutOfView(chunkX, chunkY)
  override fun isInView(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean = chunksInView.isInView(chunkX, chunkY)

  override val chunkLocationsInView get() = chunksInView.sequence()
  override val chunkColumnsInView get() = chunksInView.chunkColumnsInView()
}
