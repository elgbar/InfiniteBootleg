package no.elg.infiniteBootleg.world.ecs.creation

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import ktx.ashley.with
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.futureEntity
import no.elg.infiniteBootleg.util.removeSelf
import no.elg.infiniteBootleg.util.safeWith
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent
import no.elg.infiniteBootleg.world.ecs.components.OccupyingBlocksComponent
import no.elg.infiniteBootleg.world.ecs.components.PhysicsEventQueueComponent
import no.elg.infiniteBootleg.world.ecs.components.TextureRegionComponent
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent
import no.elg.infiniteBootleg.world.ecs.components.tags.AuthoritativeOnlyTag.Companion.authoritativeOnly
import no.elg.infiniteBootleg.world.ecs.components.tags.GravityAffectedTag.Companion.gravityAffected
import no.elg.infiniteBootleg.world.ecs.gravityAffectedBlockFamily
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
    fallingBlock.ref.id
  )
}

/**
 * @param onReady will be called when the entity is ready to be used, if it returns false the entity will be removed
 */
fun Engine.createFallingBlockStandaloneEntity(
  world: World,
  worldX: Float,
  worldY: Float,
  dx: Float,
  dy: Float,
  material: Material,
  id: String? = null,
  onReady: (Entity) -> Boolean = { true }
) {
  futureEntity { future ->
    withRequiredComponents(ProtoWorld.Entity.EntityType.FALLING_BLOCK, world, worldX, worldY, id)

    // BASIC_DYNAMIC_ENTITY_ARRAY
    safeWith { VelocityComponent(dx, dy) }
    safeWith { TextureRegionComponent(material.textureRegion ?: error("Failed to get ${material.displayName} material texture region")) }

    // This entity will handle input events
    with<PhysicsEventQueueComponent>()
    safeWith { MaterialComponent(material) }
    with<OccupyingBlocksComponent>()
    createFallingBlockBodyComponent(world, worldX, worldY, dx, dy) {
      if (!onReady(it)) {
        it.removeSelf()
      }
      future.complete(Unit)
    }
  }
}

fun Engine.createGravityAffectedBlockEntity(world: World, worldX: WorldCoord, worldY: WorldCoord, material: Material) =
  createBlockEntity(world, worldX, worldY, material, arrayOf(gravityAffectedBlockFamily to "gravityAffectedBlockFamily")) {
    this.entity.gravityAffected = true
    this.entity.authoritativeOnly = true
  }
