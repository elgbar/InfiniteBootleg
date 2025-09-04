package no.elg.infiniteBootleg.core.world.ecs

import com.badlogic.ashley.core.Entity
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.util.futureEntity
import no.elg.infiniteBootleg.core.util.singleLinePrinter
import no.elg.infiniteBootleg.core.world.chunks.Chunk
import no.elg.infiniteBootleg.core.world.ecs.api.SavableComponent
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.AuthoritativeOnlyComponent
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2dOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.checkShouldLoad
import no.elg.infiniteBootleg.core.world.ecs.components.DoorComponent
import no.elg.infiniteBootleg.core.world.ecs.components.DoorComponent.Companion.doorComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.ExplosiveComponent
import no.elg.infiniteBootleg.core.world.ecs.components.ExplosiveComponent.Companion.explosiveComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.GroundedComponent
import no.elg.infiniteBootleg.core.world.ecs.components.GroundedComponent.Companion.groundedComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.InputEventQueueComponent
import no.elg.infiniteBootleg.core.world.ecs.components.InputEventQueueComponent.Companion.inputEventQueueOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.KillableComponent
import no.elg.infiniteBootleg.core.world.ecs.components.KillableComponent.Companion.killableComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.LocallyControlledComponent
import no.elg.infiniteBootleg.core.world.ecs.components.LocallyControlledComponent.Companion.locallyControlledComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.LookDirectionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.LookDirectionComponent.Companion.lookDirectionComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.MaterialComponent
import no.elg.infiniteBootleg.core.world.ecs.components.MaterialComponent.Companion.materialComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.NameComponent
import no.elg.infiniteBootleg.core.world.ecs.components.NameComponent.Companion.nameComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.OccupyingBlocksComponent
import no.elg.infiniteBootleg.core.world.ecs.components.OccupyingBlocksComponent.Companion.occupyingBlocksComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.PhysicsEventQueueComponent
import no.elg.infiniteBootleg.core.world.ecs.components.PhysicsEventQueueComponent.Companion.physicsEventQueueOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.TextureRegionNameComponent
import no.elg.infiniteBootleg.core.world.ecs.components.TextureRegionNameComponent.Companion.textureRegionNameComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.TintedComponent
import no.elg.infiniteBootleg.core.world.ecs.components.TintedComponent.Companion.tintedComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.velocityComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.ContainerComponent
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.ContainerComponent.Companion.containerComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.HotbarComponent
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.HotbarComponent.Companion.hotbarComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.required.EntityTypeComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.EntityTypeComponent.Companion.entityTypeComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.IdComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.IdComponent.Companion.idComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.worldComponent
import no.elg.infiniteBootleg.core.world.ecs.components.tags.AuthoritativeOnlyTag
import no.elg.infiniteBootleg.core.world.ecs.components.tags.AuthoritativeOnlyTag.Companion.authoritativeOnly
import no.elg.infiniteBootleg.core.world.ecs.components.tags.AuthoritativeOnlyTag.Companion.authoritativeOnlyOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.tags.CanBeOutOfBoundsTag
import no.elg.infiniteBootleg.core.world.ecs.components.tags.CanBeOutOfBoundsTag.Companion.canBeOutOfBoundsComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.tags.FlyingTag
import no.elg.infiniteBootleg.core.world.ecs.components.tags.FlyingTag.Companion.flyingComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.tags.FollowedByCameraTag
import no.elg.infiniteBootleg.core.world.ecs.components.tags.FollowedByCameraTag.Companion.followedByCameraComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.tags.GravityAffectedTag
import no.elg.infiniteBootleg.core.world.ecs.components.tags.GravityAffectedTag.Companion.gravityAffectedComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.tags.IgnorePlaceableCheckTag
import no.elg.infiniteBootleg.core.world.ecs.components.tags.IgnorePlaceableCheckTag.Companion.ignorePlaceableCheckComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.tags.LeafDecayTag
import no.elg.infiniteBootleg.core.world.ecs.components.tags.LeafDecayTag.Companion.leafDecayComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.transients.tags.TransientEntityTag.Companion.isTransientEntity
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.protobuf.EntityKt.tags
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.entity
import no.elg.infiniteBootleg.protobuf.tagsOrNull
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

/**
 * Save an entity to a proto entity
 * @param toAuthoritative Whether the receiver of this entity is authoritative
 * @param ignoreTransient If true, will force save transient entities
 */
fun Entity.save(toAuthoritative: Boolean, ignoreTransient: Boolean = false): ProtoWorld.Entity? {
  if (!ignoreTransient && this.isTransientEntity || !toAuthoritative && this.authoritativeOnly) {
    return null
  }

  fun <DSL, T : SavableComponent<DSL>> DSL.trySave(component: T?) {
    if (toAuthoritative || component !is AuthoritativeOnlyComponent) {
      component?.apply { save() }
    }
  }

  return entity {
    // required
    trySave(this@save.entityTypeComponent)
    trySave(this@save.idComponent)
    trySave(this@save.positionComponent)
    trySave(this@save.worldComponent)

    tags = tags {
      trySave(this@save.authoritativeOnlyOrNull)
      trySave(this@save.canBeOutOfBoundsComponentOrNull)
      trySave(this@save.flyingComponentOrNull)
      trySave(this@save.followedByCameraComponentOrNull)
      trySave(this@save.gravityAffectedComponentOrNull)
      trySave(this@save.ignorePlaceableCheckComponentOrNull)
      trySave(this@save.leafDecayComponentOrNull)
    }

    // inventory
    trySave(this@save.containerComponentOrNull)
    trySave(this@save.hotbarComponentOrNull)

    trySave(this@save.box2dOrNull)
    trySave(this@save.doorComponentOrNull)
    trySave(this@save.explosiveComponentOrNull)
    trySave(this@save.groundedComponentOrNull)
    trySave(this@save.physicsEventQueueOrNull)
    trySave(this@save.inputEventQueueOrNull)
    trySave(this@save.killableComponentOrNull)
    trySave(this@save.locallyControlledComponentOrNull)
    trySave(this@save.lookDirectionComponentOrNull)
    trySave(this@save.materialComponentOrNull)
    trySave(this@save.nameComponentOrNull)
    trySave(this@save.occupyingBlocksComponentOrNull)
    trySave(this@save.textureRegionNameComponentOrNull)
    trySave(this@save.tintedComponentOrNull)
    trySave(this@save.velocityComponentOrNull)
  }
}

/**
 * Load an entity from a proto entity
 *
 * @param protoEntity The proto entity to load from
 * @param chunk The chunk this entity is in, if any, will be ignored if the `protoEntity` does not have a chunk field
 * @param configure A function to configure the entity after it has been loaded, but before it's added to the engine. Useful to add transient components
 */
fun World.load(protoEntity: ProtoWorld.Entity, chunk: Chunk? = null, configure: Entity.() -> Unit = {}): CompletableFuture<Entity> {
  require(chunk == null || this === chunk.world) { "Chunk world does not match entity world" }
  if (Settings.debug && Settings.logPersistence) {
    logger.debug { singleLinePrinter.printToString(protoEntity) }
  }
  val world = this
  return engine.futureEntity { future ->
    // Required components
    EntityTypeComponent.load(this, protoEntity)
    IdComponent.load(this, protoEntity)
    PositionComponent.load(this, protoEntity)
    WorldComponent.load(this, protoEntity) { world }

    protoEntity.tagsOrNull?.let {
      AuthoritativeOnlyTag.load(this, it)
      CanBeOutOfBoundsTag.load(this, it)
      FlyingTag.load(this, it)
      FollowedByCameraTag.load(this, it)
      GravityAffectedTag.load(this, it)
      IgnorePlaceableCheckTag.load(this, it)
      LeafDecayTag.load(this, it)
    }

    // inventory
    ContainerComponent.load(this, protoEntity)
    HotbarComponent.load(this, protoEntity)

    DoorComponent.load(this, protoEntity)
    ExplosiveComponent.load(this, protoEntity)
    GroundedComponent.load(this, protoEntity)
    InputEventQueueComponent.load(this, protoEntity)
    PhysicsEventQueueComponent.load(this, protoEntity)
    KillableComponent.load(this, protoEntity)
    LocallyControlledComponent.load(this, protoEntity)
    LookDirectionComponent.load(this, protoEntity)
    MaterialComponent.load(this, protoEntity)
    NameComponent.load(this, protoEntity)
    OccupyingBlocksComponent.load(this, protoEntity)
    TextureRegionNameComponent.load(this, protoEntity)
    TintedComponent.load(this, protoEntity)
    VelocityComponent.load(this, protoEntity)

    // Load box2d body last, as it depends on other components
    val futureCompleter: () -> (Entity) -> Unit = {
      {
        configure(it)
        future.complete(Unit)
      }
    }

    if (protoEntity.checkShouldLoad(futureCompleter)) {
      Box2DBodyComponent.load(this, protoEntity, futureCompleter)
    } else {
      futureCompleter()(entity)
    }
  }
}
