package no.elg.infiniteBootleg.screens.hud

import com.badlogic.gdx.Gdx
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.screen.ScreenRenderer
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.ecs.components.SelectedMaterialComponent.Companion.selectedMaterial
import no.elg.infiniteBootleg.world.ecs.selectedMaterialComponentFamily
import no.elg.infiniteBootleg.world.world.World

object CurrentBlockHUDRenderer {

  fun render(screenRenderer: ScreenRenderer, world: World) {
    val entity = world.engine.getEntitiesFor(selectedMaterialComponentFamily).firstOrNull() ?: return
    val material = entity.selectedMaterial.material
    material.textureRegion?.also { texture ->
      screenRenderer.batch.draw(
        texture,
        Gdx.graphics.width - Block.BLOCK_SIZE * 3f * ClientMain.SCALE,
        Gdx.graphics.height - Block.BLOCK_SIZE * 3f * ClientMain.SCALE,
        Block.BLOCK_SIZE * 2f * ClientMain.SCALE,
        Block.BLOCK_SIZE * 2f * ClientMain.SCALE
      )
    }
  }
}
