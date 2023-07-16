package no.elg.infiniteBootleg.world.blocks

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.api.HUDDebuggable
import no.elg.infiniteBootleg.api.Savable
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.block
import no.elg.infiniteBootleg.protobuf.entityOrNull
import no.elg.infiniteBootleg.protobuf.material
import no.elg.infiniteBootleg.util.CheckableDisposable
import no.elg.infiniteBootleg.util.compactLoc
import no.elg.infiniteBootleg.util.isInsideChunk
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.Material.Companion.fromOrdinal
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.render.RotatableTextureRegion
import no.elg.infiniteBootleg.world.world.World

interface Block : CheckableDisposable, HUDDebuggable, Savable<ProtoWorld.Block.Builder> {

  val texture: RotatableTextureRegion?
  val material: Material
  val chunk: Chunk

  /**
   * @return World this block exists in
   */
  val world: World

  /**
   * @return The offset/local position of this block within its chunk
   */
  val localX: Int

  /**
   * @return The offset/local position of this block within its chunk
   */
  val localY: Int

  /**
   * Connected blocks to ashley engine
   */
  val entity: Entity?

  override fun hudDebug(): String {
    return "Block $material"
  }

  companion object {

    val Block.compactWorldLoc: Long get() = compactLoc(worldX, worldY)
    val Block.worldX: Int get() = chunk.getWorldX(localX)
    val Block.worldY: Int get() = chunk.getWorldY(localY)

    /**
     * Remove this block by setting it to air
     */
    fun Block.remove(updateTexture: Boolean = true, prioritize: Boolean = false, sendUpdatePacket: Boolean = true) {
      if (chunk.getRawBlock(localX, localY) === this) {
        chunk.removeBlock(localX, localY, updateTexture = updateTexture, prioritize = prioritize, sendUpdatePacket = sendUpdatePacket)
      }
    }

    /**
     * Remove this block by setting it to air, done asynchronous
     */
    fun Block.removeAsync(updateTexture: Boolean = true, prioritize: Boolean = false, sendUpdatePacket: Boolean = true, postRemove: () -> Unit = {}) {
      Main.inst().scheduler.executeAsync {
        remove(updateTexture, prioritize, sendUpdatePacket)
        postRemove()
      }
    }

    fun Block.getRawRelative(dir: Direction, load: Boolean = true): Block? {
      val newLocalX = localX + dir.dx
      val newLocalY = localY + dir.dy
      return if (isInsideChunk(newLocalX, newLocalY)) {
        chunk.getRawBlock(newLocalX, newLocalY)
      } else {
        val newWorldX: Int = worldX + dir.dx
        val newWorldY: Int = worldY + dir.dy
        world.getRawBlock(newWorldX, newWorldY, load)
      }
    }

    fun Block?.materialOrAir(): Material = this?.material ?: Material.AIR

    fun fromProto(world: World, chunk: Chunk, localX: Int, localY: Int, protoBlock: ProtoWorld.Block?): Block? {
      if (protoBlock == null) {
        return null
      }
      val mat = fromOrdinal(protoBlock.material.ordinal)
      if (mat === Material.AIR || mat.isEntity) {
        return null
      }
      return mat.createBlock(world, chunk, localX, localY, protoBlock.entityOrNull)
    }

    fun save(material: Material): ProtoWorld.Block.Builder = block {
      this.material = material {
        ordinal = material.ordinal
      }
    }.toBuilder()

    const val BLOCK_SIZE = 16
  }
}
