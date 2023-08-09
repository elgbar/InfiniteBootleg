package no.elg.infiniteBootleg.world.generator.features

import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.getNoise
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.generator.noise.FastNoiseLite
import no.elg.infiniteBootleg.world.world.World
import no.elg.infiniteBootleg.world.world.World.Companion.getLocationsWithin

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
    val treeHeight = (shouldGenerateTreeNoise.getNoise(worldX, worldY, 1) * 16)
    val locs = World.getLocationsAABB(worldX.toFloat(), worldY.toFloat(), 0f, treeHeight)
    Material.BIRCH_TRUNK.createBlocks(world, locs, false)

    val leavesRadius = (shouldGenerateTreeNoise.getNoise(worldX, worldY, 2) * 5).coerceAtLeast(3f)
    val leaves = getLocationsWithin(worldX, worldY + treeHeight.toInt(), leavesRadius)
    Material.BIRCH_LEAVES.createBlocks(world, leaves, false)

    if (treeHeight >= 6) {
      val leavesRadius2 = (shouldGenerateTreeNoise.getNoise(worldX, worldY, 3) * 4).coerceIn(1f, leavesRadius - 0.5f)
      val leaves2 = getLocationsWithin(worldX, worldY + (treeHeight - leavesRadius).toInt(), leavesRadius2)
      Material.BIRCH_LEAVES.createBlocks(world, leaves2, false)
    }
  }
}
