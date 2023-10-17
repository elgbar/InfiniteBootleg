package no.elg.infiniteBootleg.world.ecs.creation

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import ktx.ashley.with
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.futureEntity
import no.elg.infiniteBootleg.util.safeWith
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent
import no.elg.infiniteBootleg.world.ecs.components.OccupyingBlocksComponent
import no.elg.infiniteBootleg.world.ecs.components.PhysicsEventQueueComponent
import no.elg.infiniteBootleg.world.ecs.components.TextureRegionComponent
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent
import no.elg.infiniteBootleg.world.ecs.components.tags.AuthoritativeOnlyTag.Companion.authoritativeOnly
import no.elg.infiniteBootleg.world.ecs.components.tags.CanBeOutOfBoundsTag.Companion.canBeOutOfBounds
import no.elg.infiniteBootleg.world.ecs.components.tags.GravityAffectedTag.Companion.gravityAffected
import no.elg.infiniteBootleg.world.ecs.components.tags.LeafDecayTag.Companion.leafDecay
import no.elg.infiniteBootleg.world.ecs.gravityAffectedBlockFamily
import no.elg.infiniteBootleg.world.ecs.leafBlockFamily
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
  futureEntity { future ->
    withRequiredComponents(ProtoWorld.Entity.EntityType.FALLING_BLOCK, world, worldX, worldY, id)

    // BASIC_DYNAMIC_ENTITY_ARRAY
    safeWith { VelocityComponent(dx, dy) }
    safeWith { TextureRegionComponent(material.textureRegion ?: error("Failed to get ${material.name} material texture region")) }

    // This entity will handle input events
    with<PhysicsEventQueueComponent>()
    safeWith { MaterialComponent(material) }
    with<OccupyingBlocksComponent>()
    createFallingBlockBodyComponent(world, worldX, worldY, dx, dy) {
      onReady(it)
      future.complete(Unit)
    }
  }
}

fun Engine.createGravityAffectedBlockEntity(
  world: World,
  chunk: Chunk,
  worldX: WorldCoord,
  worldY: WorldCoord,
  material: Material
) = createBlockEntity(world, chunk, worldX, worldY, material, arrayOf(gravityAffectedBlockFamily to "gravityAffectedBlockFamily")) {
  this.entity.gravityAffected = true
  this.entity.authoritativeOnly = true
}

fun Engine.createLeafEntity(
  world: World,
  chunk: Chunk,
  worldX: WorldCoord,
  worldY: WorldCoord,
  material: Material
) = createBlockEntity(world, chunk, worldX, worldY, material, arrayOf(leafBlockFamily to "leafBlockFamily")) {
  this.entity.leafDecay = true
  this.entity.canBeOutOfBounds = true // leaves can be out of bounds, as they are removed by chunks when it is unloaded
  this.entity.authoritativeOnly = true
}
