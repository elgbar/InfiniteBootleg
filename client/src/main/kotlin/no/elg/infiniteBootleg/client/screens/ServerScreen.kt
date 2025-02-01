package no.elg.infiniteBootleg.client.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.kotcrab.vis.ui.widget.VisTextField
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel
import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.scene2d.actor
import ktx.scene2d.horizontalGroup
import ktx.scene2d.vis.spinner
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.net.ClientChannel
import no.elg.infiniteBootleg.client.util.onInteract
import no.elg.infiniteBootleg.core.net.ServerClient
import no.elg.infiniteBootleg.core.net.serverBoundLoginPacket

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
object ServerScreen : StageScreen() {
  private val logger = KotlinLogging.logger {}
  override fun create() {
    super.create()
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
            ConnectingScreen.startLivelinessTest()

            val username = nameField.text
            val serverClient = ServerClient(username)
            val clientChannel = ClientChannel(serverClient)
            val thread = Thread({
              try {
                logger.info { "Trying to log into the server '${hostField.text}:${portSpinner.value}' with username '$username'" }
                clientChannel.connect(hostField.text, portSpinner.value) {
                  val channel = clientChannel.channel
                  ConnectingScreen.channel = channel
                  channel.writeAndFlush(serverBoundLoginPacket(username))
                }
              } catch (e: InterruptedException) {
                logger.info(e) { "Server interruption received" }
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
