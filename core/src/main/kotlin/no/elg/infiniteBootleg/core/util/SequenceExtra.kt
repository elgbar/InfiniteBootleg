package no.elg.infiniteBootleg.core.util

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet

inline fun <T, R1, R2> Sequence<T>.partitionMap(predicate: (T) -> Boolean, mapFirst: (T) -> R1, mapSecond: (T) -> R2): Pair<List<R1>, List<R2>> {
  val first = ArrayList<R1>()
  val second = ArrayList<R2>()
  for (element in this) {
    if (predicate(element)) {
      first.add(mapFirst(element))
    } else {
      second.add(mapSecond(element))
    }
  }
  return Pair(first, second)
}

inline fun <T, R> Sequence<T>.partitionMap(predicate: (T) -> Boolean, map: (T) -> R): Pair<List<R>, List<R>> = partitionMap<T, R, R>(predicate, map, map)

@JvmName("partitionMapLong")
inline fun <T> Sequence<T>.partitionMap(predicate: (T) -> Boolean, map: (T) -> Long): Pair<LongSet, LongSet> {
  val first = LongOpenHashSet()
  val second = LongOpenHashSet()
  for (element in this) {
    if (predicate(element)) {
      first.add(map(element))
    } else {
      second.add(map(element))
    }
  }
  return Pair(first, second)
}
