package no.elg.infiniteBootleg.world.ecs.creation

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import ktx.ashley.entity
import ktx.ashley.with
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.Constants
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.box2d.Filters
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEventQueue
import no.elg.infiniteBootleg.world.ecs.components.tags.GravityAffectedTag.Companion.gravityAffected
import no.elg.infiniteBootleg.world.ecs.components.tags.LeafDecayTag.Companion.leafDecay
import no.elg.infiniteBootleg.world.ecs.components.transients.OccupyingBlocksComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.TextureRegionComponent
import no.elg.infiniteBootleg.world.ecs.drawableEntitiesFamily
import no.elg.infiniteBootleg.world.ecs.entityWithPhysicsEventFamily
import no.elg.infiniteBootleg.world.ecs.gravityAffectedBlockFamily
import no.elg.infiniteBootleg.world.ecs.leafBlockFamily
import no.elg.infiniteBootleg.world.ecs.standaloneGridOccupyingBlocksFamily
import no.elg.infiniteBootleg.world.world.World

fun Engine.createFallingBlockStandaloneEntity(world: World, fallingBlock: ProtoWorld.Entity) {
  val material = Material.fromOrdinal(fallingBlock.material.ordinal)
  createFallingBlockStandaloneEntity(
    world,
    fallingBlock.position.x,
    fallingBlock.position.y,
    fallingBlock.velocity.x,
    fallingBlock.velocity.y,
    material,
    fallingBlock.uuid
  )
}

fun Engine.createFallingBlockStandaloneEntity(
  world: World,
  worldX: Float,
  worldY: Float,
  dx: Float,
  dy: Float,
  material: Material,
  id: String? = null,
  onReady: (Entity) -> Unit = {}
) {
  entity {
    withRequiredComponents(ProtoWorld.Entity.EntityType.FALLING_BLOCK, world, worldX, worldY)

    // BASIC_DYNAMIC_ENTITY_ARRAY
    with(VelocityComponent(dx, dy))

    with(TextureRegionComponent(material.textureRegion ?: error("Failed to get ${material.name} material texture region")))

    // This entity will handle input events
    with<PhysicsEventQueue>()
    with(MaterialComponent(material))
    with<OccupyingBlocksComponent>()

    val bodyDef = BodyDef()
    bodyDef.type = BodyDef.BodyType.DynamicBody
    bodyDef.position.set(worldX, worldY)
    bodyDef.linearVelocity.set(dx, dy)
    bodyDef.linearDamping = 0.5f
    bodyDef.fixedRotation = true

    createBody2DBodyComponent(
      entity, world, worldX, worldY, dx, dy, 1f, 1f,
      arrayOf(
        basicDynamicEntityFamily to "basicDynamicEntityFamily",
        drawableEntitiesFamily to "drawableEntitiesFamily",
        entityWithPhysicsEventFamily to "entityWithPhysicsEventFamily",
        standaloneGridOccupyingBlocksFamily to "standaloneGridOccupyingBlocksFamily"
      )
    ) {
      val shape = PolygonShape()
      shape.setAsBox(0.45f, 0.45f)

      val def = FixtureDef()
      def.shape = shape
      def.density = Constants.DEFAULT_FIXTURE_DENSITY
      def.friction = Constants.DEFAULT_FIXTURE_FRICTION
      def.restitution = 0f
//      it.gravityScale = 0.01f

      val fix: Fixture = it.createFixture(def)
      fix.filterData = Filters.GR_FB__FALLING_BLOCK_FILTER
      fix.userData = it.userData
      shape.dispose()
      onReady(this.entity)
    }
  }
}

fun Engine.createGravityAffectedBlockEntity(world: World, chunk: Chunk, worldX: Int, worldY: Int, material: Material) =
  createBlockEntity(world, chunk, worldX, worldY, material, arrayOf(gravityAffectedBlockFamily to "gravityAffectedBlockFamily")) {
    this.entity.gravityAffected = true
  }

fun Engine.createLeafEntity(world: World, chunk: Chunk, worldX: Int, worldY: Int, material: Material) =
  createBlockEntity(world, chunk, worldX, worldY, material, arrayOf(leafBlockFamily to "leafBlockFamily")) {
    this.entity.leafDecay = true
  }
