package no.elg.infiniteBootleg.client.net

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.screens.ConnectingScreen
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.net.ChannelHandlerContextWrapper
import no.elg.infiniteBootleg.core.net.serverBoundClientDisconnectPacket
import no.elg.infiniteBootleg.core.util.launchOnAsync
import no.elg.infiniteBootleg.core.util.launchOnMain

private val logger = KotlinLogging.logger {}

internal fun ChannelHandlerContextWrapper.fatal(msg: String) {
  require(Settings.client)
  launchOnAsync {
    delay(50L)
    close()
  }
  launchOnMain {
    ConnectingScreen.info = msg
    ClientMain.inst().screen = ConnectingScreen
    val serverClient = ClientMain.inst().serverClient
    if (serverClient?.sharedInformation != null) {
      this@fatal.writeAndFlushPacket(serverClient.serverBoundClientDisconnectPacket(msg))
    }
  }
  logger.error { msg }
}
