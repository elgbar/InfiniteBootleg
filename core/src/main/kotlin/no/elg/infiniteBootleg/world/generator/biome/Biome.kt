package no.elg.infiniteBootleg.world.generator.biome

import ktx.collections.GdxArray
import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.generator.chunk.PerlinChunkGenerator
import no.elg.infiniteBootleg.world.generator.noise.PerlinNoise
import kotlin.math.abs
import kotlin.math.floor

/**
 * @author Elg
 */
enum class Biome @SafeVarargs constructor(
  private val y: Double,
  private val z: Double,
  private val amplitude: Double,
  private val frequency: Double,
  private val offset: Int,
  private val filler: Material,
  private val topmostBlock: Material,
  vararg topBlocks: Pair<Material, Int>
) {
  PLAINS(0.1, 0.9, 64.0, 0.009, 0, Material.STONE, Material.GRASS, Material.GRASS to 4, Material.DIRT to 10),
  MOUNTAINS(100.0, 0.9, 356.0, 0.005, 25, Material.STONE, Material.GRASS, Material.GRASS to 2, Material.DIRT to 6),
  DESERT(0.1, 0.9, 32.0, 0.005, 0, Material.SAND_STONE, Material.SAND, Material.SAND to 12);

  private val topBlocks: Array<Material>

  init {
    val mats = GdxArray<Material>(true, 16, Material::class.java)
    for ((first, second) in topBlocks) {
      mats.ensureCapacity(second)
      for (i in 0 until second) {
        mats.add(first)
      }
    }
    this.topBlocks = mats.toArray()
  }

  fun heightAt(pcg: PerlinChunkGenerator, worldX: WorldCoord): Int {
    var y = 0
    for (dx in -INTERPOLATION_RADIUS..INTERPOLATION_RADIUS) {
      y = if (dx != 0) {
        val biome = pcg.getBiome(worldX + dx)
        (y + biome.rawHeightAt(pcg.noise, worldX + dx)).toInt()
      } else {
        (y + rawHeightAt(pcg.noise, worldX)).toInt()
      }
    }
    return y / (INTERPOLATION_RADIUS * 2 + 1)
  }

  private fun rawHeightAt(noise: PerlinNoise, worldX: WorldCoord): Double {
    return rawHeightAt(noise, worldX, y, z, amplitude, frequency, offset)
  }

  fun fillUpTo(
    noise: PerlinNoise,
    chunk: Chunk,
    localX: LocalCoord,
    localY: LocalCoord,
    height: Int
  ) {
    val blocks = chunk.blocks[localX]
    for (dy in 0 until localY) {
      val mat = materialAt(noise, height, chunk.worldX + localX, chunk.worldY + dy)
      blocks[dy] = mat.createBlock(chunk.world, chunk, localX, dy)
    }
  }

  private fun materialAt(noise: PerlinNoise, height: Int, worldX: WorldCoord, worldY: WorldCoord): Material {
    var delta = height - worldY
    if (delta == 0) {
      return topmostBlock
    }
    delta += abs(floor(rawHeightAt(noise, worldX, y, z, 10.0, 0.05, 0)).toInt())
    return if (delta >= topBlocks.size) {
      filler
    } else {
      topBlocks[delta]
    }
  }

  companion object {
    const val INTERPOLATION_RADIUS = 25
    fun rawHeightAt(
      noise: PerlinNoise,
      worldX: WorldCoord,
      y: Double,
      z: Double,
      amplitude: Double,
      frequency: Double,
      offset: Int
    ): Double {
      return noise.octaveNoise(worldX * frequency, y * frequency, z * frequency, 6, 0.5) * amplitude + offset
    }
  }
}
