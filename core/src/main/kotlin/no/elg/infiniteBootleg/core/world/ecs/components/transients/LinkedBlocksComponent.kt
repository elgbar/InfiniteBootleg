package no.elg.infiniteBootleg.core.world.ecs.components.transients

import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.DebuggableComponent

class LinkedBlocksComponent(var mutableLinks: MutableSet<Block>) : DebuggableComponent {

  override fun hudDebug(): String = "Linked to the blocks: ${mutableLinks.map { it.hudDebug() }}"

  companion object : Mapper<LinkedBlocksComponent>() {
    var Entity.linkedBlocksComponentComponentOrNull: LinkedBlocksComponent? by optionalPropertyFor(mapper)

    fun Entity.linkTo(block: Block) {
      require(block.entity === this) { "Wrong linked entity ${block.entity}" }
      val linkedBlocksComponent = linkedBlocksComponentComponentOrNull ?: run {
        this.safeWith { LinkedBlocksComponent(mutableSetOf(block)) }
        return
      }
      linkedBlocksComponent.mutableLinks += block
    }

    fun Entity.unlinkFrom(block: Block) {
      require(block.entity === this) { "Wrong linked entity ${block.entity}" }
      val links = linkedBlocksComponentComponentOrNull?.mutableLinks ?: return
      links -= block
    }
  }
}
