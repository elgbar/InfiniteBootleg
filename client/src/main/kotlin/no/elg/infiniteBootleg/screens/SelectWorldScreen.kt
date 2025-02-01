package no.elg.infiniteBootleg.screens

import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.math.MathUtils
import com.kotcrab.vis.ui.widget.VisTextField
import ktx.actors.onChange
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visTextField
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.screens.hud.aSeparator
import no.elg.infiniteBootleg.screens.hud.section
import no.elg.infiniteBootleg.util.asWorldSeed
import no.elg.infiniteBootleg.util.onInteract
import no.elg.infiniteBootleg.world.generator.chunk.PerlinChunkGenerator
import no.elg.infiniteBootleg.world.world.SinglePlayerWorld

/**
 * @author Elg
 */
object SelectWorldScreen : StageScreen() {

  fun loadSingleplayerWorld(seed: Long, forceTransient: Boolean) {
    val world = SinglePlayerWorld(PerlinChunkGenerator(seed), seed, "World_$seed", forceTransient)
    ClientMain.inst().screen = WorldScreen(world)
  }

  override fun create() {
    super.create()
    rootTable {
      visTable(defaultSpacing = true) {
        section {
          visTextButton("Load World '${Settings.worldSeed}'") {
            onChange {
              loadSingleplayerWorld(Settings.worldSeed, forceTransient = false)
            }
          }
        }
        aSeparator(4)
        val seedTextField: VisTextField
        section {
          visLabel("Seed: ")
          seedTextField = visTextField(MathUtils.random.nextInt().toString()) {
            it.expandX().fillX()
          }
          visTextButton("Randomize seed") {
            onChange {
              seedTextField.text = MathUtils.random.nextInt().toString()
            }
          }
        }
        section {
          visTextButton("Random New World") {
            onChange {
              loadSingleplayerWorld(seedTextField.text.asWorldSeed(), forceTransient = false)
            }
          }
        }
        aSeparator(4)
        section {
          visTextButton("Back") {
            onInteract(stage, Keys.ESCAPE, Keys.BACK, Buttons.BACK) {
              ClientMain.inst().screen = MainMenuScreen
            }
          }
        }
      }
    }
  }
}
