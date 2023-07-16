package no.elg.infiniteBootleg.world.ecs

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.protobuf.EntityKt.player
import no.elg.infiniteBootleg.protobuf.EntityKt.tags
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.entity
import no.elg.infiniteBootleg.world.ecs.components.EntityTypeComponent.Companion.entityTypeComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.ExplosiveComponent.Companion.explosiveComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.KillableComponent.Companion.killableOrNull
import no.elg.infiniteBootleg.world.ecs.components.LocallyControlledComponent.Companion.locallyControlledOrNull
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent.Companion.materialComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.NamedComponent.Companion.nameComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.velocityOrNull
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.idComponent
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.tags.FlyingTag.Companion.flying
import no.elg.infiniteBootleg.world.ecs.components.tags.FlyingTag.Companion.flyingComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.tags.FollowedByCameraTag.Companion.followedByCameraComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.tags.GravityAffectedTag.Companion.gravityAffectedComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.tags.IgnorePlaceableCheckTag.Companion.ignorePlaceableCheckComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.transients.SharedInformationComponent.Companion.sharedInformationOrNull

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
