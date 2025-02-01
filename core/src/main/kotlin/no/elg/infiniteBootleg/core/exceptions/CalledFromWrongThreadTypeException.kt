package no.elg.infiniteBootleg.core.exceptions

/**
 * Indicates a method was called from the wrong thread type
 */
class CalledFromWrongThreadTypeException(message: String) : RuntimeException(message)
