@file:Suppress("NOTHING_TO_INLINE")

package no.elg.infiniteBootleg.util

import com.badlogic.gdx.math.Vector2
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2i
import no.elg.infiniteBootleg.world.Location
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.blocks.Block.Companion.compactWorldLoc
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.world.World
import org.jetbrains.annotations.Contract
import java.lang.Short.SIZE
import kotlin.math.abs
import kotlin.math.floor

/**
 * @param this@worldToChunk A location in world view
 * @return The location in chunk view
 */
@Contract("_ -> new")
inline fun Vector2.worldToChunk(): Location = Location(x.toInt().worldToChunk(), y.toInt().worldToChunk())

/**
 * @param this@worldToChunk The world coordinate to convert
 * @return The chunk coordinate the given coordinate is in
 */

@Contract(pure = true)
inline fun Int.worldToChunk(): Int = this shr Chunk.CHUNK_SIZE_SHIFT

/**
 * @param this@worldToChunk The world coordinate to convert
 * @return The chunk coordinate the given coordinate is in
 */
@Contract(pure = true)
inline fun Float.worldToChunk(): Int = toInt().worldToChunk()

/**
 * Convert a chunk coordinate to world coordinate
 *
 * @param this@chunkToWorld The chunk coordinate to convert
 * @return The chunk coordinate in world view
 */

@Contract(pure = true)
inline fun Int.chunkToWorld(): Int = this shl Chunk.CHUNK_SIZE_SHIFT

/**
 * Convert a chunk coordinate to world coordinate
 *
 * @param this@chunkToWorld The chunk coordinate to convert
 * @return The chunk coordinate in world view
 */
@Contract(pure = true)
inline fun Float.chunkToWorld(): Int = toInt().chunkToWorld()

/**
 * Convert a chunk coordinate to world coordinate with an offset within the chunk
 *
 * @param chunkCoord The chunk coordinate to convert
 * @param offset     The offset within the chunk (no bounds checking), but it should be in the range
 * `0 <= offset < `[Chunk.CHUNK_SIZE]
 * @return The chunk coordinate in world view plus the offset
 */

@Contract(pure = true)
inline fun chunkToWorld(chunkCoord: Int, offset: Int): Int = chunkCoord.chunkToWorld() + offset

/**
 * Calculate the offset the given world coordinate have in its chunk
 *
 * @param this@chunkOffset A coordinate in world view
 * @return The local coordinate given coordinate have in chunk view
 */

@Contract(pure = true)
inline fun Int.chunkOffset(): Int = this - worldToChunk().chunkToWorld()

/**
 * @param localX The chunk local x coordinate
 * @param localY The chunk local y coordinate
 * @return If given x and y are both between 0 (inclusive) and [Chunk.CHUNK_SIZE]
 * (exclusive)
 */

@Contract(pure = true)
inline fun isInsideChunk(localX: Int, localY: Int): Boolean = localX >= 0 && localX < Chunk.CHUNK_SIZE && localY >= 0 && localY < Chunk.CHUNK_SIZE

/**
 * @param localX The chunk local x coordinate
 * @param localY The chunk local y coordinate
 * @return If given x and y are on the edge of a chunk, while still inside the chunk
 */
@Contract(pure = true)
inline fun isInnerEdgeOfChunk(localX: Int, localY: Int): Boolean = localX == 0 || localX == Chunk.CHUNK_SIZE - 1 || localY == 0 || localY == Chunk.CHUNK_SIZE - 1

/**
 * @param worldX The world x coordinate
 * @param worldY The world y coordinate
 * @return The chunk location for the given world coordinates
 */
@Contract(pure = true)
inline fun worldXYtoChunkCompactLoc(worldX: Int, worldY: Int): Long = compactLoc(worldX.worldToChunk(), worldY.worldToChunk())

/**
 * Store two integers inside a single long
 *
 * @param x The x coordinate of the location
 * @param y The y coordinate of the location
 * @return A long containing both the x and y int
 */

inline fun compactLoc(x: Int, y: Int): Long = // as an int have 32 bits and long 64, we can store two ints inside a long
  x.toLong() shl Integer.SIZE or (y.toLong() and 0xffffffffL)

/**
 * @param this@decompactLocX A long created by [.compactLoc]
 * @return The x coordinate of the compacted location
 */

inline fun Long.decompactLocX(): Int = (this shr Integer.SIZE).toInt()

/**
 * @param this@decompactLocY A long created by [.compactLoc]
 * @return The y coordinate of the compacted location
 */

inline fun Long.decompactLocY(): Int = toInt()

inline fun Long.worldCompactToChunk(): Long = compactLoc(decompactLocX().worldToChunk(), decompactLocY().worldToChunk())

inline fun compactShort(a: Short, b: Short, c: Short, d: Short): Long = compactLoc(compactShort(a, b), compactShort(c, d))

inline fun compactShort(a: Short, b: Short): Int = a.toInt() shl SIZE or (b.toInt() and 0xffff)

inline fun compactChunkToWorld(chunk: Chunk, localX: Int, localY: Int): Long = compactLoc(chunkToWorld(chunk.chunkX, localX), chunkToWorld(chunk.chunkY, localY))

/**
 * @param this@decompactShortA A long created by [.compactLoc]
 * @return The x coordinate of the compacted location
 */

inline fun Int.decompactShortA(): Short = (this shr SIZE).toShort()

/**
 * @param this@decompactShortB A long created by [.compactLoc]
 * @return The y coordinate of the compacted location
 */

inline fun Int.decompactShortB(): Short = toShort()

inline fun stringifyCompactLoc(x: Int, y: Int): String = "($x,$y)"

inline fun stringifyCompactLoc(compactLoc: Long): String = stringifyCompactLoc(compactLoc.decompactLocX(), compactLoc.decompactLocY())

inline fun stringifyCompactLoc(chunk: Chunk): String = stringifyCompactLoc(chunk.compactLocation)
inline fun stringifyCompactLoc(block: Block): String = stringifyCompactLoc(block.compactWorldLoc)

inline fun stringifyCompactLoc(vector: Vector2i): String = stringifyCompactLoc(vector.x, vector.y)

inline fun stringifyChunkToWorld(chunk: Chunk, localX: Int, localY: Int): String = "(${chunkToWorld(chunk.chunkX, localX)},${chunkToWorld(chunk.chunkY, localY)})"

/**
 * @param worldCoord A part of a coordinate in the world
 * @return Which coordinate a block at the given world coordinate will have
 */
inline fun worldToBlock(worldCoord: Float): Int = floor(worldCoord.toDouble()).toInt()

/**
 * @param worldOffset Given by [WorldBody.getWorldOffsetX] and [WorldBody.getWorldOffsetY]
 * @param worldCoord  The world coordinate to translate to screen coordinates
 * @return The world coordinate to translated to screen coordinates
 */
inline fun worldToScreen(worldCoord: Float, worldOffset: Float): Float = (worldCoord + worldOffset) * Block.BLOCK_SIZE

operator fun Long.component1(): Int = this.decompactLocX()
operator fun Long.component2(): Int = this.decompactLocY()

inline fun isBlockInsideRadius(worldX: Float, worldY: Float, blockBlockX: Int, targetBlockY: Int, radius: Float): Boolean =
  abs(Vector2.dst2(worldX, worldY, blockBlockX + World.HALF_BLOCK_SIZE, targetBlockY + World.HALF_BLOCK_SIZE)) < radius * radius
