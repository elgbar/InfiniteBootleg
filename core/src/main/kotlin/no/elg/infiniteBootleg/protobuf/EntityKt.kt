// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: serialization/persistence.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package no.elg.infiniteBootleg.protobuf;

@kotlin.jvm.JvmName("-initializeentity")
public inline fun entity(block: no.elg.infiniteBootleg.protobuf.EntityKt.Dsl.() -> kotlin.Unit): no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity =
  no.elg.infiniteBootleg.protobuf.EntityKt.Dsl._create(no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `persistence.Entity`
 */
public object EntityKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity = _builder.build()

    /**
     * `.persistence.Entity.EntityType type = 1;`
     */
    public var type: no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.EntityType
      @JvmName("getType")
      get() = _builder.getType()
      @JvmName("setType")
      set(value) {
        _builder.setType(value)
      }
    public var typeValue: kotlin.Int
      @JvmName("getTypeValue")
      get() = _builder.getTypeValue()
      @JvmName("setTypeValue")
      set(value) {
        _builder.setTypeValue(value)
      }
    /**
     * `.persistence.Entity.EntityType type = 1;`
     */
    public fun clearType() {
      _builder.clearType()
    }

    /**
     * `.persistence.Vector2f position = 2;`
     */
    public var position: no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2f
      @JvmName("getPosition")
      get() = _builder.getPosition()
      @JvmName("setPosition")
      set(value) {
        _builder.setPosition(value)
      }
    /**
     * `.persistence.Vector2f position = 2;`
     */
    public fun clearPosition() {
      _builder.clearPosition()
    }
    /**
     * `.persistence.Vector2f position = 2;`
     * @return Whether the position field is set.
     */
    public fun hasPosition(): kotlin.Boolean {
      return _builder.hasPosition()
    }

    /**
     * `.persistence.Vector2f velocity = 3;`
     */
    public var velocity: no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2f
      @JvmName("getVelocity")
      get() = _builder.getVelocity()
      @JvmName("setVelocity")
      set(value) {
        _builder.setVelocity(value)
      }
    /**
     * `.persistence.Vector2f velocity = 3;`
     */
    public fun clearVelocity() {
      _builder.clearVelocity()
    }
    /**
     * `.persistence.Vector2f velocity = 3;`
     * @return Whether the velocity field is set.
     */
    public fun hasVelocity(): kotlin.Boolean {
      return _builder.hasVelocity()
    }

    /**
     * `string uuid = 4;`
     */
    public var uuid: kotlin.String
      @JvmName("getUuid")
      get() = _builder.getUuid()
      @JvmName("setUuid")
      set(value) {
        _builder.setUuid(value)
      }
    /**
     * `string uuid = 4;`
     */
    public fun clearUuid() {
      _builder.clearUuid()
    }

    /**
     * `bool flying = 5;`
     */
    public var flying: kotlin.Boolean
      @JvmName("getFlying")
      get() = _builder.getFlying()
      @JvmName("setFlying")
      set(value) {
        _builder.setFlying(value)
      }
    /**
     * `bool flying = 5;`
     */
    public fun clearFlying() {
      _builder.clearFlying()
    }

    /**
     * `optional string name = 6;`
     */
    public var name: kotlin.String
      @JvmName("getName")
      get() = _builder.getName()
      @JvmName("setName")
      set(value) {
        _builder.setName(value)
      }
    /**
     * `optional string name = 6;`
     */
    public fun clearName() {
      _builder.clearName()
    }
    /**
     * `optional string name = 6;`
     * @return Whether the name field is set.
     */
    public fun hasName(): kotlin.Boolean {
      return _builder.hasName()
    }

    /**
     * `optional .persistence.Entity.Living living = 16;`
     */
    public var living: no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Living
      @JvmName("getLiving")
      get() = _builder.getLiving()
      @JvmName("setLiving")
      set(value) {
        _builder.setLiving(value)
      }
    /**
     * `optional .persistence.Entity.Living living = 16;`
     */
    public fun clearLiving() {
      _builder.clearLiving()
    }
    /**
     * `optional .persistence.Entity.Living living = 16;`
     * @return Whether the living field is set.
     */
    public fun hasLiving(): kotlin.Boolean {
      return _builder.hasLiving()
    }
    public val EntityKt.Dsl.livingOrNull: no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Living?
      get() = _builder.livingOrNull

    /**
     * `optional .persistence.Entity.Material material = 17;`
     */
    public var material: no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Material
      @JvmName("getMaterial")
      get() = _builder.getMaterial()
      @JvmName("setMaterial")
      set(value) {
        _builder.setMaterial(value)
      }
    /**
     * `optional .persistence.Entity.Material material = 17;`
     */
    public fun clearMaterial() {
      _builder.clearMaterial()
    }
    /**
     * `optional .persistence.Entity.Material material = 17;`
     * @return Whether the material field is set.
     */
    public fun hasMaterial(): kotlin.Boolean {
      return _builder.hasMaterial()
    }
    public val EntityKt.Dsl.materialOrNull: no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Material?
      get() = _builder.materialOrNull

    /**
     * `optional .persistence.Entity.Player player = 18;`
     */
    public var player: no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Player
      @JvmName("getPlayer")
      get() = _builder.getPlayer()
      @JvmName("setPlayer")
      set(value) {
        _builder.setPlayer(value)
      }
    /**
     * `optional .persistence.Entity.Player player = 18;`
     */
    public fun clearPlayer() {
      _builder.clearPlayer()
    }
    /**
     * `optional .persistence.Entity.Player player = 18;`
     * @return Whether the player field is set.
     */
    public fun hasPlayer(): kotlin.Boolean {
      return _builder.hasPlayer()
    }
    public val EntityKt.Dsl.playerOrNull: no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Player?
      get() = _builder.playerOrNull
  }
  @kotlin.jvm.JvmName("-initializeliving")
  public inline fun living(block: no.elg.infiniteBootleg.protobuf.EntityKt.LivingKt.Dsl.() -> kotlin.Unit): no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Living =
    no.elg.infiniteBootleg.protobuf.EntityKt.LivingKt.Dsl._create(no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Living.newBuilder()).apply { block() }._build()
  /**
   * Protobuf type `persistence.Entity.Living`
   */
  public object LivingKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
      private val _builder: no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Living.Builder
    ) {
      public companion object {
        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _create(builder: no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Living.Builder): Dsl = Dsl(builder)
      }

      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _build(): no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Living = _builder.build()

      /**
       * `int32 health = 1;`
       */
      public var health: kotlin.Int
        @JvmName("getHealth")
        get() = _builder.getHealth()
        @JvmName("setHealth")
        set(value) {
          _builder.setHealth(value)
        }
      /**
       * `int32 health = 1;`
       */
      public fun clearHealth() {
        _builder.clearHealth()
      }

      /**
       * `int32 max_health = 2;`
       */
      public var maxHealth: kotlin.Int
        @JvmName("getMaxHealth")
        get() = _builder.getMaxHealth()
        @JvmName("setMaxHealth")
        set(value) {
          _builder.setMaxHealth(value)
        }
      /**
       * `int32 max_health = 2;`
       */
      public fun clearMaxHealth() {
        _builder.clearMaxHealth()
      }
    }
  }
  @kotlin.jvm.JvmName("-initializematerial")
  public inline fun material(block: no.elg.infiniteBootleg.protobuf.EntityKt.MaterialKt.Dsl.() -> kotlin.Unit): no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Material =
    no.elg.infiniteBootleg.protobuf.EntityKt.MaterialKt.Dsl._create(no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Material.newBuilder()).apply { block() }._build()
  /**
   * Protobuf type `persistence.Entity.Material`
   */
  public object MaterialKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
      private val _builder: no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Material.Builder
    ) {
      public companion object {
        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _create(builder: no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Material.Builder): Dsl = Dsl(builder)
      }

      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _build(): no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Material = _builder.build()

      /**
       * ```
       *ordinal from Material enum
       * ```
       *
       * `int32 material_ordinal = 1;`
       */
      public var materialOrdinal: kotlin.Int
        @JvmName("getMaterialOrdinal")
        get() = _builder.getMaterialOrdinal()
        @JvmName("setMaterialOrdinal")
        set(value) {
          _builder.setMaterialOrdinal(value)
        }
      /**
       * ```
       *ordinal from Material enum
       * ```
       *
       * `int32 material_ordinal = 1;`
       */
      public fun clearMaterialOrdinal() {
        _builder.clearMaterialOrdinal()
      }
    }
  }
  @kotlin.jvm.JvmName("-initializeplayer")
  public inline fun player(block: no.elg.infiniteBootleg.protobuf.EntityKt.PlayerKt.Dsl.() -> kotlin.Unit): no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Player =
    no.elg.infiniteBootleg.protobuf.EntityKt.PlayerKt.Dsl._create(no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Player.newBuilder()).apply { block() }._build()
  /**
   * Protobuf type `persistence.Entity.Player`
   */
  public object PlayerKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
      private val _builder: no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Player.Builder
    ) {
      public companion object {
        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _create(builder: no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Player.Builder): Dsl = Dsl(builder)
      }

      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _build(): no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Player = _builder.build()

      /**
       * `float torch_angle_deg = 1;`
       */
      public var torchAngleDeg: kotlin.Float
        @JvmName("getTorchAngleDeg")
        get() = _builder.getTorchAngleDeg()
        @JvmName("setTorchAngleDeg")
        set(value) {
          _builder.setTorchAngleDeg(value)
        }
      /**
       * `float torch_angle_deg = 1;`
       */
      public fun clearTorchAngleDeg() {
        _builder.clearTorchAngleDeg()
      }

      /**
       * `bool controlled = 2;`
       */
      public var controlled: kotlin.Boolean
        @JvmName("getControlled")
        get() = _builder.getControlled()
        @JvmName("setControlled")
        set(value) {
          _builder.setControlled(value)
        }
      /**
       * `bool controlled = 2;`
       */
      public fun clearControlled() {
        _builder.clearControlled()
      }
    }
  }
}
@kotlin.jvm.JvmSynthetic
@com.google.errorprone.annotations.CheckReturnValue
public inline fun no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.copy(block: no.elg.infiniteBootleg.protobuf.EntityKt.Dsl.() -> kotlin.Unit): no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity =
  no.elg.infiniteBootleg.protobuf.EntityKt.Dsl._create(this.toBuilder()).apply { block() }._build()

@kotlin.jvm.JvmSynthetic
@com.google.errorprone.annotations.CheckReturnValue
public inline fun no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Living.copy(block: no.elg.infiniteBootleg.protobuf.EntityKt.LivingKt.Dsl.() -> kotlin.Unit): no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Living =
  no.elg.infiniteBootleg.protobuf.EntityKt.LivingKt.Dsl._create(this.toBuilder()).apply { block() }._build()

@kotlin.jvm.JvmSynthetic
@com.google.errorprone.annotations.CheckReturnValue
public inline fun no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Material.copy(block: no.elg.infiniteBootleg.protobuf.EntityKt.MaterialKt.Dsl.() -> kotlin.Unit): no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Material =
  no.elg.infiniteBootleg.protobuf.EntityKt.MaterialKt.Dsl._create(this.toBuilder()).apply { block() }._build()

@kotlin.jvm.JvmSynthetic
@com.google.errorprone.annotations.CheckReturnValue
public inline fun no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Player.copy(block: no.elg.infiniteBootleg.protobuf.EntityKt.PlayerKt.Dsl.() -> kotlin.Unit): no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Player =
  no.elg.infiniteBootleg.protobuf.EntityKt.PlayerKt.Dsl._create(this.toBuilder()).apply { block() }._build()

public val no.elg.infiniteBootleg.protobuf.ProtoWorld.EntityOrBuilder.positionOrNull: no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2f?
  get() = if (hasPosition()) getPosition() else null

public val no.elg.infiniteBootleg.protobuf.ProtoWorld.EntityOrBuilder.velocityOrNull: no.elg.infiniteBootleg.protobuf.ProtoWorld.Vector2f?
  get() = if (hasVelocity()) getVelocity() else null

public val no.elg.infiniteBootleg.protobuf.ProtoWorld.EntityOrBuilder.livingOrNull: no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Living?
  get() = if (hasLiving()) getLiving() else null

public val no.elg.infiniteBootleg.protobuf.ProtoWorld.EntityOrBuilder.materialOrNull: no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Material?
  get() = if (hasMaterial()) getMaterial() else null

public val no.elg.infiniteBootleg.protobuf.ProtoWorld.EntityOrBuilder.playerOrNull: no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.Player?
  get() = if (hasPlayer()) getPlayer() else null

