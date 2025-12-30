package no.elg.infiniteBootleg.core.world.ecs.system.magic

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.Vector2
import ktx.ashley.remove
import no.elg.infiniteBootleg.core.world.Constants
import no.elg.infiniteBootleg.core.world.box2d.extensions.gravityScale
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.system.AuthoritativeSystem
import no.elg.infiniteBootleg.core.world.ecs.components.Box2DBodyComponent.Companion.box2dBody
import no.elg.infiniteBootleg.core.world.ecs.components.VelocityComponent.Companion.velocityComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.core.world.ecs.components.transients.SpellStateComponent
import no.elg.infiniteBootleg.core.world.ecs.components.transients.SpellStateComponent.Companion.spellStateComponent
import no.elg.infiniteBootleg.core.world.ecs.spellEntityFamily
import no.elg.infiniteBootleg.protobuf.Packets

object SpellRemovalSystem : IteratingSystem(spellEntityFamily, UPDATE_PRIORITY_DEFAULT), AuthoritativeSystem {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val spellStateComponent = entity.spellStateComponent
    val maxTravelDistance = spellStateComponent.state.spellRange
    val (currWorldX, currWorldY) = entity.positionComponent

    val distanceTravelled = Vector2.dst2(spellStateComponent.spawnX.toFloat(), spellStateComponent.spawnY.toFloat(), currWorldX, currWorldY)
    fun willRemainStationary(): Boolean = entity.velocityComponent.isStill() && entity.box2dBody.gravityScale < Constants.DEFAULT_GRAVITY_SCALE / 2f
    val haveTravelledMaxDistance = distanceTravelled > maxTravelDistance * maxTravelDistance
    if (haveTravelledMaxDistance || willRemainStationary()) {
      entity.world.removeEntity(entity, Packets.DespawnEntity.DespawnReason.NATURAL)
      entity.remove<SpellStateComponent>()
    }
  }
}
