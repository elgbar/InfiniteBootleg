package no.elg.infiniteBootleg.net

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ScheduledFuture

/**
 *
 * Shared information between the server and client
 *
 * @author Elg
 */
data class SharedInformation(val entityId: String, val secret: String) {

  private var lastHeartbeat: Instant = Instant.now()

  val requestedEntities: Cache<String, Unit> by lazy {
    Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofSeconds(5))
      .build()
  }

  var heartbeatTask: ScheduledFuture<*>? = null
    set(value) {
      field?.cancel(false)
      field = value
    }

  /**
   * @return if we have lost connection
   */
  fun lostConnection(): Boolean {
    val now = Instant.now()
    val latest = now.minusMillis(HEARTBEAT_PERIOD_MS * MISSED_HEARTBEAT_LOST_CONNECTION)
//    logger
//      .debug("Heartbeat", "Time since last beat " + Duration.between(lastHeartbeat, now) + " seconds. Should disconnect? " + lastHeartbeat.isBefore(latest))
    // Uh-oh we didn't receive a heartbeat in quite some time
    return lastHeartbeat.isBefore(latest)
  }

  fun beat() {
    lastHeartbeat = Instant.now()
  }

  companion object {
    /**
     * How often, in milliseconds, a heartbeat pulse should be sent
     */
    const val HEARTBEAT_PERIOD_MS = 1000L

    /**
     * How many heartbeats should be missed before the connection should be considered lost
     */
    const val MISSED_HEARTBEAT_LOST_CONNECTION = 10L
  }
}
