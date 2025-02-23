package no.elg.infiniteBootleg.core.world.ecs

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Family
import io.github.oshai.kotlinlogging.KotlinLogging
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.onEntityAdded
import ktx.ashley.onEntityRemoved
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.util.removeSelf
import no.elg.infiniteBootleg.core.util.toComponentsString
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.core.world.ecs.components.DoorComponent
import no.elg.infiniteBootleg.core.world.ecs.components.ExplosiveComponent
import no.elg.infiniteBootleg.core.world.ecs.components.GroundedComponent
import no.elg.infiniteBootleg.core.world.ecs.components.InputEventQueueComponent
import no.elg.infiniteBootleg.core.world.ecs.components.KillableComponent
import no.elg.infiniteBootleg.core.world.ecs.components.LocallyControlledComponent
import no.elg.infiniteBootleg.core.world.ecs.components.MaterialComponent
import no.elg.infiniteBootleg.core.world.ecs.components.NameComponent
import no.elg.infiniteBootleg.core.world.ecs.components.OccupyingBlocksComponent
import no.elg.infiniteBootleg.core.world.ecs.components.PhysicsEventQueueComponent
import no.elg.infiniteBootleg.core.world.ecs.components.TextureRegionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.ContainerComponent
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.HotbarComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.EntityTypeComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.IdComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent
import no.elg.infiniteBootleg.core.world.ecs.components.tags.AuthoritativeOnlyTag
import no.elg.infiniteBootleg.core.world.ecs.components.tags.FollowedByCameraTag
import no.elg.infiniteBootleg.core.world.ecs.components.tags.GravityAffectedTag
import no.elg.infiniteBootleg.core.world.ecs.components.tags.LeafDecayTag
import no.elg.infiniteBootleg.core.world.ecs.components.transients.SpellStateComponent
import no.elg.infiniteBootleg.core.world.ecs.components.transients.tags.ToBeDestroyedTag
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

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
  MaterialComponent::class
)

val DYNAMIC_STANDALONE_ENTITY = arrayOf(
  *BASIC_STANDALONE_ENTITY,
  VelocityComponent::class
)
val CONTROLLED_STANDALONE_ENTITY = arrayOf(
  *DYNAMIC_STANDALONE_ENTITY,
  GroundedComponent::class,
  LocallyControlledComponent::class,
  InputEventQueueComponent::class
)

val PLAYERS_ENTITY_ARRAY = arrayOf(
  *DYNAMIC_STANDALONE_ENTITY,
  NameComponent::class,
  KillableComponent::class,
  PhysicsEventQueueComponent::class
)
val INVENTORY_COMPONENTS = arrayOf(ContainerComponent::class, HotbarComponent::class)

/**
 * Build a family that excludes [no.elg.infiniteBootleg.core.world.ecs.components.transients.tags.ToBeDestroyedTag]. This means that all entities in the family are not stale
 */
fun Family.Builder.buildAlive(vararg additionalExcludes: KClass<out Component>): Family = exclude(*additionalExcludes, ToBeDestroyedTag::class).get()
fun KClass<out Component>.toFamily(vararg additionalExcludes: KClass<out Component>): Family = allOf(this).buildAlive(*additionalExcludes)

val entityContainerFamily: Family = allOf(*DYNAMIC_STANDALONE_ENTITY, ContainerComponent::class).buildAlive()
val blockContainerFamily: Family = allOf(*BASIC_BLOCK_ENTITY, ContainerComponent::class).buildAlive()

val blockEntityFamily: Family = allOf(*BASIC_BLOCK_ENTITY).buildAlive()
val doorEntityFamily: Family = allOf(*BASIC_BLOCK_ENTITY, DoorComponent::class).buildAlive()

val gravityAffectedBlockFamily: Family = allOf(*BASIC_BLOCK_ENTITY, GravityAffectedTag::class).buildAlive()
val explosiveBlockFamily: Family = allOf(*BASIC_BLOCK_ENTITY, ExplosiveComponent::class).buildAlive()
val leafBlockFamily: Family = allOf(*BASIC_BLOCK_ENTITY, LeafDecayTag::class).buildAlive()

val standaloneGridOccupyingBlocksFamily: Family = allOf(*BASIC_STANDALONE_ENTITY, OccupyingBlocksComponent::class, MaterialComponent::class).buildAlive()

val playerFamily: Family = allOf(*PLAYERS_ENTITY_ARRAY).buildAlive()
val localPlayerFamily: Family = allOf(
  *PLAYERS_ENTITY_ARRAY,
  *INVENTORY_COMPONENTS,
  GroundedComponent::class,
  LocallyControlledComponent::class,
  FollowedByCameraTag::class,
  TextureRegionComponent::class,
  InputEventQueueComponent::class,
  PhysicsEventQueueComponent::class
).buildAlive()

val basicRequiredEntityFamily: Family = allOf(*REQUIRED_COMPONENTS).buildAlive()
val basicRequiredEntityFamilyToSendToClient: Family = allOf(*REQUIRED_COMPONENTS).buildAlive(AuthoritativeOnlyTag::class)
val basicStandaloneEntityFamily: Family = allOf(*BASIC_STANDALONE_ENTITY).buildAlive()
val drawableEntitiesFamily: Family = allOf(*BASIC_STANDALONE_ENTITY, TextureRegionComponent::class).buildAlive()
val selectedMaterialComponentFamily: Family = allOf(*BASIC_STANDALONE_ENTITY, *INVENTORY_COMPONENTS).buildAlive()
val basicDynamicEntityFamily: Family = allOf(*DYNAMIC_STANDALONE_ENTITY).buildAlive()

val followEntityFamily: Family = allOf(*BASIC_STANDALONE_ENTITY, FollowedByCameraTag::class).buildAlive()
val controlledEntityFamily: Family = allOf(*CONTROLLED_STANDALONE_ENTITY).buildAlive()

val controlledEntityWithInputEventFamily: Family = allOf(*CONTROLLED_STANDALONE_ENTITY, InputEventQueueComponent::class).buildAlive()
val entityWithPhysicsEventFamily: Family = allOf(*BASIC_STANDALONE_ENTITY, PhysicsEventQueueComponent::class).buildAlive()

val staleEntityFamily: Family = allOf(ToBeDestroyedTag::class).get()

val spellEntityFamily: Family = allOf(*DYNAMIC_STANDALONE_ENTITY, TextureRegionComponent::class, SpellStateComponent::class).buildAlive()
val namedEntitiesFamily: Family = allOf(*REQUIRED_COMPONENTS, NameComponent::class).buildAlive()

// ////////////////////////////////////
//  Common system update priorities  //
// ////////////////////////////////////

const val UPDATE_PRIORITY_ID_CHECK = Int.MIN_VALUE
const val UPDATE_PRIORITY_BEFORE_EVENTS = -2_000
const val UPDATE_PRIORITY_EVENT_HANDLING = -1_500
const val UPDATE_PRIORITY_EARLY = -1_000
const val UPDATE_PRIORITY_DEFAULT = 0
const val UPDATE_PRIORITY_LATE = 1_000
const val UPDATE_PRIORITY_LAST = 2_000

private val knownIds = ObjectOpenHashSet<String>()

fun ensureUniquenessListener(engine: Engine) {
  val idFamily = IdComponent::class.toFamily()
  engine.onEntityAdded(idFamily, UPDATE_PRIORITY_ID_CHECK) { newlyAddedEntity ->
    val newId = newlyAddedEntity.id
    if (Settings.enableUniquenessEntityIdCheck && newId in knownIds) {
      logger.warn {
        val componentsString = newlyAddedEntity.toComponentsString()
        "Duplicate entity with id '$newId' removed: Components $componentsString"
      }
      newlyAddedEntity.removeSelf()
    } else {
      knownIds += newId
    }
  }
}

fun disposeEntitiesOnRemoval(engine: Engine) {
  val family = Box2DBodyComponent::class.toFamily()
  engine.onEntityRemoved(family, UPDATE_PRIORITY_ID_CHECK) { entity ->
    knownIds -= entity.id
    entity.box2d.dispose()
  }
}
