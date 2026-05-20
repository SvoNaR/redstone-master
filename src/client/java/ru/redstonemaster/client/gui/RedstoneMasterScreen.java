package ru.redstonemaster.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class RedstoneMasterScreen extends Screen {
	private static final double PANEL_SCALE = 0.8;
	private static final int PANEL_PADDING = 8;
	private static final int HEADER_HEIGHT = 20;
	private static final int CLOSE_BUTTON_SIZE = 20;

	private RedstoneMasterTab currentTab = RedstoneMasterTab.WELCOME;

	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;

	public RedstoneMasterScreen() {
		super(Component.literal("Redstone Master"));
	}

	@Override
	protected void init() {
		this.updatePanelBounds();
		this.rebuildHeaderButtons();
	}

	private void updatePanelBounds() {
		this.panelWidth = (int) (this.width * PANEL_SCALE);
		this.panelHeight = (int) (this.height * PANEL_SCALE);
		this.panelX = (this.width - this.panelWidth) / 2;
		this.panelY = (this.height - this.panelHeight) / 2;
	}

	private void rebuildHeaderButtons() {
		this.clearWidgets();

		int headerY = this.panelY + PANEL_PADDING;
		int tabsAreaWidth = this.panelWidth - PANEL_PADDING * 2 - CLOSE_BUTTON_SIZE - 4;
		int tabWidth = tabsAreaWidth / 3;
		int tabX = this.panelX + PANEL_PADDING;

		this.addRenderableWidget(Button.builder(
						Component.translatable("gui.redstone_master.tab.tutorial"),
						button -> this.selectTab(RedstoneMasterTab.TUTORIAL))
				.bounds(tabX, headerY, tabWidth, HEADER_HEIGHT)
				.build());

		this.addRenderableWidget(Button.builder(
						Component.translatable("gui.redstone_master.tab.about_author"),
						button -> this.selectTab(RedstoneMasterTab.ABOUT_AUTHOR))
				.bounds(tabX + tabWidth, headerY, tabWidth, HEADER_HEIGHT)
				.build());

		this.addRenderableWidget(Button.builder(
						Component.translatable("gui.redstone_master.tab.about_mod"),
						button -> this.selectTab(RedstoneMasterTab.ABOUT_MOD))
				.bounds(tabX + tabWidth * 2, headerY, tabWidth, HEADER_HEIGHT)
				.build());

		int closeX = this.panelX + this.panelWidth - PANEL_PADDING - CLOSE_BUTTON_SIZE;
		this.addRenderableWidget(Button.builder(
						Component.literal("X"),
						button -> this.onClose())
				.bounds(closeX, headerY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
				.build());
	}

	private void selectTab(RedstoneMasterTab tab) {
		this.currentTab = tab;
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		this.updatePanelBounds();
		this.rebuildHeaderButtons();
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		// Оставляем 20% экрана прозрачными — виден мир или фон главного меню.
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		this.renderMenuBackground(graphics, this.panelX, this.panelY, this.panelWidth, this.panelHeight);
		super.render(graphics, mouseX, mouseY, delta);
		this.renderContent(graphics);
	}

	private void renderContent(GuiGraphics graphics) {
		int contentX = this.panelX + PANEL_PADDING + 4;
		int contentY = this.panelY + PANEL_PADDING + HEADER_HEIGHT + 12;
		int contentWidth = this.panelWidth - (PANEL_PADDING + 4) * 2;

		List<FormattedCharSequence> lines = this.font.split(this.currentTab.getContent(), contentWidth);
		for (FormattedCharSequence line : lines) {
			graphics.drawString(this.font, line, contentX, contentY, 0xFFFFFF, true);
			contentY += this.font.lineHeight;
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
