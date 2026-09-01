package ru.redstonemaster.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import ru.redstonemaster.client.auth.ModWebAuthService;
import ru.redstonemaster.client.gui.tutorial.TutorialLessonProgress;
import ru.redstonemaster.client.profile.ModAvatarManager;
import ru.redstonemaster.config.ModConfig;
import ru.redstonemaster.config.ModContentLanguage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

final class RedstoneMasterProfilePanel {
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_GAP = 8;
	private static final int CARD_PADDING = 8;
	private static final int CARD_GAP = 10;
	private static final int SIDEBAR_WIDTH = 96;
	private static final int AVATAR_DISPLAY_SIZE = 56;
	private static final int AVATAR_FRAME = 2;
	private static final int CARD_BG = 0xD92B2118;
	private static final int CARD_BORDER = 0xFF6B5344;
	private static final int LABEL_COLOR = 0xFF9CA3AF;
	private static final int VALUE_COLOR = 0xFFFFFFFF;
	private static final int SECTION_COLOR = 0xFFE8C070;
	private static final int SUCCESS_COLOR = 0xFF55FF55;
	private static final int ROLE_BADGE_BG = 0x997F1D1D;
	private static final int ROLE_BADGE_BORDER = 0xFFB91C1C;
	private static final int IMAGE_COLOR = 0xFFFFFFFF;
	private static final DateTimeFormatter MEMBER_SINCE_FORMAT =
			DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

	private static final int LOGGED_IN_CARD_HEIGHT = 132;

	private final RedstoneMasterScreen screen;
	private boolean showLoginSuccess;

	RedstoneMasterProfilePanel(RedstoneMasterScreen screen) {
		this.screen = screen;
	}

	void onTabOpened() {
		ModAvatarManager.ensureGuestAvatar();
		ModAvatarManager.loadProfileAvatar();
	}

	void setShowLoginSuccess(boolean showLoginSuccess) {
		this.showLoginSuccess = showLoginSuccess;
	}

	void rebuildWidgets() {
		int innerX = this.screen.getContentX() + RedstoneMasterScreen.CONTENT_INNER_PADDING;
		int innerWidth = this.screen.getContentWidth() - RedstoneMasterScreen.CONTENT_INNER_PADDING * 2;
		int buttonWidth = Math.min(180, innerWidth);
		int buttonX = innerX + (innerWidth - buttonWidth) / 2;
		int y = this.computeLogoutButtonY();

		ModConfig config = ModConfig.get();
		ModWebAuthService authService = ModWebAuthService.get();
		if (config.profileLoggedIn) {
			this.screen.addContentWidget(Button.builder(
							ModContentLanguage.translatable("gui.redstone-master.profile.logout"),
							button -> {
								this.showLoginSuccess = false;
								authService.logout();
							})
					.bounds(buttonX, y, buttonWidth, ROW_HEIGHT)
					.build());
			return;
		}
		if (authService.getPhase() == ModWebAuthService.AuthPhase.WAITING_BROWSER) {
			return;
		}

		this.screen.addContentWidget(Button.builder(
						ModContentLanguage.translatable("gui.redstone-master.profile.login"),
						button -> authService.beginAuth("login"))
				.bounds(buttonX, y, buttonWidth, ROW_HEIGHT)
				.build());
		y += ROW_HEIGHT + ROW_GAP;
		this.screen.addContentWidget(Button.builder(
						ModContentLanguage.translatable("gui.redstone-master.profile.register"),
						button -> authService.beginAuth("register"))
				.bounds(buttonX, y, buttonWidth, ROW_HEIGHT)
				.build());
	}

	void render(GuiGraphics graphics) {
		int textX = this.screen.getContentX() + RedstoneMasterScreen.CONTENT_INNER_PADDING;
		int textY = this.screen.getContentY() + RedstoneMasterScreen.CONTENT_INNER_PADDING;
		int textWidth = this.screen.getContentWidth() - RedstoneMasterScreen.CONTENT_INNER_PADDING * 2;
		ModConfig config = ModConfig.get();
		ModWebAuthService authService = ModWebAuthService.get();

		if (config.profileLoggedIn) {
			if (this.showLoginSuccess) {
				graphics.drawString(
						this.screen.getScreenFont(),
						ModContentLanguage.get("gui.redstone-master.profile.login_success"),
						textX,
						textY,
						SUCCESS_COLOR,
						true
				);
				textY += this.screen.getFontLineHeight() + ROW_GAP;
			}
			this.renderLoggedInCard(graphics, textX, textY, textWidth, config);
			return;
		}

		if (authService.getPhase() == ModWebAuthService.AuthPhase.WAITING_BROWSER) {
			this.screen.renderTextContentAt(
					graphics,
					ModContentLanguage.translatable("gui.redstone-master.profile.waiting_browser"),
					textX,
					textY,
					textWidth
			);
			return;
		}

		if (authService.getPhase() == ModWebAuthService.AuthPhase.FAILED) {
			String errorKey = authService.getLastErrorKey();
			if (errorKey == null || errorKey.isBlank()) {
				errorKey = "gui.redstone-master.profile.auth.error";
			}
			textY = this.screen.renderTextContentAt(
					graphics,
					ModContentLanguage.translatable(errorKey),
					textX,
					textY,
					textWidth
			);
			textY += this.screen.getFontLineHeight() + ROW_GAP;
		}

		this.screen.renderTextContentAt(
				graphics,
				ModContentLanguage.translatable("gui.redstone-master.profile.guest_hint"),
				textX,
				textY,
				textWidth
		);
	}

	private int renderLoggedInCard(GuiGraphics graphics, int x, int y, int width, ModConfig config) {
		int cardHeight = LOGGED_IN_CARD_HEIGHT;
		this.drawCard(graphics, x, y, width, cardHeight);

		int sidebarX = x + CARD_PADDING;
		int sidebarCenterX = sidebarX + SIDEBAR_WIDTH / 2;
		int sidebarY = y + CARD_PADDING;

		int avatarX = sidebarCenterX - AVATAR_DISPLAY_SIZE / 2;
		int avatarY = sidebarY;
		this.renderAvatar(graphics, avatarX, avatarY);

		int usernameY = avatarY + AVATAR_DISPLAY_SIZE + 6;
		String username = config.profileUsername;
		int usernameWidth = this.screen.getScreenFont().width(username);
		graphics.drawString(
				this.screen.getScreenFont(),
				username,
				sidebarCenterX - usernameWidth / 2,
				usernameY,
				VALUE_COLOR,
				true
		);

		String roleLabel = this.localizedRoleText(config.profileRole);
		int badgeWidth = this.screen.getScreenFont().width(roleLabel) + 10;
		int badgeHeight = this.screen.getScreenFont().lineHeight + 4;
		int badgeX = sidebarCenterX - badgeWidth / 2;
		int badgeY = usernameY + this.screen.getFontLineHeight() + 4;
		graphics.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + badgeHeight, ROLE_BADGE_BG);
		graphics.renderOutline(badgeX, badgeY, badgeWidth, badgeHeight, ROLE_BADGE_BORDER);
		graphics.drawString(
				this.screen.getScreenFont(),
				roleLabel,
				badgeX + 5,
				badgeY + 2,
				VALUE_COLOR,
				true
		);

		int detailsX = x + SIDEBAR_WIDTH + CARD_GAP + CARD_PADDING;
		int detailsWidth = width - SIDEBAR_WIDTH - CARD_GAP - CARD_PADDING * 2;
		int detailsY = y + CARD_PADDING;
		graphics.drawString(
				this.screen.getScreenFont(),
				ModContentLanguage.get("gui.redstone-master.profile.section.account"),
				detailsX,
				detailsY,
				SECTION_COLOR,
				true
		);
		detailsY += this.screen.getFontLineHeight() + 6;

		detailsY = this.renderDetailRow(
				graphics,
				detailsX,
				detailsY,
				detailsWidth,
				ModContentLanguage.get("gui.redstone-master.profile.label.email"),
				config.profileEmail.isBlank() ? "—" : config.profileEmail
		);
		detailsY = this.renderDetailRow(
				graphics,
				detailsX,
				detailsY,
				detailsWidth,
				ModContentLanguage.get("gui.redstone-master.profile.label.role"),
				roleLabel
		);
		detailsY = this.renderDetailRow(
				graphics,
				detailsX,
				detailsY,
				detailsWidth,
				ModContentLanguage.get("gui.redstone-master.profile.label.member_since"),
				this.formatMemberSince(config.profileCreatedAt)
		);
		this.renderDetailRow(
				graphics,
				detailsX,
				detailsY,
				detailsWidth,
				ModContentLanguage.get("gui.redstone-master.profile.label.lessons"),
				TutorialLessonProgress.countCompletedTotal() + " / " + TutorialLessonProgress.countLessonsTotal()
		);

		return y + cardHeight;
	}

	private int computeLogoutButtonY() {
		int y = this.screen.getContentY() + RedstoneMasterScreen.CONTENT_INNER_PADDING;
		if (this.showLoginSuccess && ModConfig.get().profileLoggedIn) {
			y += this.screen.getFontLineHeight() + ROW_GAP;
		}
		return y + LOGGED_IN_CARD_HEIGHT + ROW_GAP;
	}

	private void drawCard(GuiGraphics graphics, int x, int y, int width, int height) {
		graphics.fill(x, y, x + width, y + height, CARD_BG);
		graphics.renderOutline(x, y, width, height, CARD_BORDER);
	}

	private void renderAvatar(GuiGraphics graphics, int x, int y) {
		int frameX = x - AVATAR_FRAME;
		int frameY = y - AVATAR_FRAME;
		int frameSize = AVATAR_DISPLAY_SIZE + AVATAR_FRAME * 2;
		graphics.fill(frameX, frameY, frameX + frameSize, frameY + frameSize, ROLE_BADGE_BG);
		graphics.renderOutline(frameX, frameY, frameSize, frameSize, ROLE_BADGE_BORDER);

		Identifier avatarId = ModAvatarManager.getTabAvatarId();
		int textureWidth = ModAvatarManager.getTabAvatarTextureWidth();
		int textureHeight = ModAvatarManager.getTabAvatarTextureHeight();
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				avatarId,
				x,
				y,
				0.0f,
				0.0f,
				AVATAR_DISPLAY_SIZE,
				AVATAR_DISPLAY_SIZE,
				textureWidth,
				textureHeight,
				textureWidth,
				textureHeight,
				IMAGE_COLOR
		);
	}

	private int renderDetailRow(GuiGraphics graphics, int x, int y, int width, String label, String value) {
		graphics.drawString(this.screen.getScreenFont(), label, x, y, LABEL_COLOR, true);
		y += this.screen.getFontLineHeight() + 2;
		for (var line : this.screen.getScreenFont().split(Component.literal(value), width)) {
			graphics.drawString(this.screen.getScreenFont(), line, x, y, VALUE_COLOR, true);
			y += this.screen.getFontLineHeight();
		}
		return y + 4;
	}

	private String localizedRoleText(String role) {
		if (role == null || role.isBlank()) {
			return "—";
		}
		return switch (role.toUpperCase()) {
			case "MODERATOR" -> ModContentLanguage.get("gui.redstone-master.profile.role.moderator");
			case "ADMIN" -> ModContentLanguage.get("gui.redstone-master.profile.role.admin");
			default -> ModContentLanguage.get("gui.redstone-master.profile.role.user");
		};
	}

	private String formatMemberSince(String createdAt) {
		if (createdAt == null || createdAt.isBlank()) {
			return "—";
		}
		try {
			return MEMBER_SINCE_FORMAT.format(Instant.parse(createdAt));
		} catch (Exception ignored) {
			return createdAt;
		}
	}

	void dispose() {
	}
}
