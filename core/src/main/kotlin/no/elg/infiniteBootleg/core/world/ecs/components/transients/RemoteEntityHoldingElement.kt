package no.elg.infiniteBootleg.core.world.ecs.components.transients

import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import no.elg.infiniteBootleg.core.world.ContainerElement
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.DebuggableComponent

/**
 * A remote entity is holding an element, hack to not have to setup that the entity has a container component
 */
class RemoteEntityHoldingElement(var element: ContainerElement) : DebuggableComponent {

  override fun hudDebug(): String = "Holding: ${element.displayName}"

  companion object : Mapper<RemoteEntityHoldingElement>() {
    var Entity.remoteEntityHoldingElementComponentOrNull: RemoteEntityHoldingElement? by optionalPropertyFor(mapper)
    val Entity.remoteEntityHoldingElementOrNull: ContainerElement? get() = remoteEntityHoldingElementComponentOrNull?.element
  }
}
