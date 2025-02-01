package no.elg.infiniteBootleg.core.world.ecs.components.transients

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.LongMap
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.core.util.ProgressHandler
import no.elg.infiniteBootleg.core.util.toVector2i
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.compactWorldLoc
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.DebuggableComponent
import no.elg.infiniteBootleg.protobuf.BreakingBlockKt.breakingProgress
import no.elg.infiniteBootleg.protobuf.Packets

class CurrentlyBreakingComponent : DebuggableComponent {

  val breaking: LongMap<CurrentlyBreaking> = LongMap(16, 0.8f)

  override fun hudDebug(): String = "Currently breaking ${breaking.values().map { "${it.block.hudDebug()} (${it.progressHandler.progress * 100f}%)" }}"

  companion object : Mapper<CurrentlyBreakingComponent>() {
    var Entity.currentlyBreakingComponent by propertyFor(mapper)
    var Entity.currentlyBreakingComponentOrNull by optionalPropertyFor(mapper)
  }

  data class CurrentlyBreaking(
    val block: Block,
    val progressHandler: ProgressHandler = ProgressHandler(block.material.hardness, Interpolation.linear, 0f, 1f)
  ) {
    fun toBreakingProgress(zeroProgress: Boolean): Packets.BreakingBlock.BreakingProgress =
      breakingProgress {
        blockLocation = block.compactWorldLoc.toVector2i()
        progress = if (zeroProgress) 0f else progressHandler.progress
      }
  }
}
