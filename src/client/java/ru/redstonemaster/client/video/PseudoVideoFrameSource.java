package ru.redstonemaster.client.video;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import ru.redstonemaster.RedstoneMasterClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/** Кадры псевдо-видео в JAR: {@code frame_00000.png}, {@code frame_00001.png}, … — читаются через {@link NativeImage}. */
final class PseudoVideoFrameSource {
	private static final Gson GSON = new Gson();
	private static final String FRAME_PREFIX = "frame_";
	private static final String CACHE_VERSION = "png-only-v5";

	private PseudoVideoFrameSource() {
	}

	static Optional<PreparedFrames> prepare(String videoId, ResourceManager resourceManager) throws IOException {
		PseudoVideoPrepareResult result = prepareResumable(videoId, resourceManager, () -> true);
		return switch (result.status()) {
			case COMPLETE -> result.frames();
			case PAUSED, MISSING -> Optional.empty();
		};
	}

	static PseudoVideoPrepareResult prepareResumable(
			String videoId,
			ResourceManager resourceManager,
			BooleanSupplier continuePreparing
	) throws IOException {
		List<Identifier> pngFrames = listPngFrames(resourceManager, videoId);
		if (pngFrames.isEmpty()) {
			return PseudoVideoPrepareResult.missing();
		}

		Path cacheDir = getCacheDirectory(videoId);
		Path manifestPath = cacheDir.resolve("manifest.json");

		String sourceHash = computeSourceHash(resourceManager, pngFrames);
		PseudoVideoManifest existing = readManifest(manifestPath);
		if (existing != null
				&& sourceHash.equals(existing.sourceHash())
				&& existing.frameCount() == pngFrames.size()
				&& isCacheComplete(cacheDir, existing.frameCount())) {
			return PseudoVideoPrepareResult.complete(new PreparedFrames(cacheDir, existing));
		}

		if (existing != null && !sourceHash.equals(existing.sourceHash())) {
			deleteDirectory(cacheDir);
			existing = null;
		}

		int width = existing != null ? existing.width() : 0;
		int height = existing != null ? existing.height() : 0;
		int fps = existing != null ? existing.fps() : PseudoVideoConstants.FPS;
		int startIndex = countCompletedFrames(cacheDir);
		if (existing != null && sourceHash.equals(existing.sourceHash())) {
			startIndex = Math.max(startIndex, existing.frameCount());
		}

		if (startIndex >= pngFrames.size()) {
			startIndex = 0;
		}

		if (startIndex == 0 && !Files.exists(cacheDir)) {
			Files.createDirectories(cacheDir.resolve("frames"));
		} else {
			Files.createDirectories(cacheDir.resolve("frames"));
		}

		Identifier metaId = metaIdentifier(videoId);
		if (resourceManager.getResource(metaId).isPresent()) {
			Resource metaResource = resourceManager.getResourceOrThrow(metaId);
			try (InputStreamReader reader = new InputStreamReader(metaResource.open(), StandardCharsets.UTF_8)) {
				JsonObject meta = GSON.fromJson(reader, JsonObject.class);
				if (meta != null) {
					if (meta.has("fps")) {
						fps = meta.get("fps").getAsInt();
					}
					if (meta.has("width")) {
						width = meta.get("width").getAsInt();
					}
					if (meta.has("height")) {
						height = meta.get("height").getAsInt();
					}
				}
			}
		}

		for (int index = startIndex; index < pngFrames.size(); index++) {
			if (!continuePreparing.getAsBoolean()) {
				if (index > 0) {
					PseudoVideoManifest partial = new PseudoVideoManifest(fps, index, width, height, sourceHash);
					Files.writeString(manifestPath, GSON.toJson(partial));
				}
				return PseudoVideoPrepareResult.paused();
			}

			Path pngPath = framePngPath(cacheDir, index);
			Resource resource = resourceManager.getResourceOrThrow(pngFrames.get(index));
			try (InputStream input = resource.open();
				 NativeImage nativeImage = NativeImage.read(input)) {
				if (width <= 0) {
					width = nativeImage.getWidth();
				}
				if (height <= 0) {
					height = nativeImage.getHeight();
				}
				nativeImage.writeToFile(pngPath);
			}
		}

		PseudoVideoManifest manifest = new PseudoVideoManifest(fps, pngFrames.size(), width, height, sourceHash);
		Files.writeString(manifestPath, GSON.toJson(manifest));
		return PseudoVideoPrepareResult.complete(new PreparedFrames(cacheDir, manifest));
	}

	static int countCompletedFrames(Path cacheDir) {
		int count = 0;
		while (true) {
			Path png = framePngPath(cacheDir, count);
			if (!Files.exists(png)) {
				break;
			}
			try {
				if (Files.size(png) < 32L) {
					break;
				}
			} catch (IOException e) {
				break;
			}
			count++;
		}
		return count;
	}

	static Path framePngPath(Path cacheDir, int frameIndex) {
		return cacheDir.resolve("frames").resolve(String.format(Locale.ROOT, "%05d.png", frameIndex));
	}

	static Path getCacheDirectory(String videoId) {
		return FabricLoader.getInstance()
				.getGameDir()
				.resolve("config")
				.resolve(PseudoVideoConstants.CACHE_FOLDER)
				.resolve(sanitize(videoId));
	}

	private static String sanitize(String videoId) {
		return videoId.replaceAll("[^a-zA-Z0-9._-]", "_");
	}

	private static Identifier metaIdentifier(String videoId) {
		return Identifier.fromNamespaceAndPath(
				RedstoneMasterClient.MOD_ID,
				PseudoVideoConstants.RESOURCE_ROOT + "/" + videoId + "/meta.json"
		);
	}

	private static Identifier pngFrameIdentifier(String videoId, int index) {
		return Identifier.fromNamespaceAndPath(
				RedstoneMasterClient.MOD_ID,
				PseudoVideoConstants.RESOURCE_ROOT + "/" + videoId + "/"
						+ String.format(Locale.ROOT, FRAME_PREFIX + "%05d.png", index)
		);
	}

	private static List<Identifier> listPngFrames(ResourceManager resourceManager, String videoId) {
		List<Identifier> frames = new ArrayList<>();
		for (int index = 0; index < 10_000; index++) {
			Identifier frameId = pngFrameIdentifier(videoId, index);
			if (resourceManager.getResource(frameId).isEmpty()) {
				if (index == 0) {
					return List.of();
				}
				break;
			}
			frames.add(frameId);
		}

		if (!frames.isEmpty()) {
			return frames;
		}

		String folder = PseudoVideoConstants.RESOURCE_ROOT + "/" + videoId;
		Map<Identifier, Resource> resources = resourceManager.listResources(folder, PseudoVideoFrameSource::isPngFrame);
		List<Identifier> listed = new ArrayList<>(resources.keySet());
		listed.sort(Comparator.comparing(Identifier::getPath));
		return listed;
	}

	private static boolean isPngFrame(Identifier id) {
		if (!RedstoneMasterClient.MOD_ID.equals(id.getNamespace())) {
			return false;
		}
		String path = id.getPath();
		return path.endsWith(".png") && path.contains(FRAME_PREFIX);
	}

	private static PseudoVideoManifest readManifest(Path manifestPath) {
		if (!Files.exists(manifestPath)) {
			return null;
		}
		try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(manifestPath), StandardCharsets.UTF_8)) {
			return GSON.fromJson(reader, PseudoVideoManifest.class);
		} catch (IOException e) {
			return null;
		}
	}

	private static boolean isCacheComplete(Path cacheDir, int frameCount) {
		for (int i = 0; i < frameCount; i++) {
			Path png = framePngPath(cacheDir, i);
			if (!Files.exists(png)) {
				return false;
			}
			try {
				if (Files.size(png) < 32L) {
					return false;
				}
			} catch (IOException e) {
				return false;
			}
		}
		return frameCount > 0;
	}

	private static String computeSourceHash(ResourceManager resourceManager, List<Identifier> frames) throws IOException {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (Exception e) {
			throw new IOException("SHA-256 not available", e);
		}
		digest.update(CACHE_VERSION.getBytes(StandardCharsets.UTF_8));
		for (Identifier frameId : frames) {
			digest.update(frameId.toString().getBytes(StandardCharsets.UTF_8));
			Resource resource = resourceManager.getResourceOrThrow(frameId);
			try (InputStream input = resource.open()) {
				byte[] buffer = new byte[8192];
				int read;
				while ((read = input.read(buffer)) >= 0) {
					if (read > 0) {
						digest.update(buffer, 0, read);
					}
				}
			}
		}
		return HexFormat.of().formatHex(digest.digest());
	}

	private static void deleteDirectory(Path directory) throws IOException {
		if (!Files.exists(directory)) {
			return;
		}
		try (var walk = Files.walk(directory)) {
			walk.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException ignored) {
				}
			});
		}
	}

	record PreparedFrames(Path cacheDir, PseudoVideoManifest manifest) {
		Path framePath(int index) {
			return PseudoVideoFrameSource.framePngPath(this.cacheDir, index);
		}
	}
}
