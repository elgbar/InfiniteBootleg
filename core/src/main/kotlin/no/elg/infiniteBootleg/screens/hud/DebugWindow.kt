package no.elg.infiniteBootleg.screens.hud

import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisWindow
import com.kotcrab.vis.ui.widget.spinner.FloatSpinnerModel
import ktx.actors.isShown
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.scene2d.KTable
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actors
import ktx.scene2d.horizontalGroup
import ktx.scene2d.vis.separator
import ktx.scene2d.vis.spinner
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visSlider
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visTextTooltip
import ktx.scene2d.vis.visWindow
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.events.api.EventsTracker
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.screens.hide
import no.elg.infiniteBootleg.screens.toggleShown
import no.elg.infiniteBootleg.util.INITIAL_BRUSH_SIZE
import no.elg.infiniteBootleg.util.INITIAL_INSTANT_BREAK
import no.elg.infiniteBootleg.util.INITIAL_INTERACT_RADIUS
import no.elg.infiniteBootleg.util.toAbled
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.ecs.components.LocallyControlledComponent.Companion.locallyControlledComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.tags.IgnorePlaceableCheckTag.Companion.ignorePlaceableCheck
import no.elg.infiniteBootleg.world.render.WorldRender.Companion.MAX_ZOOM
import no.elg.infiniteBootleg.world.world.ClientWorld
import java.math.BigDecimal
import kotlin.concurrent.read

class DebugWindow(private val stage: Stage, private val debugMenu: VisWindow) {

  val isDebugMenuVisible: Boolean get() = debugMenu.isVisible || debugMenu.isShown()

  fun toggleDebugMenu() {
    if (!isDebugMenuVisible) {
      updateAllValues()
    }
    debugMenu.toggleShown(stage, true)
  }
}

private val onAnyElementChanged = mutableListOf<() -> Unit>()

private fun updateAllValues() {
  for (onChange in onAnyElementChanged) {
    onChange()
  }
}

@Scene2dDsl
private fun KTable.toggleableDebugButton(
  name: String,
  description: String? = null,
  style: String = "debug-menu-button",
  booleanGetter: () -> Boolean,
  onToggle: () -> Unit
): VisTextButton =
  visTextButton(name, style) {
    pad(5f)
    isDisabled = booleanGetter()

    val tooltipLabel: VisLabel
    fun tooltipText() = description ?: "$name is ${isDisabled.toAbled()}"
    visTextTooltip(tooltipText()) {
      tooltipLabel = this.content as VisLabel
    }

    onClick {
      onToggle()
      updateAllValues()
    }
    it.fillX()
    onAnyElementChanged += {
      isDisabled = booleanGetter()
      tooltipLabel.setText(tooltipText())
    }
  }

@Scene2dDsl
private fun KTable.floatSpinner(
  name: String,
  srcValueGetter: () -> Number,
  min: Number,
  max: Number,
  step: Number,
  decimals: Int,
  onChange: (Float) -> Unit
) {
  val model = FloatSpinnerModel(srcValueGetter().toString(), min.toString(), max.toString(), step.toString(), decimals)
  spinner(name, model) {
    it.fillX()

    onChange {
      onChange(model.value.toFloat())
      updateAllValues()
    }
    onAnyElementChanged += {
      model.setValue(BigDecimal.valueOf(srcValueGetter().toDouble()), false)
    }
  }
}

@Scene2dDsl
private fun KTable.floatSlider(
  name: String,
  srcValueGetter: () -> Float,
  min: Float,
  max: Float,
  step: Float,
  onChange: (Float) -> Unit
) {
  horizontalGroup {
    it.fillX()
    space(5f)
    visLabel(name)
    visSlider(min, max, step) {
      this.name = name
      setValue(srcValueGetter())

      onAnyElementChanged += {
        setValue(srcValueGetter())
      }
      onChange {
        onChange(this.value)
        updateAllValues()
      }
    }
  }
}

fun Stage.addDebugOverlay(world: ClientWorld): DebugWindow {
  actors {
    val debugMenu = visWindow("Debug Menu") {
      hide()
      defaults().space(5f).padLeft(2.5f).padRight(2.5f).padBottom(2.5f)

      val cols = 5

      @Scene2dDsl
      fun section(theRow: KTable.() -> Unit) {
        theRow()
        row()
      }

      @Scene2dDsl
      fun aSeparator() {
        section {
          separator {
            it.fillX()
            it.colspan(cols)
          }
        }
      }

      section {
        toggleableDebugButton(
          "General debug",
          "Handles whether to log debug messages and\nsmaller debug features without a dedicated debug option",
          booleanGetter = Settings::debug,
          onToggle = Main.inst().console.exec::debug
        )
        toggleableDebugButton(
          "Render chunks borders",
          "Render an outline around each chunk",
          booleanGetter = Settings::renderChunkBounds,
          onToggle = Main.inst().console.exec::debChu
        )
        toggleableDebugButton(
          "Render chunk updates",
          "Flash a chunk when a chunks texture changes",
          booleanGetter = Settings::renderChunkUpdates,
          onToggle = Main.inst().console.exec::debChuUpd
        )
        toggleableDebugButton(
          "Render entity markers",
          "Render entity block markers, a block to connect entities which represent a block as a temporary marker in the block world",
          booleanGetter = Settings::debugEntityMarkerBlocks
        ) { Settings.debugEntityMarkerBlocks = !Settings.debugEntityMarkerBlocks }
      }

      section {
        toggleableDebugButton(
          "Render existing air",
          "Render air blocks, which are normally indistinguishable from non-existing blocks, as cute little clouds ",
          booleanGetter = Settings::renderAirBlocks
        ) { Settings.renderAirBlocks = !Settings.renderAirBlocks }
        toggleableDebugButton(
          "Validate families",
          "Whether entity families should be validated. If invalid the entity will not be added to the entity engine",
          booleanGetter = Settings::validateEntityFamilies
        ) { Settings.validateEntityFamilies = !Settings.validateEntityFamilies }
        toggleableDebugButton(
          "Render top block changes",
          "Render changes to the top block of a chunk column",
          booleanGetter = Settings::renderTopBlockChanges
        ) { Settings.renderTopBlockChanges = !Settings.renderTopBlockChanges }
      }

      // Lights
      section {
        toggleableDebugButton(
          "Render entity lighting",
          "Render a small dot at the block the entity's light level is calculated from",
          booleanGetter = Settings::debugEntityLight,
          onToggle = Main.inst().console.exec::debEntLit
        )
        toggleableDebugButton(
          "Render block lighting",
          """Render an overlay which displays which blocks the light level of the block under the mouse is calculated.
A red overlay denotes a luminescent block, while a yellow overlay denotes the skylight is the source of the light.""",
          booleanGetter = Settings::debugBlockLight,
          onToggle = Main.inst().console.exec::debBlkLit
        )
        toggleableDebugButton(
          "Render light updates",
          "Render a small dot over blocks with new lighting",
          booleanGetter = Settings::renderBlockLightUpdates,
          onToggle = Main.inst().console.exec::debLitUpd
        )
        toggleableDebugButton(
          "Render lights",
          "Whether to render light at all. Will break lighting information if the world is updated with this off",
          booleanGetter = Settings::renderLight,
          onToggle = Main.inst().console.exec::lights
        )
      }
      section {
        toggleableDebugButton(
          "Recalculate lights",
          "Recalulate all lights in the world and re-render the chunks",
          "toggle-menu-button",
          { false }
        ) {
          world.chunksLock.read {
            world.chunks.values().forEach(Chunk::updateAllBlockLights)
          }
        }
        toggleableDebugButton(
          "Render all chunks",
          "Re-render the texture of all loaded chunks in the world",
          "toggle-menu-button",
          { false }
        ) {
          world.chunksLock.read {
            world.chunks.values().forEach { it.queueForRendering(false) }
          }
        }
      }
      // Event tracker
      section {
        toggleableDebugButton(
          "Log events",
          "Whether to log when anything event related happens",
          booleanGetter = EventManager::isLoggingAnyEvents,
          onToggle = Main.inst().console.exec::trackEvents
        )
        toggleableDebugButton("Log events dispatched", "Whether to log when an event is dispatched", booleanGetter = EventManager::isLoggingEventsDispatched) {
          EventManager.getOrCreateEventsTracker().also { it.log = it.log xor EventsTracker.LOG_EVENTS_DISPATCHED }
        }
        toggleableDebugButton("Log listeners change", "Whether to log when an event is listener is registered", booleanGetter = EventManager::isLoggingEventListenersChange) {
          EventManager.getOrCreateEventsTracker().also { it.log = it.log xor EventsTracker.LOG_EVENT_LISTENERS_CHANGE }
        }
        toggleableDebugButton(
          "Log events listened to",
          "Whether to log when an event is listened to. This is very spammy",
          booleanGetter = EventManager::isLoggingEventsListenedTo
        ) {
          EventManager.getOrCreateEventsTracker().also { it.log = it.log xor EventsTracker.LOG_EVENTS_LISTENED_TO }
        }
      }

      aSeparator()
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
          booleanGetter = { world.controlledPlayerEntities.any { it.ignorePlaceableCheck } },
          onToggle = Main.inst().console.exec::placeCheck
        )
        toggleableDebugButton(
          "Instant break",
          "Whether to instantly break blocks instead of slowly mining them",
          booleanGetter = instantBreakGetter,
          onToggle = Main.inst().console.exec::instantBreak
        )
        floatSpinner(
          "Brush size",
          brushSizeGetter,
          1f,
          64f,
          0.25f,
          2,
          Main.inst().console.exec::brush
        )
        floatSpinner("Reach radius", reachRadiusGetter, 1f, 512f, 1f, 0, Main.inst().console.exec::interactRadius)
      }

      section {
        val zoomValueGetter: () -> Float = { ClientMain.inst().world?.render?.camera?.zoom ?: 1f }
        floatSlider("Zoom", zoomValueGetter, 0.1f, MAX_ZOOM * 5, 0.1f) {
          val clientWorld = ClientMain.inst().world ?: return@floatSlider
          clientWorld.render.camera.zoom = it
          clientWorld.render.update()
        }
      }
      aSeparator()

      val box2dDebug = world.render.box2DDebugRenderer
      section {
        toggleableDebugButton("Debug Box2D", booleanGetter = Settings::renderBox2dDebug, onToggle = Main.inst().console.exec::debBox)
      }
      section {
        toggleableDebugButton("Box2D draw bodies", booleanGetter = box2dDebug::isDrawBodies, onToggle = Main.inst().console.exec::drawBodies)
        toggleableDebugButton("Box2D draw joints", booleanGetter = box2dDebug::isDrawJoints, onToggle = Main.inst().console.exec::drawJoints)
        toggleableDebugButton("Box2D draw AABBs", booleanGetter = box2dDebug::isDrawAABBs, onToggle = Main.inst().console.exec::drawAABBs)
        toggleableDebugButton("Box2D draw inactiveBodies", booleanGetter = box2dDebug::isDrawInactiveBodies, onToggle = Main.inst().console.exec::drawInactiveBodies)
      }
      section {
        toggleableDebugButton("Box2D draw velocities", booleanGetter = box2dDebug::isDrawVelocities, onToggle = Main.inst().console.exec::drawVelocities)
        toggleableDebugButton("Box2D draw contacts", booleanGetter = box2dDebug::isDrawContacts, onToggle = Main.inst().console.exec::drawContacts)
      }
      aSeparator()
      section {
        toggleableDebugButton("Vsync", booleanGetter = Settings::vsync) { Settings.vsync = !Settings.vsync }
        floatSpinner("Max FPS", Settings::foregroundFPS, 0, 512, 1, 0) { Settings.foregroundFPS = it.toInt() }
        val oneDayInSeconds = 86400f
        floatSpinner("Save every (sec)", Settings::savePeriodSeconds, 1f, oneDayInSeconds, 1f, 0) {
          Settings.savePeriodSeconds = it.toLong()
        }
      }
      pack()
    }
    return DebugWindow(this@addDebugOverlay, debugMenu)
  }
}
