package no.elg.infiniteBootleg.core.world.ecs.creation

import com.badlogic.ashley.core.Engine
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.ecs.components.DecayingComponent
import no.elg.infiniteBootleg.core.world.ecs.decayingBlockFamily
import no.elg.infiniteBootleg.core.world.magic.parts.SunGem
import no.elg.infiniteBootleg.core.world.world.World

fun Engine.createPhosphorusSpellBlockEntity(world: World, worldX: WorldCoord, worldY: WorldCoord, material: Material) =
  createBlockEntity(world, worldX, worldY, material, arrayOf(decayingBlockFamily to "decayingBlockFamily")) {
    // Give it max power, it should be moderated by SunGem.onSpellLand
    entity.add(DecayingComponent(SunGem.maxPower))
  }
