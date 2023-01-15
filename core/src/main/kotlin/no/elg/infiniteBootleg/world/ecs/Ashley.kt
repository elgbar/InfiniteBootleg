package no.elg.infiniteBootleg.world.ecs

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Family
import ktx.ashley.allOf
import ktx.ashley.onEntityAdded
import ktx.ashley.onEntityRemoved
import ktx.ashley.oneOf
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.world.ecs.components.ControlledComponent
import no.elg.infiniteBootleg.world.ecs.components.GroundedComponent
import no.elg.infiniteBootleg.world.ecs.components.KillableComponent
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent
import no.elg.infiniteBootleg.world.ecs.components.NamedComponent
import no.elg.infiniteBootleg.world.ecs.components.TextureRegionComponent
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent
import no.elg.infiniteBootleg.world.ecs.components.events.InputEvent.KeyDownEvent
import no.elg.infiniteBootleg.world.ecs.components.events.InputEvent.KeyTypedEvent
import no.elg.infiniteBootleg.world.ecs.components.events.InputEvent.KeyUpEvent
import no.elg.infiniteBootleg.world.ecs.components.events.InputEvent.MouseMovedEvent
import no.elg.infiniteBootleg.world.ecs.components.events.InputEvent.ScrolledEvent
import no.elg.infiniteBootleg.world.ecs.components.events.InputEvent.TouchDownEvent
import no.elg.infiniteBootleg.world.ecs.components.events.InputEvent.TouchDraggedEvent
import no.elg.infiniteBootleg.world.ecs.components.events.InputEvent.TouchUpEvent
import no.elg.infiniteBootleg.world.ecs.components.required.Box2DBodyComponent
import no.elg.infiniteBootleg.world.ecs.components.required.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent
import kotlin.reflect.KClass

/**
 * The list of components all entities are expected to have
 */
val BASIC_ENTITY_ARRAY = arrayOf(
  WorldComponent::class,
  IdComponent::class,
  Box2DBodyComponent::class,
  PositionComponent::class
)

val BASIC_DYNAMIC_ENTITY_ARRAY = arrayOf(
  *BASIC_ENTITY_ARRAY,
  VelocityComponent::class,
  GroundedComponent::class
)

val blockEntityFamily: Family = allOf(*BASIC_ENTITY_ARRAY, MaterialComponent::class, TextureRegionComponent::class).get()

val playerFamily: Family = allOf(*BASIC_DYNAMIC_ENTITY_ARRAY, NamedComponent::class, KillableComponent::class, TextureRegionComponent::class).get()

/**
 * The basic components ALL entities should have
 */
val basicEntityFamily: Family = allOf(*BASIC_ENTITY_ARRAY).get()
val drawableEntitiesFamily: Family = allOf(*BASIC_ENTITY_ARRAY, TextureRegionComponent::class).get()
val basicDynamicEntityFamily: Family = allOf(*BASIC_DYNAMIC_ENTITY_ARRAY).get()
val controlledEntityFamily: Family = allOf(*BASIC_DYNAMIC_ENTITY_ARRAY, ControlledComponent.LocallyControlledComponent::class).get()
val controlledEntityWithEventFamily: Family =
  allOf(*BASIC_DYNAMIC_ENTITY_ARRAY, ControlledComponent.LocallyControlledComponent::class)
    .oneOf(
      KeyDownEvent::class,
      KeyUpEvent::class,
      KeyTypedEvent::class,
      TouchDownEvent::class,
      TouchUpEvent::class,
      TouchDraggedEvent::class,
      MouseMovedEvent::class,
      ScrolledEvent::class
    ).get()

fun KClass<out Component>.toFamily(): Family = allOf(this).get()

// ////////////////////////////////////
//  Common system update priorities  //
// ////////////////////////////////////

const val UPDATE_PRIORITY_ID_CHECK = Int.MIN_VALUE
const val UPDATE_PRIORITY_FIRST = -2_000
const val UPDATE_PRIORITY_EARLY = -1_000
const val UPDATE_PRIORITY_DEFAULT = 0
const val UPDATE_PRIORITY_LATE = 1_000
const val UPDATE_PRIORITY_LAST = 2_000

fun ensureUniquenessListener(engine: Engine) {
  val family = IdComponent::class.toFamily()
  engine.onEntityAdded(family, UPDATE_PRIORITY_ID_CHECK) { entity ->
    val duplicateEntities = engine.getEntitiesFor(family)
    if (duplicateEntities.filter { it.id.id == entity.id.id }.size > 1) {
      Main.logger().warn("Duplicate entity with id '${entity.id.id}' added.")
      engine.removeEntity(entity)
    }
  }
}

fun disposeEntitiesOnRemoval(engine: Engine) {
  val family = Box2DBodyComponent::class.toFamily()
  engine.onEntityRemoved(family, UPDATE_PRIORITY_ID_CHECK) { entity ->
    entity.box2d.dispose()
  }
}
