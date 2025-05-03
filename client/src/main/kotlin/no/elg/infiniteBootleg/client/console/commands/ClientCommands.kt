package no.elg.infiniteBootleg.client.console.commands

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.strongjoshua.console.annotation.ConsoleDoc
import com.strongjoshua.console.annotation.HiddenCommand
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import ktx.async.schedule
import no.elg.infiniteBootleg.client.inventory.container.closeContainer
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.screens.ConnectingScreen.info
import no.elg.infiniteBootleg.client.screens.HUDRenderer
import no.elg.infiniteBootleg.client.screens.MainMenuScreen
import no.elg.infiniteBootleg.client.screens.SelectWorldScreen.loadSingleplayerWorld
import no.elg.infiniteBootleg.client.screens.WorldScreen
import no.elg.infiniteBootleg.client.world.world.ClientWorld
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.console.CmdArgNames
import no.elg.infiniteBootleg.core.console.commands.CommonCommands
import no.elg.infiniteBootleg.core.inventory.container.Container
import no.elg.infiniteBootleg.core.inventory.container.ContainerOwner
import no.elg.infiniteBootleg.core.inventory.container.OwnedContainer
import no.elg.infiniteBootleg.core.inventory.container.impl.AutoSortedContainer
import no.elg.infiniteBootleg.core.inventory.container.impl.ContainerImpl
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.net.ServerClient.Companion.sendServerBoundPacket
import no.elg.infiniteBootleg.core.net.serverBoundClientDisconnectPacket
import no.elg.infiniteBootleg.core.util.ReflectionUtil
import no.elg.infiniteBootleg.core.util.asWorldSeed
import no.elg.infiniteBootleg.core.util.chunkToWorld
import no.elg.infiniteBootleg.core.util.launchOnAsync
import no.elg.infiniteBootleg.core.util.launchOnMain
import no.elg.infiniteBootleg.core.util.stringifyCompactLoc
import no.elg.infiniteBootleg.core.util.toAbled
import no.elg.infiniteBootleg.core.util.worldToChunk
import no.elg.infiniteBootleg.core.world.ContainerElement
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.Tool
import no.elg.infiniteBootleg.core.world.chunks.Chunk.Companion.CHUNK_SIZE
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.core.world.ecs.components.LocallyControlledComponent.Companion.locallyControlledComponent
import no.elg.infiniteBootleg.core.world.ecs.components.NameComponent.Companion.nameOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.ContainerComponent
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.ContainerComponent.Companion.containerComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.ContainerComponent.Companion.containerOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.ContainerComponent.Companion.ownedContainerOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.teleport
import no.elg.infiniteBootleg.core.world.ecs.components.tags.FlyingTag.Companion.flying
import no.elg.infiniteBootleg.core.world.ecs.components.tags.IgnorePlaceableCheckTag.Companion.ignorePlaceableCheck
import no.elg.infiniteBootleg.core.world.render.WorldRender.Companion.MAX_ZOOM
import no.elg.infiniteBootleg.core.world.render.WorldRender.Companion.MIN_ZOOM
import no.elg.infiniteBootleg.core.world.world.World.Companion.LIGHT_SOURCE_LOOK_BLOCKS
import java.util.Locale

private val logger = KotlinLogging.logger {}

@Suppress("unused")
class ClientCommands : CommonCommands() {

  private val clientWorld: ClientWorld?
    get() = ClientMain.inst().world ?: run {
      logger.error { "Failed to find the current client world" }
      null
    }

  @HiddenCommand
  fun ga() = giveAll()

  @CmdArgNames("quantity")
  @ConsoleDoc(description = "Give one of each element to player", paramDescriptions = ["Quantity to give, default 100"])
  fun giveAll(quantity: Int = 100) {
    (Tool.tools + Material.normalMaterials).forEach { give(it::class.simpleName!!, quantity) }
  }

  private fun takeOrGive(
    elementName: String,
    quantity: Int,
    task: String,
    antiTask: String,
    func: (Container, ContainerElement) -> Unit
  ) {
    val world = clientWorld ?: return
    val entities = world.controlledPlayerEntities
    if (entities.size() == 0) {
      logger.error { "There is no local, controlled, player in this world" }
      return
    }
    val player = entities.first()
    val container = player.containerOrNull ?: run {
      logger.error { "Player has no container" }
      return
    }

    val element: ContainerElement = ContainerElement.valueOfOrNull(elementName) ?: run {
      logger.error { "Unknown container element '$elementName'" }
      return
    }
    if (quantity < 1) {
      logger.error { "Quantity must be at least 1, use '$antiTask' command to add items" }
      return
    }
    func(container, element)
  }

  @CmdArgNames("item")
  @ConsoleDoc(description = "Give a single item to player", paramDescriptions = ["Item to give"])
  fun give(elementName: String) = give(elementName, 1)

  @CmdArgNames("item")
  @ConsoleDoc(description = "Take a single item from the player", paramDescriptions = ["Item to take"])
  fun take(elementName: String) = take(elementName, 1)

  @CmdArgNames("item", "quantity")
  @ConsoleDoc(description = "Give item to player", paramDescriptions = ["Item to take", "Quantity to give, default 1"])
  fun take(elementName: String, quantity: Int) {
    takeOrGive(elementName, quantity, "take", "give") { container, element ->
      val notRemoved = container.remove(element, quantity.toUInt())
      if (notRemoved == 0u) {
        logger.info { "Took $quantity ${element.displayName} from player" }
      } else {
        logger.info {
          val taken = quantity - notRemoved.toInt()
          "Failed to take $notRemoved ${element.displayName} from player, took $taken ${element.displayName} instead"
        }
      }
    }
  }

  @CmdArgNames("item", "quantity")
  @ConsoleDoc(description = "Give item to player", paramDescriptions = ["Item to give", "Quantity to give, default 1"])
  fun give(elementName: String, quantity: Int) {
    takeOrGive(elementName, quantity, "give", "take") { container, element ->
      val item = element.toItem(stock = quantity.toUInt())
      val notAdded = container.add(item)
      if (notAdded.isEmpty()) {
        logger.info { "Gave player ${item.stock} ${element.displayName}" }
      } else {
        logger.error {
          val notGiven = notAdded.sumOf { it.stock }
          val given = item.stock - notGiven
          if (given == 0u) {
            "Failed to give player ${item.stock} ${element.displayName}"
          } else {
            "Managed to give player $given ${element.displayName}, failed to find room for $notGiven ${element.displayName}"
          }
        }
      }
    }
  }

  @CmdArgNames("color")
  @ConsoleDoc(description = "Set the color of the sky", paramDescriptions = ["Name of color"])
  fun skyColor(colorName: String) {
    val world = world ?: return
    val skylight = world.worldTime.baseColor
    try {
      val color = ReflectionUtil.getStaticField(Color::class.java, colorName.uppercase(Locale.getDefault())) as Color
      skylight.set(color)
      logger.info { "Sky color changed to ${colorName.lowercase()} ($color)" }
    } catch (e: Exception) {
      logger.error { "Unknown color '$colorName'" }
    }
  }

  @ConsoleDoc(description = "Toggle rendering of lights")
  fun lights() {
    Settings.renderLight = !Settings.renderLight
    val world = world ?: return

    world.readChunks { readableChunks ->
      readableChunks.values().forEach {
        if (Settings.renderLight) {
          it.updateAllBlockLights()
        }
        it.dirty()
      }
    }
    logger.info {
      "Lighting is now ${Settings.renderLight.toAbled()}"
    }
  }

  @ConsoleDoc(description = "Toggle flight for player")
  fun fly() {
    val world = clientWorld ?: return
    val entities = world.controlledPlayerEntities
    if (entities.size() == 0) {
      logger.info { "There is no local, controlled player in this world" }
    }
    for (entity in entities) {
      val wasFlying: Boolean = entity.flying
      entity.flying = !wasFlying
      val box2DBodyComponent: Box2DBodyComponent = entity.box2d
      if (wasFlying) {
        box2DBodyComponent.enableGravity()
      } else {
        box2DBodyComponent.disableGravity()
      }
      logger.info { "Player is now ${if (wasFlying) "not " else ""}flying" }
    }
  }

  @ConsoleDoc(description = "Toggles debug rendering of Box2D objects")
  fun debBox() {
    Settings.renderBox2dDebug = !Settings.renderBox2dDebug
    logger.info {
      "Debug rendering for Box2D is now ${Settings.renderBox2dDebug.toAbled()}"
    }
  }

  @ConsoleDoc(description = "Toggle drawBodies for box2d debug rendering")
  fun drawBodies() {
    val world = clientWorld ?: return
    val box2dDebugRenderer = world.render.box2DDebugRenderer
    box2dDebugRenderer.isDrawBodies = !box2dDebugRenderer.isDrawBodies
    logger.info { "Box2D debug draw Bodies is ${box2dDebugRenderer.isDrawBodies.toAbled()}" }
  }

  @ConsoleDoc(description = "Toggle drawJoints for box2d debug rendering")
  fun drawJoints() {
    val world = clientWorld ?: return
    val box2dDebugRenderer = world.render.box2DDebugRenderer
    box2dDebugRenderer.isDrawJoints = !box2dDebugRenderer.isDrawJoints
    logger.info { "Box2D debug draw Joints is ${box2dDebugRenderer.isDrawJoints.toAbled()}" }
  }

  @ConsoleDoc(description = "Toggle drawAABBs for box2d debug rendering")
  fun drawAABBs() {
    val world = clientWorld ?: return
    val box2dDebugRenderer = world.render.box2DDebugRenderer
    box2dDebugRenderer.isDrawAABBs = !box2dDebugRenderer.isDrawAABBs
    logger.info { "Box2D debug draw AABBs is ${box2dDebugRenderer.isDrawAABBs.toAbled()}" }
  }

  @ConsoleDoc(description = "Toggle drawInactiveBodies for box2d debug rendering")
  fun drawInactiveBodies() {
    val world = clientWorld ?: return
    val box2dDebugRenderer = world.render.box2DDebugRenderer
    box2dDebugRenderer.isDrawInactiveBodies = !box2dDebugRenderer.isDrawInactiveBodies
    logger.info { "Box2D debug draw InactiveBodies is ${box2dDebugRenderer.isDrawInactiveBodies.toAbled()}" }
  }

  @ConsoleDoc(description = "Toggle drawVelocities for box2d debug rendering")
  fun drawVelocities() {
    val world = clientWorld ?: return
    val box2dDebugRenderer = world.render.box2DDebugRenderer
    box2dDebugRenderer.isDrawVelocities = !box2dDebugRenderer.isDrawVelocities
    logger.info { "Box2D debug draw Velocities is ${box2dDebugRenderer.isDrawVelocities.toAbled()}" }
  }

  @ConsoleDoc(description = "Toggle drawContacts for box2d debug rendering")
  fun drawContacts() {
    val world = clientWorld ?: return
    val box2dDebugRenderer = world.render.box2DDebugRenderer
    box2dDebugRenderer.isDrawContacts = !box2dDebugRenderer.isDrawContacts
    logger.info { "Box2D debug draw Contacts is ${box2dDebugRenderer.isDrawContacts.toAbled()}" }
  }

  @ConsoleDoc(description = "Toggles debug rendering of chunk bounds")
  fun renderChunkBorders() {
    Settings.renderChunkBounds = !Settings.renderChunkBounds
    logger.info { "Debug rendering of chunks is now ${Settings.renderChunkBounds.toAbled()}" }
  }

  @ConsoleDoc(description = "Toggles debug rendering of chunk updates")
  fun debChuUpd() {
    Settings.renderChunkUpdates = !Settings.renderChunkUpdates
    logger.info { "Debug rendering of chunk updates is now ${Settings.renderChunkUpdates.toAbled()}" }
  }

  @ConsoleDoc(description = "Toggles debug rendering of block light updates")
  fun debLitUpd() {
    Settings.renderBlockLightUpdates = !Settings.renderBlockLightUpdates
    logger.info { "Debug rendering of block light updates is now ${Settings.renderBlockLightUpdates.toAbled()}" }
  }

  @ConsoleDoc(description = "Toggles debug rendering of entity lighting")
  fun debEntLit() {
    Settings.debugEntityLight = !Settings.debugEntityLight
    logger.info {
      "Debug rendering of entity light is now ${Settings.debugEntityLight.toAbled()}"
    }
    if (Settings.debugEntityLight) {
      logger.info { "A white box is rendered over the block each entity source their brightness from" }
    }
  }

  @ConsoleDoc(description = "Toggles debug rendering of block lighting")
  fun debBlkLit() {
    Settings.debugBlockLight = !Settings.debugBlockLight
    logger.info {
      "Debug rendering of block light is now ${Settings.debugBlockLight.toAbled()}"
    }
    if (Settings.debugBlockLight) {
      logger.info { "A red box is rendered over the luminescent blocks and a yellow box represents the skylight each block source their brightness from" }
    }
  }

  @ConsoleDoc(description = "Toggles smoothed camera movement when following a player")
  fun lerp() {
    Settings.enableCameraFollowLerp = !Settings.enableCameraFollowLerp
    logger.info {
      "Camera leap is now " + Settings.enableCameraFollowLerp.toAbled()
    }
  }

  @CmdArgNames("x", "y")
  @ConsoleDoc(description = "Teleport to given world coordinate", paramDescriptions = ["World x coordinate", "World y coordinate"])
  fun tp(worldX: Float, worldY: Float) {
    val clientWorld = clientWorld ?: return
    launchOnAsync {
      val entities = clientWorld.controlledPlayerEntities
      if (entities.size() > 0) {
        world?.loadChunk(worldX.worldToChunk(), worldY.worldToChunk())
        clientWorld.render.lookAt(worldX, worldY) // Do not lerp when teleporting
        entities.forEach { it.teleport(worldX, worldY, killVelocity = true) }
        logger.info { "Teleported entity to ${stringifyCompactLoc(worldX, worldY)}" }
      }
    }
  }

  @ConsoleDoc(description = "Teleport the camera to the pointer location")
  fun lookAt() {
    val mouseLocator = ClientMain.inst().mouseLocator
    lookAt(mouseLocator.mouseWorldX, mouseLocator.mouseWorldY)
  }

  @CmdArgNames("x", "y")
  @ConsoleDoc(description = "Teleport the camera to given world coordinate", paramDescriptions = ["World x coordinate", "World y coordinate"])
  fun lookAt(worldX: Float, worldY: Float) {
    val clientWorld = clientWorld ?: return
    launchOnAsync {
      clientWorld.render.lookAt(worldX, worldY)
      logger.info { "Teleported camera to ${stringifyCompactLoc(worldX, worldY)}" }
    }
  }

  @CmdArgNames("mode")
  @ConsoleDoc(description = "Toggle how much information to show", paramDescriptions = ["block, debug, graph, mindebug, or none"])
  fun hud(modusName: String) {
    val screen = ClientMain.inst().screen
    if (screen !is WorldScreen) {
      logger.error { "Not currently in a world, cannot change hud" }
      return
    }
    val hud: HUDRenderer = screen.hud
    val mode = when (modusName) {
      "block" -> HUDRenderer.DISPLAY_CURRENT_BLOCK
      "mindebug" -> HUDRenderer.DISPLAY_MINIMAL_DEBUG
      "debug" -> HUDRenderer.DISPLAY_DEBUG
      "graph" -> HUDRenderer.DISPLAY_GRAPH_FPS
      "none" -> HUDRenderer.DISPLAY_NOTHING
      else -> -1
    }
    if (mode < 0) {
      logger.error { "Unknown HUD modus '$modusName'" }
    } else if (mode == 0) {
      hud.displayNothing()
    } else {
      hud.toggleMode(mode)
    }
  }

  @CmdArgNames("zoom level")
  @ConsoleDoc(description = "Change the zoom level of the world camera", paramDescriptions = ["The new zoom level must be between $MIN_ZOOM and $MAX_ZOOM"])
  fun zoom(zoom: Float) = zoom(zoom, limit = false)

  @CmdArgNames("zoom level", "limit")
  @ConsoleDoc(
    description = "Change the zoom level of the world camera",
    paramDescriptions = ["The new zoom level must be between $MIN_ZOOM and $MAX_ZOOM (if limited)", "Whether to limit the zoom level"]
  )
  fun zoom(zoom: Float, limit: Boolean) {
    val clientWorld = clientWorld ?: return
    val render = clientWorld.render
    render.camera.zoom = zoom.let {
      if (limit) {
        it.coerceIn(MIN_ZOOM, MAX_ZOOM)
      } else {
        it
      }
    }
    render.update()
    logger.info { "Zoom level is now ${render.camera.zoom}" }
  }

  @ConsoleDoc(description = "Get the brush sizes")
  fun brush() {
    val world = clientWorld ?: return
    val localPlayers = world.controlledPlayerEntities
    if (localPlayers.size() == 0) {
      logger.info { "There is no local, controlled player in this world" }
    }
    for (entity in localPlayers) {
      logger.info { "Brush size for player ${entity.nameOrNull ?: "Unknown"} is ${entity.locallyControlledComponent.brushSize}" }
    }
  }

  @CmdArgNames("size")
  @ConsoleDoc(description = "Set the brush size of the mouse", paramDescriptions = ["New brush size, positive integer"])
  fun brush(size: Float) {
    val world = clientWorld ?: return
    val entities = world.controlledPlayerEntities
    if (entities.size() == 0) {
      logger.error { "There is no local, controlled player in this world" }
    }
    if (size < 1) {
      logger.error { "Brush size must be at least 1" }
      return
    }
    for (entity in entities) {
      entity.locallyControlledComponent.brushSize = size
      logger.info { "New brush size is now $size" }
    }
  }

  @CmdArgNames("interactRadius")
  @ConsoleDoc(description = "Set the interact radius from the player", paramDescriptions = ["New interact radius, positive integer"])
  fun interactRadius(interactRadius: Float) {
    val world = clientWorld ?: return
    val entities = world.controlledPlayerEntities
    if (entities.size() == 0) {
      logger.error { "There is no local, controlled player in this world" }
    }
    if (interactRadius < 1) {
      logger.error { "Interact radius must be at least 1" }
      return
    }
    for (entity in entities) {
      entity.locallyControlledComponent.interactRadius = interactRadius
      logger.info { "New interact radius is now $interactRadius" }
    }
  }

  @ConsoleDoc(description = "Set the interact radius from the player")
  fun instantBreak() {
    val world = clientWorld ?: return
    val entities = world.controlledPlayerEntities
    if (entities.size() == 0) {
      logger.error { "There is no local, controlled player in this world" }
    }
    for (entity in entities) {
      val locallyControlledComponent = entity.locallyControlledComponent
      locallyControlledComponent.instantBreak = !locallyControlledComponent.instantBreak
      logger.info { "Instant break for ${entity.nameOrNull} is now ${(!locallyControlledComponent.instantBreak).toAbled()}" }
    }
  }

  @ConsoleDoc(description = "Toggle whether a player can place blocks disconnected from other blocks")
  fun placeCheck() {
    val world = clientWorld ?: return
    val entities = world.controlledPlayerEntities
    if (entities.size() == 0) {
      logger.error { "There is no local, controlled player in this world" }
    }
    for (entity in entities) {
      val wasIgnoring: Boolean = entity.ignorePlaceableCheck
      entity.ignorePlaceableCheck = !wasIgnoring
      logger.info { "Place check is now ${wasIgnoring.toAbled()}" }
    }
  }

  @ConsoleDoc(description = "Toggle vsync")
  @CmdArgNames("enable")
  fun vsync(enable: Boolean) {
    Gdx.graphics.setVSync(enable)
    logger.info { "VSync is now ${enable.toAbled()}" }
  }

  @ConsoleDoc(description = "Set max FPS, if < 0 there is no limit. ")
  @CmdArgNames("fps")
  fun maxFPS(fps: Int) {
    Gdx.graphics.setForegroundFPS(fps)
    logger.info { "Max foreground fps is now $fps" }
  }

  @ConsoleDoc(description = "Disconnect from the server")
  fun quit() {
    disconnect()
  }

  @ConsoleDoc(description = "Disconnect from the server")
  fun disconnect() {
    ClientMain.inst().serverClient?.sendServerBoundPacket { serverBoundClientDisconnectPacket("Disconnect command") }
    if (Main.isAuthoritative) {
      world?.save()
    }
    info = "Disconnected"
    schedule(50f / 1000f) { ClientMain.inst().screen = MainMenuScreen }
  }

  @ConsoleDoc(description = "Switch inventory of player", paramDescriptions = ["The inventory to use, can be 'creative', 'autosort' or 'container'"])
  fun inv(invType: String) {
    val world = clientWorld ?: return
    val entities = world.controlledPlayerEntities
    if (entities.size() == 0) {
      logger.error { "There is no local, controlled, player in this world" }
      return
    }

    val player = entities.first()
    val oldOwnedContainer = player.ownedContainerOrNull?.also { player.closeContainer() }

    val newContainer: Container = when (invType.lowercase(Locale.getDefault())) {
      "autosort", "as" -> AutoSortedContainer("Auto Sorted Inventory")
      "container", "co" -> ContainerImpl("Inventory")
      else -> {
        logger.error { "Unknown storage type '$invType'" }
        return
      }
    }

    if (oldOwnedContainer != null) {
      val oldContent = oldOwnedContainer.container.content.filterNotNull().toTypedArray()
      newContainer.add(*oldContent)
    }
    val owner = oldOwnedContainer?.owner ?: ContainerOwner.from(player)
    player.containerComponentOrNull = ContainerComponent(OwnedContainer(owner, newContainer))
    logger.info { "New inventory '${newContainer.name}' is ${newContainer::class.simpleName}" }
  }

  fun torchTest(delayMillis: Long = 50) {
    val world = clientWorld ?: return
    val dx = 2
    val dy = 1
    val chunkXs = world.render.chunksInView.run { (horizontalStart + dx).chunkToWorld(0) until (horizontalEnd - dx).chunkToWorld(CHUNK_SIZE) }
    val y = (world.render.chunksInView.verticalEnd - dy).chunkToWorld(0)
    launchOnMain {
      for (x in chunkXs) {
        launchOnAsync {
          world.setBlock(x, y + LIGHT_SOURCE_LOOK_BLOCKS, Material.Sand, prioritize = true)
          world.setBlock(x, y, Material.Torch, prioritize = true)
        }
        delay(delayMillis)
      }
    }
  }

  fun sandTest(delayMillis: Long = 50) {
    val world = clientWorld ?: return
    val dx = 2
    val dy = 1
    val chunkXs = world.render.chunksInView.run { (horizontalStart + dx).chunkToWorld(0) until (horizontalEnd - dx).chunkToWorld(CHUNK_SIZE) }
    val y = (world.render.chunksInView.verticalEnd - dy).chunkToWorld(0)
    launchOnMain {
      for (x in chunkXs) {
        coroutineScope {
          launchOnAsync { world.setBlock(x, y, Material.Sand, prioritize = true) }
        }
        delay(delayMillis)
      }
    }
  }

  @ConsoleDoc(
    description = "Load the given world, this will load a non-transient world",
    paramDescriptions = ["Load a single player world from the given seed"]
  )
  @CmdArgNames("worldSeed")
  fun loadWorld(worldSeed: String) = loadWorld(worldSeed, false)

  @ConsoleDoc(
    description = "Load the given world",
    paramDescriptions = ["Load a single player world from the given seed", "Whether to force the world to be transient"]
  )
  @CmdArgNames("worldSeed", "forceTransient")
  fun loadWorld(worldSeed: String, forceTransient: Boolean) {
    val seed = worldSeed.asWorldSeed()
    if (worldSilent?.seed == seed) {
      logger.error { "Already in the world '$worldSeed' (long: $seed)" }
      return
    }

    ClientMain.inst().screen = MainMenuScreen
    loadSingleplayerWorld(worldSeed.asWorldSeed(), forceTransient = forceTransient)
  }
}
