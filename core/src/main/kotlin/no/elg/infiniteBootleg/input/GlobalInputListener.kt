package no.elg.infiniteBootleg.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.screens.WorldScreen

private val logger = KotlinLogging.logger {}

object GlobalInputListener : InputAdapter() {

  private var oldWidth = 0
  private var oldHeight = 0

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      Input.Keys.ESCAPE -> {
        val screen = ClientMain.inst().screen
        if (screen is WorldScreen && screen.isDebugMenuVisible) {
          screen.debugMenu.close()
          screen.staffMenu.close()
        }
        if (Main.inst().console.isVisible) {
          Main.inst().console.isVisible = false
        }
      }

      Input.Keys.ENTER -> {
        if (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT)) {
          val mode = Gdx.graphics.displayMode
          if (Gdx.graphics.isFullscreen) {
            if (ClientMain.scale > 1) {
              Gdx.graphics.setWindowedMode(1920, 1080)
            } else {
              Gdx.graphics.setWindowedMode(1280, 720)
            }
          } else {
            oldWidth = mode.width
            oldHeight = mode.height
            Gdx.graphics.setFullscreenMode(mode)
          }
          return true
        }
      }

      Input.Keys.F8 -> {
        if (Main.isClient) {
          val screen = ClientMain.inst().screen
          if (screen is WorldScreen) {
            screen.staffMenu.toggleShown(screen.stage)
          }
        }
      }

      Input.Keys.F7 -> {
        if (Main.isClient) {
          val screen = ClientMain.inst().screen
          if (screen is WorldScreen) {
            screen.debugMenu.toggleShown()
          }
        }
      }
    }
    return false
  }
}
