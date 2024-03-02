package no.elg.infiniteBootleg.world.ecs.creation

import com.badlogic.ashley.core.Engine
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.ecs.components.tags.AuthoritativeOnlyTag.Companion.authoritativeOnly
import no.elg.infiniteBootleg.world.ecs.components.tags.CanBeOutOfBoundsTag.Companion.canBeOutOfBounds
import no.elg.infiniteBootleg.world.ecs.components.tags.LeafDecayTag.Companion.leafDecay
import no.elg.infiniteBootleg.world.ecs.leafBlockFamily
import no.elg.infiniteBootleg.world.world.World

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
