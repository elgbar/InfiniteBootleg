package no.elg.infiniteBootleg.core.world.blocks

import com.badlogic.gdx.graphics.Color
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import ktx.collections.GdxArray
import ktx.collections.isNotEmpty
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.events.BlockLightChangedEvent
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.chunkToWorld
import no.elg.infiniteBootleg.core.util.distCubed
import no.elg.infiniteBootleg.core.util.dst2
import no.elg.infiniteBootleg.core.world.Material.Companion.emitsLight
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.core.world.blocks.BlockLight.Companion.LIGHT_RESOLUTION
import no.elg.infiniteBootleg.core.world.blocks.LightMap.Companion.Brightness
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.chunks.ChunkColumn
import no.elg.infiniteBootleg.core.world.world.World
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class BlockLight(val chunk: Chunk, val localX: LocalCoord, val localY: LocalCoord) {

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
  var lightMap: LightMap = NO_LIGHTS_LIGHT_MAP
    private set

  private val event by lazy(LazyThreadSafetyMode.PUBLICATION) { BlockLightChangedEvent(chunk, localX, localY) }

  init {
    if (isAboveTopBlock()) {
      setToSkyLight(false)
    } else {
      setToNoLight(false)
    }
  }

  fun calculateLightFrom(neighbor: Block, worldX: WorldCoord, worldY: WorldCoord, tmpLightMap: LightMap) {
    val nx = neighbor.worldX + 0.5
    val ny = neighbor.worldY + 0.5
    // If null but we still got here, then the block is a skylight and thus should be white
    val tint: Color = neighbor.material.lightColor ?: Color.WHITE
    // Compensate intensity so all tint colors produce equal total light energy as white
    val tintSum = tint.r + tint.g + tint.b
    val brightnessCompensation = if (Settings.lightColorEnergyCompensation && tintSum > 0f) 3f / tintSum else 1f
    val maxDistance = World.LIGHT_SOURCE_LOOK_BLOCKS.toDouble()
    val maxDistSq = maxDistance * maxDistance

    for (dx in 0 until LIGHT_RESOLUTION) {
      for (dy in 0 until LIGHT_RESOLUTION) {
        val cellX = worldX + centerOfSubcell(dx)
        val cellY = worldY + centerOfSubcell(dy)

        // Compute ray attenuation per subcell, stepping at subcell resolution
        val attenuation = if (Settings.lightOcclusion) {
          computeRayAttenuation(nx, ny, cellX, cellY)
        } else {
          1.0f
        }
        if (attenuation <= 0f) continue

        val distSq = distCubed(cellX, cellY, nx, ny)

        val intensity = when (Settings.lightIntensityMapping) {
          // Linear falloff: 1.0 at source, 0.0 at maxDistance
          Settings.LightIntensityMapping.LINEAR -> {
            val t = (sqrt(distSq) / maxDistance).coerceIn(0.0, 1.0)
            1.0 - t
          }
          // Smoothstep falloff: zero derivative at both ends, same brightness profile as linear
          Settings.LightIntensityMapping.SMOOTH_FALLOFF_LINEAR_SPACE -> {
            val t = (sqrt(distSq) / maxDistance).coerceIn(0.0, 1.0)
            1.0 - t * t * (3.0 - 2.0 * t)
          }
          // Smoothstep in squared-distance space: bright for longer, fast falloff near edge, no sqrt needed
          Settings.LightIntensityMapping.SMOOTH_FALLOFF_SQUARED_SPACE -> {
            val tSq = (distSq / maxDistSq).coerceIn(0.0, 1.0)
            1.0 - tSq * tSq * (3.0 - 2.0 * tSq)
          }
        }

        val lightMapIndex = lightMapIndex(dx, dy)
        tmpLightMap.updateColor(lightMapIndex, (intensity * Settings.lightIntensityMultiplier * brightnessCompensation * attenuation).toFloat(), tint)
      }
    }
  }

  /**
   * Compute light attenuation along the ray from source to destination using DDA at subcell resolution.
   * Steps through a grid that is [LIGHT_RESOLUTION] times finer than the block grid for precise ray paths.
   * Opacity is applied once per block entry (not per subcell) so diagonal rays aren't over-attenuated.
   * Skips source and destination subcells.
   *
   * @return attenuation in [0.0, 1.0] where 1.0 = no occlusion, 0.0 = fully blocked
   */
  private fun computeRayAttenuation(srcX: Double, srcY: Double, dstX: Double, dstY: Double): Float {
    val cellSize = 1.0 / LIGHT_RESOLUTION

    // Convert to subcell grid coordinates
    val srcCellX = floor(srcX / cellSize).toInt()
    val srcCellY = floor(srcY / cellSize).toInt()
    val dstCellX = floor(dstX / cellSize).toInt()
    val dstCellY = floor(dstY / cellSize).toInt()

    if (srcCellX == dstCellX && srcCellY == dstCellY) return 1.0f

    val dirX = dstX - srcX
    val dirY = dstY - srcY

    // Normalize per-step opacity by L2/L1 ratio of the ray direction.
    // Diagonal rays have more DDA steps than axis-aligned rays for the same distance,
    // so we scale down each step's opacity to compensate, producing circular shadows.
    val euclidean = sqrt(dirX * dirX + dirY * dirY)
    val manhattan = abs(dirX) + abs(dirY)
    val opacityNormalizer = if (manhattan > 0) (euclidean / manhattan).toFloat() else 1f

    val stepX = if (dirX > 0) 1 else -1
    val stepY = if (dirY > 0) 1 else -1

    val tDeltaX = if (dirX != 0.0) abs(cellSize / dirX) else Double.MAX_VALUE
    val tDeltaY = if (dirY != 0.0) abs(cellSize / dirY) else Double.MAX_VALUE

    var tMaxX = if (dirX > 0) {
      ((srcCellX + 1) * cellSize - srcX) / dirX
    } else if (dirX < 0) {
      (srcX - srcCellX * cellSize) / -dirX
    } else {
      Double.MAX_VALUE
    }

    var tMaxY = if (dirY > 0) {
      ((srcCellY + 1) * cellSize - srcY) / dirY
    } else if (dirY < 0) {
      (srcY - srcCellY * cellSize) / -dirY
    } else {
      Double.MAX_VALUE
    }

    var cx = srcCellX
    var cy = srcCellY
    var attenuation = 1.0f
    val world = chunk.world

    while (true) {
      // Step to the next subcell
      if (tMaxX < tMaxY) {
        cx += stepX
        tMaxX += tDeltaX
      } else {
        cy += stepY
        tMaxY += tDeltaY
      }

      // Reached destination subcell, stop
      if (cx == dstCellX && cy == dstCellY) break

      // Convert subcell coords to block coords
      val blockX = Math.floorDiv(cx, LIGHT_RESOLUTION)
      val blockY = Math.floorDiv(cy, LIGHT_RESOLUTION)

      val block = world.getRawBlock(blockX, blockY, loadChunk = false)
      val opacity = block?.material?.lightOpacity ?: 0f
      attenuation *= (1.0f - opacity * opacityNormalizer)
      if (attenuation <= 0f) return 0f
    }

    return attenuation
  }

  private fun isAboveTopBlock(worldY: WorldCoord = chunk.chunkY.chunkToWorld(localY)): Boolean =
    chunk.chunkColumn.isBlockAboveTopBlock(
      localX,
      worldY,
      ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG
    )

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
    // Note: Currently we only use this event for debugging
    if (Settings.renderBlockLightUpdates) {
      EventManager.dispatchEventAsync(event)
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
      val tmpLightMap = LightMap()
      for (neighbor in lightBlocks) {
        calculateLightFrom(neighbor, worldX, worldY, tmpLightMap)
      }
      when (Settings.lightToneMapping) {
        Settings.LightToneMapping.REINHARD -> tmpLightMap.calculateReinhardToneMapping()
        Settings.LightToneMapping.REINHARD_JODIE_LUMINANCE_BY_INTENSITY -> tmpLightMap.calculateReinhardJodieToneMapping()
        Settings.LightToneMapping.REINHARD_JODIE_LUMINANCE_BY_COLOR -> tmpLightMap.calculateReinhardJodieToneMappingLuminanceByColor()
        Settings.LightToneMapping.CLAMP -> Unit
      }
      ensureActive()

      averageBrightness = tmpLightMap.averageBrightness()
      if (averageBrightness == COMPLETE_DARKNESS) {
        setToNoLight(publishEvent = isLit)
      } else {
        isLit = true
        isSkylight = false
        lightMap = tmpLightMap
        dispatchLightChangeEvent()
      }
      return@coroutineScope true
    }
  }

  fun findLuminescentBlocks(worldX: WorldCoord, worldY: WorldCoord): GdxArray<Block> =
    chunk.world.getBlocksAABBFromCenter(
      worldX + 0.5f,
      worldY + 0.5f,
      World.LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA_F,
      World.LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA_F,
      raw = true,
      loadChunk = false,
      includeAir = false,
      filter = EMITS_LIGHT_FILTER
    )

  fun findSkylightBlocks(worldX: WorldCoord, worldY: WorldCoord): GdxArray<Block> {
    var skyblocks: GdxArray<Block>? = null
    // Since we're not creating air blocks above, we will instead just load
    for (offsetWorldX: LocalCoord in -World.LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA..World.LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA) {
      val topWorldX: WorldCoord = worldX + offsetWorldX
      val topWorldY: WorldCoord = chunk.world.getTopBlockWorldY(
        topWorldX,
        ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG
      ) + 1

      val topWorldYR: WorldCoord = chunk.world.getTopBlockWorldY(
        topWorldX + 1,
        ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG
      ) + 1
      val offsetYR: LocalCoord = topWorldYR - topWorldY

      val topWorldYL: WorldCoord = chunk.world.getTopBlockWorldY(
        topWorldX - 1,
        ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG
      ) + 1
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
    return skyblocks ?: World.EMPTY_BLOCKS_ARRAY
  }

  /**
   * @return If the given block is a valid skylight candidate
   */
  private fun skylightBlockFilter(worldX: Float, worldY: Float, block: Block): Boolean =
    !block.material.blocksLight &&
      !block.material.emitsLight &&
      block.chunk.chunkColumn.isBlockAboveTopBlock(
        block.localX,
        block.worldY,
        ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG
      ) &&
      dst2(
        worldX.toInt(),
        worldY.toInt(),
        block.worldX,
        block.worldY
      ) <= World.LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA_POW

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
    val worldYTop = min(max(worldYA, worldYB), worldY + World.LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA)
    val worldYBtm = max(min(worldYA, worldYB), worldY - World.LIGHT_SOURCE_LOOK_BLOCKS_WITH_EXTRA)
    val columnHeight = worldYTop - worldYBtm
    if (columnHeight <= 0) {
      // The bottom world y-coordinate is below (or at) the top world y-coordinate!
      // This means the worldY coordinate is too far away from the potential skylight column
      // thus no skylight can reach this block
      return World.EMPTY_BLOCKS_ARRAY
    }
    return chunk.world.getBlocksAABB(topWorldX, worldYBtm, 0f, columnHeight, raw = false, loadChunk = false, includeAir = true) {
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

    inline fun lightMapIndex(dx: Int, dy: Int): Int = dx * LIGHT_RESOLUTION + dy

    const val MIN_Y_OFFSET = 1

    fun centerOfSubcell(subcellCoordinate: Int): Double = ((1.0 / LIGHT_RESOLUTION) + subcellCoordinate.toDouble()) / LIGHT_RESOLUTION
  }
}
