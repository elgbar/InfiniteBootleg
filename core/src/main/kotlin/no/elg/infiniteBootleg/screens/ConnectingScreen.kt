package no.elg.infiniteBootleg.screens

import com.badlogic.gdx.Input.Keys
import com.kotcrab.vis.ui.widget.VisLabel
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.Channel
import ktx.scene2d.actor
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.server.fatal
import no.elg.infiniteBootleg.util.onInteract
import java.util.concurrent.ScheduledFuture
private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
object ConnectingScreen : StageScreen() {
  var channel: Channel? = null
  private val field: VisLabel = VisLabel("...")

  var info: String
    get() = ConnectingScreen.field.text.toString()
    set(value) {
      logger.debug { value }
      ConnectingScreen.field.setText(value)
    }

  /**
   * The connection attempt
   */
  private var connectAttempt = 0
  private var livelinessTest: ScheduledFuture<*>? = null
    set(value) {
      field?.cancel(false)
      field = value
    }

  fun startLivelinessTest() {
    val attempt = connectAttempt
    // note the delay must be more than 50ms
    livelinessTest = Main.inst().scheduler.scheduleSync(5000L) {
      if (channel == null) {
        logger.error { "Liveliness test is too early, connection not yet established" }
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
    livelinessTest = null
  }

  override fun create() {
    super.create()
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
