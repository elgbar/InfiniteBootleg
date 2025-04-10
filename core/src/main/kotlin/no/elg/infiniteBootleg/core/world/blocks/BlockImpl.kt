package no.elg.infiniteBootleg.core.world.blocks

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.core.util.LocalCoord
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.Material.Companion.asProto
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.ecs.save
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.block

/**
 * A block in the world each block is a part of a chunk which is a part of a world. Each block know
 * its world location and its location within the parent chunk.
 *
 * @author Elg
 */
class BlockImpl(override val chunk: Chunk, override val localX: LocalCoord, override val localY: LocalCoord, override val material: Material, override var entity: Entity? = null) :
  Block {

  override fun save(): ProtoWorld.Block =
    block {
      this.material = this@BlockImpl.material.asProto()
      this@BlockImpl.entity?.save(toAuthoritative = true)?.also { entity = it }
    }

  override fun dispose() {
    entity?.also {
      world.removeEntity(it, Packets.DespawnEntity.DespawnReason.NATURAL)
      entity = null
    }
  }

  override fun hashCode(): Int {
    var result = material.hashCode()
    result = 31 * result + chunk.hashCode()
    result = 31 * result + localX
    result = 31 * result + localY
    return result
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val block = other as BlockImpl
    if (localX != block.localX) {
      return false
    }
    if (localY != block.localY) {
      return false
    }
    return if (material !== block.material) {
      false
    } else {
      chunk == block.chunk
    }
  }

  override fun toString(): String = "Block{material=$material, chunk=$chunk, worldX=$worldX, worldY=$worldY}"
}
