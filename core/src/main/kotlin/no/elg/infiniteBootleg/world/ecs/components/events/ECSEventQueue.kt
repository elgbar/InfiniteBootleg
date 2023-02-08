package no.elg.infiniteBootleg.world.ecs.components.events

import com.badlogic.ashley.core.Component
import java.util.concurrent.ConcurrentLinkedQueue

interface ECSEventQueue<T : ECSEvent> : Component {

  val events: ConcurrentLinkedQueue<T>
}
