package no.elg.infiniteBootleg.world.blocks

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import ktx.collections.GdxArray
import ktx.collections.isNotEmpty
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.events.BlockLightChangedEvent
import no.elg.infiniteBootleg.events.api.EventManager.dispatchEventAsync
import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.chunkToWorld
import no.elg.infiniteBootleg.util.distCubed
import no.elg.infiniteBootleg.util.dst2
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.world.blocks.BlockLight.Companion.LIGHT_RESOLUTION
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.chunks.ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG
import no.elg.infiniteBootleg.world.world.World.Companion.EMPTY_BLOCKS_ARRAY
import no.elg.infiniteBootleg.world.world.World.Companion.LIGHT_SOURCE_LOOK_BLOCKS
import no.elg.infiniteBootleg.world.world.World.Companion.LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA
import no.elg.infiniteBootleg.world.world.World.Companion.LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA_F
import no.elg.infiniteBootleg.world.world.World.Companion.LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA_POW
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * How bright a block is
 *
 * The range is from [BlockLight.Companion.COMPLETE_DARKNESS] to [BlockLight.Companion.FULL_BRIGHTNESS]
 */
typealias Brightness = Float
typealias BrightnessArray = FloatArray

class BlockLight(
  val chunk: Chunk,
  val localX: LocalCoord,
  val localY: LocalCoord
) {

  /**
   * Whether this block is above the top-most block of the given world y-coordinate
   *
   * It denotes that this is a fully lit block, and thus light can be skipped rendered on it.
   */
  var isSkylight: Boolean = false
    private set

  /**
   * If this block have any light shining onto it. If not then it should be rendered as a black square
   */
  var isLit: Boolean = false
    private set

  /**
   * The average brightness of the block
   */
  var averageBrightness: Brightness = COMPLETE_DARKNESS
    private set

  /**
   * Brightness, in the range `0f..1f`, of each sub-cells.
   *
   * ## Layout
   *
   * The array is a 2D array with the size of [LIGHT_RESOLUTION] x [LIGHT_RESOLUTION], stored in row-major order.
   *
   * Do not modify
   */
  @field:Volatile
  var lightMap: BrightnessArray = NO_LIGHTS_LIGHT_MAP
    private set

  private val event = BlockLightChangedEvent(chunk, localX, localY)

  init {
    if (isAboveTopBlock()) {
      setToSkyLight(false)
    } else {
      setToNoLight(false)
    }
  }

  fun calculateLightFrom(
    neighbor: Block,
    worldX: WorldCoord,
    worldY: WorldCoord,
    tmpLightMap: FloatArray,
    firstTime: Boolean
  ) {
    // Written to hopefully be auto-vectorized by the JVM
    for (dx in 0 until LIGHT_RESOLUTION) {
      for (dy in 0 until LIGHT_RESOLUTION) {
        // Calculate distance for each light cell
        val cellX = worldX + centerOfSubcell(dx)
        val cellY = worldY + centerOfSubcell(dy)
        val nx = neighbor.worldX + 0.5
        val ny = neighbor.worldY + 0.5
        val distCubed1 = distCubed(cellX, cellY, nx, ny)
        val f = LIGHT_SOURCE_LOOK_BLOCKS * LIGHT_SOURCE_LOOK_BLOCKS
        val distCubed = distCubed1 / f
        val negSignum = -sign(distCubed)
        val intensity = 1 + (negSignum * distCubed)

        if (firstTime || tmpLightMap[lightMapIndex(dx, dy)] < intensity) {
          tmpLightMap[lightMapIndex(dx, dy)] = intensity.toFloat()
        }
      }
    }
  }

  private fun isAboveTopBlock(worldY: WorldCoord = chunk.chunkY.chunkToWorld(localY)): Boolean = chunk.chunkColumn.isBlockAboveTopBlock(localX, worldY, BLOCKS_LIGHT_FLAG)

  private fun setToSkyLight(publishEvent: Boolean) {
    isLit = true
    isSkylight = true
    averageBrightness = FULL_BRIGHTNESS
    lightMap = SKYLIGHT_LIGHT_MAP
    if (publishEvent) {
      dispatchLightChangeEvent()
    }
  }

  private fun setToNoLight(publishEvent: Boolean) {
    isLit = false
    isSkylight = false
    averageBrightness = COMPLETE_DARKNESS
    lightMap = NO_LIGHTS_LIGHT_MAP
    if (publishEvent) {
      dispatchLightChangeEvent()
    }
  }

  private fun dispatchLightChangeEvent() {
    if (Settings.renderBlockLightUpdates) {
      dispatchEventAsync(event)
    }
  }

  /**
   * @return If any changes was made to the lighting
   */
  internal suspend fun recalculateLighting(): Boolean {
    if (!Settings.renderLight || chunk.isInvalid) {
      return false
    }
    val worldX = chunk.chunkX.chunkToWorld(localX)
    val worldY = chunk.chunkY.chunkToWorld(localY)

    if (isAboveTopBlock(worldY)) {
      if (isSkylight) {
        return false
      }
      // This block is a skylight, its always lit fully
      setToSkyLight(publishEvent = true)
      return true
    }
    return coroutineScope {
      // find light sources around this block
      val deferredBlocks = listOf(
        async { findLuminescentBlocks(worldX, worldY) },
        async { findSkylightBlocks(worldX, worldY) }
      )
      val (blockLights, skyLights) = deferredBlocks.awaitAll()
      ensureActive()

      val lightBlocks = if (blockLights.isEmpty && skyLights.isEmpty) {
        if (isLit) {
          setToNoLight(publishEvent = true)
          return@coroutineScope true
        }
        return@coroutineScope false
      } else if (blockLights.isEmpty) {
        skyLights
      } else if (skyLights.isEmpty) {
        blockLights
      } else {
        skyLights.ensureCapacity(blockLights.size)
        for (block in blockLights) {
          if (block !in skyLights) {
            skyLights.add(block)
          }
        }
        skyLights
      }
      ensureActive()
      val tmpLightMap: BrightnessArray = FloatArray(LIGHT_RESOLUTION_SQUARE) { COMPLETE_DARKNESS }
      var firstTime = true
      for (neighbor in lightBlocks) {
        calculateLightFrom(neighbor, worldX, worldY, tmpLightMap, firstTime)
        firstTime = false
      }
      ensureActive()
      isSkylight = false
      isLit = true
      lightMap = tmpLightMap
      val total = tmpLightMap.sum()
      averageBrightness = total / LIGHT_RESOLUTION_SQUARE
      dispatchLightChangeEvent()
      return@coroutineScope true
    }
  }

  fun findLuminescentBlocks(worldX: WorldCoord, worldY: WorldCoord): GdxArray<Block> =
    chunk.world.getBlocksAABBFromCenter(
      worldX + 0.5f,
      worldY + 0.5f,
      LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA_F,
      LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA_F,
      raw = true,
      loadChunk = false,
      includeAir = false,
      filter = EMITS_LIGHT_FILTER
    )

  fun findSkylightBlocks(worldX: WorldCoord, worldY: WorldCoord): GdxArray<Block> {
    var skyblocks: GdxArray<Block>? = null
    // Since we're not creating air blocks above, we will instead just load
    for (offsetWorldX: LocalCoord in -LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA..LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA) {
      val topWorldX: WorldCoord = worldX + offsetWorldX
      val topWorldY: WorldCoord = chunk.world.getTopBlockWorldY(topWorldX, BLOCKS_LIGHT_FLAG) + 1

      val topWorldYR: WorldCoord = chunk.world.getTopBlockWorldY(topWorldX + 1, BLOCKS_LIGHT_FLAG) + 1
      val offsetYR: LocalCoord = topWorldYR - topWorldY

      val topWorldYL: WorldCoord = chunk.world.getTopBlockWorldY(topWorldX - 1, BLOCKS_LIGHT_FLAG) + 1
      val offsetYL: LocalCoord = topWorldYL - topWorldY

      val offsetY: LocalCoord
      val topWorldYLR: WorldCoord
      if (offsetYL > offsetYR) {
        offsetY = offsetYL
        topWorldYLR = topWorldYL
      } else {
        offsetY = offsetYR
        topWorldYLR = topWorldYR
      }

      if (offsetY <= MIN_Y_OFFSET) {
        // Too little offset to bother with columns of skylight (or the block is above left and right)
        val block = chunk.getBlock(topWorldX, topWorldY, loadChunk = false) ?: continue
        if (skylightBlockFilter(worldX.toFloat(), worldY.toFloat(), block)) {
          val actualSkyblocks = skyblocks ?: GdxArray<Block>(false, 1).also { array -> skyblocks = array }
          actualSkyblocks.add(block)
        }
        continue
      }
      val findSkylightBlockColumn = findSkylightBlockColumn(
        worldX.toFloat(),
        worldY.toFloat(),
        topWorldX.toFloat(),
        topWorldY.toFloat(),
        topWorldYLR.toFloat()
      )
      if (findSkylightBlockColumn.isNotEmpty()) {
        if (skyblocks == null) {
          skyblocks = findSkylightBlockColumn
        } else {
          skyblocks.addAll(findSkylightBlockColumn)
        }
      }
    }
    return skyblocks ?: EMPTY_BLOCKS_ARRAY
  }

  /**
   * @return If the given block is a valid skylight candidate
   */
  private fun skylightBlockFilter(worldX: Float, worldY: Float, block: Block): Boolean {
    return !block.material.blocksLight &&
      !block.material.emitsLight &&
      block.chunk.chunkColumn.isBlockAboveTopBlock(block.localX, block.worldY, BLOCKS_LIGHT_FLAG) &&
      dst2(worldX.toInt(), worldY.toInt(), block.worldX, block.worldY) <= LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA_POW
  }

  /**
   * Given two y-coordinates ([worldYA] and [worldYB]) get a column of blocks between the two
   */
  private fun findSkylightBlockColumn(
    worldX: Float,
    worldY: Float,
    topWorldX: Float,
    worldYA: Float,
    worldYB: Float
  ): GdxArray<Block> {
    val worldYTop = min(max(worldYA, worldYB), worldY + LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA)
    val worldYBtm = max(min(worldYA, worldYB), worldY - LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA)
    val columnHeight = worldYTop - worldYBtm
    if (columnHeight <= 0) {
      // The bottom world y-coordinate is below (or at) the top world y-coordinate!
      // This means the worldY coordinate is too far away from the potential skylight column
      // thus no skylight can reach this block
      return EMPTY_BLOCKS_ARRAY
    }
    return chunk.world.getBlocksAABB(topWorldX, worldYBtm, 0f, columnHeight, raw = false, loadChunk = false, includeAir = true) { it ->
      skylightBlockFilter(worldX, worldY, it)
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is BlockLight) return false

    if (chunk != other.chunk) return false
    if (localX != other.localX) return false
    return localY == other.localY
  }

  override fun hashCode(): Int {
    var result = chunk.hashCode()
    result = 31 * result + localX
    result = 31 * result + localY
    return result
  }

  companion object {

    /**
     * Lower inclusive bound for brightness
     *
     * `0f`
     */
    const val COMPLETE_DARKNESS: Brightness = 0f

    /**
     * Upper inclusive bound for brightness
     *
     * `1f`
     */
    const val FULL_BRIGHTNESS: Brightness = 1f

    const val LIGHT_RESOLUTION = 2
    const val LIGHT_RESOLUTION_SQUARE = LIGHT_RESOLUTION * LIGHT_RESOLUTION

    val EMITS_LIGHT_FILTER = { block: Block -> block.material.emitsLight }

    val SKYLIGHT_LIGHT_MAP: BrightnessArray = FloatArray(LIGHT_RESOLUTION_SQUARE) { FULL_BRIGHTNESS }
    val NO_LIGHTS_LIGHT_MAP: BrightnessArray = FloatArray(LIGHT_RESOLUTION_SQUARE) { COMPLETE_DARKNESS }

    inline fun lightMapIndex(dx: Int, dy: Int): Int = dx * LIGHT_RESOLUTION + dy

    const val MIN_Y_OFFSET = 1

    fun centerOfSubcell(subcellCoordinate: Int): Double = ((1.0 / LIGHT_RESOLUTION) + subcellCoordinate.toDouble()) / LIGHT_RESOLUTION
  }
}
