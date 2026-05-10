package no.elg.infiniteBootleg.core.world.blocks

import no.elg.infiniteBootleg.protobuf.ProtoWorld

/**
 * The geometric shape of a block within its 1x1 cell.
 *
 * [FULL] occupies the entire cell. The four [STAIR] variants occupy 3/4 of the cell with one
 * 0.5x0.5 quadrant cut out as air; the cut quadrant renders the sky/cave background, and the
 * collision shape is a right triangle that approximates the cut as a smooth slope.
 */
enum class BlockShape {
  FULL,

  /** Air in the top-right (north-east) quadrant. Slope falls from top-left to bottom-right. */
  STAIR_NE,

  /** Air in the top-left (north-west) quadrant. Slope falls from top-right to bottom-left. */
  STAIR_NW,

  /** Air in the bottom-right (south-east) quadrant. Inverted ceiling slope. */
  STAIR_SE,

  /** Air in the bottom-left (south-west) quadrant. Inverted ceiling slope. */
  STAIR_SW;

  val isStair: Boolean get() = this != FULL

  /**
   * Whether the sub-cell at [rx], [ry] (with [resolution] sub-cells per side, origin bottom-left)
   * lies in the air quadrant. Used by the renderer to skip drawing the foreground texture there.
   */
  fun isInAirQuadrant(rx: Int, ry: Int, resolution: Int): Boolean {
    val half = resolution / 2
    val rightHalf = rx >= half
    val topHalf = ry >= half
    return when (this) {
      FULL -> false
      STAIR_NE -> topHalf && rightHalf
      STAIR_NW -> topHalf && !rightHalf
      STAIR_SE -> !topHalf && rightHalf
      STAIR_SW -> !topHalf && !rightHalf
    }
  }

  fun toProto(): ProtoWorld.BlockShape = when (this) {
    FULL -> ProtoWorld.BlockShape.FULL
    STAIR_NE -> ProtoWorld.BlockShape.STAIR_NE
    STAIR_NW -> ProtoWorld.BlockShape.STAIR_NW
    STAIR_SE -> ProtoWorld.BlockShape.STAIR_SE
    STAIR_SW -> ProtoWorld.BlockShape.STAIR_SW
  }

  companion object {
    fun fromProto(proto: ProtoWorld.BlockShape): BlockShape = when (proto) {
      ProtoWorld.BlockShape.FULL -> FULL
      ProtoWorld.BlockShape.STAIR_NE -> STAIR_NE
      ProtoWorld.BlockShape.STAIR_NW -> STAIR_NW
      ProtoWorld.BlockShape.STAIR_SE -> STAIR_SE
      ProtoWorld.BlockShape.STAIR_SW -> STAIR_SW
      ProtoWorld.BlockShape.UNRECOGNIZED -> FULL
    }
  }
}
