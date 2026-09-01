package ru.redstonemaster.client.video;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import ru.redstonemaster.config.ModContentLanguage;

public final class PseudoVideoRenderer {
	public static final float NORMAL_DISPLAY_SCALE = 0.55f;
	public static final float FULLSCREEN_DISPLAY_SCALE = 1f;
	public static final int FRAME_BORDER_THICKNESS = 1;
	private static final int COLOR = 0xFFFFFFFF;
	private static final int STATUS_COLOR = 0xFFBBBBBB;
	private static final int TIME_COLOR = 0xFFFFFFFF;

	private PseudoVideoRenderer() {
	}

	public static int[] computeDisplaySize(int maxWidth, int maxHeight, float scale) {
		PseudoVideoService service = PseudoVideoService.get();
		int texWidth = service.getFrameWidth();
		int texHeight = service.getFrameHeight();
		if (texWidth <= 0 || texHeight <= 0) {
			int placeholderHeight = Math.max(72, Math.round(maxWidth * 9f / 16f));
			int placeholderWidth = Math.max(1, Math.round(placeholderHeight * 16f / 9f));
			return fitScaledSize(placeholderWidth, placeholderHeight, maxWidth, maxHeight, scale);
		}
		int width;
		int height;
		if (texWidth <= maxWidth) {
			width = texWidth;
			height = texHeight;
		} else {
			float fit = (float) maxWidth / texWidth;
			width = maxWidth;
			height = Math.max(1, Math.round(texHeight * fit));
		}
		return fitScaledSize(width, height, maxWidth, maxHeight, scale);
	}

	public static int frameLeftForDisplay(int areaX, int areaWidth, int displayWidth) {
		return areaX + Math.max(0, (areaWidth - displayWidth) / 2) - FRAME_BORDER_THICKNESS;
	}

	public static int measureVideoDisplayHeight(int maxWidth) {
		return computeDisplaySize(maxWidth, 0, NORMAL_DISPLAY_SCALE)[1];
	}

	public static int measureHeight(int maxWidth, boolean hasVideo) {
		if (!hasVideo) {
			return 0;
		}
		return measureVideoDisplayHeight(maxWidth) + FRAME_BORDER_THICKNESS * 2;
	}

	public static void drawFrameOutline(GuiGraphics graphics, int x, int y, int width, int height, int borderColor) {
		graphics.renderOutline(x, y, width, height, borderColor);
	}

	public static void renderPlayer(
			GuiGraphics graphics,
			Font font,
			PseudoVideoLayout layout,
			int listTop,
			int contentBottom,
			int borderColor,
			boolean drawTimeLabel
	) {
		if (layout.videoBlockTop() + layout.videoBlockHeight() >= listTop && layout.videoBlockTop() <= contentBottom) {
			drawFrameOutline(
					graphics,
					layout.frameLeft(),
					layout.videoBlockTop(),
					layout.frameOuterWidth(),
					layout.videoBlockHeight(),
					borderColor
			);
		}
		renderFrameContent(
				graphics,
				font,
				layout.videoDrawX(),
				layout.videoDrawY(),
				layout.videoDisplayWidth(),
				layout.videoDisplayHeight(),
				listTop,
				contentBottom
		);
		if (layout.sliderBlockTop() + layout.sliderBlockHeight() >= listTop
				&& layout.sliderBlockTop() <= contentBottom) {
			drawFrameOutline(
					graphics,
					layout.frameLeft(),
					layout.sliderBlockTop(),
					layout.frameOuterWidth(),
					layout.sliderBlockHeight(),
					borderColor
			);
		}
		if (layout.controlsBlockTop() + layout.controlsBlockHeight() >= listTop
				&& layout.controlsBlockTop() <= contentBottom) {
			drawFrameOutline(
					graphics,
					layout.controlsFrameLeft(),
					layout.controlsBlockTop(),
					layout.controlsFrameOuterWidth(),
					layout.controlsBlockHeight(),
					borderColor
			);
			if (drawTimeLabel) {
				drawTimeLabel(graphics, font, layout, listTop, contentBottom);
			}
		}
	}

	public static void drawTimeLabel(
			GuiGraphics graphics,
			Font font,
			PseudoVideoLayout layout,
			int listTop,
			int contentBottom
	) {
		if (layout.timeTextY() + font.lineHeight < listTop || layout.timeTextY() > contentBottom) {
			return;
		}
		graphics.drawString(
				font,
				PseudoVideoService.get().getPlaybackTimeLabel(),
				layout.timeTextX(),
				layout.timeTextY(),
				TIME_COLOR,
				true
		);
	}

	private static void renderFrameContent(
			GuiGraphics graphics,
			Font font,
			int drawX,
			int drawY,
			int displayWidth,
			int displayHeight,
			int listTop,
			int contentBottom
	) {
		PseudoVideoService service = PseudoVideoService.get();
		PseudoVideoService.PrepareState state = service.getPrepareState();
		if (state == PseudoVideoService.PrepareState.IDLE) {
			return;
		}
		if (state == PseudoVideoService.PrepareState.LOADING) {
			drawStatus(
					graphics,
					font,
					drawX,
					drawY,
					displayWidth,
					displayHeight,
					listTop,
					contentBottom,
					"gui.redstone-master.tutorial.video.loading"
			);
			return;
		}
		if (state == PseudoVideoService.PrepareState.FAILED) {
			int nextY = drawStatus(
					graphics,
					font,
					drawX,
					drawY,
					displayWidth,
					displayHeight,
					listTop,
					contentBottom,
					"gui.redstone-master.tutorial.video.unavailable"
			);
			String detail = service.getPrepareError();
			if (detail != null && !detail.isBlank()) {
				for (var line : font.split(Component.literal("(" + detail + ")"), displayWidth)) {
					if (nextY + font.lineHeight >= listTop && nextY <= contentBottom) {
						graphics.drawString(font, line, drawX, nextY, STATUS_COLOR, true);
					}
					nextY += font.lineHeight;
				}
			}
			return;
		}

		int texWidth = service.getFrameWidth();
		int texHeight = service.getFrameHeight();
		if (texWidth <= 0 || texHeight <= 0 || service.getFrameTextureId() == null) {
			drawStatus(
					graphics,
					font,
					drawX,
					drawY,
					displayWidth,
					displayHeight,
					listTop,
					contentBottom,
					"gui.redstone-master.tutorial.video.unavailable"
			);
			return;
		}

		if (drawY + displayHeight >= listTop && drawY <= contentBottom) {
			Identifier textureId = service.getFrameTextureId();
			Minecraft client = Minecraft.getInstance();
			if (client != null) {
				client.getTextureManager().getTexture(textureId);
			}
			graphics.blit(
					RenderPipelines.GUI_TEXTURED,
					textureId,
					drawX,
					drawY,
					0f,
					0f,
					displayWidth,
					displayHeight,
					texWidth,
					texHeight,
					texWidth,
					texHeight,
					COLOR
			);
		}
	}

	private static int drawStatus(
			GuiGraphics graphics,
			Font font,
			int x,
			int y,
			int boxWidth,
			int boxHeight,
			int listTop,
			int contentBottom,
			String key
	) {
		int textY = y + Math.max(0, (boxHeight - font.lineHeight) / 2);
		for (var line : font.split(ModContentLanguage.translatable(key), boxWidth)) {
			if (textY + font.lineHeight >= listTop && textY <= contentBottom) {
				int lineX = x + Math.max(0, (boxWidth - font.width(line)) / 2);
				graphics.drawString(font, line, lineX, textY, STATUS_COLOR, true);
			}
			textY += font.lineHeight;
		}
		return textY;
	}

	private static int[] fitScaledSize(int width, int height, int maxWidth, int maxHeight, float scale) {
		int scaledWidth = Math.max(1, Math.round(width * scale));
		int scaledHeight = Math.max(1, Math.round(height * scale));
		if (maxHeight > 0 && scaledHeight > maxHeight) {
			float shrink = (float) maxHeight / scaledHeight;
			scaledWidth = Math.max(1, Math.round(scaledWidth * shrink));
			scaledHeight = maxHeight;
		}
		if (scaledWidth > maxWidth) {
			float shrink = (float) maxWidth / scaledWidth;
			scaledWidth = maxWidth;
			scaledHeight = Math.max(1, Math.round(scaledHeight * shrink));
		}
		return new int[] {scaledWidth, scaledHeight};
	}
}
