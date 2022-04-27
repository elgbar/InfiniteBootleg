package no.elg.infiniteBootleg.world.box2d

import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody
import com.badlogic.gdx.physics.box2d.EdgeShape
import ktx.collections.GdxLongArray
import no.elg.infiniteBootleg.CheckableDisposable
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Updatable
import no.elg.infiniteBootleg.util.CoordUtil
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.Chunk.CHUNK_SIZE
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.Direction.EAST
import no.elg.infiniteBootleg.world.Direction.NORTH
import no.elg.infiniteBootleg.world.Direction.SOUTH
import no.elg.infiniteBootleg.world.Direction.WEST
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.render.WorldRender.BOX2D_LOCK

/**
 * @author Elg
 */
class ChunkBody(private val chunk: Chunk) : Updatable, CheckableDisposable {

  private val edgeShape = EdgeShape()

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
      // Use edgeShape as a lock.
      // We could create a new object as a semaphore, but why bother when we already have one.
      synchronized(edgeShape) {
        oldBody = field
        field = value
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
   */
  fun onBodyCreated(tmpBody: Body) {
    val edges = GdxLongArray(false, 1 + CHUNK_SIZE * CHUNK_SIZE)
    for (localX in 0 until CHUNK_SIZE) {
      for (localY in 0 until CHUNK_SIZE) {

        val block = chunk.getRawBlock(localX, localY)
        if (block == null || !block.material.isSolid) {
          continue
        }

        val worldX = CoordUtil.chunkToWorld(chunk.chunkX, localX)
        val worldY = CoordUtil.chunkToWorld(chunk.chunkY, localY)

        for ((dir, edgeDelta) in EDGE_DEF) {

          // Create a unique id for each edge
          val edgeId = CoordUtil.compactShort(localX.toShort(), dir.dx.toShort(), localY.toShort(), dir.dy.toShort())
          if (edgeId in edges ||
            CoordUtil.isInnerEdgeOfChunk(localX, localY) &&
            !CoordUtil.isInsideChunk(localX + dir.dx, localY + dir.dy) &&
            !chunk.world.isChunkLoaded(CoordUtil.worldToChunk(worldX + dir.dx), CoordUtil.worldToChunk(worldY + dir.dy))
          ) {
            continue
          }
          edgeShape.set(
            localX + edgeDelta[0],
            localY + edgeDelta[1],
            localX + edgeDelta[2],
            localY + edgeDelta[3]
          )
          edges.add(edgeId)

          val fix = tmpBody.createFixture(edgeShape, 0f)

          if (!block.material.blocksLight()) {
            fix.filterData = World.TRANSPARENT_BLOCK_ENTITY_FILTER
          }
        }
      }
    }

    // if this got disposed while creating the new chunk fixture, this is the easiest cleanup solution
    synchronized(this) {
      if (isDisposed) {
        box2dBody = null
        chunk.world.worldBody.destroyBody(tmpBody)
      } else {
        box2dBody = tmpBody
        Main.inst().scheduler.executeAsync {
          chunk.world.updateLights()
          chunk.world.render.update()
        }
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

  companion object {

    /**
     * represent the direction to look and if no solid block there how to create a fixture at that location (ie
     * two relative vectors)
     * the value of the pair is as follows `dxStart`, `dyStart`, `dxEnd`, `dyEnd`
     * this can be visually represented with a cube:
     *
     * ```
     * (0,1)---(1,1)
     *  |         |
     *  |         |
     *  |         |
     * (0,0)---(1,0)
     * ```
     *
     * * Where `d` stands for delta
     * * `x`/`y` is if this is the `x` or `y` component of the coordinate
     * * `end`/`start` is if this is the start or end vector
     */
    private val EDGE_DEF: Array<Pair<Direction, FloatArray>> = arrayOf(
      NORTH to floatArrayOf(0f, 1f, 1f, 1f),
      EAST to floatArrayOf(1f, 0f, 1f, 1f),
      SOUTH to floatArrayOf(0f, 0f, 1f, 0f),
      WEST to floatArrayOf(0f, 0f, 0f, 1f)
    )
  }
}
