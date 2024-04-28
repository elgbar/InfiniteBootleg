package no.elg.infiniteBootleg.world.managers.container

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.utils.Disposable
import com.google.errorprone.annotations.concurrent.GuardedBy
import no.elg.infiniteBootleg.events.ContainerEvent
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.inventory.container.Container
import no.elg.infiniteBootleg.inventory.container.ContainerOwner
import no.elg.infiniteBootleg.inventory.container.OwnedContainer
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.world.ecs.components.inventory.ContainerComponent.Companion.containerOrNull
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

class ContainerOwnerListener(
  private val internalContainers: ConcurrentHashMap<ContainerOwner, OwnedContainer>,
  private val engine: Engine,
  family: Family,
  private val convertEntityToOwner: (Entity) -> ContainerOwner
) : EntityListener, Disposable {

  @GuardedBy("chunksLock")
  private val owners: Map<Entity, ContainerOwner> = Collections.synchronizedMap(WeakHashMap())

  init {
    engine.addEntityListener(family, this)
  }

  override fun entityAdded(entity: Entity) {
    val container: Container = entity.containerOrNull ?: run {
      Main.logger().error("Failed to add a container entity to the list of internal containers!")
      return
    }
    val owner = owners.getOrElse(entity) { convertEntityToOwner(entity) }
    val ownedContainer = OwnedContainer(owner, container)
    Main.logger().debug("ContainerOwnerListener") { "Adding container $ownedContainer" }
    internalContainers[owner] = ownedContainer
    EventManager.dispatchEvent(ContainerEvent.Added(ownedContainer))
  }

  override fun entityRemoved(entity: Entity) {
    val owner = owners[entity] ?: return
    val ownedContainer = internalContainers.remove(owner) ?: return
    Main.logger().debug("ContainerOwnerListener") { "Removing container $ownedContainer" }
    EventManager.dispatchEvent(ContainerEvent.Removed(ownedContainer))
  }

  override fun dispose() {
    engine.removeEntityListener(this)
    owners.values.forEach(internalContainers::remove)
  }
}