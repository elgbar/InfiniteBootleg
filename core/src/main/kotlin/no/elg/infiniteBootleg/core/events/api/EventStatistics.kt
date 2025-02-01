package no.elg.infiniteBootleg.core.events.api

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

object EventStatistics {

  private val eventReasons: MutableMap<KClass<out Event>, MutableMap<String, EventReason>> = ConcurrentHashMap()

  fun clear() {
    eventReasons.clear()
  }

  fun createRapport(): String {
    val sb = StringBuilder()
    eventReasons.values.flatMap { it.values }.sortedByDescending { it.count.get() }.forEach {
      sb.appendLine(it)
    }
    return sb.toString()
  }

  fun reportDispatch(event: Event, reason: String?) {
    val reason = reason ?: (event as? ReasonedEvent)?.reason ?: event.toString()
    val eventReason = eventReasons.getOrPut(event::class) { ConcurrentHashMap() }.getOrPut(reason) { EventReason(event::class, reason) }
    eventReason.increment()
  }

  class EventReason(val type: KClass<out Event>, val reason: String) {
    var count: AtomicInteger = AtomicInteger(0)

    fun increment() {
      count.incrementAndGet()
    }

    override fun toString(): String = "@|green,bold ${count.get()}x|@ of @|yellow ${type.simpleName}|@: $reason"
  }
}
