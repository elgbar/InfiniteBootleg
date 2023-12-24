package no.elg.infiniteBootleg.world.magic

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.world.Staff
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

interface SpellState {
  /**
   * The entity that holds this staff (and is casting the spell)
   */
  val holder: Entity

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
    fun SpellState?.canCastAgain(): Boolean = this == null || castMark.hasPassedNow()
  }
}

data class MutableSpellState(
  override val holder: Entity,
  override val staff: Staff,
  override var spellRange: Double,
  override var spellVelocity: Double,
  override var castDelay: Duration,
  override var gemPower: Double,
  override val entityModifications: MutableList<(Entity) -> Unit>,
  override var castMark: TimeMark = TimeSource.Monotonic.markNow() + Duration.INFINITE
) : SpellState
