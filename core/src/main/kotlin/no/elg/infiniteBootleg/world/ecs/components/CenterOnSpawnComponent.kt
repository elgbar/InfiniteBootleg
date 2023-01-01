package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Component
import ktx.ashley.Mapper

/**
 * Center the entity when spawned
 */
class CenterOnSpawnComponent : Component {
  companion object : Mapper<CenterOnSpawnComponent>()
}
