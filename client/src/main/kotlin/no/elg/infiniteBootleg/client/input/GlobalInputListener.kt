package no.elg.infiniteBootleg.client.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.screens.WorldScreen
import no.elg.infiniteBootleg.client.screens.hud.DEBUG_MENU_ID
import no.elg.infiniteBootleg.client.screens.hud.STAFF_CREATOR_ID
import no.elg.infiniteBootleg.client.screens.hud.addDebugOverlay
import no.elg.infiniteBootleg.client.screens.hud.addStaffCreatorOverlay
import no.elg.infiniteBootleg.core.main.Main

object GlobalInputListener : InputAdapter() {

  private var oldWidth = 0
  private var oldHeight = 0

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      Input.Keys.ESCAPE -> {
        val screen = ClientMain.inst().screen
        if (screen is WorldScreen && screen.isAnyDebugMenuVisible) {
          screen.world.render.interfaceManager.closeAllInterfaces()
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
            val _ = screen.world.render.interfaceManager.toggleInterface(STAFF_CREATOR_ID, screen.stage) {
              addStaffCreatorOverlay(screen.world)
            }
          }
        }
      }

      Input.Keys.F7 -> {
        if (Main.isClient) {
          val screen = ClientMain.inst().screen
          if (screen is WorldScreen) {
            val clientWorld = screen.world
            val interfaceManager = clientWorld.render.interfaceManager
            val _ = interfaceManager.toggleInterface(DEBUG_MENU_ID, screen.stage) {
              val staffMenu = interfaceManager.getOrCreate(STAFF_CREATOR_ID) { addStaffCreatorOverlay(clientWorld) }
              screen.stage.addDebugOverlay(clientWorld, staffMenu)
            }
          }
        }
      }
    }
    return false
  }
}
