package no.elg.infiniteBootleg.server

import io.netty.channel.ChannelHandlerContext
import java.util.UUID
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.protobuf.Packets
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_LOGIN_STATUS
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_START_GAME
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.CB_UPDATE_CHUNK
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_BLOCK_UPDATE
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_DISCONNECT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_HEARTBEAT
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_MOVE_ENTITY
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.DX_SECRET_EXCHANGE
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.SB_CLIENT_WORLD_LOADED
import no.elg.infiniteBootleg.protobuf.Packets.Packet.Type.UNRECOGNIZED
import no.elg.infiniteBootleg.protobuf.Packets.SecretExchange
import no.elg.infiniteBootleg.protobuf.Packets.ServerLoginStatus
import no.elg.infiniteBootleg.protobuf.Packets.StartGame
import no.elg.infiniteBootleg.protobuf.Packets.UpdateChunk
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.EntityType.PLAYER
import no.elg.infiniteBootleg.screens.ConnectingScreen
import no.elg.infiniteBootleg.screens.WorldScreen
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.subgrid.enitites.Player

/**
 * @author Elg
 */
fun Client.handleClientBoundPackets(packet: Packets.Packet) {
  when (packet.type) {
    CB_LOGIN_STATUS -> {
      if (packet.hasServerLoginStatus()) {
        val loginStatus = packet.serverLoginStatus.status
        loginStatus(loginStatus)
      }
    }
    CB_START_GAME -> {
      if (packet.hasStartGame()) {
        startGame(packet.startGame)
      }
    }
    CB_UPDATE_CHUNK -> {
      if (packet.hasUpdateChunk()) {
        updateChunk(packet.updateChunk)
      }
    }

    DX_HEARTBEAT -> {
      if (packet.hasHeartbeat()) {
        //very simple to implement!
        ctx.writeAndFlush(packet)
      }
    }
    DX_MOVE_ENTITY -> TODO()
    DX_BLOCK_UPDATE -> TODO()

    DX_SECRET_EXCHANGE -> {
      if (packet.hasSecretExchange()) {
        handleSecretExchange(ctx, packet.secretExchange)
      }
    }

    DX_DISCONNECT -> {
      if (packet.hasDisconnect()) {
        ConnectingScreen.info = "Disconnected: ${packet.disconnect.reason}"
      } else {
        ConnectingScreen.info = "Disconnected: Unknown reason"
      }
      ctx.close()
    }

    UNRECOGNIZED -> {
      ctx.fatal("Unknown packet type received")
    }
    else -> {
      ctx.fatal("Cannot handle packet of type " + packet.type)
    }
  }
}

private fun Client.handleSecretExchange(ctx: ChannelHandlerContext, secretExchange: SecretExchange) {
  val uuid = try {
    UUID.fromString(secretExchange.entityUUID)
  } catch (e: IllegalArgumentException) {
    ctx.fatal("Failed to decode entity UUID ${secretExchange.entityUUID}")
    return
  }
  credentials = ConnectionCredentials(uuid, secretExchange.secret)
  ctx.writeAndFlush(clientSecretResponse(credentials))
}

fun Client.updateChunk(updateChunk: UpdateChunk) {
  val chunk = world?.chunkLoader?.clientLoad(updateChunk.chunk) ?: return
  world?.updateChunk(chunk)
}


fun Client.startGame(startGame: StartGame) {
  val protoWorld = startGame.world
  Main.inst().scheduler.executeSync {
    this.world = World(protoWorld, this).apply {
      serverLoad(protoWorld)
    }
    if (startGame.controlling.type != PLAYER) {
      ctx.fatal("Can only control a player, got ${startGame.controlling.type}")
    } else {
      this.controllingEntity = startGame.controlling
      ctx.writeAndFlush(serverBoundPacket(SB_CLIENT_WORLD_LOADED))
    }
  }
}

fun Client.loginStatus(loginStatus: ServerLoginStatus.ServerStatus) {
  when (loginStatus) {
    ServerLoginStatus.ServerStatus.ALREADY_LOGGED_IN -> {
      ctx.fatal("You are already logged in!")
      return
    }
    ServerLoginStatus.ServerStatus.FULL_SERVER -> {
      ctx.fatal("Server is full")
      return
    }
    ServerLoginStatus.ServerStatus.PROCEED_LOGIN -> {
      ConnectingScreen.info = "Logging in..."
    }
    ServerLoginStatus.ServerStatus.LOGIN_SUCCESS -> {
      val world = world
      val entity = controllingEntity
      if (world == null || entity == null) {
        ctx.fatal("Failed to get world and/or entity")
        return
      }
      ConnectingScreen.info = "Login success!"
      Main.inst().scheduler.executeSync {
        ClientMain.inst().screen = WorldScreen(world, false)
        val player = Player(world, entity)
        if (player.isInvalid) {
          ctx.fatal("Invalid player client side")
        } else {
          player.name = name
          world.addEntity(player)
          world.worldTicker.start()
          player.giveControls()
        }
      }
    }
    ServerLoginStatus.ServerStatus.UNRECOGNIZED -> {
      ctx.fatal("Unrecognized server status")
      return
    }
  }
}

