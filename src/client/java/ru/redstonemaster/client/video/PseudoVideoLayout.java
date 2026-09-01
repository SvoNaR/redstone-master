package ru.redstonemaster.client.video;

import net.minecraft.client.gui.Font;

/** Вертикальная раскладка: видео → ползунок → панель управления (рамки вплотную). */
public record PseudoVideoLayout(
		int frameLeft,
		int frameOuterWidth,
		int videoBlockTop,
		int videoBlockHeight,
		int videoDrawX,
		int videoDrawY,
		int videoDisplayWidth,
		int videoDisplayHeight,
		int sliderBlockTop,
		int sliderBlockHeight,
		int controlsBlockTop,
		int controlsBlockHeight,
		int controlsFrameLeft,
		int controlsFrameOuterWidth,
		int timeTextX,
		int timeTextY,
		int seekBackButtonX,
		int playButtonX,
		int seekForwardButtonX,
		int fullscreenButtonX
) {
	public static final int SLIDER_INNER_HEIGHT = 10;
	public static final int CONTROLS_SIDE_PADDING = 4;

	public static PseudoVideoLayout embedded(
			int contentTop,
			int textX,
			int textWidth,
			int maxBottom,
			int controlsInnerHeight,
			int controlsInnerWidth,
			int timeBlockWidth,
			int fullscreenButtonWidth,
			int seekBackWidth,
			int playButtonWidth,
			int seekForwardWidth,
			Font font
	) {
		int border = PseudoVideoRenderer.FRAME_BORDER_THICKNESS;
		int chromeHeight = sliderAndControlsHeight(controlsInnerHeight);
		int maxDisplayHeight = 0;
		if (maxBottom > contentTop) {
			int maxVideoBlockHeight = maxBottom - contentTop - chromeHeight;
			maxDisplayHeight = Math.max(1, maxVideoBlockHeight - border * 2);
		}
		int[] display = PseudoVideoRenderer.computeDisplaySize(
				textWidth,
				maxDisplayHeight,
				PseudoVideoRenderer.NORMAL_DISPLAY_SCALE
		);
		int frameLeft = PseudoVideoRenderer.frameLeftForDisplay(textX, textWidth, display[0]);
		return build(
				frameLeft,
				display,
				contentTop,
				controlsInnerHeight,
				controlsInnerWidth,
				timeBlockWidth,
				fullscreenButtonWidth,
				seekBackWidth,
				playButtonWidth,
				seekForwardWidth,
				font
		);
	}

	private static int sliderAndControlsHeight(int controlsInnerHeight) {
		int border = PseudoVideoRenderer.FRAME_BORDER_THICKNESS;
		return SLIDER_INNER_HEIGHT + border * 2 + controlsInnerHeight + border * 2;
	}

	public static PseudoVideoLayout windowFullscreen(
			int screenWidth,
			int screenHeight,
			int controlsInnerHeight,
			int controlsInnerWidth,
			int timeBlockWidth,
			int fullscreenButtonWidth,
			int seekBackWidth,
			int playButtonWidth,
			int seekForwardWidth,
			Font font
	) {
		int border = PseudoVideoRenderer.FRAME_BORDER_THICKNESS;
		int sliderBlockHeight = SLIDER_INNER_HEIGHT + border * 2;
		int controlsBlockHeight = controlsInnerHeight + border * 2;
		int chromeHeight = sliderBlockHeight + controlsBlockHeight;
		int[] display = PseudoVideoRenderer.computeDisplaySize(
				screenWidth,
				Math.max(1, screenHeight - chromeHeight),
				PseudoVideoRenderer.FULLSCREEN_DISPLAY_SCALE
		);
		int frameLeft = (screenWidth - display[0]) / 2 - border;
		int videoBlockHeight = display[1] + border * 2;
		int totalHeight = videoBlockHeight + sliderBlockHeight + controlsBlockHeight;
		int videoBlockTop = Math.max(0, (screenHeight - totalHeight) / 2);
		return build(
				frameLeft,
				display,
				videoBlockTop,
				controlsInnerHeight,
				controlsInnerWidth,
				timeBlockWidth,
				fullscreenButtonWidth,
				seekBackWidth,
				playButtonWidth,
				seekForwardWidth,
				font
		);
	}

	private static PseudoVideoLayout build(
			int frameLeft,
			int[] display,
			int videoBlockTop,
			int controlsInnerHeight,
			int controlsInnerWidth,
			int timeBlockWidth,
			int fullscreenButtonWidth,
			int seekBackWidth,
			int playButtonWidth,
			int seekForwardWidth,
			Font font
	) {
		int border = PseudoVideoRenderer.FRAME_BORDER_THICKNESS;
		int displayHeight = display[1];
		int videoBlockHeight = displayHeight + border * 2;
		int sliderBlockTop = videoBlockTop + videoBlockHeight;
		int controlsBlockTop = sliderBlockTop + SLIDER_INNER_HEIGHT + border * 2;
		return buildAt(
				frameLeft,
				display,
				videoBlockTop,
				controlsInnerHeight,
				controlsInnerWidth,
				timeBlockWidth,
				fullscreenButtonWidth,
				seekBackWidth,
				playButtonWidth,
				seekForwardWidth,
				sliderBlockTop,
				controlsBlockTop,
				font
		);
	}

	private static PseudoVideoLayout buildAt(
			int frameLeft,
			int[] display,
			int videoBlockTop,
			int controlsInnerHeight,
			int controlsInnerWidth,
			int timeBlockWidth,
			int fullscreenButtonWidth,
			int seekBackWidth,
			int playButtonWidth,
			int seekForwardWidth,
			int sliderBlockTop,
			int controlsBlockTop,
			Font font
	) {
		int border = PseudoVideoRenderer.FRAME_BORDER_THICKNESS;
		int displayWidth = display[0];
		int displayHeight = display[1];
		int frameOuterWidth = displayWidth + border * 2;
		int videoBlockHeight = displayHeight + border * 2;
		int sliderBlockHeight = SLIDER_INNER_HEIGHT + border * 2;
		int controlsBlockHeight = controlsInnerHeight + border * 2;
		int videoDrawX = frameLeft + border;
		int videoDrawY = videoBlockTop + border;
		int controlsInnerY = controlsBlockTop + border;
		int timeLabelWidth = Math.max(0, timeBlockWidth - CONTROLS_SIDE_PADDING * 2);
		int videoCenterX = frameLeft + frameOuterWidth / 2;
		int playButtonX = videoCenterX - playButtonWidth / 2;
		int seekBackButtonX = playButtonX - seekBackWidth;
		int seekForwardButtonX = playButtonX + playButtonWidth;
		int fullscreenButtonX = seekForwardButtonX + seekForwardWidth;
		int timeTextX = seekBackButtonX - timeLabelWidth;
		int timeTextY = controlsInnerY + Math.max(0, (controlsInnerHeight - font.lineHeight) / 2);
		int controlsFrameLeft = timeTextX - CONTROLS_SIDE_PADDING - border;
		int controlsFrameOuterWidth = border * 2
				+ CONTROLS_SIDE_PADDING
				+ timeLabelWidth
				+ seekBackWidth
				+ playButtonWidth
				+ seekForwardWidth
				+ fullscreenButtonWidth;

		return new PseudoVideoLayout(
				frameLeft,
				frameOuterWidth,
				videoBlockTop,
				videoBlockHeight,
				videoDrawX,
				videoDrawY,
				displayWidth,
				displayHeight,
				sliderBlockTop,
				sliderBlockHeight,
				controlsBlockTop,
				controlsBlockHeight,
				controlsFrameLeft,
				controlsFrameOuterWidth,
				timeTextX,
				timeTextY,
				seekBackButtonX,
				playButtonX,
				seekForwardButtonX,
				fullscreenButtonX
		);
	}

	public int totalHeight() {
		return this.videoBlockHeight + this.sliderBlockHeight + this.controlsBlockHeight;
	}

	public int sliderWidgetX() {
		return this.frameLeft + PseudoVideoRenderer.FRAME_BORDER_THICKNESS;
	}

	public int sliderWidgetY() {
		return this.sliderBlockTop + PseudoVideoRenderer.FRAME_BORDER_THICKNESS;
	}

	public int sliderWidgetWidth() {
		return this.frameOuterWidth - PseudoVideoRenderer.FRAME_BORDER_THICKNESS * 2;
	}

	public int controlsWidgetY() {
		return this.controlsBlockTop + PseudoVideoRenderer.FRAME_BORDER_THICKNESS;
	}
}
