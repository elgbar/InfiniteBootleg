package no.elg.infiniteBootleg.screen.hud

import com.badlogic.gdx.Gdx
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.screen.ScreenRenderer
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.subgrid.LivingEntity

object CurrentBlock {

  fun render(sr: ScreenRenderer, player: LivingEntity) {
    val h = Gdx.graphics.height
    val mat = player.controls?.selected
    if (mat != null && mat.textureRegion != null) {
      sr.batch
        .draw(
          mat.textureRegion,
          Gdx.graphics.width - Block.BLOCK_SIZE * 3f * ClientMain.SCALE,
          h - Block.BLOCK_SIZE * 3f * ClientMain.SCALE,
          Block.BLOCK_SIZE * 2f * ClientMain.SCALE,
          Block.BLOCK_SIZE * 2f * ClientMain.SCALE
        )
    }
  }
}
