package no.elg.infiniteBootleg.world

import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.util.CoordUtil
import no.elg.infiniteBootleg.world.Material.AIR

class BlockLight(
  val chunk: Chunk,
  val localX: Int,
  val localY: Int
) {

  /**
   * Whether this block is above the top-most block of the given world y-coordinate
   *
   * It denotes that this is a fully lit block, and thus light can be skipped rendered on it.
   */
  var isSkylight: Boolean
    private set

  /**
   * If this block have any light shining onto it. If not then it should be rendered as a black square
   */
  var isLit: Boolean
    private set

  /**
   * The average brightness of the block
   */
  var averageBrightness: Float
    private set

  /**
   * Brightness, in the range `0..1`, of each sub-cells.
   *
   * Do not modify
   */
  var lightMap: Array<FloatArray> = NO_LIGHTS_LIGHT_MAP
    get() {
      synchronized(this) {
        return field
      }
    }
    private set(value) {
      synchronized(this) {
        field = value
      }
    }

  private val chunkImpl = chunk as ChunkImpl

  init {
    isSkylight = chunk.chunkColumn.isBlockAboveTopBlock(localX, CoordUtil.chunkToWorld(chunk.chunkY, localY), ChunkColumn.BLOCKS_LIGHT_FLAG)
    isLit = isSkylight
    averageBrightness = if (isSkylight) {
      1f
    } else {
      0f
    }

    lightMap = if (isSkylight) {
      SKYLIGHT_LIGHT_MAP
    } else {
      Array(Block.LIGHT_RESOLUTION) { FloatArray(Block.LIGHT_RESOLUTION) }
    }
  }

  fun recalculateLighting() {
    recalculateLighting(NEVER_CANCEL_UPDATE_ID)
  }

  fun recalculateLighting(updateId: Int) {
    //    System.out.println("Recalculating light for " + getMaterial() + " block " + getWorldX() +
    // "," + getWorldY());
    if (!Settings.renderLight) {
      return
    }

    fun isCancelled(): Boolean {
      return updateId != NEVER_CANCEL_UPDATE_ID && updateId != chunkImpl.currentUpdateId.get()
    }

    val chunkColumn = chunk.chunkColumn
    val worldX: Int = CoordUtil.chunkToWorld(chunk.chunkX, localX)
    val worldY: Int = CoordUtil.chunkToWorld(chunk.chunkY, localY)

    if (chunkColumn.isBlockAboveTopBlock(localX, worldY, ChunkColumn.BLOCKS_LIGHT_FLAG)) {
      // This block is a skylight, its always lit fully
      isLit = true
      isSkylight = true
      averageBrightness = 1f
      lightMap = SKYLIGHT_LIGHT_MAP
      return
    }

    var isLitNext = false
    val tmpLightMap = Array(Block.LIGHT_RESOLUTION) { FloatArray(Block.LIGHT_RESOLUTION) }

    if (isCancelled()) {
      return
    }
    // find light sources around this block
    val blocksAABB = chunk
      .world
      .getBlocksAABB(
        worldX + 0.5f,
        worldY + 0.5f,
        Block.LIGHT_SOURCE_LOOK_BLOCKS.toFloat(),
        Block.LIGHT_SOURCE_LOOK_BLOCKS.toFloat(),
        false,
        false,
        ::isCancelled
      )
    if (isCancelled()) {
      return
    }
    for (neighbor in blocksAABB) {
      val neighChunkCol = neighbor.chunk.chunkColumn
      val neiMat = neighbor.material
      if (neiMat.isLuminescent ||
        (neiMat == AIR && neighChunkCol.isBlockAboveTopBlock(neighbor.localX, neighbor.worldY, ChunkColumn.BLOCKS_LIGHT_FLAG))
      ) {
        if (isCancelled()) {
          return
        }
        isLitNext = true
        for (dx in 0 until Block.LIGHT_RESOLUTION) {
          for (dy in 0 until Block.LIGHT_RESOLUTION) {
            // Calculate distance for each light cell
            val dist = (
              Location.distCubed(
                (
                  worldX + dx.toFloat() / Block.LIGHT_RESOLUTION
                  ).toDouble(),
                (
                  worldY + dy.toFloat() / Block.LIGHT_RESOLUTION
                  ).toDouble(),
                neighbor.worldX + 0.5,
                neighbor.worldY + 0.5
              ) /
                (Block.LIGHT_SOURCE_LOOK_BLOCKS * Block.LIGHT_SOURCE_LOOK_BLOCKS)
              )
            val old: Float = tmpLightMap[dx][dy]
            val normalizedIntensity = if (dist == 0.0) {
              0f
            } else if (dist > 0) {
              1 - dist.toFloat()
            } else {
              1 + dist.toFloat()
            }

            if (old < normalizedIntensity) {
              tmpLightMap[dx][dy] = normalizedIntensity
            }
          }
        }
      }
    }

    if (isCancelled()) {
      return
    }

    isSkylight = false
    isLit = isLitNext
    if (isLitNext) {
      lightMap = tmpLightMap
      var total = 0.0
      for (x in 0 until Block.LIGHT_RESOLUTION) {
        for (y in 0 until Block.LIGHT_RESOLUTION) {
          total += lightMap[x][y].toDouble()
        }
      }
      averageBrightness = (total / (Block.LIGHT_RESOLUTION * Block.LIGHT_RESOLUTION)).toFloat()
    } else {
      lightMap = NO_LIGHTS_LIGHT_MAP
      averageBrightness = 0f
    }
  }

  companion object {
    const val NEVER_CANCEL_UPDATE_ID = -1
    val SKYLIGHT_LIGHT_MAP: Array<FloatArray> = Array(Block.LIGHT_RESOLUTION) { FloatArray(Block.LIGHT_RESOLUTION) { 1f } }
    val NO_LIGHTS_LIGHT_MAP: Array<FloatArray> = Array(Block.LIGHT_RESOLUTION) { FloatArray(Block.LIGHT_RESOLUTION) { 0f } }
  }
}
