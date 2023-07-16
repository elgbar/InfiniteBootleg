package no.elg.infiniteBootleg.server

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import no.elg.infiniteBootleg.TestGraphic
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
    getClient().handleLoginStatus(FULL_SERVER)
    verify(exactly = 1) { ctx.close() }
  }

  @Test
  fun `disconnect when already logged in`() {
    getClient().handleLoginStatus(ALREADY_LOGGED_IN)
    verify(exactly = 1) { ctx.close() }
  }

  @Test
  fun `do not disconnect when PROCEED_LOGIN`() {
    getClient().handleLoginStatus(PROCEED_LOGIN)
    verify(exactly = 0) { ctx.close() }
  }

  companion object {
    const val CLIENT_NAME = "test"
  }
}
