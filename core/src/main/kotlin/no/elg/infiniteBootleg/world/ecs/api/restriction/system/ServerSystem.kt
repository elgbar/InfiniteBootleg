package no.elg.infiniteBootleg.world.ecs.api.restriction.system

/**
 * A server system handles logic that should only run when the instance is the server instance.
 *
 * This is not the same as a [AuthoritativeSystem], as this is intended to be used for network og server side only logic.
 */
interface ServerSystem
