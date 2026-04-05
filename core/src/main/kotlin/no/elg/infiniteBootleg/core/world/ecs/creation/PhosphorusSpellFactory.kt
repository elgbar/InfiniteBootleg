package no.elg.infiniteBootleg.core.world.ecs.creation

import com.badlogic.ashley.core.Engine
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.ecs.components.DecayingComponent
import no.elg.infiniteBootleg.core.world.ecs.components.tags.AuthoritativeOnlyTag.Companion.authoritativeOnly
import no.elg.infiniteBootleg.core.world.ecs.components.tags.CanBeOutOfBoundsTag.Companion.canBeOutOfBounds
import no.elg.infiniteBootleg.core.world.ecs.decayingBlockFamily
import no.elg.infiniteBootleg.core.world.magic.parts.SunGem
import no.elg.infiniteBootleg.core.world.world.World

fun Engine.createPhosphorusSpellEntity(world: World, worldX: WorldCoord, worldY: WorldCoord, material: Material) =
  createBlockEntity(world, worldX, worldY, material, arrayOf(decayingBlockFamily to "decayingBlockFamily")) {
    entity.add(DecayingComponent(SunGem.maxPower))
    entity.canBeOutOfBounds = true // leaves can be out of bounds, as they are removed by chunks when it is unloaded
    entity.authoritativeOnly = true
  }
