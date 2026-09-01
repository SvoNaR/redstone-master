package ru.redstonemaster.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
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
	private static final int CARD_BG = 0xD9000000;
	private static final int CARD_BORDER = 0xFF404040;
	private static final int LABEL_COLOR = 0xFF9CA3AF;
	private static final int VALUE_COLOR = 0xFFFFFFFF;
	private static final int SECTION_COLOR = 0xFFE8C070;
	private static final int SUCCESS_COLOR = 0xFF55FF55;
	private static final int ROLE_BADGE_BG = 0x997F1D1D;
	private static final int ROLE_BADGE_BORDER = 0xFFB91C1C;
	private static final int IMAGE_COLOR = 0xFFFFFFFF;
	private static final int BUTTON_HORIZONTAL_PADDING = 12;
	private static final int DETAIL_LABEL_VALUE_GAP = 8;
	private static final int DETAIL_ROW_GAP = 4;
	private static final String PLACEHOLDER = "—";
	private static final DateTimeFormatter MEMBER_SINCE_FORMAT =
			DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

	private static final int PROFILE_CARD_HEIGHT = 100;

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
		ModWebAuthService authService = ModWebAuthService.get();
		if (authService.getPhase() == ModWebAuthService.AuthPhase.WAITING_BROWSER) {
			return;
		}

		int innerX = this.screen.getContentX() + RedstoneMasterScreen.CONTENT_INNER_PADDING;
		int innerWidth = this.screen.getContentWidth() - RedstoneMasterScreen.CONTENT_INNER_PADDING * 2;
		int buttonY = this.computeButtonRowY();
		boolean loggedIn = ModConfig.get().profileLoggedIn;

		String leftLabelKey = loggedIn
				? "gui.redstone-master.profile.open_on_website"
				: "gui.redstone-master.profile.register";
		String rightLabelKey = loggedIn
				? "gui.redstone-master.profile.logout"
				: "gui.redstone-master.profile.login";

		int leftWidth = this.buttonWidthFor(leftLabelKey);
		int rightWidth = this.buttonWidthFor(rightLabelKey);
		int buttonWidth = Math.max(leftWidth, rightWidth);
		int totalWidth = buttonWidth * 2;
		int buttonX = innerX + innerWidth - totalWidth;

		this.screen.addContentWidget(Button.builder(
						ModContentLanguage.translatable(leftLabelKey),
						button -> {
							if (loggedIn) {
								authService.openProfileInBrowser();
							} else {
								authService.beginAuth("register");
							}
						})
				.bounds(buttonX, buttonY, buttonWidth, ROW_HEIGHT)
				.build());

		this.screen.addContentWidget(Button.builder(
						ModContentLanguage.translatable(rightLabelKey),
						button -> {
							if (loggedIn) {
								this.showLoginSuccess = false;
								authService.logout();
							} else {
								authService.beginAuth("login");
							}
						})
				.bounds(buttonX + buttonWidth, buttonY, buttonWidth, ROW_HEIGHT)
				.build());
	}

	void render(GuiGraphics graphics) {
		int textX = this.screen.getContentX() + RedstoneMasterScreen.CONTENT_INNER_PADDING;
		int textY = this.screen.getContentY() + RedstoneMasterScreen.CONTENT_INNER_PADDING;
		int textWidth = this.screen.getContentWidth() - RedstoneMasterScreen.CONTENT_INNER_PADDING * 2;
		ModConfig config = ModConfig.get();
		ModWebAuthService authService = ModWebAuthService.get();

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

		if (config.profileLoggedIn && this.showLoginSuccess) {
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

		this.renderProfileCard(graphics, textX, textY, textWidth, config.profileLoggedIn, config);
	}

	private void renderProfileCard(
			GuiGraphics graphics,
			int x,
			int y,
			int width,
			boolean loggedIn,
			ModConfig config
	) {
		this.drawCard(graphics, x, y, width, PROFILE_CARD_HEIGHT);

		int sidebarX = x + CARD_PADDING;
		int sidebarCenterX = sidebarX + SIDEBAR_WIDTH / 2;
		int sidebarY = y + CARD_PADDING;

		int avatarX = sidebarCenterX - AVATAR_DISPLAY_SIZE / 2;
		int avatarY = sidebarY;
		this.renderAvatar(graphics, avatarX, avatarY);

		String username = loggedIn && !config.profileUsername.isBlank()
				? config.profileUsername
				: PLACEHOLDER;
		int usernameY = avatarY + AVATAR_DISPLAY_SIZE + 6;
		int usernameWidth = this.screen.getScreenFont().width(username);
		graphics.drawString(
				this.screen.getScreenFont(),
				username,
				sidebarCenterX - usernameWidth / 2,
				usernameY,
				VALUE_COLOR,
				true
		);

		String roleLabel = loggedIn ? this.localizedRoleText(config.profileRole) : PLACEHOLDER;
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

		String email = loggedIn && !config.profileEmail.isBlank() ? config.profileEmail : PLACEHOLDER;
		String memberSince = loggedIn ? this.formatMemberSince(config.profileCreatedAt) : PLACEHOLDER;
		String lessons = loggedIn
				? TutorialLessonProgress.countCompletedTotal() + " / " + TutorialLessonProgress.countLessonsTotal()
				: PLACEHOLDER;

		this.renderDetailFieldsColumn(
				graphics,
				detailsX,
				detailsY,
				new DetailField(ModContentLanguage.get("gui.redstone-master.profile.label.email"), email),
				new DetailField(ModContentLanguage.get("gui.redstone-master.profile.label.role"), roleLabel),
				new DetailField(ModContentLanguage.get("gui.redstone-master.profile.label.member_since"), memberSince),
				new DetailField(ModContentLanguage.get("gui.redstone-master.profile.label.lessons"), lessons)
		);
	}

	private int computeButtonRowY() {
		int y = this.screen.getContentY() + RedstoneMasterScreen.CONTENT_INNER_PADDING;
		ModWebAuthService authService = ModWebAuthService.get();
		if (ModConfig.get().profileLoggedIn && this.showLoginSuccess) {
			y += this.screen.getFontLineHeight() + ROW_GAP;
		}
		if (authService.getPhase() == ModWebAuthService.AuthPhase.FAILED) {
			y += this.screen.getFontLineHeight() + ROW_GAP;
		}
		return y + PROFILE_CARD_HEIGHT + ROW_GAP;
	}

	private int buttonWidthFor(String labelKey) {
		int textWidth = this.screen.getScreenFont().width(ModContentLanguage.get(labelKey));
		return textWidth + BUTTON_HORIZONTAL_PADDING * 2;
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

	private record DetailField(String label, String value) {
	}

	private void renderDetailFieldsColumn(
			GuiGraphics graphics,
			int x,
			int y,
			DetailField... fields
	) {
		var font = this.screen.getScreenFont();
		int labelColumnWidth = 0;
		for (DetailField field : fields) {
			labelColumnWidth = Math.max(labelColumnWidth, font.width(field.label));
		}
		int valueX = x + labelColumnWidth + DETAIL_LABEL_VALUE_GAP;
		int rowY = y;
		int lineHeight = this.screen.getFontLineHeight();
		for (DetailField field : fields) {
			graphics.drawString(font, field.label, x, rowY, LABEL_COLOR, true);
			graphics.drawString(font, field.value, valueX, rowY, VALUE_COLOR, true);
			rowY += lineHeight + DETAIL_ROW_GAP;
		}
	}

	private String localizedRoleText(String role) {
		if (role == null || role.isBlank()) {
			return PLACEHOLDER;
		}
		return switch (role.toUpperCase()) {
			case "MODERATOR" -> ModContentLanguage.get("gui.redstone-master.profile.role.moderator");
			case "ADMIN" -> ModContentLanguage.get("gui.redstone-master.profile.role.admin");
			default -> ModContentLanguage.get("gui.redstone-master.profile.role.user");
		};
	}

	private String formatMemberSince(String createdAt) {
		if (createdAt == null || createdAt.isBlank()) {
			return PLACEHOLDER;
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
