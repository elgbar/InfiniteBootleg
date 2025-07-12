package no.elg.infiniteBootleg.client.screens.hud

import com.badlogic.gdx.scenes.scene2d.Stage
import ktx.actors.isShown
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.vis.visTable
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.util.IBVisWindow
import no.elg.infiniteBootleg.client.util.ibVisWindowClosed
import no.elg.infiniteBootleg.client.util.setIBDefaults
import no.elg.infiniteBootleg.client.world.render.FuturePositionRenderer
import no.elg.infiniteBootleg.client.world.world.ClientWorld
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.events.api.EventsTracker
import no.elg.infiniteBootleg.core.util.INITIAL_BRUSH_SIZE
import no.elg.infiniteBootleg.core.util.INITIAL_INSTANT_BREAK
import no.elg.infiniteBootleg.core.util.INITIAL_INTERACT_RADIUS
import no.elg.infiniteBootleg.core.util.launchOnAsync
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.chunks.TexturedChunk
import no.elg.infiniteBootleg.core.world.ecs.components.LocallyControlledComponent.Companion.locallyControlledComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.tags.IgnorePlaceableCheckTag.Companion.ignorePlaceableCheck
import no.elg.infiniteBootleg.core.world.render.WorldRender.Companion.MAX_ZOOM

class DebugWindow(
  private val stage: Stage,
  private val debugMenu: IBVisWindow,
  private val onAnyElementChanged: MutableList<() -> Unit>,
  private val debugWindows: List<IBVisWindow>
) {

  val isDebugMenuVisible: Boolean get() = debugWindows.any { it.isShown() }

  fun toggleShown() {
    if (!isDebugMenuVisible) {
      updateAllValues(onAnyElementChanged)
    }
    debugMenu.toggleShown(stage, true)
  }

  fun close() {
    debugMenu.close()
  }
}

fun Stage.addDebugOverlay(world: ClientWorld, staffMenu: IBVisWindow): DebugWindow {
  val debugWindows = mutableListOf<IBVisWindow>()
  val onAnyElementChanged: MutableList<() -> Unit> = mutableListOf()
  debugWindows += staffMenu
  val debugMenu = world.ibVisWindowClosed("Debug Menu") {
    closeOnEscape()

    visTable {
      setIBDefaults()

      @Scene2dDsl
      fun sep() = aSeparator()

      section {
        toggleableDebugButton(
          "General debug",
          "Handles whether to log debug messages and\nsmaller debug features without a dedicated debug option",
          onAnyElementChanged = onAnyElementChanged,
          booleanGetter = Settings::debug,
          onToggle = ClientMain.inst().console.exec::debug
        )
        toggleableDebugButton(
          "Render chunks borders",
          "Render an outline around each chunk",
          onAnyElementChanged = onAnyElementChanged,
          booleanGetter = Settings::renderChunkBounds,
          onToggle = ClientMain.inst().console.exec::renderChunkBorders
        )
        toggleableDebugButton(
          "Render chunk updates",
          """Flash a chunk when a chunks texture changes
            |* Yellow - Chunk is prioritized for rendering
            |* Red    - Chunk is not prioritized for rendering
          """.trimMargin(),
          onAnyElementChanged = onAnyElementChanged,
          booleanGetter = Settings::renderChunkUpdates,
          onToggle = ClientMain.inst().console.exec::debChuUpd
        )
        toggleableDebugButton(
          "Render entity markers",
          "Render entity block markers, a block to connect entities which represent a block as a temporary marker in the block world",
          onAnyElementChanged = onAnyElementChanged,
          property = Settings::debugEntityMarkerBlocks
        )
      }

      section {
        toggleableDebugButton(
          "Render existing air",
          "Render air blocks, which are normally indistinguishable from non-existing blocks, as cute little clouds",
          onAnyElementChanged = onAnyElementChanged,
          property = Settings::renderAirBlocks
        )
        toggleableDebugButton(
          "Validate families",
          "Whether entity families should be validated. If invalid the entity will not be added to the entity engine",
          onAnyElementChanged = onAnyElementChanged,
          property = Settings::validateEntityFamilies
        )
        toggleableDebugButton(
          "Render top block changes",
          "Render changes to the top block of a chunk column",
          onAnyElementChanged = onAnyElementChanged,
          property = Settings::renderTopBlockChanges
        )

        toggleableDebugButton(
          "UI debug",
          "Toggles debugging for scene 2d",
          onAnyElementChanged = onAnyElementChanged,
          // cant use property here, as kotlin no longer support accessing methods named the same as the synthetic property
          booleanGetter = { this@addDebugOverlay.isDebugAll },
          onToggle = { this@addDebugOverlay.isDebugAll = !this@addDebugOverlay.isDebugAll }
        )
      }

      // Lights
      section {
        toggleableDebugButton(
          "Render entity lighting",
          "Render a small dot at the block the entity's light level is calculated from",
          onAnyElementChanged = onAnyElementChanged,
          booleanGetter = Settings::debugEntityLight,
          onToggle = ClientMain.inst().console.exec::debEntLit
        )
        toggleableDebugButton(
          "Render block lighting",
          """Render an overlay which displays which blocks the light level of the block under the mouse is calculated. 
              |A red overlay denotes a luminescent block, while a yellow overlay denotes the skylight is the source of the light.
          """.trimMargin(),
          onAnyElementChanged = onAnyElementChanged,
          booleanGetter = Settings::debugBlockLight,
          onToggle = ClientMain.inst().console.exec::debBlkLit
        )
        toggleableDebugButton(
          "Render light updates",
          "Render a small dot over blocks with new lighting",
          onAnyElementChanged = onAnyElementChanged,
          booleanGetter = Settings::renderBlockLightUpdates,
          onToggle = ClientMain.inst().console.exec::debLitUpd
        )
        toggleableDebugButton(
          "Render lights",
          "Whether to render light at all. Will break lighting information if the world is updated with this off",
          onAnyElementChanged = onAnyElementChanged,
          booleanGetter = Settings::renderLight,
          onToggle = ClientMain.inst().console.exec::lights
        )
      }
      section {
        toggleableDebugButton(
          "Recalculate lights",
          "Recalulate all lights in the world and re-render the chunks",
          "toggle-menu-button",
          onAnyElementChanged,
          { false },
          {
            world.readChunks { readableChunks ->
              readableChunks.values().forEach(Chunk::updateAllBlockLights)
            }
          }
        )
        toggleableDebugButton(
          "Recalculate topblock lights",
          "Recalulate the top most block in the world",
          "toggle-menu-button",
          onAnyElementChanged,
          { false },
          {
            world.render.chunkColumnsInView.forEach {
              launchOnAsync {
                for (localX in 0 until Chunk.CHUNK_SIZE) {
                  world.getChunkColumn(it).updateTopBlockWithoutHint(localX)
                }
              }
            }
          }
        )
        toggleableDebugButton(
          "Render all chunks",
          "Re-render the texture of all loaded chunks in the world",
          "toggle-menu-button",
          onAnyElementChanged,
          { false },
          {
            world.readChunks { readableChunks ->
              readableChunks.values().mapNotNull { it as? TexturedChunk }.forEach { it.queueForRendering(false) }
            }
          }
        )
        toggleableDebugButton(
          "Show staff creator",
          "Show the staff creator overlay",
          "toggle-menu-button",
          onAnyElementChanged,
          { false },
          {
            staffMenu.show(this@addDebugOverlay, true)
            staffMenu.toFront()
          }
        )
      }
      section {
        toggleableDebugButton(
          "Render leaf decay",
          "Render each block checked for leaf decay",
          onAnyElementChanged = onAnyElementChanged,
          property = Settings::renderLeafDecay
        )
        toggleableDebugButton(
          "Render closest block to chunk",
          "Render which block in the current players chunk is closes to the block pointed at",
          onAnyElementChanged = onAnyElementChanged,
          property = Settings::renderClosestBlockToPlayerChunk
        )
        toggleableDebugButton(
          "Render top block",
          "Render top blocks in chunk columns. Solid = green, light = blue, both = cyan",
          onAnyElementChanged = onAnyElementChanged,
          property = Settings::renderTopBlocks
        )
        toggleableDebugButton(
          "Render grounded touching block",
          "Render the blocks the player is touching",
          onAnyElementChanged = onAnyElementChanged,
          property = Settings::renderGroundedTouchingBlocks
        )
      }

//      section {
      // Room for one four buttons
//      }

      sep()
      // Future positions renderers
      section {
        toggleableDebugButton(
          "Render future positions",
          "Render the N future position of spell entities",
          onAnyElementChanged = onAnyElementChanged,
          property = FuturePositionRenderer::enabled
        )
        toggleableDebugButton(
          "Check collisions",
          "Will stop rendering if a point is inside a block",
          onAnyElementChanged = onAnyElementChanged,
          property = FuturePositionRenderer::collisionCheck
        )

        intSpinner(
          name = "Number of steps",
          srcValueGetter = FuturePositionRenderer::numberOfStepsToSee,
          min = 1f,
          max = 256f,
          step = 8f,
          decimals = 0,
          onAnyElementChanged = onAnyElementChanged,
          onChange = FuturePositionRenderer::numberOfStepsToSee::set
        )
      }

      sep()
      // Player related debug controls
      section {
        val entities = world.controlledPlayerEntities
        val instantBreakGetter: () -> Boolean = {
          entities.map { it.locallyControlledComponentOrNull }.firstOrNull()?.instantBreak ?: INITIAL_INSTANT_BREAK
        }

        val brushSizeGetter: () -> Number = {
          entities.map { it.locallyControlledComponentOrNull }.firstOrNull()?.brushSize ?: INITIAL_BRUSH_SIZE
        }
        val reachRadiusGetter: () -> Number = {
          entities.map { it.locallyControlledComponentOrNull }.firstOrNull()?.interactRadius ?: INITIAL_INTERACT_RADIUS
        }
        toggleableDebugButton(
          "Ignore place check",
          "Whether to ignore the placeable check when placing blocks. This is useful for debugging and building",
          onAnyElementChanged = onAnyElementChanged,
          booleanGetter = { world.controlledPlayerEntities.any { it.ignorePlaceableCheck } },
          onToggle = ClientMain.inst().console.exec::placeCheck
        )
        toggleableDebugButton(
          "Instant break",
          "Whether to instantly break blocks instead of slowly mining them",
          onAnyElementChanged = onAnyElementChanged,
          booleanGetter = instantBreakGetter,
          onToggle = ClientMain.inst().console.exec::instantBreak
        )
        floatSpinner(
          name = "Brush size",
          srcValueGetter = brushSizeGetter,
          min = 1f,
          max = 64f,
          step = 0.25f,
          decimals = 2,
          onAnyElementChanged = onAnyElementChanged,
          onChange = ClientMain.inst().console.exec::brush
        )
        floatSpinner("Reach radius", reachRadiusGetter, 1f, 512f, 1f, 0, onAnyElementChanged, ClientMain.inst().console.exec::interactRadius)
      }

      section {
        val zoomValueGetter: () -> Float = { ClientMain.inst().world?.render?.camera?.zoom ?: 1f }
        floatSlider("Zoom", zoomValueGetter, 0.1f, MAX_ZOOM * 5, 0.1f, onAnyElementChanged) {
          val clientWorld = ClientMain.inst().world ?: return@floatSlider
          clientWorld.render.camera.zoom = it
          clientWorld.render.update()
        }
        intSpinner(
          name = "Max chunk to render each frame",
          srcValueGetter = Settings::chunksToRenderEachFrame,
          min = 1,
          max = Int.MAX_VALUE,
          step = 1,
          decimals = 0,
          onAnyElementChanged = onAnyElementChanged,
          onChange = Settings::chunksToRenderEachFrame::set
        )
      }
      sep()

      val box2dDebug = world.render.box2DDebugRenderer
      section {
        toggleableDebugButton(
          "Debug Box2D",
          onAnyElementChanged = onAnyElementChanged,
          booleanGetter = Settings::renderBox2dDebug,
          onToggle = ClientMain.inst().console.exec::debBox
        )
      }
      section {
        toggleableDebugButton(
          "Box2D draw bodies",
          onAnyElementChanged = onAnyElementChanged,
          booleanGetter = box2dDebug::isDrawBodies,
          onToggle = ClientMain.inst().console.exec::drawBodies
        )
        toggleableDebugButton(
          "Box2D draw joints",
          onAnyElementChanged = onAnyElementChanged,
          booleanGetter = box2dDebug::isDrawJoints,
          onToggle = ClientMain.inst().console.exec::drawJoints
        )
        toggleableDebugButton(
          "Box2D draw AABBs",
          onAnyElementChanged = onAnyElementChanged,
          booleanGetter = box2dDebug::isDrawAABBs,
          onToggle = ClientMain.inst().console.exec::drawAABBs
        )
        toggleableDebugButton(
          "Box2D draw inactiveBodies",
          onAnyElementChanged = onAnyElementChanged,
          booleanGetter = box2dDebug::isDrawInactiveBodies,
          onToggle = ClientMain.inst().console.exec::drawInactiveBodies
        )
      }
      section {
        toggleableDebugButton(
          "Box2D draw velocities",
          onAnyElementChanged = onAnyElementChanged,
          booleanGetter = box2dDebug::isDrawVelocities,
          onToggle = ClientMain.inst().console.exec::drawVelocities
        )
        toggleableDebugButton(
          "Box2D draw contacts",
          onAnyElementChanged = onAnyElementChanged,
          booleanGetter = box2dDebug::isDrawContacts,
          onToggle = ClientMain.inst().console.exec::drawContacts
        )
      }
      sep()
      section {
        toggleableDebugButton("Vsync", onAnyElementChanged = onAnyElementChanged, booleanGetter = Settings::vsync, onToggle = { Settings.vsync = !Settings.vsync })
        intSpinner("Max FPS", Settings::foregroundFPS, 0, 512, 1, 0, onAnyElementChanged, Settings::foregroundFPS::set)
        val oneDayInSeconds = 86400f
        floatSpinner("Save every (sec)", Settings::savePeriodSeconds, 1f, oneDayInSeconds, 1f, 0, onAnyElementChanged, Settings::savePeriodSeconds::set)
      }
      sep()
      section {
        toggleableDebugButton(
          "Log packets",
          "Log packets between client and server",
          onAnyElementChanged = onAnyElementChanged,
          property = Settings::logPackets
        )
        toggleableDebugButton(
          "Log persistence",
          "Log files saved to and loaded from disk",
          onAnyElementChanged = onAnyElementChanged,
          property = Settings::logPersistence
        )
      }
      // Event tracker
      section {
        toggleableDebugButton(
          "Log events",
          "Whether to log when anything event related happens",
          onAnyElementChanged = onAnyElementChanged,
          booleanGetter = EventManager::isLoggingAnyEvents,
          onToggle = ClientMain.inst().console.exec::trackEvents
        )
        toggleableDebugButton(
          "Log events dispatched",
          "Whether to log when an event is dispatched",
          onAnyElementChanged = onAnyElementChanged,
          booleanGetter = EventManager::isLoggingEventsDispatched,
          onToggle = {
            EventManager.getOrCreateEventsTracker().also { it.log = it.log xor EventsTracker.LOG_EVENTS_DISPATCHED }
          }
        )
        toggleableDebugButton(
          "Log listeners change",
          "Whether to log when an event is listener is registered",
          onAnyElementChanged = onAnyElementChanged,
          booleanGetter = EventManager::isLoggingEventListenersChange,
          onToggle = {
            EventManager.getOrCreateEventsTracker().also { it.log = it.log xor EventsTracker.LOG_EVENT_LISTENERS_CHANGE }
          }
        )
        toggleableDebugButton(
          "Log events listened to",
          "Whether to log when an event is listened to. This is very spammy",
          onAnyElementChanged = onAnyElementChanged,
          booleanGetter = EventManager::isLoggingEventsListenedTo,
          onToggle = {
            EventManager.getOrCreateEventsTracker().also { it.log = it.log xor EventsTracker.LOG_EVENTS_LISTENED_TO }
          }
        )
      }
    }
    pack()
  }.also { debugWindows += it }
  return DebugWindow(this@addDebugOverlay, debugMenu, onAnyElementChanged, debugWindows)
}
