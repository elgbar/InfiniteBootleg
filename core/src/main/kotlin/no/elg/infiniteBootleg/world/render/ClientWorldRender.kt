package no.elg.infiniteBootleg.world.render

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
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.api.Renderer
import no.elg.infiniteBootleg.api.render.OverlayRenderer.Companion.isInactive
import no.elg.infiniteBootleg.inventory.container.InterfaceId
import no.elg.infiniteBootleg.inventory.container.OwnedContainer
import no.elg.infiniteBootleg.inventory.ui.createContainerActor
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.screens.StageScreen
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.IBVisWindow
import no.elg.infiniteBootleg.util.WorldCompactLoc
import no.elg.infiniteBootleg.util.WorldCoordNumber
import no.elg.infiniteBootleg.util.decompactLocX
import no.elg.infiniteBootleg.util.decompactLocY
import no.elg.infiniteBootleg.util.safeUse
import no.elg.infiniteBootleg.world.BOX2D_LOCK
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.managers.interfaces.InterfaceManager
import no.elg.infiniteBootleg.world.render.ChunksInView.Companion.chunkColumnsInView
import no.elg.infiniteBootleg.world.render.ChunksInView.Companion.iterator
import no.elg.infiniteBootleg.world.render.debug.AirBlockRenderer
import no.elg.infiniteBootleg.world.render.debug.BlockLightDebugRenderer
import no.elg.infiniteBootleg.world.render.debug.DebugChunkRenderer
import no.elg.infiniteBootleg.world.render.debug.EntityMarkerBlockRenderer
import no.elg.infiniteBootleg.world.render.debug.TopBlockChangeRenderer
import no.elg.infiniteBootleg.world.world.ClientWorld
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
    EntityRenderer(this),
    DebugChunkRenderer(this),
    BlockLightDebugRenderer(this),
    TopBlockChangeRenderer(this),
    FuturePositionRenderer(this),
    EntityMarkerBlockRenderer(this)
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
  val batch: SpriteBatch = SpriteBatch()
  val camera: OrthographicCamera = OrthographicCamera().also {
    it.setToOrtho(false)
    it.zoom = 1f
    it.position.x = 0f
    it.position.y = 0f
  }
  val chunkRenderer: ChunkRenderer = ChunkRenderer(this)
  val box2DDebugRenderer: Box2DDebugRenderer by lazy { Box2DDebugRenderer(true, false, false, false, true, false) }

  private val dad: DragAndDrop = DragAndDrop()

  val interfaceManager = InterfaceManager()

  fun openInterface(interfaceId: InterfaceId, createIfMissing: () -> IBVisWindow? = { null }) {
    interfaceManager.openInterface(interfaceId, maybeStage ?: return, createIfMissing)
  }

  fun closeInterface(interfaceId: InterfaceId) {
    interfaceManager.closeInterface(interfaceId)
  }

  fun toggleInterface(interfaceId: InterfaceId, createIfMissing: () -> IBVisWindow? = { null }) {
    interfaceManager.toggleInterface(interfaceId, maybeStage ?: return, createIfMissing)
  }

//  /**
//   * Register an IContainer with the UI, if one is already registered return a saved instance
//   *
//   * @return The ContainerActor for the given container
//   */
//  fun getContainerActor(ownedContainer: OwnedContainer): CompletableFuture<WindowAndStage>? {
//    val stage = maybeStage ?: return null
//    val interfaceId = ownedContainer.owner.toInterfaceId()
//
//    val storedActor = interfaceManager.getInterface(interfaceId)
//    return if (storedActor == null) {
//      Main.inst().scheduler.executeSync {
//        val actor = interfaceManager.getInterface(interfaceId) ?: createContainerActor(ownedContainer)
//        WindowAndStage(actor, stage)
//      }
//    } else {
//      CompletableFuture.completedFuture(WindowAndStage(storedActor, stage))
//    }
//  }

  fun createContainerActor(ownedContainer: OwnedContainer) = world.createContainerActor(ownedContainer, dad, batch)

  fun lookAt(loc: WorldCompactLoc) = lookAt(loc.decompactLocX(), loc.decompactLocY())
  fun lookAt(worldX: WorldCoordNumber, worldY: WorldCoordNumber) {
    camera.position.set(worldX.toFloat() * Block.BLOCK_SIZE, worldY.toFloat() * Block.BLOCK_SIZE, 0f)
    update()
  }

  override fun render() {
    batch.projectionMatrix = camera.combined
    chunkRenderer.renderMultiple()
    batch.safeUse {
      for (renderer in renderers) {
        if (renderer.isInactive) continue
        if (!batch.isDrawing) {
          logger.warn { "Batch was not drawing when rendering, starting batch" }
          batch.begin()
        }
        batch.projectionMatrix = camera.combined
        renderer.render()
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
    box2dDebugM4.set(camera.combined).scl(Block.BLOCK_SIZE.toFloat())
    val width = camera.viewportWidth * camera.zoom
    val height = camera.viewportHeight * camera.zoom
    val w = width * abs(camera.up.y) + height * abs(camera.up.x)
    val h = height * abs(camera.up.y) + width * abs(camera.up.x)
    viewBound[camera.position.x - w / 2, camera.position.y - h / 2, w] = h
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
    for (renderer in renderers.filterIsInstance<Disposable>()) {
      renderer.dispose()
    }
  }

  override fun isOutOfView(chunk: Chunk): Boolean = chunksInView.isOutOfView(chunk.chunkX, chunk.chunkY)
  override fun isOutOfView(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean = chunksInView.isOutOfView(chunkX, chunkY)
  override fun isInView(chunkX: ChunkCoord, chunkY: ChunkCoord): Boolean = chunksInView.isInView(chunkX, chunkY)

  override val chunkLocationsInView get() = chunksInView.iterator()
  override val chunkColumnsInView get() = chunksInView.chunkColumnsInView()
}
