package no.elg.infiniteBootleg.screen.hud

import com.badlogic.gdx.Gdx
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.util.CoordUtil
import no.elg.infiniteBootleg.util.fastIntFormat
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.ClientWorld
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.subgrid.LivingEntity
import java.text.DecimalFormat

object DebugText {

  val dotFourFormat = DecimalFormat("0.0000")

  @JvmStatic
  inline fun fpsString(sb: StringBuilder, world: ClientWorld?) {
    val worldTicker = world?.worldTicker
    val tpsDelta = (worldTicker?.tpsDelta ?: 0) / 1_000_000.0
    val realTPS = worldTicker?.realTPS ?: -1

    sb.append("FPS: ").fastIntFormat(Gdx.graphics.framesPerSecond, 4)
      .append(" | delta: ").append(dotFourFormat.format(Gdx.graphics.deltaTime.toDouble())).append(" ms")
      .append(" | tps: ").fastIntFormat(realTPS.toInt(), 2)
      .append(" | tps delta: ").append(dotFourFormat.format(tpsDelta)).append(" ms")
//      .append(" | active threads ").append(Main.inst().scheduler.activeThreads)
  }

  @JvmStatic
  fun lights(sb: StringBuilder, world: ClientWorld) {
    sb.append("Active Lights: ")
    val handler = world.render.rayHandler
    sb.append(handler.enabledLights.size)
  }

  @JvmStatic
  fun pointing(
    sb: StringBuilder,
    world: ClientWorld,
    mouseBlockX: Int,
    mouseBlockY: Int
  ) {
    val block = world.getRawBlock(mouseBlockX, mouseBlockY)
    val material = block?.material ?: Material.AIR
    val rawX = ClientMain.inst().mouseX
    val rawY = ClientMain.inst().mouseY
    val exists = block != null
    val blockDebug = block?.hudDebug() ?: ""
    val format = "Pointing at %-5s (% 8.2f,% 8.2f) block (% 5d,% 5d) exists? %-5b %s"
    sb.append(String.format(format, material, rawX, rawY, mouseBlockX, mouseBlockY, exists, blockDebug))
  }

  @JvmStatic
  fun chunk(
    sb: StringBuilder,
    world: ClientWorld,
    mouseBlockX: Int,
    mouseBlockY: Int
  ) {
    val pc = world.getChunkFromWorld(mouseBlockX, mouseBlockY)
    val chunkY = CoordUtil.worldToChunk(mouseBlockY)
    val chunkX = CoordUtil.worldToChunk(mouseBlockX)
    if (pc == null) {
      val format = "chunk (% 4d,% 4d) : not loaded"
      sb.append(String.format(format, chunkX, chunkY))
    } else {
      val biome = world.chunkLoader.generator.getBiome(mouseBlockX)
      val allAir = pc.isAllAir
      val allowUnloading = pc.isAllowingUnloading
      val format = "chunk (% 4d,% 4d) : type: %-9.9s just air? %-5b can unload? %-5b"
      sb.append(String.format(format, chunkX, chunkY, biome, allAir, allowUnloading))
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
    for (entity in world.getEntities(ClientMain.inst().mouseX, ClientMain.inst().mouseY)) {
      sb.append(entity.simpleName()).append("[").append(entity.hudDebug()).append("]").append(nl)
    }
  }
}
