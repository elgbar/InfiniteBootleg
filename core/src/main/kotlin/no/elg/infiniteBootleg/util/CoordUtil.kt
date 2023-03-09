package no.elg.infiniteBootleg.util

import com.badlogic.gdx.math.Vector2
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.Location
import org.jetbrains.annotations.Contract

fun Int.worldToChunk(): Int = CoordUtil.worldToChunk(this)
fun Float.worldToChunk(): Int = CoordUtil.worldToChunk(this)
fun Int.chunkToWorld(): Int = CoordUtil.chunkToWorld(this)
fun Float.chunkToWorld(): Int = CoordUtil.chunkToWorld(this)
fun Int.chunkOffset(): Int = CoordUtil.chunkOffset(this)

/**
 * Translate between world(block) and chunk coordinates
 *
 * @author Elg
 */
object CoordUtil {

  /**
   * @param worldLocation A location in world view
   * @return The location in chunk view
   */
  @JvmStatic
  @Contract("_ -> new")
  fun worldToChunk(worldLocation: Location): Location {
    return Location(worldToChunk(worldLocation.x), worldToChunk(worldLocation.y))
  }

  /**
   * @param worldLocation A location in world view
   * @return The location in chunk view
   */
  @Contract("_ -> new")
  fun worldToChunk(worldLocation: Vector2): Location {
    return Location(worldToChunk(worldLocation.x.toInt()), worldToChunk(worldLocation.y.toInt()))
  }

  /**
   * @param worldCoord The world coordinate to convert
   * @return The chunk coordinate the given coordinate is in
   */
  @JvmStatic
  @Contract(pure = true)
  fun worldToChunk(worldCoord: Int): Int {
    return worldCoord shr Chunk.CHUNK_SIZE_SHIFT
  }

  /**
   * @param worldCoord The world coordinate to convert
   * @return The chunk coordinate the given coordinate is in
   */
  @Contract(pure = true)
  fun worldToChunk(worldCoord: Float): Int {
    return worldToChunk(worldCoord.toInt())
  }

  /**
   * @param chunkLocation A location in chunk view
   * @return The location in world view
   */
  @JvmStatic
  @Contract("_ -> new")
  fun chunkToWorld(chunkLocation: Location): Location {
    return Location(chunkToWorld(chunkLocation.x), chunkToWorld(chunkLocation.y))
  }

  /**
   * Convert a chunk coordinate to world coordinate
   *
   * @param chunkCoord The chunk coordinate to convert
   * @return The chunk coordinate in world view
   */
  @JvmStatic
  @Contract(pure = true)
  fun chunkToWorld(chunkCoord: Int): Int {
    return chunkCoord shl Chunk.CHUNK_SIZE_SHIFT
  }

  /**
   * Convert a chunk coordinate to world coordinate
   *
   * @param chunkCoord The chunk coordinate to convert
   * @return The chunk coordinate in world view
   */
  @Contract(pure = true)
  fun chunkToWorld(chunkCoord: Float): Int {
    return chunkToWorld(chunkCoord.toInt())
  }

  /**
   * @param chunkLocation A location in chunk view
   * @param localX        Local x offset from chunk position (no bounds checking)
   * @param localY        Local y offset from chunk position (no bounds checking)
   * @return The location in world view with the local offsets
   */
  @JvmStatic
  @Contract("_, _, _ -> new")
  fun chunkToWorld(chunkLocation: Location, localX: Int, localY: Int): Location {
    return Location(
      chunkToWorld(chunkLocation.x, localX),
      chunkToWorld(chunkLocation.y, localY)
    )
  }

  /**
   * Convert a chunk coordinate to world coordinate with an offset within the chunk
   *
   * @param chunkCoord The chunk coordinate to convert
   * @param offset     The offset within the chunk (no bounds checking), but it should be in the range
   * `0 <= offset < `[Chunk.CHUNK_SIZE]
   * @return The chunk coordinate in world view plus the offset
   */
  @JvmStatic
  @Contract(pure = true)
  fun chunkToWorld(chunkCoord: Int, offset: Int): Int {
    return chunkToWorld(chunkCoord) + offset
  }

  /**
   * Calculate the offset the given world coordinate have in its chunk
   *
   * @param worldCoord A coordinate in world view
   * @return The local coordinate given coordinate have in chunk view
   */
  @JvmStatic
  @Contract(pure = true)
  fun chunkOffset(worldCoord: Int): Int {
    return worldCoord - chunkToWorld(worldToChunk(worldCoord))
  }

  /**
   * @param localX The chunk local x coordinate
   * @param localY The chunk local y coordinate
   * @return If given x and y are both between 0 (inclusive) and [Chunk.CHUNK_SIZE]
   * (exclusive)
   */
  @JvmStatic
  @Contract(pure = true)
  fun isInsideChunk(localX: Int, localY: Int): Boolean {
    return localX >= 0 && localX < Chunk.CHUNK_SIZE && localY >= 0 && localY < Chunk.CHUNK_SIZE
  }

  /**
   * @param localX The chunk local x coordinate
   * @param localY The chunk local y coordinate
   * @return If given x and y are on the edge of a chunk, while still inside the chunk
   */
  @Contract(pure = true)
  fun isInnerEdgeOfChunk(localX: Int, localY: Int): Boolean {
    return localX == 0 || localX == Chunk.CHUNK_SIZE - 1 || localY == 0 || localY == Chunk.CHUNK_SIZE - 1
  }

  /**
   * @param worldX The world x coordinate
   * @param worldY The world y coordinate
   * @return The chunk location for the given world coordinates
   */
  @Contract(pure = true)
  fun worldXYtoChunkLoc(worldX: Int, worldY: Int): Location {
    return Location(worldToChunk(worldX), worldToChunk(worldY))
  }

  /**
   * Store two intergers inside a single long
   *
   * @param x The x coordinate of the location
   * @param y The y coordinate of the location
   * @return A long containing both the x and y int
   */
  @JvmStatic
  fun compactLoc(x: Int, y: Int): Long {
    // as an int have 32 bits and long 64, we can store two ints inside a long
    return x.toLong() shl Integer.SIZE or (y.toLong() and 0xffffffffL)
  }

  /**
   * @param compactLoc A long created by [.compactLoc]
   * @return The x coordinate of the compacted location
   */
  @JvmStatic
  fun decompactLocX(compactLoc: Long): Int {
    return (compactLoc shr Integer.SIZE).toInt()
  }

  /**
   * @param compactLoc A long created by [.compactLoc]
   * @return The y coordinate of the compacted location
   */
  @JvmStatic
  fun decompactLocY(compactLoc: Long): Int {
    return compactLoc.toInt()
  }

  fun compactShort(a: Short, b: Short, c: Short, d: Short): Long {
    return compactLoc(compactShort(a, b), compactShort(c, d))
  }

  @JvmStatic
  fun compactShort(a: Short, b: Short): Int {
    return a.toInt() shl java.lang.Short.SIZE or (b.toInt() and 0xffff)
  }

  /**
   * @param compactLoc A long created by [.compactLoc]
   * @return The x coordinate of the compacted location
   */
  @JvmStatic
  fun decompactShortA(compactLoc: Int): Short {
    return (compactLoc shr java.lang.Short.SIZE).toShort()
  }

  /**
   * @param compactLoc A long created by [.compactLoc]
   * @return The y coordinate of the compacted location
   */
  @JvmStatic
  fun decompactShortB(compactLoc: Int): Short {
    return compactLoc.toShort()
  }

  @JvmStatic
  fun stringifyCompactLoc(compactLoc: Long): String {
    return "(" + decompactLocX(compactLoc) + "," + decompactLocY(compactLoc) + ")"
  }

  @JvmStatic
  fun stringifyCompactLoc(chunk: Chunk): String {
    return stringifyCompactLoc(chunk.compactLocation)
  }

  @JvmStatic
  fun stringifyChunkToWorld(chunk: Chunk, localX: Int, localY: Int): String {
    return (
      "(" +
        chunkToWorld(chunk.chunkX, localX) +
        "," +
        chunkToWorld(chunk.chunkY, localY) +
        ")"
      )
  }

  /**
   * @param compactLoc A long created by [.compactLoc]
   * @return The compacted location as a [Location]
   */
  @JvmStatic
  fun decompactLoc(compactLoc: Long): Location {
    return Location(decompactLocX(compactLoc), decompactLocY(compactLoc))
  }

  /**
   * @param worldCoord A part of a coordinate in the world
   * @return Which coordinate a block at the given world coordinate will have
   */
  fun worldToBlock(worldCoord: Float): Int {
    return Math.floor(worldCoord.toDouble()).toInt()
  }

  /**
   * @param worldOffset Given by [WorldBody.getWorldOffsetX] and [WorldBody.getWorldOffsetY]
   * @param worldCoord  The world coordinate to translate to screen coordinates
   * @return The world coordinate to translated to screen coordinates
   */
  fun worldToScreen(worldCoord: Float, worldOffset: Float): Float {
    return (worldCoord + worldOffset) * Block.BLOCK_SIZE
  }
}
