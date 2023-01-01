package no.elg.infiniteBootleg.util

class DebugStackTraceException : Exception()

fun stacktrace(): String {
  return DebugStackTraceException().stackTraceToString()
}
