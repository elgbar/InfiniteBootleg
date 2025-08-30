package no.elg.infiniteBootleg.core.world.managers.container

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.core.inventory.container.ContainerOwner
import no.elg.infiniteBootleg.core.inventory.container.OwnedContainer
import no.elg.infiniteBootleg.core.world.ecs.blockContainerFamily
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.core.world.ecs.entityContainerFamily
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class AuthoritativeWorldContainerManager(engine: Engine) :
  Disposable,
  WorldContainerManager {

  private val internalContainers = ConcurrentHashMap<ContainerOwner, OwnedContainer>()

  private val entityListeners = listOf(
    ContainerOwnerListener(internalContainers, engine, entityContainerFamily) { ContainerOwner.from(it) },
    ContainerOwnerListener(internalContainers, engine, blockContainerFamily) {
      val pos = it.positionComponent
      ContainerOwner.from(pos.blockX, pos.blockY)
    }
  )

  override fun find(owner: ContainerOwner): CompletableFuture<OwnedContainer> =
    internalContainers[owner]?.let { return CompletableFuture.completedFuture(it) }
      ?: CompletableFuture.failedFuture(NoSuchElementException("No container found with the owner $owner"))

  override fun dispose() {
    entityListeners.forEach(Disposable::dispose)
  }
}
