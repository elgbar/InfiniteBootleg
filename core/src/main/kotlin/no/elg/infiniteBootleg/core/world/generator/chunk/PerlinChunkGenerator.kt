package no.elg.infiniteBootleg.core.world.generator.chunk

import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.chunkOffset
import no.elg.infiniteBootleg.core.util.chunkToWorld
import no.elg.infiniteBootleg.core.util.getNoise
import no.elg.infiniteBootleg.core.util.getNoisePositive
import no.elg.infiniteBootleg.core.util.worldToChunk
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.generator.ChunkGeneratedListener
import no.elg.infiniteBootleg.core.world.generator.biome.Biome
import no.elg.infiniteBootleg.core.world.generator.biome.Biome.Companion.INTERPOLATION_RADIUS
import no.elg.infiniteBootleg.core.world.generator.features.ForestGenerator
import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite
import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite.FractalType
import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite.NoiseType
import no.elg.infiniteBootleg.core.world.world.World

/**
 * @author Elg
 */
class PerlinChunkGenerator(override val seed: Long) :
  ChunkGenerator,
  Disposable {

  private val chunkGeneratedListener = ChunkGeneratedListener(this)
  private val sparseTreeGenerator = ForestGenerator(seed, 0.8)
  private val denseTreeGenerator = ForestGenerator(seed, 0.7)

  val biomeTypeNoise: FastNoiseLite = FastNoiseLite(seed.toInt()).also {
    it.setNoiseType(NoiseType.Perlin)
    it.setFrequency(0.001)
  }
  private val noiseWormCaves: FastNoiseLite = FastNoiseLite(seed.toInt()).also {
    it.setNoiseType(NoiseType.Perlin)
    it.setFrequency(WORM_SIZE_FREQUENCY)
    it.setFractalType(FractalType.Ridged)
    it.setFractalOctaves(1)
    it.setFractalLacunarity(1.0)
    it.setFractalGain(0.5)
  }

  private val noiseCaveAmplitue: FastNoiseLite = FastNoiseLite(seed.toInt()).also {
    it.setNoiseType(NoiseType.Perlin)
    it.setFrequency(0.07)
  }

  private val noiseCheeseCave: FastNoiseLite = FastNoiseLite(seed.toInt()).also {
    it.setNoiseType(NoiseType.OpenSimplex2)
    it.setFrequency(0.05)
    it.setFractalType(FractalType.FBm)
    it.setFractalOctaves(2)
    it.setFractalLacunarity(1.0)
    it.setFractalGain(0.5)
  }

  private val noiseGreatHall: FastNoiseLite = FastNoiseLite(seed.toInt()).also {
    it.setNoiseType(NoiseType.OpenSimplex2)
    it.setFrequency(0.004)
    it.setFractalType(FractalType.FBm)
    it.setFractalOctaves(2)
    it.setFractalLacunarity(1.0)
    it.setFractalGain(0.5)
  }

  fun getBiomeHeight(worldX: WorldCoord): Double = biomeTypeNoise.getNoisePositive(worldX, 0.5, BIOME_HEIGHT_AMPLITUDE)

  override fun getBiome(worldX: WorldCoord): Biome {
    val height = getBiomeHeight(worldX)
    return when {
      height > 0.65 -> Biome.Mountains
      height > 0.45 -> Biome.Plains
      height > 0.15 -> Biome.Desert
      else -> Biome.Plains
    }
  }

  override fun getHeight(worldX: WorldCoord): Int = getBiome(worldX).heightAt(worldX)

  override fun generate(world: World, chunkX: ChunkCoord, chunkY: ChunkCoord): Chunk {
    val chunk = Main.Companion.inst().chunkFactory.createChunk(world, chunkX, chunkY)
    for (localX in 0 until Chunk.Companion.CHUNK_SIZE) {
      val worldX = chunkX.chunkToWorld(localX)
      val biome = getBiome(worldX)
      val genHeight = biome.heightAt(worldX)
      val genChunkY = genHeight.worldToChunk()
      val seed = seed.toInt()
      if (chunkY == genChunkY) {
        biome.fillUpTo(seed, chunk, localX, genHeight.chunkOffset() + 1, genHeight)
      } else if (chunkY < genChunkY) {
        biome.fillUpTo(seed, chunk, localX, Chunk.Companion.CHUNK_SIZE, genHeight)
      }

      // generate caves (where there is something to generate them in
      if (chunkY <= genChunkY) {
        generateCaves(chunk, chunkY, genHeight, biome, worldX)
      }
    }
    chunk.finishLoading()

    return chunk
  }

  override fun generateFeatures(chunk: Chunk) {
    for (localX in 0 until Chunk.Companion.CHUNK_SIZE) {
      val worldX = chunk.chunkX.chunkToWorld(localX)
      val biome = getBiome(worldX)
      val genHeight = biome.heightAt(worldX)
      val material = biome.materialAt(seed.toInt(), 0, worldX, 0)
      val genChunkY = genHeight.worldToChunk()
      if (chunk.chunkY == genChunkY && material == biome.topBlocks[0]) {
        if (biome == Biome.Mountains) {
          sparseTreeGenerator.generateFeature(chunk, worldX, genHeight + 1)
        } else if (biome == Biome.Plains) {
          denseTreeGenerator.generateFeature(chunk, worldX, genHeight + 1)
        }
      }
    }
  }

  private fun generateCaves(
    chunk: Chunk,
    chunkY: ChunkCoord,
    genHeight: Int,
    biome: Biome,
    worldX: WorldCoord
  ) {
    val worldChunkY = chunkY.chunkToWorld()
    val localX = worldX.chunkOffset()
    val worldXd = worldX.toDouble()
    for (localY in 0 until Chunk.Companion.CHUNK_SIZE) {
      val worldY = worldChunkY + localY
      val worldYd = worldY.toDouble()

      // calculate the size of the worm
      val caveAmplitude = 1 + noiseCaveAmplitue.getNoise(worldXd, worldYd, CAVE_SIZE_AMPLITUDE)

      val worm = noiseWormCaves.getNoise(worldXd, worldYd, amplitude = caveAmplitude)
      val cheese by lazy { noiseCheeseCave.getNoisePositive(worldXd, worldYd, amplitude = caveAmplitude) }
      val greatHall by lazy { noiseGreatHall.getNoisePositive(worldXd, worldYd, amplitude = caveAmplitude) }

      val diffToSurface = (genHeight - worldY).toDouble()
      val depthModifier = (diffToSurface / biome.biomeMaxDepth).coerceAtMost(1.0)
      if (worm > SNAKE_CAVE_CREATION_THRESHOLD / depthModifier ||
        cheese > CHEESE_CAVE_CREATION_THRESHOLD / depthModifier ||
        greatHall > SNAKE_CAVE_CREATION_THRESHOLD / depthModifier
      ) {
        chunk.removeBlock(localX, localY, updateTexture = false, sendUpdatePacket = false)
      }
    }
  }

  fun Biome.heightAt(worldX: WorldCoord): Int {
    var y = 0
    val seed = seed.toInt()
    for (dx in -INTERPOLATION_RADIUS..INTERPOLATION_RADIUS) {
      y = if (dx != 0) {
        val biome = getBiome(worldX + dx)
        (y + biome.rawHeightAt(seed, worldX + dx)).toInt()
      } else {
        (y + rawHeightAt(seed, worldX)).toInt()
      }
    }
    val finalY = y / (INTERPOLATION_RADIUS * 2 + 1)
    return finalY
  }

  override fun dispose() {
    chunkGeneratedListener.dispose()
  }

  companion object {
    /** Noise values above this value will be cave (i.e., air).  */
    private const val SNAKE_CAVE_CREATION_THRESHOLD = 0.92
    private const val CHEESE_CAVE_CREATION_THRESHOLD = 0.8

    /** How much the size of the caves (worms) changes  */
    private const val CAVE_SIZE_AMPLITUDE = 0.15

    /** How fast the size of the caves (worms) changes  */
    private const val WORM_SIZE_FREQUENCY = 0.007

    /** How many blocks of the surface should not be caved in  */
    private const val CAVELESS_DEPTH = 16.0

    private const val BIOME_HEIGHT_AMPLITUDE = 1.25
  }
}
