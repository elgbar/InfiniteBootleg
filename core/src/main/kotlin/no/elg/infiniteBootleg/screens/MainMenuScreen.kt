package no.elg.infiniteBootleg.screens

import com.badlogic.gdx.Input.Keys
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import no.elg.infiniteBootleg.ClientMain

/**
 * @author Elg
 */
object MainMenuScreen : StageScreen() {
  init {
    rootTable {
      visTable(defaultSpacing = true) {
        visTextButton("Singleplayer") {
          onInteract(stage, Keys.NUM_1) {
            ClientMain.inst().screen = SelectWorldScreen
          }
        }
        row()
        visTextButton("Multiplayer") {
          onInteract(stage, Keys.NUM_1) {
            ClientMain.inst().screen = ServerScreen
          }
        }
      }
    }
  }
}