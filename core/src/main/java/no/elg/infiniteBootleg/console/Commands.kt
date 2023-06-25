package no.elg.infiniteBootleg.console

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.utils.Array
import com.strongjoshua.console.CommandExecutor
import com.strongjoshua.console.LogLevel
import com.strongjoshua.console.annotation.ConsoleDoc
import com.strongjoshua.console.annotation.HiddenCommand
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.events.api.EventManager.eventsTracker
import no.elg.infiniteBootleg.events.api.EventManager.getOrCreateEventsTracker
import no.elg.infiniteBootleg.input.KeyboardControls
import no.elg.infiniteBootleg.screen.HUDRenderer
import no.elg.infiniteBootleg.screens.ConnectingScreen.info
import no.elg.infiniteBootleg.screens.MainMenuScreen
import no.elg.infiniteBootleg.screens.WorldScreen
import no.elg.infiniteBootleg.server.clientBoundWorldSettings
import no.elg.infiniteBootleg.server.sendDuplexPacket
import no.elg.infiniteBootleg.server.serverBoundClientDisconnectPacket
import no.elg.infiniteBootleg.server.serverBoundWorldSettings
import no.elg.infiniteBootleg.util.ReflectionUtil
import no.elg.infiniteBootleg.util.Ticker
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.ecs.components.LocallyControlledComponent.Companion.locallyControlled
import no.elg.infiniteBootleg.world.ecs.components.required.Box2DBodyComponent
import no.elg.infiniteBootleg.world.ecs.components.required.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.tags.FlyingTag.Companion.flying
import no.elg.infiniteBootleg.world.ecs.components.tags.IgnorePlaceableCheckTag.Companion.ignorePlaceableCheck
import no.elg.infiniteBootleg.world.ecs.localPlayerFamily
import no.elg.infiniteBootleg.world.render.WorldRender
import no.elg.infiniteBootleg.world.time.WorldTime
import no.elg.infiniteBootleg.world.world.ClientWorld
import no.elg.infiniteBootleg.world.world.World
import java.util.Locale

/**
 * @author Elg
 */
@Suppress("unused")
class Commands(private val logger: ConsoleLogger) : CommandExecutor() {
  private val world: World?
    get() = Main.inst().world ?: kotlin.run {
      logger.error("CMD", "Failed to find the current world")
      null
    }

  private val clientWorld: ClientWorld?
    get() = ClientMain.inst().world ?: kotlin.run {
      logger.error("CMD", "Failed to find world")
      null
    }

  @CmdArgNames("red", "green", "blue", "alpha")
  @ConsoleDoc(description = "Set the color of the sky. Params are expected to be between 0 and 1", paramDescriptions = ["red", "green", "blue", "alpha"])
  fun skyColor(r: Float, g: Float, b: Float, a: Float) {
    val world = world ?: return
    val skylight = world.worldTime.baseColor
    skylight.set(r, g, b, a)
    logger.success("Sky color changed to $skylight")
  }

  @ClientsideOnly
  @CmdArgNames("color")
  @ConsoleDoc(description = "Set the color of the sky", paramDescriptions = ["Name of color"])
  fun skyColor(colorName: String) {
    val world = world ?: return
    val skylight = world.worldTime.baseColor
    try {
      val color = ReflectionUtil.getStaticField(Color::class.java, colorName.uppercase(Locale.getDefault())) as Color
      skylight.set(color)
      logger.log("Sky color changed to ${colorName.lowercase()} ($color)")
    } catch (e: Exception) {
      logger.log(LogLevel.ERROR, "Unknown color '$colorName'")
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
    logger.log(
      LogLevel.SUCCESS,
      "Lighting is now ${if (Settings.renderLight) "enabled" else "disabled"}"
    )
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Reload all loaded chunks if unloading is allowed")
  fun reload() {
    reload(false)
  }

  @CmdArgNames("force")
  @ClientsideOnly
  @ConsoleDoc(description = "Reload all loaded chunks", paramDescriptions = ["Force unloading of chunks even when unloading is disallowed"])
  fun reload(force: Boolean) {
    val world = world ?: return
    world.reload(force)
    logger.log(LogLevel.SUCCESS, "All chunks have been reloaded")
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggle flight for player")
  fun fly() {
    val world = clientWorld ?: return
    val entities = world.engine.getEntitiesFor(localPlayerFamily)
    if (entities.size() == 0) {
      logger.log("There is no local, controlled player in this world")
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
      logger.log(LogLevel.SUCCESS, "Player is now ${if (wasFlying) "not " else ""}flying")
    }
  }

  @ConsoleDoc(
    description = "Pauses the world ticker. This includes Box2D world updates, light updates, unloading of" +
      " chunks, entity updates and chunks update"
  )
  fun pause() {
    val world = world ?: return
    val ticker: Ticker = world.worldTicker
    if (ticker.isPaused) {
      logger.error("World is already paused")
    } else {
      ticker.pause()
      logger.log(LogLevel.SUCCESS, "World is now paused")
    }
  }

  @ConsoleDoc(description = "Resumes the world ticker. This includes Box2D world updates, light updates, unloading of chunks, entity updates and chunks update")
  fun resume() {
    val world = world ?: return
    val ticker: Ticker = world.worldTicker
    if (ticker.isPaused) {
      world.worldTicker.resume()
      world.render.update()
      logger.log(LogLevel.SUCCESS, "World is now resumed")
    } else {
      logger.error("World is not paused")
    }
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggles debug rendering of Box2D objects")
  fun debBox() {
    Settings.renderBox2dDebug = !Settings.renderBox2dDebug
    Settings.debug = true
    logger.log(
      LogLevel.SUCCESS,
      "Debug rendering for Box2D is now ${if (Settings.renderBox2dDebug) "enabled" else "disabled"}"
    )
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggles debug rendering of chunk bounds")
  fun debChu() {
    Settings.renderChunkBounds = !Settings.renderChunkBounds
    Settings.debug = true
    logger.log(
      LogLevel.SUCCESS,
      "Debug rendering of chunks is now ${if (Settings.renderBox2dDebug) "enabled" else "disabled"}"
    )
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggles debug rendering of entity lighting")
  fun debEntLit() {
    Settings.debugEntityLight = !Settings.debugEntityLight
    Settings.debug = true
    logger.log(
      LogLevel.SUCCESS,
      "Debug rendering of entity light is now ${if (Settings.debugEntityLight) "enabled" else "disabled"}"
    )
    if (Settings.debugEntityLight) {
      logger.log(LogLevel.DEFAULT, "A white box is rendered over the block each entity source their brightness from")
    }
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggles debug rendering of block lighting")
  fun debBlkLit() {
    Settings.debugBlockLight = !Settings.debugBlockLight
    Settings.debug = true
    logger.log(
      LogLevel.SUCCESS,
      "Debug rendering of block light is now ${if (Settings.debugBlockLight) "enabled" else "disabled"}"
    )
    if (Settings.debugBlockLight) {
      logger.log(LogLevel.DEFAULT, "A red box is rendered over the luminescent blocks and a yellow box represents the skylight each block source their brightness from")
    }
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggles smoothed camera movement when following a player")
  fun lerp() {
    Settings.enableCameraFollowLerp = !Settings.enableCameraFollowLerp
    logger.log(
      LogLevel.SUCCESS,
      "Camera leap is now " + if (Settings.enableCameraFollowLerp) "enabled" else "disabled"
    )
  }

  @CmdArgNames("x", "y")
  @ClientsideOnly
  @ConsoleDoc(description = "Teleport to given world coordinate", paramDescriptions = ["World x coordinate", "World y coordinate"])
  fun tp(worldX: Float, worldY: Float) {
    val clientWorld = clientWorld ?: return
    val render = clientWorld.render
    val worldBody = clientWorld.worldBody
    render.camera.position.x = worldX * Block.BLOCK_SIZE + worldBody.worldOffsetX
    render.camera.position.y = worldY * Block.BLOCK_SIZE + worldBody.worldOffsetY
    render.update()
    logger.logf(LogLevel.SUCCESS, "Teleported camera to (% .2f,% .2f)", worldX, worldY)

    // TODO for ashley
    //    Player player = getSPPlayer();
    //    if (player == null) {
    //      return;
    //    }
    //    player.teleport(worldX, worldY, false);
//    logger.logf(LogLevel.SUCCESS, "Teleported player to (% .2f,% .2f)", worldX, worldY)
  }

  @CmdArgNames("mode")
  @ClientsideOnly
  @ConsoleDoc(description = "Toggle how much information to show", paramDescriptions = ["block, debug, graph, mindebug, or none"])
  fun hud(modusName: String) {
    val screen = ClientMain.inst().screen
    if (screen !is WorldScreen) {
      logger.error("Not currently in a world, cannot change hud")
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
      logger.error("Unknown HUD modus '$modusName'")
    } else if (mode == 0) {
      hud.displayNothing()
    } else {
      hud.toggleMode(mode)
    }
  }

  @CmdArgNames("zoom level")
  @ClientsideOnly
  @ConsoleDoc(description = "Change the zoom level of the world camera", paramDescriptions = ["The new zoom level, min is " + WorldRender.MIN_ZOOM])
  fun zoom(zoom: Float) {
    var zoom = zoom
    if (zoom < WorldRender.MIN_ZOOM) {
      logger.warn(
        "Given zoom level (%.3f) is less than the minimum % .3f",
        zoom,
        WorldRender.MIN_ZOOM
      )
      zoom = WorldRender.MIN_ZOOM
    }
    val clientWorld = clientWorld ?: return
    val render = clientWorld.render
    render.camera.zoom = Math.max(zoom, WorldRender.MIN_ZOOM)
    render.update()
    logger.success("Zoom level is now $zoom")
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Get the brush sizes")
  fun brush() {
    val world = clientWorld ?: return
    val entities = world.engine.getEntitiesFor(localPlayerFamily)
    if (entities.size() == 0) {
      logger.log("There is no local, controlled player in this world")
    }
    for (entity in entities) {
      val controls: KeyboardControls = entity.locallyControlled.keyboardControls
      logger.logf(
        LogLevel.SUCCESS,
        "Brush size for player are now %.2f blocks for breaking and %.2f blocks for placing",
        controls.breakBrushSize,
        controls.placeBrushSize
      )
    }
  }

  @CmdArgNames("type", "size")
  @ClientsideOnly
  @ConsoleDoc(description = "Set the brush size of the mouse", paramDescriptions = ["Type of brush to change, can be 'break' and 'place'", "New brush size, positive integer"])
  fun brush(type: String?, size: Float) {
    val world = clientWorld ?: return
    if (type == null) {
      logger.error("CMD", "Valid brush types are 'break' and 'place'")
      return
    }
    val entities = world.engine.getEntitiesFor(localPlayerFamily)
    if (entities.size() == 0) {
      logger.error("There is no local, controlled player in this world")
    }
    for (entity in entities) {
      val controls: KeyboardControls = entity.locallyControlled.keyboardControls
      if (type.startsWith("b")) {
        controls.breakBrushSize = size
      } else if (type.startsWith("p")) {
        controls.placeBrushSize = size
      } else {
        logger.error("CMD", "Valid brush types are 'break' and 'place'")
        return
      }
    }
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggle whether a player can place blocks disconnected from other blocks")
  fun placeCheck() {
    val world = clientWorld ?: return
    val entities = world.engine.getEntitiesFor(localPlayerFamily)
    if (entities.size() == 0) {
      logger.error("There is no local, controlled player in this world")
    }
    for (entity in entities) {
      val wasIgnoring: Boolean = entity.ignorePlaceableCheck
      entity.ignorePlaceableCheck = !wasIgnoring
      logger.success("Place check is now " + if (wasIgnoring) "enabled" else "disabled")
    }
  }

  @CmdArgNames("scale")
  @ConsoleDoc(description = "How fast the time flows", paramDescriptions = ["The new scale of time"])
  fun timescale(scale: Float) {
    val world = world ?: return
    val worldTime = world.worldTime
    val old = worldTime.timeScale
    worldTime.timeScale = scale
    logger.success("Changed time scale from % .3f to % .3f", old, scale)
    sendDuplexPacket(
      { clientBoundWorldSettings(null, null, scale) }
    ) { serverBoundWorldSettings(null, null, scale) }
  }

  @ConsoleDoc(description = "Toggle if time ticks or not")
  fun toggleTime() {
    Settings.dayTicking = !Settings.dayTicking
    logger.success("Time is now " + (if (Settings.dayTicking) "" else "not ") + "ticking")
    sendDuplexPacket(
      { clientBoundWorldSettings(null, null, if (Settings.dayTicking) 1f else 0f) }
    ) { serverBoundWorldSettings(null, null, if (Settings.dayTicking) 1f else 0f) }
  }

  @CmdArgNames("time of day")
  @ConsoleDoc(description = "Set the current time", paramDescriptions = ["Time of day such as day, noon, dusk, night"])
  fun time(timeOfDay: String) {
    val time: Float
    time = try {
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
          logger.error("CMD", "Unknown time of day, try sunrise, midday, sunset or midnight")
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
    logger.success("Changed time from % .3f to % .3f", old, time)
  }

  @HiddenCommand
  @ClientsideOnly
  @CmdArgNames("dx", "dy")
  @ConsoleDoc(description = "Shift world offset")
  fun swo(x: Float, y: Float) {
    val world = world ?: return
    world.worldBody.shiftWorldOffset(x, y)
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
      val entities = world.engine.entities
      for (body in bodies) {
        val userData = body.userData
        if (userData is Entity) {
          val id: String = userData.id
          if (world.containsEntity(id)) {
            continue
          }
          invalid++
          logger.error("Entity", "Found entity not added to the world! $id")
        }
      }
      if (invalid == 0) {
        logger.success("No invalid bodies found!")
      }
    }
  }

  @ConsoleDoc(description = "Save the world server side")
  fun save() {
    val world = world ?: return
    world.save()
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggle vsync")
  @CmdArgNames("enable")
  fun vsync(enable: Boolean) {
    Gdx.graphics.setVSync(enable)
    logger.success("VSync is now ${if (enable) "enabled" else "disabled"}")
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Set max FPS, if < 0 there is no limit. ")
  @CmdArgNames("fps")
  fun maxFPS(fps: Int) {
    Gdx.graphics.setForegroundFPS(fps)
    logger.success("Max foreground fps is now $fps")
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Disconnect from the server")
  fun quit() {
    disconnect()
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Disconnect from the server")
  fun disconnect() {
    val client = ClientMain.inst().serverClient
    client?.ctx?.writeAndFlush(
      client.serverBoundClientDisconnectPacket("Disconnect command")
    )
    info = "Disconnected"
    Main.inst()
      .scheduler
      .scheduleSync(50L) { ClientMain.inst().screen = MainMenuScreen }
  }

  @ConsoleDoc(description = "Some debug info")
  fun chunkInfo() {
    val world = world ?: return
    Main.logger().log("Debug chunk Info")
    Main.logger().log("Loaded chunks: ${world.loadedChunks.size}")
    val loadedChunkPos = world.loadedChunks.items.sorted().joinToString("\n") { "(${it.chunkX}, ${it.chunkY}) in view? ${!world.render.isOutOfView(it)}" }
    Main.logger().log("Chunk pos: \n$loadedChunkPos")
  }

  @ConsoleDoc(description = "Toggle whether to track events")
  fun trackEvents() {
    val eventTracker = getOrCreateEventsTracker()
    eventTracker.log = !eventTracker.log
    logger.success("Events are now ${if (eventTracker.log) "" else "not "}tracked")
  }

  @ConsoleDoc(description = "Toggle whether to track events")
  fun printTrackedEvents() {
    val eventTracker = eventsTracker
    if (eventTracker == null) {
      logger.error("There is no active event tracker")
      return
    }
    for (recordedEvent in eventTracker.recordedEvents) {
      logger.log(eventTracker.toString())
    }
  }
}