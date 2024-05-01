package no.elg.infiniteBootleg.world.world

import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.server.ServerClient

class ServerClientWorld(protoWorld: ProtoWorld.World, val serverClient: ServerClient) : ClientWorld(protoWorld, forceTransient = true) {

  override fun dispose() {
    super.dispose()
    serverClient.dispose()
  }
}
