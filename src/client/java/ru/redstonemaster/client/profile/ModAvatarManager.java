package ru.redstonemaster.client.profile;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ru.redstonemaster.RedstoneMasterClient;
import ru.redstonemaster.config.ModConfig;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

public final class ModAvatarManager {
	private static final Identifier FALLBACK_AVATAR = Identifier.fromNamespaceAndPath(
			RedstoneMasterClient.MOD_ID, "textures/gui/profile_avatar.png");
	private static final Identifier DYNAMIC_AVATAR = Identifier.fromNamespaceAndPath(
			RedstoneMasterClient.MOD_ID, "dynamic/profile_avatar");
	private static final int FALLBACK_TEXTURE_SIZE = 8;
	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "redstone-master-avatar");
		thread.setDaemon(true);
		return thread;
	});

	private static final AtomicReference<Identifier> currentAvatar = new AtomicReference<>(FALLBACK_AVATAR);
	private static volatile int textureWidth = FALLBACK_TEXTURE_SIZE;
	private static volatile int textureHeight = FALLBACK_TEXTURE_SIZE;
	private static volatile String loadingUrl = "";

	private ModAvatarManager() {
	}

	public static void ensureGuestAvatar() {
		ModConfig config = ModConfig.get();
		if (config.guestAvatarDefault <= 0) {
			config.guestAvatarDefault = ThreadLocalRandom.current().nextInt(1, 9);
			config.save();
		}
		if (!config.profileLoggedIn) {
			loadFromUrl(buildDefaultAvatarUrl(config.guestAvatarDefault));
		}
	}

	public static void loadProfileAvatar() {
		ModConfig config = ModConfig.get();
		if (config.profileLoggedIn && config.profileAvatarUrl != null && !config.profileAvatarUrl.isBlank()) {
			loadFromUrl(config.profileAvatarUrl);
			return;
		}
		ensureGuestAvatar();
	}

	public static Identifier getTabAvatarId() {
		return currentAvatar.get();
	}

	public static int getTabAvatarTextureWidth() {
		return textureWidth;
	}

	public static int getTabAvatarTextureHeight() {
		return textureHeight;
	}

	private static String buildDefaultAvatarUrl(int skinIndex) {
		int index = Math.clamp(skinIndex, 1, 8);
		String base = ModConfig.get().webBaseUrl;
		if (base == null || base.isBlank()) {
			base = "http://localhost:8080";
		}
		if (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		return base + "/avatars/defaults/skin" + index + ".png";
	}

	private static void loadFromUrl(String url) {
		if (url == null || url.isBlank() || url.equals(loadingUrl)) {
			return;
		}
		loadingUrl = url;
		EXECUTOR.execute(() -> {
			try {
				HttpRequest request = HttpRequest.newBuilder()
						.uri(URI.create(url))
						.GET()
						.build();
				HttpResponse<InputStream> response = HttpClient.newHttpClient()
						.send(request, HttpResponse.BodyHandlers.ofInputStream());
				if (response.statusCode() != 200) {
					return;
				}
				try (InputStream inputStream = response.body();
					 NativeImage image = NativeImage.read(inputStream)) {
					int width = image.getWidth();
					int height = image.getHeight();
					NativeImage copy = new NativeImage(width, height, false);
					copy.copyFrom(image);
					Minecraft client = Minecraft.getInstance();
					if (client == null) {
						copy.close();
						return;
					}
					client.execute(() -> registerTexture(copy, width, height));
				}
			} catch (Exception ignored) {
			}
		});
	}

	private static void registerTexture(NativeImage image, int width, int height) {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			image.close();
			return;
		}
		client.getTextureManager().release(DYNAMIC_AVATAR);
		DynamicTexture dynamicTexture = new DynamicTexture(() -> "redstone-master profile avatar", image);
		client.getTextureManager().register(DYNAMIC_AVATAR, dynamicTexture);
		textureWidth = width;
		textureHeight = height;
		currentAvatar.set(DYNAMIC_AVATAR);
	}
}
