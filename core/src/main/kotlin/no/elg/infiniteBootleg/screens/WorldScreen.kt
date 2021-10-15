package no.elg.infiniteBootleg.screens

import com.badlogic.gdx.ScreenAdapter
import ktx.assets.disposeSafely
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.screen.HUDRenderer
import no.elg.infiniteBootleg.world.World

/**
 * @author Elg
 */
class WorldScreen(val world: World) : ScreenAdapter() {

  var hud: HUDRenderer = HUDRenderer()
    private set

  override fun render(delta: Float) {

    //noinspection ConstantConditions
    world.input?.update()
    if (!world.worldTicker.isPaused) {
      //only update controls when we're not paused
      for (entity in world.livingEntities) {
        entity.update()
      }
    }
    world.render.render()
    hud.render()
  }

  override fun resize(width: Int, height: Int) {
    world.resize(width, height)
  }

  override fun show() {
    Main.inst().setWorld(world)
    world.load()
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
