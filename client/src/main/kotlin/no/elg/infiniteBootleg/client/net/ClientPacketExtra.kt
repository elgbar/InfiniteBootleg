package no.elg.infiniteBootleg.client.net

import kotlinx.coroutines.delay
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.screens.ConnectingScreen
import no.elg.infiniteBootleg.client.world.world.ServerClientWorld
import no.elg.infiniteBootleg.core.net.ChannelHandlerContextWrapper
import no.elg.infiniteBootleg.core.net.ServerClient
import no.elg.infiniteBootleg.core.net.serverBoundClientDisconnectPacket
import no.elg.infiniteBootleg.core.util.launchOnAsyncSuspendable
import no.elg.infiniteBootleg.core.util.launchOnMainSuspendable
import no.elg.infiniteBootleg.core.world.world.World

val ServerClient.world: World get() = worldOrNull ?: ctx.fatal("World is not set for client $name")
val ServerClient.clientWorld: ServerClientWorld get() = world as? ServerClientWorld ?: ctx.fatal("Failed to get world as a ServerClientWorld, was ${world.javaClass.simpleName}")

internal fun ChannelHandlerContextWrapper.fatal(msg: String): Nothing {
  launchOnAsyncSuspendable {
    delay(50L)
    close()
  }
  launchOnMainSuspendable {
    ConnectingScreen.info = msg
    ClientMain.inst().screen = ConnectingScreen
    val serverClient = ClientMain.inst().serverClient
    if (serverClient?.sharedInformation != null) {
      this@fatal.writeAndFlushPacket(serverClient.serverBoundClientDisconnectPacket(msg))
    }
    serverClient?.dispose()
  }
  error(msg)
}
