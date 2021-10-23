package no.elg.infiniteBootleg.server

import java.util.UUID

/**
 *
 * A bad JWT
 *
 * @author Elg
 */
data class ConnectionCredentials(val entityUUID: UUID, val secret: String)
