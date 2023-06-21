package no.elg.infiniteBootleg.world.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.google.common.base.Preconditions
import ktx.collections.gdxSetOf
import no.elg.infiniteBootleg.KAssets.whiteTexture
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Block.TNT
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.Block.Companion.worldX
import no.elg.infiniteBootleg.world.Block.Companion.worldY
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.Location
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.world.World
import kotlin.math.abs

/**
 * A block that explodes after [.fuseDurationTicks] ticks
 *
 * @author Elg
 */
class TntBlock(world: World, chunk: Chunk, localX: Int, localY: Int, material: Material) : TickingBlock(world, chunk, localX, localY, material) {

  /** How long, in ticks, the fuse time should be  */
  val fuseDurationTicks: Int = (world.worldTicker.tps * FUSE_DURATION_SECONDS).toInt()
  private val strength: Float = EXPLOSION_STRENGTH.toFloat()
  private var glowing = false

  @Volatile
  private var exploded = false
  private var startTick: Long

  init {
    startTick = world.tick
  }

  override fun shouldTick(): Boolean {
    return !exploded
  }

  override fun tick() {
    if (exploded) {
      return
    }
    val ticked = world.tick - startTick
    if (ticked > fuseDurationTicks && Main.isAuthoritative()) {
      exploded = true
      Main.inst().scheduler.executeAsync {
        val destroyed = gdxSetOf<Block>()
        val worldX = worldX
        val worldY = worldY
        val world = world
        var x = MathUtils.floor(worldX - strength)
        while (x < worldX + strength) {
          var y = MathUtils.floor(worldY - strength)
          while (y < worldY + strength) {
            val block = world.getRawBlock(x, y, true)
            val mat = block?.material ?: Material.AIR
            val hardness = mat.hardness
            if (mat != Material.AIR && hardness >= 0 && block != null) {
              val dist = (Location.distCubed(worldX, worldY, block.worldX, block.worldY) * hardness * abs(MathUtils.random.nextGaussian() + RESISTANCE))
              if (dist < strength * strength && (block !is TntBlock || block === this)) {
                destroyed.add(block)
              }
            }
            y++
          }
          x++
        }
        world.removeBlocks(destroyed)
      }
    }
  }

  // Do min 1 as 0 would set startTick to current tick
  @get:Synchronized
  @set:Synchronized
  var ticksLeft: Long
    get() {
      val ticked = world.tick - startTick
      return fuseDurationTicks - ticked
    }
    set(ticksLeft) {
      // Do min 1 as 0 would set startTick to current tick
      startTick = world.tick - (fuseDurationTicks - ticksLeft)
      Preconditions.checkState(ticksLeft == this.ticksLeft, ticksLeft.toString() + " =/= " + this.ticksLeft)
    }

  override val texture: TextureRegion?
    get() = if (glowing) whiteTexture else super.texture

  override fun save(): ProtoWorld.Block.Builder {
    return super.save().setTnt(TNT.newBuilder().setTicksLeft(ticksLeft))
  }

  override fun load(protoBlock: ProtoWorld.Block) {
    super.load(protoBlock)
    Preconditions.checkArgument(protoBlock.hasTnt())
    val tnt = protoBlock.tnt
    ticksLeft = tnt.ticksLeft
  }

  override fun hudDebug(): String = "ticks left: $ticksLeft, exploded? $exploded"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is TntBlock) return false
    if (!super.equals(other)) return false

    if (fuseDurationTicks != other.fuseDurationTicks) return false
    if (strength != other.strength) return false
    if (glowing != other.glowing) return false
    if (exploded != other.exploded) return false
    if (startTick != other.startTick) return false
    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + fuseDurationTicks
    result = 31 * result + strength.hashCode()
    result = 31 * result + glowing.hashCode()
    result = 31 * result + exploded.hashCode()
    result = 31 * result + startTick.hashCode()
    return result
  }

  companion object {

    /** Maximum explosion radius  */
    const val EXPLOSION_STRENGTH = 40

    /**
     * Randomness to what blocks are destroyed.
     *
     * Lower means more blocks destroyed, but more random holes around the crater.
     *
     * Higher means fewer blocks destroyed but less unconnected destroyed blocks. Note that too
     * large will not look good
     *
     * Minimum value should be above 3 as otherwise the edge of the explosion will clearly be
     * visible
     */
    const val RESISTANCE = 8
    const val FUSE_DURATION_SECONDS = 3f
  }
}
