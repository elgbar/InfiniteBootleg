package no.elg.infiniteBootleg.util

import org.apache.commons.lang3.EnumUtils

inline fun <reified T> valueOfOrNull(name: String): T? where T : Enum<T> = EnumUtils.getEnumIgnoreCase(T::class.java, name)
