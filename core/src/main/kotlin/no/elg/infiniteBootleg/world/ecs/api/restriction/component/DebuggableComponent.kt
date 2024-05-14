package no.elg.infiniteBootleg.world.ecs.api.restriction.component

import com.badlogic.ashley.core.Component
import no.elg.infiniteBootleg.api.HUDDebuggable

interface DebuggableComponent : Component, HUDDebuggable {

  companion object {
    fun Component.debugString(): String =
      when (this) {
        is HUDDebuggable -> hudDebug()
        else -> toString()
      }
  }
}