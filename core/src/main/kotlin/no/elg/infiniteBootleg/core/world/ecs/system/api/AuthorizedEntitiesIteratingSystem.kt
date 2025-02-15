package no.elg.infiniteBootleg.core.world.ecs.system.api

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import no.elg.infiniteBootleg.core.main.Main

/**
 * Process only authorized entities in a family
 */
abstract class AuthorizedEntitiesIteratingSystem(family: Family, priority: Int) : ConditionalIteratingSystem(family, priority) {

  open fun additionalCondition(entity: Entity): Boolean = true
  final override fun condition(entity: Entity): Boolean = Main.inst().isAuthorizedToChange(entity) && additionalCondition(entity)
}
