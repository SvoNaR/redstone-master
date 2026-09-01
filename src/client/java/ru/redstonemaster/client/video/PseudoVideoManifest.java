package ru.redstonemaster.client.video;

public record PseudoVideoManifest(
		int fps,
		int frameCount,
		int width,
		int height,
		String sourceHash
) {
	public static PseudoVideoManifest defaults(int frameCount, int width, int height, String sourceHash) {
		return new PseudoVideoManifest(PseudoVideoConstants.FPS, frameCount, width, height, sourceHash);
	}
}
