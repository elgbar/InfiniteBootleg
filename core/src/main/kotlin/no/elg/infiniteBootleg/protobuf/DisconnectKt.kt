// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: serialization/packets.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package no.elg.infiniteBootleg.protobuf;

@kotlin.jvm.JvmName("-initializedisconnect")
public inline fun disconnect(block: no.elg.infiniteBootleg.protobuf.DisconnectKt.Dsl.() -> kotlin.Unit): no.elg.infiniteBootleg.protobuf.Packets.Disconnect =
  no.elg.infiniteBootleg.protobuf.DisconnectKt.Dsl._create(no.elg.infiniteBootleg.protobuf.Packets.Disconnect.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `packets.Disconnect`
 */
public object DisconnectKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: no.elg.infiniteBootleg.protobuf.Packets.Disconnect.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: no.elg.infiniteBootleg.protobuf.Packets.Disconnect.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): no.elg.infiniteBootleg.protobuf.Packets.Disconnect = _builder.build()

    /**
     * `string reason = 1;`
     */
    public var reason: kotlin.String
      @JvmName("getReason")
      get() = _builder.getReason()
      @JvmName("setReason")
      set(value) {
        _builder.setReason(value)
      }
    /**
     * `string reason = 1;`
     */
    public fun clearReason() {
      _builder.clearReason()
    }
  }
}
@kotlin.jvm.JvmSynthetic
@com.google.errorprone.annotations.CheckReturnValue
public inline fun no.elg.infiniteBootleg.protobuf.Packets.Disconnect.copy(block: no.elg.infiniteBootleg.protobuf.DisconnectKt.Dsl.() -> kotlin.Unit): no.elg.infiniteBootleg.protobuf.Packets.Disconnect =
  no.elg.infiniteBootleg.protobuf.DisconnectKt.Dsl._create(this.toBuilder()).apply { block() }._build()

