// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: serialization/packets.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package no.elg.infiniteBootleg.protobuf;

@kotlin.jvm.JvmName("-initializepacket")
public inline fun packet(block: no.elg.infiniteBootleg.protobuf.PacketKt.Dsl.() -> kotlin.Unit): no.elg.infiniteBootleg.protobuf.Packets.Packet =
  no.elg.infiniteBootleg.protobuf.PacketKt.Dsl._create(no.elg.infiniteBootleg.protobuf.Packets.Packet.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `packets.Packet`
 */
public object PacketKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: no.elg.infiniteBootleg.protobuf.Packets.Packet.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: no.elg.infiniteBootleg.protobuf.Packets.Packet.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): no.elg.infiniteBootleg.protobuf.Packets.Packet = _builder.build()

    /**
     * `.packets.Packet.Type type = 1;`
     */
    public var type: no.elg.infiniteBootleg.protobuf.Packets.Packet.Type
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
     * `.packets.Packet.Type type = 1;`
     */
    public fun clearType() {
      _builder.clearType()
    }

    /**
     * `.packets.Packet.Direction direction = 2;`
     */
    public var direction: no.elg.infiniteBootleg.protobuf.Packets.Packet.Direction
      @JvmName("getDirection")
      get() = _builder.getDirection()
      @JvmName("setDirection")
      set(value) {
        _builder.setDirection(value)
      }
    public var directionValue: kotlin.Int
      @JvmName("getDirectionValue")
      get() = _builder.getDirectionValue()
      @JvmName("setDirectionValue")
      set(value) {
        _builder.setDirectionValue(value)
      }
    /**
     * `.packets.Packet.Direction direction = 2;`
     */
    public fun clearDirection() {
      _builder.clearDirection()
    }

    /**
     * ```
     *Only optional before sharing the secret
     * ```
     *
     * `optional string secret = 3;`
     */
    public var secret: kotlin.String
      @JvmName("getSecret")
      get() = _builder.getSecret()
      @JvmName("setSecret")
      set(value) {
        _builder.setSecret(value)
      }
    /**
     * ```
     *Only optional before sharing the secret
     * ```
     *
     * `optional string secret = 3;`
     */
    public fun clearSecret() {
      _builder.clearSecret()
    }
    /**
     * ```
     *Only optional before sharing the secret
     * ```
     *
     * `optional string secret = 3;`
     * @return Whether the secret field is set.
     */
    public fun hasSecret(): kotlin.Boolean {
      return _builder.hasSecret()
    }

    /**
     * ```
     *very frequent packets
     * ```
     *
     * `optional .packets.Heartbeat heartbeat = 8;`
     */
    public var heartbeat: no.elg.infiniteBootleg.protobuf.Packets.Heartbeat
      @JvmName("getHeartbeat")
      get() = _builder.getHeartbeat()
      @JvmName("setHeartbeat")
      set(value) {
        _builder.setHeartbeat(value)
      }
    /**
     * ```
     *very frequent packets
     * ```
     *
     * `optional .packets.Heartbeat heartbeat = 8;`
     */
    public fun clearHeartbeat() {
      _builder.clearHeartbeat()
    }
    /**
     * ```
     *very frequent packets
     * ```
     *
     * `optional .packets.Heartbeat heartbeat = 8;`
     * @return Whether the heartbeat field is set.
     */
    public fun hasHeartbeat(): kotlin.Boolean {
      return _builder.hasHeartbeat()
    }
    public val PacketKt.Dsl.heartbeatOrNull: no.elg.infiniteBootleg.protobuf.Packets.Heartbeat?
      get() = _builder.heartbeatOrNull

    /**
     * ```
     *dual
     * ```
     *
     * `optional .packets.MoveEntity moveEntity = 9;`
     */
    public var moveEntity: no.elg.infiniteBootleg.protobuf.Packets.MoveEntity
      @JvmName("getMoveEntity")
      get() = _builder.getMoveEntity()
      @JvmName("setMoveEntity")
      set(value) {
        _builder.setMoveEntity(value)
      }
    /**
     * ```
     *dual
     * ```
     *
     * `optional .packets.MoveEntity moveEntity = 9;`
     */
    public fun clearMoveEntity() {
      _builder.clearMoveEntity()
    }
    /**
     * ```
     *dual
     * ```
     *
     * `optional .packets.MoveEntity moveEntity = 9;`
     * @return Whether the moveEntity field is set.
     */
    public fun hasMoveEntity(): kotlin.Boolean {
      return _builder.hasMoveEntity()
    }
    public val PacketKt.Dsl.moveEntityOrNull: no.elg.infiniteBootleg.protobuf.Packets.MoveEntity?
      get() = _builder.moveEntityOrNull

    /**
     * ```
     *client bound
     * ```
     *
     * `optional .packets.UpdateChunk updateChunk = 10;`
     */
    public var updateChunk: no.elg.infiniteBootleg.protobuf.Packets.UpdateChunk
      @JvmName("getUpdateChunk")
      get() = _builder.getUpdateChunk()
      @JvmName("setUpdateChunk")
      set(value) {
        _builder.setUpdateChunk(value)
      }
    /**
     * ```
     *client bound
     * ```
     *
     * `optional .packets.UpdateChunk updateChunk = 10;`
     */
    public fun clearUpdateChunk() {
      _builder.clearUpdateChunk()
    }
    /**
     * ```
     *client bound
     * ```
     *
     * `optional .packets.UpdateChunk updateChunk = 10;`
     * @return Whether the updateChunk field is set.
     */
    public fun hasUpdateChunk(): kotlin.Boolean {
      return _builder.hasUpdateChunk()
    }
    public val PacketKt.Dsl.updateChunkOrNull: no.elg.infiniteBootleg.protobuf.Packets.UpdateChunk?
      get() = _builder.updateChunkOrNull

    /**
     * `optional .packets.SpawnEntity spawnEntity = 11;`
     */
    public var spawnEntity: no.elg.infiniteBootleg.protobuf.Packets.SpawnEntity
      @JvmName("getSpawnEntity")
      get() = _builder.getSpawnEntity()
      @JvmName("setSpawnEntity")
      set(value) {
        _builder.setSpawnEntity(value)
      }
    /**
     * `optional .packets.SpawnEntity spawnEntity = 11;`
     */
    public fun clearSpawnEntity() {
      _builder.clearSpawnEntity()
    }
    /**
     * `optional .packets.SpawnEntity spawnEntity = 11;`
     * @return Whether the spawnEntity field is set.
     */
    public fun hasSpawnEntity(): kotlin.Boolean {
      return _builder.hasSpawnEntity()
    }
    public val PacketKt.Dsl.spawnEntityOrNull: no.elg.infiniteBootleg.protobuf.Packets.SpawnEntity?
      get() = _builder.spawnEntityOrNull

    /**
     * `optional .packets.DespawnEntity despawnEntity = 12;`
     */
    public var despawnEntity: no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity
      @JvmName("getDespawnEntity")
      get() = _builder.getDespawnEntity()
      @JvmName("setDespawnEntity")
      set(value) {
        _builder.setDespawnEntity(value)
      }
    /**
     * `optional .packets.DespawnEntity despawnEntity = 12;`
     */
    public fun clearDespawnEntity() {
      _builder.clearDespawnEntity()
    }
    /**
     * `optional .packets.DespawnEntity despawnEntity = 12;`
     * @return Whether the despawnEntity field is set.
     */
    public fun hasDespawnEntity(): kotlin.Boolean {
      return _builder.hasDespawnEntity()
    }
    public val PacketKt.Dsl.despawnEntityOrNull: no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity?
      get() = _builder.despawnEntityOrNull

    /**
     * ```
     *dual
     * ```
     *
     * `optional .packets.UpdateBlock updateBlock = 13;`
     */
    public var updateBlock: no.elg.infiniteBootleg.protobuf.Packets.UpdateBlock
      @JvmName("getUpdateBlock")
      get() = _builder.getUpdateBlock()
      @JvmName("setUpdateBlock")
      set(value) {
        _builder.setUpdateBlock(value)
      }
    /**
     * ```
     *dual
     * ```
     *
     * `optional .packets.UpdateBlock updateBlock = 13;`
     */
    public fun clearUpdateBlock() {
      _builder.clearUpdateBlock()
    }
    /**
     * ```
     *dual
     * ```
     *
     * `optional .packets.UpdateBlock updateBlock = 13;`
     * @return Whether the updateBlock field is set.
     */
    public fun hasUpdateBlock(): kotlin.Boolean {
      return _builder.hasUpdateBlock()
    }
    public val PacketKt.Dsl.updateBlockOrNull: no.elg.infiniteBootleg.protobuf.Packets.UpdateBlock?
      get() = _builder.updateBlockOrNull

    /**
     * ```
     *more rare packets
     * ```
     *
     * `optional .packets.Login login = 17;`
     */
    public var login: no.elg.infiniteBootleg.protobuf.Packets.Login
      @JvmName("getLogin")
      get() = _builder.getLogin()
      @JvmName("setLogin")
      set(value) {
        _builder.setLogin(value)
      }
    /**
     * ```
     *more rare packets
     * ```
     *
     * `optional .packets.Login login = 17;`
     */
    public fun clearLogin() {
      _builder.clearLogin()
    }
    /**
     * ```
     *more rare packets
     * ```
     *
     * `optional .packets.Login login = 17;`
     * @return Whether the login field is set.
     */
    public fun hasLogin(): kotlin.Boolean {
      return _builder.hasLogin()
    }
    public val PacketKt.Dsl.loginOrNull: no.elg.infiniteBootleg.protobuf.Packets.Login?
      get() = _builder.loginOrNull

    /**
     * ```
     *client bound
     * ```
     *
     * `optional .packets.StartGame startGame = 18;`
     */
    public var startGame: no.elg.infiniteBootleg.protobuf.Packets.StartGame
      @JvmName("getStartGame")
      get() = _builder.getStartGame()
      @JvmName("setStartGame")
      set(value) {
        _builder.setStartGame(value)
      }
    /**
     * ```
     *client bound
     * ```
     *
     * `optional .packets.StartGame startGame = 18;`
     */
    public fun clearStartGame() {
      _builder.clearStartGame()
    }
    /**
     * ```
     *client bound
     * ```
     *
     * `optional .packets.StartGame startGame = 18;`
     * @return Whether the startGame field is set.
     */
    public fun hasStartGame(): kotlin.Boolean {
      return _builder.hasStartGame()
    }
    public val PacketKt.Dsl.startGameOrNull: no.elg.infiniteBootleg.protobuf.Packets.StartGame?
      get() = _builder.startGameOrNull

    /**
     * ```
     *client bound
     * ```
     *
     * `optional .packets.ServerLoginStatus serverLoginStatus = 20;`
     */
    public var serverLoginStatus: no.elg.infiniteBootleg.protobuf.Packets.ServerLoginStatus
      @JvmName("getServerLoginStatus")
      get() = _builder.getServerLoginStatus()
      @JvmName("setServerLoginStatus")
      set(value) {
        _builder.setServerLoginStatus(value)
      }
    /**
     * ```
     *client bound
     * ```
     *
     * `optional .packets.ServerLoginStatus serverLoginStatus = 20;`
     */
    public fun clearServerLoginStatus() {
      _builder.clearServerLoginStatus()
    }
    /**
     * ```
     *client bound
     * ```
     *
     * `optional .packets.ServerLoginStatus serverLoginStatus = 20;`
     * @return Whether the serverLoginStatus field is set.
     */
    public fun hasServerLoginStatus(): kotlin.Boolean {
      return _builder.hasServerLoginStatus()
    }
    public val PacketKt.Dsl.serverLoginStatusOrNull: no.elg.infiniteBootleg.protobuf.Packets.ServerLoginStatus?
      get() = _builder.serverLoginStatusOrNull

    /**
     * `optional .packets.Disconnect disconnect = 21;`
     */
    public var disconnect: no.elg.infiniteBootleg.protobuf.Packets.Disconnect
      @JvmName("getDisconnect")
      get() = _builder.getDisconnect()
      @JvmName("setDisconnect")
      set(value) {
        _builder.setDisconnect(value)
      }
    /**
     * `optional .packets.Disconnect disconnect = 21;`
     */
    public fun clearDisconnect() {
      _builder.clearDisconnect()
    }
    /**
     * `optional .packets.Disconnect disconnect = 21;`
     * @return Whether the disconnect field is set.
     */
    public fun hasDisconnect(): kotlin.Boolean {
      return _builder.hasDisconnect()
    }
    public val PacketKt.Dsl.disconnectOrNull: no.elg.infiniteBootleg.protobuf.Packets.Disconnect?
      get() = _builder.disconnectOrNull

    /**
     * `optional .packets.SecretExchange secretExchange = 22;`
     */
    public var secretExchange: no.elg.infiniteBootleg.protobuf.Packets.SecretExchange
      @JvmName("getSecretExchange")
      get() = _builder.getSecretExchange()
      @JvmName("setSecretExchange")
      set(value) {
        _builder.setSecretExchange(value)
      }
    /**
     * `optional .packets.SecretExchange secretExchange = 22;`
     */
    public fun clearSecretExchange() {
      _builder.clearSecretExchange()
    }
    /**
     * `optional .packets.SecretExchange secretExchange = 22;`
     * @return Whether the secretExchange field is set.
     */
    public fun hasSecretExchange(): kotlin.Boolean {
      return _builder.hasSecretExchange()
    }
    public val PacketKt.Dsl.secretExchangeOrNull: no.elg.infiniteBootleg.protobuf.Packets.SecretExchange?
      get() = _builder.secretExchangeOrNull

    /**
     * ```
     *server bound
     * ```
     *
     * `optional .packets.ChunkRequest chunkRequest = 23;`
     */
    public var chunkRequest: no.elg.infiniteBootleg.protobuf.Packets.ChunkRequest
      @JvmName("getChunkRequest")
      get() = _builder.getChunkRequest()
      @JvmName("setChunkRequest")
      set(value) {
        _builder.setChunkRequest(value)
      }
    /**
     * ```
     *server bound
     * ```
     *
     * `optional .packets.ChunkRequest chunkRequest = 23;`
     */
    public fun clearChunkRequest() {
      _builder.clearChunkRequest()
    }
    /**
     * ```
     *server bound
     * ```
     *
     * `optional .packets.ChunkRequest chunkRequest = 23;`
     * @return Whether the chunkRequest field is set.
     */
    public fun hasChunkRequest(): kotlin.Boolean {
      return _builder.hasChunkRequest()
    }
    public val PacketKt.Dsl.chunkRequestOrNull: no.elg.infiniteBootleg.protobuf.Packets.ChunkRequest?
      get() = _builder.chunkRequestOrNull

    /**
     * `optional .packets.EntityRequest entityRequest = 24;`
     */
    public var entityRequest: no.elg.infiniteBootleg.protobuf.Packets.EntityRequest
      @JvmName("getEntityRequest")
      get() = _builder.getEntityRequest()
      @JvmName("setEntityRequest")
      set(value) {
        _builder.setEntityRequest(value)
      }
    /**
     * `optional .packets.EntityRequest entityRequest = 24;`
     */
    public fun clearEntityRequest() {
      _builder.clearEntityRequest()
    }
    /**
     * `optional .packets.EntityRequest entityRequest = 24;`
     * @return Whether the entityRequest field is set.
     */
    public fun hasEntityRequest(): kotlin.Boolean {
      return _builder.hasEntityRequest()
    }
    public val PacketKt.Dsl.entityRequestOrNull: no.elg.infiniteBootleg.protobuf.Packets.EntityRequest?
      get() = _builder.entityRequestOrNull

    /**
     * `optional .packets.WorldSettings worldSettings = 25;`
     */
    public var worldSettings: no.elg.infiniteBootleg.protobuf.Packets.WorldSettings
      @JvmName("getWorldSettings")
      get() = _builder.getWorldSettings()
      @JvmName("setWorldSettings")
      set(value) {
        _builder.setWorldSettings(value)
      }
    /**
     * `optional .packets.WorldSettings worldSettings = 25;`
     */
    public fun clearWorldSettings() {
      _builder.clearWorldSettings()
    }
    /**
     * `optional .packets.WorldSettings worldSettings = 25;`
     * @return Whether the worldSettings field is set.
     */
    public fun hasWorldSettings(): kotlin.Boolean {
      return _builder.hasWorldSettings()
    }
    public val PacketKt.Dsl.worldSettingsOrNull: no.elg.infiniteBootleg.protobuf.Packets.WorldSettings?
      get() = _builder.worldSettingsOrNull
  }
}
@kotlin.jvm.JvmSynthetic
@com.google.errorprone.annotations.CheckReturnValue
public inline fun no.elg.infiniteBootleg.protobuf.Packets.Packet.copy(block: no.elg.infiniteBootleg.protobuf.PacketKt.Dsl.() -> kotlin.Unit): no.elg.infiniteBootleg.protobuf.Packets.Packet =
  no.elg.infiniteBootleg.protobuf.PacketKt.Dsl._create(this.toBuilder()).apply { block() }._build()

public val no.elg.infiniteBootleg.protobuf.Packets.PacketOrBuilder.heartbeatOrNull: no.elg.infiniteBootleg.protobuf.Packets.Heartbeat?
  get() = if (hasHeartbeat()) getHeartbeat() else null

public val no.elg.infiniteBootleg.protobuf.Packets.PacketOrBuilder.moveEntityOrNull: no.elg.infiniteBootleg.protobuf.Packets.MoveEntity?
  get() = if (hasMoveEntity()) getMoveEntity() else null

public val no.elg.infiniteBootleg.protobuf.Packets.PacketOrBuilder.updateChunkOrNull: no.elg.infiniteBootleg.protobuf.Packets.UpdateChunk?
  get() = if (hasUpdateChunk()) getUpdateChunk() else null

public val no.elg.infiniteBootleg.protobuf.Packets.PacketOrBuilder.spawnEntityOrNull: no.elg.infiniteBootleg.protobuf.Packets.SpawnEntity?
  get() = if (hasSpawnEntity()) getSpawnEntity() else null

public val no.elg.infiniteBootleg.protobuf.Packets.PacketOrBuilder.despawnEntityOrNull: no.elg.infiniteBootleg.protobuf.Packets.DespawnEntity?
  get() = if (hasDespawnEntity()) getDespawnEntity() else null

public val no.elg.infiniteBootleg.protobuf.Packets.PacketOrBuilder.updateBlockOrNull: no.elg.infiniteBootleg.protobuf.Packets.UpdateBlock?
  get() = if (hasUpdateBlock()) getUpdateBlock() else null

public val no.elg.infiniteBootleg.protobuf.Packets.PacketOrBuilder.loginOrNull: no.elg.infiniteBootleg.protobuf.Packets.Login?
  get() = if (hasLogin()) getLogin() else null

public val no.elg.infiniteBootleg.protobuf.Packets.PacketOrBuilder.startGameOrNull: no.elg.infiniteBootleg.protobuf.Packets.StartGame?
  get() = if (hasStartGame()) getStartGame() else null

public val no.elg.infiniteBootleg.protobuf.Packets.PacketOrBuilder.serverLoginStatusOrNull: no.elg.infiniteBootleg.protobuf.Packets.ServerLoginStatus?
  get() = if (hasServerLoginStatus()) getServerLoginStatus() else null

public val no.elg.infiniteBootleg.protobuf.Packets.PacketOrBuilder.disconnectOrNull: no.elg.infiniteBootleg.protobuf.Packets.Disconnect?
  get() = if (hasDisconnect()) getDisconnect() else null

public val no.elg.infiniteBootleg.protobuf.Packets.PacketOrBuilder.secretExchangeOrNull: no.elg.infiniteBootleg.protobuf.Packets.SecretExchange?
  get() = if (hasSecretExchange()) getSecretExchange() else null

public val no.elg.infiniteBootleg.protobuf.Packets.PacketOrBuilder.chunkRequestOrNull: no.elg.infiniteBootleg.protobuf.Packets.ChunkRequest?
  get() = if (hasChunkRequest()) getChunkRequest() else null

public val no.elg.infiniteBootleg.protobuf.Packets.PacketOrBuilder.entityRequestOrNull: no.elg.infiniteBootleg.protobuf.Packets.EntityRequest?
  get() = if (hasEntityRequest()) getEntityRequest() else null

public val no.elg.infiniteBootleg.protobuf.Packets.PacketOrBuilder.worldSettingsOrNull: no.elg.infiniteBootleg.protobuf.Packets.WorldSettings?
  get() = if (hasWorldSettings()) getWorldSettings() else null
