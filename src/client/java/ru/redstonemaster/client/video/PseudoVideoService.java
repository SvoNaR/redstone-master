package ru.redstonemaster.client.video;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ru.redstonemaster.RedstoneMasterClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

public final class PseudoVideoService {
	private static final Identifier FRAME_TEXTURE_ID = Identifier.fromNamespaceAndPath(
			RedstoneMasterClient.MOD_ID,
			"dynamic/pseudo_video_frame"
	);
	private static final PseudoVideoService INSTANCE = new PseudoVideoService();

	private String activeVideoId = "";
	private PreparedVideo prepared;
	private DynamicTexture frameTexture;
	private int frameIndex;
	private boolean playing;
	private boolean scrubbing;
	private float frameAccumulator;
	private volatile PrepareState prepareState = PrepareState.IDLE;
	private volatile String prepareError = "";
	private final AtomicReference<String> pendingVideoId = new AtomicReference<>("");

	private PseudoVideoService() {
	}

	public static PseudoVideoService get() {
		return INSTANCE;
	}

	public void activate(String videoId) {
		if (videoId == null || videoId.isBlank()) {
			this.release();
			return;
		}
		if (videoId.equals(this.activeVideoId) && this.prepared != null) {
			return;
		}
		this.releaseMemory();
		this.activeVideoId = videoId;
		this.frameIndex = 0;
		this.playing = false;
		this.frameAccumulator = 0f;
		this.prepareState = PrepareState.LOADING;
		this.prepareError = "";
		this.pendingVideoId.set(videoId);

		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			this.prepareState = PrepareState.FAILED;
			this.prepareError = "client";
			return;
		}

		// listResources и регистрация текстур — только на клиентском потоке.
		client.execute(() -> {
			String requestedId = videoId;
			if (!requestedId.equals(this.pendingVideoId.get())) {
				return;
			}
			try {
				var result = PseudoVideoFrameSource.prepare(requestedId, client.getResourceManager());
				this.applyPreparedFrames(requestedId, result);
			} catch (IOException exception) {
				if (requestedId.equals(this.pendingVideoId.get())) {
					this.prepareState = PrepareState.FAILED;
					this.prepareError = exception.getMessage() != null ? exception.getMessage() : "error";
				}
			}
		});
	}

	public void tick(Minecraft client) {
		if (this.prepareState != PrepareState.READY || this.prepared == null || !this.playing || this.scrubbing) {
			return;
		}
		int fps = Math.max(1, this.prepared.manifest().fps());
		this.frameAccumulator += fps / 20f;
		while (this.frameAccumulator >= 1f) {
			this.frameAccumulator -= 1f;
			int next = this.frameIndex + 1;
			if (next >= this.prepared.manifest().frameCount()) {
				next = 0;
			}
			this.showFrame(next);
		}
	}

	public void togglePlayPause() {
		if (this.prepareState == PrepareState.READY) {
			this.playing = !this.playing;
		}
	}

	public void seekBackward() {
		this.seekBy(-PseudoVideoConstants.SEEK_FRAMES);
	}

	public void seekForward() {
		this.seekBy(PseudoVideoConstants.SEEK_FRAMES);
	}

	public void beginScrub() {
		this.scrubbing = true;
		this.frameAccumulator = 0f;
	}

	public void endScrub() {
		this.scrubbing = false;
	}

	public int getFrameIndex() {
		return this.frameIndex;
	}

	public int getFrameCount() {
		PseudoVideoManifest manifest = this.getManifest();
		return manifest != null ? manifest.frameCount() : 0;
	}

	public double getPlaybackProgress() {
		int count = this.getFrameCount();
		if (count <= 1) {
			return 0.0;
		}
		return (double) this.frameIndex / (count - 1);
	}

	public int getPlaybackFps() {
		PseudoVideoManifest manifest = this.getManifest();
		return manifest != null ? Math.max(1, manifest.fps()) : PseudoVideoConstants.FPS;
	}

	public int getCurrentTimeSeconds() {
		return this.frameIndex / this.getPlaybackFps();
	}

	public int getDurationSeconds() {
		int count = this.getFrameCount();
		if (count <= 0) {
			return 0;
		}
		return Math.max(0, (count - 1) / this.getPlaybackFps());
	}

	public String getPlaybackTimeLabel() {
		return formatClock(this.getCurrentTimeSeconds()) + "/" + formatClock(this.getDurationSeconds());
	}

	public static String formatClock(int totalSeconds) {
		int seconds = Math.max(0, totalSeconds);
		return String.format("%02d:%02d", seconds / 60, seconds % 60);
	}

	public void seekToProgress(double progress) {
		int count = this.getFrameCount();
		if (count <= 0) {
			return;
		}
		double clamped = Math.clamp(progress, 0.0, 1.0);
		int target = count <= 1 ? 0 : (int) Math.round(clamped * (count - 1));
		this.seekToFrame(target);
	}

	public void seekToFrame(int index) {
		if (this.prepareState != PrepareState.READY || this.prepared == null) {
			return;
		}
		this.showFrame(index);
	}

	public void release() {
		this.pendingVideoId.set("");
		this.activeVideoId = "";
		this.prepareState = PrepareState.IDLE;
		this.prepareError = "";
		this.prepared = null;
		this.playing = false;
		this.scrubbing = false;
		this.frameIndex = 0;
		this.frameAccumulator = 0f;
		this.releaseMemory();
	}

	public boolean isPlaying() {
		return this.playing;
	}

	public PrepareState getPrepareState() {
		return this.prepareState;
	}

	public String getPrepareError() {
		return this.prepareError;
	}

	public PseudoVideoManifest getManifest() {
		return this.prepared != null ? this.prepared.manifest() : null;
	}

	public Identifier getFrameTextureId() {
		return this.frameTexture != null ? FRAME_TEXTURE_ID : null;
	}

	public int getFrameWidth() {
		PseudoVideoManifest manifest = this.getManifest();
		return manifest != null ? manifest.width() : 0;
	}

	public int getFrameHeight() {
		PseudoVideoManifest manifest = this.getManifest();
		return manifest != null ? manifest.height() : 0;
	}

	private void seekBy(int delta) {
		if (this.prepared == null) {
			return;
		}
		int count = this.prepared.manifest().frameCount();
		if (count <= 0) {
			return;
		}
		int target = Math.floorMod(this.frameIndex + delta, count);
		this.showFrame(target);
	}

	private void applyPreparedFrames(String requestedId, java.util.Optional<PseudoVideoFrameSource.PreparedFrames> result) {
		if (!requestedId.equals(this.pendingVideoId.get())) {
			return;
		}
		if (result.isEmpty()) {
			this.prepareState = PrepareState.FAILED;
			this.prepareError = "missing";
			return;
		}
		PseudoVideoFrameSource.PreparedFrames frames = result.get();
		this.prepared = new PreparedVideo(requestedId, frames);
		this.prepareState = PrepareState.READY;
		this.showFrame(0);
	}

	private void showFrame(int index) {
		if (this.prepared == null) {
			return;
		}
		int count = this.prepared.manifest().frameCount();
		if (count <= 0) {
			return;
		}
		this.frameIndex = Math.clamp(index, 0, count - 1);
		Path pngPath = this.prepared.frames().framePath(this.frameIndex);
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		try (InputStream input = Files.newInputStream(pngPath)) {
			NativeImage image = NativeImage.read(input);
			NativeImage copy = new NativeImage(image.getWidth(), image.getHeight(), false);
			copy.copyFrom(image);
			image.close();
			this.uploadFrame(client, copy);
		} catch (IOException e) {
			this.prepareState = PrepareState.FAILED;
			this.prepareError = "frame";
		}
	}

	private void uploadFrame(Minecraft client, NativeImage image) {
		this.releaseMemory();
		this.frameTexture = new DynamicTexture(() -> "redstone-master pseudo video", image);
		client.getTextureManager().register(FRAME_TEXTURE_ID, this.frameTexture);
		client.getTextureManager().getTexture(FRAME_TEXTURE_ID);
	}

	private void releaseMemory() {
		Minecraft client = Minecraft.getInstance();
		if (client != null) {
			client.getTextureManager().release(FRAME_TEXTURE_ID);
		}
		if (this.frameTexture != null) {
			this.frameTexture.close();
			this.frameTexture = null;
		}
	}

	public enum PrepareState {
		IDLE,
		LOADING,
		READY,
		FAILED
	}

	private record PreparedVideo(String videoId, PseudoVideoFrameSource.PreparedFrames frames) {
		PseudoVideoManifest manifest() {
			return this.frames.manifest();
		}
	}
}
