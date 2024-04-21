package no.elg.infiniteBootleg.inventory.container

import no.elg.infiniteBootleg.inventory.container.Container.Companion.asProto
import no.elg.infiniteBootleg.inventory.container.Container.Companion.fromProto
import no.elg.infiniteBootleg.inventory.container.ContainerOwner.Companion.asProto
import no.elg.infiniteBootleg.inventory.container.ContainerOwner.Companion.fromProto
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.ownedContainer
import no.elg.infiniteBootleg.world.ecs.api.ProtoConverter

data class OwnedContainer(val owner: ContainerOwner, val container: Container) {

  companion object : ProtoConverter<OwnedContainer, ProtoWorld.OwnedContainer> {
    override fun ProtoWorld.OwnedContainer.fromProto(): OwnedContainer =
      OwnedContainer(
        owner = owner.fromProto() ?: throw IllegalArgumentException("Owner is null"),
        container = container.fromProto()
      )

    override fun OwnedContainer.asProto(): ProtoWorld.OwnedContainer =
      ownedContainer {
        owner = this@asProto.owner.asProto()
        container = this@asProto.container.asProto()
      }
  }
}
