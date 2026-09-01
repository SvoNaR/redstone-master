package ru.redstonemaster.client.gui.tutorial;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TutorialTextures {
	private static final int IMAGE_COLOR = 0xFFFFFFFF;
	private static final int CAPTION_COLOR = 0xFFBBBBBB;
	/** Внутренняя область рамки (одинаковая для всех иллюстраций). */
	private static final int FRAME_INNER_SIZE = 200;
	private static final int FRAME_PADDING = 4;
	/** Отступ подписи от нижнего края рамки. */
	private static final int CAPTION_OFFSET_BELOW_IMAGE = 4;
	private static final Map<Identifier, int[]> SIZE_CACHE = new HashMap<>();

	private TutorialTextures() {
	}

	public static Identifier textureId(String path) {
		if (path == null || path.isBlank()) {
			return null;
		}
		String normalized = path.replace('\\', '/');
		if (!normalized.startsWith("textures/")) {
			normalized = "textures/" + normalized;
		}
		return Identifier.fromNamespaceAndPath("redstone-master", normalized);
	}

	private static int frameInnerWidth(int maxWidth) {
		return Math.min(maxWidth, FRAME_INNER_SIZE);
	}

	private static int frameInnerHeight() {
		return FRAME_INNER_SIZE;
	}

	private static int uniformFrameWidth(int maxWidth) {
		return frameInnerWidth(maxWidth) + FRAME_PADDING * 2;
	}

	private static int uniformFrameHeight() {
		return frameInnerHeight() + FRAME_PADDING * 2;
	}

	private static int[] resolveTextureSize(Identifier id) {
		return SIZE_CACHE.computeIfAbsent(id, key -> {
			Minecraft client = Minecraft.getInstance();
			if (client == null) {
				return new int[] {64, 64};
			}
			try {
				Resource resource = client.getResourceManager().getResourceOrThrow(key);
				try (NativeImage image = NativeImage.read(resource.open())) {
					return new int[] {image.getWidth(), image.getHeight()};
				}
			} catch (Exception e) {
				return new int[] {64, 64};
			}
		});
	}

	/** Масштаб «вписать в рамку», не выходя за её границы. */
	private static int[] computeFittedSize(int texWidth, int texHeight, int boxWidth, int boxHeight) {
		if (texWidth <= 0 || texHeight <= 0) {
			return new int[] {1, 1};
		}
		float scale = Math.min((float) boxWidth / texWidth, (float) boxHeight / texHeight);
		return new int[] {
				Math.max(1, Math.round(texWidth * scale)),
				Math.max(1, Math.round(texHeight * scale))
		};
	}

	private static int measureCaptionTextHeight(Font font, String caption, int maxWidth) {
		if (caption == null || caption.isBlank()) {
			return 0;
		}
		return font.split(Component.literal(caption), maxWidth).size() * font.lineHeight;
	}

	private record ImageBlockLayout(
			int frameWidth,
			int frameHeight,
			int frameX,
			int frameY,
			int imageX,
			int imageY,
			int displayW,
			int displayH,
			int captionY,
			int blockHeight
	) {
	}

	private static ImageBlockLayout layoutImageBlock(
			Font font,
			int x,
			int y,
			int maxWidth,
			int texW,
			int texH,
			String caption
	) {
		int innerW = frameInnerWidth(maxWidth);
		int innerH = frameInnerHeight();
		int[] display = computeFittedSize(texW, texH, innerW, innerH);
		int displayW = display[0];
		int displayH = display[1];
		int frameWidth = uniformFrameWidth(maxWidth);
		int frameHeight = uniformFrameHeight();
		int frameX = x + Math.max(0, (maxWidth - frameWidth) / 2);
		int frameY = y;
		int imageX = frameX + FRAME_PADDING + Math.max(0, (innerW - displayW) / 2);
		int imageY = frameY + FRAME_PADDING + Math.max(0, (innerH - displayH) / 2);
		int captionHeight = measureCaptionTextHeight(font, caption, maxWidth);
		int captionY = frameY + frameHeight + (captionHeight > 0 ? CAPTION_OFFSET_BELOW_IMAGE : 0);
		int blockHeight = frameHeight + (captionHeight > 0 ? CAPTION_OFFSET_BELOW_IMAGE + captionHeight : 0);
		return new ImageBlockLayout(
				frameWidth,
				frameHeight,
				frameX,
				frameY,
				imageX,
				imageY,
				displayW,
				displayH,
				captionY,
				blockHeight
		);
	}

	private static int measureImageBlockHeight(Font font, TutorialImage image, int maxWidth) {
		if (image == null || image.path().isBlank()) {
			return 0;
		}
		Identifier id = textureId(image.path());
		if (id == null) {
			return 0;
		}
		int[] texSize = resolveTextureSize(id);
		return layoutImageBlock(font, 0, 0, maxWidth, texSize[0], texSize[1], image.displayCaption()).blockHeight();
	}

	public static int measureImagesHeight(List<TutorialImage> images, Font font, int maxWidth, int gap) {
		int total = 0;
		for (TutorialImage image : images) {
			int blockHeight = measureImageBlockHeight(font, image, maxWidth);
			if (blockHeight <= 0) {
				continue;
			}
			total += blockHeight + gap;
		}
		return total;
	}

	public static int renderImages(
			GuiGraphics graphics,
			Font font,
			List<TutorialImage> images,
			int x,
			int y,
			int maxWidth,
			int listTop,
			int contentBottom,
			int gap,
			int frameColor
	) {
		Minecraft client = Minecraft.getInstance();
		for (TutorialImage image : images) {
			if (image == null || image.path().isBlank()) {
				continue;
			}
			Identifier id = textureId(image.path());
			if (id == null) {
				continue;
			}
			client.getTextureManager().getTexture(id);
			int[] texSize = resolveTextureSize(id);
			String caption = image.displayCaption();
			ImageBlockLayout layout = layoutImageBlock(
					font,
					x,
					y,
					maxWidth,
					texSize[0],
					texSize[1],
					caption
			);

			if (y + layout.blockHeight() >= listTop && y <= contentBottom) {
				if (layout.frameY() + layout.frameHeight() >= listTop && layout.frameY() <= contentBottom) {
					graphics.renderOutline(
							layout.frameX(),
							layout.frameY(),
							layout.frameWidth(),
							layout.frameHeight(),
							frameColor
					);
				}
				if (layout.imageY() + layout.displayH() >= listTop && layout.imageY() <= contentBottom) {
					graphics.blit(
							RenderPipelines.GUI_TEXTURED,
							id,
							layout.imageX(),
							layout.imageY(),
							0.0f,
							0.0f,
							layout.displayW(),
							layout.displayH(),
							texSize[0],
							texSize[1],
							texSize[0],
							texSize[1],
							IMAGE_COLOR
					);
				}
				if (!caption.isBlank()) {
					int captionY = layout.captionY();
					for (FormattedCharSequence line : font.split(Component.literal(caption), maxWidth)) {
						if (captionY + font.lineHeight >= listTop && captionY <= contentBottom) {
							int lineX = x + Math.max(0, (maxWidth - font.width(line)) / 2);
							graphics.drawString(font, line, lineX, captionY, CAPTION_COLOR, true);
						}
						captionY += font.lineHeight;
					}
				}
			}
			y += layout.blockHeight() + gap;
		}
		return y;
	}

	public static void clearCache() {
		SIZE_CACHE.clear();
	}
}
