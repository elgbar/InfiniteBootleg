@file:Suppress("NOTHING_TO_INLINE")

package no.elg.infiniteBootleg.core.util

import com.badlogic.gdx.math.Vector2
import ktx.collections.GdxLongArray
import no.elg.infiniteBootleg.core.world.Direction
import no.elg.infiniteBootleg.core.world.HorizontalDirection
import no.elg.infiniteBootleg.core.world.VerticalDirection
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.core.world.world.World.Companion.HALF_BLOCK_SIZE_D
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2i
import org.jetbrains.annotations.Contract
import java.lang.Float.intBitsToFloat
import java.lang.Short.SIZE
import kotlin.math.abs
import kotlin.math.floor

/**
 * @param this@worldToChunk The world coordinate to convert
 * @return The chunk coordinate the given coordinate is in
 */

@Contract(pure = true)
inline fun WorldCoord.worldToChunk(): ChunkCoord = this shr Chunk.Companion.CHUNK_SIZE_SHIFT

/**
 * @param this@worldToChunk The world coordinate to convert
 * @return The chunk coordinate the given coordinate is in
 */
@Contract(pure = true)
inline fun Number.worldToChunk(): ChunkCoord = toInt().worldToChunk()

@Contract(pure = true)
inline fun WorldCompactLoc.worldToChunk(): ChunkCompactLoc = compactInt(decompactLocX().worldToChunk(), decompactLocY().worldToChunk())

@Contract(pure = true)
inline fun WorldCompactLoc.worldToChunkX(): ChunkCoord = decompactLocX().worldToChunk()

@Contract(pure = true)
inline fun WorldCompactLoc.worldToChunkY(): ChunkCoord = decompactLocY().worldToChunk()

/**
 * Convert a chunk coordinate to world coordinate with an offset within the chunk
 *
 * @param chunkCoord The chunk coordinate to convert
 * @param offset     The offset within the chunk (no bounds checking), but it should be in the range
 * `0 <= offset < `[Chunk.Companion.CHUNK_SIZE]
 * @return The chunk coordinate in world view plus the offset
 */
@Contract(pure = true)
inline fun ChunkCoord.chunkToWorld(offset: LocalCoord): WorldCoord = chunkToWorld() + offset

@Contract(pure = true)
inline fun ChunkCoord.chunkToWorld(): WorldCoord = this shl Chunk.Companion.CHUNK_SIZE_SHIFT

@Contract(pure = true)
inline fun ChunkCompactLoc.chunkToWorld(localCoords: LocalCompactLoc): WorldCompactLoc = compactChunkToWorld(this, localCoords.decompactLocX(), localCoords.decompactLocY())

/**
 * @param worldCoord A part of a coordinate in the world
 * @return Which coordinate a block at the given world coordinate will have
 */
@Contract(pure = true)
inline fun WorldCoordNumber.worldToBlock(): WorldCoord = floor(this.toDouble()).toInt()

/**
 * @param worldOffset Given by [WorldBody.getWorldOffsetX] and [WorldBody.getWorldOffsetY]
 * @param this@worldToScreen  The world coordinate to translate to screen coordinates
 * @return The world coordinate to translated to screen coordinates
 */
@Contract(pure = true)
inline fun WorldCoordNumber.worldToScreen(): Float = toFloat() * Block.Companion.BLOCK_TEXTURE_SIZE

/**
 * Calculate the offset the given world coordinate have in its chunk
 *
 * @param this@chunkOffset A coordinate in world view
 * @return The local coordinate given coordinate have in chunk view
 */
@Contract(pure = true)
inline fun WorldCoord.chunkOffset(): LocalCoord = this - worldToChunk().chunkToWorld()

@Contract(pure = true)
inline fun WorldCompactLoc.chunkOffsetX(): LocalCoord = this.decompactLocX().chunkOffset()

@Contract(pure = true)
inline fun WorldCompactLoc.chunkOffsetY(): LocalCoord = this.decompactLocY().chunkOffset()

/**
 * @param localX The chunk local x coordinate
 * @param localY The chunk local y coordinate
 * @return If given x and y are both between 0 (inclusive) and [Chunk.Companion.CHUNK_SIZE]
 * (exclusive)
 */
@Contract(pure = true)
inline fun isInsideChunk(localX: LocalCoord, localY: LocalCoord): Boolean = localX >= 0 && localX < Chunk.Companion.CHUNK_SIZE && localY >= 0 && localY < Chunk.Companion.CHUNK_SIZE

/**
 * @param localX The chunk local x coordinate
 * @param localY The chunk local y coordinate
 * @return If given x and y are on the edge of a chunk, while still inside the chunk
 */
@Contract(pure = true)
inline fun isInnerEdgeOfChunk(localCoord: LocalCoord): Boolean = localCoord == 0 || localCoord == Chunk.Companion.CHUNK_SIZE - 1

@Contract(pure = true)
inline fun Block.findWhichInnerEdgesOfChunk(): List<Direction> =
  mutableListOf<Direction>().also {
    if (localX == 0) it += Direction.WEST
    if (localX == Chunk.Companion.CHUNK_SIZE - 1) it += Direction.EAST
    if (localY == 0) it += Direction.SOUTH
    if (localY == Chunk.Companion.CHUNK_SIZE - 1) it += Direction.NORTH
  }

/**
 * @param localX The chunk local x coordinate
 * @param localY The chunk local y coordinate
 * @return If given x and y are on the edge of a chunk, while still inside the chunk
 */
@Contract(pure = true)
inline fun isInnerEdgeOfChunk(localX: LocalCoord, localY: LocalCoord): Boolean = isInnerEdgeOfChunk(localX) || isInnerEdgeOfChunk(localY)

@Contract(pure = true)
inline fun isInnerEdgeOfChunk(localLoc: LocalCompactLoc): Boolean {
  val (localX, localY) = localLoc
  return isInnerEdgeOfChunk(localX, localY)
}

/**
 * If this chunk is a neighbor of the given block.
 */
@Contract(pure = true)
inline fun Chunk.isCardinalNeighbor(chunk: Chunk) = abs(chunk.chunkX - this.chunkX) + abs(chunk.chunkY - this.chunkY) == 1

/**
 * @return If this block is on the edge to the given chunk
 */
@Contract(pure = true)
inline fun Block.isNextTo(chunk: Chunk): Boolean = this.chunk.isCardinalNeighbor(chunk) && isInnerEdgeOfChunk(localX, localY)

@Contract(pure = true)
fun Chunk.closestBlockTo(block: Block): LocalCompactLoc {
  val other = block.chunk
  val localX = when (horizontalDirectionTo(other)) {
    HorizontalDirection.WESTWARD -> 0
    HorizontalDirection.HORIZONTALLY_ALIGNED -> block.localX
    HorizontalDirection.EASTWARD -> Chunk.Companion.CHUNK_SIZE - 1
  }
  val localY = when (verticalDirectionTo(other)) {
    VerticalDirection.NORTHWARD -> Chunk.Companion.CHUNK_SIZE - 1
    VerticalDirection.VERTICALLY_ALIGNED -> block.localY
    VerticalDirection.SOUTHWARD -> 0
  }
  return compactInt(localX, localY)
}

@Contract(pure = true)
fun Chunk.shortestDistanceSquared(block: Block): Float {
  val (localX, localY) = closestBlockTo(block)
  return Vector2.dst2(chunkX.chunkToWorld(localX) + 0.5f, chunkY.chunkToWorld(localY) + 0.5f, block.worldX + 0.5f, block.worldY + 0.5f)
}

@Contract(pure = true)
fun Chunk.isWithinRadius(block: Block, radius: Float): Boolean = shortestDistanceSquared(block) <= radius * radius

/**
 * @param worldX The world x coordinate
 * @param worldY The world y coordinate
 * @return The chunk location for the given world coordinates
 */
@Contract(pure = true)
inline fun worldXYtoChunkCompactLoc(worldX: WorldCoord, worldY: WorldCoord): ChunkCompactLoc = compactInt(worldX.worldToChunk(), worldY.worldToChunk())

/**
 * An int have 32 bits and long 64, we can store two ints inside a long
 *
 * @param x The x coordinate of the location
 * @param y The y coordinate of the location
 * @return A long containing both the x and y int
 */
@Contract(pure = true)
inline fun compactInt(x: Int, y: Int): Compacted2Int = x.toLong() shl Integer.SIZE or (y.toLong() and 0xffffffffL)

/**
 * An int have 32 bits and long 64, we can store two ints inside a long
 *
 * @param x The x coordinate of the location
 * @param y The y coordinate of the location
 * @return A long containing both the x and y int
 */
@Contract(pure = true)
inline fun compactFloat(x: Float, y: Float): Compacted2Float = compactInt(x.toRawBits(), y.toRawBits())

/**
 * An int have 32 bits and long 64, we can store two ints inside a long.
 *
 * This is a lossy conversion, as the two longs cannot fit into an int.
 *
 * @param x The x coordinate of the location, acts as a long keep
 * @param y The y coordinate of the location
 * @return A long containing both the x and y int
 */
@Contract(pure = true)
inline fun compactLong(x: Long, y: Long): Compacted2Int = x shl Integer.SIZE or (y and 0xffffffffL)

/**
 * @param this@decompactLocX A long created by [compactFloat]
 * @return The x coordinate of the compacted location
 */

@Contract(pure = true)
inline fun Compacted2Int.decompactLocX(): Int = (this shr Integer.SIZE).toInt()

/**
 * @param this@decompactLocY A long created by [compactFloat]
 * @return The y coordinate of the compacted location
 */

@Contract(pure = true)
inline fun Compacted2Int.decompactLocY(): Int = toInt()

inline fun Compacted2Float.decompactLocXf(): Float = intBitsToFloat(decompactLocX())
inline fun Compacted2Float.decompactLocYf(): Float = intBitsToFloat(decompactLocY())

// @Contract(pure = true)
// inline fun compactShort(a: Short, b: Short, c: Short, d: Short): Long = compactLoc(compactShort(a, b), compactShort(c, d))

@Contract(pure = true)
inline fun compactShort(a: Short, b: Short): Int = a.toInt() shl SIZE or (b.toInt() and 0xffff)

@Contract(pure = true)
inline fun compactChunkToWorld(chunkPos: ChunkCompactLoc, localX: LocalCoord, localY: LocalCoord): WorldCompactLoc =
  compactInt(chunkPos.decompactLocX().chunkToWorld(localX), chunkPos.decompactLocY().chunkToWorld(localY))

@Contract(pure = true)
inline fun compactChunkToWorld(chunk: Chunk, localX: LocalCoord, localY: LocalCoord): WorldCompactLoc =
  compactInt(chunk.chunkX.chunkToWorld(localX), chunk.chunkY.chunkToWorld(localY))

/**
 * @param this@decompactShortA A long created by [compactFloat]
 * @return The x coordinate of the compacted location
 */
@Contract(pure = true)
inline fun Int.decompactShortA(): Short = (this shr SIZE).toShort()

/**
 * @param this@decompactShortB A long created by [compactFloat]
 * @return The y coordinate of the compacted location
 */
@Contract(pure = true)
inline fun Int.decompactShortB(): Short = toShort()

@Contract(pure = true)
inline fun stringifyCompactLoc(x: Number, y: Number): String = "($x,$y)"

@Contract(pure = true)
inline fun stringifyCompactLoc(compactLoc: Compacted2Int): String = stringifyCompactLoc(compactLoc.decompactLocX(), compactLoc.decompactLocY())

@Contract(pure = true)
inline fun stringifyCompactLoc(posComp: PositionComponent): String = stringifyCompactLoc(posComp.blockX, posComp.blockY)

@Contract(pure = true)
inline fun stringifyCompactLoc(chunk: Chunk): String = stringifyCompactLoc(chunk.compactLocation)

@Contract(pure = true)
inline fun stringifyCompactLoc(block: Block): String = stringifyCompactLoc(block.worldX, block.worldY)

@Contract(pure = true)
inline fun stringifyCompactLocWithChunk(blockX: WorldCoordNumber, blockY: WorldCoordNumber, chunkX: ChunkCoordNumber, chunkY: ChunkCoordNumber): String =
  "${stringifyCompactLoc(blockX, blockY)} in chunk ${stringifyCompactLoc(chunkX, chunkY)}"

@Contract(pure = true)
inline fun stringifyCompactLocWithChunk(block: Block): String = stringifyCompactLocWithChunk(block.worldX, block.worldY, block.chunk.chunkX, block.chunk.chunkY)

@Contract(pure = true)
inline fun stringifyCompactLocWithChunk(posComp: PositionComponent): String = stringifyCompactLocWithChunk(posComp.blockX, posComp.blockY, posComp.chunkX, posComp.chunkY)

@Contract(pure = true)
inline fun stringifyCompactLoc(vector: Vector2i): String = stringifyCompactLoc(vector.x, vector.y)

@Contract(pure = true)
inline fun stringifyChunkToWorld(chunk: Chunk, localX: LocalCoord, localY: LocalCoord): String = "(${chunk.chunkX.chunkToWorld(localX)},${chunk.chunkY.chunkToWorld(localY)})"

@Contract(pure = true)
inline fun stringifyChunkToWorld(chunk: Chunk, localLoc: LocalCompactLoc): String =
  "(${chunk.chunkX.chunkToWorld(localLoc.decompactLocX())},${chunk.chunkY.chunkToWorld(localLoc.decompactLocY())})"

@Contract(pure = true)
inline operator fun Compacted2Int.component1(): Int = this.decompactLocX()

@Contract(pure = true)
inline operator fun Compacted2Int.component2(): Int = this.decompactLocY()

@Contract(pure = true)
inline fun isBlockInsideRadius(
  worldX: Float,
  worldY: Float,
  targetBlockX: WorldCoord,
  targetBlockY: WorldCoord,
  radius: Float
): Boolean = isBlockInsideRadius(worldX, worldY, targetBlockX, targetBlockY, radius.toDouble())

@Contract(pure = true)
fun isBlockInsideRadius(
  worldX: Float,
  worldY: Float,
  targetBlockX: WorldCoord,
  targetBlockY: WorldCoord,
  radius: Double
): Boolean = Vector2.dst2(worldX, worldY, targetBlockX + World.Companion.HALF_BLOCK_SIZE, targetBlockY + World.Companion.HALF_BLOCK_SIZE) < radius * radius

@Contract(pure = true)
fun isBlockInsideRadius(
  worldX: Double,
  worldY: Double,
  targetBlockX: WorldCoord,
  targetBlockY: WorldCoord,
  radius: Double
): Boolean = distCubed(worldX, worldY, targetBlockX + HALF_BLOCK_SIZE_D, targetBlockY + HALF_BLOCK_SIZE_D) < radius * radius

@Contract(pure = true)
fun isBlockInsideRadius(
  worldX: WorldCoord,
  worldY: WorldCoord,
  targetBlockX: WorldCoord,
  targetBlockY: WorldCoord,
  radius: Double
): Boolean = distCubed(worldX, worldY, targetBlockX, targetBlockY) < radius * radius

@Contract(pure = true)
inline fun relativeCompact(x: Int, y: Int, dir: Direction): Compacted2Int {
  val (dx, dy) = dir.compact
  return compactInt(x + dx, y + dy)
}

@Contract(pure = true)
inline fun distCubed(x1: Int, y1: Int, x2: Int, y2: Int): Compacted2Int = (x2 - x1).toLong() * (x2 - x1) + (y2 - y1).toLong() * (y2 - y1)

@Contract(pure = true)
inline fun distCubed(x1: Double, y1: Double, x2: Double, y2: Double): Double = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)

typealias Progress = Float

/**
 * Indicate two float has been compacted into a long
 *
 * Use [decompactLocXf] and [decompactLocYf] to get the original float values
 */
typealias Compacted2Float = Long

/**
 * Indicate two int has been compacted into a long
 *
 * Use [decompactLocX] and [decompactLocY] to get the original int values
 */
typealias Compacted2Int = Long

typealias LocalCompactLocF = Compacted2Float
typealias WorldCompactLocF = Compacted2Float
typealias ChunkCompactLocF = Compacted2Float

typealias LocalCompactLoc = Compacted2Int
typealias WorldCompactLoc = Compacted2Int
typealias ChunkCompactLoc = Compacted2Int

typealias LocalCoord = Int
typealias WorldCoord = Int
typealias ChunkCoord = Int

typealias LocalCoordFloat = Float
typealias WorldCoordFloat = Float
typealias ChunkCoordFloat = Float

typealias LocalCoordSquaredFloat = Float
typealias WorldCoordSquaredFloat = Float
typealias ChunkCoordSquaredFloat = Float

typealias LocalCoordNumber = Number
typealias WorldCoordNumber = Number
typealias ChunkCoordNumber = Number

typealias LocalCoordArray = IntArray
typealias WorldCoordArray = IntArray
typealias ChunkCoordArray = IntArray

typealias LocalCompactLocArray = LongArray
typealias WorldCompactLocArray = LongArray
typealias ChunkCompactLocArray = LongArray

typealias LocalCompactLocGdxArray = GdxLongArray
typealias WorldCompactLocGdxArray = GdxLongArray
typealias ChunkCompactLocGdxArray = GdxLongArray
