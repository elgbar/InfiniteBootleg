package no.elg.infiniteBootleg.core.world.generator.chunk

import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.chunkOffset
import no.elg.infiniteBootleg.core.util.chunkToWorld
import no.elg.infiniteBootleg.core.util.getNoise
import no.elg.infiniteBootleg.core.util.worldToChunk
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.generator.ChunkGeneratedListener
import no.elg.infiniteBootleg.core.world.generator.biome.Biome
import no.elg.infiniteBootleg.core.world.generator.features.ForestGenerator
import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite
import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite.FractalType
import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite.NoiseType
import no.elg.infiniteBootleg.core.world.generator.noise.PerlinNoise
import no.elg.infiniteBootleg.core.world.world.World
import kotlin.math.abs
import kotlin.math.min

/**
 * @author Elg
 */
class PerlinChunkGenerator(override val seed: Long) : ChunkGenerator, Disposable {

  private val chunkGeneratedListener = ChunkGeneratedListener(this)
  private val sparseTreeGenerator = ForestGenerator(seed, 0.8f)
  private val denseTreeGenerator = ForestGenerator(seed, 0.7f)

  val noise: PerlinNoise = PerlinNoise(seed)
  private val noise2: FastNoiseLite = FastNoiseLite(seed.toInt()).also {
    it.SetNoiseType(NoiseType.Perlin)
    it.SetFrequency(0.01f)
    it.SetFractalType(FractalType.Ridged)
    it.SetFractalOctaves(1)
    it.SetFractalLacunarity(1.0f)
    it.SetFractalGain(0.5f)
  }

  private val noiseCheeseCave: FastNoiseLite = FastNoiseLite(seed.toInt()).also {
    it.SetNoiseType(NoiseType.OpenSimplex2)
    it.SetFrequency(0.05f)
    it.SetFractalOctaves(2)
    it.SetFractalLacunarity(1.0f)
    it.SetFractalGain(0.5f)
  }

  private val noiseGreatHall: FastNoiseLite = FastNoiseLite(seed.toInt()).also {
    it.SetNoiseType(NoiseType.OpenSimplex2)
    it.SetFrequency(0.004f)
    it.SetFractalOctaves(2)
    it.SetFractalLacunarity(1.0f)
    it.SetFractalGain(0.5f)
  }

  fun getBiomeHeight(worldX: WorldCoord): Double = (noise.noise(worldX.toDouble(), 0.5, 0.5, BIOME_HEIGHT_AMPLITUDE, 0.001) + BIOME_HEIGHT_AMPLITUDE) / (BIOME_HEIGHT_AMPLITUDE * 2)

  override fun getBiome(worldX: WorldCoord): Biome {
    val height = getBiomeHeight(worldX)
    return when {
      height > 0.65 -> Biome.MOUNTAINS
      height > 0.45 -> Biome.PLAINS
      height > 0.15 -> Biome.DESERT
      else -> Biome.PLAINS
    }
  }

  override fun getHeight(worldX: WorldCoord): Int {
    return getBiome(worldX).heightAt(this, worldX)
  }

  override fun generate(world: World, chunkX: ChunkCoord, chunkY: ChunkCoord): Chunk {
    val chunk = Main.Companion.inst().chunkFactory.createChunk(world, chunkX, chunkY)
    for (localX in 0 until Chunk.Companion.CHUNK_SIZE) {
      val worldX = chunkX.chunkToWorld(localX)
      val biome = getBiome(worldX)
      val genHeight = biome.heightAt(this, worldX)
      val genChunkY = genHeight.worldToChunk()
      if (chunkY == genChunkY) {
        biome.fillUpTo(noise, chunk, localX, genHeight.chunkOffset() + 1, genHeight)
      } else if (chunkY < genChunkY) {
        biome.fillUpTo(noise, chunk, localX, Chunk.Companion.CHUNK_SIZE, genHeight)
      }

      // generate caves (where there is something to generate them in
      if (chunkY <= genChunkY) {
        generateCaves(chunk, chunkY, genHeight, worldX)
      }
    }
    chunk.finishLoading()

    return chunk
  }

  override fun generateFeatures(chunk: Chunk) {
    for (localX in 0 until Chunk.Companion.CHUNK_SIZE) {
      val worldX = chunk.chunkX.chunkToWorld(localX)
      val biome = getBiome(worldX)
      val genHeight = biome.heightAt(this, worldX)
      val genChunkY = genHeight.worldToChunk()
      if (chunk.chunkY == genChunkY) {
        if (biome == Biome.MOUNTAINS) {
          sparseTreeGenerator.generateFeature(chunk, worldX, genHeight + 1)
        } else if (biome == Biome.PLAINS) {
          denseTreeGenerator.generateFeature(chunk, worldX, genHeight + 1)
        }
      }
    }
  }

  private fun generateCaves(chunk: Chunk, chunkY: ChunkCoord, genHeight: Int, worldX: WorldCoord) {
    val worldChunkY = chunkY.chunkToWorld()
    val localX = worldX.chunkOffset()
    val worldXd = worldX.toDouble()
    for (localY in 0 until Chunk.Companion.CHUNK_SIZE) {
      val worldY = worldChunkY + localY
      val worldYd = worldY.toDouble()

      // calculate the size of the worm
      val wormSize = 1 + abs(noise.noise(worldXd, worldYd, 1.0, WORM_SIZE_AMPLITUDE, WORM_SIZE_FREQUENCY))
      val caveNoise = noise2.GetNoise(worldXd, worldYd) / wormSize
      val diffToSurface = (genHeight - worldY).toDouble()
      val depthModifier = min(1.0, diffToSurface / CAVELESS_DEPTH)

      val cheese = noiseCheeseCave.getNoise(worldXd, worldYd, wormSize)
      val greatHall = noiseGreatHall.getNoise(worldXd, worldYd, wormSize)
      if (caveNoise > SNAKE_CAVE_CREATION_THRESHOLD / depthModifier ||
        cheese > CHEESE_CAVE_CREATION_THRESHOLD / depthModifier ||
        greatHall > SNAKE_CAVE_CREATION_THRESHOLD / depthModifier
      ) {
        chunk.removeBlock(localX, localY, updateTexture = false, sendUpdatePacket = false)
      }
    }
  }

  companion object {
    /** Noise values above this value will be cave (i.e., air).  */
    private const val SNAKE_CAVE_CREATION_THRESHOLD = 0.92
    private const val CHEESE_CAVE_CREATION_THRESHOLD = 0.8

    /** How much the size of the caves (worms) changes  */
    private const val WORM_SIZE_AMPLITUDE = 0.15

    /** How fast the size of the caves (worms) changes  */
    private const val WORM_SIZE_FREQUENCY = 0.1

    /** How many blocks of the surface should not be caved in  */
    private const val CAVELESS_DEPTH = 16.0

    private const val BIOME_HEIGHT_AMPLITUDE = 1.25
  }

  override fun dispose() {
    chunkGeneratedListener.dispose()
  }
}
