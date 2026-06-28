package no.elg.infiniteBootleg.core.world.ecs.components.transients

import com.badlogic.ashley.core.Entity
import ktx.ashley.Mapper
import ktx.ashley.optionalPropertyFor
import no.elg.infiniteBootleg.core.items.Item
import no.elg.infiniteBootleg.core.items.Item.Companion.displayName
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.DebuggableComponent

/**
 * A remote entity is holding an element, hack to not have to setup that the entity has a container component
 */
class RemoteEntityHoldingItem(var item: Item) : DebuggableComponent {

  override fun hudDebug(): String = "Holding: ${item.displayName}"

  companion object : Mapper<RemoteEntityHoldingItem>() {
    var Entity.remoteEntityHoldingItemComponentOrNull: RemoteEntityHoldingItem? by optionalPropertyFor(mapper)
    val Entity.remoteEntityHoldingItemOrNull: Item? get() = remoteEntityHoldingItemComponentOrNull?.item
  }
}
