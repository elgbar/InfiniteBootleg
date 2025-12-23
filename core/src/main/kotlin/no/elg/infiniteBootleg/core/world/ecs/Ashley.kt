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
import no.elg.infiniteBootleg.core.world.ecs.components.TextureRegionNameComponent
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
import no.elg.infiniteBootleg.core.world.ecs.components.transients.tags.ReactToEventTag
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

fun Family.Builder.getExcluding(vararg excluding: KClass<out Component>): Family = exclude(*excluding).get()
fun KClass<out Component>.toFamily(): Family = allOf(this).get()

val entityContainerFamily: Family = allOf(*DYNAMIC_STANDALONE_ENTITY, ContainerComponent::class).get()
val blockContainerFamily: Family = allOf(*BASIC_BLOCK_ENTITY, ContainerComponent::class).get()

val blockEntityFamily: Family = allOf(*BASIC_BLOCK_ENTITY).get()
val doorEntityFamily: Family = allOf(*BASIC_STANDALONE_ENTITY, DoorComponent::class).get()

val gravityAffectedBlockFamily: Family = allOf(*BASIC_BLOCK_ENTITY, GravityAffectedTag::class).get()
val gravityAffectedBlockFamilyActive: Family = allOf(*BASIC_BLOCK_ENTITY, GravityAffectedTag::class, ReactToEventTag::class).get()
val explosiveBlockFamily: Family = allOf(*BASIC_BLOCK_ENTITY, ExplosiveComponent::class).get()
val leafBlockFamily: Family = allOf(*BASIC_BLOCK_ENTITY, LeafDecayTag::class).get()

val standaloneGridOccupyingBlocksFamily: Family = allOf(*BASIC_STANDALONE_ENTITY, OccupyingBlocksComponent::class, MaterialComponent::class).get()

val playerFamily: Family = allOf(*PLAYERS_ENTITY_ARRAY).get()
val localPlayerFamily: Family = allOf(
  *PLAYERS_ENTITY_ARRAY,
  *INVENTORY_COMPONENTS,
  GroundedComponent::class,
  LocallyControlledComponent::class,
  FollowedByCameraTag::class,
  TextureRegionNameComponent::class,
  InputEventQueueComponent::class,
  PhysicsEventQueueComponent::class
).get()

val basicRequiredEntityFamily: Family = allOf(*REQUIRED_COMPONENTS).get()
val basicRequiredEntityFamilyToSendToClient: Family = allOf(*REQUIRED_COMPONENTS).getExcluding(AuthoritativeOnlyTag::class)
val basicStandaloneEntityFamily: Family = allOf(*BASIC_STANDALONE_ENTITY).get()
val drawableEntitiesFamily: Family = allOf(*BASIC_STANDALONE_ENTITY, TextureRegionNameComponent::class).get()
val selectedMaterialComponentFamily: Family = allOf(*BASIC_STANDALONE_ENTITY, *INVENTORY_COMPONENTS).get()
val basicDynamicEntityFamily: Family = allOf(*DYNAMIC_STANDALONE_ENTITY).get()

val followEntityFamily: Family = allOf(*BASIC_STANDALONE_ENTITY, FollowedByCameraTag::class).get()
val controlledEntityFamily: Family = allOf(*CONTROLLED_STANDALONE_ENTITY).get()

val controlledEntityWithInputEventFamily: Family = allOf(*CONTROLLED_STANDALONE_ENTITY, InputEventQueueComponent::class).get()
val entityWithPhysicsEventFamily: Family = allOf(
  *BASIC_STANDALONE_ENTITY,
  PhysicsEventQueueComponent::class
).get()

val spellEntityFamily: Family = allOf(*DYNAMIC_STANDALONE_ENTITY, TextureRegionNameComponent::class, SpellStateComponent::class).get()
val namedEntitiesFamily: Family = allOf(*REQUIRED_COMPONENTS, NameComponent::class).get()

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

const val BEFORE = -1
const val AFTER = 1

fun ensureUniquenessListener(engine: Engine) {
  if (Settings.enableUniquenessEntityIdCheck) {
    val knownIds = ObjectOpenHashSet<String>()
    val idFamily = IdComponent::class.toFamily()

    engine.onEntityAdded(idFamily, UPDATE_PRIORITY_ID_CHECK) { newlyAddedEntity ->
      val newId = newlyAddedEntity.id
      if (newId in knownIds) {
        logger.warn {
          val componentsString = newlyAddedEntity.toComponentsString()
          "Duplicate entity with id '$newId' removed: Components $componentsString"
        }
        newlyAddedEntity.removeSelf()
      } else {
        knownIds += newId
      }
    }

    engine.onEntityRemoved(idFamily, UPDATE_PRIORITY_ID_CHECK) { entity ->
      knownIds -= entity.id
    }
  }
}

fun disposeBox2dOnRemoval(engine: Engine) {
  val family = Box2DBodyComponent::class.toFamily()
  engine.onEntityRemoved(family, UPDATE_PRIORITY_ID_CHECK) { entity ->
    entity.box2d.dispose()
  }
}
