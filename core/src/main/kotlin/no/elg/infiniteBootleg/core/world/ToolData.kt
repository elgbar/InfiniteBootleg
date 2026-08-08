package no.elg.infiniteBootleg.core.world

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.core.items.ToolItem
import no.elg.infiniteBootleg.core.items.ToolItemFist
import no.elg.infiniteBootleg.core.util.BlockUnitF
import no.elg.infiniteBootleg.core.util.INITIAL_BRUSH_SIZE
import no.elg.infiniteBootleg.core.util.INITIAL_INTERACT_RADIUS
import no.elg.infiniteBootleg.core.util.WorldCompactLoc
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.centerOfBlock
import no.elg.infiniteBootleg.core.util.interactableBlocksWithinRadius
import no.elg.infiniteBootleg.core.world.Tool.Companion.filterNotAirBlock
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.core.world.world.World.Companion.getLocationsAABBFromLowerLeftCorner
import no.elg.infiniteBootleg.protobuf.ElementKt.ToolElementKt.broadaxeData
import no.elg.infiniteBootleg.protobuf.ElementKt.ToolElementKt.pickaxeData
import no.elg.infiniteBootleg.protobuf.ElementKt.ToolElementKt.reclaimerData
import no.elg.infiniteBootleg.protobuf.ElementKt.toolElement
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Element.ToolElement.BroadaxeData.MajorAxis
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Element.ToolElement.HandData
import no.elg.infiniteBootleg.protobuf.broadaxeOrNull
import no.elg.infiniteBootleg.protobuf.pickaxeOrNull
import no.elg.infiniteBootleg.protobuf.reclaimerOrNull
import kotlin.math.floor
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Element.ToolElement as ProtoToolElement

sealed interface ToolData {
  val interactionRadius: BlockUnitF

  fun breakableLocs(entity: Entity, world: World, blockX: WorldCoord, blockY: WorldCoord): Sequence<WorldCompactLoc>

  fun asProto(): ProtoToolElement

  fun toItem(maxStock: UInt, stock: UInt): ToolItem<*>

  fun onRightClick(): Unit = Unit
}

data class PickaxeToolData(override var interactionRadius: BlockUnitF = INITIAL_INTERACT_RADIUS, var brushRadius: BlockUnitF = INITIAL_BRUSH_SIZE) : ToolData {

  override fun breakableLocs(entity: Entity, world: World, blockX: WorldCoord, blockY: WorldCoord): Sequence<WorldCompactLoc> {
    val baseSeq = World.getLocationsWithin(blockX, blockY, brushRadius).asSequence()
    return entity
      .interactableBlocksWithinRadius(world, interactionRadius, baseSeq)
      .filterNotAirBlock(world)
  }

  override fun asProto(): ProtoToolElement =
    toolElement {
      interactionRadius = this@PickaxeToolData.interactionRadius
      pickaxe = pickaxeData {
        this.brushRadius = brushRadius
      }
    }

  override fun toItem(maxStock: UInt, stock: UInt): ToolItem<PickaxeToolData> = Tool.Pickaxe.toItem(maxStock, stock, this)

  companion object {
    fun fromProto(proto: ProtoToolElement): PickaxeToolData = PickaxeToolData(proto.interactionRadius, proto.pickaxeOrNull?.brushRadius ?: INITIAL_BRUSH_SIZE)
  }
}

data class ReclaimerToolData(override var interactionRadius: BlockUnitF = INITIAL_INTERACT_RADIUS, var brushRadius: BlockUnitF = INITIAL_BRUSH_SIZE) : ToolData {

  override fun breakableLocs(entity: Entity, world: World, blockX: WorldCoord, blockY: WorldCoord): Sequence<WorldCompactLoc> {
    val baseSeq = World.getLocationsWithin(blockX, blockY, brushRadius).asSequence()
    return entity
      .interactableBlocksWithinRadius(world, interactionRadius, baseSeq)
      .filterNotAirBlock(world)
  }

  override fun asProto(): ProtoToolElement =
    toolElement {
      interactionRadius = this@ReclaimerToolData.interactionRadius
      reclaimer = reclaimerData {
        this.brushRadius = brushRadius
      }
    }

  override fun toItem(maxStock: UInt, stock: UInt): ToolItem<ReclaimerToolData> = Tool.Reclaimer.toItem(maxStock, stock, this)

  companion object {
    fun fromProto(proto: ProtoToolElement): ReclaimerToolData = ReclaimerToolData(proto.interactionRadius, proto.reclaimerOrNull?.brushRadius ?: INITIAL_BRUSH_SIZE)
  }
}

data class BroadaxeToolData(
  override var interactionRadius: BlockUnitF = INITIAL_INTERACT_RADIUS,
  var brushMajorAxisSize: BlockUnitF = INITIAL_BRUSH_SIZE,
  var horizontalIsMajorAxis: Boolean = INITIAL_MAJOR_AXIS
) : ToolData {

  override fun onRightClick() {
    horizontalIsMajorAxis = !horizontalIsMajorAxis
  }

  override fun breakableLocs(entity: Entity, world: World, blockX: WorldCoord, blockY: WorldCoord): Sequence<WorldCompactLoc> =
    if (horizontalIsMajorAxis) {
      breakableLocsHorizontally(entity, world, blockX, blockY)
    } else {
      breakableLocsVertically(entity, world, blockX, blockY)
    }

  private fun breakableLocsVertically(entity: Entity, world: World, blockX: WorldCoord, blockY: WorldCoord): Sequence<WorldCompactLoc> {
    val ifAboveOfEntity = entity.positionComponent.y <= blockY.centerOfBlock()
    val topWorldY = blockY.toDouble()
    val offsetY = when {
      brushMajorAxisSize == 1f -> 0.0
      ifAboveOfEntity -> floor(brushMajorAxisSize) - 1.0
      else -> -floor(brushMajorAxisSize) + 1.0
    }

    val locationsAABBFromCorner = getLocationsAABBFromLowerLeftCorner(
      blockX.toDouble() - (MINOR_AXIS_OFFSET / 2.0),
      topWorldY,
      MINOR_AXIS_OFFSET,
      offsetY
    )
    return locationsAABBFromCorner.asSequence().filterNotAirBlock(world)
  }

  private fun breakableLocsHorizontally(entity: Entity, world: World, blockX: WorldCoord, blockY: WorldCoord): Sequence<WorldCompactLoc> {
    val ifLeftOfEntity = entity.positionComponent.x <= blockX.centerOfBlock()
    val leftWorldX = blockX.toDouble()
    val offsetX = when {
      brushMajorAxisSize == 1f -> 0.0
      ifLeftOfEntity -> floor(brushMajorAxisSize) - 1.0
      else -> -floor(brushMajorAxisSize) + 1.0
    }

    val locationsAABBFromCorner = getLocationsAABBFromLowerLeftCorner(
      leftWorldX,
      blockY.toDouble() - (MINOR_AXIS_OFFSET / 2.0),
      offsetX,
      MINOR_AXIS_OFFSET
    )
    return locationsAABBFromCorner.asSequence().filterNotAirBlock(world)
  }

  override fun asProto(): ProtoToolElement =
    toolElement {
      interactionRadius = this@BroadaxeToolData.interactionRadius
      broadaxe = broadaxeData {
        this.brushRadiusInMajorAxis = brushMajorAxisSize
        this.axis = if (horizontalIsMajorAxis) MajorAxis.HORIZONTAL else MajorAxis.VERTICAL
      }
    }

  override fun toItem(maxStock: UInt, stock: UInt): ToolItem<BroadaxeToolData> = Tool.Broadaxe.toItem(maxStock, stock, this)

  companion object {
    /**
     * Always 3 blocks in the minor axis, to calculate offset we remove one
     */
    private const val MINOR_AXIS_OFFSET: Double = 3 - 1.0

    private const val INITIAL_MAJOR_AXIS = true

    fun fromProto(proto: ProtoToolElement): BroadaxeToolData {
      // Handles misconfigured broadaxeData as empty data
      val broadaxeToolData = proto.broadaxeOrNull
      return BroadaxeToolData(
        interactionRadius = proto.interactionRadius,
        brushMajorAxisSize = broadaxeToolData?.brushRadiusInMajorAxis ?: INITIAL_BRUSH_SIZE,
        horizontalIsMajorAxis = broadaxeToolData?.run { axis == MajorAxis.HORIZONTAL } ?: INITIAL_MAJOR_AXIS
      )
    }
  }
}

object FistToolData : ToolData {
  override val interactionRadius: BlockUnitF = 3f

  override fun breakableLocs(entity: Entity, world: World, blockX: WorldCoord, blockY: WorldCoord): Sequence<WorldCompactLoc> {
    val baseSeq = World.getLocationsWithin(blockX, blockY, INITIAL_BRUSH_SIZE).asSequence()
    return entity
      .interactableBlocksWithinRadius(world, interactionRadius, baseSeq)
      .filterNotAirBlock(world)
  }

  override fun asProto(): ProtoToolElement =
    toolElement {
      interactionRadius = 0f
      hand = HandData.getDefaultInstance()
    }

  override fun toItem(maxStock: UInt, stock: UInt): ToolItem<FistToolData> = ToolItemFist
}
