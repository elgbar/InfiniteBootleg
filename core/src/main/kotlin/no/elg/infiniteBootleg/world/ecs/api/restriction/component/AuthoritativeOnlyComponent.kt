package no.elg.infiniteBootleg.world.ecs.api.restriction.component

import com.badlogic.ashley.core.Component

/**
 * Mark a component as authoritative, meaning that it should not be sent to client which are not authoritative over an entity.
 *
 * For example in multiplayer a client is authoritative over the entity they control, but not over other entities.
 * Other players should not be able to see for example the inventory of another player.
 */
interface AuthoritativeOnlyComponent : Component
