package no.elg.infiniteBootleg.core.world.ecs.components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.ClientComponent
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.tinted
import no.elg.infiniteBootleg.protobuf.ProtoWorld

data class TintedComponent(val tint: Color) :
  EntitySavableComponent,
  ClientComponent {

  override fun hudDebug(): String = "tint (RRGGBBAA) $tint"

  companion object : EntityLoadableMapper<TintedComponent>() {
    var Entity.tintedComponentOrNull by optionalPropertyFor(mapper)
    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasTinted()

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity): TintedComponent? = safeWith { TintedComponent(Color(protoEntity.tinted.rgba8888)) }
  }

  override fun EntityKt.Dsl.save() {
    tinted = tinted {
      rgba8888 = Color.rgba8888(tint)
    }
  }
}
