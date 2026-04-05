package no.elg.infiniteBootleg.core.world.magic.parts

import com.badlogic.ashley.core.Entity
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.util.toTitleCase
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.Tool
import no.elg.infiniteBootleg.core.world.ecs.components.DecayingComponent
import no.elg.infiniteBootleg.core.world.ecs.components.DecayingComponent.Companion.decayComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.core.world.magic.Description
import no.elg.infiniteBootleg.core.world.magic.MagicEffectsWithRating
import no.elg.infiniteBootleg.core.world.magic.Named
import no.elg.infiniteBootleg.core.world.magic.SpellState

private val logger = KotlinLogging.logger {}

enum class GemRating(val powerPercent: Double) : Named {
  FLAWLESS(1.0),
  SCRATCHED(0.85),
  CHIPPED(0.7),
  CRACKED(0.5),
  FRACTURED(0.25),
  RUINED(0.0);

  override val displayName: String = name.toTitleCase()
}

sealed interface GemType :
  Named,
  Description,
  MagicEffectsWithRating<GemRating> {

  val maxPower: Double

  companion object {
    fun valueOf(serializedName: String): GemType? =
      when (serializedName) {
        Diamond.serializedName -> Diamond

        else -> {
          logger.error { "Failed to parse gem type '$serializedName', it will be absent" }
          null
        }
      }
  }
}

data object Diamond : GemType {

  override val displayName: String = "Diamond"

  override val maxPower: Double = 4.0 // blocks radius

  override val description: String
    get() = "Destructive mining spell that breaks blocks in an area upon landing."

  override fun onSpellLand(state: SpellState, spellEntity: Entity, rating: GemRating) {
    val breakRadius = (maxPower * state.gemPower * rating.powerPercent).coerceAtLeast(1.0)
    val world = spellEntity.world
    val pos = spellEntity.positionComponent

    val breakableBlocks = Tool.Pickaxe.breakableLocs(spellEntity, world, pos.blockX, pos.blockY, breakRadius.toFloat(), state.spellRange.toFloat()).asIterable()
    world.removeBlocks(breakableBlocks, state.caster)
  }
}

data object SunGem : GemType {

  override val displayName: String = "Sun Gem"

  override val maxPower: Double = 300.0 // Duration in seconds before extinguishing the light

  override val description: String
    get() = "Impossible to break light where the spell lands"

  override fun onSpellLand(state: SpellState, spellEntity: Entity, rating: GemRating) {
    val lightDuration = (maxPower * state.gemPower * rating.powerPercent).coerceAtLeast(1.0)
    val world = spellEntity.world
    val pos = spellEntity.positionComponent

    val block = world.setBlock(pos.blockX, pos.blockY, Material.PhosphorusSpell)
    val entity = block?.entity ?: return
    val decayComp = entity.decayComponentOrNull
    if (decayComp == null) {
      // should not really happen, but just in case
      entity.addAndReturn(DecayingComponent(lightDuration))
    } else {
      decayComp.timeLeftSeconds = decayComp.timeLeftSeconds.coerceAtMost(lightDuration)
    }
  }
}
