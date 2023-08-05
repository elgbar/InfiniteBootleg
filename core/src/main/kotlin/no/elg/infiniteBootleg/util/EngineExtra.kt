package no.elg.infiniteBootleg.util

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import ktx.ashley.AshleyDsl
import ktx.ashley.CreateComponentException
import ktx.ashley.EngineEntity
import ktx.ashley.create
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Get or creates an instance of the component [T] and adds it to this [entity][EngineEntity].
 *
 * @param T the [Component] type to get or create.
 * @param configure inlined function with [T] as the receiver to allow additional configuration of the [Component].
 * @return the created ƒ[Component].
 * @throws [CreateComponentException] if the engine was unable to create the component
 * @see [create]
 */
inline fun <reified T : Component> EngineEntity.with(component: T, configure: (@AshleyDsl T).() -> Unit = {}): T {
  contract { callsInPlace(configure, InvocationKind.EXACTLY_ONCE) }
  component.configure()
  entity.add(component)
  return component
}

/**
 * Create and add an [Entity] to the [Engine].
 *
 * @param configure inlined function with the created [EngineEntity] as the receiver to allow further configuration of
 * the [Entity]. The [EngineEntity] holds the created [Entity] and this [Engine].
 * @param whenReady a [CompletableFuture] that when completed will add the entity to the engine. It has a timeout of 1 second. IT MUST BE COMPLETED!
 * @return the future created [Entity].
 */
inline fun Engine.futureEntity(configure: EngineEntity.(whenReady: CompletableFuture<Unit>) -> Unit = {}): CompletableFuture<Entity> {
  contract { callsInPlace(configure, InvocationKind.EXACTLY_ONCE) }
  val entity: Entity = createEntity()
  val configurationFuture = CompletableFuture<Unit>().orTimeout(1, TimeUnit.SECONDS).exceptionally { throw IllegalStateException("Failed to complete future entity init", it) }
  EngineEntity(this, entity).configure(configurationFuture)
  val addedToEngineFuture = CompletableFuture<Entity>().orTimeout(2, TimeUnit.SECONDS).exceptionally { throw IllegalStateException("Failed to add entity to engine", it) }
  configurationFuture.thenApply {
    entity.world.postBox2dRunnable {
      addEntity(entity)
      addedToEngineFuture.complete(entity)
    }
  }
  return addedToEngineFuture
}
