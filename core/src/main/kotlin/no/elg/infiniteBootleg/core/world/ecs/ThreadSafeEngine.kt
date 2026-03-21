package no.elg.infiniteBootleg.core.world.ecs

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.core.NonPooledComponentOperationHandler
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.utils.ObjectMap
import com.google.errorprone.annotations.concurrent.GuardedBy
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.events.api.ThreadType
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.util.CheckableDisposable
import no.elg.infiniteBootleg.core.util.EntityFlags.INVALID_FLAG
import no.elg.infiniteBootleg.core.util.EntityFlags.enableFlag
import no.elg.infiniteBootleg.core.util.isInvalid
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.system.AuthoritativeSystem
import no.elg.infiniteBootleg.core.world.ecs.system.api.AuthorizedEntitiesIteratingSystem

private val logger = KotlinLogging.logger {}

class ThreadSafeEngine :
  Engine(),
  CheckableDisposable {

  /**
   * If the engine has started. Thready type is not checked if not started.
   */
  private var started = false

  fun assertOnPhysicsThread() {
    if (started) {
      ThreadType.PHYSICS.requireCorrectThreadType { "This can only be called from the physics thread" }
    }
  }

  init {
    val engineClass = Engine::class.java
    val field = engineClass.getDeclaredField("componentOperationHandler").also { it.isAccessible = true }

    val engineDelayedInformerClass =
      engineClass.declaredClasses.find { it.name == "com.badlogic.ashley.core.Engine\$EngineDelayedInformer" } ?: error { "Failed to find EngineDelayedInformer class" }
    val constructor = engineDelayedInformerClass.getDeclaredConstructor(engineClass).apply { isAccessible = true }
    val engineDelayedInformer: Any = constructor?.newInstance(this) ?: error { "Failed to create EngineDelayedInformer" }

    field.set(this, NonPooledComponentOperationHandler(engineDelayedInformer))
  }

  fun start() {
    started = true
  }

  @GuardedBy("entityFamilyCache")
  private val entityFamilyCache = ObjectMap<Family, ImmutableArray<Entity>>()

  override fun createEntity(): Entity {
    assertOnPhysicsThread()
    return super.createEntity()
  }

  override fun <T : Component> createComponent(componentType: Class<T>): T {
    assertOnPhysicsThread()
    return super.createComponent(componentType)
  }

  override fun addEntity(entity: Entity) {
    assertOnPhysicsThread()
    super.addEntity(entity)
  }

  override fun removeEntity(entity: Entity) {
    assertOnPhysicsThread()
    if (entity.isInvalid) {
      return
    }
    entity.enableFlag(INVALID_FLAG)
    super.removeEntity(entity)
  }

  override fun removeAllEntities(family: Family) {
    assertOnPhysicsThread()
    super.removeAllEntities(family)
  }

  override fun removeAllEntities() {
    assertOnPhysicsThread()
    super.removeAllEntities()
  }

  override fun getEntities(): ImmutableArray<Entity> {
    assertOnPhysicsThread()
    return super.getEntities()
  }

  override fun addSystem(system: EntitySystem) {
    assertOnPhysicsThread()
    fun addToSystemConditionally(system: EntitySystem, type: String, cond: () -> Boolean) {
      if (cond()) {
        logger.debug { "Adding $type system ${system::class.simpleName}" }
        super.addSystem(system)
      } else {
        logger.debug { "Not adding $type system ${system::class.simpleName}" }
      }
    }

    if (system is AuthoritativeSystem && system is AuthorizedEntitiesIteratingSystem) {
      logger.warn {
        "System ${system::class.simpleName} is both an AuthoritativeSystem and an AuthorizedEntitiesIteratingSystem. " +
          "It is pointless to check if the system can modify an entity when we're authoritative"
      }
    }

    when (system) {
      is AuthoritativeSystem -> addToSystemConditionally(system, "authoritative") { Main.isAuthoritative }

      else -> {
        logger.debug { "Adding system ${system::class.simpleName}" }
        super.addSystem(system)
      }
    }
  }

  override fun removeSystem(system: EntitySystem) {
    assertOnPhysicsThread()
    super.removeSystem(system)
  }

  override fun removeAllSystems() {
    assertOnPhysicsThread()
    super.removeAllSystems()
  }

  inline fun <reified T : EntitySystem> getSystem(): T? = getSystem(T::class.java)

  override fun <T : EntitySystem> getSystem(systemType: Class<T>): T? {
    assertOnPhysicsThread()
    return super.getSystem(systemType)
  }

  override fun getSystems(): ImmutableArray<EntitySystem> {
    assertOnPhysicsThread()
    return super.getSystems()
  }

  override fun getEntitiesFor(family: Family): ImmutableArray<Entity> {
    assertOnPhysicsThread()
    synchronized(entityFamilyCache) {
      val entities = entityFamilyCache.get(family)
      if (entities != null) {
        return entities
      }
    }
    val entities = super.getEntitiesFor(family)
    synchronized(entityFamilyCache) {
      entityFamilyCache.put(family, entities)
    }
    return entities
  }

  override fun addEntityListener(listener: EntityListener) {
    assertOnPhysicsThread()
    super.addEntityListener(listener)
  }

  override fun addEntityListener(priority: Int, listener: EntityListener) {
    assertOnPhysicsThread()
    super.addEntityListener(priority, listener)
  }

  override fun addEntityListener(family: Family, listener: EntityListener) {
    assertOnPhysicsThread()
    super.addEntityListener(family, listener)
  }

  override fun addEntityListener(family: Family, priority: Int, listener: EntityListener) {
    assertOnPhysicsThread()
    super.addEntityListener(family, priority, listener)
  }

  override fun removeEntityListener(listener: EntityListener) {
    assertOnPhysicsThread()
    super.removeEntityListener(listener)
  }

  override fun update(deltaTime: Float) {
    assertOnPhysicsThread()
    super.update(deltaTime)
  }

  override fun addEntityInternal(entity: Entity) {
    assertOnPhysicsThread()
    super.addEntityInternal(entity)
  }

  override fun removeEntityInternal(entity: Entity) {
    assertOnPhysicsThread()
    super.removeEntityInternal(entity)
  }

  override var isDisposed: Boolean = false
    private set

  override fun dispose() {
    assertOnPhysicsThread()
    isDisposed = true
  }
}
