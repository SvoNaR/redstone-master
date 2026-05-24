package ru.redstonemaster.client.gui;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import ru.redstonemaster.RedstoneMasterClient;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import ru.redstonemaster.client.gui.tutorial.TutorialSessionPersistence;
import ru.redstonemaster.client.gui.tutorial.TutorialStudyTarget;
import ru.redstonemaster.config.ModConfig;
import ru.redstonemaster.config.ModContentLanguage;
import ru.redstonemaster.config.ModSessionState;

import org.jetbrains.annotations.Nullable;

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
			"redstone-master", "textures/gui/photo1_main_menu.png");
	private static final Identifier PROFILE_AVATAR = Identifier.fromNamespaceAndPath(
			"redstone-master", "textures/gui/profile_avatar.png");
	private static final int PROFILE_AVATAR_TEXTURE_SIZE = 8;
	private static final int PROFILE_AVATAR_DISPLAY_SIZE = 8;
	private static final int PROFILE_AVATAR_GAP = 2;
	private static final int MAIN_MENU_PHOTO_TEXTURE_SIZE = 1024;
	private static final int MAIN_MENU_PHOTO_DISPLAY_SIZE = 40;
	private static final int MAIN_MENU_PHOTO_FRAME_PADDING = 2;
	private static final int MAIN_MENU_CREDIT_GROUP_PADDING = 4;
	private static final int MAIN_MENU_CREDIT_GAP = 4;
	private static final float MAIN_MENU_GREETING_SCALE = 1.5f;
	private static final int MAIN_MENU_GREETING_BOTTOM_GAP_LINES = 1;
	private static final int MAIN_MENU_BLOCK_GAP = 4;
	private static final int CREDIT_SVONAR_COLOR = 0xFFFF4444;
	private static final int CREDIT_FOXICY_COLOR = 0xFF55FF55;
	private static final String MAIN_MENU_BRAND = "Redstone Master";
	private static final String MAIN_MENU_GREETING_KEY = "gui.redstone-master.main_menu.greeting";
	private static final String MAIN_MENU_TITLE_SUFFIX_KEY = "gui.redstone-master.main_menu.title_suffix";
	private static final String MAIN_MENU_CREDIT_PREFIX_KEY = "gui.redstone-master.main_menu.credit_prefix";

	private RedstoneMasterTab currentTab = RedstoneMasterTab.MAIN_MENU;
	private final RedstoneMasterSettingsPanel settingsPanel = new RedstoneMasterSettingsPanel(this);
	private final RedstoneMasterTutorialPanel tutorialPanel = new RedstoneMasterTutorialPanel(this);
	@Nullable
	private final Screen previousScreen;
	private boolean sessionRestored;
	private boolean navigationHistoryInitialized;
	private boolean applyingNavigationHistory;
	private final RedstoneMasterNavigationHistory navigationHistory = new RedstoneMasterNavigationHistory();

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
	@Nullable
	private Button profileTabButton;

	public RedstoneMasterScreen(@Nullable Screen previousScreen) {
		super(ModContentLanguage.translatable("gui.redstone-master.title"));
		this.previousScreen = previousScreen;
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
		if (!this.navigationHistoryInitialized) {
			this.navigationHistory.reset(this.captureNavigationSnapshot());
			this.navigationHistoryInitialized = true;
		}
	}

	@Override
	public void onClose() {
		this.persistSessionState();
		if (this.minecraft != null) {
			this.minecraft.setScreen(this.previousScreen);
		}
	}

	private void restoreSessionState() {
		if (!ModConfig.get().rememberSession || !ModSessionState.get().hasSaved()) {
			this.applyDefaultSessionUi();
			return;
		}
		ModSessionState session = ModSessionState.get();
		RedstoneMasterTab savedTab = RedstoneMasterTab.fromName(session.getLastTab());
		this.currentTab = savedTab != null ? savedTab : RedstoneMasterTab.MAIN_MENU;
		this.settingsPanel.setScrollOffset(session.getSettingsScrollOffset());
		this.tutorialPanel.restoreExpandedSections(session.getExpandedTutorialSections());
		TutorialStudyTarget studyTarget = TutorialSessionPersistence.fromStorageKey(session.getTutorialStudyTarget());
		if (studyTarget != null && !this.tutorialPanel.isValidStudyTarget(studyTarget)) {
			studyTarget = null;
		}
		if (studyTarget != null) {
			this.tutorialPanel.restoreNavigationState(
					studyTarget,
					session.getTutorialStudyScrollOffset(),
					session.getTutorialScrollOffset()
			);
		} else {
			this.tutorialPanel.restoreNavigationState(null, session.getTutorialScrollOffset(), 0);
		}
	}

	private void persistSessionState() {
		if (!ModConfig.get().rememberSession) {
			ModSessionState.get().clear();
			return;
		}
		TutorialStudyTarget studyTarget = this.tutorialPanel.getStudyTargetForNavigation();
		ModSessionState.get().save(
				this.currentTab.name(),
				this.settingsPanel.getScrollOffset(),
				this.tutorialPanel.getListScrollOffsetForPersistence(),
				this.tutorialPanel.getExpandedSectionsCsv(),
				TutorialSessionPersistence.toStorageKey(studyTarget),
				studyTarget != null ? this.tutorialPanel.getScrollOffset() : 0
		);
	}

	/** Сброс прокрутки и обучения в памяти (вкладка не меняется). */
	void clearStoredSessionContent() {
		this.settingsPanel.setScrollOffset(0);
		this.tutorialPanel.restoreExpandedSections("");
		this.tutorialPanel.restoreNavigationState(null, 0, 0);
		this.navigationHistory.reset(this.captureNavigationSnapshot());
	}

	/** При открытии мода без сохранения сессии — главная вкладка и сброс позиций. */
	void applyDefaultSessionUi() {
		this.currentTab = RedstoneMasterTab.MAIN_MENU;
		this.clearStoredSessionContent();
	}

	void rebuildAllWidgets() {
		this.updatePanelBounds();
		this.rebuildNavigation();
	}

	void rebuildSettingsWidgets() {
		this.rebuildNavigation();
	}

	void rebuildTutorialWidgets() {
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
		this.tutorialPanel.dispose();

		int innerX = this.panelX + PANEL_PADDING;
		int innerWidth = this.panelWidth - PANEL_PADDING * 2;

		int closeAreaWidth = ICON_BUTTON_SIZE + CLOSE_BUTTON_GAP;
		int navButtonWidth = (innerWidth - closeAreaWidth) / 4;

		this.addRenderableWidget(Button.builder(
						ModContentLanguage.translatable("gui.redstone-master.tab.main_menu"),
						button -> this.selectTab(RedstoneMasterTab.MAIN_MENU))
				.bounds(innerX, this.navY, navButtonWidth, NAV_BAR_HEIGHT)
				.build());

		this.addRenderableWidget(Button.builder(
						ModContentLanguage.translatable("gui.redstone-master.tab.tutorial"),
						button -> this.selectTab(RedstoneMasterTab.TUTORIAL))
				.bounds(innerX + navButtonWidth, this.navY, navButtonWidth, NAV_BAR_HEIGHT)
				.build());

		this.addRenderableWidget(Button.builder(
						ModContentLanguage.translatable("gui.redstone-master.tab.settings"),
						button -> this.selectTab(RedstoneMasterTab.SETTINGS))
				.bounds(innerX + navButtonWidth * 2, this.navY, navButtonWidth, NAV_BAR_HEIGHT)
				.build());

		this.profileTabButton = Button.builder(
						ModContentLanguage.translatable("gui.redstone-master.tab.profile"),
						button -> this.selectTab(RedstoneMasterTab.PROFILE))
				.bounds(innerX + navButtonWidth * 3, this.navY, navButtonWidth, NAV_BAR_HEIGHT)
				.build();
		this.addRenderableWidget(this.profileTabButton);

		int closeX = innerX + navButtonWidth * 4 + CLOSE_BUTTON_GAP;
		this.addRenderableWidget(Button.builder(
						Component.literal("X"),
						button -> this.onClose())
				.bounds(closeX, this.navY, ICON_BUTTON_SIZE, ICON_BUTTON_SIZE)
				.tooltip(Tooltip.create(ModContentLanguage.translatable("gui.redstone-master.close")))
				.build());

		if (this.currentTab == RedstoneMasterTab.SETTINGS) {
			this.settingsPanel.rebuildWidgets();
		}
		if (this.currentTab == RedstoneMasterTab.TUTORIAL) {
			this.tutorialPanel.rebuildWidgets();
		}
	}

	private void selectTab(RedstoneMasterTab tab) {
		if (this.currentTab == RedstoneMasterTab.SETTINGS && tab != RedstoneMasterTab.SETTINGS) {
			this.persistSettingsScroll();
		}
		if (this.currentTab == RedstoneMasterTab.TUTORIAL && tab != RedstoneMasterTab.TUTORIAL) {
			this.persistTutorialScroll();
		}
		this.currentTab = tab;
		this.rebuildNavigation();
		this.onNavigationPointReached();
	}

	void onNavigationPointReached() {
		if (!this.applyingNavigationHistory) {
			this.navigationHistory.push(this.captureNavigationSnapshot());
		}
	}

	public void navigateBack() {
		RedstoneMasterNavigationSnapshot snapshot = this.navigationHistory.goBack();
		if (snapshot != null) {
			this.applyNavigationSnapshot(snapshot);
			return;
		}
		if (this.previousScreen != null) {
			this.onClose();
		}
	}

	public void navigateForward() {
		RedstoneMasterNavigationSnapshot snapshot = this.navigationHistory.goForward();
		if (snapshot != null) {
			this.applyNavigationSnapshot(snapshot);
		}
	}

	private RedstoneMasterNavigationSnapshot captureNavigationSnapshot() {
		return new RedstoneMasterNavigationSnapshot(
				this.currentTab,
				this.tutorialPanel.getStudyTargetForNavigation(),
				this.tutorialPanel.getScrollOffset(),
				this.tutorialPanel.getSavedListScrollOffset(),
				this.settingsPanel.getScrollOffset(),
				this.tutorialPanel.getExpandedSectionsCsv()
		);
	}

	private void applyNavigationSnapshot(RedstoneMasterNavigationSnapshot snapshot) {
		this.applyingNavigationHistory = true;
		try {
			if (this.currentTab == RedstoneMasterTab.SETTINGS && snapshot.tab() != RedstoneMasterTab.SETTINGS) {
				this.persistSettingsScroll();
			}
			if (this.currentTab == RedstoneMasterTab.TUTORIAL && snapshot.tab() != RedstoneMasterTab.TUTORIAL) {
				this.persistTutorialScroll();
			}

			this.currentTab = snapshot.tab();
			this.settingsPanel.setScrollOffset(snapshot.settingsScrollOffset());
			this.tutorialPanel.restoreExpandedSections(snapshot.expandedTutorialSections());
			this.tutorialPanel.restoreNavigationState(
					snapshot.studyTarget(),
					snapshot.tutorialScrollOffset(),
					snapshot.tutorialSavedListScrollOffset()
			);
			this.rebuildNavigation();
		} finally {
			this.applyingNavigationHistory = false;
		}
	}

	private void persistTutorialScroll() {
		this.persistSessionState();
	}

	private void persistSettingsScroll() {
		this.persistSessionState();
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		this.rebuildAllWidgets();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
		if (RedstoneMasterClient.navigateBackKey.matchesMouse(event)) {
			this.navigateBack();
			return true;
		}
		if (RedstoneMasterClient.navigateForwardKey.matchesMouse(event)) {
			this.navigateForward();
			return true;
		}
		return super.mouseClicked(event, doubled);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.currentTab == RedstoneMasterTab.SETTINGS && this.settingsPanel.mouseScrolled(scrollX, scrollY)) {
			return true;
		}
		if (this.currentTab == RedstoneMasterTab.TUTORIAL && this.tutorialPanel.mouseScrolled(scrollX, scrollY)) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		if (this.shouldShowTitleMenuPanorama()) {
			this.renderPanorama(graphics, delta);
		}
		this.renderMenuBackground(graphics, this.panelX, this.panelY, this.panelWidth, this.panelHeight);
	}

	private boolean shouldShowTitleMenuPanorama() {
		return this.previousScreen instanceof TitleScreen;
	}

	private void renderProfileTabAvatar(GuiGraphics graphics) {
		if (this.profileTabButton == null || !this.profileTabButton.visible) {
			return;
		}
		String label = ModContentLanguage.get("gui.redstone-master.tab.profile");
		int labelWidth = this.font.width(label);
		int avatarX = this.profileTabButton.getX() + (this.profileTabButton.getWidth() + labelWidth) / 2 + PROFILE_AVATAR_GAP;
		int avatarY = this.profileTabButton.getY() + (this.profileTabButton.getHeight() - PROFILE_AVATAR_DISPLAY_SIZE) / 2;
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				PROFILE_AVATAR,
				avatarX,
				avatarY,
				0.0f,
				0.0f,
				PROFILE_AVATAR_DISPLAY_SIZE,
				PROFILE_AVATAR_DISPLAY_SIZE,
				PROFILE_AVATAR_TEXTURE_SIZE,
				PROFILE_AVATAR_TEXTURE_SIZE,
				IMAGE_COLOR
		);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		this.renderProfileTabAvatar(graphics);
		this.renderDecorations(graphics);
		this.renderContent(graphics);
		if (this.currentTab == RedstoneMasterTab.SETTINGS) {
			this.settingsPanel.renderLabels(graphics);
		}
		if (this.currentTab == RedstoneMasterTab.TUTORIAL) {
			this.tutorialPanel.render(graphics);
		}
	}

	private int getLineColor() {
		return ModConfig.get().highContrastBorders ? LINE_COLOR_HIGH_CONTRAST : LINE_COLOR_NORMAL;
	}

	private int getScaledTitleHeight() {
		return (int) Math.ceil(this.font.lineHeight * TITLE_SCALE);
	}

	private Component getTitleComponent() {
		return Component.literal(ModContentLanguage.get("gui.redstone-master.title"))
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
			case TUTORIAL -> { /* tutorialPanel.render */ }
			default -> this.renderTextContent(graphics, this.currentTab.getContent());
		}
	}

	private void renderMainMenuContent(GuiGraphics graphics) {
		int textX = this.contentX + CONTENT_INNER_PADDING;
		int textY = this.contentY + CONTENT_INNER_PADDING;
		int textWidth = this.contentWidth - CONTENT_INNER_PADDING * 2;
		int maxTextBottom = this.contentY + this.contentHeight - CONTENT_INNER_PADDING - this.getMainMenuFooterReservedHeight();

		textY = this.renderScaledTextBlock(
				graphics,
				ModContentLanguage.translatable(MAIN_MENU_GREETING_KEY),
				textX,
				textY,
				textWidth,
				MAIN_MENU_GREETING_SCALE,
				maxTextBottom
		);
		textY += this.font.lineHeight * MAIN_MENU_GREETING_BOTTOM_GAP_LINES;
		textY = this.renderStyledTextBlock(
				graphics,
				this.buildMainMenuTitleLine(),
				textX,
				textY,
				textWidth,
				maxTextBottom
		);
		textY += MAIN_MENU_BLOCK_GAP;
		this.renderStyledTextBlock(
				graphics,
				RedstoneMasterTab.MAIN_MENU.getContent(),
				textX,
				textY,
				textWidth,
				maxTextBottom
		);
		this.renderMainMenuFooter(graphics);
	}

	private Component buildMainMenuTitleLine() {
		return this.coloredLiteral(MAIN_MENU_BRAND, TITLE_COLOR)
				.append(this.coloredLiteral(ModContentLanguage.get(MAIN_MENU_TITLE_SUFFIX_KEY), TEXT_COLOR));
	}

	private Component buildMainMenuCredit() {
		return this.coloredLiteral(ModContentLanguage.get(MAIN_MENU_CREDIT_PREFIX_KEY), TEXT_COLOR)
				.append(this.coloredLiteral("SvoNaR", CREDIT_SVONAR_COLOR))
				.append(this.coloredLiteral(" & ", TEXT_COLOR))
				.append(this.coloredLiteral("foxicy", CREDIT_FOXICY_COLOR))
				.append(this.coloredLiteral(" <3", TEXT_COLOR));
	}

	private MutableComponent coloredLiteral(String text, int color) {
		return Component.literal(text)
				.withStyle(style -> style.withColor(TextColor.fromRgb(color & 0xFFFFFF)));
	}

	private int getMainMenuFooterReservedHeight() {
		int photoFrameSize = MAIN_MENU_PHOTO_DISPLAY_SIZE + MAIN_MENU_PHOTO_FRAME_PADDING * 2;
		int contentHeight = Math.max(this.font.lineHeight, photoFrameSize);
		return contentHeight + MAIN_MENU_CREDIT_GROUP_PADDING * 2;
	}

	private void renderMainMenuFooter(GuiGraphics graphics) {
		int lineColor = this.getLineColor();
		Component credit = this.buildMainMenuCredit();
		int photoX = this.contentX + this.contentWidth - CONTENT_INNER_PADDING - MAIN_MENU_PHOTO_DISPLAY_SIZE;
		int photoY = this.contentY + this.contentHeight - CONTENT_INNER_PADDING - MAIN_MENU_PHOTO_DISPLAY_SIZE;
		int creditWidth = this.font.width(credit);
		int creditX = photoX - MAIN_MENU_CREDIT_GAP - creditWidth;
		int creditY = photoY + (MAIN_MENU_PHOTO_DISPLAY_SIZE - this.font.lineHeight) / 2;

		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				MAIN_MENU_PHOTO,
				photoX,
				photoY,
				0.0f,
				0.0f,
				MAIN_MENU_PHOTO_DISPLAY_SIZE,
				MAIN_MENU_PHOTO_DISPLAY_SIZE,
				MAIN_MENU_PHOTO_TEXTURE_SIZE,
				MAIN_MENU_PHOTO_TEXTURE_SIZE,
				MAIN_MENU_PHOTO_TEXTURE_SIZE,
				MAIN_MENU_PHOTO_TEXTURE_SIZE,
				IMAGE_COLOR
		);

		int photoFrameX = photoX - MAIN_MENU_PHOTO_FRAME_PADDING;
		int photoFrameY = photoY - MAIN_MENU_PHOTO_FRAME_PADDING;
		int photoFrameSize = MAIN_MENU_PHOTO_DISPLAY_SIZE + MAIN_MENU_PHOTO_FRAME_PADDING * 2;
		graphics.renderOutline(photoFrameX, photoFrameY, photoFrameSize, photoFrameSize, lineColor);

		graphics.drawString(this.font, credit, creditX, creditY, -1, true);

		int groupLeft = creditX - MAIN_MENU_CREDIT_GROUP_PADDING;
		int groupTop = Math.min(creditY, photoFrameY) - MAIN_MENU_CREDIT_GROUP_PADDING;
		int groupWidth = photoFrameX + photoFrameSize - groupLeft + MAIN_MENU_CREDIT_GROUP_PADDING;
		int groupHeight = Math.max(creditY + this.font.lineHeight, photoFrameY + photoFrameSize)
				- groupTop
				+ MAIN_MENU_CREDIT_GROUP_PADDING;
		graphics.renderOutline(groupLeft, groupTop, groupWidth, groupHeight, lineColor);
	}

	private void renderTextContent(GuiGraphics graphics, Component text) {
		this.renderTextContentAt(
				graphics,
				text,
				this.contentX + CONTENT_INNER_PADDING,
				this.contentY + CONTENT_INNER_PADDING,
				this.contentWidth - CONTENT_INNER_PADDING * 2,
				Integer.MAX_VALUE
		);
	}

	private int renderTextContentAt(GuiGraphics graphics, Component text, int textX, int textY, int textWidth) {
		return this.renderTextContentAt(graphics, text, textX, textY, textWidth, Integer.MAX_VALUE);
	}

	private int renderTextContentAt(
			GuiGraphics graphics,
			Component text,
			int textX,
			int textY,
			int textWidth,
			int maxTextBottom
	) {
		return this.renderStyledTextBlock(graphics, text, textX, textY, textWidth, maxTextBottom);
	}

	private int renderStyledTextBlock(
			GuiGraphics graphics,
			Component text,
			int textX,
			int textY,
			int textWidth,
			int maxTextBottom
	) {
		for (FormattedCharSequence line : this.font.split(text, textWidth)) {
			if (textY + this.font.lineHeight > maxTextBottom) {
				break;
			}
			graphics.drawString(this.font, line, textX, textY, -1, true);
			textY += this.font.lineHeight;
		}
		return textY;
	}

	private int renderScaledTextBlock(
			GuiGraphics graphics,
			Component text,
			int textX,
			int textY,
			int textWidth,
			float scale,
			int maxTextBottom
	) {
		int scaledLineHeight = (int) Math.ceil(this.font.lineHeight * scale);
		int scaledWidth = Math.max(1, (int) (textWidth / scale));
		List<FormattedCharSequence> lines = this.font.split(text, scaledWidth);
		int blockHeight = scaledLineHeight * lines.size();
		if (textY + blockHeight > maxTextBottom) {
			return textY;
		}

		var pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(textX, textY);
		pose.scale(scale, scale);
		int localY = 0;
		for (FormattedCharSequence line : lines) {
			graphics.drawString(this.font, line, 0, localY, -1, true);
			localY += this.font.lineHeight;
		}
		pose.popMatrix();
		return textY + blockHeight;
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
