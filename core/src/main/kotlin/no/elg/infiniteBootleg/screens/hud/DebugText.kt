package no.elg.infiniteBootleg.screens.hud

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.Gdx
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.util.chunkOffset
import no.elg.infiniteBootleg.util.compactLoc
import no.elg.infiniteBootleg.util.fastIntFormat
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.blocks.Block.Companion.materialOrAir
import no.elg.infiniteBootleg.world.blocks.BlockLight.Companion.NO_LIGHTS_LIGHT_MAP
import no.elg.infiniteBootleg.world.blocks.BlockLight.Companion.SKYLIGHT_LIGHT_MAP
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.chunks.ChunkImpl
import no.elg.infiniteBootleg.world.ecs.components.GroundedComponent.Companion.groundedComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.velocityComponent
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.tags.FlyingTag.Companion.flying
import no.elg.infiniteBootleg.world.generator.chunk.PerlinChunkGenerator
import no.elg.infiniteBootleg.world.render.ChunkRenderer.Companion.LIGHT_RESOLUTION
import no.elg.infiniteBootleg.world.world.ClientWorld
import java.text.DecimalFormat

object DebugText {

  val dotFourFormat = DecimalFormat("0.0000")

  inline fun fpsString(sb: StringBuilder, world: ClientWorld?) {
    val worldTps = world?.worldTicker?.realTPS ?: -1
    val physicsTps = world?.worldTicker?.box2DTicker?.ticker?.realTPS ?: -1

    sb.append("FPS: ").fastIntFormat(Gdx.graphics.framesPerSecond, 4)
      .append(" | delta: ").append(dotFourFormat.format(Gdx.graphics.deltaTime.toDouble())).append(" ms")
      .append(" | wtps: ").fastIntFormat(worldTps.toInt(), 2)
      .append(" | ptps: ").fastIntFormat(physicsTps.toInt(), 2)
  }

  fun lights(sb: StringBuilder, world: ClientWorld, mouseBlockX: Int, mouseBlockY: Int) {
    val localX = mouseBlockX.chunkOffset()
    val localY = mouseBlockY.chunkOffset()

    fun calcSubCell(coord: Float): Int {
      val fixedCoord = if (coord < 0f) 1f - (-coord % 1f) else coord % 1f
      return ((fixedCoord % 1f) * LIGHT_RESOLUTION).toInt()
    }

    val rawX = calcSubCell(ClientMain.inst().mouseLocator.mouseWorldX)
    val rawY = calcSubCell(ClientMain.inst().mouseLocator.mouseWorldY)

    val chunk = world.getChunk(compactLoc(mouseBlockX.worldToChunk(), mouseBlockY.worldToChunk()), false)
    val blockLight = chunk?.getBlockLight(localX, localY)

    val isLit = blockLight?.isLit ?: "maybe"
    val skylight = blockLight?.isSkylight ?: "maybe"
    val usingSkyArr = blockLight?.lightMap === SKYLIGHT_LIGHT_MAP
    val usingNoLigArr = blockLight?.lightMap === NO_LIGHTS_LIGHT_MAP
    val avg = blockLight?.averageBrightness ?: 0.0f
    val sub = blockLight?.lightMap?.getOrNull(rawX)?.getOrNull(rawY) ?: 0.0
    val format = " lit? %-5s (using no light arr? %-5s) sky? %-5s (using sky arr? %-5s) avg brt %1.3f sub-cell[%1d, %1d] %1.3f"
    sb.append(String.format(format, isLit, usingNoLigArr, skylight, usingSkyArr, avg, rawX, rawY, sub))
  }

  fun pointing(sb: StringBuilder, world: ClientWorld, mouseBlockX: Int, mouseBlockY: Int) {
    val localX = mouseBlockX.chunkOffset()
    val localY = mouseBlockY.chunkOffset()
    val chunk = world.getChunk(compactLoc(mouseBlockX.worldToChunk(), mouseBlockY.worldToChunk()), false)
    val block = chunk?.getRawBlock(localX, localY)
    val material = block.materialOrAir()
    val rawX = ClientMain.inst().mouseLocator.mouseWorldX
    val rawY = ClientMain.inst().mouseLocator.mouseWorldY
    val exists = block != null
    val blockDebug = block?.hudDebug() ?: ""

    val format = "Pointing at %-5s (% 8.2f,% 8.2f) block (% 5d,% 5d) exists? %-5s %s"
    sb.append(String.format(format, material, rawX, rawY, mouseBlockX, mouseBlockY, exists, blockDebug))
  }

  fun chunk(sb: StringBuilder, world: ClientWorld, mouseBlockX: Int, mouseBlockY: Int) {
    val chunkX = mouseBlockX.worldToChunk()
    val chunkY = mouseBlockY.worldToChunk()
    val pc = world.getChunk(chunkX, chunkY, false)
    val cc = world.getChunkColumn(chunkX)
    val topBlock = cc.topBlockHeight(mouseBlockX.chunkOffset())
    if (pc == null) {
      val format = "chunk (% 4d,% 4d) [top block %2d]: <not loaded>"
      sb.append(String.format(format, chunkX, chunkY, topBlock))
    } else {
      val generator = world.chunkLoader.generator
      val biome = generator.getBiome(mouseBlockX)
      val biomeHeight = if (generator is PerlinChunkGenerator) generator.getBiomeHeight(mouseBlockX) else 0f
      val allAir = pc.isAllAir
      val modified = pc.shouldSave()
      val allowUnloading = pc.isAllowedToUnload
      val skychunk = cc.isChunkAboveTopBlock(chunkY)
      val upId = if (pc is ChunkImpl) pc.currentUpdateId.get() else -1
      val format = "chunk (% 4d,% 4d) [top % 4d]: type: %-9.9s|noise % .2f|all air?%-5b|can unload?%-5b|sky?%-5b|light id% 3d|modified?%-5b"
      sb.append(String.format(format, chunkX, chunkY, topBlock, biome, biomeHeight, allAir, allowUnloading, skychunk, upId, modified))
    }
  }

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

  fun pos(sb: StringBuilder, players: ImmutableArray<Entity>) {
    if (players.size() == 0) {
      sb.append("No player")
      return
    }
    val player = players.first()
    val velocity = player.velocityComponent
    val position = player.positionComponent
    val grounded = player.groundedComponentOrNull
    val onGround = grounded?.onGround ?: false
    val canMoveLeft = grounded?.canMoveLeft ?: false
    val canMoveRight = grounded?.canMoveRight ?: false
    val feetContacts = grounded?.feetContacts?.size ?: 0
    val leftArmContacts = grounded?.leftArmContacts?.size ?: 0
    val rightArmContacts = grounded?.rightArmContacts?.size ?: 0
    val flying = player.flying
    val format = "p: (% 8.2f,% 8.2f) v: (% 8.2f,% 8.2f) php: (% 8.2f,% 8.2f) g? %-5b (%-5b <> %-5b) (%d + (%d + %d)) f? %-5b"
    sb.append(
      String.format(
        format,
        position.x,
        position.y,
        velocity.dx,
        velocity.dy,
        0f, 0f,
        onGround,
        canMoveLeft,
        canMoveRight,
        feetContacts,
        leftArmContacts,
        rightArmContacts,
        flying
      )
    )
  }

  fun ents(sb: StringBuilder, world: ClientWorld, mouseWorldX: Float, mouseWorldY: Float) {
    sb.append("E = \n")
    for (entity in world.getEntities(mouseWorldX, mouseWorldY)) {
      sb.append("${entity.id}: ${entity.components.map { it.javaClass.simpleName.removeSuffix("Component") }.sorted()}").appendLine()
    }
  }
}
