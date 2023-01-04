package no.elg.infiniteBootleg.world.ecs.components.required

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.propertyFor
import java.util.UUID

data class IdComponent(val id: String = UUID.randomUUID().toString()) : Component {
  companion object : Mapper<IdComponent>() {

    fun createRandomId(): IdComponent = IdComponent(UUID.randomUUID().toString())

    val Entity.id by propertyFor(mapper)
  }
}
