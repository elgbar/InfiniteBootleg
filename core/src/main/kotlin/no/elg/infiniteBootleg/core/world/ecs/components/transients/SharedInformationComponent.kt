package no.elg.infiniteBootleg.core.world.ecs.components.transients

import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.core.net.SharedInformation
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.AuthoritativeOnlyComponent

data class SharedInformationComponent(val sharedInformation: SharedInformation) : AuthoritativeOnlyComponent {

  override fun hudDebug(): String = "entityUUID: ${sharedInformation.entityId}"

  companion object : Mapper<SharedInformationComponent>() {
    var Entity.sharedInformation by propertyFor(mapper)
    var Entity.sharedInformationOrNull by optionalPropertyFor(mapper)
  }
}
