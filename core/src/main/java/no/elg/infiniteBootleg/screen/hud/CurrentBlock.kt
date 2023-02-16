package no.elg.infiniteBootleg.screen.hud

import com.badlogic.gdx.Gdx
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.screen.ScreenRenderer
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.Material

object CurrentBlock {

  var lastMaterial: Material = Material.AIR

  fun render(sr: ScreenRenderer) {
    val h = Gdx.graphics.height
    val material = lastMaterial
    if (material.textureRegion != null) {
      sr.batch
        .draw(
          material.textureRegion,
          Gdx.graphics.width - Block.BLOCK_SIZE * 3f * ClientMain.SCALE,
          h - Block.BLOCK_SIZE * 3f * ClientMain.SCALE,
          Block.BLOCK_SIZE * 2f * ClientMain.SCALE,
          Block.BLOCK_SIZE * 2f * ClientMain.SCALE
        )
    }
  }
}
