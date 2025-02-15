package no.elg.infiniteBootleg.core.console

import io.github.oshai.kotlinlogging.Marker
import io.github.oshai.kotlinlogging.slf4j.toKotlinLogging
import org.slf4j.MarkerFactory

val clientSideServerBoundMarker: Marker = MarkerFactory.getMarker("client->server").toKotlinLogging()
val clientSideClientBoundMarker: Marker = MarkerFactory.getMarker("client<-server").toKotlinLogging()
