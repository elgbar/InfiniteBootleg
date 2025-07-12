package no.elg.infiniteBootleg.client.screens.hud

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.Gdx
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.world.render.ChunkRenderer
import no.elg.infiniteBootleg.client.world.textureRegion
import no.elg.infiniteBootleg.client.world.world.ClientWorld
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.chunkOffset
import no.elg.infiniteBootleg.core.util.compactInt
import no.elg.infiniteBootleg.core.util.fastIntFormat
import no.elg.infiniteBootleg.core.util.toComponentsString
import no.elg.infiniteBootleg.core.util.worldToChunk
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.materialOrAir
import no.elg.infiniteBootleg.core.world.blocks.BlockLight.Companion.LIGHT_RESOLUTION
import no.elg.infiniteBootleg.core.world.blocks.BlockLight.Companion.NO_LIGHTS_LIGHT_MAP
import no.elg.infiniteBootleg.core.world.blocks.BlockLight.Companion.SKYLIGHT_LIGHT_MAP
import no.elg.infiniteBootleg.core.world.blocks.BlockLight.Companion.lightMapIndex
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.chunks.ChunkColumn.Companion.FeatureFlag
import no.elg.infiniteBootleg.core.world.chunks.TexturedChunk
import no.elg.infiniteBootleg.core.world.ecs.components.GroundedComponent.Companion.groundedComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.velocityComponent
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.HotbarComponent.Companion.selectedItem
import no.elg.infiniteBootleg.core.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.tags.FlyingTag.Companion.flying
import no.elg.infiniteBootleg.core.world.generator.chunk.PerlinChunkGenerator
import no.elg.infiniteBootleg.core.world.world.World
import java.text.DecimalFormat

object DebugText {

  private val dotFourFormat = DecimalFormat("0.0000")

  fun fpsString(sb: StringBuilder, world: ClientWorld?) {
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

    val chunk = world.getChunk(compactInt(mouseBlockX.worldToChunk(), mouseBlockY.worldToChunk()), false)
    val blockLight = chunk?.getBlockLight(localX, localY)

    val isLit = blockLight?.isLit ?: "maybe"
    val skylight = blockLight?.isSkylight ?: "maybe"
    val usingSkyArr = blockLight?.lightMap === SKYLIGHT_LIGHT_MAP
    val usingNoLigArr = blockLight?.lightMap === NO_LIGHTS_LIGHT_MAP
    val avg = blockLight?.averageBrightness ?: Float.NaN
    val sub = blockLight?.lightMap?.get(lightMapIndex(rawX, rawY)) ?: Float.NaN
    val format = "lit? %-5s (using no light arr? %-5s) sky? %-5s (using sky arr? %-5s) avg brt %1.3f sub-cell[%1d, %1d] %1.3f"
    sb.append(String.format(format, isLit, usingNoLigArr, skylight, usingSkyArr, avg, rawX, rawY, sub))
  }

  fun pointing(sb: StringBuilder, world: ClientWorld, mouseBlockX: Int, mouseBlockY: Int) {
    val localX = mouseBlockX.chunkOffset()
    val localY = mouseBlockY.chunkOffset()
    val chunkX = mouseBlockX.worldToChunk()
    val chunk = world.getChunk(compactInt(chunkX, mouseBlockY.worldToChunk()), false)
    val block = chunk?.getRawBlock(localX, localY)
    val material = block.materialOrAir()
    val rawX = ClientMain.inst().mouseLocator.mouseWorldX
    val rawY = ClientMain.inst().mouseLocator.mouseWorldY
    val exists = block != null
    val blockDebug = block?.let { " ${it.hudDebug()}" } ?: ""

    val format = "Pointing at %-5s (% 8.2f,% 8.2f) block (% 5d,% 5d) local (% 5d,% 5d) exists? %-5s%s"
    sb.append(String.format(format, material.displayName, rawX, rawY, mouseBlockX, mouseBlockY, localX, localY, exists, blockDebug))

    val blockEntity = block?.entity?.let(::ent) ?: "N/A"
    sb.appendLine().append("Block entity: $blockEntity")
  }

  fun chunk(sb: StringBuilder, world: ClientWorld, mouseBlockX: Int, mouseBlockY: Int) {
    val chunkX = mouseBlockX.worldToChunk()
    val chunkY = mouseBlockY.worldToChunk()
    val pc = world.getChunk(chunkX, chunkY, false)
    val cc = world.getChunkColumn(chunkX)
    val topBlock = cc.topBlockHeight(mouseBlockX.chunkOffset())
    val topBlockSolid = cc.topBlockHeight(mouseBlockX.chunkOffset(), FeatureFlag.SOLID_FLAG)
    val topBlockLight = cc.topBlockHeight(mouseBlockX.chunkOffset(), FeatureFlag.BLOCKS_LIGHT_FLAG)
    if (pc == null) {
      val format = "chunk (% 4d,% 4d) [top/solid/light % 3d/% 3d/% 3d]: <not loaded>"
      sb.append(String.format(format, chunkX, chunkY, topBlock, topBlockSolid, topBlockLight))
    } else {
      val generator = world.chunkLoader.generator
      val biome = generator.getBiome(mouseBlockX)
      val rawHeight = biome.rawHeightAt(world.seed.toInt(), mouseBlockX)
      val biomeHeight = if (generator is PerlinChunkGenerator) generator.getBiomeHeight(mouseBlockX) else 0.0
      val allAir = pc.isAllAir
      val modified = pc.shouldSave()
      val allowUnloading = pc.allowedToUnload
      val skychunk = cc.isChunkAboveTopBlock(chunkY)
      val hasTexture = (pc as? TexturedChunk)?.hasTexture() ?: false
      val format = "chunk (% 4d,% 4d) [top/solid/light % 3d/% 3d/% 3d]: type: %-9.9s|noise % .2f|gheight % .3f|all air?%-5b|can unload?%-5b|sky?%-5b|modified?%-5b|has texture?%-5b"
      sb.append(
        String.format(
          format,
          chunkX,
          chunkY,
          topBlock,
          topBlockSolid,
          topBlockLight,
          biome::class.simpleName,
          biomeHeight,
          rawHeight,
          allAir,
          allowUnloading,
          skychunk,
          modified,
          hasTexture
        )
      )
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
    val holeContacts = grounded?.holeContacts?.size ?: 0
    val leftArmContacts = grounded?.leftArmContacts?.size ?: 0
    val rightArmContacts = grounded?.rightArmContacts?.size ?: 0
    val flying = player.flying
    val holding = player.selectedItem?.element?.textureRegion?.name ?: "N/A"
    sb.append(
      String.format(
        "p: (% 8.2f,% 8.2f) v: (% 8.2f,% 8.2f) php: (% 8.2f,% 8.2f) g? %-5b (%-5b <> %-5b) (g%d h%d l%d r%d)) f? %-5b h %s",
        position.x,
        position.y,
        velocity.dx,
        velocity.dy,
        0f, 0f,
        onGround,
        canMoveLeft,
        canMoveRight,
        feetContacts,
        holeContacts,
        leftArmContacts,
        rightArmContacts,
        flying,
        holding
      )
    )
  }

  fun counters(builder: StringBuilder, world: ClientWorld) {
    builder.append("Chunk reads/writes: ").append(world.chunkReads.get()).append(" / ").append(world.chunkWrites.get())
      .append(" | active listeners: ").append(EventManager.activeListeners.get())
      .append(", added/removed ").append(EventManager.registeredListeners.get()).append(" / ").append(EventManager.unregisteredListeners.get())
      .append(", active 1sh: ").append(EventManager.activeOneTimeRefListeners.get()).append(" | Dispatched events: ")
      .append(EventManager.dispatchedEvents.get()).append(" listened to: ").append(EventManager.listenerListenedToEvent.get())
      .appendLine()
      .append(" > Chunk Rdr Q: ").append(ChunkRenderer.chunksInRenderQueue).append(" | Chunk size avg: ")
      .append((World.CHUNK_ADDED_THREAD_LOCAL.get() - World.CHUNK_REMOVED_THREAD_LOCAL.get()) / World.CHUNK_THREAD_LOCAL.get())
  }

  fun ents(sb: StringBuilder, world: ClientWorld, mouseWorldX: WorldCoord, mouseWorldY: WorldCoord) {
    sb.append("E = \n")
    for (entity in world.getEntities(mouseWorldX, mouseWorldY)) {
      sb.append(ent(entity)).appendLine()
    }
  }

  private fun ent(entity: Entity): String = "${entity.id}: ${entity.toComponentsString()}"
}
