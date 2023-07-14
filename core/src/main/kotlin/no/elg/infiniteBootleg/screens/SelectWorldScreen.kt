package no.elg.infiniteBootleg.screens

import com.badlogic.gdx.Input.Keys
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.world.generator.chunk.PerlinChunkGenerator
import no.elg.infiniteBootleg.world.world.SinglePlayerWorld

/**
 * @author Elg
 */
object SelectWorldScreen : StageScreen() {
  override fun create() {
    super.create()
    rootTable {
      visTable(defaultSpacing = true) {
        visTextButton("Load World '${Settings.worldSeed}'") {
          onInteract(stage, Keys.NUM_0) {
            ClientMain.inst().screen = WorldScreen(SinglePlayerWorld(PerlinChunkGenerator(Settings.worldSeed.toLong()), Settings.worldSeed.toLong(), "World"))
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
