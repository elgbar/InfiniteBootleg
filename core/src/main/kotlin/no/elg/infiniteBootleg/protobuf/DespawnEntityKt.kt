// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: serialization/packets.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package no.elg.infiniteBootleg.protobuf;

@kotlin.jvm.JvmName("-initializedespawnEntity")
public inline fun despawnEntity(block: no.elg.infiniteBootleg.protobuf.DespawnEntityKt.Dsl.() -> kotlin.Unit): no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity =
  no.elg.infiniteBootleg.protobuf.DespawnEntityKt.Dsl._create(no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `packets.DespawnEntity`
 */
public object DespawnEntityKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity = _builder.build()

    /**
     * `string uuid = 1;`
     */
    public var uuid: kotlin.String
      @JvmName("getUuid")
      get() = _builder.getUuid()
      @JvmName("setUuid")
      set(value) {
        _builder.setUuid(value)
      }
    /**
     * `string uuid = 1;`
     */
    public fun clearUuid() {
      _builder.clearUuid()
    }

    /**
     * `.packets.DespawnEntity.DespawnReason despawnReason = 2;`
     */
    public var despawnReason: no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.DespawnReason
      @JvmName("getDespawnReason")
      get() = _builder.getDespawnReason()
      @JvmName("setDespawnReason")
      set(value) {
        _builder.setDespawnReason(value)
      }
    public var despawnReasonValue: kotlin.Int
      @JvmName("getDespawnReasonValue")
      get() = _builder.getDespawnReasonValue()
      @JvmName("setDespawnReasonValue")
      set(value) {
        _builder.setDespawnReasonValue(value)
      }
    /**
     * `.packets.DespawnEntity.DespawnReason despawnReason = 2;`
     */
    public fun clearDespawnReason() {
      _builder.clearDespawnReason()
    }
  }
}
@kotlin.jvm.JvmSynthetic
public inline fun no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity.copy(block: no.elg.infiniteBootleg.protobuf.DespawnEntityKt.Dsl.() -> kotlin.Unit): no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity =
  no.elg.infiniteBootleg.protobuf.DespawnEntityKt.Dsl._create(this.toBuilder()).apply { block() }._build()

