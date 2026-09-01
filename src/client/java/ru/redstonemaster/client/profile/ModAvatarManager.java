package ru.redstonemaster.client.profile;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ru.redstonemaster.RedstoneMasterClient;
import ru.redstonemaster.config.ModConfig;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
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
	private static final int GUEST_AVATAR_COUNT = 8;
	private static final int GUEST_TEXTURE_SIZE = 8;
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
			config.guestAvatarDefault = ThreadLocalRandom.current().nextInt(1, GUEST_AVATAR_COUNT + 1);
			config.save();
		}
		if (!config.profileLoggedIn) {
			applyGuestAvatar(config.guestAvatarDefault);
		}
	}

	public static void loadProfileAvatar() {
		ModConfig config = ModConfig.get();
		if (config.profileLoggedIn) {
			Path cachePath = getProfileAvatarCachePath(config.profileUsername);
			if (Files.exists(cachePath)) {
				loadFromFile(cachePath);
			}
			if (config.profileAvatarUrl != null && !config.profileAvatarUrl.isBlank()) {
				loadFromUrl(config.profileAvatarUrl, cachePath);
			} else if (!Files.exists(cachePath)) {
				useFallbackAvatar();
			}
			return;
		}
		loadingUrl = "";
		ensureGuestAvatar();
	}

	/** Сброс дедупликации загрузки после смены профиля (новый вход). */
	public static void resetPendingLoads() {
		loadingUrl = "";
	}

	/** После выхода из аккаунта — случайная стандартная аватарка (skin1–skin8) из ресурсов мода. */
	public static void resetToDefaultGuestAvatar() {
		ModConfig config = ModConfig.get();
		int skin = ThreadLocalRandom.current().nextInt(1, GUEST_AVATAR_COUNT + 1);
		config.guestAvatarDefault = skin;
		config.save();
		resetPendingLoads();
		applyGuestAvatar(skin);
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

	private static Path getProfileAvatarCachePath(String username) {
		String safeName = sanitizeUsername(username);
		return FabricLoader.getInstance()
				.getConfigDir()
				.resolve("redstone-master/profile_avatars/" + safeName + ".png");
	}

	private static String sanitizeUsername(String username) {
		if (username == null || username.isBlank()) {
			return "profile";
		}
		String sanitized = username.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
		return sanitized.isBlank() ? "profile" : sanitized;
	}

	private static Identifier defaultGuestAvatarId(int skinIndex) {
		int index = Math.clamp(skinIndex, 1, GUEST_AVATAR_COUNT);
		return Identifier.fromNamespaceAndPath(
				RedstoneMasterClient.MOD_ID,
				"textures/gui/avatars/default/skin" + index + ".png");
	}

	private static void applyGuestAvatar(int skinIndex) {
		Identifier avatarId = defaultGuestAvatarId(skinIndex);
		Minecraft client = Minecraft.getInstance();
		Runnable apply = () -> {
			if (client != null && client.getResourceManager().getResource(avatarId).isEmpty()) {
				useFallbackAvatar();
				return;
			}
			if (client != null) {
				client.getTextureManager().release(DYNAMIC_AVATAR);
			}
			textureWidth = GUEST_TEXTURE_SIZE;
			textureHeight = GUEST_TEXTURE_SIZE;
			currentAvatar.set(avatarId);
		};
		if (client != null) {
			client.execute(apply);
		} else {
			apply.run();
		}
	}

	private static void loadFromFile(Path cachePath) {
		if (cachePath == null) {
			return;
		}
		EXECUTOR.execute(() -> {
			try {
				if (!Files.exists(cachePath)) {
					return;
				}
				try (NativeImage image = NativeImage.read(Files.newInputStream(cachePath))) {
					registerImageCopy(image);
				}
			} catch (IOException ignored) {
			}
		});
	}

	private static void loadFromUrl(String url, Path cachePath) {
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
					if (cachePath != null) {
						saveToCache(image, cachePath);
					}
					registerImageCopy(image);
				}
			} catch (Exception ignored) {
			}
		});
	}

	private static void saveToCache(NativeImage image, Path cachePath) {
		try {
			Files.createDirectories(cachePath.getParent());
			image.writeToFile(cachePath);
		} catch (IOException ignored) {
		}
	}

	private static void registerImageCopy(NativeImage image) {
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

	private static void useFallbackAvatar() {
		Minecraft client = Minecraft.getInstance();
		if (client != null) {
			client.execute(() -> {
				textureWidth = FALLBACK_TEXTURE_SIZE;
				textureHeight = FALLBACK_TEXTURE_SIZE;
				currentAvatar.set(FALLBACK_AVATAR);
			});
		}
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
