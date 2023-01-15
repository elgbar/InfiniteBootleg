package no.elg.infiniteBootleg.screens

import com.badlogic.gdx.ScreenAdapter
import ktx.assets.disposeSafely
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.events.WorldLoadedEvent
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.screen.HUDRenderer
import no.elg.infiniteBootleg.world.ClientWorld

/**
 * @author Elg
 */
class WorldScreen(val world: ClientWorld, val load: Boolean = true) : ScreenAdapter() {

  var hud: HUDRenderer = HUDRenderer()
    private set

  private var worldFinishedLoading = false

  override fun render(delta: Float) {
    if (worldFinishedLoading) {
      //noinspection ConstantConditions
      world.input.update()
      world.render.render()
      hud.render()
      Main.inst().console.draw()
    }
  }

  override fun resize(width: Int, height: Int) {
    world.resize(width, height)
    hud.resize(width, height)
  }

  override fun show() {
    require(!worldFinishedLoading) { "Same world screen can not be shown twice!" }
    EventManager.clear()

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
    dispose()
  }

  override fun dispose() {
    if (Main.isSingleplayer()) {
      world.save()
    }
    ClientMain.inst().updateStatus(null)
    world.disposeSafely()
  }
}
