package no.elg.infiniteBootleg.core.console

import io.github.oshai.kotlinlogging.Marker
import io.github.oshai.kotlinlogging.slf4j.toKotlinLogging
import org.slf4j.MarkerFactory

val serverSideClientBoundMarker: Marker = MarkerFactory.getMarker("server->client").toKotlinLogging()
val serverSideServerBoundMarker: Marker = MarkerFactory.getMarker("server<-client").toKotlinLogging()
