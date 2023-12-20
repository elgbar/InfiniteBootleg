package no.elg.infiniteBootleg.world.magic

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.world.Staff
import kotlin.time.Duration

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
}

data class MutableSpellState(
  /**
   * The entity that holds this staff (and is casting the spell)
   */
  override val holder: Entity,
  /**
   * The staff used to cast this spell
   */
  override val staff: Staff,
  /**
   * How far the spell can reach
   */
  override var spellRange: Double,
  /**
   * How fast the spell travels
   */
  override var spellVelocity: Double,
  /**
   * How long until the next spell can be cast
   */
  override var castDelay: Duration,
  /**
   * How powerful the spell is (the meaning of power is up to the gem).
   *
   * This is normally in the range of [0, 1]
   */
  override var gemPower: Double,
  /**
   * Modifications to be done to the spell entity after it has been cast
   */
  override val entityModifications: MutableList<(Entity) -> Unit>
) : SpellState
