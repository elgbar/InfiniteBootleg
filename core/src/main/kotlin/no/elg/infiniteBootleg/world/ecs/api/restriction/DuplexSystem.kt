package no.elg.infiniteBootleg.world.ecs.api.restriction

/**
 * Shortcut to mark a system as both client and server side system
 */
interface DuplexSystem : ClientSystem, ServerSystem
