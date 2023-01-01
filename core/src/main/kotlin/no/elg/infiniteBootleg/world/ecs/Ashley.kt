package no.elg.infiniteBootleg.world.ecs

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import ktx.ashley.allOf
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent
import no.elg.infiniteBootleg.world.ecs.components.OnGroundComponent
import no.elg.infiniteBootleg.world.ecs.components.PositionComponent
import no.elg.infiniteBootleg.world.ecs.components.SizedComponent
import no.elg.infiniteBootleg.world.ecs.components.TextureRegionComponent
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent
import no.elg.infiniteBootleg.world.ecs.components.WorldComponent

/**
 * The list of components all entities are expected to have
 */
val BASIC_ENTITY_ARRAY = arrayOf(
  WorldComponent::class,
  SizedComponent::class,
  Box2DBodyComponent::class,
  PositionComponent::class,
)

val BASIC_DYNAMIC_ENTITY_ARRAY = arrayOf(
  *BASIC_ENTITY_ARRAY,
  VelocityComponent::class,
  TextureRegionComponent::class,
  OnGroundComponent::class
)


val blockEntityFamily: Family = allOf(*BASIC_ENTITY_ARRAY).allOf(MaterialComponent::class).get()
val playerFamily: Family = allOf(*BASIC_DYNAMIC_ENTITY_ARRAY).get()

/**
 * The basic components ALL entities should have
 */
val basicEntityFamily: Family = allOf(*BASIC_ENTITY_ARRAY).get()
val basicDynamicEntityFamily: Family = allOf(*BASIC_DYNAMIC_ENTITY_ARRAY).get()

var Entity.world by propertyFor(WorldComponent.mapper)
var Entity.position by propertyFor(PositionComponent.mapper)
var Entity.velocity by propertyFor(VelocityComponent.mapper)
var Entity.box2d by propertyFor(Box2DBodyComponent.mapper)
var Entity.box2dOrNull by optionalPropertyFor(Box2DBodyComponent.mapper)
var Entity.sized by propertyFor(SizedComponent.mapper)
var Entity.texture by propertyFor(TextureRegionComponent.mapper)
var Entity.onGround by propertyFor(OnGroundComponent.mapper)
