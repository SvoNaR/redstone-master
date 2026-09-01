package ru.redstonemaster.client.video;

import java.util.Optional;

final class PseudoVideoPrepareResult {
	enum Status {
		COMPLETE,
		PAUSED,
		MISSING
	}

	private final Status status;
	private final PseudoVideoFrameSource.PreparedFrames frames;

	private PseudoVideoPrepareResult(Status status, PseudoVideoFrameSource.PreparedFrames frames) {
		this.status = status;
		this.frames = frames;
	}

	static PseudoVideoPrepareResult complete(PseudoVideoFrameSource.PreparedFrames frames) {
		return new PseudoVideoPrepareResult(Status.COMPLETE, frames);
	}

	static PseudoVideoPrepareResult paused() {
		return new PseudoVideoPrepareResult(Status.PAUSED, null);
	}

	static PseudoVideoPrepareResult missing() {
		return new PseudoVideoPrepareResult(Status.MISSING, null);
	}

	Status status() {
		return this.status;
	}

	Optional<PseudoVideoFrameSource.PreparedFrames> frames() {
		return Optional.ofNullable(this.frames);
	}
}
