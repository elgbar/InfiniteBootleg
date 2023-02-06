package no.elg.infiniteBootleg.world.ecs.components.events

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import ktx.ashley.configureEntity
import no.elg.infiniteBootleg.world.ecs.controlledEntityFamily
import no.elg.infiniteBootleg.world.ecs.with

interface ECSEvent : Component {

  companion object {
    private val defaultFilter: (Entity) -> Boolean = { true }
    fun Engine.handleEvent(event: ECSEvent, filter: (Entity) -> Boolean = defaultFilter) {
      this.getEntitiesFor(controlledEntityFamily).filter(filter).forEach {
        this.configureEntity(it) {
          with(event)
        }
      }
    }
  }
}
