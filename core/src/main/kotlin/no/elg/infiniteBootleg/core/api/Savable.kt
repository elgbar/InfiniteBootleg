package no.elg.infiniteBootleg.core.api

import com.google.protobuf.MessageOrBuilder

/**
 * @author Elg
 */
interface Savable<T : MessageOrBuilder?> {
  fun save(): T
}
