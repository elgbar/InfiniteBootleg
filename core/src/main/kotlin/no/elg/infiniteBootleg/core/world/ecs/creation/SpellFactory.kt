package no.elg.infiniteBootleg.core.world.ecs.creation

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import ktx.ashley.with
import no.elg.infiniteBootleg.core.assets.InfAssets
import no.elg.infiniteBootleg.core.util.futureEntity
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.ecs.components.OccupyingBlocksComponent
import no.elg.infiniteBootleg.core.world.ecs.components.TextureRegionNameComponent
import no.elg.infiniteBootleg.core.world.ecs.components.TintedComponent
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent
import no.elg.infiniteBootleg.core.world.ecs.components.transients.SpellStateComponent
import no.elg.infiniteBootleg.core.world.ecs.components.transients.tags.TransientEntityTag.Companion.isTransientEntity
import no.elg.infiniteBootleg.core.world.magic.SpellState
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.EntityType.SPELL

fun Engine.createSpellEntity(
  world: World,
  worldX: Float,
  worldY: Float,
  dx: Float,
  dy: Float,
  spellState: SpellState,
  id: String? = null,
  onReady: (Entity) -> Unit = {}
) {
  futureEntity { future ->
    withRequiredComponents(SPELL, world, worldX, worldY, id)

    // BASIC_DYNAMIC_ENTITY_ARRAY
    safeWith { VelocityComponent(dx, dy) }
    safeWith { TextureRegionNameComponent(InfAssets.Companion.SPELL_TEXTURE) }
    safeWith { TintedComponent(Color.RED) }
    this.entity.isTransientEntity = true

    // This entity will handle input events
//    with<PhysicsEventQueueComponent>()
    with<OccupyingBlocksComponent>()
    safeWith { SpellStateComponent(spellState, worldX, worldY, dx, dy) }
    createSpellBodyComponent(world, worldX, worldY, dx, dy) { entity ->
      spellState.entityModifications.forEach { modification -> entity.modification() }
      onReady(entity)
      future.complete(Unit)
    }
  }
}
