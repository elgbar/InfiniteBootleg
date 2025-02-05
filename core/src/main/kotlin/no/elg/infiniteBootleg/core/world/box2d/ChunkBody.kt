package no.elg.infiniteBootleg.core.world.box2d

import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody
import com.badlogic.gdx.physics.box2d.ChainShape
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.utils.LongMap
import com.google.errorprone.annotations.concurrent.GuardedBy
import no.elg.infiniteBootleg.core.api.Updatable
import no.elg.infiniteBootleg.core.util.CheckableDisposable
import no.elg.infiniteBootleg.core.util.compactLoc
import no.elg.infiniteBootleg.core.util.isAir
import no.elg.infiniteBootleg.core.util.isMarkerBlock
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.compactWorldLoc
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.ecs.components.PhysicsEventQueueComponent.Companion.queuePhysicsEvent
import no.elg.infiniteBootleg.core.world.ecs.components.events.PhysicsEvent
import no.elg.infiniteBootleg.core.world.world.World

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
    for (localX in 0 until Chunk.Companion.CHUNK_SIZE) {
      for (localY in 0 until Chunk.Companion.CHUNK_SIZE) {
        val block = chunk.getRawBlock(localX, localY)
        if (block.isAir()) {
          continue
        }
        addBlock(block)
      }
    }

    // if this got disposed while creating the new chunk fixture, this is the easiest cleanup solution
    if (isDisposed) {
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

  fun addBlock(block: Block, box2dBody: Body? = null) {
    val worldBody = chunk.world.worldBody
    chunk.world.postBox2dRunnable {
      val body = box2dBody ?: this.box2dBody
      if (body == null) {
        worldBody.updateChunk(this@ChunkBody)
        return@postBox2dRunnable
      }
      val localX = block.localX
      val localY = block.localY

      val compactLoc = compactLoc(localX, localY)
      val cacheFix: Fixture? = fixtureMap.get(compactLoc)
      val fixture: Fixture = if (cacheFix != null) {
        cacheFix
      } else {
        val chainShape = ChainShape()
        chainShape.createLoop(
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
        body.createFixture(chainShape, 0f).also {
          fixtureMap.put(compactLoc, it)
          chainShape.dispose()
        }
      }

      val material = block.material
      fixture.userData = block
//      chunk.world.engine.queuePhysicsEvent(PhysicsEvent.BlockChangedEvent(fixture, material))
      fixture.filterData = when {
        block.isMarkerBlock() -> Filters.NON_INTERACTIVE__GROUND_FILTER
        material.isCollidable -> Filters.GR_FB_EN__GROUND_FILTER
        else -> Filters.GR_FB__GROUND_FILTER
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

  override fun hashCode(): Int {
    return chunk.hashCode()
  }
}
