package no.elg.infiniteBootleg.core.world.box2d.service

//import com.badlogic.ashley.core.Entity
//import io.github.oshai.kotlinlogging.KotlinLogging
//import no.elg.infiniteBootleg.core.main.Main
//import no.elg.infiniteBootleg.core.util.isAir
//import no.elg.infiniteBootleg.core.util.isBeingRemoved
//import no.elg.infiniteBootleg.core.util.isNotAir
//import no.elg.infiniteBootleg.core.world.blocks.Block
//import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.worldX
//import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.worldY
//import no.elg.infiniteBootleg.core.world.chunks.Chunk
//import no.elg.infiniteBootleg.core.world.ecs.components.MaterialComponent.Companion.materialOrNull
//import no.elg.infiniteBootleg.core.world.ecs.components.events.PhysicsEvent
//import no.elg.infiniteBootleg.core.world.ecs.system.event.PhysicsSystem.getOtherFixtureUserData
//import no.elg.infiniteBootleg.protobuf.Packets
//
//object FallingBlockContactService {
//
//  private val logger = KotlinLogging.logger {}
//
//  private const val MAX_DELTA_UP = Chunk.Companion.CHUNK_SIZE
//
//  fun handleFallingBlockContactBeginsEvent(entity: Entity, event: PhysicsEvent.ContactBeginsEvent) {
//    if (Main.Companion.isAuthoritative) {
//      val material = entity.materialOrNull ?: return
//      val block = event.getOtherFixtureUserData<Block>(entity) { true } ?: return
//      val newX: Int = block.worldX
//      val newY: Int = block.worldY
//      val world = block.world
//      synchronized(world) {
//        if (entity.isBeingRemoved) {
//          return
//        }
//        world.removeEntity(entity, Packets.DespawnEntity.DespawnReason.NATURAL)
//
//        var deltaY = 0
//        var blockUp: Block?
//        do {
//          if (deltaY > MAX_DELTA_UP) {
//            logger.trace { "Reached max delta up for handling falling block contact" }
//            return
//          }
//          deltaY++
//          blockUp = block.chunk.getBlock(newX, newY + deltaY, true)
//        } while (blockUp.isNotAir(true))
//
//        // Only place the block if there is actual free space above
//        if (blockUp.isAir(true)) {
//          world.setBlock(newX, newY + deltaY, material)
//        }
//      }
//    }
//  }
//}
