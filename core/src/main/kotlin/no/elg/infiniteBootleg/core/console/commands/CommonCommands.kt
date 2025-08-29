package no.elg.infiniteBootleg.core.console.commands

import com.badlogic.ashley.core.Entity
import com.strongjoshua.console.CommandExecutor
import com.strongjoshua.console.annotation.ConsoleDoc
import com.strongjoshua.console.annotation.HiddenCommand
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.console.AuthoritativeOnly
import no.elg.infiniteBootleg.core.console.CmdArgNames
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.events.api.EventStatistics
import no.elg.infiniteBootleg.core.events.api.EventsTracker
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.net.clientBoundWorldSettings
import no.elg.infiniteBootleg.core.net.serverBoundWorldSettings
import no.elg.infiniteBootleg.core.util.ChunkCoord
import no.elg.infiniteBootleg.core.util.IllegalAction
import no.elg.infiniteBootleg.core.util.launchOnMainSuspendable
import no.elg.infiniteBootleg.core.util.stringifyCompactLoc
import no.elg.infiniteBootleg.core.util.toAbled
import no.elg.infiniteBootleg.core.util.toTitleCase
import no.elg.infiniteBootleg.core.world.WorldTime
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.AuthoritativeOnlyComponent
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.ClientComponent
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.DebuggableComponent.Companion.debugString
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.TagComponent
import no.elg.infiniteBootleg.core.world.ecs.components.NameComponent.Companion.name
import no.elg.infiniteBootleg.core.world.ecs.components.NameComponent.Companion.nameOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.core.world.ticker.Ticker
import no.elg.infiniteBootleg.core.world.world.World
import java.util.Locale

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
@Suppress("unused")
open class CommonCommands : CommandExecutor() {
  /**
   * Returns the current world if any, or logs an error
   */
  protected val world: World?
    get() = worldSilent ?: run {
      logger.error { "Failed to find the current world" }
      null
    }

  /**
   * Returns the current world if any, will not log an error when not found
   */
  protected val worldSilent: World? get() = Main.Companion.inst().world

  protected fun findEntity(nameOrId: String): Entity? {
    val world = world ?: return null
    return world.getEntity(nameOrId)
      ?: world.namedEntities.find { it.nameOrNull == nameOrId }
      ?: world.validEntities.find { it.id.startsWith(nameOrId) }
      ?: world.namedEntities.find { it.name.startsWith(nameOrId) }
      ?: run {
        logger.error { "No entity with id or name '$nameOrId'" }
        return null
      }
  }

  protected fun entityNameId(entity: Entity) = "${entity.id}${entity.nameOrNull?.let { " ($it)" } ?: ""}"

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
      logger.info { "Saved $world" }
    }
  }

  // /////////////////
  // OPEN COMMANDS //
  // /////////////////

  @ConsoleDoc(description = "Run command delayed")
  @CmdArgNames("delayMs", "command")
  fun delayed(delayMs: Long, command: String) {
    launchOnMainSuspendable {
      delay(delayMs)
      Main.inst().console.execCommand(command)
    }
    logger.info { "Scheduled command '$command' to run in $delayMs ms" }
  }

  @ConsoleDoc(description = "Run command delayed with one arg")
  @CmdArgNames("delayMs", "command", "arg1")
  fun delayed(delayMs: Long, command: String, arg1: String) {
    delayed(delayMs, "$command $arg1")
  }

  @ConsoleDoc(description = "Run command delayed with two args")
  @CmdArgNames("delayMs", "command", "arg1", "arg2")
  fun delayed(delayMs: Long, command: String, arg1: String, arg2: String) {
    delayed(delayMs, "$command $arg1 $arg2")
  }

  @ConsoleDoc(description = "Run command delayed with three args")
  @CmdArgNames("delayMs", "command", "arg1", "arg2", "arg3")
  fun delayed(
    delayMs: Long,
    command: String,
    arg1: String,
    arg2: String,
    arg3: String
  ) {
    delayed(delayMs, "$command $arg1 $arg2 $arg3")
  }

  @ConsoleDoc(description = "Run command delayed with four args")
  @CmdArgNames("delayMs", "command", "arg1", "arg2", "arg3", "arg4")
  fun delayed(
    delayMs: Long,
    command: String,
    arg1: String,
    arg2: String,
    arg3: String,
    arg4: String
  ) {
    delayed(delayMs, "$command $arg1 $arg2 $arg3 $arg4")
  }

  @AuthoritativeOnly
  @ConsoleDoc(description = "Print the world seed")
  fun seed() {
    val world = world ?: return
    logger.info { world.seed.toString() }
  }

  @AuthoritativeOnly
  @ConsoleDoc(description = "Toggle debug")
  fun debug() {
    Settings.debug = !Settings.debug
    logger.info { "Debug is now ${Settings.debug.toAbled()}" }
  }

  @AuthoritativeOnly
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
    Main.Companion.inst().packetSender.sendDuplexPacket(
      { clientBoundWorldSettings(null, null, scale) }
    ) { serverBoundWorldSettings(null, null, scale) }
  }

  @ConsoleDoc(description = "Toggle if time ticks or not")
  fun toggleTime() {
    Settings.dayTicking = !Settings.dayTicking
    logger.info { "Time is now " + (if (Settings.dayTicking) "" else "not ") + "ticking" }
    Main.Companion.inst().packetSender.sendDuplexPacket(
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
        "dawn" -> WorldTime.Companion.DAWN_TIME
        "day", "sunrise" -> WorldTime.Companion.SUNRISE_TIME
        "midday", "noon" -> WorldTime.Companion.MIDDAY_TIME
        "sunset" -> WorldTime.Companion.SUNSET_TIME
        "dusk" -> WorldTime.Companion.DUSK_TIME
        "midnight", "night" -> WorldTime.Companion.MIDNIGHT_TIME
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
    Main.Companion.inst().packetSender.sendDuplexPacket(
      { clientBoundWorldSettings(null, time, null) }
    ) { serverBoundWorldSettings(null, time, null) }
    logger.info { "Changed time from $old to $time" }
  }

//  @HiddenCommand
//  @ConsoleDoc(description = "check dangling entities")
//  fun cde() {
//    val world = world ?: return
//    world.postBox2dRunnable {
//      val worldBox2dWorld = world.worldBody.box2dWorld
//      val bodies = Array<b2BodyId>(worldBox2dWorld.bodyCount)
//      worldBox2dWorld.getBodies(bodies)
//      var invalid = 0
//      var nullUserData = 0
//      for (body in bodies) {
//        val userData = body.userData
//        if (userData is Entity) {
//          val id: String = userData.id
//          if (world.containsEntity(id)) {
//            continue
//          }
//          invalid++
//          logger.error { "Found entity not added to the world! $id" }
//        } else if (userData is ChunkBody) {
//          val chunk = userData.chunk
//          val worldChunk = world.getChunk(chunk.compactLocation, load = false)
//          if (chunk !== worldChunk) {
//            invalid++
//            logger.error { "Found chunk body that is not the same as the world's chunk. chunk userdata: $chunk, world chunk: $worldChunk" }
//          }
//        } else if (userData == null) {
//          nullUserData++
//        } else {
//          logger.warn { "Found body with non-entity userdata: $userData" }
//        }
//      }
//      if (nullUserData > 0) {
//        logger.warn { "Found bodies $nullUserData with null as userdata" }
//      }
//      if (invalid == 0) {
//        logger.info { "No invalid bodies found!" }
//      }
//    }
//  }

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
    val eventTracker = EventManager.getOrCreateEventsTracker()
    if (eventTracker.logAnything) {
      eventTracker.log = EventsTracker.Companion.LOG_NOTHING
    } else {
      eventTracker.log = EventsTracker.Companion.LOG_EVERYTHING
    }
    logger.info { "Events are now ${if (eventTracker.logAnything) "" else "not "}tracked" }
  }

  @ConsoleDoc(description = "Toggle whether to track events")
  fun printTrackedEvents() {
    val eventTracker = EventManager.eventsTracker
    if (eventTracker == null) {
      logger.error { "There is no active event tracker" }
      return
    }
    for (recordedEvent in eventTracker.recordedEvents) {
      logger.info { eventTracker.toString() }
    }
  }

  @CmdArgNames("component")
  @ConsoleDoc(description = "Find entities by their component name, use * for all", paramDescriptions = ["component name"])
  fun entities(searchTerm: String) {
    val world = world ?: return
    val entities = if (searchTerm == "*") {
      world.validEntities.toList()
    } else {
      world.validEntities.filter {
        it.components.any { component ->
          component != null && component.javaClass.simpleName.removeSuffix("Component").removeSuffix("Tag").equals(searchTerm, true)
        }
      }
    }

    logger.info { "Found ${entities.size} entities" }
    logger.info { entities.joinToString { it.id } }
  }

  @CmdArgNames("entityId")
  @ConsoleDoc(description = "List components of an entity", paramDescriptions = ["Entity id or name"])
  fun inspect(entityId: String) {
    val entity = findEntity(entityId) ?: return
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

  @CmdArgNames("entityId", "component")
  @ConsoleDoc(description = "Inspect a component of an entity", paramDescriptions = ["Entity id or name", "The simple name of the component to inspect"])
  fun inspect(entityId: String, componentName: String) {
    val entity = findEntity(entityId) ?: return
    val searchTerm = componentName.removeSuffix("Component")
    val component = entity.components.filterNotNull().find { it::class.simpleName?.removeSuffix("Component")?.removeSuffix("Tag").equals(searchTerm, true) } ?: run {
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

  @ConsoleDoc(description = "Find entities in a chunk", paramDescriptions = ["The x component of the chunk coordinate", "The y component of the chunk coordinate"])
  @CmdArgNames("chunkX", "chunkY")
  fun entInChunk(chunkX: ChunkCoord, chunkY: ChunkCoord) {
    val world = world ?: return
    val chunk = world.getChunk(chunkX, chunkY, load = false) ?: run {
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

  @ConsoleDoc(description = "Log stats for events logged")
  fun eventStats() {
    logger.info { EventStatistics.createRapport() }
  }

  @ConsoleDoc(description = "Clear all event stats")
  fun clearEventStats() {
    EventStatistics.clear()
    logger.info { "Cleared event stats by command" }
  }

  @ConsoleDoc(description = "List ecs systems in the iteration order")
  fun systemOrder() {
    val world = world ?: return
    val systems = world.engine.systems
    logger.info { "Systems in iteration order:" }
    for (system in systems) {
      logger.info { system::class.simpleName + " (${system.priority})" }
    }
  }

  @HiddenCommand
  fun gc() {
    System.gc()
  }
}
