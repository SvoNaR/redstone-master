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
import ru.redstonemaster.client.auth.ModWebAuthService;
import ru.redstonemaster.client.gui.RedstoneMasterScreen;
import ru.redstonemaster.client.profile.ModAvatarManager;
import ru.redstonemaster.config.ModConfig;
import ru.redstonemaster.config.ModContentLanguage;

import com.mojang.blaze3d.platform.InputConstants;

public class RedstoneMasterClient implements ClientModInitializer {
	public static final String MOD_ID = "redstone-master";

	public static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(MOD_ID, "main")
	);

	public static KeyMapping openGuiKey;
	public static KeyMapping navigateBackKey;
	public static KeyMapping navigateForwardKey;

	/** Блокирует повторное открытие после закрытия через beforeKeyPress (в мире consumeClick ещё не съеден). */
	private static boolean suppressNextOpenKey;

	@Override
	public void onInitializeClient() {
		ModConfig.load();
		ModAvatarManager.ensureGuestAvatar();
		ModAvatarManager.loadProfileAvatar();

		openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.redstone-master.open_gui",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_BRACKET,
				KEY_CATEGORY
		));

		navigateBackKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.redstone-master.open_gui.navigation_back",
				InputConstants.Type.MOUSE,
				GLFW.GLFW_MOUSE_BUTTON_4,
				KEY_CATEGORY
		));

		navigateForwardKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.redstone-master.open_gui.navigation_forward",
				InputConstants.Type.MOUSE,
				GLFW.GLFW_MOUSE_BUTTON_5,
				KEY_CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ModWebAuthService.get().tick(client);
			if (client.screen instanceof RedstoneMasterScreen modScreen) {
				if (ModWebAuthService.get().consumeProfileUiStale()) {
					modScreen.rebuildAllWidgets();
				}
				while (openGuiKey.consumeClick()) {
					handleOpenKey(client);
				}
				while (navigateBackKey.consumeClick()) {
					modScreen.navigateBack();
				}
				while (navigateForwardKey.consumeClick()) {
					modScreen.navigateForward();
				}
				return;
			}
			if (client.screen != null) {
				ModWebAuthService.get().tick(client);
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
					if (activeScreen instanceof RedstoneMasterScreen modScreen) {
						if (ModConfig.get().closeOnRepeatKey) {
							suppressNextOpenKey = true;
							modScreen.onClose();
						}
						while (openGuiKey.consumeClick()) {
						}
					} else {
						handleOpenKey(client);
					}
					return;
				}
				if (activeScreen instanceof RedstoneMasterScreen modScreen) {
					if (navigateBackKey.matches(event)) {
						modScreen.navigateBack();
					} else if (navigateForwardKey.matches(event)) {
						modScreen.navigateForward();
					}
				}
			});
		});
	}

	public static void handleOpenKey(net.minecraft.client.Minecraft client) {
		if (client.screen instanceof RedstoneMasterScreen modScreen) {
			if (ModConfig.get().closeOnRepeatKey) {
				modScreen.onClose();
			}
			while (openGuiKey.consumeClick()) {
			}
			return;
		}

		if (suppressNextOpenKey) {
			suppressNextOpenKey = false;
			while (openGuiKey.consumeClick()) {
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
