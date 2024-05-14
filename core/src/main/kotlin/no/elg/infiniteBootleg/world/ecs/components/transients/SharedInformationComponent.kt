package no.elg.infiniteBootleg.world.ecs.components.transients

import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.server.SharedInformation
import no.elg.infiniteBootleg.world.ecs.api.restriction.component.AuthoritativeOnlyComponent

data class SharedInformationComponent(val sharedInformation: SharedInformation) : AuthoritativeOnlyComponent {

  override fun hudDebug(): String = "entityUUID: ${sharedInformation.entityUUID}"

  companion object : Mapper<SharedInformationComponent>() {
    var Entity.sharedInformation by propertyFor(SharedInformationComponent.mapper)
    var Entity.sharedInformationOrNull by optionalPropertyFor(SharedInformationComponent.mapper)
  }
}
