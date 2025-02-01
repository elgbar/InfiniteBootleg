package no.elg.infiniteBootleg.core.inventory.container

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.util.WorldCompactLoc
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.compactLoc
import no.elg.infiniteBootleg.core.util.toProtoEntityRef
import no.elg.infiniteBootleg.core.util.worldToChunk
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.core.world.ecs.api.OptionalProtoConverter
import no.elg.infiniteBootleg.core.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.protobuf.containerOwner
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
    fun from(block: Block): ContainerOwner = BlockOwner(compactLoc(block.worldX, block.worldY))

    fun toInterfaceId(entity: Entity): InterfaceId = from(entity).toInterfaceId()
    fun toInterfaceId(worldX: WorldCoord, worldY: WorldCoord): InterfaceId = from(worldX, worldY).toInterfaceId()
    fun toInterfaceId(block: Block): InterfaceId = from(block).toInterfaceId()

    /**
     * Check if the owner is valid in the current world
     */
    fun ContainerOwner.isValid(): Boolean = Main.Companion.inst().world?.let { isValid(it) } ?: false

    override fun ProtoContainerOwner.fromProto(): ContainerOwner? =
      when (this.ownerCase) {
        ProtoContainerOwner.OwnerCase.ENTITYOWNER -> EntityOwner(this.entityOwner.id)
        ProtoContainerOwner.OwnerCase.WORLDOWNER -> BlockOwner(this.worldOwner)
        else -> null
      }

    override fun ContainerOwner.asProto(): ProtoContainerOwner =
      containerOwner {
        when (this@asProto) {
          is EntityOwner -> entityOwner = entityId.toProtoEntityRef()
          is BlockOwner -> worldOwner = loc
        }
      }
  }
}
