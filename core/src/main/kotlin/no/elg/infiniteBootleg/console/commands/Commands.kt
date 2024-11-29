package no.elg.infiniteBootleg.console.commands

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.utils.Array
import com.strongjoshua.console.CommandExecutor
import com.strongjoshua.console.annotation.ConsoleDoc
import com.strongjoshua.console.annotation.HiddenCommand
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import ktx.async.schedule
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.console.AuthoritativeOnly
import no.elg.infiniteBootleg.console.ClientsideOnly
import no.elg.infiniteBootleg.console.CmdArgNames
import no.elg.infiniteBootleg.events.api.EventManager.eventsTracker
import no.elg.infiniteBootleg.events.api.EventManager.getOrCreateEventsTracker
import no.elg.infiniteBootleg.events.api.EventStatistics
import no.elg.infiniteBootleg.events.api.EventsTracker.Companion.LOG_EVERYTHING
import no.elg.infiniteBootleg.events.api.EventsTracker.Companion.LOG_NOTHING
import no.elg.infiniteBootleg.inventory.container.Container
import no.elg.infiniteBootleg.inventory.container.ContainerOwner
import no.elg.infiniteBootleg.inventory.container.OwnedContainer
import no.elg.infiniteBootleg.inventory.container.impl.AutoSortedContainer
import no.elg.infiniteBootleg.inventory.container.impl.ContainerImpl
import no.elg.infiniteBootleg.items.Item
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.screens.ConnectingScreen.info
import no.elg.infiniteBootleg.screens.HUDRenderer
import no.elg.infiniteBootleg.screens.MainMenuScreen
import no.elg.infiniteBootleg.screens.WorldScreen
import no.elg.infiniteBootleg.server.ServerClient.Companion.sendServerBoundPacket
import no.elg.infiniteBootleg.server.clientBoundWorldSettings
import no.elg.infiniteBootleg.server.sendDuplexPacket
import no.elg.infiniteBootleg.server.serverBoundClientDisconnectPacket
import no.elg.infiniteBootleg.server.serverBoundWorldSettings
import no.elg.infiniteBootleg.util.ChunkCoord
import no.elg.infiniteBootleg.util.IllegalAction
import no.elg.infiniteBootleg.util.ReflectionUtil
import no.elg.infiniteBootleg.util.chunkToWorld
import no.elg.infiniteBootleg.util.launchOnAsync
import no.elg.infiniteBootleg.util.launchOnMain
import no.elg.infiniteBootleg.util.stringifyCompactLoc
import no.elg.infiniteBootleg.util.toAbled
import no.elg.infiniteBootleg.util.toTitleCase
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.ContainerElement
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.WorldTime
import no.elg.infiniteBootleg.world.chunks.Chunk.Companion.CHUNK_SIZE
import no.elg.infiniteBootleg.world.ecs.api.restriction.component.AuthoritativeOnlyComponent
import no.elg.infiniteBootleg.world.ecs.api.restriction.component.ClientComponent
import no.elg.infiniteBootleg.world.ecs.api.restriction.component.DebuggableComponent.Companion.debugString
import no.elg.infiniteBootleg.world.ecs.api.restriction.component.TagComponent
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.world.ecs.components.LocallyControlledComponent.Companion.locallyControlledComponent
import no.elg.infiniteBootleg.world.ecs.components.NameComponent.Companion.nameOrNull
import no.elg.infiniteBootleg.world.ecs.components.inventory.ContainerComponent
import no.elg.infiniteBootleg.world.ecs.components.inventory.ContainerComponent.Companion.closeContainer
import no.elg.infiniteBootleg.world.ecs.components.inventory.ContainerComponent.Companion.containerComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.inventory.ContainerComponent.Companion.containerOrNull
import no.elg.infiniteBootleg.world.ecs.components.inventory.ContainerComponent.Companion.ownedContainerOrNull
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.teleport
import no.elg.infiniteBootleg.world.ecs.components.tags.FlyingTag.Companion.flying
import no.elg.infiniteBootleg.world.ecs.components.tags.IgnorePlaceableCheckTag.Companion.ignorePlaceableCheck
import no.elg.infiniteBootleg.world.render.WorldRender.Companion.MAX_ZOOM
import no.elg.infiniteBootleg.world.render.WorldRender.Companion.MIN_ZOOM
import no.elg.infiniteBootleg.world.ticker.Ticker
import no.elg.infiniteBootleg.world.world.ClientWorld
import no.elg.infiniteBootleg.world.world.World
import java.util.Locale

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
@Suppress("unused")
class Commands : CommandExecutor() {
  private val world: World?
    get() = Main.inst().world ?: run {
      logger.error { "Failed to find the current world" }
      null
    }

  private val clientWorld: ClientWorld?
    get() = ClientMain.inst().world ?: run {
      logger.error { "Failed to find the current client world" }
      null
    }

  private fun findEntity(nameOrId: String): Entity? {
    val world = world ?: return null
    return world.getEntity(nameOrId) ?: world.namedEntities.find { it.nameOrNull == nameOrId } ?: run {
      logger.error { "No entity with UUID or name '$nameOrId'" }
      return null
    }
  }

  private fun entityNameId(entity: Entity) = "${entity.id}${entity.nameOrNull?.let { " ($it)" } ?: ""}"

  // ///////////////////////////////
  // AUTHORITATIVE ONLY COMMANDS //
  // ///////////////////////////////

  @AuthoritativeOnly
  @ConsoleDoc(description = "Save the world if possible")
  fun save() {
    val world = world ?: return
    if (world.isTransient) {
      logger.error { "Cannot save the transient $world" }
    } else {
      world.save()
      logger.info { "World $world saved" }
    }
  }

  // /////////////////
  // OPEN COMMANDS //
  // /////////////////

  @ConsoleDoc(description = "Toggle debug")
  fun debug() {
    Settings.debug = !Settings.debug
    logger.info { "Debug is now ${Settings.debug.toAbled()}" }
  }

  @ConsoleDoc(
    description = "Pauses the world ticker. This includes Box2D world updates, light updates, unloading of" +
      " chunks, entity updates and chunks update"
  )
  fun pause() {
    val world = world ?: return
    val ticker: Ticker = world.worldTicker
    if (ticker.isPaused) {
      logger.error { "World is already paused" }
    } else {
      ticker.pause()
      logger.info { "World is now paused" }
    }
  }

  @ConsoleDoc(description = "Resumes the world ticker. This includes Box2D world updates, light updates, unloading of chunks, entity updates and chunks update")
  fun resume() {
    val world = world ?: return
    val ticker: Ticker = world.worldTicker
    if (ticker.isPaused) {
      world.worldTicker.resume()
      world.render.update()
      logger.info { "World is now resumed" }
    } else {
      logger.error { "World is not paused" }
    }
  }

  @CmdArgNames("red", "green", "blue", "alpha")
  @ConsoleDoc(description = "Set the color of the sky. Params are expected to be between 0 and 1", paramDescriptions = ["red", "green", "blue", "alpha"])
  fun skyColor(r: Float, g: Float, b: Float, a: Float) {
    val world = world ?: return
    val skylight = world.worldTime.baseColor
    skylight.set(r, g, b, a)
    logger.info { "Sky color changed to $skylight" }
  }

  @CmdArgNames("scale")
  @ConsoleDoc(description = "How fast the time flows", paramDescriptions = ["The new scale of time"])
  fun timescale(scale: Float) {
    val world = world ?: return
    val worldTime = world.worldTime
    val old = worldTime.timeScale
    worldTime.timeScale = scale
    logger.info { "Changed time scale from $old to $scale" }
    sendDuplexPacket(
      { clientBoundWorldSettings(null, null, scale) }
    ) { serverBoundWorldSettings(null, null, scale) }
  }

  @ConsoleDoc(description = "Toggle if time ticks or not")
  fun toggleTime() {
    Settings.dayTicking = !Settings.dayTicking
    logger.info { "Time is now " + (if (Settings.dayTicking) "" else "not ") + "ticking" }
    sendDuplexPacket(
      { clientBoundWorldSettings(null, null, if (Settings.dayTicking) 1f else 0f) }
    ) { serverBoundWorldSettings(null, null, if (Settings.dayTicking) 1f else 0f) }
  }

  @CmdArgNames("time of day")
  @ConsoleDoc(description = "Set the current time", paramDescriptions = ["Time of day such as day, noon, dusk, night"])
  fun time(timeOfDay: String) {
    val time: Float = try {
      // There is a chance this method is selected before  the other time method
      timeOfDay.toFloat()
    } catch (ignored: NumberFormatException) {
      when (timeOfDay.lowercase(Locale.getDefault())) {
        "dawn" -> WorldTime.DAWN_TIME
        "day", "sunrise" -> WorldTime.SUNRISE_TIME
        "midday", "noon" -> WorldTime.MIDDAY_TIME
        "sunset" -> WorldTime.SUNSET_TIME
        "dusk" -> WorldTime.DUSK_TIME
        "midnight", "night" -> WorldTime.MIDNIGHT_TIME
        "end" -> Int.MAX_VALUE.toFloat()
        else -> {
          logger.error { "Unknown time of day, try sunrise, midday, sunset or midnight" }
          return
        }
      }
    }

    // call the other time function
    time(time)
  }

  @CmdArgNames("time")
  @ConsoleDoc(description = "Set the current time", paramDescriptions = ["The new time as a number with sunrise as 0, noon as 90, dusk as 180 etc"])
  fun time(time: Float) {
    val world = world ?: return
    val worldTime = world.worldTime
    val old = worldTime.time
    worldTime.time = time
    sendDuplexPacket(
      { clientBoundWorldSettings(null, time, null) }
    ) { serverBoundWorldSettings(null, time, null) }
    logger.info { "Changed time from $old to $time" }
  }

  @HiddenCommand
  @ConsoleDoc(description = "check dangling entities")
  fun cde() {
    val world = world ?: return
    world.postBox2dRunnable {
      val worldBox2dWorld = world.worldBody.box2dWorld
      val bodies = Array<Body>(worldBox2dWorld.bodyCount)
      worldBox2dWorld.getBodies(bodies)
      var invalid = 0
      var nullUserData = 0
      for (body in bodies) {
        val userData = body.userData
        if (userData is Entity) {
          val id: String = userData.id
          if (world.containsEntity(id)) {
            continue
          }
          invalid++
          logger.error { "Found entity not added to the world! $id" }
        } else if (userData == null) {
          nullUserData++
        } else {
          logger.warn { "Found body with non-entity userdata: $userData" }
        }
      }
      if (nullUserData > 0) {
        logger.warn { "Found bodies $nullUserData with null as userdata" }
      }
      if (invalid == 0) {
        logger.info { "No invalid bodies found!" }
      }
    }
  }

  @HiddenCommand
  @CmdArgNames("action")
  @ConsoleDoc(description = "Do something illegal")
  fun illegalAction(action: String) {
    IllegalAction.valueOf(action.trim().uppercase()).handle { "Illegal test action $action" }
  }

  @ConsoleDoc(description = "Some debug info")
  fun chunkInfo() {
    val world = world ?: return
    logger.info { "Debug chunk Info" }
    logger.info { "Loaded chunks: ${world.loadedChunks.size}" }
    val loadedChunkPos = world.loadedChunks.items.sorted().joinToString("\n") { "(${it.chunkX}, ${it.chunkY}) in view? ${!world.render.isOutOfView(it)}" }
    logger.info { "Chunk pos: \n$loadedChunkPos" }
  }

  @ConsoleDoc(description = "Toggle whether to track events")
  fun trackEvents() {
    val eventTracker = getOrCreateEventsTracker()
    if (eventTracker.logAnything) {
      eventTracker.log = LOG_NOTHING
    } else {
      eventTracker.log = LOG_EVERYTHING
    }
    logger.info { "Events are now ${if (eventTracker.logAnything) "" else "not "}tracked" }
  }

  @ConsoleDoc(description = "Toggle whether to track events")
  fun printTrackedEvents() {
    val eventTracker = eventsTracker
    if (eventTracker == null) {
      logger.error { "There is no active event tracker" }
      return
    }
    for (recordedEvent in eventTracker.recordedEvents) {
      logger.info { eventTracker.toString() }
    }
  }

  @CmdArgNames("item")
  @ConsoleDoc(description = "Give an item to player", paramDescriptions = ["Item to given"])
  fun give(elementName: String) = give(elementName, 1)

  @CmdArgNames("item", "quantity")
  @ConsoleDoc(description = "Give item to player", paramDescriptions = ["Item to given", "Quantity to give, default 1"])
  fun give(elementName: String, quantity: Int) {
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

    if (quantity < 1) {
      logger.error { "Quantity must be at least 1" }
      return
    }
    val item: Item = ContainerElement.valueOf(elementName)?.toItem(stock = quantity.toUInt()) ?: run {
      logger.error { "Unknown container element '$elementName'" }
      return
    }

    val notAdded = container.add(item)
    if (notAdded.isEmpty()) {
      logger.info { "Gave player $item" }
    } else {
      logger.error { "Failed to give player $item, not enough space for $notAdded" }
    }
  }

  @CmdArgNames("component")
  @ConsoleDoc(description = "Find entities by their component name, use * for all", paramDescriptions = ["component name"])
  fun entities(searchTerm: String) {
    val world = world ?: return
    val entities = if (searchTerm == "*") {
      world.validEntities.toList()
    } else {
      world.validEntities.filter { it.components.any { component -> component.javaClass.simpleName.removeSuffix("Component").removeSuffix("Tag").equals(searchTerm, true) } }
    }

    logger.info { "Found ${entities.size} entities" }
    logger.info { entities.joinToString { it.id } }
  }

  @CmdArgNames("entity")
  @ConsoleDoc(description = "List components of an entity", paramDescriptions = ["Entity UUID or name"])
  fun inspect(entityUUID: String) {
    val entity = findEntity(entityUUID) ?: return
    logger.info { "===[ ${entityNameId(entity)} ]===" }
    val (tags, nonTags) = entity.components.partition { it is TagComponent }
    if (nonTags.isNotEmpty()) {
      logger.info { "Components" }
      for (component in nonTags) {
        logger.info { "- ${component::class.simpleName}: ${component.debugString()}" }
      }
    }
    if (tags.isNotEmpty()) {
      logger.info { "Tags" }
      for (component in tags) {
        logger.info { "- ${component::class.simpleName}" }
      }
    }
  }

  @CmdArgNames("entity", "component")
  @ConsoleDoc(description = "Inspect a component of an entity", paramDescriptions = ["Entity UUID or name", "The simple name of the component to inspect"])
  fun inspect(entityUUID: String, componentName: String) {
    val entity = findEntity(entityUUID) ?: return
    val searchTerm = componentName.removeSuffix("Component")
    val component = entity.components.find { it::class.simpleName?.removeSuffix("Component")?.removeSuffix("Tag").equals(searchTerm, true) } ?: run {
      logger.error { "No component with name '$componentName' in entity ${entityNameId(entity)}" }
      return
    }

    logger.info { "===[ ${component::class.simpleName?.toTitleCase()} ]===" }

    fun printInfo(info: String, success: () -> Boolean) {
      if (success()) {
        logger.info { " (V) $info" }
      } else {
        logger.info { " (X) $info" }
      }
    }

    printInfo("Client only") { component is ClientComponent }
    printInfo("Authoritative only") { component is AuthoritativeOnlyComponent }
    printInfo("Tag") { component is TagComponent }

    logger.info { component.debugString() }
  }

  // ////////////////////////
  // CLIENT SIDE COMMANDS //
  // ////////////////////////

  @ClientsideOnly
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

  @ClientsideOnly
  @ConsoleDoc(description = "Toggle rendering of lights")
  fun lights() {
    Settings.renderLight = !Settings.renderLight
    val world = world ?: return
    if (Settings.renderLight) {
      world.render.update()
    }
    for (chunk in world.loadedChunks) {
      chunk.dirty()
    }
    logger.info {
      "Lighting is now ${Settings.renderLight.toAbled()}"
    }
  }

  @ClientsideOnly
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

  @ClientsideOnly
  @ConsoleDoc(description = "Toggles debug rendering of Box2D objects")
  fun debBox() {
    Settings.renderBox2dDebug = !Settings.renderBox2dDebug
    logger.info {
      "Debug rendering for Box2D is now ${Settings.renderBox2dDebug.toAbled()}"
    }
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggle drawBodies for box2d debug rendering")
  fun drawBodies() {
    val world = clientWorld ?: return
    val box2dDebugRenderer = world.render.box2DDebugRenderer
    box2dDebugRenderer.isDrawBodies = !box2dDebugRenderer.isDrawBodies
    logger.info { "Box2D debug draw Bodies is ${box2dDebugRenderer.isDrawBodies.toAbled()}" }
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggle drawJoints for box2d debug rendering")
  fun drawJoints() {
    val world = clientWorld ?: return
    val box2dDebugRenderer = world.render.box2DDebugRenderer
    box2dDebugRenderer.isDrawJoints = !box2dDebugRenderer.isDrawJoints
    logger.info { "Box2D debug draw Joints is ${box2dDebugRenderer.isDrawJoints.toAbled()}" }
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggle drawAABBs for box2d debug rendering")
  fun drawAABBs() {
    val world = clientWorld ?: return
    val box2dDebugRenderer = world.render.box2DDebugRenderer
    box2dDebugRenderer.isDrawAABBs = !box2dDebugRenderer.isDrawAABBs
    logger.info { "Box2D debug draw AABBs is ${box2dDebugRenderer.isDrawAABBs.toAbled()}" }
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggle drawInactiveBodies for box2d debug rendering")
  fun drawInactiveBodies() {
    val world = clientWorld ?: return
    val box2dDebugRenderer = world.render.box2DDebugRenderer
    box2dDebugRenderer.isDrawInactiveBodies = !box2dDebugRenderer.isDrawInactiveBodies
    logger.info { "Box2D debug draw InactiveBodies is ${box2dDebugRenderer.isDrawInactiveBodies.toAbled()}" }
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggle drawVelocities for box2d debug rendering")
  fun drawVelocities() {
    val world = clientWorld ?: return
    val box2dDebugRenderer = world.render.box2DDebugRenderer
    box2dDebugRenderer.isDrawVelocities = !box2dDebugRenderer.isDrawVelocities
    logger.info { "Box2D debug draw Velocities is ${box2dDebugRenderer.isDrawVelocities.toAbled()}" }
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggle drawContacts for box2d debug rendering")
  fun drawContacts() {
    val world = clientWorld ?: return
    val box2dDebugRenderer = world.render.box2DDebugRenderer
    box2dDebugRenderer.isDrawContacts = !box2dDebugRenderer.isDrawContacts
    logger.info { "Box2D debug draw Contacts is ${box2dDebugRenderer.isDrawContacts.toAbled()}" }
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggles debug rendering of chunk bounds")
  fun debChu() {
    Settings.renderChunkBounds = !Settings.renderChunkBounds
    logger.info { "Debug rendering of chunks is now ${Settings.renderChunkBounds.toAbled()}" }
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggles debug rendering of chunk updates")
  fun debChuUpd() {
    Settings.renderChunkUpdates = !Settings.renderChunkUpdates
    logger.info { "Debug rendering of chunk updates is now ${Settings.renderChunkUpdates.toAbled()}" }
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggles debug rendering of block light updates")
  fun debLitUpd() {
    Settings.renderBlockLightUpdates = !Settings.renderBlockLightUpdates
    logger.info { "Debug rendering of block light updates is now ${Settings.renderBlockLightUpdates.toAbled()}" }
  }

  @ClientsideOnly
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

  @ClientsideOnly
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

  @ClientsideOnly
  @ConsoleDoc(description = "Toggles smoothed camera movement when following a player")
  fun lerp() {
    Settings.enableCameraFollowLerp = !Settings.enableCameraFollowLerp
    logger.info {
      "Camera leap is now " + Settings.enableCameraFollowLerp.toAbled()
    }
  }

  @CmdArgNames("x", "y")
  @ClientsideOnly
  @ConsoleDoc(description = "Teleport to given world coordinate", paramDescriptions = ["World x coordinate", "World y coordinate"])
  fun tp(worldX: Float, worldY: Float) {
    val clientWorld = clientWorld ?: return
    launchOnAsync {
      clientWorld.render.lookAt(worldX, worldY)
      logger.info { "Teleported camera to ${stringifyCompactLoc(worldX, worldY)}" }

      val entities = clientWorld.controlledPlayerEntities
      if (entities.size() > 0) {
        world?.loadChunk(worldX.worldToChunk(), worldY.worldToChunk())
        entities.forEach { it.teleport(worldX, worldY, killVelocity = true) }
        logger.info { "Teleported entity to ${stringifyCompactLoc(worldX, worldY)}" }
      }
    }
  }

  @CmdArgNames("mode")
  @ClientsideOnly
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
  @ClientsideOnly
  @ConsoleDoc(description = "Change the zoom level of the world camera", paramDescriptions = ["The new zoom level must be between $MIN_ZOOM and $MAX_ZOOM"])
  fun zoom(zoom: Float) = zoom(zoom, true)

  @CmdArgNames("zoom level", "limit")
  @ClientsideOnly
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

  @ClientsideOnly
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
  @ClientsideOnly
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
  @ClientsideOnly
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

  @ClientsideOnly
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

  @ClientsideOnly
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

  @ClientsideOnly
  @ConsoleDoc(description = "Toggle vsync")
  @CmdArgNames("enable")
  fun vsync(enable: Boolean) {
    Gdx.graphics.setVSync(enable)
    logger.info { "VSync is now ${enable.toAbled()}" }
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Set max FPS, if < 0 there is no limit. ")
  @CmdArgNames("fps")
  fun maxFPS(fps: Int) {
    Gdx.graphics.setForegroundFPS(fps)
    logger.info { "Max foreground fps is now $fps" }
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Disconnect from the server")
  fun quit() {
    disconnect()
  }

  @ClientsideOnly
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
  @ClientsideOnly
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

  @ConsoleDoc(description = "Find entities in a chunk", paramDescriptions = ["The x component of the chunk coordinate", "The y component of the chunk coordinate"])
  @CmdArgNames("chunkX", "chunkY")
  fun entInChunk(chunkX: ChunkCoord, chunkY: ChunkCoord) {
    val world = world ?: return
    val chunk = world.getChunk(chunkX, chunkY, load = true) ?: run {
      logger.error { "Failed to find a chunk at ${stringifyCompactLoc(chunkX, chunkY)}" }
      return
    }
    chunk.queryEntities { entities ->
      entities.forEach { (_, entity) ->
        logger.info { entityNameId(entity) }
      }
      logger.info { "In total ${entities.size} entities were found in chunk ${stringifyCompactLoc(chunkX, chunkY)}" }
    }
  }

  @ClientsideOnly
  fun sandTest(delayMillis: Long = 50) {
    val world = clientWorld ?: return
    val dx = 2
    val dy = 1
    val chunkXs = world.render.chunksInView.run { (horizontalStart + dx).chunkToWorld(0) until (horizontalEnd - dx).chunkToWorld(CHUNK_SIZE) }
    val y = (world.render.chunksInView.verticalEnd - dy).chunkToWorld(0)
    launchOnMain {
      for (x in chunkXs) {
        coroutineScope {
          launchOnAsync { world.setBlock(x, y, Material.SAND, prioritize = true) }
        }
        delay(delayMillis)
      }
    }
  }

  fun eventStats() {
    logger.info { EventStatistics.createRapport() }
  }

  fun clearEventStats() {
    EventStatistics.clear()
    logger.info { "Cleared event stats by command" }
  }
}
