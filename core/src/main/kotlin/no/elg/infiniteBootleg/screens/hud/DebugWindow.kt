package no.elg.infiniteBootleg.screens.hud

import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisWindow
import com.kotcrab.vis.ui.widget.spinner.FloatSpinnerModel
import ktx.actors.isShown
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actors
import ktx.scene2d.horizontalGroup
import ktx.scene2d.vis.KVisWindow
import ktx.scene2d.vis.separator
import ktx.scene2d.vis.spinner
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visSlider
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visTextTooltip
import ktx.scene2d.vis.visWindow
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.screens.hide
import no.elg.infiniteBootleg.screens.toggleShown
import no.elg.infiniteBootleg.util.INITIAL_BRUSH_SIZE
import no.elg.infiniteBootleg.util.INITIAL_INSTANT_BREAK
import no.elg.infiniteBootleg.util.INITIAL_INTERACT_RADIUS
import no.elg.infiniteBootleg.util.toAbled
import no.elg.infiniteBootleg.world.blocks.EntityMarkerBlock
import no.elg.infiniteBootleg.world.ecs.components.LocallyControlledComponent.Companion.locallyControlledComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.tags.IgnorePlaceableCheckTag.Companion.ignorePlaceableCheck
import no.elg.infiniteBootleg.world.render.WorldRender.Companion.MAX_ZOOM
import no.elg.infiniteBootleg.world.world.ClientWorld
import java.math.BigDecimal

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
private fun KVisWindow.toggleableDebugButton(
  name: String,
  booleanGetter: () -> Boolean,
  onToggle: () -> Unit
): VisTextButton = visTextButton(name, style = "debug-menu-button") {
  pad(5f)
  isDisabled = booleanGetter()

  val tooltipLabel: VisLabel
  fun tooltipText() = "$name is ${isDisabled.toAbled()}"
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
private fun KVisWindow.floatSpinner(name: String, srcValueGetter: () -> Number, min: Number, max: Number, step: Number, decimals: Int, onChange: (Float) -> Unit) {
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
private fun KVisWindow.floatSlider(name: String, srcValueGetter: () -> Float, min: Float, max: Float, step: Float, onChange: (Float) -> Unit) {
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
      fun aRow(theRow: () -> Unit) {
        theRow()
        row()
      }

      @Scene2dDsl
      fun aSeparator() {
        aRow {
          separator {
            it.fillX()
            it.colspan(cols)
          }
        }
      }

      //
      aRow {
        toggleableDebugButton("General debug", Settings::debug, Main.inst().console.exec::debug)
        toggleableDebugButton("Render chunks borders", Settings::renderChunkBounds, Main.inst().console.exec::debChu)
        toggleableDebugButton("Render chunk updates", Settings::renderChunkUpdates, Main.inst().console.exec::debChuUpd)
        toggleableDebugButton("Render entity markers", Settings::debugEntityMarkerBlocks, EntityMarkerBlock::toggleDebugEntityMarkerBlocks)
      }
      // Lights
      aRow {
        toggleableDebugButton("Render entity lighting", Settings::debugEntityLight, Main.inst().console.exec::debEntLit)
        toggleableDebugButton("Render block lighting", Settings::debugBlockLight, Main.inst().console.exec::debBlkLit)
        toggleableDebugButton("Render light updates", Settings::renderBlockLightUpdates, Main.inst().console.exec::debLitUpd)
        toggleableDebugButton("Render lights", Settings::renderLight, Main.inst().console.exec::lights)
      }

      aSeparator()
      // Player related debug controls
      aRow {
        val entities = world.controlledPlayerEntities
        val instantBreakGetter: () -> Boolean = {
          entities
            .map { it.locallyControlledComponentOrNull }
            .firstOrNull()?.instantBreak ?: INITIAL_INSTANT_BREAK
        }

        val brushSizeGetter: () -> Number = {
          entities
            .map { it.locallyControlledComponentOrNull }
            .firstOrNull()?.brushSize ?: INITIAL_BRUSH_SIZE
        }
        val reachRadiusGetter: () -> Number = {
          entities
            .map { it.locallyControlledComponentOrNull }
            .firstOrNull()?.interactRadius ?: INITIAL_INTERACT_RADIUS
        }
        toggleableDebugButton("Ignore place check", { world.controlledPlayerEntities.any { it.ignorePlaceableCheck } }, Main.inst().console.exec::placeCheck)
        toggleableDebugButton("Instant break", instantBreakGetter, Main.inst().console.exec::instantBreak)
        floatSpinner("Brush size", brushSizeGetter, 1f, 64f, 0.25f, 2, Main.inst().console.exec::brush)
        floatSpinner("Reach radius", reachRadiusGetter, 1f, 512f, 1f, 0, Main.inst().console.exec::interactRadius)
      }

      aRow {
        val zoomValueGetter: () -> Float = { ClientMain.inst().world?.render?.camera?.zoom ?: 1f }
        floatSlider("Zoom", zoomValueGetter, 0.1f, MAX_ZOOM * 5, 0.1f) {
          val clientWorld = ClientMain.inst().world ?: return@floatSlider
          clientWorld.render.camera.zoom = it
        }
      }
      aSeparator()

      val box2dDebug = world.render.box2DDebugRenderer
      aRow {
        toggleableDebugButton("Debug Box2D", Settings::renderBox2dDebug, Main.inst().console.exec::debBox)
      }
      aRow {
        toggleableDebugButton("Box2D draw bodies", box2dDebug::isDrawBodies, Main.inst().console.exec::drawBodies)
        toggleableDebugButton("Box2D draw joints", box2dDebug::isDrawJoints, Main.inst().console.exec::drawJoints)
        toggleableDebugButton("Box2D draw AABBs", box2dDebug::isDrawAABBs, Main.inst().console.exec::drawAABBs)
        toggleableDebugButton("Box2D draw inactiveBodies", box2dDebug::isDrawInactiveBodies, Main.inst().console.exec::drawInactiveBodies)
      }
      aRow {
        toggleableDebugButton("Box2D draw velocities", box2dDebug::isDrawVelocities, Main.inst().console.exec::drawVelocities)
        toggleableDebugButton("Box2D draw contacts", box2dDebug::isDrawContacts, Main.inst().console.exec::drawContacts)
      }
      aSeparator()
      aRow {
        toggleableDebugButton("Vsync", Settings::vsync) { Settings.vsync = !Settings.vsync }
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
