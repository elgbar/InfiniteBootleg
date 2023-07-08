package no.elg.infiniteBootleg.world.generator.features

import no.elg.infiniteBootleg.util.getNoise
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.generator.noise.FastNoiseLite
import no.elg.infiniteBootleg.world.world.World
import no.elg.infiniteBootleg.world.world.World.Companion.getLocationsWithin

class ForestGenerator(override val seed: Long, private val cutoff: Float) : FeatureGenerator {

  private val shouldGenerateTreeNoise: FastNoiseLite = FastNoiseLite(seed.toInt()).also {
    it.SetNoiseType(FastNoiseLite.NoiseType.Perlin)
    it.SetFrequency(0.55f)
  }

  override fun generateFeature(chunk: Chunk, worldX: Int, worldY: Int) {
    val warmth = shouldGenerateTreeNoise.getNoise(worldX.toDouble(), worldY.toDouble())
    if (warmth > cutoff) {
      createTree(chunk.world, worldX, worldY)
    }
  }

  private fun createTree(world: World, worldX: Int, worldY: Int) {
    val treeHeight = (shouldGenerateTreeNoise.getNoise(worldX.toDouble(), worldY.toDouble(), 1.0) * 16)
    val locs = World.getLocationsAABB(worldX.toFloat(), worldY.toFloat(), 0f, treeHeight)
    Material.BIRCH_TRUNK.createBlocks(world, locs.toSet(), false)

    val leavesRadius = (shouldGenerateTreeNoise.getNoise(worldX.toDouble(), worldY.toDouble(), 2.0) * 5).coerceAtLeast(3f)
    val leaves = getLocationsWithin(worldX, worldY + treeHeight.toInt(), leavesRadius)
    Material.BIRCH_LEAVES.createBlocks(world, leaves.toSet(), false)

    if (treeHeight >= 6) {
      val leavesRadius2 = (shouldGenerateTreeNoise.getNoise(worldX.toDouble(), worldY.toDouble(), 3.0) * 4).coerceIn(1f, leavesRadius - 0.5f)
      val leaves2 = getLocationsWithin(worldX, worldY + (treeHeight - leavesRadius).toInt(), leavesRadius2)
      Material.BIRCH_LEAVES.createBlocks(world, leaves2.toSet(), false)
    }
  }
}
