package no.elg.infiniteBootleg.server

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import java.util.UUID
import no.elg.infiniteBootleg.TestGraphic
import no.elg.infiniteBootleg.protobuf.Packets.ServerLoginStatus.ServerStatus.ALREADY_LOGGED_IN
import no.elg.infiniteBootleg.protobuf.Packets.ServerLoginStatus.ServerStatus.FULL_SERVER
import no.elg.infiniteBootleg.protobuf.Packets.ServerLoginStatus.ServerStatus.LOGIN_SUCCESS
import no.elg.infiniteBootleg.protobuf.Packets.ServerLoginStatus.ServerStatus.PROCEED_LOGIN
import no.elg.infiniteBootleg.world.subgrid.enitites.Player
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * @author Elg
 */


class TowardsServerClientPacketsHandlerKtTest : TestGraphic() {

  lateinit var ctx: ChannelHandlerContext
  lateinit var channel: Channel

  fun getClient(): ServerClient {
    return ServerClient(CLIENT_NAME).also {
      it.ctx = ctx
    }
  }

  @BeforeEach
  fun beforeEach() {
    ctx = mockk(relaxed = true)
    channel = mockk()
    every { ctx.channel() }.returns(channel)
  }

  @Test
  fun updateChunk() {

  }

  @Test
  fun startGame() {

  }

  @Test
  fun `disconnect when server full`() {
    getClient().loginStatus(FULL_SERVER)
    verify(exactly = 1) { ctx.close() }
  }

  @Test
  fun `disconnect when already logged in`() {
    getClient().loginStatus(ALREADY_LOGGED_IN)
    verify(exactly = 1) { ctx.close() }
  }

  @Test
  fun `do not disconnect when PROCEED_LOGIN`() {
    getClient().loginStatus(PROCEED_LOGIN)
    verify(exactly = 0) { ctx.close() }
  }

  @Test
  fun `disconnect when already logged in2`() {
    val newWorld = createNewWorld()
    val client = getClient()
    client.world = newWorld
    client.controllingEntity = Player(newWorld, 0f, 0f, UUID.randomUUID()).save().build()
    client.loginStatus(LOGIN_SUCCESS)
    verify(exactly = 0) { ctx.close() }
  }

  companion object {
    const val CLIENT_NAME = "test"
  }
}
