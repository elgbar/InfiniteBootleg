package no.elg.infiniteBootleg.util

class DebugStackTraceException : Exception()

fun stacktrace(): String = DebugStackTraceException().stackTraceToString()
