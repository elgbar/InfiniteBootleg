package no.elg.infiniteBootleg.core.world.generator.features

import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.getNoisePositive
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite
import no.elg.infiniteBootleg.core.world.world.World
import kotlin.math.roundToInt

class ForestGenerator(override val seed: Long, private val cutoff: Double) : FeatureGenerator {

  private val shouldGenerateTreeNoise: FastNoiseLite = FastNoiseLite(seed.toInt()).also {
    it.setFrequency(0.55)
  }

  override fun generateFeature(chunk: Chunk, worldX: WorldCoord, worldY: WorldCoord) {
    val warmth = shouldGenerateTreeNoise.getNoisePositive(worldX, worldY)
    if (warmth > cutoff) {
      createTree(chunk.world, worldX, worldY)
    }
  }

  private fun createTree(world: World, worldX: WorldCoord, worldY: WorldCoord) {
    val treeHeight = shouldGenerateTreeNoise.getNoisePositive(worldX, worldY, 1, MAX_TREE_HEIGHT).coerceAtLeast(MIN_TREE_HEIGHT).roundToInt()
    val locs = World.getLocationsAABBFromCorner(worldX.toFloat(), worldY.toFloat() + treeHeight, 0.0, treeHeight.toDouble())
    Material.BirchTrunk.createBlocks(world, locs, prioritize = false, allowOverwiteNonAir = true)

    val leavesRadius = shouldGenerateTreeNoise.getNoisePositive(worldX, worldY, 2, MAX_PRIMARY_LEAVES_RADIUS).coerceAtLeast(MIN_PRIMARY_LEAVES_RADIUS)
    generateLeavesBlob(world, worldX, worldY + treeHeight, leavesRadius)

    if (treeHeight >= MIN_TREE_HEIGHT_FOR_SECONDARY_LEAVES) {
      val leavesRadius2 = shouldGenerateTreeNoise.getNoisePositive(
        worldX,
        worldY,
        3,
        MAX_SECONDARY_LEAVES_RADIUS
      ).coerceIn(MIN_SECONDARY_LEAVES_RADIUS, leavesRadius - World.HALF_BLOCK_SIZE)
      generateLeavesBlob(world, worldX, worldY + (treeHeight - leavesRadius).toInt(), leavesRadius2)
    }
  }

  private fun generateLeavesBlob(world: World, worldX: WorldCoord, worldY: WorldCoord, radius: Double) {
    val leaves = World.getLocationsWithin(worldX, worldY, radius)
    Material.BirchLeaves.createBlocks(world, leaves, false)
  }

  companion object {
    const val MAX_TREE_HEIGHT = 16.0
    const val MIN_TREE_HEIGHT = 3.0

    const val MAX_PRIMARY_LEAVES_RADIUS = 5.0
    const val MIN_PRIMARY_LEAVES_RADIUS = 3.0

    const val MIN_TREE_HEIGHT_FOR_SECONDARY_LEAVES = 6

    const val MAX_SECONDARY_LEAVES_RADIUS = 4.0
    const val MIN_SECONDARY_LEAVES_RADIUS = 1.0
  }
}
