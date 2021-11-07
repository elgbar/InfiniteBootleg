package no.elg.infiniteBootleg.screens

import com.badlogic.gdx.Input.Keys
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.world.ClientWorld
import no.elg.infiniteBootleg.world.generator.PerlinChunkGenerator

/**
 * @author Elg
 */
object SelectWorldScreen : StageScreen() {
  init {
    rootTable {
      visTable(defaultSpacing = true) {
        visTextButton("Load World '${Settings.worldSeed}'") {
          onInteract(stage, Keys.NUM_0) {
            ClientMain.inst().screen = WorldScreen(ClientWorld(PerlinChunkGenerator(Settings.worldSeed.toLong()), Settings.worldSeed.toLong(), "World"))
          }
        }
//        row()
//        visTextButton("New World '${Settings.worldSeed}'") {
//          onInteract(stage, Keys.NUM_1) {
//            setScreen(WorldScreen(World(PerlinChunkGenerator(Settings.worldSeed.toLong()), Settings.worldSeed.toLong())))
//          }
//        }
        row()
        row()
        visTextButton("Back") {
          onInteract(stage, Keys.ESCAPE, Keys.BACK) {
            ClientMain.inst().screen = MainMenuScreen
          }
        }
      }
    }
  }
}
