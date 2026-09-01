package ru.redstonemaster.client.video;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class PseudoVideoSeekSlider extends AbstractSliderButton {
	private boolean widgetScrubbing;
	private boolean syncing;

	public PseudoVideoSeekSlider(int x, int y, int width, int height) {
		super(x, y, width, height, Component.empty(), 0.0);
	}

	@Override
	protected void applyValue() {
		if (this.syncing) {
			return;
		}
		PseudoVideoService service = PseudoVideoService.get();
		if (!this.widgetScrubbing) {
			this.widgetScrubbing = true;
			service.beginScrub();
		}
		service.seekToProgress(this.value);
	}

	@Override
	protected void updateMessage() {
	}

	@Override
	public void onRelease(MouseButtonEvent event) {
		super.onRelease(event);
		if (this.widgetScrubbing) {
			this.widgetScrubbing = false;
			PseudoVideoService.get().endScrub();
		}
	}

	public void syncFromPlayback() {
		if (this.widgetScrubbing || PseudoVideoService.get().getPrepareState() != PseudoVideoService.PrepareState.READY) {
			return;
		}
		double progress = PseudoVideoService.get().getPlaybackProgress();
		if (Math.abs(this.value - progress) < 0.0005) {
			return;
		}
		this.syncing = true;
		this.setValue(progress);
		this.syncing = false;
	}
}
