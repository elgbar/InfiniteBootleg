package no.elg.infiniteBootleg.world.ecs

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Family
import ktx.ashley.allOf
import ktx.ashley.onEntityAdded
import ktx.ashley.onEntityRemoved
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.world.ecs.components.EntityTypeComponent
import no.elg.infiniteBootleg.world.ecs.components.ExplosiveComponent
import no.elg.infiniteBootleg.world.ecs.components.InventoryComponent
import no.elg.infiniteBootleg.world.ecs.components.KillableComponent
import no.elg.infiniteBootleg.world.ecs.components.LocallyControlledComponent
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent
import no.elg.infiniteBootleg.world.ecs.components.NamedComponent
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent
import no.elg.infiniteBootleg.world.ecs.components.events.InputEventQueue
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEventQueue
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent
import no.elg.infiniteBootleg.world.ecs.components.tags.FollowedByCameraTag
import no.elg.infiniteBootleg.world.ecs.components.tags.GravityAffectedTag
import no.elg.infiniteBootleg.world.ecs.components.tags.LeafDecayTag
import no.elg.infiniteBootleg.world.ecs.components.transients.Box2DBodyComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.world.ecs.components.transients.ChunkComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.DoorComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.GroundedComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.OccupyingBlocksComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.SelectedInventoryItemComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.TextureRegionComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.WorldComponent
import kotlin.reflect.KClass

/**
 * The list of components **all** entities are expected to have
 */
val REQUIRED_COMPONENTS = arrayOf(
  IdComponent::class,
  WorldComponent::class,
  PositionComponent::class,
  EntityTypeComponent::class
)

/**
 * The list of components non-block entities must have
 */
val BASIC_STANDALONE_ENTITY = arrayOf(
  *REQUIRED_COMPONENTS,
  Box2DBodyComponent::class
)

val BASIC_BLOCK_ENTITY = arrayOf(
  *REQUIRED_COMPONENTS,
  MaterialComponent::class,
  ChunkComponent::class
)

val DYNAMIC_STANDALONE_ENTITY = arrayOf(
  *BASIC_STANDALONE_ENTITY,
  VelocityComponent::class
)
val CONTROLLED_STANDALONE_ENTITY = arrayOf(
  *DYNAMIC_STANDALONE_ENTITY,
  GroundedComponent::class,
  LocallyControlledComponent::class
)

val blockEntityFamily: Family = allOf(*BASIC_BLOCK_ENTITY).get()
val doorEntityFamily: Family = allOf(*BASIC_BLOCK_ENTITY, DoorComponent::class).get()

val gravityAffectedBlockFamily: Family = allOf(*BASIC_BLOCK_ENTITY, GravityAffectedTag::class).get()
val explosiveBlockFamily: Family = allOf(*BASIC_BLOCK_ENTITY, ExplosiveComponent::class).get()
val leafBlockFamily: Family = allOf(*BASIC_BLOCK_ENTITY, LeafDecayTag::class).get()

val fallingStandaloneBlockFamily: Family = allOf(*BASIC_STANDALONE_ENTITY, VelocityComponent::class, OccupyingBlocksComponent::class).get()
val standaloneGridOccupyingBlocksFamily: Family = allOf(*BASIC_STANDALONE_ENTITY, OccupyingBlocksComponent::class).get()

val PLAYERS_ENTITY_ARRAY = arrayOf(
  *DYNAMIC_STANDALONE_ENTITY,
  GroundedComponent::class,
  NamedComponent::class,
  KillableComponent::class,
  InventoryComponent::class
)
val playerFamily: Family = allOf(*PLAYERS_ENTITY_ARRAY).get()
val localPlayerFamily: Family = allOf(
  *PLAYERS_ENTITY_ARRAY,
  LocallyControlledComponent::class,
  FollowedByCameraTag::class,
  SelectedInventoryItemComponent::class,
  TextureRegionComponent::class
).get()

val basicRequiredEntityFamily: Family = allOf(*REQUIRED_COMPONENTS).get()
val basicStandaloneEntityFamily: Family = allOf(*BASIC_STANDALONE_ENTITY).get()
val drawableEntitiesFamily: Family = allOf(*BASIC_STANDALONE_ENTITY, TextureRegionComponent::class).get()
val selectedMaterialComponentFamily: Family = allOf(*BASIC_STANDALONE_ENTITY, SelectedInventoryItemComponent::class).get()
val basicDynamicEntityFamily: Family = allOf(*DYNAMIC_STANDALONE_ENTITY).get()

val followEntityFamily: Family = allOf(*BASIC_STANDALONE_ENTITY, FollowedByCameraTag::class).get()
val controlledEntityFamily: Family = allOf(*CONTROLLED_STANDALONE_ENTITY).get()

val controlledEntityWithInputEventFamily: Family = allOf(*CONTROLLED_STANDALONE_ENTITY, InputEventQueue::class).get()
val entityWithPhysicsEventFamily: Family = allOf(*BASIC_STANDALONE_ENTITY, PhysicsEventQueue::class).get()

fun KClass<out Component>.toFamily(): Family = allOf(this).get()

// ////////////////////////////////////
//  Common system update priorities  //
// ////////////////////////////////////

const val UPDATE_PRIORITY_ID_CHECK = Int.MIN_VALUE
const val UPDATE_PRIORITY_FIRST = -2_000
const val UPDATE_PRIORITY_EVENT_HANDLING = -1_500
const val UPDATE_PRIORITY_EARLY = -1_000
const val UPDATE_PRIORITY_DEFAULT = 0
const val UPDATE_PRIORITY_LATE = 1_000
const val UPDATE_PRIORITY_LAST = 2_000

fun ensureUniquenessListener(engine: Engine) {
  val family = IdComponent::class.toFamily()
  engine.onEntityAdded(family, UPDATE_PRIORITY_ID_CHECK) { entity ->
    val duplicateEntities = engine.getEntitiesFor(family)
    if (duplicateEntities.filter { it.id == entity.id }.size > 1) {
      Main.logger().warn("Duplicate entity with id '${entity.id}' removed")
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
