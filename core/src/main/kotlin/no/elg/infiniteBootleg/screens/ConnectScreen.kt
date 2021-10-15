package no.elg.infiniteBootleg.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.kotcrab.vis.ui.widget.VisTextField
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel
import java.util.UUID
import ktx.scene2d.actor
import ktx.scene2d.horizontalGroup
import ktx.scene2d.vis.spinner
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Direction.SERVER
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type
import no.elg.infiniteBootleg.server.Client
import no.elg.infiniteBootleg.util.Util

/**
 * @author Elg
 */
object ConnectScreen : StageScreen() {
  init {
    rootTable {
      visTable(defaultSpacing = true) {

        val hostField = VisTextField("localhost")
        val portSpinner = IntSpinnerModel(8558, 0, Char.MAX_VALUE.code)

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
            val client = Client()
            val runnable = Runnable {
              println("Connected")
              val channel = client.channel ?: error("Could not connect to server")

              val packet = Packets.Packet.newBuilder()
                .setDirection(SERVER)
                .setType(Type.LOGIN)
                .setLoginPacker(
                  Packets.Login.newBuilder()
                    .setUsername("elg_")
                    .setUuid(UUID.randomUUID().toString())
                    .setVersion(Util.getVersion())
                ).build()

              channel.writeAndFlush(packet)
            }
            val thread = Thread({
              try {
                client.connect(hostField.text, portSpinner.value, runnable)
              } catch (e: InterruptedException) {
                Main.inst().console.log("SERVER", "Server interruption received", e)
                Gdx.app.exit()
              }
            }, "Server")
            thread.isDaemon = true
            thread.start()


//            Main.inst().screen = WorldScreen(World(PerlinChunkGenerator(Settings.worldSeed.toLong()), Settings.worldSeed.toLong()))
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
