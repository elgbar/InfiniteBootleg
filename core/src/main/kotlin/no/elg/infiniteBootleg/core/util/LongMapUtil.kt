package no.elg.infiniteBootleg.core.util

import com.badlogic.gdx.utils.LongMap

object LongMapUtil {
  operator fun LongMap.Entry<*>.component1(): Long = key
  operator fun <V> LongMap.Entry<V>.component2(): V = value
}
