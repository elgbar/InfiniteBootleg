package no.elg.infiniteBootleg.screens

import com.badlogic.gdx.ScreenAdapter
import ktx.assets.disposeSafely
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.screen.HUDRenderer
import no.elg.infiniteBootleg.world.World

/**
 * @author Elg
 */
class WorldScreen(val world: World, val load: Boolean = true) : ScreenAdapter() {

  var hud: HUDRenderer = HUDRenderer()
    private set

  override fun render(delta: Float) {

    //noinspection ConstantConditions
    world.input?.update()
    if (!world.worldTicker.isPaused) {
      //only update controls when we're not paused
      for (player in world.players) {
        player.update()
      }
    }
    world.render.render()
    hud.render()
    Main.inst().console.draw()
  }

  override fun resize(width: Int, height: Int) {
    world.resize(width, height)
  }

  override fun show() {
    Main.inst().setWorld(world)

    if (load) {
      world.load()
    }
    if (Settings.client) {
      Main.inst().console.addToInputMultiplexer()
    }
    world.input?.also { ClientMain.inst().inputMultiplexer.addProcessor(it) }
  }

  override fun hide() {
    world.save()
    Main.inst().setWorld(null)
    dispose()
  }

  override fun dispose() {
    world.disposeSafely()
  }
}
