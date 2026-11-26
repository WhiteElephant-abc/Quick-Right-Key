package io.github.whiteelephantabc.quickrightkey

import net.minecraft.resources.Identifier

object QuickRightKey {
	const val MOD_ID: String = "quickrightkey"

	fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)
}
