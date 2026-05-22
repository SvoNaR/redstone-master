package ru.redstonemaster.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import ru.redstonemaster.RedstoneMasterClient;
import ru.redstonemaster.config.ModConfig;
import ru.redstonemaster.config.ModContentLanguage;

import java.util.List;

public class RedstoneMasterScreen extends Screen {
	public static final int CONTENT_INNER_PADDING = 6;
	private static final int PANEL_PADDING = 8;
	private static final int NAV_BAR_HEIGHT = 20;
	private static final int ICON_BUTTON_SIZE = 20;
	private static final int CLOSE_BUTTON_GAP = 2;
	private static final int TITLE_TOP_PADDING = 4;
	private static final int TITLE_TO_NAV_GAP = 2;
	private static final int NAV_DOWN_OFFSET = 4;
	private static final int CONTENT_TOP_GAP = 10;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int IMAGE_COLOR = 0xFFFFFFFF;
	private static final int LINE_COLOR_NORMAL = 0xFF000000;
	private static final int LINE_COLOR_HIGH_CONTRAST = 0xFFFFFFFF;
	private static final int TITLE_COLOR = 0xFF800020;
	private static final float TITLE_SCALE = 1.5f;
	private static final Identifier MAIN_MENU_PHOTO = Identifier.fromNamespaceAndPath(
			"redstone_master", "textures/gui/photo1_main_menu.png");
	private static final int MAIN_MENU_PHOTO_SIZE = 1024;
	private static final int TEXT_TO_PHOTO_GAP = 8;

	private RedstoneMasterTab currentTab = RedstoneMasterTab.MAIN_MENU;
	private final RedstoneMasterSettingsPanel settingsPanel = new RedstoneMasterSettingsPanel(this);
	private boolean sessionRestored;

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
		super(ModContentLanguage.translatable("gui.redstone_master.title"));
	}

	@Override
	protected void init() {
		if (ModConfig.get().initializeLanguageOnFirstOpen()) {
			ModContentLanguage.clearCache();
		}
		if (!this.sessionRestored) {
			this.restoreSessionState();
			this.sessionRestored = true;
		}
		this.rebuildAllWidgets();
	}

	@Override
	public void onClose() {
		this.persistSessionState();
		super.onClose();
	}

	private void restoreSessionState() {
		ModConfig config = ModConfig.get();
		if (!config.rememberSession) {
			this.currentTab = RedstoneMasterTab.MAIN_MENU;
			this.settingsPanel.setScrollOffset(0);
			return;
		}
		RedstoneMasterTab savedTab = RedstoneMasterTab.fromName(config.lastTab);
		this.currentTab = savedTab != null ? savedTab : RedstoneMasterTab.MAIN_MENU;
		this.settingsPanel.setScrollOffset(config.settingsScrollOffset);
	}

	private void persistSessionState() {
		ModConfig config = ModConfig.get();
		if (!config.rememberSession) {
			return;
		}
		config.lastTab = this.currentTab.name();
		config.settingsScrollOffset = this.settingsPanel.getScrollOffset();
		config.save();
	}

	void rebuildAllWidgets() {
		this.updatePanelBounds();
		this.rebuildNavigation();
	}

	void rebuildSettingsWidgets() {
		this.rebuildNavigation();
	}

	private void updatePanelBounds() {
		double panelScale = ModConfig.get().panelScale;
		this.panelWidth = (int) (this.width * panelScale);
		this.panelHeight = (int) (this.height * panelScale);
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
		this.settingsPanel.dispose();

		int innerX = this.panelX + PANEL_PADDING;
		int innerWidth = this.panelWidth - PANEL_PADDING * 2;

		int closeAreaWidth = ICON_BUTTON_SIZE + CLOSE_BUTTON_GAP;
		int navButtonWidth = (innerWidth - closeAreaWidth) / 4;

		this.addRenderableWidget(Button.builder(
						ModContentLanguage.translatable("gui.redstone_master.tab.main_menu"),
						button -> this.selectTab(RedstoneMasterTab.MAIN_MENU))
				.bounds(innerX, this.navY, navButtonWidth, NAV_BAR_HEIGHT)
				.build());

		this.addRenderableWidget(Button.builder(
						ModContentLanguage.translatable("gui.redstone_master.tab.tutorial"),
						button -> this.selectTab(RedstoneMasterTab.TUTORIAL))
				.bounds(innerX + navButtonWidth, this.navY, navButtonWidth, NAV_BAR_HEIGHT)
				.build());

		this.addRenderableWidget(Button.builder(
						ModContentLanguage.translatable("gui.redstone_master.tab.settings"),
						button -> this.selectTab(RedstoneMasterTab.SETTINGS))
				.bounds(innerX + navButtonWidth * 2, this.navY, navButtonWidth, NAV_BAR_HEIGHT)
				.build());

		this.addRenderableWidget(Button.builder(
						ModContentLanguage.translatable("gui.redstone_master.tab.profile"),
						button -> this.selectTab(RedstoneMasterTab.PROFILE))
				.bounds(innerX + navButtonWidth * 3, this.navY, navButtonWidth, NAV_BAR_HEIGHT)
				.build());

		int closeX = innerX + navButtonWidth * 4 + CLOSE_BUTTON_GAP;
		this.addRenderableWidget(Button.builder(
						Component.literal("X"),
						button -> this.onClose())
				.bounds(closeX, this.navY, ICON_BUTTON_SIZE, ICON_BUTTON_SIZE)
				.tooltip(Tooltip.create(ModContentLanguage.translatable("gui.redstone_master.close")))
				.build());

		if (this.currentTab == RedstoneMasterTab.SETTINGS) {
			this.settingsPanel.rebuildWidgets();
		}
	}

	private void selectTab(RedstoneMasterTab tab) {
		if (this.currentTab == RedstoneMasterTab.SETTINGS && tab != RedstoneMasterTab.SETTINGS) {
			this.persistSettingsScroll();
		}
		this.currentTab = tab;
		this.rebuildNavigation();
	}

	private void persistSettingsScroll() {
		if (ModConfig.get().rememberSession) {
			ModConfig.get().settingsScrollOffset = this.settingsPanel.getScrollOffset();
		}
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		this.rebuildAllWidgets();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (RedstoneMasterClient.openGuiKey.matches(event) && ModConfig.get().closeOnRepeatKey) {
			this.onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.currentTab == RedstoneMasterTab.SETTINGS && this.settingsPanel.mouseScrolled(scrollX, scrollY)) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
		if (this.currentTab == RedstoneMasterTab.SETTINGS) {
			this.settingsPanel.renderLabels(graphics);
		}
	}

	private int getLineColor() {
		return ModConfig.get().highContrastBorders ? LINE_COLOR_HIGH_CONTRAST : LINE_COLOR_NORMAL;
	}

	private int getScaledTitleHeight() {
		return (int) Math.ceil(this.font.lineHeight * TITLE_SCALE);
	}

	private Component getTitleComponent() {
		return Component.literal(ModContentLanguage.get("gui.redstone_master.title"))
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
		int lineColor = this.getLineColor();
		graphics.renderOutline(this.panelX, this.panelY, this.panelWidth, this.panelHeight, lineColor);
		this.renderTitle(graphics);

		graphics.hLine(
				this.panelX + PANEL_PADDING,
				this.panelX + this.panelWidth - PANEL_PADDING,
				this.separatorY,
				lineColor
		);

		graphics.renderOutline(this.contentX, this.contentY, this.contentWidth, this.contentHeight, lineColor);
	}

	private void renderContent(GuiGraphics graphics) {
		switch (this.currentTab) {
			case MAIN_MENU -> this.renderMainMenuContent(graphics);
			case SETTINGS -> { /* подписи рисуются в settingsPanel.renderLabels */ }
			default -> this.renderTextContent(graphics, this.currentTab.getContent());
		}
	}

	private void renderMainMenuContent(GuiGraphics graphics) {
		int textX = this.contentX + CONTENT_INNER_PADDING;
		int textY = this.contentY + CONTENT_INNER_PADDING;
		int textWidth = this.contentWidth - CONTENT_INNER_PADDING * 2;

		textY = this.renderTextContentAt(graphics, RedstoneMasterTab.MAIN_MENU.getContent(), textX, textY, textWidth);
		textY += TEXT_TO_PHOTO_GAP;

		int photoAreaBottom = this.contentY + this.contentHeight - CONTENT_INNER_PADDING;
		int photoAreaHeight = photoAreaBottom - textY;
		int photoAreaWidth = this.contentWidth - CONTENT_INNER_PADDING * 2;

		if (photoAreaHeight <= 0 || photoAreaWidth <= 0) {
			return;
		}

		int photoSize = Math.min(photoAreaWidth, photoAreaHeight);
		int photoX = this.contentX + (this.contentWidth - photoSize) / 2;

		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				MAIN_MENU_PHOTO,
				photoX,
				textY,
				0.0f,
				0.0f,
				photoSize,
				photoSize,
				MAIN_MENU_PHOTO_SIZE,
				MAIN_MENU_PHOTO_SIZE,
				MAIN_MENU_PHOTO_SIZE,
				MAIN_MENU_PHOTO_SIZE,
				IMAGE_COLOR
		);
	}

	private void renderTextContent(GuiGraphics graphics, Component text) {
		this.renderTextContentAt(
				graphics,
				text,
				this.contentX + CONTENT_INNER_PADDING,
				this.contentY + CONTENT_INNER_PADDING,
				this.contentWidth - CONTENT_INNER_PADDING * 2
		);
	}

	private int renderTextContentAt(GuiGraphics graphics, Component text, int textX, int textY, int textWidth) {
		List<FormattedCharSequence> lines = this.font.split(text, textWidth);
		for (FormattedCharSequence line : lines) {
			graphics.drawString(this.font, line, textX, textY, TEXT_COLOR, true);
			textY += this.font.lineHeight;
		}
		return textY;
	}

	@Override
	public boolean isPauseScreen() {
		ModConfig config = ModConfig.get();
		return config.pauseOnOpen && this.minecraft != null && this.minecraft.isSingleplayer();
	}

	int getContentX() {
		return this.contentX;
	}

	int getContentY() {
		return this.contentY;
	}

	int getContentWidth() {
		return this.contentWidth;
	}

	int getContentHeight() {
		return this.contentHeight;
	}

	int getNavY() {
		return this.navY;
	}

	<T extends net.minecraft.client.gui.components.events.GuiEventListener & net.minecraft.client.gui.components.Renderable & net.minecraft.client.gui.narration.NarratableEntry> T addContentWidget(
			T widget
	) {
		return this.addRenderableWidget(widget);
	}
}
