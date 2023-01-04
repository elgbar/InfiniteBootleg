package no.elg.infiniteBootleg.world.ecs.components.tags

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.tagFor

/**
 * Center the entity when spawned
 */
object CenterOnSpawnTag : Component, Mapper<CenterOnSpawnTag>() {
  var Entity.centerOnSpawn by tagFor(CenterOnSpawnTag)
}
