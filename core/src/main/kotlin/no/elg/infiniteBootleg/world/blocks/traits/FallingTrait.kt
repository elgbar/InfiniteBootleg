package no.elg.infiniteBootleg.world.blocks.traits

/**
 * @author Elg
 */
@Deprecated("Use ashley entity components instead")
interface FallingTrait : TickingTrait {

  val falling: Boolean

  fun tryFall()

  override fun tick() {
    tryFall()
  }
}
