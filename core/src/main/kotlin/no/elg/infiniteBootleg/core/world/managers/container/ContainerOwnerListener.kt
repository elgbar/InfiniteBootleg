package no.elg.infiniteBootleg.core.world.managers.container

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.utils.Disposable
import com.google.errorprone.annotations.concurrent.GuardedBy
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.events.ContainerEvent
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.inventory.container.Container
import no.elg.infiniteBootleg.core.inventory.container.ContainerOwner
import no.elg.infiniteBootleg.core.inventory.container.OwnedContainer
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.ContainerComponent.Companion.containerOrNull
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}
class ContainerOwnerListener(
  private val internalContainers: ConcurrentHashMap<ContainerOwner, OwnedContainer>,
  private val engine: Engine,
  family: Family,
  private val convertEntityToOwner: (Entity) -> ContainerOwner
) : EntityListener,
  Disposable {

  @GuardedBy("chunksLock")
  private val owners: Map<Entity, ContainerOwner> = Collections.synchronizedMap(WeakHashMap())

  init {
    engine.addEntityListener(family, this)
  }

  override fun entityAdded(entity: Entity) {
    val container: Container = entity.containerOrNull ?: run {
      logger.error { "Failed to add a container entity to the list of internal containers!" }
      return
    }
    val owner = owners.getOrElse(entity) { convertEntityToOwner(entity) }
    val ownedContainer = OwnedContainer(owner, container)
    logger.debug { "Adding container $ownedContainer" }
    internalContainers[owner] = ownedContainer
    EventManager.dispatchEvent(ContainerEvent.Added(ownedContainer))
  }

  override fun entityRemoved(entity: Entity) {
    val owner = owners[entity] ?: return
    val ownedContainer = internalContainers.remove(owner) ?: return
    logger.debug { "Removing container $ownedContainer" }
    EventManager.dispatchEvent(ContainerEvent.Removed(ownedContainer))
  }

  override fun dispose() {
    engine.removeEntityListener(this)
    owners.values.forEach(internalContainers::remove)
  }
}
