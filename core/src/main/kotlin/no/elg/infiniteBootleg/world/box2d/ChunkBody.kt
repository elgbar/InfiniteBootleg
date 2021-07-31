package no.elg.infiniteBootleg.world.box2d

import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody
import com.badlogic.gdx.physics.box2d.EdgeShape
import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.util.CoordUtil
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.Direction.EAST
import no.elg.infiniteBootleg.world.Direction.NORTH
import no.elg.infiniteBootleg.world.Direction.SOUTH
import no.elg.infiniteBootleg.world.Direction.WEST
import no.elg.infiniteBootleg.world.Location
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.blocks.StaticLightBlock
import no.elg.infiniteBootleg.world.render.WorldRender.BOX2D_LOCK

/**
 * @author Elg
 */
class ChunkBody(private val chunk: Chunk) : Disposable {

  private var box2dBody: Body? = null

  @field:Volatile
  private var disposed = false

  //make there is only one delayed check for this chunk
  private var unsureFixture = false

  /**calculate the shape of the chunk (box2d)*/
  private val bodyDef = BodyDef().also {
    it.position[chunk.chunkX * Chunk.CHUNK_SIZE.toFloat()] = chunk.chunkY * Chunk.CHUNK_SIZE.toFloat()
    it.fixedRotation = true
    it.type = StaticBody
  }

  /**
   * Update the box2d fixture of this chunk
   *
   * @param recalculateNeighbors
   * If the neighbors also should be updated
   * @param lightsOnly
   */
  fun update(recalculateNeighbors: Boolean, lightsOnly: Boolean) {
    if (lightsOnly) {
      updateLights()
      return
    }
    if (chunk.isAllAir) {
      destroyCurrentBody()
      return
    }

    val tmpBody = synchronized(BOX2D_LOCK) {
      if (disposed) {
        return
      }
      chunk.world.worldBody.createBody(bodyDef)
    }
    val edgeShape = EdgeShape()

    for (localX in 0 until Chunk.CHUNK_SIZE) {
      for (localY in 0 until Chunk.CHUNK_SIZE) {

        val block = chunk.getRawBlock(localX, localY)
        if (block == null || !block.material.isSolid) {
          continue
        }

        val worldX = CoordUtil.chunkToWorld(chunk.chunkX, localX)
        val worldY = CoordUtil.chunkToWorld(chunk.chunkY, localY)

        for ((dir, edgeDelta) in EDGE_DEF) {

          //FIXME only check the chunk if the local coordinates are outside this chunk
          if (!CoordUtil.isInsideChunk(localX + dir.dx, localY + dir.dy) && !chunk.world.isChunkLoaded(
              CoordUtil.worldToChunk(worldX + dir.dx),
              CoordUtil.worldToChunk(worldY + dir.dy)
            )
          ) {
            continue
          }

          val rel = if (CoordUtil.isInsideChunk(localX + dir.dx, localY + dir.dy)) {
            chunk.getRawBlock(localX + dir.dx, localY + dir.dy)
          } else {
            val relChunk = chunk.world.getChunkFromWorld(worldX + dir.dx, worldY + dir.dy) ?: continue
            val relOffsetX = CoordUtil.chunkOffset(worldX + dir.dx)
            val relOffsetY = CoordUtil.chunkOffset(worldY + dir.dy)
            relChunk.blocks[relOffsetX][relOffsetY]
          }

          if (rel == null
            || !rel.material.isSolid
            || dir == NORTH && localY == Chunk.CHUNK_SIZE - 1 //always render top of chunk
            || dir == EAST && localX == Chunk.CHUNK_SIZE - 1 //and the east side
            || dir == WEST && localX == 0 //and the west side
            || (!rel.material.blocksLight() && block.material.blocksLight()) //prevent leaking of light
          ) {

            edgeShape.set(
              localX + edgeDelta[0].toFloat(),
              localY + edgeDelta[1].toFloat(),
              localX + edgeDelta[2].toFloat(),
              localY + edgeDelta[3].toFloat()
            )

            val fix = synchronized(BOX2D_LOCK) {
              tmpBody.createFixture(edgeShape, 0f)
            }
            if (!block.material.blocksLight()) {
              fix.filterData = World.TRANSPARENT_BLOCK_ENTITY_FILTER
            }
          }
        }
      }
    }

    edgeShape.dispose()
    synchronized(BOX2D_LOCK) {
      destroyCurrentBody()
      box2dBody = tmpBody

      //we got disposed while creating the new chunk fixture, this is the easiest cleanup solution
      if (disposed) {
        destroyCurrentBody()
        return
      }
    }
    Main.inst().scheduler.executeAsync { chunk.world.render.update() }
    var potentiallyDirty = false

    //TODO Try to optimize this (ie select what directions to recalculate)
    for (direction in Direction.values()) {
      val relChunk: Location = Location.relative(chunk.chunkX, chunk.chunkY, direction)
      if (chunk.world.isChunkLoaded(relChunk)) {
        if (recalculateNeighbors) {
          Main.inst().scheduler.executeAsync {
            chunk.world.getChunk(relChunk)?.chunkBody?.update(false, !direction.isCardinal)
          }
        }
      } else {
        potentiallyDirty = true
      }
    }
    if (potentiallyDirty) {
      scheduleFixtureReload(true)
    }
    updateLights()
  }

  @Synchronized
  private fun scheduleFixtureReload(initial: Boolean) {
    if (unsureFixture) {
      return
    }
    unsureFixture = true
    Main.inst().scheduler.scheduleAsync({
      synchronized(this@ChunkBody) {
        unsureFixture = false
        if (chunk.isNeighborsLoaded) {
          update(false, false)
        } else {
          scheduleFixtureReload(false)
        }
      }
    }, if (initial) INITIAL_UNSURE_FIXTURE_RELOAD_DELAY else UNSURE_FIXTURE_RELOAD_DELAY)
  }

  private fun updateLights() {
    for (localX in 0 until Chunk.CHUNK_SIZE) {
      for (localY in 0 until Chunk.CHUNK_SIZE) {
        val block = chunk.getRawBlock(localX, localY)
        if (block is StaticLightBlock) {
          block.updateLight()
        }
      }
    }
  }

  private fun destroyCurrentBody() {
    synchronized(BOX2D_LOCK) {
      chunk.world.worldBody.destroyBody(box2dBody)
      box2dBody = null
    }
  }

  override fun dispose() {
    synchronized(BOX2D_LOCK) {
      if (disposed) return
      disposed = true
      destroyCurrentBody()
    }
  }

  companion object {
    const val INITIAL_UNSURE_FIXTURE_RELOAD_DELAY = 10L
    const val UNSURE_FIXTURE_RELOAD_DELAY = 100L

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
     *  |         |
     * (0,0)---(1,0)
     * ```
     *
     * * Where `d` stands for delta
     * * `x`/`y` is if this is the `x` or `y` component of the coordinate
     * * `end`/`start` is if this is the start or end vector
     */
    val EDGE_DEF: Array<Pair<Direction, ByteArray>> = arrayOf(
      NORTH to byteArrayOf(0, 1, 1, 1),
      EAST to byteArrayOf(1, 0, 1, 1),
      SOUTH to byteArrayOf(0, 0, 1, 0),
      WEST to byteArrayOf(0, 0, 0, 1)
    )
  }
}
