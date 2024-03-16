package no.elg.infiniteBootleg.world.managers.container

import no.elg.infiniteBootleg.inventory.container.Container.Companion.asProto
import no.elg.infiniteBootleg.inventory.container.Container.Companion.fromProto
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.WorldKt.WorldContainersKt.worldContainer
import no.elg.infiniteBootleg.protobuf.WorldKt.worldContainers
import no.elg.infiniteBootleg.util.toCompact
import no.elg.infiniteBootleg.util.toVector2i
import no.elg.infiniteBootleg.protobuf.ProtoWorld.World as ProtoWorld

class AuthoritativeWorldContainerManager : WorldContainerManager() {

  fun loadFromProto(proto: ProtoWorld.WorldContainers) {
    Main.logger().debug("WorldContainerManager") { "Loading ${proto.containersList.size} containers in world" }
    proto.containersList.forEach { containerProto ->
      containers[containerProto.position.toCompact()] = containerProto.container.fromProto()
    }
  }

  override fun asProto(): ProtoWorld.WorldContainers =
    worldContainers {
      containers += this@AuthoritativeWorldContainerManager.containers.map { (loc, worldContainer) ->
        worldContainer {
          position = loc.toVector2i()
          container = worldContainer.asProto()
        }
      }
    }
}
