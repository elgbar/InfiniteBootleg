package no.elg.infiniteBootleg.client.net

import kotlinx.coroutines.delay
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.screens.ConnectingScreen
import no.elg.infiniteBootleg.client.world.world.ServerClientWorld
import no.elg.infiniteBootleg.core.net.ChannelHandlerContextWrapper
import no.elg.infiniteBootleg.core.net.ServerClient
import no.elg.infiniteBootleg.core.net.serverBoundClientDisconnectPacket
import no.elg.infiniteBootleg.core.util.launchOnAsync
import no.elg.infiniteBootleg.core.util.launchOnMain
import no.elg.infiniteBootleg.core.world.world.World

val ServerClient.world: World get() = worldOrNull ?: ctx.fatal("World is not set for client $name")
val ServerClient.clientWorld: ServerClientWorld get() = world as? ServerClientWorld ?: ctx.fatal("Failed to get world as a ServerClientWorld, was ${world.javaClass.simpleName}")

internal fun ChannelHandlerContextWrapper.fatal(msg: String): Nothing {
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
  error(msg)
}
