package no.elg.infiniteBootleg.client.world.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import ktx.graphics.use
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.world.render.texture.TextureNeighbor
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.Settings.outlineFunc
import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.util.chunkToWorld
import no.elg.infiniteBootleg.core.util.getNoisePositive
import no.elg.infiniteBootleg.core.util.isMarkerBlock
import no.elg.infiniteBootleg.core.util.safeUse
import no.elg.infiniteBootleg.core.util.withColor
import no.elg.infiniteBootleg.core.world.Direction
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.getRawRelative
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.materialOrAir
import no.elg.infiniteBootleg.core.world.blocks.BlockLight
import no.elg.infiniteBootleg.core.world.blocks.BlockShape
import no.elg.infiniteBootleg.core.world.blocks.LightMap
import no.elg.infiniteBootleg.core.world.blocks.LightMap.Companion.Brightness
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.chunks.ChunkColumn
import no.elg.infiniteBootleg.core.world.chunks.TexturedChunk
import no.elg.infiniteBootleg.core.world.generator.noise.FastNoiseLite
import no.elg.infiniteBootleg.core.world.render.texture.RotatableTextureRegion
import no.elg.infiniteBootleg.core.world.world.World
import java.util.EnumMap

/**
 * Render chunks. Which chunk to render is managed by [QueuedChunkRenderer]
 */
class ChunkRenderer(world: World) : Disposable {

  private val batch: SpriteBatch = SpriteBatch().also {
    it.projectionMatrix = Matrix4().setToOrtho2D(0f, 0f, Chunk.CHUNK_TEXTURE_SIZE.toFloat(), Chunk.CHUNK_TEXTURE_SIZE.toFloat())
  }

  private val splitCache: MutableMap<TextureRegion, Array<Array<TextureRegion>>> = HashMap()
  private val stairSplitCache: MutableMap<TextureRegion, Array<Array<TextureRegion>>> = HashMap()

  private val rotationNoise: FastNoiseLite = FastNoiseLite(world.seed.toInt()).also {
    it.setNoiseType(FastNoiseLite.NoiseType.OpenSimplex2)
    it.setFrequency(1.0)
  }

  private val tmpColor = Color()

  /**
   * Render a [TexturedChunk] to its [TexturedChunk.texture]
   */
  fun renderChunk(chunk: TexturedChunk) {
    val fbo = chunk.frameBuffer ?: return
    val chunkColumn = chunk.chunkColumn
    val assets = ClientMain.inst().assets

    // this is the main render function
    fbo.use { _ ->
      Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
      Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
      batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
      batch.safeUse { _ ->
        for (localX in 0 until Chunk.CHUNK_SIZE) {
          val topLightBlockHeight = chunkColumn.topBlockHeight(localX, ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG)
          for (localY in 0 until Chunk.CHUNK_SIZE) {
            if (batch.packedColor != Color.WHITE_FLOAT_BITS) {
              batch.color = Color.WHITE
            }
            val block = chunk.getRawBlock(localX, localY)
            val material = block.materialOrAir()
            val shape = block?.shape ?: BlockShape.FULL
            val isStair = shape.isStair
            val texture: RotatableTextureRegion
            val secondaryTexture: RotatableTextureRegion?

            val worldY = chunk.chunkY.chunkToWorld(localY)
            val dx = localX * Block.BLOCK_TEXTURE_SIZE_F
            val dy = localY * Block.BLOCK_TEXTURE_SIZE_F

            val isMarker = block.isMarkerBlock()
            if (material.invisibleBlock || isMarker) {
              if (isMarker && topLightBlockHeight == worldY) {
                val blockLight = chunk.getBlockLight(localX, localY)
                // Draw half the texture as sky and lower half as cave.
                // This will remove visual artifact when a marker block is falling and updating the top light block height
                drawHalfwayTexture(assets.caveTexture, assets.skyTexture, blockLight.lightMap, dx, dy)
                continue
              } else if (topLightBlockHeight < worldY) {
                // Fast path for air above top light. It should always be lit 100%
                batch.draw(assets.skyTexture.textureRegion, dx, dy, Block.BLOCK_TEXTURE_SIZE_F, Block.BLOCK_TEXTURE_SIZE_F)
                continue
              } else {
                texture = assets.caveTexture
              }
              secondaryTexture = null
            } else {
              texture = block?.texture ?: continue
              // Stair blocks have a transparent quadrant; draw sky/cave behind them so the cutout shows through.
              secondaryTexture = if (material.hasTransparentTexture || isStair) {
                if (topLightBlockHeight > worldY) assets.caveTexture else assets.skyTexture
              } else {
                null
              }
            }

            if (Settings.renderLight) {
              val blockLight = chunk.getBlockLight(localX, localY)
              if (blockLight.isLit && (!blockLight.isSkylight || texture.rotationAllowed)) {
                val rotation = if (isStair) NO_ROTATION else calculateRotation(chunk, localX, localY)
                if (secondaryTexture != null) {
                  drawShadedBlock(secondaryTexture, blockLight.lightMap, dx, dy, rotation, BlockShape.FULL)
                }
                drawShadedBlock(texture, blockLight.lightMap, dx, dy, rotation, shape)
              } else {
                if (blockLight.isLit) {
                  val rotation = if (isStair) NO_ROTATION else calculateRotation(chunk, localX, localY)
                  if (secondaryTexture != null) {
                    drawRotatedTexture(secondaryTexture, dx, dy, rotation)
                  }
                  if (isStair) {
                    drawStairForeground(texture, dx, dy, shape)
                  } else {
                    drawRotatedTexture(texture, dx, dy, rotation)
                  }
                } else {
                  // Optimization: the block is not lit or in the sky, the background is already cleared to black
                  continue
                }
              }
            } else {
              // No light, no problem
              val rotation = if (isStair) NO_ROTATION else calculateRotation(chunk, localX, localY)
              if (secondaryTexture != null) {
                drawRotatedTexture(secondaryTexture, dx, dy, rotation)
              }
              if (isStair) {
                drawStairForeground(texture, dx, dy, shape)
              } else {
                drawRotatedTexture(texture, dx, dy, rotation)
              }
            }
          }
        }

        // --- Outline pass: darken collidable block edges facing non-collidable blocks by 50% ---
        batch.flush()
        Gdx.gl.glBlendEquation(outlineFunc.gl)
        try {
          batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE)
          val whiteRegion = assets.caveTexture.textureRegion
          val highlightPercent = Settings.outlineHighlightPercent
          batch.withColor(highlightPercent, highlightPercent, highlightPercent, 0f, tmpColor) {
            for (localX in 0 until Chunk.CHUNK_SIZE) {
              for (localY in 0 until Chunk.CHUNK_SIZE) {
                val block = chunk.getRawBlock(localX, localY) ?: continue
                if (block.isCollidable()) continue

                if (Settings.renderLight) {
                  val blockLight = chunk.getBlockLight(localX, localY)
                  if (!blockLight.isLit) continue
                }

                val dx = localX * Block.BLOCK_TEXTURE_SIZE_F
                val dy = localY * Block.BLOCK_TEXTURE_SIZE_F

                if (block.shape.isStair) {
                  drawStairOutline(block, block.shape, whiteRegion, dx, dy)
                  continue
                }

                for (direction in Direction.CARDINAL) {
                  val neighbor = block.getRawRelative(direction, false)
                  if (neighbor.isCollidable()) {
                    val size = 1f
                    when (direction) {
                      Direction.NORTH -> batch.draw(whiteRegion, dx, dy + Block.BLOCK_TEXTURE_SIZE_F - size, Block.BLOCK_TEXTURE_SIZE_F, size)
                      Direction.SOUTH -> batch.draw(whiteRegion, dx, dy, Block.BLOCK_TEXTURE_SIZE_F, size)
                      Direction.EAST -> batch.draw(whiteRegion, dx + Block.BLOCK_TEXTURE_SIZE_F - size, dy, size, Block.BLOCK_TEXTURE_SIZE_F)
                      Direction.WEST -> batch.draw(whiteRegion, dx, dy, size, Block.BLOCK_TEXTURE_SIZE_F)
                      else -> {}
                    }
                  }
                }
              }
            }
          }
        } finally {
          batch.flush()
          Gdx.gl.glBlendEquation(GL20.GL_FUNC_ADD)
        }
      }
    }
  }

  inline fun Block?.isCollidable() = isMarkerBlock() || !materialOrAir().isCollidable

  private fun calculateRotation(chunk: Chunk, localX: LocalCoord, localY: LocalCoord): Int {
    val noise = rotationNoise.getNoisePositive(chunk.chunkX.chunkToWorld(localX), chunk.chunkY.chunkToWorld(localY))
    val cardinalDirections = 4
    val cardinalDirectionDegrees = 90
    return (noise * cardinalDirections).toInt() * cardinalDirectionDegrees
  }

  private fun drawHalfwayTexture(
    caveTexture: RotatableTextureRegion,
    skyTexture: RotatableTextureRegion,
    lights: LightMap,
    dx: Float,
    dy: Float
  ) {
    // Lower half is shaded as it would normally be, otherwise it's a bit too bright
    drawShadedBlock(caveTexture, lights, dx, dy, NO_ROTATION)
    // Overdraw the top half as it should always be lit 100%
    batch.draw(
      skyTexture.textureRegion,
      dx,
      dy + Block.HALF_BLOCK_TEXTURE_SIZE_F,
      Block.BLOCK_TEXTURE_SIZE_F,
      Block.HALF_BLOCK_TEXTURE_SIZE_F
    )
  }

  private fun drawRotatedTexture(texture: RotatableTextureRegion, dx: Float, dy: Float, rotation: Int) {
    if (rotation == NO_ROTATION || !texture.rotationAllowed) {
      batch.draw(texture.textureRegion, dx, dy, Block.BLOCK_TEXTURE_SIZE_F, Block.BLOCK_TEXTURE_SIZE_F)
    } else {
      batch.draw(
        texture.textureRegion,
        dx,
        dy,
        Block.HALF_BLOCK_TEXTURE_SIZE_F,
        Block.HALF_BLOCK_TEXTURE_SIZE_F,
        Block.BLOCK_TEXTURE_SIZE_F,
        Block.BLOCK_TEXTURE_SIZE_F,
        1f,
        1f,
        rotation.toFloat()
      )
    }
  }

  private fun drawShadedBlock(
    textureRegion: RotatableTextureRegion,
    lights: LightMap,
    dx: Float,
    dy: Float,
    rotation: Int,
    shape: BlockShape = BlockShape.FULL
  ) {
    val texture = textureRegion.textureRegion
    val tileWidth = texture.regionWidth / BlockLight.LIGHT_RESOLUTION
    val tileHeight = texture.regionHeight / BlockLight.LIGHT_RESOLUTION
    val split = splitCache.computeIfAbsent(texture) { t: TextureRegion -> t.split(tileWidth, tileHeight) }
    var ry = 0
    val splitLength = split.size
    while (ry < splitLength) {
      val regions = split[BlockLight.LIGHT_RESOLUTION - ry - 1]
      var rx = 0
      val regionsLength = regions.size
      while (rx < regionsLength) {
        if (shape.isStair && shape.isInAirQuadrant(rx, ry, BlockLight.LIGHT_RESOLUTION)) {
          rx++
          continue
        }
        val region = regions[rx]
        val lightMapIndex = BlockLight.lightMapIndex(rx, ry)
        val brightnessR: Brightness = lights.r[lightMapIndex]
        val brightnessG: Brightness = lights.g[lightMapIndex]
        val brightnessB: Brightness = lights.b[lightMapIndex]
        batch.withColor(brightnessR, brightnessG, brightnessB, 1f, tmpColor) {
          if (rotation == NO_ROTATION || !textureRegion.rotationAllowed) {
            batch.draw(
              region,
              dx + rx * LIGHT_SUBBLOCK_SIZE,
              dy + ry * LIGHT_SUBBLOCK_SIZE,
              LIGHT_SUBBLOCK_SIZE,
              LIGHT_SUBBLOCK_SIZE
            )
          } else {
            batch.draw(
              region,
              dx + rx * LIGHT_SUBBLOCK_SIZE,
              dy + ry * LIGHT_SUBBLOCK_SIZE,
              HALF_LIGHT_SUBBLOCK_SIZE,
              HALF_LIGHT_SUBBLOCK_SIZE,
              LIGHT_SUBBLOCK_SIZE,
              LIGHT_SUBBLOCK_SIZE,
              1f,
              1f,
              rotation.toFloat()
            )
          }
        }
        rx++
      }
      ry++
    }
  }

  /**
   * Draw outlines that follow a stair block's L-shape: the half of each cardinal edge on the
   * solid L-shape (only when the corresponding neighbor is non-collidable), plus the two
   * interior edges of the cut quadrant (always — they're a solid/internal-air boundary).
   */
  private fun drawStairOutline(
    block: Block,
    shape: BlockShape,
    whiteRegion: TextureRegion,
    dx: Float,
    dy: Float
  ) {
    val full = Block.BLOCK_TEXTURE_SIZE_F
    val half = Block.HALF_BLOCK_TEXTURE_SIZE_F
    val px = 1f

    fun ifAir(
      direction: Direction,
      x: Float,
      y: Float,
      w: Float,
      h: Float
    ) {
      if (block.getRawRelative(direction, false).isCollidable()) {
        batch.draw(whiteRegion, x, y, w, h)
      }
    }

    when (shape) {
      BlockShape.STAIR_NE -> { // air quadrant: top-right
        ifAir(Direction.SOUTH, dx, dy, full, px)
        ifAir(Direction.WEST, dx, dy, px, full)
        ifAir(Direction.NORTH, dx, dy + full - px, half, px) // left half of top
        ifAir(Direction.EAST, dx + full - px, dy, px, half) // bottom half of right
        batch.draw(whiteRegion, dx + half, dy + half - px, half, px) // top of solid below cut
        batch.draw(whiteRegion, dx + half - px, dy + half, px, half) // right of solid left of cut
      }

      BlockShape.STAIR_NW -> { // air quadrant: top-left
        ifAir(Direction.SOUTH, dx, dy, full, px)
        ifAir(Direction.EAST, dx + full - px, dy, px, full)
        ifAir(Direction.NORTH, dx + half, dy + full - px, half, px) // right half of top
        ifAir(Direction.WEST, dx, dy, px, half) // bottom half of left
        batch.draw(whiteRegion, dx, dy + half - px, half, px) // top of solid below cut
        batch.draw(whiteRegion, dx + half, dy + half, px, half) // left of solid right of cut
      }

      BlockShape.STAIR_SE -> { // air quadrant: bottom-right
        ifAir(Direction.NORTH, dx, dy + full - px, full, px)
        ifAir(Direction.WEST, dx, dy, px, full)
        ifAir(Direction.SOUTH, dx, dy, half, px) // left half of bottom
        ifAir(Direction.EAST, dx + full - px, dy + half, px, half) // top half of right
        batch.draw(whiteRegion, dx + half, dy + half, half, px) // bottom of solid above cut
        batch.draw(whiteRegion, dx + half - px, dy, px, half) // right of solid left of cut
      }

      BlockShape.STAIR_SW -> { // air quadrant: bottom-left
        ifAir(Direction.NORTH, dx, dy + full - px, full, px)
        ifAir(Direction.EAST, dx + full - px, dy, px, full)
        ifAir(Direction.SOUTH, dx + half, dy, half, px) // right half of bottom
        ifAir(Direction.WEST, dx, dy + half, px, half) // top half of left
        batch.draw(whiteRegion, dx, dy + half, half, px) // bottom of solid above cut
        batch.draw(whiteRegion, dx + half, dy, px, half) // left of solid right of cut
      }

      BlockShape.FULL -> {} // not a stair; outline pass uses the cardinal-direction loop
    }
  }

  /**
   * Unlit foreground draw for a stair block: split the texture into a 2x2 grid and draw the
   * 3 quadrants that are NOT cut away. Mirrors [drawRotatedTexture] but with a stair cutout.
   */
  private fun drawStairForeground(textureRegion: RotatableTextureRegion, dx: Float, dy: Float, shape: BlockShape) {
    val texture = textureRegion.textureRegion
    val halfW = texture.regionWidth / 2
    val halfH = texture.regionHeight / 2
    val split = stairSplitCache.computeIfAbsent(texture) { t: TextureRegion -> t.split(halfW, halfH) }
    // split[0] is the TOP row (libGDX TextureRegion.split convention), split[1] is the bottom.
    for (qy in 0..1) {
      val regions = split[1 - qy] // qy=0 (bottom) -> split[1], qy=1 (top) -> split[0]
      for (qx in 0..1) {
        if (shape.isInAirQuadrant(qx * 2, qy * 2, 4)) continue
        batch.draw(
          regions[qx],
          dx + qx * Block.HALF_BLOCK_TEXTURE_SIZE_F,
          dy + qy * Block.HALF_BLOCK_TEXTURE_SIZE_F,
          Block.HALF_BLOCK_TEXTURE_SIZE_F,
          Block.HALF_BLOCK_TEXTURE_SIZE_F
        )
      }
    }
  }

  val Block.texture: RotatableTextureRegion?
    get() {
      val map = EnumMap<Direction, Material>(Direction::class.java)
      for (direction in Direction.CARDINAL) {
        val relMat = this.getRawRelative(direction, false).materialOrAir()
        map[direction] = relMat
      }
      return TextureNeighbor.getTexture(material, map)
    }

  override fun dispose() {
    batch.dispose()
  }

  companion object {
    const val LIGHT_SUBBLOCK_SIZE = Block.BLOCK_TEXTURE_SIZE_F / BlockLight.LIGHT_RESOLUTION
    const val HALF_LIGHT_SUBBLOCK_SIZE = LIGHT_SUBBLOCK_SIZE * 0.5f

    const val NO_ROTATION = 0
  }
}
