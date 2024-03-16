package no.elg.infiniteBootleg.world.ecs.components.transients

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.LongMap
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.BreakingBlockKt.breakingProgress
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.server.ServerClient.Companion.sendServerBoundPacket
import no.elg.infiniteBootleg.server.serverBoundBreakingBlock
import no.elg.infiniteBootleg.util.ProgressHandler
import no.elg.infiniteBootleg.util.toVector2i
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.blocks.Block.Companion.compactWorldLoc

class CurrentlyBreakingComponent : Component {

  val breaking: LongMap<CurrentlyBreaking> = LongMap(16, 0.8f)

  fun reset() {
    sendCurrentProgress(true)
    breaking.clear()
  }

  fun sendCurrentProgress(zeroProgress: Boolean = false) {
    if (Main.isServerClient && breaking.size > 0) {
      ClientMain.inst().serverClient.sendServerBoundPacket {
        val progresses = breaking.values().map { it.toBreakingProgress(zeroProgress) }
        serverBoundBreakingBlock(progresses)
      }
    }
  }

  companion object : Mapper<CurrentlyBreakingComponent>() {
    var Entity.currentlyBreakingComponent by propertyFor(CurrentlyBreakingComponent.mapper)
    var Entity.currentlyBreakingComponentOrNull by optionalPropertyFor(CurrentlyBreakingComponent.mapper)
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
