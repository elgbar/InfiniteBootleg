package no.elg.infiniteBootleg.input

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.utils.Disposable
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.screens.HUDRenderer
import no.elg.infiniteBootleg.screens.WorldScreen
import no.elg.infiniteBootleg.util.isControlPressed
import no.elg.infiniteBootleg.util.isShiftPressed
import no.elg.infiniteBootleg.world.ecs.components.inventory.ContainerComponent.Companion.toggleContainer
import no.elg.infiniteBootleg.world.render.ClientWorldRender
import no.elg.infiniteBootleg.world.render.WorldRender.Companion.MAX_ZOOM
import no.elg.infiniteBootleg.world.render.WorldRender.Companion.MIN_ZOOM
import no.elg.infiniteBootleg.world.ticker.Ticker
import no.elg.infiniteBootleg.world.world.ClientWorld

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
class WorldInputHandler(private val worldRender: ClientWorldRender) : InputAdapter(), Disposable {

  override fun keyDown(keycode: Int): Boolean {
    if (keycode != Input.Keys.TAB && (ClientMain.inst().shouldIgnoreWorldInput() || (Main.isMultiplayer && keycode != Input.Keys.F3))) {
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
    camera.zoom = (camera.zoom + (amountY * SCROLL_SPEED)).coerceIn(MIN_ZOOM, MAX_ZOOM)
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
