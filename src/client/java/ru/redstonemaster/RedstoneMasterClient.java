package ru.redstonemaster;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import ru.redstonemaster.client.gui.RedstoneMasterScreen;
import ru.redstonemaster.config.ModConfig;
import ru.redstonemaster.config.ModContentLanguage;

import com.mojang.blaze3d.platform.InputConstants;

public class RedstoneMasterClient implements ClientModInitializer {
	public static final String MOD_ID = "redstone_master";

	public static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(MOD_ID, "main")
	);

	public static KeyMapping openGuiKey;

	@Override
	public void onInitializeClient() {
		ModConfig.load();

		openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.redstone_master.open_gui",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_BRACKET,
				KEY_CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openGuiKey.consumeClick()) {
				handleOpenKey(client);
			}
		});
	}

	public static void handleOpenKey(net.minecraft.client.Minecraft client) {
		if (client.screen instanceof RedstoneMasterScreen) {
			if (ModConfig.get().closeOnRepeatKey) {
				client.setScreen(null);
			}
		} else {
			ModConfig config = ModConfig.get();
			if (config.initializeLanguageOnFirstOpen()) {
				ModContentLanguage.clearCache();
			}
			client.setScreen(new RedstoneMasterScreen());
		}
	}
}
