package no.elg.infiniteBootleg.server.world.ecs.components.transients

import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.DebuggableComponent
import kotlin.time.DurationUnit
import kotlin.time.TimeSource
import kotlin.time.TimeSource.Monotonic.ValueTimeMark

/**
 * Last time the position of an entity was updated
 *
 * Required for server player entities
 */
data class LastPositionComponent(var lastUpdated: ValueTimeMark = TimeSource.Monotonic.markNow()) : DebuggableComponent {

  fun update() {
    lastUpdated = TimeSource.Monotonic.markNow()
  }

  fun getAndUpdateElapsedSeconds(): Double {
    val elapsedNow = lastUpdated.elapsedNow()
    update()
    return elapsedNow.toDouble(DurationUnit.SECONDS).coerceIn(0.0, MAX_ALLOWED_ELAPSED_SECONDS)
  }

  override fun hudDebug(): String = "Last pos update ${lastUpdated.elapsedNow().toString(DurationUnit.SECONDS, 4)} ago"

  companion object : Mapper<LastPositionComponent>() {
    private const val MAX_ALLOWED_ELAPSED_SECONDS = 1.0
    var Entity.lastPositionUpdatedComponent: LastPositionComponent by propertyFor(mapper)
  }
}
