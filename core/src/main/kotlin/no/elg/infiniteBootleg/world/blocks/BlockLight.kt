package no.elg.infiniteBootleg.world.blocks

import com.badlogic.gdx.utils.LongMap
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import ktx.collections.GdxArray
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.events.BlockLightChangedEvent
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.chunkToWorld
import no.elg.infiniteBootleg.util.distCubed
import no.elg.infiniteBootleg.util.dst2
import no.elg.infiniteBootleg.util.launchOnAsync
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.chunks.ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG
import no.elg.infiniteBootleg.world.render.ChunkRenderer.Companion.LIGHT_RESOLUTION
import no.elg.infiniteBootleg.world.world.World.Companion.LIGHT_SOURCE_LOOK_BLOCKS
import no.elg.infiniteBootleg.world.world.World.Companion.LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA
import no.elg.infiniteBootleg.world.world.World.Companion.LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA_POW
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

private val logger = KotlinLogging.logger {}

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

  init {
    isSkylight = chunk.chunkColumn.isBlockAboveTopBlock(localX, chunk.chunkY.chunkToWorld(localY), BLOCKS_LIGHT_FLAG)
    isLit = isSkylight
    averageBrightness = if (isSkylight) {
      1f
    } else {
      0f
    }

    lightMap = if (isSkylight) {
      SKYLIGHT_LIGHT_MAP
    } else {
      Array(LIGHT_RESOLUTION) { FloatArray(LIGHT_RESOLUTION) }
    }
  }

  private var job: Job? = null
    set(value) {
      synchronized(this) {
        field?.cancel()
        field = value
      }
    }

  suspend fun recalculateLighting() =
    coroutineScope {
      job = launch(start = CoroutineStart.DEFAULT) {
        recalculateLighting0()
        ensureActive()
        chunk.queueForRendering(prioritize = false)
      }
    }

  fun calculateLightFrom(neighbor: Block, worldX: WorldCoord, worldY: WorldCoord, tmpLightMap: Array<FloatArray>) {
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

        val old: Float = tmpLightMap[dx][dy]
        if (old < intensity) {
          tmpLightMap[dx][dy] = intensity.toFloat()
        }
      }
    }
  }

  private suspend fun CoroutineScope.recalculateLighting0() {
    if (!Settings.renderLight || chunk.isInvalid) {
      return
    }

    val chunkColumn = chunk.chunkColumn
    val worldX = chunk.chunkX.chunkToWorld(localX)
    val worldY = chunk.chunkY.chunkToWorld(localY)

    if (chunkColumn.isBlockAboveTopBlock(localX, worldY, BLOCKS_LIGHT_FLAG)) {
      // This block is a skylight, its always lit fully
      isLit = true
      isSkylight = true
      averageBrightness = 1f
      lightMap = SKYLIGHT_LIGHT_MAP
      return
    }
    ensureActive()

    var isLitNext = false
    val tmpLightMap = Array(LIGHT_RESOLUTION) { FloatArray(LIGHT_RESOLUTION) }
    val chunkCache = LongMap<Chunk>()
    chunkCache.put(chunk.compactLocation, chunk)

    fun calculateLightFrom(neighbors: GdxArray<Block>) {
      for (neighbor in neighbors) {
        isLitNext = true
        calculateLightFrom(neighbor, worldX, worldY, tmpLightMap)
      }
    }

    // find light sources around this block
    coroutineScope {
      launch { calculateLightFrom(findLuminescentBlocks(worldX, worldY, chunkCache)) }
      launch { calculateLightFrom(findSkylightBlocks(worldX, worldY, this, chunkCache)) }
    }
    ensureActive()

    isSkylight = false
    isLit = isLitNext
    if (isLitNext) {
      lightMap = tmpLightMap
      var total = 0f
      for (x in 0 until LIGHT_RESOLUTION) {
        for (y in 0 until LIGHT_RESOLUTION) {
          total += lightMap[x][y]
        }
      }
      averageBrightness = (total / (LIGHT_RESOLUTION * LIGHT_RESOLUTION))
    } else {
      lightMap = NO_LIGHTS_LIGHT_MAP
      averageBrightness = 0f
    }
    if (Settings.renderBlockLightUpdates) {
      ensureActive()
      launchOnAsync { EventManager.dispatchEvent(BlockLightChangedEvent(chunk, localX, localY)) }
    }
  }

  fun findLuminescentBlocks(worldX: WorldCoord, worldY: WorldCoord, chunkCache: LongMap<Chunk>? = null): GdxArray<Block> =
    chunk.world.getBlocksAABBFromCenter(
      worldX + 0.5f,
      worldY + 0.5f,
      LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA,
      LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA,
      raw = true,
      loadChunk = false,
      includeAir = false,
      chunkCache = chunkCache,
      filter = EMITS_LIGHT_FILTER
    )

  fun findSkylightBlocks(worldX: WorldCoord, worldY: WorldCoord, scope: CoroutineScope? = null, chunkCache: LongMap<Chunk>? = null): GdxArray<Block> {
    val skyblocks = GdxArray<Block>()
    // Since we're not creating air blocks above, we will instead just load
    for (offsetWorldX: LocalCoord in -floor(LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA).toInt()..ceil(LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA).toInt()) {
      scope?.ensureActive()
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
        // add one to get the air block above the top-block
        val block = chunk.world.getBlock(topWorldX, topWorldY, loadChunk = false) ?: continue
        if (skylightBlockFilter(worldX.toFloat(), worldY.toFloat(), block)) {
          skyblocks.add(block)
        }
        continue
      }
      val findSkylightBlockColumn = findSkylightBlockColumn(
        worldX.toFloat(),
        worldY.toFloat(),
        topWorldX.toFloat(),
        topWorldY.toFloat(),
        topWorldYLR.toFloat(),
        chunkCache
      ) ?: continue
      skyblocks.addAll(findSkylightBlockColumn)
    }
    return skyblocks
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
    worldYB: Float,
    chunkCache: LongMap<Chunk>?
  ): GdxArray<Block>? {
    val worldYTop = min(max(worldYA, worldYB), worldY + LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA)
    val worldYBtm = max(min(worldYA, worldYB), worldY - LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA)
    val columnHeight = worldYTop - worldYBtm
    if (columnHeight <= 0) {
      // The bottom world y-coordinate is below (or at) the top world y-coordinate!
      // This means the worldY coordinate is too far away from the potential skylight column
      // thus no skylight can reach this block
      return null
    }
    return chunk.world.getBlocksAABB(topWorldX, worldYBtm, 0f, columnHeight, raw = false, loadChunk = false, includeAir = true, chunkCache) { it ->
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
    val EMITS_LIGHT_FILTER = { block: Block -> block.material.emitsLight }

    val SKYLIGHT_LIGHT_MAP: Array<FloatArray> = Array(LIGHT_RESOLUTION) { FloatArray(LIGHT_RESOLUTION) { 1f } }
    val NO_LIGHTS_LIGHT_MAP: Array<FloatArray> = Array(LIGHT_RESOLUTION) { FloatArray(LIGHT_RESOLUTION) { 0f } }

    const val MIN_Y_OFFSET = 1

    fun centerOfSubcell(subcellCoordinate: Int): Double = ((1.0 / LIGHT_RESOLUTION) + subcellCoordinate.toDouble()) / LIGHT_RESOLUTION
  }
}
