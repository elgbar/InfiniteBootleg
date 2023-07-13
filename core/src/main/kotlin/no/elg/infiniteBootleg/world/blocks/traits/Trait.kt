package no.elg.infiniteBootleg.world.blocks.traits

import no.elg.infiniteBootleg.world.Block

@Deprecated("Use ashley entity components instead")
interface Trait : Block {

  val handlers: TraitHandlerCollection
}
