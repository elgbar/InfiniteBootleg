package no.elg.infiniteBootleg.world.ecs.components.inventory

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import no.elg.infiniteBootleg.inventory.container.Container
import no.elg.infiniteBootleg.inventory.container.Container.Companion.close
import no.elg.infiniteBootleg.inventory.container.Container.Companion.isOpen
import no.elg.infiniteBootleg.inventory.container.Container.Companion.open
import no.elg.infiniteBootleg.inventory.container.Container.Companion.toggle
import no.elg.infiniteBootleg.inventory.container.OwnedContainer
import no.elg.infiniteBootleg.inventory.container.OwnedContainer.Companion.asProto
import no.elg.infiniteBootleg.inventory.container.OwnedContainer.Companion.fromProto
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.safeWith
import no.elg.infiniteBootleg.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.ecs.api.restriction.AuthoritativeOnlyComponent

data class ContainerComponent(val ownedContainer: OwnedContainer) : EntitySavableComponent, AuthoritativeOnlyComponent {

  override fun EntityKt.Dsl.save() {
    ownedContainer = this@ContainerComponent.ownedContainer.asProto()
  }

  companion object : EntityLoadableMapper<ContainerComponent>() {
    var Entity.containerComponentOrNull by optionalPropertyFor(mapper)

    val Entity.containerOrNull: Container? get() = containerComponentOrNull?.ownedContainer?.container
    val Entity.ownedContainerOrNull: OwnedContainer? get() = containerComponentOrNull?.ownedContainer

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

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) = safeWith { ContainerComponent(protoEntity.ownedContainer.fromProto()) }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasOwnedContainer()
  }
}
