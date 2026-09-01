package ru.redstonemaster.client.gui.tutorial;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TutorialTextures {
	private static final int IMAGE_COLOR = 0xFFFFFFFF;
	private static final int MAX_DISPLAY_SIZE = 200;
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

	private static int[] computeDisplaySize(int texWidth, int texHeight, int maxWidth) {
		int limit = Math.min(maxWidth, MAX_DISPLAY_SIZE);
		if (texWidth <= limit && texHeight <= limit) {
			return new int[] {texWidth, texHeight};
		}
		float scale = Math.min((float) limit / texWidth, (float) limit / texHeight);
		return new int[] {
				Math.max(1, Math.round(texWidth * scale)),
				Math.max(1, Math.round(texHeight * scale))
		};
	}

	public static int measureImagesHeight(List<String> imagePaths, int maxWidth, int gap) {
		int total = 0;
		for (String path : imagePaths) {
			if (path == null || path.isBlank()) {
				continue;
			}
			Identifier id = textureId(path);
			if (id == null) {
				continue;
			}
			int[] texSize = resolveTextureSize(id);
			int[] display = computeDisplaySize(texSize[0], texSize[1], maxWidth);
			total += display[1] + gap;
		}
		return total;
	}

	public static int renderImages(
			GuiGraphics graphics,
			net.minecraft.client.gui.Font font,
			List<String> imagePaths,
			int x,
			int y,
			int maxWidth,
			int listTop,
			int contentBottom,
			int gap
	) {
		Minecraft client = Minecraft.getInstance();
		for (String path : imagePaths) {
			Identifier id = textureId(path);
			if (id == null) {
				continue;
			}
			client.getTextureManager().getTexture(id);
			int[] texSize = resolveTextureSize(id);
			int texW = texSize[0];
			int texH = texSize[1];
			int[] display = computeDisplaySize(texW, texH, maxWidth);
			int displayW = display[0];
			int displayH = display[1];

			if (y + displayH >= listTop && y <= contentBottom) {
				graphics.blit(
						RenderPipelines.GUI_TEXTURED,
						id,
						x,
						y,
						0.0f,
						0.0f,
						displayW,
						displayH,
						texW,
						texH,
						texW,
						texH,
						IMAGE_COLOR
				);
			}
			y += displayH + gap;
		}
		return y;
	}

	public static void clearCache() {
		SIZE_CACHE.clear();
	}
}
