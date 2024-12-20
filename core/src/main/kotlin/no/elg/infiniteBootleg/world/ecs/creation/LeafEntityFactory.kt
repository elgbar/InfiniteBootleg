package no.elg.infiniteBootleg.world.ecs.creation

import com.badlogic.ashley.core.Engine
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.ecs.components.tags.AuthoritativeOnlyTag.Companion.authoritativeOnly
import no.elg.infiniteBootleg.world.ecs.components.tags.CanBeOutOfBoundsTag.Companion.canBeOutOfBounds
import no.elg.infiniteBootleg.world.ecs.components.tags.LeafDecayTag.Companion.leafDecay
import no.elg.infiniteBootleg.world.ecs.leafBlockFamily
import no.elg.infiniteBootleg.world.world.World

fun Engine.createLeafEntity(world: World, worldX: WorldCoord, worldY: WorldCoord, material: Material) =
  createBlockEntity(world, worldX, worldY, material, arrayOf(leafBlockFamily to "leafBlockFamily")) {
    entity.leafDecay = true
    entity.canBeOutOfBounds = true // leaves can be out of bounds, as they are removed by chunks when it is unloaded
    entity.authoritativeOnly = true
  }
