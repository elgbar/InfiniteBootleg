package no.elg.infiniteBootleg.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.screens.HUDRenderer
import no.elg.infiniteBootleg.screens.WorldScreen
import no.elg.infiniteBootleg.world.render.ClientWorldRender
import no.elg.infiniteBootleg.world.render.WorldRender
import no.elg.infiniteBootleg.world.ticker.Ticker
import no.elg.infiniteBootleg.world.world.ClientWorld

/**
 * @author Elg
 */
class WorldInputHandler(private val worldRender: ClientWorldRender) : InputAdapter(), Disposable {

  override fun keyDown(keycode: Int): Boolean {
    if (ClientMain.inst().shouldIgnoreWorldInput() || (Main.isMultiplayer && keycode != Input.Keys.F3)) {
      return false
    }
    when (keycode) {
      Input.Keys.F3 -> {
        val screen = ClientMain.inst().screen
        if (screen is WorldScreen) {
          val hud: HUDRenderer = screen.hud
          if (!hud.hasMode(HUDRenderer.DISPLAY_MINIMAL_DEBUG) &&
            !hud.hasMode(HUDRenderer.DISPLAY_DEBUG)
          ) {
            hud.enableMode(HUDRenderer.DISPLAY_MINIMAL_DEBUG)
          } else if (hud.hasMode(HUDRenderer.DISPLAY_MINIMAL_DEBUG)) {
            hud.enableMode(HUDRenderer.DISPLAY_DEBUG)
            hud.disableMode(HUDRenderer.DISPLAY_MINIMAL_DEBUG)
          } else {
            hud.disableMode(HUDRenderer.DISPLAY_DEBUG)
            hud.disableMode(HUDRenderer.DISPLAY_MINIMAL_DEBUG)
          }
          if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
            hud.toggleMode(HUDRenderer.DISPLAY_GRAPH_FPS)
          }
        }
      }

      Input.Keys.F5 -> world.save()
      Input.Keys.F9 -> world.reload()
      Input.Keys.F12 -> {
        val ticker: Ticker = world.worldTicker
        if (ticker.isPaused) {
          ticker.resume()
          Main.logger().log("World", "Ticker resumed by F12")
        } else {
          ticker.pause()
          Main.logger().log("World", "Ticker paused by F12")
        }
      }

      else -> return false
    }
    return true
  }

  override fun scrolled(amountX: Float, amountY: Float): Boolean {
    if (ClientMain.inst().shouldIgnoreWorldInput()) {
      return false
    }
    val camera = worldRender.camera
    camera.zoom += (amountX + amountY) / 2 * SCROLL_SPEED
    if (camera.zoom < WorldRender.MIN_ZOOM) {
      camera.zoom = WorldRender.MIN_ZOOM
    } else if (camera.zoom > WorldRender.MAX_ZOOM) {
      camera.zoom = WorldRender.MAX_ZOOM
    }
    worldRender.update()
    return true
  }

  val world: ClientWorld get() = worldRender.world

  override fun dispose() {
    ClientMain.inst().inputMultiplexer.removeProcessor(this)
  }

  companion object {
    private const val SCROLL_SPEED = 0.25f
  }
}
