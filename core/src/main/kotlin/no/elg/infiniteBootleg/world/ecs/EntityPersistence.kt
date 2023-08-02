package no.elg.infiniteBootleg.world.ecs

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import ktx.ashley.entity
import no.elg.infiniteBootleg.protobuf.EntityKt.player
import no.elg.infiniteBootleg.protobuf.EntityKt.tags
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.entity
import no.elg.infiniteBootleg.protobuf.tagsOrNull
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.ecs.components.EntityTypeComponent
import no.elg.infiniteBootleg.world.ecs.components.EntityTypeComponent.Companion.entityTypeComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.ExplosiveComponent
import no.elg.infiniteBootleg.world.ecs.components.ExplosiveComponent.Companion.explosiveComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.KillableComponent
import no.elg.infiniteBootleg.world.ecs.components.KillableComponent.Companion.killableOrNull
import no.elg.infiniteBootleg.world.ecs.components.LocallyControlledComponent.Companion.locallyControlledOrNull
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent.Companion.materialComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.NamedComponent
import no.elg.infiniteBootleg.world.ecs.components.NamedComponent.Companion.nameComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.velocityOrNull
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.idComponent
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.tags.FlyingTag
import no.elg.infiniteBootleg.world.ecs.components.tags.FlyingTag.Companion.flying
import no.elg.infiniteBootleg.world.ecs.components.tags.FlyingTag.Companion.flyingComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.tags.FollowedByCameraTag
import no.elg.infiniteBootleg.world.ecs.components.tags.FollowedByCameraTag.Companion.followedByCameraComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.tags.GravityAffectedTag
import no.elg.infiniteBootleg.world.ecs.components.tags.GravityAffectedTag.Companion.gravityAffectedComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.tags.IgnorePlaceableCheckTag
import no.elg.infiniteBootleg.world.ecs.components.tags.IgnorePlaceableCheckTag.Companion.ignorePlaceableCheckComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.transients.ChunkComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.SharedInformationComponent.Companion.sharedInformationOrNull
import no.elg.infiniteBootleg.world.ecs.components.transients.WorldComponent
import no.elg.infiniteBootleg.world.world.World

fun Entity.save(forceControlled: Boolean = false): ProtoWorld.Entity {
  return entity {
    flying = this@save.flying

    this@save.positionComponent.apply { save() }
    this@save.idComponent.apply { save() }
    this@save.nameComponentOrNull?.apply { save() }

    this@save.killableOrNull?.apply { save() }
    this@save.velocityOrNull?.apply { save() }
    this@save.materialComponentOrNull?.apply { save() }
    this@save.explosiveComponentOrNull?.apply { save() }
    this@save.entityTypeComponentOrNull?.apply { save() }

    tags = tags {
      this@save.flyingComponentOrNull?.apply { save() }
      this@save.followedByCameraComponentOrNull?.apply { save() }
      this@save.gravityAffectedComponentOrNull?.apply { save() }
      this@save.ignorePlaceableCheckComponentOrNull?.apply { save() }
    }

    val isLocallyControlled = this@save.locallyControlledOrNull != null
    val isPlayer = this@save.sharedInformationOrNull != null || isLocallyControlled
    if (isPlayer) {
      player = player {
        controlled = forceControlled || isLocallyControlled
      }
    }
  }
}

fun Engine.load(protoEntity: ProtoWorld.Entity, world: World, chunk: Chunk? = null): Entity = entity {
  with(WorldComponent(world))
  chunk?.run {
    with(ChunkComponent(chunk))
  }
  EntityTypeComponent.load(this, protoEntity)
  PositionComponent.load(this, protoEntity)
  IdComponent.load(this, protoEntity)

  NamedComponent.load(this, protoEntity)
  KillableComponent.load(this, protoEntity)
  VelocityComponent.load(this, protoEntity)
  MaterialComponent.load(this, protoEntity)
  ExplosiveComponent.load(this, protoEntity)

  protoEntity.tagsOrNull?.let {
    FlyingTag.load(this, it)
    FollowedByCameraTag.load(this, it)
    GravityAffectedTag.load(this, it)
    IgnorePlaceableCheckTag.load(this, it)
  }
}
