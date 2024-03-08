package no.elg.infiniteBootleg.util

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.utils.Pool
import com.badlogic.gdx.utils.Pools

inline fun <reified T : Pool.Poolable> obtainFromPool(): T = Pools.obtain(T::class.java)
inline fun <reified T : Pool.Poolable> obtainAndUse(action: (T) -> Unit) {
  val poolable = obtainFromPool<T>()
  try {
    action(poolable)
  } finally {
    Pools.free(poolable)
  }
}

inline fun <reified T : Event> Actor.fireAndForget() = obtainAndUse<T> { this.fire(it) }