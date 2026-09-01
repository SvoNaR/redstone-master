package ru.redstonemaster.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.redstonemaster.config.ModContentLanguage;

import java.util.function.Consumer;

final class ModSearchEditBox {
	private ModSearchEditBox() {
	}

	static EditBox create(
			Font font,
			int x,
			int y,
			int width,
			int height,
			String hintKey,
			String currentValue,
			Consumer<String> onValueChange
	) {
		Component hint = styledHint(hintKey);
		EditBox searchBox = new EditBox(font, x, y, width, height, hint);
		searchBox.setMaxLength(64);
		searchBox.setHint(hint);
		searchBox.setValue(currentValue);
		searchBox.setResponder(onValueChange);
		return searchBox;
	}

	static Component styledHint(String hintKey) {
		return ModContentLanguage.translatable(hintKey).copy().withStyle(EditBox.SEARCH_HINT_STYLE);
	}

	static void restoreFocus(EditBox searchBox, Screen screen, int cursorPosition) {
		if (searchBox == null) {
			return;
		}
		int position = Math.clamp(cursorPosition, 0, searchBox.getValue().length());
		searchBox.setFocused(true);
		screen.setFocused(searchBox);
		searchBox.setCursorPosition(position);
		searchBox.setHighlightPos(position);
	}

	static void updateBounds(EditBox searchBox, int x, int y, int width, int height) {
		if (searchBox == null) {
			return;
		}
		searchBox.setX(x);
		searchBox.setY(y);
		searchBox.setWidth(width);
		searchBox.setHeight(height);
	}
}
