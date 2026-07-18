package no.elg.infiniteBootleg.core.events

import no.elg.infiniteBootleg.core.events.api.ReasonedEvent
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.HotbarComponent.Companion.HotbarSlot

data class HotbarItemScrolled(val oldSlot: HotbarSlot, val newSlow: HotbarSlot) : ReasonedEvent {

  override val reason: String
    get() = "Player changed the hotbar item from $oldSlot to $newSlow"
}
