package no.elg.infiniteBootleg.world.blocks

import com.badlogic.ashley.core.Entity
import com.google.common.base.Preconditions
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.blocks.Block.Companion.getRawRelative
import no.elg.infiniteBootleg.world.blocks.Block.Companion.materialOrAir
import no.elg.infiniteBootleg.world.blocks.Block.Companion.save
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.render.RotatableTextureRegion
import no.elg.infiniteBootleg.world.render.TextureNeighbor
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
  override val localX: Int,
  override val localY: Int,
  override val material: Material,
  override val entity: Entity? = null
) : Block {

  override var isDisposed: Boolean = false
    private set

  override fun save(): ProtoWorld.Block.Builder {
    return save(material)
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

  override fun load(protoBlock: ProtoWorld.Block) {
    Preconditions.checkArgument(protoBlock.material.ordinal == material.ordinal)
  }

  override fun dispose() {
    if (isDisposed) {
      Main.logger().warn("Disposed block ${this::class.simpleName} ($worldX, $worldY) twice")
    }
    isDisposed = true
    entity?.also {
      if (!it.isScheduledForRemoval) {
        world.engine.removeEntity(it)
      }
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
