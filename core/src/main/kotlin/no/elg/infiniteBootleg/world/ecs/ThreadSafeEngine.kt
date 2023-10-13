package no.elg.infiniteBootleg.world.ecs

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectMap
import com.google.errorprone.annotations.concurrent.GuardedBy
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.world.ecs.api.restriction.ClientSystem
import no.elg.infiniteBootleg.world.ecs.api.restriction.ServerSystem
import no.elg.infiniteBootleg.world.ecs.components.events.ECSEventQueueComponent

class ThreadSafeEngine : Engine(), Disposable {

  private val engineLock = Any()

  @GuardedBy("entityFamilyCache")
  private val entityFamilyCache = ObjectMap<Family, ImmutableArray<Entity>>()

  override fun createEntity(): Entity {
    return super.createEntity()
  }

  override fun <T : Component> createComponent(componentType: Class<T>): T {
    return super.createComponent(componentType)
  }

  override fun addEntity(entity: Entity): Unit =
    synchronized(engineLock) {
      super.addEntity(entity)
    }

  override fun removeEntity(entity: Entity): Unit =
    synchronized(engineLock) {
      super.removeEntity(entity)
    }

  override fun removeAllEntities(family: Family): Unit =
    synchronized(engineLock) {
      super.removeAllEntities(family)
    }

  override fun removeAllEntities(): Unit =
    synchronized(engineLock) {
      super.removeAllEntities()
    }

  override fun getEntities(): ImmutableArray<Entity> =
    synchronized(engineLock) {
      return super.getEntities()
    }

  override fun addSystem(system: EntitySystem): Unit =
    synchronized(engineLock) {
      if (system is ClientSystem && system is ServerSystem) {
        Main.logger().debug("Engine", "Adding duplex system ${system::class.simpleName}")
        super.addSystem(system)
      } else if (system is ClientSystem) {
        if (Main.isClient) {
          Main.logger().debug("Engine", "Adding client only system ${system::class.simpleName}")
          super.addSystem(system)
        } else {
          Main.logger().debug("Engine", "Not adding client only system ${system::class.simpleName}")
        }
      } else if (system is ServerSystem) {
        if (Main.isServer) {
          Main.logger().debug("Engine", "Adding server only system ${system::class.simpleName}")
          super.addSystem(system)
        } else {
          Main.logger().debug("Engine", "Not adding server only system ${system::class.simpleName}")
        }
      } else {
        Main.logger().warn("Engine", "System ${system::class.simpleName} is not a client or server system, it might be using server/client main and might crash when used")
        super.addSystem(system)
      }
    }

  override fun removeSystem(system: EntitySystem): Unit =
    synchronized(engineLock) {
      super.removeSystem(system)
    }

  override fun removeAllSystems() =
    synchronized(engineLock) {
      super.removeAllSystems()
    }

  override fun <T : EntitySystem> getSystem(systemType: Class<T>): T? =
    synchronized(engineLock) {
      return super.getSystem(systemType)
    }

  override fun getSystems(): ImmutableArray<EntitySystem> =
    synchronized(engineLock) {
      return super.getSystems()
    }

  override fun getEntitiesFor(family: Family): ImmutableArray<Entity> {
    synchronized(entityFamilyCache) {
      val entities = entityFamilyCache.get(family)
      if (entities != null) {
        return entities
      }
    }
    val entities = synchronized(engineLock) {
      super.getEntitiesFor(family)
    }
    synchronized(entityFamilyCache) {
      entityFamilyCache.put(family, entities)
    }
    return entities
  }

  override fun addEntityListener(listener: EntityListener): Unit =
    synchronized(engineLock) {
      super.addEntityListener(listener)
    }

  override fun addEntityListener(priority: Int, listener: EntityListener): Unit =
    synchronized(engineLock) {
      super.addEntityListener(priority, listener)
    }

  override fun addEntityListener(family: Family, listener: EntityListener): Unit =
    synchronized(engineLock) {
      super.addEntityListener(family, listener)
    }

  override fun addEntityListener(family: Family, priority: Int, listener: EntityListener): Unit =
    synchronized(engineLock) {
      super.addEntityListener(family, priority, listener)
    }

  override fun removeEntityListener(listener: EntityListener): Unit =
    synchronized(engineLock) {
      super.removeEntityListener(listener)
    }

  override fun update(deltaTime: Float): Unit =
    synchronized(engineLock) {
      super.update(deltaTime)
    }

  override fun dispose() {
    ECSEventQueueComponent.entitiesCache.clear()
  }
}
