package ru.redstonemaster.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import ru.redstonemaster.client.auth.ModWebAuthService;
import ru.redstonemaster.client.profile.ModAvatarManager;
import ru.redstonemaster.config.ModConfig;
import ru.redstonemaster.config.ModContentLanguage;

final class RedstoneMasterProfilePanel {
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_GAP = 8;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int SUCCESS_COLOR = 0xFF55FF55;

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
		int y = this.screen.getContentY() + RedstoneMasterScreen.CONTENT_INNER_PADDING + 48;

		ModConfig config = ModConfig.get();
		ModWebAuthService authService = ModWebAuthService.get();
		if (config.profileLoggedIn) {
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

		if (this.showLoginSuccess && config.profileLoggedIn) {
			textY = this.screen.renderTextContentAt(
					graphics,
					ModContentLanguage.translatable("gui.redstone-master.profile.login_success"),
					textX,
					textY,
					textWidth
			);
			textY += ROW_GAP;
		}

		if (config.profileLoggedIn) {
			Component username = ModContentLanguage.translatable(
					"gui.redstone-master.profile.signed_in_as",
					config.profileUsername
			);
			this.screen.renderTextContentAt(graphics, username, textX, textY, textWidth);
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
			this.screen.renderTextContentAt(
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

	void dispose() {
	}
}
