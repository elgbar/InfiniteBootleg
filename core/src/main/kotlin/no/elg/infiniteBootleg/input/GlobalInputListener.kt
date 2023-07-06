package no.elg.infiniteBootleg.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.screens.WorldScreen

object GlobalInputListener : InputAdapter() {

  private var oldWidth = 0
  private var oldHeight = 0

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      Input.Keys.ENTER -> {
        if (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT)) {
          val mode = Gdx.graphics.displayMode
          if (Gdx.graphics.isFullscreen) {
            if (ClientMain.SCALE > 1) {
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

      Input.Keys.F7 -> {
        val screen = ClientMain.inst().screen
        if (screen is WorldScreen) {
          screen.toggleDebugMenu()
        }
      }
    }
    return false
  }
}
