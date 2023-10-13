package no.elg.infiniteBootleg.world.ecs.components.transients

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.LongMap
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.util.ProgressHandler
import no.elg.infiniteBootleg.world.blocks.Block

// FIXME should not be transient, it is useful in a multiplayer setting
class CurrentlyBreakingComponent : Component {

  val breaking: LongMap<CurrentlyBreaking> = LongMap(16, 0.8f)

  fun reset() {
    breaking.clear()
  }

  companion object : Mapper<CurrentlyBreakingComponent>() {
    var Entity.currentlyBreakingComponent by propertyFor(CurrentlyBreakingComponent.mapper)
    var Entity.currentlyBreakingComponentOrNull by optionalPropertyFor(CurrentlyBreakingComponent.mapper)
  }

  data class CurrentlyBreaking(
    val block: Block,
    val progressHandler: ProgressHandler = ProgressHandler(block.material.hardness, Interpolation.linear, 0f, 1f)
  )
}
