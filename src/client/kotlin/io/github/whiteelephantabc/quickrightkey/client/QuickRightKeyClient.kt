package io.github.whiteelephantabc.quickrightkey.client

import com.mojang.blaze3d.platform.InputConstants
import io.github.whiteelephantabc.quickrightkey.QuickRightKey
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.resources.Identifier

object QuickRightKeyClient : ClientModInitializer {
	private val session = OpenBackpackSession()
	private lateinit var openKey: KeyMapping

	override fun onInitializeClient() {
		val category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(QuickRightKey.MOD_ID, "main"))
		openKey = KeyMappingHelper.registerKeyMapping(
			KeyMapping("key.quickrightkey.open", InputConstants.Type.KEYBOARD, InputConstants.KEY_B, category)
		)
		ClientTickEvents.END_CLIENT_TICK.register(object : ClientTickEvents.EndTick {
			override fun onEndTick(client: Minecraft) {
				while (openKey.consumeClick()) if (session.canStart(client)) session.start(client)
				session.tick(client)
			}
		})
		ClientPlayConnectionEvents.DISCONNECT.register(object : ClientPlayConnectionEvents.Disconnect {
			override fun onPlayDisconnect(listener: ClientPacketListener, client: Minecraft) {
				session.reset()
			}
		})
	}
}
