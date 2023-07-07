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
import no.elg.infiniteBootleg.events.WorldLoadedEvent
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.screen.HUDRenderer
import no.elg.infiniteBootleg.screens.stage.toggleableDebugButton
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
          toggleableDebugButton("Debug entity lighting", Main.inst().console.exec::debEntLit)
          toggleableDebugButton("Debug render chunks", Main.inst().console.exec::debChu)
        }
        aRow {
          toggleableDebugButton("Debug block lighting", Main.inst().console.exec::debBlkLit)
          toggleableDebugButton("Debug Box2D", Main.inst().console.exec::debBox)
        }
        aRow {
          separator {
            it.fillX()
            it.colspan(2)
          }
        }

        aRow {
          toggleableDebugButton("Box2D draw bodies", onToggle = Main.inst().console.exec::drawBodies)
          toggleableDebugButton("Box2D draw joints", onToggle = Main.inst().console.exec::drawJoints)
        }

        aRow {
          toggleableDebugButton("Box2D draw AABBs", onToggle = Main.inst().console.exec::drawAABBs)
          toggleableDebugButton("Box2D draw inactiveBodies", onToggle = Main.inst().console.exec::drawInactiveBodies)
        }

        aRow {
          toggleableDebugButton("Box2D draw velocities", onToggle = Main.inst().console.exec::drawVelocities)
          toggleableDebugButton("Box2D draw contacts", onToggle = Main.inst().console.exec::drawContacts)
        }
        pack()
      }
    }
  }
}
