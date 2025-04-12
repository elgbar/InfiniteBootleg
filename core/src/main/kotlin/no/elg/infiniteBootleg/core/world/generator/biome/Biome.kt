package no.elg.infiniteBootleg.core.world.generator.biome

import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.chunks.ChunkImpl
import no.elg.infiniteBootleg.core.world.generator.chunk.PerlinChunkGenerator
import no.elg.infiniteBootleg.core.world.generator.noise.PerlinNoise
import kotlin.math.abs
import kotlin.math.floor

sealed interface Biome {

  val y: Double
  val z: Double
  val amplitude: Double
  val frequency: Double
  val offset: Int
  val filler: Material
  val topmostBlock: Material
  val topBlocks: Array<Material>

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

  fun rawHeightAt(noise: PerlinNoise, worldX: WorldCoord): Double = rawHeightAt(noise, worldX, y, z, amplitude, frequency, offset)

  fun fillUpTo(
    noise: PerlinNoise,
    chunk: ChunkImpl,
    localX: LocalCoord,
    localY: LocalCoord,
    height: Int
  ) {
    val blocks = chunk.blocks[localX]
    for (dy in 0 until localY) {
      val mat = materialAt(noise, height, chunk.worldX + localX, chunk.worldY + dy)
      blocks[dy] = mat.createBlock(chunk.world, chunk, localX, dy, tryRevalidateChunk = false)
    }
  }

  fun materialAt(noise: PerlinNoise, height: Int, worldX: WorldCoord, worldY: WorldCoord): Material {
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

  object Plains : Biome {
    override val y = 0.1
    override val z = 0.9
    override val amplitude = 64.0
    override val frequency = 0.009
    override val offset = 0
    override val filler = Material.Stone
    override val topmostBlock = Material.Grass
    override val topBlocks = arrayOf<Material>(
      *Array(4) { Material.Grass },
      *Array(10) { Material.Dirt }
    )
  }

  object Mountains : Biome {
    override val y = 100.0
    override val z = 0.9
    override val amplitude = 356.0
    override val frequency = 0.005
    override val offset = 25
    override val filler = Material.Stone
    override val topmostBlock = Material.Grass
    override val topBlocks = arrayOf<Material>(
      *Array(2) { Material.Grass },
      *Array(6) { Material.Dirt }
    )
  }

  object Desert : Biome {
    override val y = 0.1
    override val z = 0.9
    override val amplitude = 32.0
    override val frequency = 0.005
    override val offset = 0
    override val filler = Material.Sandstone
    override val topmostBlock = Material.Sand
    override val topBlocks = arrayOf<Material>(
      *Array(12) { Material.Sand }
    )
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
    ): Double = noise.octaveNoise(worldX * frequency, y * frequency, z * frequency, 6, 0.5) * amplitude + offset
  }
}
