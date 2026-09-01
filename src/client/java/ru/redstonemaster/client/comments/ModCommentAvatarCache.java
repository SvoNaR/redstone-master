package ru.redstonemaster.client.comments;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ru.redstonemaster.RedstoneMasterClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ModCommentAvatarCache {
	private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, r -> {
		Thread thread = new Thread(r, "redstone-master-comment-avatar");
		thread.setDaemon(true);
		return thread;
	});
	private static final Map<Long, Entry> ENTRIES = new ConcurrentHashMap<>();
	private static final Identifier FALLBACK = Identifier.fromNamespaceAndPath(
			RedstoneMasterClient.MOD_ID,
			"textures/gui/profile_avatar.png");

	private record Entry(Identifier textureId, int width, int height) {
	}

	private ModCommentAvatarCache() {
	}

	public static AvatarDraw get(long commentId, String avatarUrl) {
		Entry entry = ENTRIES.get(commentId);
		if (entry != null) {
			return new AvatarDraw(entry.textureId(), entry.width(), entry.height());
		}
		if (avatarUrl != null && !avatarUrl.isBlank()) {
			scheduleLoad(commentId, avatarUrl);
		}
		return new AvatarDraw(FALLBACK, 8, 8);
	}

	private static void scheduleLoad(long commentId, String avatarUrl) {
		EXECUTOR.execute(() -> {
			if (ENTRIES.containsKey(commentId)) {
				return;
			}
			try {
				HttpRequest request = HttpRequest.newBuilder()
						.uri(URI.create(avatarUrl))
						.GET()
						.build();
				HttpResponse<InputStream> response = HttpClient.newHttpClient()
						.send(request, HttpResponse.BodyHandlers.ofInputStream());
				if (response.statusCode() != 200) {
					return;
				}
				try (InputStream inputStream = response.body();
					 NativeImage image = NativeImage.read(inputStream)) {
					register(commentId, image);
				}
			} catch (IOException | InterruptedException ignored) {
				if (ignored instanceof InterruptedException) {
					Thread.currentThread().interrupt();
				}
			}
		});
	}

	private static void register(long commentId, NativeImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		NativeImage copy = new NativeImage(width, height, false);
		copy.copyFrom(image);
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			copy.close();
			return;
		}
		client.execute(() -> {
			Identifier textureId = Identifier.fromNamespaceAndPath(
					RedstoneMasterClient.MOD_ID,
					"dynamic/comment_avatar/" + commentId);
			client.getTextureManager().release(textureId);
			DynamicTexture dynamicTexture = new DynamicTexture(() -> "comment avatar " + commentId, copy);
			client.getTextureManager().register(textureId, dynamicTexture);
			ENTRIES.put(commentId, new Entry(textureId, width, height));
		});
	}

	public record AvatarDraw(Identifier textureId, int width, int height) {
	}
}
