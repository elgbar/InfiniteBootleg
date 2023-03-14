package no.elg.infiniteBootleg.world

import com.badlogic.gdx.graphics.g2d.TextureRegion
import no.elg.infiniteBootleg.CheckableDisposable
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.api.HUDDebuggable
import no.elg.infiniteBootleg.api.Savable
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.compactLoc
import no.elg.infiniteBootleg.util.isInsideChunk
import no.elg.infiniteBootleg.world.Material.Companion.fromOrdinal
import no.elg.infiniteBootleg.world.world.World

interface Block : CheckableDisposable, HUDDebuggable, Savable<ProtoWorld.Block.Builder> {

  val texture: TextureRegion?
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

  fun load(protoBlock: ProtoWorld.Block)

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
    fun Block.remove(updateTexture: Boolean = true, prioritize: Boolean = false, sendUpdatePacket: Boolean = true) =
      chunk.removeBlock(localX, localY, updateTexture = updateTexture, prioritize = prioritize, sendUpdatePacket = sendUpdatePacket)

    /**
     * Remove this block by setting it to air, done asynchronous
     */
    fun Block.removeAsync(updateTexture: Boolean = true, prioritize: Boolean = false, sendUpdatePacket: Boolean = true) {
      Main.inst().scheduler.executeAsync { remove(updateTexture, prioritize, sendUpdatePacket) }
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

    fun fromProto(world: World, chunk: Chunk, localX: Int, localY: Int, protoBlock: ProtoWorld.Block?): Block? {
      if (protoBlock == null) {
        return null
      }
      val mat = fromOrdinal(protoBlock.materialOrdinal)
      if (mat === Material.AIR || mat.isEntity) {
        return null
      }
      val block = mat.createBlock(world, chunk, localX, localY)
      block.load(protoBlock)
      return block
    }

    fun save(material: Material): ProtoWorld.Block.Builder {
      return ProtoWorld.Block.newBuilder().setMaterialOrdinal(material.ordinal)
    }

    const val BLOCK_SIZE = 16
  }
}
