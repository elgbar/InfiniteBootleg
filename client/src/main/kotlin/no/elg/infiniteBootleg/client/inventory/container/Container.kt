package no.elg.infiniteBootleg.client.inventory.container

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.core.inventory.container.OwnedContainer
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.ContainerComponent.Companion.ownedContainerOrNull

fun OwnedContainer.isOpen(): Boolean = ClientMain.inst().world?.render?.interfaceManager?.isOpen(owner.toInterfaceId()) ?: false

fun OwnedContainer.open() {
  ClientMain.inst().world?.render?.let { render -> render.openInterface(owner.toInterfaceId()) { render.createContainerActor(this) } }
}

fun OwnedContainer.close() {
  ClientMain.inst().world?.render?.closeInterface(owner.toInterfaceId())
}

fun OwnedContainer.toggle() {
  ClientMain.inst().world?.render?.let { render -> render.toggleInterface(owner.toInterfaceId()) { render.createContainerActor(this) } }
}

fun Entity.isInventoryOpen(): Boolean = ownedContainerOrNull?.isOpen() ?: false

fun Entity.openContainer() {
  ownedContainerOrNull?.open()
}

fun Entity.closeContainer() {
  ownedContainerOrNull?.close()
}

fun Entity.toggleContainer() {
  ownedContainerOrNull?.toggle()
}
