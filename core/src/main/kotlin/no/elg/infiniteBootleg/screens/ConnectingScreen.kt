package no.elg.infiniteBootleg.screens

import com.badlogic.gdx.Input.Keys
import com.kotcrab.vis.ui.widget.VisLabel
import io.netty.channel.Channel
import ktx.scene2d.actor
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import no.elg.infiniteBootleg.Main

/**
 * @author Elg
 */
object ConnectingScreen : StageScreen() {

  var channel: Channel? = null
  private val field: VisLabel = VisLabel("...")

  var info: String
    get() = ConnectingScreen.field.text.toString()
    set(value) {
      Main.inst().consoleLogger.log("ConnectingScreen", value)
      ConnectingScreen.field.setText(value)
    }

  init {
    if (Main.inst().isNotTest) {
      rootTable {
        visTable(defaultSpacing = true) {
          addActor(actor(field))
        }
        row()
        visTextButton("Back") {
          onInteract(stage, Keys.ESCAPE, Keys.BACK) {
            channel?.close()
            Main.inst().screen = ServerScreen
          }
        }
      }
    }
  }
}
