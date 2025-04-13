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
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2i
import org.jetbrains.annotations.Contract
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

inline fun WorldCompactLoc.worldToChunk(): ChunkCompactLoc = compactLoc(decompactLocX().worldToChunk(), decompactLocY().worldToChunk())

inline fun WorldCompactLoc.worldToChunkX(): ChunkCoord = decompactLocX().worldToChunk()
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
inline fun ChunkCoord.chunkToWorld(): WorldCoord = this shl Chunk.Companion.CHUNK_SIZE_SHIFT

inline fun ChunkCompactLoc.chunkToWorld(localCoords: LocalCompactLoc): WorldCompactLoc = compactChunkToWorld(this, localCoords.decompactLocX(), localCoords.decompactLocY())

/**
 * @param worldCoord A part of a coordinate in the world
 * @return Which coordinate a block at the given world coordinate will have
 */
inline fun WorldCoordNumber.worldToBlock(): WorldCoord = floor(this.toDouble()).toInt()

/**
 * @param worldOffset Given by [WorldBody.getWorldOffsetX] and [WorldBody.getWorldOffsetY]
 * @param this@worldToScreen  The world coordinate to translate to screen coordinates
 * @return The world coordinate to translated to screen coordinates
 */
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

inline fun isInnerEdgeOfChunk(localLoc: LocalCompactLoc): Boolean {
  val (localX, localY) = localLoc
  return isInnerEdgeOfChunk(localX, localY)
}

/**
 * If this chunk is a neighbor of the given block.
 */
inline fun Chunk.isCardinalNeighbor(chunk: Chunk) = abs(chunk.chunkX - this.chunkX) + abs(chunk.chunkY - this.chunkY) == 1

/**
 * @return If this block is on the edge to the given chunk
 */
inline fun Block.isNextTo(chunk: Chunk): Boolean = this.chunk.isCardinalNeighbor(chunk) && isInnerEdgeOfChunk(localX, localY)

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
  return compactLoc(localX, localY)
}

fun Chunk.shortestDistanceSquared(block: Block): Float {
  val (localX, localY) = closestBlockTo(block)
  return Vector2.dst2(chunkX.chunkToWorld(localX) + 0.5f, chunkY.chunkToWorld(localY) + 0.5f, block.worldX + 0.5f, block.worldY + 0.5f)
}

fun Chunk.isWithinRadius(block: Block, radius: Float): Boolean = shortestDistanceSquared(block) <= radius * radius

/**
 * @param worldX The world x coordinate
 * @param worldY The world y coordinate
 * @return The chunk location for the given world coordinates
 */
@Contract(pure = true)
inline fun worldXYtoChunkCompactLoc(worldX: WorldCoord, worldY: WorldCoord): ChunkCompactLoc = compactLoc(worldX.worldToChunk(), worldY.worldToChunk())

/**
 * An int have 32 bits and long 64, we can store two ints inside a long
 *
 * @param x The x coordinate of the location
 * @param y The y coordinate of the location
 * @return A long containing both the x and y int
 */
inline fun compactLoc(x: Int, y: Int): Long = x.toLong() shl Integer.SIZE or (y.toLong() and 0xffffffffL)

/**
 * @param this@decompactLocX A long created by [compactLoc]
 * @return The x coordinate of the compacted location
 */

inline fun Long.decompactLocX(): Int = (this shr Integer.SIZE).toInt()

/**
 * @param this@decompactLocY A long created by [compactLoc]
 * @return The y coordinate of the compacted location
 */

inline fun Long.decompactLocY(): Int = toInt()

inline fun compactShort(a: Short, b: Short, c: Short, d: Short): Long = compactLoc(compactShort(a, b), compactShort(c, d))

inline fun compactShort(a: Short, b: Short): Int = a.toInt() shl SIZE or (b.toInt() and 0xffff)

inline fun compactChunkToWorld(chunkPos: ChunkCompactLoc, localX: LocalCoord, localY: LocalCoord): WorldCompactLoc =
  compactLoc(chunkPos.decompactLocX().chunkToWorld(localX), chunkPos.decompactLocY().chunkToWorld(localY))

inline fun compactChunkToWorld(chunk: Chunk, localX: LocalCoord, localY: LocalCoord): WorldCompactLoc =
  compactLoc(chunk.chunkX.chunkToWorld(localX), chunk.chunkY.chunkToWorld(localY))

/**
 * @param this@decompactShortA A long created by [compactLoc]
 * @return The x coordinate of the compacted location
 */
inline fun Int.decompactShortA(): Short = (this shr SIZE).toShort()

/**
 * @param this@decompactShortB A long created by [compactLoc]
 * @return The y coordinate of the compacted location
 */

inline fun Int.decompactShortB(): Short = toShort()

inline fun stringifyCompactLoc(x: Number, y: Number): String = "($x,$y)"

inline fun stringifyCompactLoc(compactLoc: Long): String = stringifyCompactLoc(compactLoc.decompactLocX(), compactLoc.decompactLocY())

inline fun stringifyCompactLoc(posComp: PositionComponent): String = stringifyCompactLoc(posComp.blockX, posComp.blockY)
inline fun stringifyCompactLoc(chunk: Chunk): String = stringifyCompactLoc(chunk.compactLocation)
inline fun stringifyCompactLoc(block: Block): String = stringifyCompactLoc(block.worldX, block.worldY)

inline fun stringifyCompactLocWithChunk(blockX: WorldCoordNumber, blockY: WorldCoordNumber, chunkX: ChunkCoordNumber, chunkY: ChunkCoordNumber): String =
  "${stringifyCompactLoc(blockX, blockY)} in chunk ${stringifyCompactLoc(chunkX, chunkY)}"

inline fun stringifyCompactLocWithChunk(block: Block): String = stringifyCompactLocWithChunk(block.worldX, block.worldY, block.chunk.chunkX, block.chunk.chunkY)
inline fun stringifyCompactLocWithChunk(posComp: PositionComponent): String = stringifyCompactLocWithChunk(posComp.blockX, posComp.blockY, posComp.chunkX, posComp.chunkY)

inline fun stringifyCompactLoc(vector: Vector2i): String = stringifyCompactLoc(vector.x, vector.y)

inline fun stringifyChunkToWorld(chunk: Chunk, localX: LocalCoord, localY: LocalCoord): String = "(${chunk.chunkX.chunkToWorld(localX)},${chunk.chunkY.chunkToWorld(localY)})"
inline fun stringifyChunkToWorld(chunk: Chunk, localLoc: LocalCompactLoc): String =
  "(${chunk.chunkX.chunkToWorld(localLoc.decompactLocX())},${chunk.chunkY.chunkToWorld(localLoc.decompactLocY())})"

operator fun Long.component1(): Int = this.decompactLocX()
operator fun Long.component2(): Int = this.decompactLocY()

inline fun isBlockInsideRadius(
  worldX: Float,
  worldY: Float,
  targetBlockX: WorldCoord,
  targetBlockY: WorldCoord,
  radius: Float
): Boolean = isBlockInsideRadius(worldX, worldY, targetBlockX, targetBlockY, radius.toDouble())

fun isBlockInsideRadius(
  worldX: Float,
  worldY: Float,
  targetBlockX: WorldCoord,
  targetBlockY: WorldCoord,
  radius: Double
): Boolean = abs(Vector2.dst2(worldX, worldY, targetBlockX + World.Companion.HALF_BLOCK_SIZE, targetBlockY + World.Companion.HALF_BLOCK_SIZE)) < radius * radius

typealias Progress = Float

typealias LocalCompactLoc = Long
typealias WorldCompactLoc = Long
typealias ChunkCompactLoc = Long

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

inline fun relativeCompact(x: Int, y: Int, dir: Direction): Long = compactLoc(x + dir.dx, y + dir.dy)

inline fun distCubed(x1: Int, y1: Int, x2: Int, y2: Int): Long = (x2 - x1).toLong() * (x2 - x1) + (y2 - y1).toLong() * (y2 - y1)

inline fun distCubed(x1: Double, y1: Double, x2: Double, y2: Double): Double = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)
