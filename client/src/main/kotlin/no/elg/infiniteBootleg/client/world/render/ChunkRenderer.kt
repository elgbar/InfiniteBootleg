package no.elg.infiniteBootleg.client.world.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import ktx.graphics.use
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.world.render.texture.TextureNeighbor
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.util.chunkToWorld
import no.elg.infiniteBootleg.core.util.getNoisePositive
import no.elg.infiniteBootleg.core.util.isMarkerBlock
import no.elg.infiniteBootleg.core.util.safeUse
import no.elg.infiniteBootleg.core.world.Direction
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.getRawRelative
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.materialOrAir
import no.elg.infiniteBootleg.core.world.blocks.BlockLight
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

  private val checkerboardShader: ShaderProgram = ShaderProgram(CHECKERBOARD_VERT, CHECKERBOARD_FRAG).also {
    require(it.isCompiled) { "Checkerboard shader failed to compile:\n${it.log}" }
  }

  private val splitCache: MutableMap<TextureRegion, Array<Array<TextureRegion>>> = HashMap()

  private val rotationNoise: FastNoiseLite = FastNoiseLite(world.seed.toInt()).also {
    it.setNoiseType(FastNoiseLite.NoiseType.OpenSimplex2)
    it.setFrequency(1.0)
  }

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
      batch.safeUse { _ ->
        for (localX in 0 until Chunk.CHUNK_SIZE) {
          val topLightBlockHeight = chunkColumn.topBlockHeight(
            localX,
            ChunkColumn.Companion.FeatureFlag.BLOCKS_LIGHT_FLAG
          )
          for (localY in 0 until Chunk.CHUNK_SIZE) {
            batch.color = Color.WHITE
            val block = chunk.getRawBlock(localX, localY)
            val material = block.materialOrAir()
            val texture: RotatableTextureRegion
            val secondaryTexture: RotatableTextureRegion?

            val worldY = chunk.chunkY.chunkToWorld(localY)
            val dx = localX * Block.BLOCK_TEXTURE_SIZE_F
            val dy = localY * Block.BLOCK_TEXTURE_SIZE_F

            val isMarker = block.isMarkerBlock()
            if (material.invisibleBlock || isMarker) {
              if (isMarker && topLightBlockHeight == worldY) {
                // Draw half the texture as sky and lower half as cave.
                // This will remove visual artifact when a marker block is falling and updating the top light block height
                drawHalfwayTexture(assets.caveTexture, assets.skyTexture, dx, dy)
                continue
              }
              texture = if (topLightBlockHeight > worldY) assets.caveTexture else assets.skyTexture
              secondaryTexture = null
            } else {
              texture = block?.texture ?: continue
              secondaryTexture = if (material.hasTransparentTexture) {
                if (topLightBlockHeight > worldY) assets.caveTexture else assets.skyTexture
              } else {
                null
              }
            }

            // Visible non-collidable blocks have their main texture deferred to pass 2 (checkerboard shader)
            val deferMainTexture = !material.isCollidable && !material.invisibleBlock && !isMarker

            val blockLight = chunk.getBlockLight(localX, localY)
            if (Settings.renderLight) {
              if (blockLight.isLit && (!blockLight.isSkylight || texture.rotationAllowed)) {
                val rotation = calculateRotation(chunk, localX, localY)
                if (secondaryTexture != null) {
                  drawShadedBlock(secondaryTexture, blockLight.lightMap, dx, dy, rotation)
                }
                if (!deferMainTexture) {
                  drawShadedBlock(texture, blockLight.lightMap, dx, dy, rotation)
                }
              } else {
                if (blockLight.isLit) {
                  val rotation = calculateRotation(chunk, localX, localY)
                  if (secondaryTexture != null) {
                    drawRotatedTexture(secondaryTexture, dx, dy, rotation)
                  }
                  if (!deferMainTexture) {
                    drawRotatedTexture(texture, dx, dy, rotation)
                  }
                } else {
                  // Optimization: the block is not lit or in the sky, the background is already cleared to black
                  continue
                }
              }
            } else {
              // No light, no problem
              val rotation = calculateRotation(chunk, localX, localY)
              if (secondaryTexture != null) {
                drawRotatedTexture(secondaryTexture, dx, dy, rotation)
              }
              if (!deferMainTexture) {
                drawRotatedTexture(texture, dx, dy, rotation)
              }
            }
          }
        }

        // --- Pass 2: non-collidable block textures with checkerboard shader ---
        batch.flush()
        batch.shader = checkerboardShader
        for (localX in 0 until Chunk.CHUNK_SIZE) {
          for (localY in 0 until Chunk.CHUNK_SIZE) {
            val block = chunk.getRawBlock(localX, localY) ?: continue
            val material = block.materialOrAir()
            if (material.isCollidable || material.invisibleBlock || block.isMarkerBlock()) continue

            val texture = block.texture ?: continue
            val blockLight = chunk.getBlockLight(localX, localY)

            if (Settings.renderLight && !blockLight.isLit) continue

            batch.color = Color.WHITE
            val dx = localX * Block.BLOCK_TEXTURE_SIZE_F
            val dy = localY * Block.BLOCK_TEXTURE_SIZE_F
            val rotation = calculateRotation(chunk, localX, localY)

            if (Settings.renderLight) {
              if (!blockLight.isSkylight || texture.rotationAllowed) {
                drawShadedBlock(texture, blockLight.lightMap, dx, dy, rotation)
              } else {
                drawRotatedTexture(texture, dx, dy, rotation)
              }
            } else {
              drawRotatedTexture(texture, dx, dy, rotation)
            }
          }
        }
        batch.flush()
        batch.shader = null

        // --- Pass 3: outlines on collidable block edges facing non-collidable blocks ---
        val whiteRegion = assets.whiteTexture.textureRegion
        for (localX in 0 until Chunk.CHUNK_SIZE) {
          for (localY in 0 until Chunk.CHUNK_SIZE) {
            val block = chunk.getRawBlock(localX, localY) ?: continue
            if (!block.materialOrAir().isCollidable) continue

            if (Settings.renderLight) {
              val blockLight = chunk.getBlockLight(localX, localY)
              if (!blockLight.isLit) continue
            }

            val dx = localX * Block.BLOCK_TEXTURE_SIZE_F
            val dy = localY * Block.BLOCK_TEXTURE_SIZE_F

            batch.setColor(0f, 0f, 0f, 1f)
            for (direction in Direction.CARDINAL) {
              val neighbor = block.getRawRelative(direction, false)
              if (!neighbor.materialOrAir().isCollidable) {
                when (direction) {
                  Direction.NORTH -> batch.draw(whiteRegion, dx, dy + Block.BLOCK_TEXTURE_SIZE_F - 1f, Block.BLOCK_TEXTURE_SIZE_F, 1f)
                  Direction.SOUTH -> batch.draw(whiteRegion, dx, dy, Block.BLOCK_TEXTURE_SIZE_F, 1f)
                  Direction.EAST -> batch.draw(whiteRegion, dx + Block.BLOCK_TEXTURE_SIZE_F - 1f, dy, 1f, Block.BLOCK_TEXTURE_SIZE_F)
                  Direction.WEST -> batch.draw(whiteRegion, dx, dy, 1f, Block.BLOCK_TEXTURE_SIZE_F)
                  else -> {}
                }
              }
            }
          }
        }
      }
    }
  }

  private fun calculateRotation(chunk: Chunk, localX: LocalCoord, localY: LocalCoord): Int {
    val noise = rotationNoise.getNoisePositive(chunk.chunkX.chunkToWorld(localX), chunk.chunkY.chunkToWorld(localY))
    val cardinalDirections = 4
    val cardinalDirectionDegrees = 90
    return (noise * cardinalDirections).toInt() * cardinalDirectionDegrees
  }

  private fun drawHalfwayTexture(lowerHalf: RotatableTextureRegion, upperHalf: RotatableTextureRegion, dx: Float, dy: Float) {
    batch.draw(
      upperHalf.textureRegion,
      dx,
      dy + Block.HALF_BLOCK_TEXTURE_SIZE_F,
      Block.BLOCK_TEXTURE_SIZE_F,
      Block.HALF_BLOCK_TEXTURE_SIZE_F
    )
    batch.draw(lowerHalf.textureRegion, dx, dy, Block.BLOCK_TEXTURE_SIZE_F, Block.HALF_BLOCK_TEXTURE_SIZE_F)
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
    rotation: Int
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
        val region = regions[rx]
        val lightMapIndex = BlockLight.lightMapIndex(rx, ry)
        val brightnessR: Brightness = lights.r[lightMapIndex]
        val brightnessG: Brightness = lights.g[lightMapIndex]
        val brightnessB: Brightness = lights.b[lightMapIndex]
        batch.setColor(brightnessR, brightnessG, brightnessB, 1f)
        if (textureRegion.rotationAllowed || rotation == 0) {
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
        } else {
          batch.draw(
            region,
            dx + rx * LIGHT_SUBBLOCK_SIZE,
            dy + ry * LIGHT_SUBBLOCK_SIZE,
            LIGHT_SUBBLOCK_SIZE,
            LIGHT_SUBBLOCK_SIZE
          )
        }
        rx++
      }
      ry++
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
    checkerboardShader.dispose()
  }

  companion object {
    const val LIGHT_SUBBLOCK_SIZE = Block.BLOCK_TEXTURE_SIZE_F / BlockLight.LIGHT_RESOLUTION
    const val HALF_LIGHT_SUBBLOCK_SIZE = LIGHT_SUBBLOCK_SIZE * 0.5f

    const val NO_ROTATION = 0

    // @formatter:off
    private const val CHECKERBOARD_VERT = "" +
      "attribute vec4 a_position;\n" +
      "attribute vec4 a_color;\n" +
      "attribute vec2 a_texCoord0;\n" +
      "uniform mat4 u_projTrans;\n" +
      "varying vec4 v_color;\n" +
      "varying vec2 v_texCoords;\n" +
      "void main() {\n" +
      "  v_color = a_color;\n" +
      "  v_color.a = v_color.a * (255.0/254.0);\n" +
      "  v_texCoords = a_texCoord0;\n" +
      "  gl_Position = u_projTrans * a_position;\n" +
      "}\n"

    private const val CHECKERBOARD_FRAG = "" +
      "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP\n" +
      "#endif\n" +
      "varying LOWP vec4 v_color;\n" +
      "varying vec2 v_texCoords;\n" +
      "uniform sampler2D u_texture;\n" +
      "void main() {\n" +
      "  vec4 color = v_color * texture2D(u_texture, v_texCoords);\n" +
      "  if (mod(floor(gl_FragCoord.x) + floor(gl_FragCoord.y), 2.0) < 1.0) {\n" +
      "    discard;\n" +
      "  }\n" +
      "  gl_FragColor = color;\n" +
      "}\n"
    // @formatter:on
  }
}
