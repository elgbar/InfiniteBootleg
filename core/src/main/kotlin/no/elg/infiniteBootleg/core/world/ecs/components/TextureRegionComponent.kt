package no.elg.infiniteBootleg.core.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.ClientComponent
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.texture
import no.elg.infiniteBootleg.protobuf.ProtoWorld

data class TextureRegionComponent(var textureName: String) : EntitySavableComponent, ClientComponent {

  //  val texture: RotatableTextureRegion get() = Main.inst().assets.findTexture(textureName, false)
  override fun hudDebug(): String = "texture $textureName"

  companion object : EntityLoadableMapper<TextureRegionComponent>() {
    var Entity.textureRegionComponent by propertyFor(mapper)
    var Entity.textureRegionComponentOrNull by optionalPropertyFor(mapper)

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasTexture()

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity): TextureRegionComponent? {
      return safeWith { TextureRegionComponent(protoEntity.texture.texture) }
    }
  }

  override fun EntityKt.Dsl.save() {
    texture = texture {
      texture = this@TextureRegionComponent.textureName
    }
  }
}
