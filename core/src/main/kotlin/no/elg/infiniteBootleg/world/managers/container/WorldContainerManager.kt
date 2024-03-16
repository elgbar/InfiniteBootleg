package no.elg.infiniteBootleg.world.managers.container

import no.elg.infiniteBootleg.inventory.container.Container
import no.elg.infiniteBootleg.inventory.container.impl.ContainerImpl
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.WorldKt
import no.elg.infiniteBootleg.util.WorldCompactLoc
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.compactLoc
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

abstract class WorldContainerManager {

  protected val containers = ConcurrentHashMap<WorldCompactLoc, Container>()

  fun createContainer(worldCompactLoc: WorldCompactLoc): Container =
    ContainerImpl(40, "Container").also {
      containers[worldCompactLoc] = it
    }

  fun findOrCreate(worldX: WorldCoord, worldY: WorldCoord): Container {
    val compact = compactLoc(worldX, worldY)
    return containers.getOrPut(compact) { createContainer(compact) }
  }

  /**
   * Find a container at the given world coordinates
   * @return a future that will complete with the container at the given world coordinates or a failed future if none is found
   */
  open fun find(worldX: WorldCoord, worldY: WorldCoord): CompletableFuture<Container> {
    val compact = compactLoc(worldX, worldY)
    val container = containers[compact] ?: return CompletableFuture.failedFuture(NoSuchElementException("No container at $worldX, $worldY"))
    return CompletableFuture.completedFuture(container)
  }

  open fun asProto(): ProtoWorld.World.WorldContainers = defaultProtoWorldContainers

  companion object {
    private val defaultProtoWorldContainers = WorldKt.worldContainers {}
  }
}
