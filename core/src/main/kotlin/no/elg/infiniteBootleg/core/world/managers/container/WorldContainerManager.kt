package no.elg.infiniteBootleg.core.world.managers.container

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.core.inventory.container.ContainerOwner
import no.elg.infiniteBootleg.core.inventory.container.OwnedContainer
import no.elg.infiniteBootleg.core.util.WorldCoord
import java.util.concurrent.CompletableFuture

interface WorldContainerManager {

  /**
   * Find a container with the given [owner]
   * @return a future that will complete with the container at the given world coordinates or a failed future if none is found
   */
  fun find(owner: ContainerOwner): CompletableFuture<OwnedContainer>

  /**
   * Find a container at the given world coordinates
   * @return a future that will complete with the container at the given world coordinates or a failed future if none is found
   */
  fun find(worldX: WorldCoord, worldY: WorldCoord): CompletableFuture<OwnedContainer> = find(ContainerOwner.Companion.from(worldX, worldY))

  /**
   * Find a container at the given world coordinates
   * @return a future that will complete with the container at the given world coordinates or a failed future if none is found
   */
  fun find(entity: Entity): CompletableFuture<OwnedContainer> = find(ContainerOwner.Companion.from(entity))
}
