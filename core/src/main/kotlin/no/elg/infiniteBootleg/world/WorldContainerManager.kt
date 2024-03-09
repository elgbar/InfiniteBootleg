package no.elg.infiniteBootleg.world

import no.elg.infiniteBootleg.inventory.container.Container
import no.elg.infiniteBootleg.inventory.container.Container.Companion.asProto
import no.elg.infiniteBootleg.inventory.container.Container.Companion.fromProto
import no.elg.infiniteBootleg.inventory.container.impl.ContainerImpl
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.WorldKt.WorldContainersKt.worldContainer
import no.elg.infiniteBootleg.protobuf.WorldKt.worldContainers
import no.elg.infiniteBootleg.util.WorldCompactLoc
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.compactLoc
import no.elg.infiniteBootleg.util.toCompact
import no.elg.infiniteBootleg.util.toVector2i
import no.elg.infiniteBootleg.world.ecs.api.ProtoConverter
import java.util.concurrent.ConcurrentHashMap
import no.elg.infiniteBootleg.protobuf.ProtoWorld.World as ProtoWorld

class WorldContainerManager {
  private val containers = ConcurrentHashMap<WorldCompactLoc, Container>()

  private fun createContainer(worldCompactLoc: WorldCompactLoc): Container =
    ContainerImpl(40, "Container").also {
      containers[worldCompactLoc] = it
    }

  fun findOrCreate(worldX: WorldCoord, worldY: WorldCoord): Container {
    val compact = compactLoc(worldX, worldY)
    return containers.getOrPut(compact) { createContainer(compact) }
  }

  companion object : ProtoConverter<WorldContainerManager, ProtoWorld.WorldContainers> {

    override fun ProtoWorld.WorldContainers.fromProto(): WorldContainerManager =
      WorldContainerManager().also { manager ->
        Main.logger().debug("WorldContainerManager") { "Loading ${containersList.size} containers in world" }
        containersList.forEach { containerProto ->
          manager.containers[containerProto.position.toCompact()] = containerProto.container.fromProto()
        }
      }

    override fun WorldContainerManager.asProto(): ProtoWorld.WorldContainers =
      worldContainers {
        containers += this@asProto.containers.map { (loc, worldContainer) ->
          worldContainer {
            position = loc.toVector2i()
            container = worldContainer.asProto()
          }
        }
      }
  }
}
