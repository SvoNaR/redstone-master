package ru.redstonemaster.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import ru.redstonemaster.client.gui.settings.ModSetting;
import ru.redstonemaster.config.ModConfig;
import ru.redstonemaster.config.ModContentLanguage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class RedstoneMasterSettingsPanel {
	static final int SEARCH_HEIGHT = 20;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_GAP = 2;
	private static final int SEARCH_BOTTOM_GAP = 8;
	private static final int DISCLAIMER_GAP = 4;
	private static final String DISCLAIMER_KEY = "gui.redstone-master.settings.disclaimer";
	private static final String INTERFACE_SECTION_KEY = "gui.redstone-master.settings.section.interface";
	private static final String CONTROLS_SECTION_KEY = "gui.redstone-master.settings.section.controls";
	private static final String TUTORIAL_SECTION_KEY = "gui.redstone-master.settings.section.tutorial";
	private static final int VALUE_BUTTON_WIDTH = 110;
	private static final int RESET_BUTTON_MIN_WIDTH = 44;
	private static final int RESET_BUTTON_PADDING = 8;
	private static final int RESET_BUTTON_GAP = 2;
	private static final int FOOTER_GAP = 8;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int SECTION_COLOR = 0xFFE0E0E0;
	private static final int DISCLAIMER_COLOR = 0xFFBBBBBB;

	private final RedstoneMasterScreen screen;
	private EditBox searchBox;
	private String searchQuery = "";
	private int scrollOffset;
	private final List<LayoutRow> layoutRows = new ArrayList<>();
	private final List<Button> valueButtons = new ArrayList<>();
	private final List<Button> resetButtons = new ArrayList<>();
	private Button resetAllButton;

	RedstoneMasterSettingsPanel(RedstoneMasterScreen screen) {
		this.screen = screen;
	}

	void rebuildWidgets() {
		this.layoutRows.clear();
		this.valueButtons.clear();
		this.resetButtons.clear();
		this.resetAllButton = null;

		int innerX = this.screen.getContentX() + RedstoneMasterScreen.CONTENT_INNER_PADDING;
		int innerWidth = this.screen.getContentWidth() - RedstoneMasterScreen.CONTENT_INNER_PADDING * 2;
		int searchY = this.getSearchY();
		int searchHeight = this.getSearchHeight();
		int y = this.getListTop();
		int disclaimerHeight = this.getDisclaimerHeight(innerWidth);
		this.layoutRows.add(LayoutRow.disclaimer(y, disclaimerHeight));
		y += disclaimerHeight + DISCLAIMER_GAP;

		int resetButtonWidth = this.getResetButtonWidth();
		int controlsWidth = resetButtonWidth + RESET_BUTTON_GAP;
		int valueButtonWidth = Math.min(
				VALUE_BUTTON_WIDTH,
				Math.max(48, innerWidth - controlsWidth - 80)
		);
		controlsWidth += valueButtonWidth;
		int valueButtonX = innerX + innerWidth - controlsWidth;
		int resetButtonX = valueButtonX + valueButtonWidth + RESET_BUTTON_GAP;
		int labelWidth = innerWidth - controlsWidth - 12;
		Set<String> addedSections = new LinkedHashSet<>();

		for (ModSetting setting : ModSetting.values()) {
			if (!setting.matchesSearch(this.searchQuery)) {
				continue;
			}
			if (addedSections.add(setting.getSectionKey())) {
				if (INTERFACE_SECTION_KEY.equals(setting.getSectionKey())
						|| CONTROLS_SECTION_KEY.equals(setting.getSectionKey())
						|| TUTORIAL_SECTION_KEY.equals(setting.getSectionKey())) {
					y += this.screen.getFont().lineHeight;
				}
				this.layoutRows.add(LayoutRow.section(setting.getSectionKey(), y));
				y += this.screen.getFont().lineHeight * 2;
			}

			int lineCount = this.getSettingLineCount(setting, labelWidth);
			int rowHeight = this.getSettingRowHeight(lineCount);
			int buttonY = this.getSettingButtonY(y, rowHeight);
			this.layoutRows.add(LayoutRow.setting(setting, y, rowHeight));
			Button valueButton = this.createValueButton(setting, valueButtonX, buttonY, valueButtonWidth);
			this.valueButtons.add(valueButton);
			this.screen.addContentWidget(valueButton);
			Button resetButton = this.createResetButton(setting, resetButtonX, buttonY, resetButtonWidth);
			this.resetButtons.add(resetButton);
			this.screen.addContentWidget(resetButton);
			y += rowHeight + ROW_GAP;
		}

		y += FOOTER_GAP;
		int resetAllY = y;
		this.layoutRows.add(LayoutRow.resetAll(resetAllY, ROW_HEIGHT));
		int resetAllButtonY = this.getSettingButtonY(resetAllY, ROW_HEIGHT);
		ModConfig config = ModConfig.get();
		this.resetAllButton = Button.builder(
						ModContentLanguage.translatable("gui.redstone-master.settings.reset_all"),
						button -> this.openResetAllConfirm())
				.bounds(innerX, resetAllButtonY, innerWidth, ROW_HEIGHT)
				.build();
		this.resetAllButton.active = !config.areAllModSettingsAtDefault();
		this.screen.addContentWidget(this.resetAllButton);

		this.searchBox = new EditBox(
				this.screen.getFont(),
				innerX,
				searchY,
				innerWidth,
				searchHeight,
				ModContentLanguage.translatable("gui.redstone-master.settings.search_hint")
		);
		this.searchBox.setMaxLength(64);
		this.searchBox.setHint(ModContentLanguage.translatable("gui.redstone-master.settings.search_hint"));
		this.searchBox.setValue(this.searchQuery);
		this.searchBox.setResponder(value -> {
			this.searchQuery = value;
			this.screen.rebuildSettingsWidgets();
		});
		this.screen.addContentWidget(this.searchBox);

		this.clampScrollOffset();
		this.applyScrollToControls();
	}

	private int getResetButtonWidth() {
		int textWidth = this.screen.getFont()
				.width(ModContentLanguage.get("gui.redstone-master.settings.reset"));
		return Math.max(RESET_BUTTON_MIN_WIDTH, textWidth + RESET_BUTTON_PADDING);
	}

	private Button createResetButton(ModSetting setting, int x, int y, int width) {
		ModConfig config = ModConfig.get();
		Button button = Button.builder(
						ModContentLanguage.translatable("gui.redstone-master.settings.reset"),
						b -> this.resetSetting(setting))
				.bounds(x, y, width, ROW_HEIGHT)
				.tooltip(Tooltip.create(
						ModContentLanguage.translatable("gui.redstone-master.settings.reset.tooltip")))
				.build();
		button.active = !config.isSettingAtDefault(setting);
		return button;
	}

	private void resetSetting(ModSetting setting) {
		ModConfig config = ModConfig.get();
		config.resetSetting(setting);
		config.save();
		switch (setting) {
			case PANEL_SCALE, HIGH_CONTRAST -> this.screen.rebuildAllWidgets();
			case AUTO_LANGUAGE, MANUAL_LANGUAGE -> this.onLanguageChanged();
			default -> this.screen.rebuildSettingsWidgets();
		}
	}

	private void openResetAllConfirm() {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.setScreen(new ConfirmScreen(
				confirmed -> {
					if (confirmed) {
						ModConfig.get().resetAllModSettings();
						ModConfig.get().save();
						ModContentLanguage.clearCache();
						this.screen.rebuildAllWidgets();
					}
					minecraft.setScreen(this.screen);
				},
				ModContentLanguage.translatable("gui.redstone-master.settings.reset_all.title"),
				ModContentLanguage.translatable("gui.redstone-master.settings.reset_all.message"),
				ModContentLanguage.translatable("gui.redstone-master.settings.reset_all.confirm"),
				ModContentLanguage.translatable("gui.redstone-master.settings.reset_all.cancel")
		));
	}

	private void onLanguageChanged() {
		ModContentLanguage.clearCache();
		this.screen.rebuildAllWidgets();
	}

	private Button createValueButton(ModSetting setting, int x, int y, int width) {
		ModConfig config = ModConfig.get();
		return switch (setting) {
			case PANEL_SCALE -> Button.builder(
							this.getPanelScaleLabel(config),
							button -> {
								config.cyclePanelScale();
								this.screen.rebuildAllWidgets();
							})
					.bounds(x, y, width, ROW_HEIGHT)
					.build();
			case PAUSE_ON_OPEN -> Button.builder(
							this.getToggleLabel(config.pauseOnOpen),
							button -> {
								config.pauseOnOpen = !config.pauseOnOpen;
								config.save();
								this.screen.rebuildSettingsWidgets();
							})
					.bounds(x, y, width, ROW_HEIGHT)
					.tooltip(Tooltip.create(
							ModContentLanguage.translatable("gui.redstone-master.settings.pause_on_open.tooltip")))
					.build();
			case HIGH_CONTRAST -> Button.builder(
							this.getToggleLabel(config.highContrastBorders),
							button -> {
								config.highContrastBorders = !config.highContrastBorders;
								config.save();
								this.screen.rebuildAllWidgets();
							})
					.bounds(x, y, width, ROW_HEIGHT)
					.build();
			case AUTO_LANGUAGE -> Button.builder(
							this.getToggleLabel(config.autoLanguage),
							button -> {
								config.autoLanguage = !config.autoLanguage;
								if (!config.autoLanguage) {
									config.syncManualLanguageFromMinecraft();
								}
								config.save();
								this.onLanguageChanged();
							})
					.bounds(x, y, width, ROW_HEIGHT)
					.tooltip(Tooltip.create(
							ModContentLanguage.translatable("gui.redstone-master.settings.auto_language.tooltip")))
					.build();
			case REMEMBER_SESSION -> Button.builder(
							this.getToggleLabel(config.rememberSession),
							button -> {
								config.rememberSession = !config.rememberSession;
								config.save();
								this.screen.rebuildSettingsWidgets();
							})
					.bounds(x, y, width, ROW_HEIGHT)
					.tooltip(Tooltip.create(
							ModContentLanguage.translatable("gui.redstone-master.settings.remember_session.tooltip")))
					.build();
			case MANUAL_LANGUAGE -> {
				Component label = config.autoLanguage
						? ModContentLanguage.getMinecraftLanguageDisplayName()
						: ModContentLanguage.getLanguageDisplayName(config.manualLanguage);
				Button button = Button.builder(label, b -> {
							if (!ModConfig.get().autoLanguage) {
								ModConfig.get().cycleManualLanguage();
								this.onLanguageChanged();
							}
						})
						.bounds(x, y, width, ROW_HEIGHT)
						.build();
				button.active = !config.autoLanguage;
				yield button;
			}
			case CLOSE_ON_REPEAT -> Button.builder(
							this.getToggleLabel(config.closeOnRepeatKey),
							button -> {
								config.closeOnRepeatKey = !config.closeOnRepeatKey;
								config.save();
								this.screen.rebuildSettingsWidgets();
							})
					.bounds(x, y, width, ROW_HEIGHT)
					.tooltip(Tooltip.create(
							ModContentLanguage.translatable("gui.redstone-master.settings.close_on_repeat.tooltip")))
					.build();
			case TUTORIAL_COLLAPSE_OTHERS -> Button.builder(
							this.getToggleLabel(config.tutorialCollapseOtherSections),
							button -> {
								config.tutorialCollapseOtherSections = !config.tutorialCollapseOtherSections;
								config.save();
								this.screen.rebuildSettingsWidgets();
							})
					.bounds(x, y, width, ROW_HEIGHT)
					.tooltip(Tooltip.create(ModContentLanguage.translatable(
							"gui.redstone-master.settings.tutorial_collapse_other.tooltip")))
					.build();
		};
	}

	private Component getPanelScaleLabel(ModConfig config) {
		int percent = (int) Math.round(config.panelScale * 100);
		return ModContentLanguage.translatable("gui.redstone-master.settings.panel_scale.value", percent);
	}

	private Component getToggleLabel(boolean enabled) {
		return ModContentLanguage.translatable(
				enabled ? "gui.redstone-master.settings.value.on" : "gui.redstone-master.settings.value.off"
		);
	}

	private int getDisclaimerHeight(int width) {
		return this.getDisclaimerLineCount(width) * this.screen.getFont().lineHeight;
	}

	private int getDisclaimerLineCount(int width) {
		return this.screen.getFont()
				.split(ModContentLanguage.translatable(DISCLAIMER_KEY), width)
				.size();
	}

	private int getSettingLineCount(ModSetting setting, int labelWidth) {
		String bulletName = "• " + ModContentLanguage.get(setting.getNameKey());
		return this.screen.getFont().split(Component.literal(bulletName), labelWidth).size();
	}

	private int getSettingRowHeight(int lineCount) {
		int textHeight = lineCount * this.screen.getFont().lineHeight;
		return Math.max(ROW_HEIGHT, textHeight);
	}

	private int getSettingButtonY(int rowY, int rowHeight) {
		return rowY + (rowHeight - ROW_HEIGHT) / 2;
	}

	private int getSettingLabelY(int rowY, int rowHeight, int lineCount) {
		int textHeight = lineCount * this.screen.getFont().lineHeight;
		return rowY + Math.max(0, (rowHeight - textHeight) / 2);
	}

	void renderLabels(GuiGraphics graphics) {
		int labelX = this.screen.getContentX() + RedstoneMasterScreen.CONTENT_INNER_PADDING + 4;
		int innerListWidth = this.screen.getContentWidth() - RedstoneMasterScreen.CONTENT_INNER_PADDING * 2;
		int resetButtonWidth = this.getResetButtonWidth();
		int controlsWidth = resetButtonWidth + RESET_BUTTON_GAP
				+ Math.min(VALUE_BUTTON_WIDTH, Math.max(48, innerListWidth - resetButtonWidth - RESET_BUTTON_GAP - 80));
		int labelWidth = innerListWidth - controlsWidth - 12;
		int listTop = this.getListTop();
		int contentBottom = this.getContentBottom();

		graphics.enableScissor(
				this.screen.getContentX() + 1,
				listTop,
				this.screen.getContentX() + this.screen.getContentWidth() - 1,
				contentBottom
		);

		int disclaimerWidth = this.screen.getContentWidth() - RedstoneMasterScreen.CONTENT_INNER_PADDING * 2;

		for (LayoutRow row : this.layoutRows) {
			if (row.isDisclaimer) {
				int drawY = row.y - this.scrollOffset;
				List<net.minecraft.util.FormattedCharSequence> lines = this.screen.getFont()
						.split(ModContentLanguage.translatable(DISCLAIMER_KEY), disclaimerWidth);
				int lineY = drawY;
				for (var line : lines) {
					if (lineY + this.screen.getFont().lineHeight >= listTop && lineY <= contentBottom) {
						graphics.drawString(this.screen.getFont(), line, labelX, lineY, DISCLAIMER_COLOR, true);
					}
					lineY += this.screen.getFont().lineHeight;
				}
			} else if (row.isSection) {
				int drawY = row.y - this.scrollOffset;
				if (drawY + this.screen.getFont().lineHeight >= listTop && drawY <= contentBottom) {
					graphics.drawString(
							this.screen.getFont(),
							ModContentLanguage.translatable(row.sectionKey),
							labelX,
							drawY,
							SECTION_COLOR,
							true
					);
				}
			} else if (row.setting != null) {
				int drawY = row.y - this.scrollOffset;
				String bulletName = "• " + ModContentLanguage.get(row.setting.getNameKey());
				List<net.minecraft.util.FormattedCharSequence> lines = this.screen.getFont()
						.split(Component.literal(bulletName), labelWidth);
				int lineY = this.getSettingLabelY(drawY, row.rowHeight, lines.size());
				for (var line : lines) {
					if (lineY + this.screen.getFont().lineHeight >= listTop && lineY <= contentBottom) {
						graphics.drawString(this.screen.getFont(), line, labelX, lineY, TEXT_COLOR, true);
					}
					lineY += this.screen.getFont().lineHeight;
				}
			}
		}

		graphics.disableScissor();
	}

	boolean mouseScrolled(double scrollX, double scrollY) {
		int maxScroll = this.getMaxScroll();
		if (maxScroll <= 0) {
			return false;
		}
		this.scrollOffset = (int) Math.clamp(this.scrollOffset - scrollY * 12, 0, maxScroll);
		this.applyScrollToControls();
		return true;
	}

	private void applyScrollToControls() {
		int listTop = this.getListTop();
		int contentBottom = this.getContentBottom();
		int settingIndex = 0;

		for (LayoutRow row : this.layoutRows) {
			if (row.isResetAll) {
				if (this.resetAllButton != null) {
					int displayY = this.getSettingButtonY(row.y - this.scrollOffset, row.rowHeight);
					this.resetAllButton.setY(displayY);
					this.resetAllButton.visible = displayY >= listTop - 1
							&& displayY + ROW_HEIGHT <= contentBottom + 1;
				}
				continue;
			}
			if (row.setting == null) {
				continue;
			}
			int displayY = this.getSettingButtonY(row.y - this.scrollOffset, row.rowHeight);
			boolean visible = displayY >= listTop - 1 && displayY + ROW_HEIGHT <= contentBottom + 1;
			if (settingIndex < this.valueButtons.size()) {
				Button valueButton = this.valueButtons.get(settingIndex);
				valueButton.setY(displayY);
				valueButton.visible = visible;
			}
			if (settingIndex < this.resetButtons.size()) {
				Button resetButton = this.resetButtons.get(settingIndex);
				resetButton.setY(displayY);
				resetButton.visible = visible;
			}
			settingIndex++;
		}
	}

	private void clampScrollOffset() {
		this.scrollOffset = (int) Math.clamp(this.scrollOffset, 0, this.getMaxScroll());
	}

	private int getMaxScroll() {
		return Math.max(0, this.getListContentHeight() - this.getListViewHeight());
	}

	int getSearchY() {
		return this.screen.getContentY() + RedstoneMasterScreen.CONTENT_INNER_PADDING;
	}

	int getSearchHeight() {
		return Math.max(SEARCH_HEIGHT, this.screen.getFont().lineHeight + 10);
	}

	int getListTop() {
		return this.getSearchY() + this.getSearchHeight() + SEARCH_BOTTOM_GAP;
	}

	private int getContentBottom() {
		return this.screen.getContentY() + this.screen.getContentHeight() - RedstoneMasterScreen.CONTENT_INNER_PADDING;
	}

	private int getListViewHeight() {
		return this.getContentBottom() - this.getListTop();
	}

	private int getListContentHeight() {
		if (this.layoutRows.isEmpty()) {
			return 0;
		}
		LayoutRow last = this.layoutRows.get(this.layoutRows.size() - 1);
		return last.y + last.rowHeight - this.getListTop();
	}

	int getContentClipTop() {
		return this.screen.getContentY() + 1;
	}

	int getContentClipBottom() {
		return this.getContentBottom();
	}

	int getScrollOffset() {
		return this.scrollOffset;
	}

	void setScrollOffset(int scrollOffset) {
		this.scrollOffset = Math.max(0, scrollOffset);
	}

	void dispose() {
		this.searchBox = null;
		this.resetAllButton = null;
		this.layoutRows.clear();
		this.valueButtons.clear();
		this.resetButtons.clear();
	}

	private static final class LayoutRow {
		private final boolean isSection;
		private final boolean isDisclaimer;
		private final boolean isResetAll;
		private final String sectionKey;
		private final ModSetting setting;
		private final int y;
		private final int rowHeight;

		private LayoutRow(
				boolean isSection,
				boolean isDisclaimer,
				boolean isResetAll,
				String sectionKey,
				ModSetting setting,
				int y,
				int rowHeight
		) {
			this.isSection = isSection;
			this.isDisclaimer = isDisclaimer;
			this.isResetAll = isResetAll;
			this.sectionKey = sectionKey;
			this.setting = setting;
			this.y = y;
			this.rowHeight = rowHeight;
		}

		static LayoutRow section(String sectionKey, int y) {
			return new LayoutRow(true, false, false, sectionKey, null, y, 0);
		}

		static LayoutRow disclaimer(int y, int rowHeight) {
			return new LayoutRow(false, true, false, null, null, y, rowHeight);
		}

		static LayoutRow setting(ModSetting setting, int y, int rowHeight) {
			return new LayoutRow(false, false, false, null, setting, y, rowHeight);
		}

		static LayoutRow resetAll(int y, int rowHeight) {
			return new LayoutRow(false, false, true, null, null, y, rowHeight);
		}
	}
}
