package no.elg.infiniteBootleg.screens.hud

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Stage
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.ashley.plusAssign
import ktx.scene2d.KTable
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actors
import ktx.scene2d.horizontalGroup
import ktx.scene2d.vis.KVisWindow
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visSlider
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visWindow
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.screens.hide
import no.elg.infiniteBootleg.screens.onKeyDown
import no.elg.infiniteBootleg.world.Staff
import no.elg.infiniteBootleg.world.ecs.components.NameComponent.Companion.nameOrNull
import no.elg.infiniteBootleg.world.ecs.components.SelectedInventoryItemComponent
import no.elg.infiniteBootleg.world.ecs.components.SelectedInventoryItemComponent.Companion.selectedInventoryItemComponentOrNull
import no.elg.infiniteBootleg.world.magic.Gem
import no.elg.infiniteBootleg.world.magic.Ring
import no.elg.infiniteBootleg.world.magic.Wood
import no.elg.infiniteBootleg.world.magic.parts.AntiGravityRing
import no.elg.infiniteBootleg.world.magic.parts.Birch
import no.elg.infiniteBootleg.world.magic.parts.Diamond
import no.elg.infiniteBootleg.world.magic.parts.GemRating
import no.elg.infiniteBootleg.world.magic.parts.GemType
import no.elg.infiniteBootleg.world.magic.parts.RatelessRingType
import no.elg.infiniteBootleg.world.magic.parts.RingRating
import no.elg.infiniteBootleg.world.magic.parts.RingType
import no.elg.infiniteBootleg.world.magic.parts.WoodRating
import no.elg.infiniteBootleg.world.magic.parts.WoodType
import no.elg.infiniteBootleg.world.world.ClientWorld

@Scene2dDsl
fun KTable.floatSlider(
  name: String,
  srcValueGetter: () -> Float,
  min: Float,
  max: Float,
  step: Float,
  onAnyElementChanged: MutableList<() -> Unit>,
  onChange: (Float) -> Unit
) {
  horizontalGroup {
    it.fillX()
    space(5f)
    visLabel(name)
    visSlider(min, max, step) {
      this.name = name
      setValue(srcValueGetter())

      onAnyElementChanged += {
        setValue(srcValueGetter())
      }
      onChange {
        onChange(this.value)
        updateAllValues(onAnyElementChanged)
      }
    }
  }
}

fun Stage.addStaffCreatorOverlay(world: ClientWorld): KVisWindow {
  actors {
    val onAnyElementChanged: MutableList<() -> Unit> = mutableListOf()
    return visWindow("Staff Creator") {
      addCloseButton()
      onKeyDown(Input.Keys.ESCAPE) { hide() }
      hide()
      defaults().space(5f).padLeft(2.5f).padRight(2.5f).padBottom(2.5f)

      fun addWoodSelector(): () -> Wood {
        var type: WoodType = Birch
        var rating: WoodRating = WoodRating.FRESHLY_CUT
        section {
          sealedSelector<WoodType>(onAnyElementChanged, type) { type = it }
          enumSelector<WoodRating>(onAnyElementChanged, rating) { rating = it }
        }
        return { Wood(type, rating) }
      }

      fun addGemSelector(): () -> Gem {
        var type: GemType = Diamond
        var rating: GemRating = GemRating.FLAWLESS
        section {
          sealedSelector<GemType>(onAnyElementChanged, type) { type = it }
          enumSelector<GemRating>(onAnyElementChanged, rating) { rating = it }
        }
        return { Gem(type, rating) }
      }

      fun addRingSelector(): () -> Ring? {
        var type: RingType<RingRating?> = AntiGravityRing
        var rating: RingRating = RingRating.FLAWLESS
        section {
          sealedSelector<RingType<RingRating?>>(onAnyElementChanged, type) {
            type = it
          }
          enumSelector<RingRating>(onAnyElementChanged, rating) {
            rating = it
          }
        }
        return { Ring(type, if (type is RatelessRingType) null else rating) }
      }

      val wood = addWoodSelector()
      val gems = mutableListOf<() -> Gem>()
      val rings = mutableListOf<() -> Ring?>()

      gems += addGemSelector()
      rings += addRingSelector()
      rings += addRingSelector()

      section {
        visTextButton("Create Staff") {
          onClick {
            val newStaff = Staff(wood(), gems.map { it() }, rings.mapNotNull { it() })

            world.controlledPlayerEntities.forEach { player ->
              Main.logger().log("Giving player ${player.nameOrNull} a new staff $newStaff")
              val existing = player.selectedInventoryItemComponentOrNull
              if (existing != null) {
                existing.element = newStaff
              } else {
                player += SelectedInventoryItemComponent(newStaff)
              }
            }
          }
        }
      }
      pack()
    }
  }
}
