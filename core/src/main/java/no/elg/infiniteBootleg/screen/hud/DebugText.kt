package no.elg.infiniteBootleg.screen.hud

import com.badlogic.gdx.Gdx
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.util.CoordUtil
import no.elg.infiniteBootleg.util.fastIntFormat
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.ChunkImpl
import no.elg.infiniteBootleg.world.ClientWorld
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.generator.PerlinChunkGenerator
import no.elg.infiniteBootleg.world.subgrid.LivingEntity
import java.text.DecimalFormat

object DebugText {

  val dotFourFormat = DecimalFormat("0.0000")

  @JvmStatic
  inline fun fpsString(sb: StringBuilder, world: ClientWorld?) {
    val worldTps = world?.worldTicker?.realTPS ?: -1
    val physicsTps = world?.box2DTicker?.realTPS ?: -1

    sb.append("FPS: ").fastIntFormat(Gdx.graphics.framesPerSecond, 4)
      .append(" | delta: ").append(dotFourFormat.format(Gdx.graphics.deltaTime.toDouble())).append(" ms")
      .append(" | wtps: ").fastIntFormat(worldTps.toInt(), 2)
      .append(" | ptps: ").fastIntFormat(physicsTps.toInt(), 2)
  }

  @JvmStatic
  fun lights(sb: StringBuilder) {
    sb.append("Active lights threads: ")
    sb.append(-1)
  }

  @JvmStatic
  fun pointing(sb: StringBuilder, world: ClientWorld, mouseBlockX: Int, mouseBlockY: Int) {
    val block = world.getRawBlock(mouseBlockX, mouseBlockY)
    val material = block?.material ?: Material.AIR
    val rawX = ClientMain.inst().mouseWorldX
    val rawY = ClientMain.inst().mouseWorldY
    val exists = block != null
    val blockDebug = block?.hudDebug() ?: ""
    val blockLight = block?.blockLight

    val isLit = blockLight?.isLit ?: "maybe"
    val skylight = blockLight?.isSkylight ?: "maybe"
    val format = "Pointing at %-5s (% 8.2f,% 8.2f) block (% 5d,% 5d) exists? %-5s lit? %-5s sky? %-5s %s"
    sb.append(String.format(format, material, rawX, rawY, mouseBlockX, mouseBlockY, exists, isLit, skylight, blockDebug))
  }

  @JvmStatic
  fun chunk(sb: StringBuilder, world: ClientWorld, mouseBlockX: Int, mouseBlockY: Int) {
    val chunkX = CoordUtil.worldToChunk(mouseBlockX)
    val chunkY = CoordUtil.worldToChunk(mouseBlockY)
    val pc = world.getChunk(chunkX, chunkY)
    val cc = world.getChunkColumn(chunkX)
    val topBlock = cc.topBlockHeight(CoordUtil.chunkOffset(mouseBlockX))
    if (pc == null) {
      val format = "chunk (% 4d,% 4d) [top block %2d]: <not loaded>"
      sb.append(String.format(format, chunkX, chunkY, topBlock))
    } else {
      val generator = world.chunkLoader.generator
      val biome = generator.getBiome(mouseBlockX)
      val biomeHeight = if (generator is PerlinChunkGenerator) generator.getBiomeHeight(mouseBlockX) else 0f
      val allAir = pc.isAllAir
      val allowUnloading = pc.isAllowingUnloading
      val skychunk = cc.isChunkAboveTopBlock(chunkY)
      val upId = if (pc is ChunkImpl) pc.lightUpdaters.get() else -1
      val format = "chunk (% 4d,% 4d) [top %2d]: type: %-9.9s|noise % .2f|all air?%-5b|can unload?%-5b|sky?%-5b|light id% 3d"
      sb.append(String.format(format, chunkX, chunkY, topBlock, biome, biomeHeight, allAir, allowUnloading, skychunk, upId))
    }
  }

  @JvmStatic
  fun time(sb: StringBuilder, world: ClientWorld) {
    val worldTime = world.worldTime
    val format = "time: %.2f (%.2f) scale: %.2f sky brightness: %.2f TOD: %s"
    sb.append(
      String.format(
        format,
        worldTime.time,
        worldTime.normalizedTime(),
        worldTime.timeScale,
        worldTime.skyBrightness,
        worldTime.timeOfDay(worldTime.time)
      )
    )
  }

  @JvmStatic
  fun viewChunk(sb: StringBuilder, world: ClientWorld) {
    val viewingChunks = world.render.chunksInView
    val chunksHor = viewingChunks.horizontalLength
    val chunksVer = viewingChunks.verticalLength
    val chunksInView = chunksHor * chunksVer
    val blocks = chunksInView * Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE
    val blocksHor = chunksHor * Chunk.CHUNK_SIZE
    val blocksVer = chunksVer * Chunk.CHUNK_SIZE
    val zoom = world.render.camera.zoom
    val format = "Viewing %d chunks (total %d blocks, w %d b, h %d b) with zoom: %.3f"
    sb.append(String.format(format, chunksInView, blocks, blocksHor, blocksVer, zoom))
  }

  @JvmStatic
  fun pos(sb: StringBuilder, player: LivingEntity?) {
    if (player == null) {
      sb.append("No player")
      return
    }
    val velocity = player.velocity
    val position = player.position
    val physicsPosition = player.physicsPosition
    val onGround = player.isOnGround
    val flying = player.isFlying
    val format = "p: (% 8.2f,% 8.2f) v: (% 8.2f,% 8.2f) php: (% 8.2f,% 8.2f) g? %-5b f? %-5b"
    sb.append(
      String.format(
        format,
        position.x,
        position.y,
        velocity.x,
        velocity.y,
        physicsPosition.x,
        physicsPosition.y,
        onGround,
        flying
      )
    )
  }

  @JvmStatic
  fun ents(sb: StringBuilder, world: ClientWorld) {
    val nl = "\n    "
    sb.append("E = ")
    for (entity in world.getEntities(ClientMain.inst().mouseWorldX, ClientMain.inst().mouseWorldY)) {
      sb.append(entity.simpleName()).append("[").append(entity.hudDebug()).append("]").append(nl)
    }
  }
}
