package no.elg.infiniteBootleg.world.ecs.creation

import com.badlogic.ashley.core.Engine
import ktx.ashley.allOf
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.safeWith
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.ecs.BASIC_BLOCK_ENTITY
import no.elg.infiniteBootleg.world.ecs.components.inventory.ContainerComponent
import no.elg.infiniteBootleg.world.ecs.components.tags.AuthoritativeOnlyTag.Companion.authoritativeOnly
import no.elg.infiniteBootleg.world.ecs.components.tags.CanBeOutOfBoundsTag.Companion.canBeOutOfBounds
import no.elg.infiniteBootleg.world.world.World

fun Engine.createContainerEntity(
  world: World,
  chunk: Chunk,
  worldX: WorldCoord,
  worldY: WorldCoord,
  material: Material
) = createBlockEntity(world, chunk, worldX, worldY, material, arrayOf(allOf(*BASIC_BLOCK_ENTITY, ContainerComponent::class).get() to "container block")) {
  this.entity.canBeOutOfBounds = true // leaves can be out of bounds, as they are removed by chunks when it is unloaded
  this.entity.authoritativeOnly = true
  entity.safeWith { ContainerComponent(world.worldContainerManager.findOrCreate(worldX, worldY)) }
}
