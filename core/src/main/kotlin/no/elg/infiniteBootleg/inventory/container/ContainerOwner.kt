package no.elg.infiniteBootleg.inventory.container

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.containerOwner
import no.elg.infiniteBootleg.util.WorldCompactLoc
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.compactLoc
import no.elg.infiniteBootleg.util.worldToChunk
import no.elg.infiniteBootleg.world.ecs.api.OptionalProtoConverter
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.world.World
import no.elg.infiniteBootleg.protobuf.ProtoWorld.ContainerOwner as ProtoContainerOwner

typealias InterfaceId = String

sealed interface ContainerOwner {

  fun toInterfaceId(): InterfaceId

  /**
   * Check if the owner is valid in the given world
   */
  fun isValid(world: World): Boolean

  data class EntityOwner(val entityId: String) : ContainerOwner {
    override fun toInterfaceId() = "{entity=$entityId}"

    /**
     * Check if the owner is valid in the given world
     *
     * An entity owner is valid if a standalone entity exists with the given id
     */
    override fun isValid(world: World): Boolean = world.getEntity(entityId) != null
  }

  data class BlockOwner(val loc: WorldCompactLoc) : ContainerOwner {
    override fun toInterfaceId() = "{loc=$loc}"

    /**
     * Check if the owner is valid in the given world
     *
     * A block owner is valid if the chunk it is in is loaded
     */
    override fun isValid(world: World): Boolean = world.isChunkLoaded(loc.worldToChunk())
  }

  companion object : OptionalProtoConverter<ContainerOwner, ProtoContainerOwner> {
    fun from(entity: Entity): ContainerOwner = EntityOwner(entity.id)
    fun from(worldX: WorldCoord, worldY: WorldCoord): ContainerOwner = BlockOwner(compactLoc(worldX, worldY))

    /**
     * Check if the owner is valid in the current world
     */
    fun ContainerOwner.isValid(): Boolean = Main.inst().world?.let { isValid(it) } ?: false

    override fun ProtoContainerOwner.fromProto(): ContainerOwner? =
      when (this.ownerCase) {
        ProtoContainerOwner.OwnerCase.ENTITYOWNER -> EntityOwner(this.entityOwner)
        ProtoContainerOwner.OwnerCase.WORLDOWNER -> BlockOwner(this.worldOwner)
        else -> null
      }

    override fun ContainerOwner.asProto(): ProtoContainerOwner =
      containerOwner {
        when (this@asProto) {
          is EntityOwner -> entityOwner = entityId
          is BlockOwner -> worldOwner = loc
        }
      }
  }
}