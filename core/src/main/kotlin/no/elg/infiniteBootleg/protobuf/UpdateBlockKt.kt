// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: serialization/packets.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package no.elg.infiniteBootleg.protobuf;

@kotlin.jvm.JvmName("-initializeupdateBlock")
public inline fun updateBlock(block: no.elg.infiniteBootleg.protobuf.UpdateBlockKt.Dsl.() -> kotlin.Unit): no.elg.infiniteBootleg.protobuf.Packets.UpdateBlock =
  no.elg.infiniteBootleg.protobuf.UpdateBlockKt.Dsl._create(no.elg.infiniteBootleg.protobuf.Packets.UpdateBlock.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `packets.UpdateBlock`
 */
public object UpdateBlockKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: no.elg.infiniteBootleg.protobuf.Packets.UpdateBlock.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: no.elg.infiniteBootleg.protobuf.Packets.UpdateBlock.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): no.elg.infiniteBootleg.protobuf.Packets.UpdateBlock = _builder.build()

    /**
     * `.persistence.Vector2i pos = 1;`
     */
    public var pos: no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2i
      @JvmName("getPos")
      get() = _builder.getPos()
      @JvmName("setPos")
      set(value) {
        _builder.setPos(value)
      }
    /**
     * `.persistence.Vector2i pos = 1;`
     */
    public fun clearPos() {
      _builder.clearPos()
    }
    /**
     * `.persistence.Vector2i pos = 1;`
     * @return Whether the pos field is set.
     */
    public fun hasPos(): kotlin.Boolean {
      return _builder.hasPos()
    }

    /**
     * `optional .persistence.Block block = 2;`
     */
    public var block: no.elg.infiniteBootleg.protobuf.ProtoWorld.Block
      @JvmName("getBlock")
      get() = _builder.getBlock()
      @JvmName("setBlock")
      set(value) {
        _builder.setBlock(value)
      }
    /**
     * `optional .persistence.Block block = 2;`
     */
    public fun clearBlock() {
      _builder.clearBlock()
    }
    /**
     * `optional .persistence.Block block = 2;`
     * @return Whether the block field is set.
     */
    public fun hasBlock(): kotlin.Boolean {
      return _builder.hasBlock()
    }
    public val UpdateBlockKt.Dsl.blockOrNull: no.elg.infiniteBootleg.protobuf.ProtoWorld.Block?
      get() = _builder.blockOrNull
  }
}
@kotlin.jvm.JvmSynthetic
public inline fun no.elg.infiniteBootleg.protobuf.Packets.UpdateBlock.copy(block: no.elg.infiniteBootleg.protobuf.UpdateBlockKt.Dsl.() -> kotlin.Unit): no.elg.infiniteBootleg.protobuf.Packets.UpdateBlock =
  no.elg.infiniteBootleg.protobuf.UpdateBlockKt.Dsl._create(this.toBuilder()).apply { block() }._build()

public val no.elg.infiniteBootleg.protobuf.Packets.UpdateBlockOrBuilder.posOrNull: no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2i?
  get() = if (hasPos()) getPos() else null

public val no.elg.infiniteBootleg.protobuf.Packets.UpdateBlockOrBuilder.blockOrNull: no.elg.infiniteBootleg.protobuf.ProtoWorld.Block?
  get() = if (hasBlock()) getBlock() else null

