package no.elg.infiniteBootleg.screens

import com.badlogic.gdx.Input.Keys
import com.kotcrab.vis.ui.widget.VisLabel
import io.netty.channel.Channel
import ktx.scene2d.actor
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.server.fatal
import java.util.concurrent.ScheduledFuture

/**
 * @author Elg
 */
object ConnectingScreen : StageScreen() {

  var channel: Channel? = null
  private val field: VisLabel = VisLabel("...")

  var info: String
    get() = ConnectingScreen.field.text.toString()
    set(value) {
      Main.logger().log("ConnectingScreen", value)
      ConnectingScreen.field.setText(value)
    }

  /**
   * The connection attempt
   */
  private var connectAttempt = 0
  private var livelinessTest: ScheduledFuture<*>? = null

  fun startLivelinessTest() {
    val attempt = connectAttempt
    // note the delay must be more than 50ms
    livelinessTest = Main.inst().scheduler.scheduleSync(5000L) {
      if (channel == null) {
        Main.logger().error("Liveliness Test", "Liveliness test is too early, connection not yet established")
      } else if (ClientMain.inst().screen is ConnectingScreen && connectAttempt == attempt) {
        // We are still trying to connect after 5 seconds
        ClientMain.inst().serverClient?.ctx?.fatal("Failed to connect, server stopped responding")
      }
    }
  }

  override fun show() {
    super.show()
    connectAttempt++
  }

  override fun hide() {
    super.hide()
    connectAttempt++
    livelinessTest?.cancel(false)
    livelinessTest = null
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
            ClientMain.inst().screen = ServerScreen
          }
        }
      }
    }
  }
}
