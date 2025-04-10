package no.elg.infiniteBootleg.client.input

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.utils.Disposable
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.client.inventory.container.toggleContainer
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.screens.HUDRenderer
import no.elg.infiniteBootleg.client.screens.WorldScreen
import no.elg.infiniteBootleg.client.util.isControlPressed
import no.elg.infiniteBootleg.client.util.isShiftPressed
import no.elg.infiniteBootleg.client.world.render.ClientWorldRender
import no.elg.infiniteBootleg.client.world.world.ClientWorld
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.world.render.WorldRender
import no.elg.infiniteBootleg.core.world.ticker.Ticker

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
class WorldInputHandler(private val worldRender: ClientWorldRender) :
  InputAdapter(),
  Disposable {

  override fun keyDown(keycode: Int): Boolean {
    if (keycode != Input.Keys.TAB && (ClientMain.inst().shouldIgnoreWorldInput() || (Main.Companion.isMultiplayer && keycode != Input.Keys.F3))) {
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
          if (isShiftPressed()) {
            hud.toggleMode(HUDRenderer.DISPLAY_GRAPH_FPS)
          }
        }
      }

      Input.Keys.TAB -> world.controlledPlayerEntities.forEach { it.toggleContainer() }

      Input.Keys.F5 -> world.save()
      Input.Keys.F12 -> {
        val ticker: Ticker = world.worldTicker
        if (ticker.isPaused) {
          ticker.resume()
          logger.info { "Ticker resumed by F12" }
        } else {
          ticker.pause()
          logger.info { "Ticker paused by F12" }
        }
      }

      else -> return false
    }
    return true
  }

  override fun scrolled(amountX: Float, amountY: Float): Boolean {
    if (ClientMain.inst().shouldIgnoreWorldInput() || !isControlPressed()) {
      return false
    }
    val camera = worldRender.camera
    camera.zoom = (camera.zoom + (amountY * SCROLL_SPEED)).coerceIn(
      WorldRender.Companion.MIN_ZOOM,
      WorldRender.Companion.MAX_ZOOM
    )
    worldRender.update()
    return true
  }

  val world: ClientWorld get() = worldRender.world

  override fun dispose() {
    ClientMain.inst().inputMultiplexer.removeProcessor(this)
  }

  companion object {
    private const val SCROLL_SPEED = 1 / 8f
  }
}
