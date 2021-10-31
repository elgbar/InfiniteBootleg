package no.elg.infiniteBootleg.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.kotcrab.vis.ui.widget.VisTextField
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel
import ktx.scene2d.actor
import ktx.scene2d.horizontalGroup
import ktx.scene2d.vis.spinner
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.server.ClientChannel
import no.elg.infiniteBootleg.server.ServerClient
import no.elg.infiniteBootleg.server.serverBoundLoginPacket
import no.elg.infiniteBootleg.util.randomUUIDFromString

/**
 * @author Elg
 */
object ServerScreen : StageScreen() {
  init {
    rootTable {
      visTable(defaultSpacing = true) {

        val nameField = VisTextField("Elg")
        val hostField = VisTextField("localhost")
        val portSpinner = IntSpinnerModel(8558, 0, Char.MAX_VALUE.code)


        horizontalGroup {
          space(10f)
          visLabel("Name: ")
          addActor(
            actor(nameField)
          )
        }
        row()
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
          spinner("Port: ", portSpinner) {
            cells.get(1)?.minWidth(minWidth)
          }
        }
        row()

        visTextButton("Connect") {
          onInteract(stage, Keys.NUM_0) {
            ConnectingScreen.info = "Connecting..."
            ClientMain.inst().screen = ConnectingScreen

            val serverClient = ServerClient(nameField.text)
            ClientMain.inst().serverClient = serverClient
            val clientChannel = ClientChannel(serverClient)
            val runnable = Runnable {
              val channel = clientChannel.channel ?: error("Could not connect to server")
              ConnectingScreen.channel = channel
              channel.writeAndFlush(serverBoundLoginPacket(nameField.text, randomUUIDFromString(nameField.text)))
            }
            val thread = Thread({
              try {
                clientChannel.connect(hostField.text, portSpinner.value, runnable)
              } catch (e: InterruptedException) {
                Main.logger().log("SERVER", "Server interruption received", e)
                Gdx.app.exit()
              }
            }, "Server")
            thread.isDaemon = true
            thread.start()
          }
        }
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
