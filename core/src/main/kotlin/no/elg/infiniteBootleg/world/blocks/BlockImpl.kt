package no.elg.infiniteBootleg.world.blocks

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.block
import no.elg.infiniteBootleg.util.LocalCoord
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.Material.Companion.asProto
import no.elg.infiniteBootleg.world.blocks.Block.Companion.getRawRelative
import no.elg.infiniteBootleg.world.blocks.Block.Companion.materialOrAir
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.ecs.save
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion
import no.elg.infiniteBootleg.world.render.texture.TextureNeighbor
import no.elg.infiniteBootleg.world.world.World
import java.util.EnumMap

/**
 * A block in the world each block is a part of a chunk which is a part of a world. Each block know
 * its world location and its location within the parent chunk.
 *
 * @author Elg
 */
class BlockImpl(
  override val world: World,
  override val chunk: Chunk,
  override val localX: LocalCoord,
  override val localY: LocalCoord,
  override val material: Material,
  override var entity: Entity? = null
) : Block {

  override var isDisposed: Boolean = false
    private set

  override fun save(): ProtoWorld.Block =
    block {
      this.material = this@BlockImpl.material.asProto()
      this@BlockImpl.entity?.save(toAuthoritative = true)?.also { entity = it }
    }

  override val texture: RotatableTextureRegion?
    get() {
      val map = EnumMap<Direction, Material>(Direction::class.java)
      for (direction in Direction.CARDINAL) {
        val relMat = this.getRawRelative(direction, false).materialOrAir()
        map[direction] = relMat
      }
      return TextureNeighbor.getTexture(material, map)
    }

  override fun dispose() {
    if (isDisposed) {
      Main.logger().warn("Disposed block ${this::class.simpleName} ($worldX, $worldY) twice")
    }
    isDisposed = true
    entity?.also {
      world.removeEntity(it, Packets.DespawnEntity.DespawnReason.NATURAL)
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
