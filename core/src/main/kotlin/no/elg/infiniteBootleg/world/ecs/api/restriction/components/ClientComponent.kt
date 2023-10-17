package no.elg.infiniteBootleg.world.ecs.api.restriction.components

import com.badlogic.ashley.core.Component

/**
 * Client only component, usually because it uses graphics or input
 */
interface ClientComponent : Component
