package no.elg.infiniteBootleg.world.ecs

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.protobuf.EntityKt.living
import no.elg.infiniteBootleg.protobuf.EntityKt.material
import no.elg.infiniteBootleg.protobuf.EntityKt.player
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.EntityType
import no.elg.infiniteBootleg.protobuf.entity
import no.elg.infiniteBootleg.protobuf.vector2f
import no.elg.infiniteBootleg.world.ecs.components.KillableComponent.Companion.killableOrNull
import no.elg.infiniteBootleg.world.ecs.components.LocallyControlledComponent.Companion.locallyControlledOrNull
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent.Companion.materialOrNull
import no.elg.infiniteBootleg.world.ecs.components.NamedComponent.Companion.nameComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.SharedInformationComponent.Companion.sharedInformationOrNull
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent.Companion.velocityOrNull
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.position
import no.elg.infiniteBootleg.world.ecs.components.tags.FlyingTag.Companion.flying

fun Entity.save(forceControlled: Boolean = false): ProtoWorld.Entity {
  return entity {
    uuid = this@save.id
    flying = this@save.flying

    this@save.nameComponentOrNull?.also {
      name = it.name
    }

    position = vector2f {
      val position = this@save.position
      x = position.x
      y = position.y
    }

    val velocityOrNull = this@save.velocityOrNull
    velocityOrNull?.also {
      velocity = vector2f {
        x = it.dx
        y = it.dy
      }
    }

    this@save.killableOrNull?.also {
      living = living {
        health = it.health
        maxHealth = it.maxHealth
      }
    }

    val isLocallyControlled = this@save.locallyControlledOrNull != null
    val isPlayer = this@save.sharedInformationOrNull != null || isLocallyControlled
    if (isPlayer) {
      player = player {
        controlled = forceControlled || isLocallyControlled
      }
    }

    val materialOrNull = this@save.materialOrNull
    materialOrNull?.also {
      material = material {
        materialOrdinal = it.material.ordinal
      }
    }

    type = when {
      isPlayer -> EntityType.PLAYER
      materialOrNull != null && velocityOrNull != null -> EntityType.FALLING_BLOCK
      materialOrNull != null && velocityOrNull == null -> EntityType.BLOCK
      else -> EntityType.GENERIC_ENTITY
    }
  }
}
