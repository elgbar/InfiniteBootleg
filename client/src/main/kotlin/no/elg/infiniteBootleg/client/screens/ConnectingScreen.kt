package no.elg.infiniteBootleg.client.screens

import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.utils.Timer
import com.kotcrab.vis.ui.widget.VisLabel
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.Channel
import ktx.async.schedule
import ktx.scene2d.actor
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.net.fatal
import no.elg.infiniteBootleg.client.util.onInteract

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
  private var livelinessTest: Timer.Task? = null
    set(value) {
      field?.cancel()
      field = value
    }

  fun startLivelinessTest() {
    val attempt = connectAttempt
    // note the delay must be more than 50ms
    livelinessTest = schedule(10f) {
      if (channel == null) {
        logger.error { "Liveliness test is too early, connection not yet established" }
      } else if (ClientMain.inst().screen is ConnectingScreen && connectAttempt == attempt) {
        // We are still trying to connect after 5 seconds
        val msg = "Failed to connect, server stopped responding"
        ClientMain.inst().serverClient?.ctx?.fatal(msg) ?: also { info = msg }
      }
      livelinessTest = null
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
