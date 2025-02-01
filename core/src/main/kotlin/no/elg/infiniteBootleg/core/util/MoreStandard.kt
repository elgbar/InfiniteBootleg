package no.elg.infiniteBootleg.core.util

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Calls the specified function [block] with `this` value as its receiver and returns `this` value.
 *
 * [block] will only be called if [condition] is `true`.
 */
inline fun <T> T.applyIf(condition: Boolean, block: T.() -> Unit): T {
  contract {
    callsInPlace(block, InvocationKind.AT_MOST_ONCE)
  }
  if (condition) {
    block()
  }
  return this
}
