package ru.redstonemaster;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import ru.redstonemaster.client.gui.RedstoneMasterScreen;
import ru.redstonemaster.config.ModConfig;
import ru.redstonemaster.config.ModContentLanguage;

import com.mojang.blaze3d.platform.InputConstants;

public class RedstoneMasterClient implements ClientModInitializer {
	public static final String MOD_ID = "redstone-master";

	public static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(MOD_ID, "main")
	);

	public static KeyMapping openGuiKey;

	@Override
	public void onInitializeClient() {
		ModConfig.load();

		openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.redstone-master.open_gui",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_BRACKET,
				KEY_CATEGORY
		));

		// В мире без открытого экрана — стандартный consumeClick().
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.screen != null) {
				return;
			}
			while (openGuiKey.consumeClick()) {
				handleOpenKey(client);
			}
		});

		// На экранах меню consumeClick() не срабатывает — регистрируем обработчик на каждый Screen.
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			ScreenKeyboardEvents.beforeKeyPress(screen).register((activeScreen, event) -> {
				if (openGuiKey.matches(event)) {
					handleOpenKey(client);
				}
			});
		});
	}

	public static void handleOpenKey(net.minecraft.client.Minecraft client) {
		if (client.screen instanceof RedstoneMasterScreen modScreen) {
			if (ModConfig.get().closeOnRepeatKey) {
				modScreen.onClose();
			}
			return;
		}

		ModConfig config = ModConfig.get();
		if (config.initializeLanguageOnFirstOpen()) {
			ModContentLanguage.clearCache();
		}
		Screen previous = client.screen;
		client.setScreen(new RedstoneMasterScreen(previous));
	}
}
