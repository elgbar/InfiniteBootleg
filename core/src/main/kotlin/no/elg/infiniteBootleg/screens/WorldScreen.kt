package no.elg.infiniteBootleg.screens

import ktx.assets.disposeSafely
import no.elg.infiniteBootleg.events.WorldLoadedEvent
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.screens.stage.DebugWindow
import no.elg.infiniteBootleg.screens.stage.addDebugOverlay
import no.elg.infiniteBootleg.world.world.ClientWorld

/**
 * @author Elg
 */
class WorldScreen(val world: ClientWorld, val load: Boolean = true) : StageScreen() {

  var hud: HUDRenderer = HUDRenderer()
    private set

  private var worldFinishedLoading = false

  private lateinit var debugMenu: DebugWindow

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
    if (Main.isSingleplayer) {
      world.save()
    }
    ClientMain.inst().updateStatus(null)
    world.disposeSafely()
  }

  val isDebugMenuVisible: Boolean get() = debugMenu.isDebugMenuVisible

  fun toggleDebugMenu() {
    debugMenu.toggleDebugMenu()
  }

  override fun create() {
    super.create()
    debugMenu = stage.addDebugOverlay(world)
  }
}
