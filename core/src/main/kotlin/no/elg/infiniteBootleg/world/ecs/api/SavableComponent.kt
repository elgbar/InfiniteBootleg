package no.elg.infiniteBootleg.world.ecs.api

import com.badlogic.ashley.core.Component

interface SavableComponent<DSL> : Component {

  fun DSL.save()
}
