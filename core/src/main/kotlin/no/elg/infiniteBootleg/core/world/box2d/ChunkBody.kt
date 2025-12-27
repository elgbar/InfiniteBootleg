package no.elg.infiniteBootleg.core.world.box2d

import com.badlogic.gdx.box2d.Box2d
import com.badlogic.gdx.box2d.enums.b2BodyType
import com.badlogic.gdx.box2d.structs.b2BodyDef
import com.badlogic.gdx.box2d.structs.b2BodyId
import com.badlogic.gdx.box2d.structs.b2ShapeId
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.api.Updatable
import no.elg.infiniteBootleg.core.events.api.ThreadType
import no.elg.infiniteBootleg.core.util.CheckableDisposable
import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.util.isLazyInitialized
import no.elg.infiniteBootleg.core.util.isMarkerBlock
import no.elg.infiniteBootleg.core.util.isNotAir
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.compactWorldLoc
import no.elg.infiniteBootleg.core.world.box2d.extensions.createPolygonShape
import no.elg.infiniteBootleg.core.world.box2d.extensions.dispose
import no.elg.infiniteBootleg.core.world.box2d.extensions.makeB2Vec2
import no.elg.infiniteBootleg.core.world.box2d.extensions.userData
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.chunks.Chunk.Companion.CHUNK_SIZE_F
import no.elg.infiniteBootleg.core.world.ecs.components.PhysicsEventQueueComponent.Companion.queuePhysicsEvent
import no.elg.infiniteBootleg.core.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.core.world.world.World
import java.util.Arrays

private val logger = KotlinLogging.logger {}

/**
 * This class will only work from the [ThreadType.PHYSICS] thread.
 * Any public methods will either be scheduled to run on the physics thread or immediately run if already on the correct thread
 *
 * @author Elg
 */
class ChunkBody(val chunk: Chunk) :
  Updatable,
  CheckableDisposable {

  // unsynchronized lazy is ok since this will only be accessed from the physics thread
  // we also want it lazy to not allocate chunk shapes for empty chunks
  private val chunkShapes: Array<b2ShapeId?> by lazy(LazyThreadSafetyMode.NONE) { arrayOfNulls(Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE) }

  @Suppress("NOTHING_TO_INLINE")
  private inline fun shapeIndex(localX: LocalCoord, localY: LocalCoord): Int = localX + localY * Chunk.CHUNK_SIZE

  private fun updateShape(block: Block, shapeId: b2ShapeId?) {
    val shapeIndex = shapeIndex(block.localX, block.localY)
    val old = chunkShapes[shapeIndex]
    if (shapeId == null) {
      block.world.engine.queuePhysicsEvent(PhysicsEvent.BlockRemovedEvent(old, block.compactWorldLoc))
    } else {
      // when needed:
//      chunk.world.engine.queuePhysicsEvent(PhysicsEvent.BlockChangedEvent(fixture, material))
    }
    old?.dispose()
    chunkShapes[shapeIndex] = shapeId
  }

  /**
   * The actual box2d body of the chunk.
   *
   * The setter must be called from the [ThreadType.PHYSICS] thread
   */
  private var box2dBody: b2BodyId? = null
    set(value) {
      ThreadType.requireCorrectThreadType(ThreadType.PHYSICS)
      val oldBody = field
      field = value
      if (::chunkShapes.isLazyInitialized()) {
        chunkShapes.forEach { it?.userData = null }
        Arrays.fill(chunkShapes, null)
      }
      if (oldBody != null) {
        // We should now be fine to destroy the old body
        chunk.world.worldBody.destroyBody(oldBody)
      }
    }
    get() {
      ThreadType.requireCorrectThreadType(ThreadType.PHYSICS)
      return field
    }

  @field:Volatile
  private var disposed = false

  override val isDisposed: Boolean
    get() = disposed

  /**
   * Update the box2d fixture of this chunk
   */
  override fun update() {
    ThreadType.PHYSICS.launchOrRun(chunk.world) {
      tryCreateChunkBodyNow(addingBlock = false)
    }
  }

  /**
   * Try to get or create the chunk body now, if it needs to be created
   *
   * @throws no.elg.infiniteBootleg.core.exceptions.CalledFromWrongThreadTypeException If not called from the [ThreadType.PHYSICS] thread
   *
   */
  private fun tryCreateChunkBodyNow(addingBlock: Boolean): b2BodyId? {
    ThreadType.requireCorrectThreadType(ThreadType.PHYSICS)
    return if (shouldCreateBody(addingBlock)) {
      val bodyDef: b2BodyDef = Box2d.b2DefaultBodyDef().apply {
        position = makeB2Vec2(chunk.chunkX * CHUNK_SIZE_F + 0.5f, chunk.chunkY * CHUNK_SIZE_F + 0.5f)
        fixedRotation(true)
        type(b2BodyType.b2_staticBody)
      }
      chunk.world.worldBody.createBodyNow(bodyDef, this::onBodyCreated)
    } else {
      box2dBody
    }
  }

  private fun shouldCreateBody(addingBlock: Boolean): Boolean {
    if (isDisposed) {
      return false
    }
    if (!addingBlock && chunk.isAllAir) {
      box2dBody = null
      return false
    }
    return box2dBody == null
  }

  /**
   * Must be called under [no.elg.infiniteBootleg.core.world.BOX2D_LOCK]
   *
   * @see World.postBox2dRunnable
   * @see WorldBody.postBox2dRunnable
   */
  private fun onBodyCreated(tmpBody: b2BodyId) {
    val blocks = chunk.asSequence().filterNotNull().filter(Block::isNotAir)
    tmpBody.userData = this

    // if this got disposed while creating the new chunk fixture, this is the easiest cleanup solution
    if (isDisposed) {
      logger.debug { "Chunk body was disposed while creating the body, disposing the new body" }
      box2dBody = null
      chunk.world.worldBody.destroyBody(tmpBody)
    } else {
      box2dBody = tmpBody
      addBlocks(blocks, tmpBody) // must be done after setting the box2dBody as setting it will clear the references
    }
  }

  fun removeBlock(block: Block) {
    require(block.chunk.chunkBody === this) { "Block $block does not belong to this chunk body, $this. it belongs to chunk ${block.chunk.chunkBody}" }
    ThreadType.PHYSICS.launchOrRun(chunk.world) {
      updateShape(block, null)
    }
  }

  private fun addBlockNow(block: Block, bodyId: b2BodyId) {
    val filter = when {
      block.isMarkerBlock() -> Filters.NON_INTERACTIVE__GROUND_FILTER
      block.material.isCollidable -> Filters.GR_FB_EN__GROUND_FILTER
      else -> Filters.GR_FB__GROUND_FILTER
    }

    val shapeDef = Box2d.b2DefaultShapeDef().also { def ->
      def.filter = filter
      def.enableSensorEvents(true)
    }
    val polygon = Box2d.b2MakeOffsetBox(0.5f, 0.5f, makeB2Vec2(block.localX, block.localY), NO_ROTATION)
    val shapeId = bodyId.createPolygonShape(shapeDef, polygon, block)

    updateShape(block, shapeId)
  }

  fun addBlocks(blocks: Sequence<Block>, box2dBody: b2BodyId? = null) =
    ThreadType.PHYSICS.launchOrRun(chunk.world) {
      val body = box2dBody ?: tryCreateChunkBodyNow(addingBlock = true) ?: let {
        logger.warn { "Failed to get the chunk body to add the blocks to!" }
        return@launchOrRun
      }
      for (block in blocks) {
        addBlockNow(block, body)
      }
    }

  fun addBlock(block: Block, box2dBody: b2BodyId? = null) = addBlocks(sequenceOf(block), box2dBody)

  override fun dispose() {
    if (isDisposed) return
    disposed = true
    ThreadType.PHYSICS.launchOrRun(chunk.world) {
      box2dBody = null
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ChunkBody) return false
    return chunk == other.chunk
  }

  override fun hashCode(): Int = chunk.hashCode()

  companion object {
//    private val chainCache: Long2ObjectMap<b2ChainDef> = Long2ObjectOpenHashMap()
//    private val mappingFunction: LongFunction<b2ChainDef> = LongFunction { loc ->
//      Box2d.b2DefaultChainDef().also {
//        val localX = loc.decompactLocX().toFloat()
//        val localY = loc.decompactLocY().toFloat()
//        it.isLoop(true)
//
//        val points = b2Vec2Pointer(b2Vec2().pointer, true, 4)
//        val vec = b2Vec2()
//        points.set(vec.set(localX + 0f, localY + 0f), 0)
//        points.set(vec.set(localX + 0f, localY + 1f), 1)
//        points.set(vec.set(localX + 1f, localY + 1f), 2)
//        points.set(vec.set(localX + 1f, localY + 0f), 3)
//
//        it.points(points)
//        it.count(4)
//      }
// //      Box2d.b2MakeSquare(0.5f)
//    }
//
//    private fun getChainShape(compactLoc: LocalCompactLoc): b2ChainDef = chainCache.computeIfAbsent(compactLoc, mappingFunction)
  }
}
