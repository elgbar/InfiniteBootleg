package no.elg.infiniteBootleg.world.ecs

import com.badlogic.ashley.core.Component
import ktx.ashley.AshleyDsl
import ktx.ashley.CreateComponentException
import ktx.ashley.EngineEntity
import ktx.ashley.create
import kotlin.contracts.ExperimentalContracts
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
@OptIn(ExperimentalContracts::class)
inline fun <reified T : Component> EngineEntity.with(component: T, configure: (@AshleyDsl T).() -> Unit = {}): T {
  contract { callsInPlace(configure, InvocationKind.EXACTLY_ONCE) }
  component.configure()
  entity.add(component)
  return component
}
