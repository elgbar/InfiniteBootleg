package no.elg.infiniteBootleg.core.util

import org.apache.commons.lang3.EnumUtils

inline fun <reified T> valueOfOrNull(name: String): T? where T : Enum<T> = EnumUtils.getEnumIgnoreCase(T::class.java, name)

fun Enum<*>.textureName(customTextureName: String? = null): String = customTextureName ?: name.lowercase()

fun rotatableTextureName(textureName: String): String = "${textureName}_rotatable"
