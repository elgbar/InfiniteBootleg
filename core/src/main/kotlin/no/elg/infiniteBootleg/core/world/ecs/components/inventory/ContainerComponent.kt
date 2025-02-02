package no.elg.infiniteBootleg.core.world.ecs.components.inventory

import com.badlogic.ashley.core.Entity
import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import no.elg.infiniteBootleg.core.inventory.container.Container
import no.elg.infiniteBootleg.core.inventory.container.ContainerOwner
import no.elg.infiniteBootleg.core.inventory.container.OwnedContainer
import no.elg.infiniteBootleg.core.inventory.container.OwnedContainer.Companion.asProto
import no.elg.infiniteBootleg.core.inventory.container.OwnedContainer.Companion.fromProto
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.AuthoritativeOnlyComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.compactBlockLoc
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld

private val logger = KotlinLogging.logger {}

data class ContainerComponent(val ownedContainer: OwnedContainer) : EntitySavableComponent, AuthoritativeOnlyComponent {

  override fun EntityKt.Dsl.save() {
    ownedContainer = this@ContainerComponent.ownedContainer.asProto()
  }

  override fun hudDebug(): String = "interface: ${ownedContainer.owner.toInterfaceId()}, container: ${ownedContainer.container}"

  companion object : EntityLoadableMapper<ContainerComponent>() {
    var Entity.containerComponentOrNull by optionalPropertyFor(mapper)

    val Entity.containerOrNull: Container? get() = containerComponentOrNull?.ownedContainer?.container
    val Entity.ownedContainerOrNull: OwnedContainer? get() = containerComponentOrNull?.ownedContainer

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) =
      safeWith {
        val ownedContainer = protoEntity.ownedContainer.fromProto()
        val owner = ownedContainer.owner
        val isValid = when (owner) {
          is ContainerOwner.EntityOwner -> entity.id == owner.entityId
          is ContainerOwner.BlockOwner -> entity.compactBlockLoc == owner.loc
        }
        if (!isValid) {
          val newOwner = when (owner) {
            is ContainerOwner.EntityOwner -> ContainerOwner.EntityOwner(entity.id)
            is ContainerOwner.BlockOwner -> ContainerOwner.BlockOwner(entity.compactBlockLoc)
          }
          logger.warn { "Invalid owner of container! Got $owner, but expected $newOwner" }
          ContainerComponent(OwnedContainer(newOwner, ownedContainer.container))
        } else {
          ContainerComponent(ownedContainer)
        }
      }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasOwnedContainer()
  }
}
