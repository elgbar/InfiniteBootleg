package no.elg.infiniteBootleg.core.world.generator.features

import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.getNoise
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite
import no.elg.infiniteBootleg.core.world.world.World
import kotlin.math.roundToInt

class ForestGenerator(override val seed: Long, private val cutoff: Float) : FeatureGenerator {

  private val shouldGenerateTreeNoise: FastNoiseLite = FastNoiseLite(seed.toInt()).also {
    it.SetNoiseType(FastNoiseLite.NoiseType.Perlin)
    it.SetFrequency(0.55f)
  }

  override fun generateFeature(chunk: Chunk, worldX: WorldCoord, worldY: WorldCoord) {
    val warmth = shouldGenerateTreeNoise.getNoise(worldX, worldY)
    if (warmth > cutoff) {
      createTree(chunk.world, worldX, worldY)
    }
  }

  private fun createTree(world: World, worldX: WorldCoord, worldY: WorldCoord) {
    val treeHeight = shouldGenerateTreeNoise.getNoise(worldX, worldY, 1, MAX_TREE_HEIGHT).coerceAtLeast(MIN_TREE_HEIGHT).roundToInt()
    val locs = World.Companion.getLocationsAABBFromCorner(worldX.toFloat(), worldY.toFloat() + treeHeight, 0f, treeHeight.toFloat())
    Material.BirchTrunk.createBlocks(world, locs, prioritize = false, allowOverwiteNonAir = true)

    val leavesRadius = shouldGenerateTreeNoise.getNoise(worldX, worldY, 2, MAX_PRIMARY_LEAVES_RADIUS).coerceAtLeast(MIN_PRIMARY_LEAVES_RADIUS)
    generateLeavesBlob(world, worldX, worldY + treeHeight, leavesRadius)

    if (treeHeight >= MIN_TREE_HEIGHT_FOR_SECONDARY_LEAVES) {
      val leavesRadius2 = shouldGenerateTreeNoise.getNoise(
        worldX,
        worldY,
        3,
        MAX_SECONDARY_LEAVES_RADIUS
      ).coerceIn(MIN_SECONDARY_LEAVES_RADIUS, leavesRadius - World.Companion.HALF_BLOCK_SIZE)
      generateLeavesBlob(world, worldX, worldY + (treeHeight - leavesRadius).toInt(), leavesRadius2)
    }
  }

  private fun generateLeavesBlob(world: World, worldX: WorldCoord, worldY: WorldCoord, radius: Float) {
    val leaves = World.Companion.getLocationsWithin(worldX, worldY, radius)
    Material.BirchLeaves.createBlocks(world, leaves, false)
  }

  companion object {
    const val MAX_TREE_HEIGHT = 16f
    const val MIN_TREE_HEIGHT = 3f

    const val MAX_PRIMARY_LEAVES_RADIUS = 5f
    const val MIN_PRIMARY_LEAVES_RADIUS = 3f

    const val MIN_TREE_HEIGHT_FOR_SECONDARY_LEAVES = 6

    const val MAX_SECONDARY_LEAVES_RADIUS = 4f
    const val MIN_SECONDARY_LEAVES_RADIUS = 1f
  }
}
