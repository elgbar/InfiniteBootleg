package no.elg.infiniteBootleg.world.ecs.api

import no.elg.infiniteBootleg.protobuf.EntityKt

interface TagSavableComponent : SavableComponent<EntityKt.TagsKt.Dsl>
