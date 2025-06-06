package no.elg.infiniteBootleg.core.world.ecs.creation

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import ktx.ashley.with
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.futureEntity
import no.elg.infiniteBootleg.core.util.removeSelf
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.Material.Companion.fromProto
import no.elg.infiniteBootleg.core.world.TexturedContainerElement
import no.elg.infiniteBootleg.core.world.ecs.components.MaterialComponent
import no.elg.infiniteBootleg.core.world.ecs.components.OccupyingBlocksComponent
import no.elg.infiniteBootleg.core.world.ecs.components.PhysicsEventQueueComponent
import no.elg.infiniteBootleg.core.world.ecs.components.TextureRegionNameComponent
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent
import no.elg.infiniteBootleg.core.world.ecs.components.tags.AuthoritativeOnlyTag.Companion.authoritativeOnly
import no.elg.infiniteBootleg.core.world.ecs.components.tags.GravityAffectedTag.Companion.gravityAffected
import no.elg.infiniteBootleg.core.world.ecs.components.transients.tags.ReactToEventTag.Companion.reactToEventTag
import no.elg.infiniteBootleg.core.world.ecs.gravityAffectedBlockFamilyActive
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.protobuf.ProtoWorld

fun Engine.createFallingBlockStandaloneEntity(world: World, fallingBlock: ProtoWorld.Entity) {
  createFallingBlockStandaloneEntity(
    world,
    fallingBlock.position.x,
    fallingBlock.position.y,
    fallingBlock.velocity.x,
    fallingBlock.velocity.y,
    fallingBlock.material.fromProto(),
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
    if (material is TexturedContainerElement) {
      safeWith { TextureRegionNameComponent(material.textureName) }
    }

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
  createBlockEntity(world, worldX, worldY, material, arrayOf(gravityAffectedBlockFamilyActive to "gravityAffectedBlockFamilyActive")) {
    entity.gravityAffected = true
    entity.authoritativeOnly = true
    entity.reactToEventTag = true
  }
