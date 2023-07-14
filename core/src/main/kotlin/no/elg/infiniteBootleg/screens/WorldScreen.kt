package no.elg.infiniteBootleg.screens

import com.kotcrab.vis.ui.widget.VisWindow
import ktx.actors.isShown
import ktx.assets.disposeSafely
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actors
import ktx.scene2d.vis.separator
import ktx.scene2d.vis.visWindow
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.events.WorldLoadedEvent
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.input.KeyboardControls.Companion.INITIAL_BRUSH_SIZE
import no.elg.infiniteBootleg.input.KeyboardControls.Companion.INITIAL_INTERACT_RADIUS
import no.elg.infiniteBootleg.screens.stage.floatSpinner
import no.elg.infiniteBootleg.screens.stage.toggleableDebugButton
import no.elg.infiniteBootleg.world.blocks.EntityMarkerBlock
import no.elg.infiniteBootleg.world.world.ClientWorld

/**
 * @author Elg
 */
class WorldScreen(val world: ClientWorld, val load: Boolean = true) : StageScreen() {

  var hud: HUDRenderer = HUDRenderer()
    private set

  private var worldFinishedLoading = false

  private val debugMenu: VisWindow

  override fun render(delta: Float) {
    if (worldFinishedLoading) {
      //noinspection ConstantConditions
      world.render.render()
      hud.render()
      Main.inst().console.draw()
      super.render(delta)
    }
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    world.resize(width, height)
    hud.resize(width, height)
  }

  override fun show() {
    super.show()
    require(!worldFinishedLoading) { "Same world screen can not be shown twice!" }
//    EventManager.clear()

    EventManager.oneShotListener { event: WorldLoadedEvent ->
      if (event.world === world) {
        worldFinishedLoading = true
        Main.logger().log("Finished loading world ${world.name} (${world.uuid})")
      }
    }

    ClientMain.inst().updateStatus(world)
    if (load) {
      world.initialize()
    }
    Main.inst().console.addToInputMultiplexer()

    ClientMain.inst().inputMultiplexer.addProcessor(world.input)
    ClientMain.inst().inputMultiplexer.addProcessor(world.ecsInput)
  }

  override fun hide() {
    super.hide()
    dispose()
  }

  override fun dispose() {
    super.dispose()
    if (Main.isSingleplayer()) {
      world.save()
    }
    ClientMain.inst().updateStatus(null)
    world.disposeSafely()
  }

  val isDebugMenuVisible: Boolean get() = debugMenu.isVisible || debugMenu.isShown()

  fun toggleDebugMenu() {
    debugMenu.toggleShown(stage, true)
  }

  init {
    stage.actors {
      debugMenu = visWindow("Debug Menu") {
        hide()
        defaults().space(5f).padLeft(2.5f).padRight(2.5f).padBottom(2.5f)

        @Scene2dDsl
        fun aRow(theRow: () -> Unit) {
          theRow()
          row()
        }
        aRow {
          toggleableDebugButton("General debug", Settings.debug, Main.inst().console.exec::debug)
          toggleableDebugButton("Debug render chunks", Settings.renderChunkBounds, Main.inst().console.exec::debChu)
          toggleableDebugButton("Debug chunk updates", Settings.renderChunkUpdates, Main.inst().console.exec::debChuUpd)
        }
        aRow {
          toggleableDebugButton("Debug block lighting", Settings.debugBlockLight, Main.inst().console.exec::debBlkLit)
          toggleableDebugButton("Debug entity lighting", Settings.debugEntityLight, Main.inst().console.exec::debEntLit)
          toggleableDebugButton("Render entity markers", false, EntityMarkerBlock::toggleDebugEntityMarkerBlocks)
        }
        aRow {
          toggleableDebugButton("Ignore place check", false, Main.inst().console.exec::placeCheck)
          toggleableDebugButton("Render lights", Settings.renderLight, Main.inst().console.exec::lights)
        }
        aRow {
          floatSpinner("Brush size", INITIAL_BRUSH_SIZE, 1f, 64f, 0.25f, Main.inst().console.exec::brush)
          floatSpinner("Reach radius", INITIAL_INTERACT_RADIUS, 1f, 1024f, 1f, Main.inst().console.exec::interactRadius)
        }
        aRow {
          separator {
            it.fillX()
            it.colspan(4)
          }
        }
        val box2dDebug = world.render.box2DDebugRenderer

        aRow {
          toggleableDebugButton("Debug Box2D", Settings.renderBox2dDebug, Main.inst().console.exec::debBox)
        }
        aRow {
          toggleableDebugButton("Box2D draw bodies", box2dDebug.isDrawBodies, Main.inst().console.exec::drawBodies)
          toggleableDebugButton("Box2D draw joints", box2dDebug.isDrawJoints, Main.inst().console.exec::drawJoints)
          toggleableDebugButton("Box2D draw AABBs", box2dDebug.isDrawAABBs, Main.inst().console.exec::drawAABBs)
        }

        aRow {
          toggleableDebugButton("Box2D draw inactiveBodies", box2dDebug.isDrawInactiveBodies, Main.inst().console.exec::drawInactiveBodies)
          toggleableDebugButton("Box2D draw velocities", box2dDebug.isDrawVelocities, Main.inst().console.exec::drawVelocities)
          toggleableDebugButton("Box2D draw contacts", box2dDebug.isDrawContacts, Main.inst().console.exec::drawContacts)
        }
        pack()
      }
    }
  }
}
