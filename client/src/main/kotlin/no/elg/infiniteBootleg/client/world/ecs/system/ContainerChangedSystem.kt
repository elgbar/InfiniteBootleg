package no.elg.infiniteBootleg.client.world.ecs.system

import com.badlogic.ashley.systems.IntervalSystem
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.screens.WorldScreen
import no.elg.infiniteBootleg.client.screens.hud.ContainerChangeRenderer.Companion.SECONDS_PER_LINE
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_DEFAULT

object ContainerChangedSystem : IntervalSystem(SECONDS_PER_LINE, UPDATE_PRIORITY_DEFAULT) {

  override fun updateInterval() {
    val containerChangeRenderer = (ClientMain.inst().screen as? WorldScreen)?.hud?.containerChangeRenderer ?: return
    containerChangeRenderer.tick()
  }
}
