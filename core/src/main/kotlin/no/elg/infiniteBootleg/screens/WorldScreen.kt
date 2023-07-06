package no.elg.infiniteBootleg.screens

import com.kotcrab.vis.ui.widget.VisWindow
import ktx.actors.isShown
import ktx.assets.disposeSafely
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actors
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
        fun addToggle(title: String, onToggle: () -> Unit) {
          toggleableDebugButton(title, onToggle)
          row()
        }
        addToggle("Debug entity lighting", Main.inst().console.exec::debEntLit)
        addToggle("Debug Box2D", Main.inst().console.exec::debBox)
        addToggle("Debug render chunks", Main.inst().console.exec::debChu)
        addToggle("Debug block lighting", Main.inst().console.exec::debBlkLit)

        pack()
      }
    }
  }
}
