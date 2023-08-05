package no.elg.infiniteBootleg.world.ecs

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.google.protobuf.TextFormat
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.input.KeyboardControls
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.EntityKt.additionalComponents
import no.elg.infiniteBootleg.protobuf.EntityKt.tags
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.entity
import no.elg.infiniteBootleg.protobuf.tagsOrNull
import no.elg.infiniteBootleg.util.futureEntity
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.box2dOrNull
import no.elg.infiniteBootleg.world.ecs.components.ExplosiveComponent
import no.elg.infiniteBootleg.world.ecs.components.ExplosiveComponent.Companion.explosiveComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.InventoryComponent
import no.elg.infiniteBootleg.world.ecs.components.InventoryComponent.Companion.inventoryComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.KillableComponent
import no.elg.infiniteBootleg.world.ecs.components.KillableComponent.Companion.killableComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.LookDirectionComponent
import no.elg.infiniteBootleg.world.ecs.components.LookDirectionComponent.Companion.lookDirectionComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent.Companion.materialComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.NameComponent
import no.elg.infiniteBootleg.world.ecs.components.NameComponent.Companion.nameComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.SelectedInventoryItemComponent
import no.elg.infiniteBootleg.world.ecs.components.SelectedInventoryItemComponent.Companion.selectedInventoryItemComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.TextureRegionComponent
import no.elg.infiniteBootleg.world.ecs.components.TextureRegionComponent.Companion.textureRegionComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.velocityComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.additional.ChunkComponent
import no.elg.infiniteBootleg.world.ecs.components.additional.ChunkComponent.Companion.chunkComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.additional.DoorComponent
import no.elg.infiniteBootleg.world.ecs.components.additional.DoorComponent.Companion.doorComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.additional.GroundedComponent
import no.elg.infiniteBootleg.world.ecs.components.additional.GroundedComponent.Companion.groundedComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.additional.LocallyControlledComponent
import no.elg.infiniteBootleg.world.ecs.components.additional.LocallyControlledComponent.Companion.locallyControlledComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.additional.OccupyingBlocksComponent
import no.elg.infiniteBootleg.world.ecs.components.additional.OccupyingBlocksComponent.Companion.occupyingBlocksComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.events.InputEventQueue
import no.elg.infiniteBootleg.world.ecs.components.events.InputEventQueue.Companion.inputEventQueueOrNull
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEventQueue
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEventQueue.Companion.physicsEventQueueOrNull
import no.elg.infiniteBootleg.world.ecs.components.required.EntityTypeComponent
import no.elg.infiniteBootleg.world.ecs.components.required.EntityTypeComponent.Companion.entityTypeComponent
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.idComponent
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.worldComponent
import no.elg.infiniteBootleg.world.ecs.components.tags.FlyingTag
import no.elg.infiniteBootleg.world.ecs.components.tags.FlyingTag.Companion.flyingComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.tags.FollowedByCameraTag
import no.elg.infiniteBootleg.world.ecs.components.tags.FollowedByCameraTag.Companion.followedByCameraComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.tags.GravityAffectedTag
import no.elg.infiniteBootleg.world.ecs.components.tags.GravityAffectedTag.Companion.gravityAffectedComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.tags.IgnorePlaceableCheckTag
import no.elg.infiniteBootleg.world.ecs.components.tags.IgnorePlaceableCheckTag.Companion.ignorePlaceableCheckComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.tags.LeafDecayTag
import no.elg.infiniteBootleg.world.ecs.components.tags.LeafDecayTag.Companion.leafDecayComponentOrNull
import no.elg.infiniteBootleg.world.world.ClientWorld
import no.elg.infiniteBootleg.world.world.World
import java.util.concurrent.CompletableFuture
import no.elg.infiniteBootleg.world.ecs.components.Box2DBodyComponent.Companion.checkShouldLoad as box2DBodyComponentCheckShouldLoad

fun Entity.save(): ProtoWorld.Entity {
  return entity {
    this@save.entityTypeComponent.apply { save() }
    this@save.idComponent.apply { save() }
    this@save.positionComponent.apply { save() }
    this@save.worldComponent.apply { save() }

    this@save.box2dOrNull?.apply { save() }
    this@save.explosiveComponentOrNull?.apply { save() }
    this@save.inventoryComponentOrNull?.apply { save() }
    this@save.killableComponentOrNull?.apply { save() }
    this@save.lookDirectionComponentOrNull?.apply { save() }
    this@save.materialComponentOrNull?.apply { save() }
    this@save.nameComponentOrNull?.apply { save() }
    this@save.selectedInventoryItemComponentOrNull?.apply { save() }
    this@save.textureRegionComponentOrNull?.apply { save() }
    this@save.velocityComponentOrNull?.apply { save() }

    tags = tags {
      this@save.flyingComponentOrNull?.apply { save() }
      this@save.followedByCameraComponentOrNull?.apply { save() }
      this@save.gravityAffectedComponentOrNull?.apply { save() }
      this@save.ignorePlaceableCheckComponentOrNull?.apply { save() }
      this@save.leafDecayComponentOrNull?.apply { save() }
    }

    additionalComponents = additionalComponents {
      this@save.chunkComponentOrNull?.apply { save() }
      this@save.doorComponentOrNull?.apply { save() }
      this@save.groundedComponentOrNull?.apply { save() }
      this@save.locallyControlledComponentOrNull?.apply { save() }
      this@save.occupyingBlocksComponentOrNull?.apply { save() }
      this@save.inputEventQueueOrNull?.apply { save() }
      this@save.physicsEventQueueOrNull?.apply { save() }
    }
  }
}

fun Engine.load(protoEntity: ProtoWorld.Entity, world: World, chunk: Chunk? = null): CompletableFuture<Entity> {
  if (Settings.debug) {
    Main.logger().debug("PB Entity", TextFormat.printer().shortDebugString(protoEntity))
  }
  return futureEntity { future ->
    // Required components
    EntityTypeComponent.load(this, protoEntity)
    IdComponent.load(this, protoEntity)
    PositionComponent.load(this, protoEntity)
    WorldComponent.load(this, protoEntity) { world }

    ExplosiveComponent.load(this, protoEntity)
    InventoryComponent.load(this, protoEntity)
    KillableComponent.load(this, protoEntity)
    LookDirectionComponent.load(this, protoEntity)
    MaterialComponent.load(this, protoEntity)
    NameComponent.load(this, protoEntity)
    SelectedInventoryItemComponent.load(this, protoEntity)
    TextureRegionComponent.load(this, protoEntity)
    VelocityComponent.load(this, protoEntity)

    protoEntity.tagsOrNull?.let {
      FlyingTag.load(this, it)
      FollowedByCameraTag.load(this, it)
      GravityAffectedTag.load(this, it)
      IgnorePlaceableCheckTag.load(this, it)
      LeafDecayTag.load(this, it)
    }

    protoEntity.additionalComponents?.let {
      ChunkComponent.load(this, it) { chunk ?: throw IllegalStateException("Chunk component without chunk") }
      DoorComponent.load(this, it)
      GroundedComponent.load(this, it)
      LocallyControlledComponent.load(this, it) {
        val clientWorld = world as ClientWorld? ?: error("Not client world")
        KeyboardControls(clientWorld)
      }
      OccupyingBlocksComponent.load(this, it)
      PhysicsEventQueue.load(this, it)
      InputEventQueue.load(this, it)
    }

    // Load box2d body last, as it depends on other components
    if (protoEntity.box2DBodyComponentCheckShouldLoad()) {
      Box2DBodyComponent.load(this, protoEntity) { { future.complete(Unit) } }
    } else {
      future.complete(Unit)
    }
  }
}
