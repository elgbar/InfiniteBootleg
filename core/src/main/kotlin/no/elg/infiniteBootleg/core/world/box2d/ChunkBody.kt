package no.elg.infiniteBootleg.core.world.box2d

import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody
import com.badlogic.gdx.physics.box2d.ChainShape
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.utils.LongMap
import com.google.errorprone.annotations.concurrent.GuardedBy
import io.github.oshai.kotlinlogging.KotlinLogging
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import no.elg.infiniteBootleg.core.api.Updatable
import no.elg.infiniteBootleg.core.util.CheckableDisposable
import no.elg.infiniteBootleg.core.util.LocalCompactLoc
import no.elg.infiniteBootleg.core.util.compactLoc
import no.elg.infiniteBootleg.core.util.decompactLocX
import no.elg.infiniteBootleg.core.util.decompactLocY
import no.elg.infiniteBootleg.core.util.isMarkerBlock
import no.elg.infiniteBootleg.core.util.isNotAir
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.compactWorldLoc
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.ecs.components.PhysicsEventQueueComponent.Companion.queuePhysicsEvent
import no.elg.infiniteBootleg.core.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.core.world.world.World

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
class ChunkBody(private val chunk: Chunk) : Updatable, CheckableDisposable {

  private val fixtureMap = LongMap<Fixture>()

  private val chunkBodyLock = Any()

  /**
   * The actual box2d body of the chunk.
   *
   * The setter is locked under [bodyLock], the old body will automatically be destroyed.
   *
   * The getter is **not** locked under [bodyLock]
   */
  @field:Volatile
  private var box2dBody: Body? = null
    set(value) {
      val oldBody: Body?
      synchronized(chunkBodyLock) {
        oldBody = field
        field = value
        fixtureMap.clear()
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
  val bodyDef = BodyDef().also {
    it.position.set(chunk.chunkX * Chunk.Companion.CHUNK_SIZE.toFloat(), chunk.chunkY * Chunk.Companion.CHUNK_SIZE.toFloat())
    it.fixedRotation = true
    it.type = StaticBody
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
  fun onBodyCreated(tmpBody: Body) {
    val blocks = chunk.asSequence().filterNotNull().filter(Block::isNotAir)
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
      fixtureMap.get(compactLoc(block.localX, block.localY))?.also { fixture ->
        fixture.filterData = Filters.NON_INTERACTIVE__GROUND_FILTER
        fixture.userData = null
        world.engine.queuePhysicsEvent(PhysicsEvent.BlockRemovedEvent(fixture, block.compactWorldLoc))
      }
    }
  }

  private fun getFixture(body: Body, localX: Int, localY: Int): Fixture {
    val compactLoc = compactLoc(localX, localY)
    val cacheFix: Fixture? = fixtureMap.get(compactLoc)
    return cacheFix ?: body.createFixture(getChainShape(compactLoc), 0f).also {
      fixtureMap.put(compactLoc, it)
    }
  }

  private fun addBlockNow(block: Block, body: Body) {
    val fixture: Fixture = getFixture(body, block.localX, block.localY)
//      chunk.world.engine.queuePhysicsEvent(PhysicsEvent.BlockChangedEvent(fixture, material))
    fixture.userData = block
    fixture.filterData = when {
      block.isMarkerBlock() -> Filters.NON_INTERACTIVE__GROUND_FILTER
      block.material.isCollidable -> Filters.GR_FB_EN__GROUND_FILTER
      else -> Filters.GR_FB__GROUND_FILTER
    }
  }

  fun addBlocks(blocks: Sequence<Block>, box2dBody: Body? = null) {
    chunk.world.postBox2dRunnable {
      val body = box2dBody ?: this.box2dBody
      if (body == null) {
        update()
        return@postBox2dRunnable
      }
      for (block in blocks) {
        addBlockNow(block, body)
      }
    }
  }

  fun addBlock(block: Block, box2dBody: Body? = null) {
    chunk.world.postBox2dRunnable {
      val body = box2dBody ?: this.box2dBody
      if (body == null) {
        update()
        return@postBox2dRunnable
      }
      addBlockNow(block, body)
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

  override fun hashCode(): Int {
    return chunk.hashCode()
  }

  companion object {
    private val chainCache: Long2ObjectMap<ChainShape> = Long2ObjectOpenHashMap()
    private val mappingFunction: java.util.function.LongFunction<ChainShape> = java.util.function.LongFunction { v ->
      ChainShape().also {
        val localX = v.decompactLocX()
        val localY = v.decompactLocY()
        it.createLoop(
          floatArrayOf(
            localX + 0f,
            localY + 0f,

            localX + 0f,
            localY + 1f,

            localX + 1f,
            localY + 1f,

            localX + 1f,
            localY + 0f
          )
        )
      }
    }

    private fun getChainShape(compactLoc: LocalCompactLoc): ChainShape = chainCache.computeIfAbsent(compactLoc, mappingFunction)
  }
}
