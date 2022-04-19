package no.elg.infiniteBootleg.world.blocks.traits

/**
 * @author Elg
 */
interface FallingTrait : TickingTrait {

  val falling: Boolean

  fun tryFall()

  override fun tick() {
    tryFall()
  }
}
