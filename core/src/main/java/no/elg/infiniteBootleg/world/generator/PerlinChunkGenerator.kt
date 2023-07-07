package no.elg.infiniteBootleg.world.generator

import no.elg.infiniteBootleg.util.FastNoise
import no.elg.infiniteBootleg.util.chunkOffset
import no.elg.infiniteBootleg.util.chunkToWorld
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.ChunkImpl
import no.elg.infiniteBootleg.world.generator.biome.Biome
import no.elg.infiniteBootleg.world.generator.noise.PerlinNoise
import no.elg.infiniteBootleg.world.world.World

/**
 * @author Elg
 */
class PerlinChunkGenerator(seed: Long) : ChunkGenerator {
  val noise: PerlinNoise
  private val noise2: FastNoise

  init {
    noise = PerlinNoise(seed)
    noise2 = FastNoise(seed.toInt())
    noise2.SetNoiseType(FastNoise.NoiseType.PerlinFractal)
    noise2.SetFrequency(0.01f)
    noise2.SetInterp(FastNoise.Interp.Quintic)
    noise2.SetFractalType(FastNoise.FractalType.RigidMulti)
    noise2.SetFractalOctaves(1)
    noise2.SetFractalLacunarity(1.0f)
    noise2.SetFractalGain(0.5f)
  }

  fun getBiomeHeight(worldX: Int): Double {
    val a = 1.25
    return (noise.noise(worldX.toDouble(), 0.5, 0.5, a, 0.001) + a) / (a * 2)
  }

  override fun getBiome(worldX: Int): Biome {
    val height = getBiomeHeight(worldX)
    return if (height > 0.65) {
      Biome.MOUNTAINS
    } else if (height > 0.45) {
      Biome.PLAINS
    } else if (height > 0.15) {
      Biome.DESERT
    } else {
      Biome.PLAINS
    }
  }

  override fun getHeight(worldX: Int): Int {
    return getBiome(worldX).heightAt(this, worldX)
  }

  override fun generate(world: World, chunkX: Int, chunkY: Int): Chunk {
    val chunk = ChunkImpl(world, chunkX, chunkY)
    for (localX in 0 until Chunk.CHUNK_SIZE) {
      val worldX = chunkToWorld(chunkX, localX)
      val biome = getBiome(worldX)
      val genHeight = biome.heightAt(this, worldX)
      val genChunkY = genHeight.worldToChunk()
      if (chunkY == genChunkY) {
        biome.fillUpTo(noise, chunk, localX, genHeight.chunkOffset() + 1, genHeight)
      } else if (chunkY < genChunkY) {
        biome.fillUpTo(noise, chunk, localX, Chunk.CHUNK_SIZE, genHeight)
      }

      // generate caves (where there is something to generate them in
      if (chunkY <= genChunkY) {
        val blocks = chunk.blocks
        val worldChunkY = chunkY.chunkToWorld()
        for (localY in 0 until Chunk.CHUNK_SIZE) {
          val worldY = worldChunkY + localY

          // calculate the size of the worm
          val wormSize = (1
            + Math.abs(
            noise.noise(worldX.toDouble(), worldY.toDouble(), 1.0, WORM_SIZE_AMPLITUDE, WORM_SIZE_FREQUENCY)
          ))
          val caveNoise = noise2.GetNoise(worldX.toFloat(), worldY.toFloat()) / wormSize
          val diffToSurface = (genHeight - worldY).toDouble()
          val depthModifier = Math.min(1.0, diffToSurface / CAVELESS_DEPTH)
          if (caveNoise > CAVE_CREATION_THRESHOLD / depthModifier) {
            blocks[localX][localY] = null
          }
        }
      }
    }
    chunk.finishLoading()
    return chunk
  }

  companion object {
    /** Noise values above this value will be cave (i.e., air).  */
    private const val CAVE_CREATION_THRESHOLD = 0.92

    /** How much the size of the caves (worms) changes  */
    private const val WORM_SIZE_AMPLITUDE = 0.15

    /** How fast the size of the caves (worms) changes  */
    private const val WORM_SIZE_FREQUENCY = 0.1

    /** How many blocks of the surface should not be caved in  */
    private const val CAVELESS_DEPTH = 16.0
  }
}
