package no.elg.infiniteBootleg.world.ecs.components.inventory

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.scenes.scene2d.Actor
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import no.elg.infiniteBootleg.inventory.container.Container
import no.elg.infiniteBootleg.inventory.container.Container.Companion.asProto
import no.elg.infiniteBootleg.inventory.container.Container.Companion.close
import no.elg.infiniteBootleg.inventory.container.Container.Companion.fromProto
import no.elg.infiniteBootleg.inventory.container.Container.Companion.isOpen
import no.elg.infiniteBootleg.inventory.container.Container.Companion.open
import no.elg.infiniteBootleg.inventory.container.Container.Companion.toggle
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.safeWith
import no.elg.infiniteBootleg.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.ecs.api.restriction.AuthoritativeOnlyComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.clientWorld
import java.util.concurrent.CompletableFuture

class ContainerComponent(val container: Container) : EntitySavableComponent, AuthoritativeOnlyComponent {

  override fun EntityKt.Dsl.save() {
    container = this@ContainerComponent.container.asProto()
  }

  companion object : EntityLoadableMapper<ContainerComponent>() {
    var Entity.containerComponentOrNull by optionalPropertyFor(mapper)

    val Entity.containerOrNull: Container? get() = containerComponentOrNull?.container

    private fun Entity.getContainerActor(container: Container?): CompletableFuture<Actor>? {
      return this.clientWorld?.render?.getContainerActor(container ?: return null)
    }

    fun Entity.isInventoryOpen(): Boolean = containerOrNull?.isOpen(this) ?: false

    fun Entity.openInventory() {
      containerOrNull?.open(this)
    }

    fun Entity.closeInventory() {
      containerOrNull?.close(this)
    }

    fun Entity.toggleInventory() {
      containerOrNull?.toggle(this)
    }

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) = safeWith { ContainerComponent(protoEntity.container.fromProto()) }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasContainer()
  }
}
