package no.elg.infiniteBootleg.world

import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.server.ServerClient

class ServerClientWorld(protoWorld: ProtoWorld.World, val serverClient: ServerClient) : ClientWorld(protoWorld)
