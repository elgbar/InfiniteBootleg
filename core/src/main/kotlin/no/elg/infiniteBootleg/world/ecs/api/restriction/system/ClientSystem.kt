package no.elg.infiniteBootleg.world.ecs.api.restriction.system

/**
 * Mark a system as client-side only so that it is not added when running as a server
 *
 * A client system handles graphics, input and other client side only logic
 */
interface ClientSystem
