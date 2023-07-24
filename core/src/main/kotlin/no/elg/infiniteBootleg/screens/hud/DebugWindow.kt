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
import ktx.scene2d.vis.KVisWindow
import ktx.scene2d.vis.separator
import ktx.scene2d.vis.spinner
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visTextTooltip
import ktx.scene2d.vis.visWindow
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.input.KeyboardControls
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.screens.hide
import no.elg.infiniteBootleg.screens.toggleShown
import no.elg.infiniteBootleg.util.toAbled
import no.elg.infiniteBootleg.world.blocks.EntityMarkerBlock
import no.elg.infiniteBootleg.world.ecs.components.tags.IgnorePlaceableCheckTag.Companion.ignorePlaceableCheck
import no.elg.infiniteBootleg.world.ecs.localPlayerFamily
import no.elg.infiniteBootleg.world.world.ClientWorld

class DebugWindow(private val stage: Stage, private val debugMenu: VisWindow) {

  val isDebugMenuVisible: Boolean get() = debugMenu.isVisible || debugMenu.isShown()

  fun toggleDebugMenu() {
    debugMenu.toggleShown(stage, true)
  }
}

private val onAnyButtonClicked = mutableListOf<() -> Unit>()

private fun updateAllButtons() {
  for (onClick in onAnyButtonClicked) {
    onClick()
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
    updateAllButtons()
  }
  it.fillX()
  onAnyButtonClicked += {
    isDisabled = booleanGetter()
    tooltipLabel.setText(tooltipText())
  }
}

@Scene2dDsl
private fun KVisWindow.floatSpinner(name: String, initialValue: Float, min: Float, max: Float, step: Float, onChange: (Float) -> Unit) {
  val model = FloatSpinnerModel(initialValue.toString(), min.toString(), max.toString(), step.toString())
  spinner(name, model) {
    it.fillX()
    onChange {
      onChange(model.value.toFloat())
    }
  }
}

fun Stage.addDebugOverlay(world: ClientWorld): DebugWindow {
  actors {
    val debugMenu = visWindow("Debug Menu") {
      hide()
      defaults().space(5f).padLeft(2.5f).padRight(2.5f).padBottom(2.5f)

      @Scene2dDsl
      fun aRow(theRow: () -> Unit) {
        theRow()
        row()
      }
      aRow {
        toggleableDebugButton("General debug", Settings::debug, Main.inst().console.exec::debug)
        toggleableDebugButton("Render chunks borders", Settings::renderChunkBounds, Main.inst().console.exec::debChu)
        toggleableDebugButton("Debug chunk updates", Settings::renderChunkUpdates, Main.inst().console.exec::debChuUpd)
      }
      aRow {
        toggleableDebugButton("Debug block lighting", Settings::debugBlockLight, Main.inst().console.exec::debBlkLit)
        toggleableDebugButton("Debug entity lighting", Settings::debugEntityLight, Main.inst().console.exec::debEntLit)
        toggleableDebugButton("Render entity markers", Settings::debugEntityMarkerBlocks, EntityMarkerBlock::toggleDebugEntityMarkerBlocks)
      }
      aRow {
        toggleableDebugButton("Ignore place check", { world.engine.getEntitiesFor(localPlayerFamily).any { it.ignorePlaceableCheck } }, Main.inst().console.exec::placeCheck)
        toggleableDebugButton("Render lights", Settings::renderLight, Main.inst().console.exec::lights)
        toggleableDebugButton("Render light updates", Settings::renderBlockLightUpdates, Main.inst().console.exec::debLitUpd)
      }
      aRow {
        floatSpinner("Brush size", KeyboardControls.INITIAL_BRUSH_SIZE, 1f, 64f, 0.25f, Main.inst().console.exec::brush)
        floatSpinner("Reach radius", KeyboardControls.INITIAL_INTERACT_RADIUS, 1f, 1024f, 1f, Main.inst().console.exec::interactRadius)
      }
      aRow {
        separator {
          it.fillX()
          it.colspan(4)
        }
      }
      val box2dDebug = world.render.box2DDebugRenderer

      aRow {
        toggleableDebugButton("Debug Box2D", Settings::renderBox2dDebug, Main.inst().console.exec::debBox)
      }
      aRow {
        toggleableDebugButton("Box2D draw bodies", box2dDebug::isDrawBodies, Main.inst().console.exec::drawBodies)
        toggleableDebugButton("Box2D draw joints", box2dDebug::isDrawJoints, Main.inst().console.exec::drawJoints)
        toggleableDebugButton("Box2D draw AABBs", box2dDebug::isDrawAABBs, Main.inst().console.exec::drawAABBs)
      }

      aRow {
        toggleableDebugButton("Box2D draw inactiveBodies", box2dDebug::isDrawInactiveBodies, Main.inst().console.exec::drawInactiveBodies)
        toggleableDebugButton("Box2D draw velocities", box2dDebug::isDrawVelocities, Main.inst().console.exec::drawVelocities)
        toggleableDebugButton("Box2D draw contacts", box2dDebug::isDrawContacts, Main.inst().console.exec::drawContacts)
      }
      pack()
    }
    return DebugWindow(this@addDebugOverlay, debugMenu)
  }
}
