package no.elg.infiniteBootleg.core.world.magic

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.core.world.Staff
import kotlin.contracts.contract
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

interface SpellState {
  /**
   * The entity cast the spell, and was holding the [staff]
   */
  val caster: Entity

  /**
   * The staff used to cast this spell
   */
  val staff: Staff

  /**
   * How far the spell can reach
   */
  val spellRange: Double

  val spellVelocity: Double

  /**
   * How long until the next spell can be cast
   */
  val castDelay: Duration

  /**
   * How powerful the spell is (the meaning of power is up to the gem).
   *
   * This is normally in the range of [0, 1]
   */
  val gemPower: Double

  /**
   * Modifications to be done to the spell entity after it has been cast
   */
  val entityModifications: List<Entity.() -> Unit>

  /**
   * When the spell was cast
   */
  val castMark: TimeMark

  companion object {
    fun SpellState?.canCastAgain(): Boolean {
      contract { returns(false) implies (this@canCastAgain != null) }
      return this == null || castMark.hasPassedNow()
    }

    fun SpellState?.canNotCastAgain(): Boolean {
      contract { returns(true) implies (this@canNotCastAgain != null) }
      return this != null && !castMark.hasPassedNow()
    }

    /**
     * @return How long until the spell can be cast again
     */
    fun SpellState.timeToCast(): Duration = castMark.elapsedNow().times(-1)
  }
}

data class MutableSpellState(
  override val caster: Entity,
  override val staff: Staff,
  override var spellRange: Double,
  override var spellVelocity: Double,
  override var castDelay: Duration,
  override var gemPower: Double,
  override val entityModifications: MutableList<(Entity) -> Unit>,
  override var castMark: TimeMark = TimeSource.Monotonic.markNow() + Duration.INFINITE
) : SpellState
