package no.elg.infiniteBootleg.world.box2d

import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody
import com.badlogic.gdx.physics.box2d.ChainShape
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.utils.LongMap
import no.elg.infiniteBootleg.CheckableDisposable
import no.elg.infiniteBootleg.api.Updatable
import no.elg.infiniteBootleg.util.CoordUtil
import no.elg.infiniteBootleg.world.BOX2D_LOCK
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE
import no.elg.infiniteBootleg.world.World
import javax.annotation.concurrent.GuardedBy

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
    it.position.set(chunk.chunkX * CHUNK_SIZE.toFloat(), chunk.chunkY * CHUNK_SIZE.toFloat())
    it.fixedRotation = true
    it.type = StaticBody
  }

  /**
   * Update the box2d fixture of this chunk
   *
   * @param recalculateNeighbors
   * If the neighbors also should be updated
   */
  override fun update() {
    chunk.world.worldBody.updateChunk(this)
  }

  @Synchronized
  fun shouldCreateBody(): Boolean {
    if (isDisposed) {
      return false
    }
    if (chunk.isAllAir) {
      box2dBody = null
      return false
    }
    return true
  }

  /**
   * Must be called under [BOX2D_LOCK]
   *
   * @see World.postBox2dRunnable
   * @see WorldBody.postBox2dRunnable
   */
  @GuardedBy("BOX2D_LOCK")
  fun onBodyCreated(tmpBody: Body) {
    for (localX in 0 until CHUNK_SIZE) {
      for (localY in 0 until CHUNK_SIZE) {
        val block = chunk.getRawBlock(localX, localY)
        if (block == null || !block.material.isSolid) {
          continue
        }

        addBlock(block)
      }
    }

    // if this got disposed while creating the new chunk fixture, this is the easiest cleanup solution
    synchronized(this) {
      if (isDisposed) {
        box2dBody = null
        chunk.world.worldBody.destroyBody(tmpBody)
      } else {
        box2dBody = tmpBody
      }
    }
  }

  fun removeBlock(block: Block) {
    chunk.world.postBox2dRunnable {
      val fixture: Fixture? = fixtureMap.get(CoordUtil.compactLoc(block.localX, block.localY))
      fixture?.filterData = Filters.NON_INTERACTIVE__GROUND_FILTER
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

      val compactLoc = CoordUtil.compactLoc(localX, localY)
      val cacheFix: Fixture? = fixtureMap.get(compactLoc)
      val fix: Fixture = if (cacheFix != null) {
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
        val newFix = body.createFixture(chainShape, 0f)
        fixtureMap.put(compactLoc, newFix)

        chainShape.dispose()
        newFix
      }

      val material = block.material
      fix.filterData = when {
        material.isBlocksLight && material.isSolid -> Filters.EN_GR_LI__GROUND_FILTER
//        material.isBlocksLight -> Filters.GR_LI__GROUND_FILTER
        material.isSolid -> Filters.EN_GR__GROUND_FILTER
        else -> Filters.GR__GROUND_FILTER
      }
    }
  }

  @Synchronized
  override fun dispose() {
    if (isDisposed) return
    disposed = true
    box2dBody = null
  }

  fun hasBody(): Boolean = box2dBody != null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ChunkBody) return false
    if (chunk != other.chunk) return false
    return true
  }

  override fun hashCode(): Int {
    return chunk.hashCode()
  }
}
