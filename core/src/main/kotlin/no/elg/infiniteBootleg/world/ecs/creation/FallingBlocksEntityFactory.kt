package no.elg.infiniteBootleg.world.ecs.creation

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import ktx.ashley.EngineEntity
import ktx.ashley.entity
import ktx.ashley.with
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.Constants
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.box2d.Filters
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.ecs.basicDynamicEntityFamily
import no.elg.infiniteBootleg.world.ecs.blockEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent
import no.elg.infiniteBootleg.world.ecs.components.TextureRegionComponent
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent
import no.elg.infiniteBootleg.world.ecs.components.block.ChunkComponent
import no.elg.infiniteBootleg.world.ecs.components.block.OccupyingBlocksComponent
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEventQueue
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent
import no.elg.infiniteBootleg.world.ecs.components.tags.GravityAffectedTag.Companion.isAffectedByGravity
import no.elg.infiniteBootleg.world.ecs.drawableEntitiesFamily
import no.elg.infiniteBootleg.world.ecs.entityWithPhysicsEventFamily
import no.elg.infiniteBootleg.world.ecs.gravityAffectedBlockFamily
import no.elg.infiniteBootleg.world.ecs.with
import no.elg.infiniteBootleg.world.world.World

fun Engine.createFallingBlockStandaloneEntity(world: World, fallingBlock: ProtoWorld.Entity) {
  val material = Material.fromOrdinal(fallingBlock.material.materialOrdinal)
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
    with(WorldComponent(world))
    with(id?.let { IdComponent(it) } ?: IdComponent.createRandomId())
    with(PositionComponent(worldX, worldY))

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
        entityWithPhysicsEventFamily to "entityWithPhysicsEventFamily"
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

/**
 * Baseline static entities which have some system attached
 */
fun Engine.createBlockEntity(
  world: World,
  chunk: Chunk,
  worldX: Int,
  worldY: Int,
  material: Material,
  wantedFamilies: Array<Pair<Family, String>> = emptyArray(),
  additionalConfiguration: EngineEntity.() -> Unit = {}
) = entity {
  with(WorldComponent(world))
  with(IdComponent.createRandomId())
  with(PositionComponent(worldX.toFloat(), worldY.toFloat()))
  with(ChunkComponent(chunk))
  with(MaterialComponent(material))
  additionalConfiguration()
  checkFamilies(entity, arrayOf(blockEntityFamily to "blockEntityFamily", *wantedFamilies))
}

fun Engine.createGravityAffectedBlockEntity(world: World, chunk: Chunk, worldX: Int, worldY: Int, material: Material) =
  createBlockEntity(world, chunk, worldX, worldY, material, arrayOf(gravityAffectedBlockFamily to "gravityAffectedBlockFamily")) {
    this.entity.isAffectedByGravity = true
  }
