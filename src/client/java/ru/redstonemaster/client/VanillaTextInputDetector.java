package ru.redstonemaster.client;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

import java.lang.reflect.Field;

/**
 * Определяет ввод текста в экранах Minecraft, где поле поиска не всегда попадает в {@link Screen#getFocused()}.
 */
final class VanillaTextInputDetector {

	private static final Field CREATIVE_SELECTED_TAB = findField(
			CreativeModeInventoryScreen.class,
			"selectedTab"
	);
	private static final Field CREATIVE_SEARCH_BOX = findField(
			CreativeModeInventoryScreen.class,
			"searchBox"
	);
	private static final Field RECIPE_BOOK_COMPONENT = findField(
			AbstractRecipeBookScreen.class,
			"recipeBookComponent"
	);
	private static final Field RECIPE_BOOK_SEARCH_BOX = findField(
			RecipeBookComponent.class,
			"searchBox"
	);

	private VanillaTextInputDetector() {
	}

	static boolean hasActiveTextInput(Screen screen) {
		if (hasFocusedTextField(screen)) {
			return true;
		}
		if (screen instanceof CreativeModeInventoryScreen creativeScreen) {
			return isCreativeSearchActive(creativeScreen);
		}
		if (screen instanceof AbstractRecipeBookScreen<?> recipeBookScreen) {
			return isRecipeBookSearchActive(recipeBookScreen);
		}
		return false;
	}

	private static boolean hasFocusedTextField(Screen screen) {
		if (screen.getFocused() instanceof EditBox || screen.getFocused() instanceof MultiLineEditBox) {
			return true;
		}
		for (var child : screen.children()) {
			if (child instanceof EditBox editBox && editBox.isFocused()) {
				return true;
			}
			if (child instanceof MultiLineEditBox multiLineEditBox && multiLineEditBox.isFocused()) {
				return true;
			}
		}
		return false;
	}

	private static boolean isCreativeSearchActive(CreativeModeInventoryScreen screen) {
		EditBox searchBox = readField(CREATIVE_SEARCH_BOX, screen, EditBox.class);
		if (searchBox != null && searchBox.isFocused()) {
			return true;
		}
		CreativeModeTab selectedTab = readField(CREATIVE_SELECTED_TAB, screen, CreativeModeTab.class);
		return selectedTab != null && selectedTab == CreativeModeTabs.searchTab();
	}

	private static boolean isRecipeBookSearchActive(AbstractRecipeBookScreen<?> screen) {
		RecipeBookComponent<?> recipeBook = readField(RECIPE_BOOK_COMPONENT, screen, RecipeBookComponent.class);
		if (recipeBook == null || !recipeBook.isVisible()) {
			return false;
		}
		EditBox searchBox = readField(RECIPE_BOOK_SEARCH_BOX, recipeBook, EditBox.class);
		return searchBox != null && searchBox.isFocused();
	}

	private static Field findField(Class<?> type, String name) {
		try {
			Field field = type.getDeclaredField(name);
			field.setAccessible(true);
			return field;
		} catch (ReflectiveOperationException exception) {
			return null;
		}
	}

	private static <T> T readField(Field field, Object instance, Class<T> type) {
		if (field == null) {
			return null;
		}
		try {
			return type.cast(field.get(instance));
		} catch (ReflectiveOperationException exception) {
			return null;
		}
	}
}
