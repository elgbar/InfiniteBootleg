package no.elg.infiniteBootleg.world.ecs.system.restriction

/**
 * Shortcut to mark a system as both client and server side system
 */
interface DuplexSystem : ClientSystem, ServerSystem
