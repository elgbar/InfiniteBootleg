package no.elg.infiniteBootleg.screens

import com.badlogic.gdx.Input.Keys
import com.kotcrab.vis.ui.widget.VisTextField
import ktx.scene2d.actor
import ktx.scene2d.horizontalGroup
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.generator.PerlinChunkGenerator

/**
 * @author Elg
 */
object ConnectScreen : StageScreen() {
  init {
    rootTable {
      visTable(defaultSpacing = true) {

        val hostField = VisTextField("localhost")
        val portField = VisTextField("8558")

        horizontalGroup {
          space(10f)
          visLabel("Host: ")
          addActor(
            actor(hostField)
          )
        }
        row()
        horizontalGroup {
          space(10f)
          visLabel("Port: ")
          addActor(
            actor(portField)
          )
        }
        row()

        visTextButton("Connect") {
          onInteract(stage, Keys.NUM_0) {
            Main.inst().screen = WorldScreen(World(PerlinChunkGenerator(Settings.worldSeed.toLong()), Settings.worldSeed.toLong()))
          }
        }
        row()
        row()
        visTextButton("Back") {
          onInteract(stage, Keys.ESCAPE, Keys.BACK) {
            Main.inst().screen = MainMenuScreen
          }
        }
      }
    }
  }
}
