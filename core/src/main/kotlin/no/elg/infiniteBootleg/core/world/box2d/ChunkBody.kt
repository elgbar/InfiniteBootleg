package no.elg.infiniteBootleg.core.world.box2d

import com.badlogic.gdx.box2d.Box2d
import com.badlogic.gdx.box2d.enums.b2BodyType
import com.badlogic.gdx.box2d.structs.b2BodyDef
import com.badlogic.gdx.box2d.structs.b2BodyId
import com.badlogic.gdx.box2d.structs.b2ShapeId
import com.google.errorprone.annotations.concurrent.GuardedBy
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.api.Updatable
import no.elg.infiniteBootleg.core.events.api.ThreadType
import no.elg.infiniteBootleg.core.util.CheckableDisposable
import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.util.isLazyInitialized
import no.elg.infiniteBootleg.core.util.isMarkerBlock
import no.elg.infiniteBootleg.core.util.isNotAir
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.box2d.extensions.createPolygonShape
import no.elg.infiniteBootleg.core.world.box2d.extensions.dispose
import no.elg.infiniteBootleg.core.world.box2d.extensions.makeB2Vec2
import no.elg.infiniteBootleg.core.world.box2d.extensions.userData
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.chunks.Chunk.Companion.CHUNK_SIZE_F
import no.elg.infiniteBootleg.core.world.world.World
import java.util.Arrays

private val logger = KotlinLogging.logger {}

/**
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

  private val chunkBodyLock = Any()

  /**
   * The actual box2d body of the chunk.
   *
   * The setter is locked under [bodyLock], the old body will automatically be destroyed.
   *
   * The getter is **not** locked under [bodyLock]
   */
  @field:Volatile
  private var box2dBody: b2BodyId? = null
    set(value) {
      val oldBody: b2BodyId?
      synchronized(chunkBodyLock) {
        oldBody = field
        field = value
        if (::chunkShapes.isLazyInitialized()) {
          chunkShapes.forEach { it?.userData = null }
          Arrays.fill(chunkShapes, null)
        }
      }
      if (oldBody != null) {
        // We should now be fine to destroy the old body
        chunk.world.worldBody.destroyBody(oldBody)
      }
    }

  @field:Volatile
  private var disposed = false

  override val isDisposed: Boolean
    get() = disposed

  /**calculate the shape of the chunk (box2d)*/
  val bodyDef: b2BodyDef = Box2d.b2DefaultBodyDef().apply {
//    position.set(chunk.chunkX * CHUNK_SIZE_F, chunk.chunkY * CHUNK_SIZE_F)
    position = makeB2Vec2(chunk.chunkX * CHUNK_SIZE_F + 0.5f, chunk.chunkY * CHUNK_SIZE_F + 0.5f)
    fixedRotation(true)
    type(b2BodyType.b2_staticBody)
  }

  /**
   * Update the box2d fixture of this chunk
   */
  override fun update() {
    chunk.world.worldBody.updateChunk(this)
  }

  @GuardedBy("BOX2D_LOCK")
  fun shouldCreateBody(): Boolean {
    if (isDisposed) {
      return false
    }
    if (chunk.isAllAir) {
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
  @GuardedBy("BOX2D_LOCK")
  fun onBodyCreated(tmpBody: b2BodyId) {
    val blocks = chunk.asSequence().filterNotNull().filter(Block::isNotAir)
    tmpBody.userData = this
    addBlocks(blocks, tmpBody)

    // if this got disposed while creating the new chunk fixture, this is the easiest cleanup solution
    if (isDisposed) {
      logger.debug { "Chunk body was disposed while creating the body, disposing the new body" }
      box2dBody = null
      chunk.world.worldBody.destroyBody(tmpBody)
    } else {
      box2dBody = tmpBody
    }
  }

  fun removeBlock(block: Block) {
    val world = chunk.world
    world.postBox2dRunnable {
      chunkShapes[shapeIndex(block.localX, block.localY)]?.dispose()
      chunkShapes[shapeIndex(block.localX, block.localY)] = null
    }
  }

  private fun addBlockNow(block: Block, bodyId: b2BodyId) {
    val filter = when {
      block.isMarkerBlock() -> Filters.NON_INTERACTIVE__GROUND_FILTER
      block.material.isCollidable -> Filters.GR_FB_EN__GROUND_FILTER
      else -> Filters.GR_FB__GROUND_FILTER
    }

    val chainShape = Box2d.b2DefaultShapeDef().also { def ->
      def.filter = filter
    }
    val polygon = Box2d.b2MakeOffsetBox(0.5f, 0.5f, makeB2Vec2(block.localX, block.localY), NO_ROTATION)
    val shapeId = bodyId.createPolygonShape(chainShape, polygon, block)
    chunkShapes[shapeIndex(block.localX, block.localY)]?.dispose()
    chunkShapes[shapeIndex(block.localX, block.localY)] = shapeId
  }

  fun addBlocks(blocks: Sequence<Block>, box2dBody: b2BodyId? = null) =
    ThreadType.PHYSICS.launchOrRun {
      val body = box2dBody ?: this.box2dBody
      if (body == null) {
        update()
      } else {
        for (block in blocks) {
          addBlockNow(block, body)
        }
      }
    }

  fun addBlock(block: Block, box2dBody: b2BodyId? = null) {
    ThreadType.PHYSICS.launchOrRun {
      val body = box2dBody ?: this.box2dBody
      if (body == null) {
        update()
      } else {
        addBlockNow(block, body)
      }
    }
  }

  override fun dispose() {
    if (isDisposed) return
    disposed = true
    box2dBody = null
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
