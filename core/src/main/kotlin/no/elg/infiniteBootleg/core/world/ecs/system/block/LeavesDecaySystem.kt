package no.elg.infiniteBootleg.core.world.ecs.system.block

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.utils.LongQueue
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.events.LeafDecayCheckEvent
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.util.chunkOffset
import no.elg.infiniteBootleg.core.util.component1
import no.elg.infiniteBootleg.core.util.component2
import no.elg.infiniteBootleg.core.util.decompactLocX
import no.elg.infiniteBootleg.core.util.decompactLocY
import no.elg.infiniteBootleg.core.util.isWithin
import no.elg.infiniteBootleg.core.util.relativeCompact
import no.elg.infiniteBootleg.core.util.worldToChunk
import no.elg.infiniteBootleg.core.world.Direction
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.materialOrAir
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.system.AuthoritativeSystem
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.compactBlockLoc
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.core.world.ecs.leafBlockFamily
import no.elg.infiniteBootleg.core.world.ecs.system.api.FamilyEntitySystem

object LeavesDecaySystem : FamilyEntitySystem(leafBlockFamily, UPDATE_PRIORITY_DEFAULT), AuthoritativeSystem {

  private const val DESPAWN_LEAVES_RADIUS = 5f

  private val stack: LongQueue = LongQueue()
  private val seen: LongSet = LongOpenHashSet()

  override fun processEntities(entities: ImmutableArray<Entity>, deltaTime: Float) {
    val entity: Entity = entities.random()
    val srcLoc = entity.compactBlockLoc
    val world = entity.world
    val chunk = world.getChunk(srcLoc.worldToChunk(), load = false) ?: return

    val event = if (Settings.debug && Settings.renderLeafDecay) LeafDecayCheckEvent(srcLoc, seen) else null

    try {
      stack.addFirst(srcLoc)
      while (stack.notEmpty()) {
        val loc = stack.removeLast()
        seen.add(loc)
        event?.also(EventManager::dispatchEvent)
        for (dir in Direction.CARDINAL) {
          val nextLoc = relativeCompact(loc.decompactLocX(), loc.decompactLocY(), dir)
          if (!isWithin(srcLoc, nextLoc, DESPAWN_LEAVES_RADIUS)) {
            // Block is too far away to be connected
            continue
          }
          val (worldX, worldY) = nextLoc

          val neighborMaterial = world.actionOnBlock(worldX, worldY, false) { localX, localY, nullableChunk ->
            // Chunk is not loaded, so we don't know if we should despawn this leaf
            val nextChunk = nullableChunk ?: return
            nextChunk.getRawBlock(localX, localY).materialOrAir()
          }

          if (neighborMaterial == Material.BirchTrunk) {
            // If there is a trunk connected to this leaf block, we don't want to despawn it and can return early
            return
          }
          if (neighborMaterial == Material.BirchLeaves && nextLoc !in seen) {
            stack.addFirst(nextLoc)
          }
        }
      }

      // If no trunks were found, remove the leaf block
      chunk.removeBlock(srcLoc.decompactLocX().chunkOffset(), srcLoc.decompactLocY().chunkOffset())
    } finally {
      stack.clear()
      seen.clear()
    }
  }
}
