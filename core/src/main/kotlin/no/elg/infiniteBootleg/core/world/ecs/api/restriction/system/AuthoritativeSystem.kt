package no.elg.infiniteBootleg.core.world.ecs.api.restriction.system

/**
 * An authoritative system handles logic that should only run when the instance is the authoritative instance.
 *
 * Since both a client and server can be authoritative, this system can be used on both the client and server, thus is universal.
 */
interface AuthoritativeSystem
