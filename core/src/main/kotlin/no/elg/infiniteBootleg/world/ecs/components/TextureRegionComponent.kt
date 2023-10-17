package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.texture
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.safeWith
import no.elg.infiniteBootleg.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.render.texture.RotatableTextureRegion

data class TextureRegionComponent(var texture: RotatableTextureRegion) : EntitySavableComponent {
  companion object : EntityLoadableMapper<TextureRegionComponent>() {
    var Entity.textureRegionComponent by propertyFor(mapper)
    var Entity.textureRegionComponentOrNull by optionalPropertyFor(mapper)
    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasTexture()

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity): TextureRegionComponent? {
      val region = Main.inst().assets.safeTextureAtlas.findRotationAwareRegion(protoEntity.texture.texture, false)
      return safeWith { TextureRegionComponent(region) }
    }
  }

  override fun EntityKt.Dsl.save() {
    texture = texture {
      texture = this@TextureRegionComponent.texture.name
    }
  }
}
