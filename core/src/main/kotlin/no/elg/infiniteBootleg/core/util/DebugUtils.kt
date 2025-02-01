package no.elg.infiniteBootleg.core.util

class DebugStackTraceException : Exception()

fun stacktrace(): String = DebugStackTraceException().stackTraceToString()
