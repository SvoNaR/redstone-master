package ru.redstonemaster.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class RedstoneMasterScreen extends Screen {
	private static final double PANEL_SCALE = 0.8;
	private static final int PANEL_PADDING = 8;
	private static final int NAV_BAR_HEIGHT = 20;
	private static final int ICON_BUTTON_SIZE = 20;
	private static final int CLOSE_BUTTON_GAP = 2;
	private static final int TITLE_TOP_PADDING = 4;
	private static final int TITLE_TO_NAV_GAP = 2;
	private static final int NAV_DOWN_OFFSET = 4;
	private static final int CONTENT_TOP_GAP = 6;
	/** Цвета в формате ARGB: без альфа-канала (0x00FFFFFF) текст не виден. */
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int LINE_COLOR = 0xFF000000;
	private static final int TITLE_COLOR = 0xFF800020;
	private static final float TITLE_SCALE = 1.5f;

	private RedstoneMasterTab currentTab = RedstoneMasterTab.MAIN_MENU;

	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;
	private int titleY;
	private int navY;
	private int separatorY;
	private int contentX;
	private int contentY;
	private int contentWidth;
	private int contentHeight;

	public RedstoneMasterScreen() {
		super(Component.translatable("gui.redstone_master.title"));
	}

	@Override
	protected void init() {
		this.updatePanelBounds();
		this.rebuildNavigation();
	}

	private void updatePanelBounds() {
		this.panelWidth = (int) (this.width * PANEL_SCALE);
		this.panelHeight = (int) (this.height * PANEL_SCALE);
		this.panelX = (this.width - this.panelWidth) / 2;
		this.panelY = (this.height - this.panelHeight) / 2;

		int innerX = this.panelX + PANEL_PADDING;
		int innerWidth = this.panelWidth - PANEL_PADDING * 2;

		this.titleY = this.panelY + PANEL_PADDING + TITLE_TOP_PADDING;
		int titleHeight = this.getScaledTitleHeight();
		this.separatorY = this.titleY + titleHeight + 2;
		this.navY = this.titleY + titleHeight + TITLE_TO_NAV_GAP + NAV_DOWN_OFFSET;
		this.contentX = innerX;
		this.contentY = this.navY + NAV_BAR_HEIGHT + CONTENT_TOP_GAP;
		this.contentWidth = innerWidth;
		this.contentHeight = this.panelY + this.panelHeight - PANEL_PADDING - this.contentY;
	}

	private void rebuildNavigation() {
		this.clearWidgets();

		int innerX = this.panelX + PANEL_PADDING;
		int innerWidth = this.panelWidth - PANEL_PADDING * 2;

		int closeAreaWidth = ICON_BUTTON_SIZE + CLOSE_BUTTON_GAP;
		int navButtonWidth = (innerWidth - closeAreaWidth) / 4;

		this.addRenderableWidget(Button.builder(
						Component.translatable("gui.redstone_master.tab.main_menu"),
						button -> this.selectTab(RedstoneMasterTab.MAIN_MENU))
				.bounds(innerX, this.navY, navButtonWidth, NAV_BAR_HEIGHT)
				.build());

		this.addRenderableWidget(Button.builder(
						Component.translatable("gui.redstone_master.tab.tutorial"),
						button -> this.selectTab(RedstoneMasterTab.TUTORIAL))
				.bounds(innerX + navButtonWidth, this.navY, navButtonWidth, NAV_BAR_HEIGHT)
				.build());

		this.addRenderableWidget(Button.builder(
						Component.translatable("gui.redstone_master.tab.settings"),
						button -> this.selectTab(RedstoneMasterTab.SETTINGS))
				.bounds(innerX + navButtonWidth * 2, this.navY, navButtonWidth, NAV_BAR_HEIGHT)
				.build());

		this.addRenderableWidget(Button.builder(
						Component.translatable("gui.redstone_master.search"),
						button -> this.selectTab(RedstoneMasterTab.SEARCH))
				.bounds(innerX + navButtonWidth * 3, this.navY, navButtonWidth, NAV_BAR_HEIGHT)
				.build());

		int closeX = innerX + navButtonWidth * 4 + CLOSE_BUTTON_GAP;
		this.addRenderableWidget(Button.builder(
						Component.literal("X"),
						button -> this.onClose())
				.bounds(closeX, this.navY, ICON_BUTTON_SIZE, ICON_BUTTON_SIZE)
				.tooltip(Tooltip.create(Component.translatable("gui.redstone_master.close")))
				.build());
	}

	private void selectTab(RedstoneMasterTab tab) {
		this.currentTab = tab;
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		this.updatePanelBounds();
		this.rebuildNavigation();
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		// Оставляем 20% экрана прозрачными — виден мир или фон главного меню.
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		this.renderMenuBackground(graphics, this.panelX, this.panelY, this.panelWidth, this.panelHeight);
		super.render(graphics, mouseX, mouseY, delta);
		this.renderDecorations(graphics);
		this.renderContent(graphics);
	}

	private int getScaledTitleHeight() {
		return (int) Math.ceil(this.font.lineHeight * TITLE_SCALE);
	}

	private Component getTitleComponent() {
		return Component.translatable("gui.redstone_master.title")
				.withStyle(Style.EMPTY.withBold(true));
	}

	private void renderTitle(GuiGraphics graphics) {
		int centerX = this.panelX + this.panelWidth / 2;
		float anchorY = this.titleY + this.font.lineHeight / 2.0f;

		var pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(centerX, anchorY);
		pose.scale(TITLE_SCALE, TITLE_SCALE);
		graphics.drawCenteredString(
				this.font,
				this.getTitleComponent(),
				0,
				(int) (-this.font.lineHeight / 2.0f),
				TITLE_COLOR
		);
		pose.popMatrix();
	}

	private void renderDecorations(GuiGraphics graphics) {
		graphics.renderOutline(this.panelX, this.panelY, this.panelWidth, this.panelHeight, LINE_COLOR);
		this.renderTitle(graphics);

		graphics.hLine(
				this.panelX + PANEL_PADDING,
				this.panelX + this.panelWidth - PANEL_PADDING,
				this.separatorY,
				LINE_COLOR
		);

		graphics.renderOutline(this.contentX, this.contentY, this.contentWidth, this.contentHeight, LINE_COLOR);
	}

	private void renderContent(GuiGraphics graphics) {
		int textX = this.contentX + 6;
		int textY = this.contentY + 6;
		int textWidth = this.contentWidth - 12;

		List<FormattedCharSequence> lines = this.font.split(this.currentTab.getContent(), textWidth);
		for (FormattedCharSequence line : lines) {
			graphics.drawString(this.font, line, textX, textY, TEXT_COLOR, true);
			textY += this.font.lineHeight;
		}
	}

	@Override
	public boolean isPauseScreen() {
		return this.minecraft != null && this.minecraft.isSingleplayer();
	}
}
