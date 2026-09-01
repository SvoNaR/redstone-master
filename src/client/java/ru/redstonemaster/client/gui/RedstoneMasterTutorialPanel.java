package ru.redstonemaster.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import ru.redstonemaster.client.gui.tutorial.TutorialCatalog;
import ru.redstonemaster.client.gui.tutorial.TutorialCatalog.FilteredSection;
import ru.redstonemaster.client.gui.tutorial.TutorialLesson;
import ru.redstonemaster.client.gui.tutorial.TutorialLessonProgress;
import ru.redstonemaster.client.gui.tutorial.TutorialSection;
import ru.redstonemaster.client.gui.tutorial.TutorialStudyTarget;
import ru.redstonemaster.client.comments.ModCommentAvatarCache;
import ru.redstonemaster.client.comments.ModLessonComment;
import ru.redstonemaster.client.comments.ModLessonCommentsService;
import ru.redstonemaster.client.gui.tutorial.TutorialTextures;
import ru.redstonemaster.client.video.PseudoVideoLayout;
import net.minecraft.client.renderer.RenderPipelines;
import ru.redstonemaster.client.video.PseudoVideoRenderer;
import ru.redstonemaster.client.video.PseudoVideoSeekSlider;
import ru.redstonemaster.client.video.PseudoVideoService;
import ru.redstonemaster.config.ModConfig;
import ru.redstonemaster.config.ModContentLanguage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class RedstoneMasterTutorialPanel {
	private static final String DISCLAIMER_KEY = "gui.redstone-master.tutorial.disclaimer";
	private static final String SECTIONS_HEADER_KEY = "gui.redstone-master.tutorial.sections_header";

	private static final int SEARCH_HEIGHT = 20;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_GAP = 2;
	private static final int SEARCH_LINE_GAP = 1;
	private static final int DISCLAIMER_GAP = 6;
	private static final int HEADER_GAP = 6;
	private static final int SECTIONS_HEADER_EXTRA_GAP_LINES = 1;
	private static final int SECTION_BLOCK_GAP = 6;
	private static final int SECTION_BLOCK_PADDING = 4;
	private static final int STUDY_BUTTON_MIN_WIDTH = 56;
	private static final int STUDY_BUTTON_PADDING = 8;
	private static final int ARROW_BUTTON_SIZE = 20;
	private static final int ARROW_TO_TITLE_GAP = 4;
	private static final int LESSON_EXTRA_INDENT = 6;
	private static final int LESSON_CHECKBOX_SIZE = 14;
	private static final int LESSON_CHECKBOX_GAP = 4;
	private static final int SECTION_PROGRESS_GAP = 8;
	private static final int IMAGE_GAP = 10;
	private static final String VIDEO_GOAL_HEADING_KEY = "gui.redstone-master.tutorial.video.goal_heading";
	private static final int VIDEO_GAP = 8;
	private static final int VIDEO_CONTROLS_HEIGHT = 20;
	private static final int VIDEO_CONTROLS_GAP = 0;
	private static final int VIDEO_AFTER_CONTROLS_GAP = 10;
	private static final int VIDEO_GOAL_HEADER_GAP = 4;
	private static final int VIDEO_PLAY_BUTTON_WIDTH = 28;
	private static final int VIDEO_FULLSCREEN_BUTTON_WIDTH = 28;
	private static final int COLLAPSE_ALL_GAP = 6;
	private static final int COMMENT_AVATAR_SIZE = 8;
	private static final int COMMENT_AVATAR_GAP = 4;
	private static final int COMMENT_BLOCK_GAP = 8;
	private static final int COMMENT_SECTION_GAP = 12;
	private static final int COMMENT_BUTTON_HEIGHT = 20;

	private static final int SECTION_COLOR = 0xFFE8C070;
	private static final int SECTION_TITLE_DOWN_OFFSET = 2;
	private static final int SECTION_SUMMARY_VERTICAL_PADDING = 1;
	private static final int LESSON_COLOR = 0xFFFFFFFF;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int HEADER_LABEL_COLOR = 0xFFE0E0E0;
	private static final int DISCLAIMER_COLOR = 0xFFBBBBBB;
	private static final int SOURCES_COLOR = 0xFFAAAAAA;
	private static final int EMPTY_COLOR = 0xFFBBBBBB;
	private static final int PROGRESS_COLOR = 0xFFBBBBBB;
	private static final int LESSON_CHECKMARK_COLOR = 0xFF55FF55;
	private static final int LINE_COLOR_NORMAL = 0xFF000000;
	private static final int LINE_COLOR_HIGH_CONTRAST = 0xFFFFFFFF;

	private final RedstoneMasterScreen screen;
	private EditBox searchBox;
	private String searchQuery = "";
	private int scrollOffset;
	private int savedListScrollOffset;
	private TutorialStudyTarget studyTarget;
	private final Set<String> expandedSections = new HashSet<>();
	private final List<LayoutRow> layoutRows = new ArrayList<>();
	private final List<Button> studyButtons = new ArrayList<>();
	private final List<Button> sectionToggleButtons = new ArrayList<>();
	private Button backButton;
	private Button collapseAllButton;
	private Button videoPlayPauseButton;
	private Button videoSeekBackButton;
	private Button videoSeekForwardButton;
	private Button videoFullscreenButton;
	private PseudoVideoSeekSlider videoSeekSlider;
	private String activeStudyVideoId = "";
	private boolean videoFullscreen;
	private Button studyCommentsButton;
	private boolean studyCommentsExpanded;
	private boolean studyCommentsLoading;
	private boolean studyCommentsLoadFailed;
	private List<ModLessonComment> studyComments = List.of();

	RedstoneMasterTutorialPanel(RedstoneMasterScreen screen) {
		this.screen = screen;
	}

	boolean isStudying() {
		return this.studyTarget != null;
	}

	void leaveTab() {
		this.exitVideoFullscreen();
		PseudoVideoService.get().release();
		this.activeStudyVideoId = "";
		this.studyTarget = null;
	}

	void resetToHome() {
		this.studyTarget = null;
		this.scrollOffset = 0;
		this.savedListScrollOffset = 0;
		this.expandedSections.clear();
	}

	/** Прокрутка списка разделов для сохранения сессии (не прокрутка страницы «Изучить»). */
	int getListScrollOffsetForPersistence() {
		return this.isStudying() ? this.savedListScrollOffset : this.scrollOffset;
	}

	void restoreExpandedSections(String csv) {
		this.expandedSections.clear();
		if (csv == null || csv.isBlank()) {
			return;
		}
		for (String part : csv.split(",")) {
			String id = part.trim();
			if (!id.isEmpty()) {
				this.expandedSections.add(id);
			}
		}
	}

	String getExpandedSectionsCsv() {
		return String.join(",", this.expandedSections);
	}

	int getScrollOffset() {
		return this.scrollOffset;
	}

	void setScrollOffset(int scrollOffset) {
		this.scrollOffset = Math.max(0, scrollOffset);
		this.updateLessonCompletionFromScroll();
	}

	void dispose() {
		PseudoVideoService.get().release();
		this.activeStudyVideoId = "";
		this.searchBox = null;
		this.backButton = null;
		this.collapseAllButton = null;
		this.videoPlayPauseButton = null;
		this.videoSeekBackButton = null;
		this.videoSeekForwardButton = null;
		this.videoFullscreenButton = null;
		this.videoSeekSlider = null;
		this.studyCommentsButton = null;
		this.layoutRows.clear();
		this.studyButtons.clear();
		this.sectionToggleButtons.clear();
	}

	void rebuildWidgets() {
		this.layoutRows.clear();
		this.studyButtons.clear();
		this.sectionToggleButtons.clear();
		this.backButton = null;
		this.videoPlayPauseButton = null;
		this.videoSeekBackButton = null;
		this.videoSeekForwardButton = null;
		this.videoFullscreenButton = null;
		this.videoSeekSlider = null;
		this.studyCommentsButton = null;

		int innerX = this.screen.getContentX() + RedstoneMasterScreen.CONTENT_INNER_PADDING;
		int innerWidth = this.screen.getContentWidth() - RedstoneMasterScreen.CONTENT_INNER_PADDING * 2;

		if (this.studyTarget != null) {
			this.rebuildStudyWidgets(innerX, innerWidth);
			return;
		}

		this.rebuildListWidgets(innerX, innerWidth, true);
	}

	void applySearchFilter() {
		if (this.studyTarget != null) {
			return;
		}
		boolean searchFocused = this.searchBox != null && this.searchBox.isFocused();
		int cursorPosition = searchFocused ? this.searchBox.getCursorPosition() : 0;
		this.removeListContentWidgets();
		this.layoutRows.clear();
		int innerX = this.screen.getContentX() + RedstoneMasterScreen.CONTENT_INNER_PADDING;
		int innerWidth = this.screen.getContentWidth() - RedstoneMasterScreen.CONTENT_INNER_PADDING * 2;
		this.scrollOffset = 0;
		this.rebuildListWidgets(innerX, innerWidth, false);
		this.updateSearchBoxLayout();
		if (searchFocused) {
			ModSearchEditBox.restoreFocus(this.searchBox, this.screen, cursorPosition);
		}
		this.clampScrollOffset();
		this.applyScrollToControls();
	}

	private void removeListContentWidgets() {
		for (Button button : this.sectionToggleButtons) {
			this.screen.removeContentWidget(button);
		}
		for (Button button : this.studyButtons) {
			this.screen.removeContentWidget(button);
		}
		if (this.collapseAllButton != null) {
			this.screen.removeContentWidget(this.collapseAllButton);
			this.collapseAllButton = null;
		}
		this.sectionToggleButtons.clear();
		this.studyButtons.clear();
	}

	private void rebuildStudyWidgets(int innerX, int innerWidth) {
		this.layoutRows.add(LayoutRow.studyBody(this.getListTop()));

		this.backButton = Button.builder(
						ModContentLanguage.translatable("gui.redstone-master.tutorial.back"),
						button -> this.closeStudy())
				.bounds(innerX, this.getSearchY(), 80, this.getSearchHeight())
				.build();
		this.screen.addContentWidget(this.backButton);

		StudyContent content = this.resolveStudyContent();
		String videoId = content != null && !content.videos().isEmpty() ? content.videos().getFirst() : "";
		if (!videoId.equals(this.activeStudyVideoId)) {
			this.activeStudyVideoId = videoId;
			if (videoId.isBlank()) {
				PseudoVideoService.get().release();
			} else {
				PseudoVideoService.get().activate(videoId);
			}
		}

		if (!videoId.isBlank()) {
			int seekBackWidth = this.getVideoSeekButtonWidth("gui.redstone-master.tutorial.video.seek_back");
			int seekForwardWidth = this.getVideoSeekButtonWidth("gui.redstone-master.tutorial.video.seek_forward");
			this.videoSeekBackButton = Button.builder(
							ModContentLanguage.translatable("gui.redstone-master.tutorial.video.seek_back"),
							button -> PseudoVideoService.get().seekBackward())
					.bounds(innerX, 0, seekBackWidth, VIDEO_CONTROLS_HEIGHT)
					.build();
			this.videoPlayPauseButton = Button.builder(
							this.getVideoPlayPauseLabel(),
							button -> {
								PseudoVideoService.get().togglePlayPause();
								button.setMessage(this.getVideoPlayPauseLabel());
							})
					.bounds(innerX, 0, VIDEO_PLAY_BUTTON_WIDTH, VIDEO_CONTROLS_HEIGHT)
					.build();
			this.videoSeekForwardButton = Button.builder(
							ModContentLanguage.translatable("gui.redstone-master.tutorial.video.seek_forward"),
							button -> PseudoVideoService.get().seekForward())
					.bounds(innerX, 0, seekForwardWidth, VIDEO_CONTROLS_HEIGHT)
					.build();
			this.screen.addContentWidget(this.videoSeekBackButton);
			this.screen.addContentWidget(this.videoPlayPauseButton);
			this.screen.addContentWidget(this.videoSeekForwardButton);

			this.videoFullscreenButton = Button.builder(
							this.getVideoFullscreenLabel(),
							button -> this.toggleVideoFullscreen())
					.bounds(innerX, 0, VIDEO_FULLSCREEN_BUTTON_WIDTH, VIDEO_CONTROLS_HEIGHT)
					.build();
			this.screen.addContentWidget(this.videoFullscreenButton);

			this.videoSeekSlider = new PseudoVideoSeekSlider(0, 0, 100, PseudoVideoLayout.SLIDER_INNER_HEIGHT);
			this.screen.addContentWidget(this.videoSeekSlider);
			this.setVideoControlsVisible(false);
			this.layoutStudyVideoControls();
		}

		if (this.canShowLessonComments()) {
			int buttonWidth = this.getStudyCommentsButtonWidth();
			this.studyCommentsButton = Button.builder(
							ModContentLanguage.translatable("gui.redstone-master.tutorial.comments.show"),
							button -> this.toggleStudyComments())
					.bounds(innerX, 0, buttonWidth, COMMENT_BUTTON_HEIGHT)
					.build();
			this.screen.addContentWidget(this.studyCommentsButton);
			this.layoutStudyCommentsButton();
		}

		this.clampScrollOffset();
		this.updateLessonCompletionFromScroll();
	}

	private Component getVideoPlayPauseLabel() {
		return PseudoVideoService.get().isPlaying()
				? ModContentLanguage.translatable("gui.redstone-master.tutorial.video.pause")
				: ModContentLanguage.translatable("gui.redstone-master.tutorial.video.play");
	}

	private int getVideoSeekButtonWidth(String key) {
		return this.screen.getFont().width(ModContentLanguage.get(key)) + STUDY_BUTTON_PADDING;
	}

	private Component getVideoFullscreenLabel() {
		return ModContentLanguage.translatable(
				this.videoFullscreen
						? "gui.redstone-master.tutorial.video.exit_fullscreen"
						: "gui.redstone-master.tutorial.video.fullscreen"
		);
	}

	private int getVideoTimeBlockWidth() {
		int sampleWidth = Math.max(
				this.screen.getFont().width("00:00/00:00"),
				this.screen.getFont().width(PseudoVideoService.get().getPlaybackTimeLabel())
		);
		return sampleWidth + PseudoVideoLayout.CONTROLS_SIDE_PADDING * 2;
	}

	private int getVideoControlsInnerWidth() {
		return this.getVideoTimeBlockWidth()
				+ this.getVideoSeekBackWidth()
				+ VIDEO_PLAY_BUTTON_WIDTH
				+ this.getVideoSeekForwardWidth()
				+ VIDEO_FULLSCREEN_BUTTON_WIDTH;
	}

	boolean isVideoFullscreen() {
		return this.videoFullscreen;
	}

	boolean handleVideoFullscreenEscape() {
		if (!this.videoFullscreen || !this.hasActiveStudyVideo()) {
			return false;
		}
		this.exitVideoFullscreen();
		return true;
	}

	private void toggleVideoFullscreen() {
		this.videoFullscreen = !this.videoFullscreen;
		if (this.videoFullscreenButton != null) {
			this.videoFullscreenButton.setMessage(this.getVideoFullscreenLabel());
		}
		this.screen.applyTutorialVideoFullscreenChrome(this.videoFullscreen);
		this.layoutStudyVideoControls();
	}

	private void exitVideoFullscreen() {
		if (!this.videoFullscreen) {
			return;
		}
		this.videoFullscreen = false;
		if (this.videoFullscreenButton != null) {
			this.videoFullscreenButton.setMessage(this.getVideoFullscreenLabel());
		}
		this.screen.applyTutorialVideoFullscreenChrome(false);
	}

	private int getVideoSeekBackWidth() {
		return this.getVideoSeekButtonWidth("gui.redstone-master.tutorial.video.seek_back");
	}

	private int getVideoSeekForwardWidth() {
		return this.getVideoSeekButtonWidth("gui.redstone-master.tutorial.video.seek_forward");
	}

	private PseudoVideoLayout computeVideoLayout(int embeddedContentTop, int textX, int textWidth, int maxBottom) {
		int timeBlockWidth = this.getVideoTimeBlockWidth();
		int seekBackWidth = this.getVideoSeekBackWidth();
		int seekForwardWidth = this.getVideoSeekForwardWidth();
		if (this.videoFullscreen) {
			return PseudoVideoLayout.windowFullscreen(
					this.screen.getScreenWidth(),
					this.screen.getScreenHeight(),
					VIDEO_CONTROLS_HEIGHT,
					this.getVideoControlsInnerWidth(),
					timeBlockWidth,
					VIDEO_FULLSCREEN_BUTTON_WIDTH,
					seekBackWidth,
					VIDEO_PLAY_BUTTON_WIDTH,
					seekForwardWidth,
					this.screen.getFont()
			);
		}
		return PseudoVideoLayout.embedded(
				embeddedContentTop,
				textX,
				textWidth,
				maxBottom,
				VIDEO_CONTROLS_HEIGHT,
				this.getVideoControlsInnerWidth(),
				timeBlockWidth,
				VIDEO_FULLSCREEN_BUTTON_WIDTH,
				seekBackWidth,
				VIDEO_PLAY_BUTTON_WIDTH,
				seekForwardWidth,
				this.screen.getFont()
		);
	}

	boolean isVideoControlWidget(Object widget) {
		return widget == this.videoPlayPauseButton
				|| widget == this.videoSeekBackButton
				|| widget == this.videoSeekForwardButton
				|| widget == this.videoFullscreenButton
				|| widget == this.videoSeekSlider;
	}

	void applyStudyChromeVisible(boolean visible) {
		if (this.searchBox != null) {
			this.searchBox.visible = visible;
		}
		if (this.backButton != null) {
			this.backButton.visible = visible;
		}
		if (this.collapseAllButton != null) {
			this.collapseAllButton.visible = visible;
		}
		for (Button button : this.sectionToggleButtons) {
			button.visible = visible;
		}
		for (Button button : this.studyButtons) {
			button.visible = visible;
		}
	}

	void restoreAfterVideoFullscreen() {
		if (this.isStudying()) {
			this.applyStudyChromeVisible(true);
			this.layoutStudyVideoControls();
			return;
		}
		this.applyStudyChromeVisible(true);
		this.applyScrollToControls();
	}

	private int getEmbeddedVideoContentTop(StudyContent content, int textWidth) {
		int y = this.getListTop() - this.scrollOffset;
		y += this.screen.getFont()
						.split(Component.literal(content.title()).withStyle(net.minecraft.ChatFormatting.BOLD), textWidth)
						.size()
				* this.screen.getFont().lineHeight
				+ 4;
		if (!content.videos().isEmpty()) {
			StudyBodyParts bodyParts = this.splitStudyBodyForVideo(content.body());
			if (this.hasStudyGoalBeforeVideo(content.body(), bodyParts)) {
				y += this.measureStudyGoalBeforeVideoHeight(bodyParts.goalParagraph(), textWidth);
			} else {
				y += VIDEO_GAP;
			}
		}
		return y;
	}

	private boolean hasStudyGoalBeforeVideo(String body, StudyBodyParts bodyParts) {
		return body.startsWith(ModContentLanguage.get(VIDEO_GOAL_HEADING_KEY));
	}

	private StudyBodyParts splitStudyBodyForVideo(String body) {
		String goalHeading = ModContentLanguage.get(VIDEO_GOAL_HEADING_KEY);
		if (!body.startsWith(goalHeading)) {
			return new StudyBodyParts("", body);
		}
		String rest = body.substring(goalHeading.length());
		if (rest.startsWith("\r\n")) {
			rest = rest.substring(2);
		} else if (rest.startsWith("\n")) {
			rest = rest.substring(1);
		}
		int sectionBreak = rest.indexOf("\n\n");
		if (sectionBreak >= 0) {
			return new StudyBodyParts(rest.substring(0, sectionBreak).trim(), rest.substring(sectionBreak + 2));
		}
		return new StudyBodyParts(rest.trim(), "");
	}

	private int measureStudyGoalBeforeVideoHeight(String goalParagraph, int textWidth) {
		int height = this.screen.getFont()
						.split(
								Component.literal(ModContentLanguage.get(VIDEO_GOAL_HEADING_KEY))
										.withStyle(net.minecraft.ChatFormatting.BOLD),
								textWidth
						)
						.size()
				* this.screen.getFont().lineHeight
				+ VIDEO_GOAL_HEADER_GAP;
		if (!goalParagraph.isBlank()) {
			height += this.screen.getFont().split(Component.literal(goalParagraph), textWidth).size()
					* this.screen.getFont().lineHeight;
		}
		return height + VIDEO_GAP;
	}

	private int renderStudyGoalBeforeVideo(
			GuiGraphics graphics,
			int textX,
			int textWidth,
			int y,
			int listTop,
			int contentBottom,
			String goalParagraph
	) {
		for (var line : this.screen.getFont()
				.split(
						Component.literal(ModContentLanguage.get(VIDEO_GOAL_HEADING_KEY))
								.withStyle(net.minecraft.ChatFormatting.BOLD),
						textWidth
				)) {
			if (y + this.screen.getFont().lineHeight >= listTop && y <= contentBottom) {
				graphics.drawString(this.screen.getFont(), line, textX, y, SECTION_COLOR, true);
			}
			y += this.screen.getFont().lineHeight;
		}
		y += VIDEO_GOAL_HEADER_GAP;
		if (!goalParagraph.isBlank()) {
			for (var line : this.screen.getFont().split(Component.literal(goalParagraph), textWidth)) {
				if (y + this.screen.getFont().lineHeight >= listTop && y <= contentBottom) {
					graphics.drawString(this.screen.getFont(), line, textX, y, TEXT_COLOR, true);
				}
				y += this.screen.getFont().lineHeight;
			}
		}
		return y + VIDEO_GAP;
	}

	private boolean hasActiveStudyVideo() {
		return this.activeStudyVideoId != null && !this.activeStudyVideoId.isBlank();
	}

	private void rebuildListWidgets(int innerX, int innerWidth, boolean recreateSearchBox) {
		int studyButtonWidth = this.getStudyButtonWidth();
		int lessonControlsWidth = this.getLessonControlsWidth(studyButtonWidth);
		int blockInnerX = innerX + SECTION_BLOCK_PADDING;
		int blockInnerWidth = innerWidth - SECTION_BLOCK_PADDING * 2;
		int progressReservedWidth = this.getSectionProgressReservedWidth();
		int y = this.getListTop();

		int disclaimerHeight = this.getDisclaimerHeight(innerWidth);
		this.layoutRows.add(LayoutRow.disclaimer(y, disclaimerHeight));
		y += disclaimerHeight + DISCLAIMER_GAP + this.getSectionsHeaderTopGap();

		int headerHeight = this.screen.getFont().lineHeight;
		this.layoutRows.add(LayoutRow.sectionsHeader(y, headerHeight));
		y += headerHeight + HEADER_GAP;

		List<FilteredSection> filtered = TutorialCatalog.filter(this.searchQuery);

		for (FilteredSection entry : filtered) {
			TutorialSection section = entry.section();
			boolean expanded = entry.forceExpanded() || this.expandedSections.contains(section.id());
			int blockTop = y;

			y += SECTION_BLOCK_PADDING;
			int sectionRowY = y;
			int sectionTitleMaxWidth = blockInnerWidth - ARROW_BUTTON_SIZE - ARROW_TO_TITLE_GAP - progressReservedWidth - 4;
			int sectionTitleLines = this.screen.getFont()
					.split(Component.literal(section.title()), sectionTitleMaxWidth)
					.size();
			int sectionRowHeight = Math.max(
					ROW_HEIGHT,
					sectionTitleLines * this.screen.getFont().lineHeight
			);

			Button arrowButton = Button.builder(
							this.getExpandArrowLabel(expanded),
							button -> this.toggleSection(section.id()))
					.bounds(blockInnerX, sectionRowY, ARROW_BUTTON_SIZE, ROW_HEIGHT)
					.build();
			this.sectionToggleButtons.add(arrowButton);
			this.screen.addContentWidget(arrowButton);

			this.layoutRows.add(LayoutRow.sectionHeader(section.id(), section.title(), sectionRowY, sectionRowHeight));
			y += sectionRowHeight + ROW_GAP;

			if (expanded) {
				String summary = section.summary();
				if (summary != null && !summary.isBlank()) {
					int lineHeight = this.screen.getFont().lineHeight;
					int summaryLineCount = this.screen.getFont()
							.split(Component.literal(summary), sectionTitleMaxWidth)
							.size();
					int summaryHeight = summaryLineCount * lineHeight + SECTION_SUMMARY_VERTICAL_PADDING * 2;
					this.layoutRows.add(LayoutRow.sectionSummary(summary, y, summaryHeight));
					y += summaryHeight + ROW_GAP;
				}

				int lessonX = blockInnerX + ARROW_BUTTON_SIZE + ARROW_TO_TITLE_GAP + LESSON_EXTRA_INDENT;
				int lessonStudyX = blockInnerX + blockInnerWidth - lessonControlsWidth;
				for (TutorialLesson lesson : entry.lessons()) {
					int lessonRowY = y;
					this.layoutRows.add(LayoutRow.lesson(section.id(), lesson.id(), lesson.title(), lessonRowY, ROW_HEIGHT));

					Button lessonStudy = Button.builder(
									ModContentLanguage.translatable("gui.redstone-master.tutorial.study"),
									button -> this.openStudy(TutorialStudyTarget.lesson(section.id(), lesson.id())))
							.bounds(lessonStudyX + LESSON_CHECKBOX_SIZE + LESSON_CHECKBOX_GAP, lessonRowY, studyButtonWidth, ROW_HEIGHT)
							.build();
					this.studyButtons.add(lessonStudy);
					this.screen.addContentWidget(lessonStudy);
					y += ROW_HEIGHT + ROW_GAP;
				}
			}

			y += SECTION_BLOCK_PADDING;
			int blockHeight = y - blockTop;
			this.layoutRows.add(LayoutRow.sectionBlock(blockTop, blockHeight));
			y += SECTION_BLOCK_GAP;
		}

		if (filtered.isEmpty()) {
			this.layoutRows.add(LayoutRow.empty(y));
		}

		this.collapseAllButton = Button.builder(
						ModContentLanguage.translatable("gui.redstone-master.tutorial.collapse_all"),
						button -> this.collapseAllSections())
				.bounds(innerX, this.getCollapseAllButtonY(), innerWidth, ROW_HEIGHT)
				.build();
		this.collapseAllButton.active = !this.expandedSections.isEmpty();
		this.screen.addContentWidget(this.collapseAllButton);

		if (recreateSearchBox) {
			if (this.searchBox != null) {
				this.screen.removeContentWidget(this.searchBox);
				this.searchBox = null;
			}
			this.ensureSearchBox(innerX, innerWidth);
		}

		this.clampScrollOffset();
		this.applyScrollToControls();
	}

	private void ensureSearchBox(int innerX, int innerWidth) {
		this.searchBox = ModSearchEditBox.create(
				this.screen.getFont(),
				innerX,
				this.getSearchY(),
				innerWidth,
				this.getSearchHeight(),
				"gui.redstone-master.tutorial.search_hint",
				this.searchQuery,
				value -> {
					this.searchQuery = value;
					this.applySearchFilter();
				}
		);
		this.screen.addContentWidget(this.searchBox);
	}

	private void updateSearchBoxLayout() {
		int innerX = this.screen.getContentX() + RedstoneMasterScreen.CONTENT_INNER_PADDING;
		int innerWidth = this.screen.getContentWidth() - RedstoneMasterScreen.CONTENT_INNER_PADDING * 2;
		ModSearchEditBox.updateBounds(
				this.searchBox,
				innerX,
				this.getSearchY(),
				innerWidth,
				this.getSearchHeight()
		);
	}

	private int getDisclaimerHeight(int width) {
		return this.screen.getFont()
				.split(ModContentLanguage.translatable(DISCLAIMER_KEY), width)
				.size() * this.screen.getFont().lineHeight;
	}

	private Component getExpandArrowLabel(boolean expanded) {
		return Component.literal(expanded ? "\u25BC" : "\u25C0");
	}

	private void toggleSection(String sectionId) {
		if (this.expandedSections.contains(sectionId)) {
			this.expandedSections.remove(sectionId);
		} else {
			if (ModConfig.get().tutorialCollapseOtherSections) {
				this.expandedSections.clear();
			}
			this.expandedSections.add(sectionId);
		}
		this.screen.rebuildTutorialWidgets();
	}

	private void collapseAllSections() {
		if (this.expandedSections.isEmpty()) {
			return;
		}
		this.expandedSections.clear();
		this.screen.rebuildTutorialWidgets();
	}

	private void openStudy(TutorialStudyTarget target) {
		if (this.studyTarget == null) {
			this.screen.onNavigationPointReached();
		}
		if (ModConfig.get().rememberSession) {
			this.savedListScrollOffset = this.scrollOffset;
		}
		this.resetStudyCommentsState();
		this.studyTarget = target;
		this.scrollOffset = 0;
		this.screen.rebuildTutorialWidgets();
		this.screen.onNavigationPointReached();
	}

	private void closeStudy() {
		if (this.studyTarget == null) {
			return;
		}
		this.exitVideoFullscreen();
		PseudoVideoService.get().release();
		this.activeStudyVideoId = "";
		this.resetStudyCommentsState();
		this.studyTarget = null;
		if (ModConfig.get().rememberSession) {
			this.scrollOffset = this.savedListScrollOffset;
		} else {
			this.scrollOffset = 0;
		}
		this.screen.rebuildTutorialWidgets();
		this.screen.onNavigationPointReached();
	}

	void restoreNavigationState(
			@org.jetbrains.annotations.Nullable TutorialStudyTarget target,
			int scrollOffset,
			int savedListScrollOffset
	) {
		this.studyTarget = target;
		this.scrollOffset = Math.max(0, scrollOffset);
		this.savedListScrollOffset = Math.max(0, savedListScrollOffset);
	}

	int getSavedListScrollOffset() {
		return this.savedListScrollOffset;
	}

	@org.jetbrains.annotations.Nullable
	TutorialStudyTarget getStudyTargetForNavigation() {
		return this.studyTarget;
	}

	boolean isValidStudyTarget(TutorialStudyTarget target) {
		TutorialCatalog.ensureLoaded();
		return switch (target) {
			case TutorialStudyTarget.SectionTarget section ->
					TutorialCatalog.findSection(section.sectionId()) != null;
			case TutorialStudyTarget.LessonTarget lesson ->
					TutorialCatalog.findLesson(lesson.sectionId(), lesson.lessonId()) != null;
		};
	}

	private int getStudyButtonWidth() {
		int textWidth = this.screen.getFont()
				.width(ModContentLanguage.get("gui.redstone-master.tutorial.study"));
		return Math.max(STUDY_BUTTON_MIN_WIDTH, textWidth + STUDY_BUTTON_PADDING);
	}

	private int getLessonControlsWidth(int studyButtonWidth) {
		return LESSON_CHECKBOX_SIZE + LESSON_CHECKBOX_GAP + studyButtonWidth;
	}

	private int getSectionProgressReservedWidth() {
		return this.screen.getFont().width("99 / 99") + SECTION_PROGRESS_GAP;
	}

	private String formatSectionProgress(String sectionId) {
		return TutorialLessonProgress.countCompleted(sectionId)
				+ " / "
				+ TutorialLessonProgress.countLessons(sectionId);
	}

	private void updateLessonCompletionFromScroll() {
		if (!(this.studyTarget instanceof TutorialStudyTarget.LessonTarget target)) {
			return;
		}
		int maxScroll = this.getMaxScroll();
		if (maxScroll <= 0 || this.scrollOffset >= maxScroll) {
			TutorialLessonProgress.markCompleted(target.sectionId(), target.lessonId());
		}
	}

	private void renderLessonCheckbox(
			GuiGraphics graphics,
			int x,
			int y,
			boolean completed,
			int listTop,
			int contentBottom,
			int lineColor
	) {
		int boxY = y + (ROW_HEIGHT - LESSON_CHECKBOX_SIZE) / 2;
		if (boxY + LESSON_CHECKBOX_SIZE < listTop || boxY > contentBottom) {
			return;
		}
		graphics.renderOutline(x, boxY, LESSON_CHECKBOX_SIZE, LESSON_CHECKBOX_SIZE, lineColor);
		if (completed) {
			String mark = "\u2713";
			int markX = x + (LESSON_CHECKBOX_SIZE - this.screen.getFont().width(mark)) / 2;
			int markY = boxY + (LESSON_CHECKBOX_SIZE - this.screen.getFont().lineHeight) / 2;
			graphics.drawString(this.screen.getFont(), mark, markX, markY, LESSON_CHECKMARK_COLOR, true);
		}
	}

	private int getLineColor() {
		return ModConfig.get().highContrastBorders ? LINE_COLOR_HIGH_CONTRAST : LINE_COLOR_NORMAL;
	}

	boolean mouseScrolled(double scrollX, double scrollY) {
		if (this.videoFullscreen) {
			return true;
		}
		int maxScroll = this.getMaxScroll();
		if (maxScroll <= 0) {
			return false;
		}
		this.scrollOffset = (int) Math.clamp(this.scrollOffset - scrollY * 12, 0, maxScroll);
		this.updateLessonCompletionFromScroll();
		if (this.isStudying()) {
			this.layoutStudyVideoControls();
		} else {
			this.applyScrollToControls();
		}
		return true;
	}

	void render(GuiGraphics graphics) {
		int listTop = this.getListTop();
		int contentBottom = this.getScrollableContentBottom();
		int innerX = this.screen.getContentX() + RedstoneMasterScreen.CONTENT_INNER_PADDING;
		int innerWidth = this.screen.getContentWidth() - RedstoneMasterScreen.CONTENT_INNER_PADDING * 2;
		int textX = innerX + 2;
		int textWidth = innerWidth - 4;

		graphics.enableScissor(
				this.screen.getContentX() + 1,
				listTop,
				this.screen.getContentX() + this.screen.getContentWidth() - 1,
				contentBottom
		);

		if (this.studyTarget != null) {
			this.renderStudyContent(graphics, innerX, textX, textWidth, listTop, contentBottom);
		} else {
			this.renderListContent(graphics, innerX, innerWidth, textX, textWidth, listTop, contentBottom);
		}

		graphics.disableScissor();
	}

	void renderVideoFullscreenOverlay(GuiGraphics graphics) {
		if (!this.videoFullscreen || !this.hasActiveStudyVideo()) {
			return;
		}
		PseudoVideoLayout layout = this.computeVideoLayout(0, 0, 0, 0);
		PseudoVideoRenderer.renderPlayer(
				graphics,
				this.screen.getFont(),
				layout,
				0,
				this.screen.getScreenHeight(),
				this.getLineColor(),
				true
		);
	}

	private void renderStudyContent(
			GuiGraphics graphics,
			int innerX,
			int textX,
			int textWidth,
			int listTop,
			int contentBottom
	) {
		StudyContent content = this.resolveStudyContent();
		if (content == null) {
			return;
		}

		boolean hasVideo = !content.videos().isEmpty();
		StudyBodyParts bodyParts = hasVideo ? this.splitStudyBodyForVideo(content.body()) : new StudyBodyParts("", content.body());
		String bodyAfterVideo = hasVideo ? bodyParts.remainingBody() : content.body();
		int y = this.getListTop() - this.scrollOffset;
		for (var line : this.screen.getFont()
				.split(Component.literal(content.title()).withStyle(net.minecraft.ChatFormatting.BOLD), textWidth)) {
			if (y + this.screen.getFont().lineHeight >= listTop && y <= contentBottom) {
				graphics.drawString(this.screen.getFont(), line, textX, y, SECTION_COLOR, true);
			}
			y += this.screen.getFont().lineHeight;
		}
		y += 4;

		if (hasVideo && this.hasStudyGoalBeforeVideo(content.body(), bodyParts)) {
			y = this.renderStudyGoalBeforeVideo(graphics, textX, textWidth, y, listTop, contentBottom, bodyParts.goalParagraph());
		}

		if (hasVideo && !this.videoFullscreen) {
			if (!this.hasStudyGoalBeforeVideo(content.body(), bodyParts)) {
				y += VIDEO_GAP;
			}
			int lineColor = this.getLineColor();
			PseudoVideoLayout layout = this.computeVideoLayout(y, textX, textWidth, contentBottom);
			PseudoVideoRenderer.renderPlayer(
					graphics,
					this.screen.getFont(),
					layout,
					listTop,
					contentBottom,
					lineColor,
					true
			);
			y = layout.controlsBlockTop() + layout.controlsBlockHeight() + VIDEO_AFTER_CONTROLS_GAP;
		}

		if (!bodyAfterVideo.isBlank()) {
			for (var line : this.screen.getFont().split(Component.literal(bodyAfterVideo), textWidth)) {
				if (y + this.screen.getFont().lineHeight >= listTop && y <= contentBottom) {
					graphics.drawString(this.screen.getFont(), line, textX, y, TEXT_COLOR, true);
				}
				y += this.screen.getFont().lineHeight;
			}
		}
		y += IMAGE_GAP;

		y = TutorialTextures.renderImages(
				graphics,
				this.screen.getFont(),
				content.images(),
				textX,
				y,
				textWidth,
				listTop,
				contentBottom,
				IMAGE_GAP
		);

		y += 4;
		if (content.sources() != null && !content.sources().isBlank()) {
			for (var line : this.screen.getFont().split(Component.literal(content.sources()), textWidth)) {
				if (y + this.screen.getFont().lineHeight >= listTop && y <= contentBottom) {
					graphics.drawString(this.screen.getFont(), line, textX, y, SOURCES_COLOR, true);
				}
				y += this.screen.getFont().lineHeight;
			}
		}

		y += this.renderStudyCommentsSection(graphics, textX, textWidth, y, listTop, contentBottom);
	}

	private StudyContent resolveStudyContent() {
		if (this.studyTarget instanceof TutorialStudyTarget.SectionTarget sectionTarget) {
			TutorialSection section = TutorialCatalog.findSection(sectionTarget.sectionId());
			if (section == null) {
				return null;
			}
			return new StudyContent(section.title(), section.summary(), section.sources(), section.imagePaths(), List.of());
		}
		if (this.studyTarget instanceof TutorialStudyTarget.LessonTarget lessonTarget) {
			TutorialLesson lesson = TutorialCatalog.findLesson(lessonTarget.sectionId(), lessonTarget.lessonId());
			TutorialSection section = TutorialCatalog.findSection(lessonTarget.sectionId());
			if (lesson == null) {
				return null;
			}
			String sources = section != null ? section.sources() : "";
			return new StudyContent(lesson.title(), lesson.body(), sources, lesson.imagePaths(), lesson.videoIds());
		}
		return null;
	}

	private void renderListContent(
			GuiGraphics graphics,
			int innerX,
			int innerWidth,
			int textX,
			int textWidth,
			int listTop,
			int contentBottom
	) {
		int lineColor = this.getLineColor();
		int studyButtonWidth = this.getStudyButtonWidth();
		int lessonControlsWidth = this.getLessonControlsWidth(studyButtonWidth);
		int progressReservedWidth = this.getSectionProgressReservedWidth();
		int blockInnerX = innerX + SECTION_BLOCK_PADDING;
		int blockInnerWidth = innerWidth - SECTION_BLOCK_PADDING * 2;
		int sectionTitleX = blockInnerX + ARROW_BUTTON_SIZE + ARROW_TO_TITLE_GAP;
		int sectionTitleMaxWidth = blockInnerWidth - ARROW_BUTTON_SIZE - ARROW_TO_TITLE_GAP - progressReservedWidth - 4;
		int sectionProgressRight = innerX + innerWidth - SECTION_BLOCK_PADDING;
		int lessonX = sectionTitleX + LESSON_EXTRA_INDENT;
		int lessonCheckboxX = blockInnerX + blockInnerWidth - lessonControlsWidth;

		for (LayoutRow row : this.layoutRows) {
			int drawY = row.y - this.scrollOffset;

			if (row.isSectionBlock) {
				if (drawY + row.rowHeight >= listTop && drawY <= contentBottom) {
					graphics.renderOutline(innerX, drawY, innerWidth, row.rowHeight, lineColor);
				}
				continue;
			}

			if (row.isDisclaimer) {
				int lineY = drawY;
				for (var line : this.screen.getFont()
						.split(ModContentLanguage.translatable(DISCLAIMER_KEY), textWidth)) {
					if (lineY + this.screen.getFont().lineHeight >= listTop && lineY <= contentBottom) {
						graphics.drawString(this.screen.getFont(), line, textX, lineY, DISCLAIMER_COLOR, true);
					}
					lineY += this.screen.getFont().lineHeight;
				}
			} else if (row.isSectionsHeader) {
				if (drawY + this.screen.getFont().lineHeight >= listTop && drawY <= contentBottom) {
					graphics.drawString(
							this.screen.getFont(),
							ModContentLanguage.translatable(SECTIONS_HEADER_KEY),
							textX,
							drawY,
							HEADER_LABEL_COLOR,
							true
					);
				}
			} else if (row.isEmpty) {
				if (drawY + this.screen.getFont().lineHeight >= listTop && drawY <= contentBottom) {
					graphics.drawString(
							this.screen.getFont(),
							ModContentLanguage.translatable("gui.redstone-master.tutorial.no_results"),
							textX,
							drawY,
							EMPTY_COLOR,
							true
					);
				}
			} else if (row.sectionTitle != null) {
				var titleLines = this.screen.getFont()
						.split(Component.literal(row.sectionTitle), sectionTitleMaxWidth);
				int textHeight = titleLines.size() * this.screen.getFont().lineHeight;
				int textY = drawY + Math.max(0, (row.rowHeight - textHeight) / 2) + SECTION_TITLE_DOWN_OFFSET;
				for (var line : titleLines) {
					if (textY + this.screen.getFont().lineHeight >= listTop && textY <= contentBottom) {
						graphics.drawString(this.screen.getFont(), line, sectionTitleX, textY, SECTION_COLOR, true);
					}
					textY += this.screen.getFont().lineHeight;
				}
				if (row.sectionId != null) {
					String progress = this.formatSectionProgress(row.sectionId);
					int progressWidth = this.screen.getFont().width(progress);
					int progressY = drawY + (row.rowHeight - this.screen.getFont().lineHeight) / 2 + SECTION_TITLE_DOWN_OFFSET;
					if (progressY + this.screen.getFont().lineHeight >= listTop && progressY <= contentBottom) {
						graphics.drawString(
								this.screen.getFont(),
								progress,
								sectionProgressRight - progressWidth,
								progressY,
								PROGRESS_COLOR,
								true
						);
					}
				}
			} else if (row.sectionSummary != null) {
				int lineY = drawY + SECTION_SUMMARY_VERTICAL_PADDING;
				for (var line : this.screen.getFont()
						.split(Component.literal(row.sectionSummary), sectionTitleMaxWidth)) {
					if (lineY + this.screen.getFont().lineHeight >= listTop && lineY <= contentBottom) {
						graphics.drawString(this.screen.getFont(), line, sectionTitleX, lineY, DISCLAIMER_COLOR, true);
					}
					lineY += this.screen.getFont().lineHeight;
				}
			} else if (row.lessonTitle != null) {
				if (drawY + this.screen.getFont().lineHeight >= listTop && drawY <= contentBottom) {
					graphics.drawString(
							this.screen.getFont(),
							Component.literal("• " + row.lessonTitle),
							lessonX,
							drawY + (ROW_HEIGHT - this.screen.getFont().lineHeight) / 2,
							LESSON_COLOR,
							true
					);
				}
				if (row.sectionId != null && row.lessonId != null) {
					this.renderLessonCheckbox(
							graphics,
							lessonCheckboxX,
							drawY,
							TutorialLessonProgress.isCompleted(row.sectionId, row.lessonId),
							listTop,
							contentBottom,
							lineColor
					);
				}
			}
		}
	}

	void layoutStudyVideoControls() {
		if (!this.isStudying() || !this.hasActiveStudyVideo()) {
			this.layoutStudyCommentsButton();
			return;
		}
		int innerX = this.screen.getContentX() + RedstoneMasterScreen.CONTENT_INNER_PADDING;
		int innerWidth = this.screen.getContentWidth() - RedstoneMasterScreen.CONTENT_INNER_PADDING * 2;
		int textWidth = innerWidth - 4;
		this.layoutStudyVideoControls(innerX, textWidth, this.getListTop(), this.getScrollableContentBottom());
		this.layoutStudyCommentsButton();
	}

	void layoutStudyCommentsButton() {
		if (this.studyCommentsButton == null || !this.canShowLessonComments()) {
			return;
		}
		StudyContent content = this.resolveStudyContent();
		if (content == null) {
			return;
		}
		int innerX = this.screen.getContentX() + RedstoneMasterScreen.CONTENT_INNER_PADDING;
		int innerWidth = this.screen.getContentWidth() - RedstoneMasterScreen.CONTENT_INNER_PADDING * 2;
		int textWidth = innerWidth - 4;
		int buttonY = this.getListTop() - this.scrollOffset
				+ this.measureStudyContentHeight(content, textWidth)
				+ COMMENT_SECTION_GAP;
		int listTop = this.getListTop();
		int contentBottom = this.getScrollableContentBottom();
		this.studyCommentsButton.setX(innerX + 2);
		this.studyCommentsButton.setY(buttonY);
		this.studyCommentsButton.setWidth(this.getStudyCommentsButtonWidth());
		this.studyCommentsButton.setHeight(COMMENT_BUTTON_HEIGHT);
		boolean visible = buttonY + COMMENT_BUTTON_HEIGHT >= listTop && buttonY <= contentBottom;
		this.studyCommentsButton.visible = visible;
		this.studyCommentsButton.active = !this.studyCommentsLoading;
	}

	private void layoutStudyVideoControls(int innerX, int textWidth, int listTop, int contentBottom) {
		if (!this.hasActiveStudyVideo()
				|| this.videoPlayPauseButton == null
				|| this.videoSeekBackButton == null
				|| this.videoSeekForwardButton == null
				|| this.videoFullscreenButton == null
				|| this.videoSeekSlider == null) {
			return;
		}

		StudyContent content = this.resolveStudyContent();
		if (content == null) {
			return;
		}

		int textX = innerX + 2;
		int embeddedTop = this.getEmbeddedVideoContentTop(content, textWidth);
		PseudoVideoLayout layout = this.computeVideoLayout(embeddedTop, textX, textWidth, contentBottom);
		int clipTop = this.videoFullscreen ? 0 : listTop;
		int clipBottom = this.videoFullscreen ? this.screen.getScreenHeight() : contentBottom;

		this.videoSeekSlider.setX(layout.sliderWidgetX());
		this.videoSeekSlider.setY(layout.sliderWidgetY());
		this.videoSeekSlider.setWidth(layout.sliderWidgetWidth());
		this.videoSeekSlider.setHeight(PseudoVideoLayout.SLIDER_INNER_HEIGHT);
		this.videoSeekSlider.active = PseudoVideoService.get().getPrepareState() == PseudoVideoService.PrepareState.READY;
		this.videoSeekSlider.syncFromPlayback();
		int sliderBottom = layout.sliderWidgetY() + PseudoVideoLayout.SLIDER_INNER_HEIGHT;
		boolean sliderVisible = sliderBottom >= clipTop && layout.sliderWidgetY() <= clipBottom;
		this.videoSeekSlider.visible = sliderVisible;

		int controlsY = layout.controlsWidgetY();
		int controlsBottom = controlsY + VIDEO_CONTROLS_HEIGHT;

		this.videoSeekBackButton.setX(layout.seekBackButtonX());
		this.videoSeekBackButton.setY(controlsY);
		this.videoPlayPauseButton.setX(layout.playButtonX());
		this.videoPlayPauseButton.setY(controlsY);
		this.videoPlayPauseButton.setMessage(this.getVideoPlayPauseLabel());
		this.videoSeekForwardButton.setX(layout.seekForwardButtonX());
		this.videoSeekForwardButton.setY(controlsY);
		this.videoFullscreenButton.setX(layout.fullscreenButtonX());
		this.videoFullscreenButton.setY(controlsY);
		this.videoFullscreenButton.setMessage(this.getVideoFullscreenLabel());

		boolean controlsVisible = controlsY >= clipTop && controlsBottom <= clipBottom;
		this.videoSeekBackButton.visible = controlsVisible;
		this.videoPlayPauseButton.visible = controlsVisible;
		this.videoSeekForwardButton.visible = controlsVisible;
		this.videoFullscreenButton.visible = controlsVisible;
	}

	private void setVideoControlsVisible(boolean visible) {
		if (this.videoSeekBackButton != null) {
			this.videoSeekBackButton.visible = visible;
		}
		if (this.videoPlayPauseButton != null) {
			this.videoPlayPauseButton.visible = visible;
		}
		if (this.videoSeekForwardButton != null) {
			this.videoSeekForwardButton.visible = visible;
		}
		if (this.videoFullscreenButton != null) {
			this.videoFullscreenButton.visible = visible;
		}
		if (this.videoSeekSlider != null) {
			this.videoSeekSlider.visible = visible;
		}
	}

	private void applyScrollToControls() {
		int listTop = this.getListTop();
		int contentBottom = this.getScrollableContentBottom();
		int toggleIndex = 0;
		int studyIndex = 0;

		for (LayoutRow row : this.layoutRows) {
			if (row.isEmpty || row.isStudyBody || row.isDisclaimer || row.isSectionsHeader || row.isSectionBlock) {
				continue;
			}
			int displayY = row.y - this.scrollOffset;
			boolean visible = displayY >= listTop - 1 && displayY + row.rowHeight <= contentBottom + 1;

			if (row.sectionTitle != null) {
				if (toggleIndex < this.sectionToggleButtons.size()) {
					Button arrow = this.sectionToggleButtons.get(toggleIndex++);
					arrow.setY(displayY);
					arrow.setHeight(ROW_HEIGHT);
					arrow.visible = visible;
				}
			} else if (row.lessonTitle != null && studyIndex < this.studyButtons.size()) {
				Button study = this.studyButtons.get(studyIndex++);
				study.setY(displayY);
				study.visible = visible;
			}
		}
	}

	private int getMaxScroll() {
		if (this.isStudying()) {
			StudyContent content = this.resolveStudyContent();
			if (content == null) {
				return 0;
			}
			int innerWidth = this.screen.getContentWidth() - RedstoneMasterScreen.CONTENT_INNER_PADDING * 2 - 4;
			return Math.max(0, this.measureStudyHeight(content, innerWidth) - this.getListViewHeight());
		}
		return Math.max(0, this.getListContentHeight() - this.getListViewHeight());
	}

	private int measureStudyHeight(StudyContent content, int innerWidth) {
		return this.measureStudyContentHeight(content, innerWidth)
				+ this.measureStudyCommentsBlockHeight(innerWidth)
				+ 16;
	}

	private int getListContentHeight() {
		if (this.layoutRows.isEmpty()) {
			return 0;
		}
		LayoutRow last = this.layoutRows.get(this.layoutRows.size() - 1);
		if (last.isStudyBody) {
			StudyContent content = this.resolveStudyContent();
			if (content == null) {
				return 0;
			}
			int innerWidth = this.screen.getContentWidth() - RedstoneMasterScreen.CONTENT_INNER_PADDING * 2 - 4;
			return this.measureStudyHeight(content, innerWidth);
		}
		return last.y + last.rowHeight - this.getListTop();
	}

	int getSearchY() {
		return this.screen.getContentY() + RedstoneMasterScreen.CONTENT_INNER_PADDING;
	}

	int getSearchHeight() {
		return Math.max(SEARCH_HEIGHT, this.screen.getFont().lineHeight + 10);
	}

	int getListTop() {
		return this.getSearchY() + this.getSearchHeight() + this.getGapAfterSearch();
	}

	private int getGapAfterSearch() {
		return this.screen.getFont().lineHeight * SEARCH_LINE_GAP;
	}

	private int getSectionsHeaderTopGap() {
		return this.screen.getFont().lineHeight * SECTIONS_HEADER_EXTRA_GAP_LINES;
	}

	private int getContentBottom() {
		return this.screen.getContentY() + this.screen.getContentHeight() - RedstoneMasterScreen.CONTENT_INNER_PADDING;
	}

	private int getCollapseAllButtonY() {
		return this.getContentBottom() - ROW_HEIGHT;
	}

	private int getScrollableContentBottom() {
		if (this.isStudying()) {
			return this.getContentBottom();
		}
		return this.getCollapseAllButtonY() - COLLAPSE_ALL_GAP;
	}

	private int getListViewHeight() {
		return this.getScrollableContentBottom() - this.getListTop();
	}

	private void clampScrollOffset() {
		this.scrollOffset = (int) Math.clamp(this.scrollOffset, 0, this.getMaxScroll());
		this.updateLessonCompletionFromScroll();
	}

	private boolean canShowLessonComments() {
		return this.studyTarget instanceof TutorialStudyTarget.LessonTarget
				&& ModLessonCommentsService.get().isWebsiteReachable();
	}

	private void resetStudyCommentsState() {
		this.studyCommentsExpanded = false;
		this.studyCommentsLoading = false;
		this.studyCommentsLoadFailed = false;
		this.studyComments = List.of();
	}

	private void toggleStudyComments() {
		if (this.studyCommentsExpanded) {
			this.studyCommentsExpanded = false;
			this.screen.rebuildTutorialWidgets();
			return;
		}
		if (!(this.studyTarget instanceof TutorialStudyTarget.LessonTarget lessonTarget)) {
			return;
		}
		this.studyCommentsLoading = true;
		this.studyCommentsLoadFailed = false;
		this.studyCommentsExpanded = true;
		ModLessonCommentsService.get().fetchComments(
				lessonTarget.sectionId(),
				lessonTarget.lessonId(),
				comments -> Minecraft.getInstance().execute(() -> {
					this.studyComments = comments;
					this.studyCommentsLoading = false;
					this.studyCommentsLoadFailed = false;
					this.screen.rebuildTutorialWidgets();
				}),
				() -> Minecraft.getInstance().execute(() -> {
					this.studyComments = List.of();
					this.studyCommentsLoading = false;
					this.studyCommentsLoadFailed = true;
					this.screen.rebuildTutorialWidgets();
				})
		);
	}

	private int getStudyCommentsButtonWidth() {
		return this.screen.getFont().width(ModContentLanguage.get("gui.redstone-master.tutorial.comments.show"))
				+ STUDY_BUTTON_PADDING;
	}

	private int measureStudyContentHeight(StudyContent content, int innerWidth) {
		int height = 0;
		height += this.screen.getFont().split(Component.literal(content.title()), innerWidth).size()
				* this.screen.getFont().lineHeight + 4;
		boolean hasVideo = !content.videos().isEmpty();
		StudyBodyParts bodyParts = hasVideo ? this.splitStudyBodyForVideo(content.body()) : new StudyBodyParts("", content.body());
		String bodyAfterVideo = hasVideo ? bodyParts.remainingBody() : content.body();
		if (hasVideo) {
			if (this.hasStudyGoalBeforeVideo(content.body(), bodyParts)) {
				height += this.measureStudyGoalBeforeVideoHeight(bodyParts.goalParagraph(), innerWidth);
			} else {
				height += VIDEO_GAP;
			}
			height += PseudoVideoLayout.embedded(
					0,
					0,
					innerWidth,
					0,
					VIDEO_CONTROLS_HEIGHT,
					this.getVideoControlsInnerWidth(),
					this.getVideoTimeBlockWidth(),
					VIDEO_FULLSCREEN_BUTTON_WIDTH,
					this.getVideoSeekBackWidth(),
					VIDEO_PLAY_BUTTON_WIDTH,
					this.getVideoSeekForwardWidth(),
					this.screen.getFont()
			).totalHeight();
			height += VIDEO_AFTER_CONTROLS_GAP;
		}
		if (!bodyAfterVideo.isBlank()) {
			height += this.screen.getFont().split(Component.literal(bodyAfterVideo), innerWidth).size()
					* this.screen.getFont().lineHeight;
		}
		height += IMAGE_GAP;
		height += TutorialTextures.measureImagesHeight(content.images(), innerWidth, IMAGE_GAP);
		height += 4;
		if (content.sources() != null && !content.sources().isBlank()) {
			height += this.screen.getFont().split(Component.literal(content.sources()), innerWidth).size()
					* this.screen.getFont().lineHeight;
		}
		return height;
	}

	private int measureStudyCommentsBlockHeight(int innerWidth) {
		if (!this.canShowLessonComments()) {
			return 0;
		}
		int height = COMMENT_SECTION_GAP + COMMENT_BUTTON_HEIGHT;
		if (!this.studyCommentsExpanded) {
			return height;
		}
		height += COMMENT_SECTION_GAP;
		if (this.studyCommentsLoading || this.studyCommentsLoadFailed) {
			height += this.screen.getFont().lineHeight;
			return height;
		}
		if (this.studyComments.isEmpty()) {
			height += this.screen.getFont().lineHeight;
			return height;
		}
		for (ModLessonComment comment : this.studyComments) {
			height += this.measureSingleCommentHeight(comment, innerWidth) + COMMENT_BLOCK_GAP;
		}
		return height;
	}

	private int measureSingleCommentHeight(ModLessonComment comment, int textWidth) {
		int headerHeight = Math.max(COMMENT_AVATAR_SIZE, this.screen.getFont().lineHeight);
		int bodyWidth = textWidth;
		int bodyLines = this.screen.getFont().split(Component.literal(comment.body()), bodyWidth).size();
		return headerHeight + 2 + bodyLines * this.screen.getFont().lineHeight;
	}

	private int renderStudyCommentsSection(
			GuiGraphics graphics,
			int textX,
			int textWidth,
			int y,
			int listTop,
			int contentBottom
	) {
		if (!this.canShowLessonComments()) {
			return 0;
		}
		int startY = y;
		y += COMMENT_SECTION_GAP + COMMENT_BUTTON_HEIGHT;
		if (!this.studyCommentsExpanded) {
			return y - startY;
		}
		y += COMMENT_SECTION_GAP;
		Component statusLine;
		if (this.studyCommentsLoading) {
			statusLine = ModContentLanguage.translatable("gui.redstone-master.tutorial.comments.loading");
		} else if (this.studyCommentsLoadFailed) {
			statusLine = ModContentLanguage.translatable("gui.redstone-master.tutorial.comments.error");
		} else if (this.studyComments.isEmpty()) {
			statusLine = ModContentLanguage.translatable("gui.redstone-master.tutorial.comments.empty");
		} else {
			for (ModLessonComment comment : this.studyComments) {
				y = this.renderSingleComment(graphics, textX, textWidth, y, listTop, contentBottom, comment);
				y += COMMENT_BLOCK_GAP;
			}
			return y - startY;
		}
		if (y + this.screen.getFont().lineHeight >= listTop && y <= contentBottom) {
			graphics.drawString(this.screen.getFont(), statusLine, textX, y, DISCLAIMER_COLOR, true);
		}
		y += this.screen.getFont().lineHeight;
		return y - startY;
	}

	private int renderSingleComment(
			GuiGraphics graphics,
			int textX,
			int textWidth,
			int y,
			int listTop,
			int contentBottom,
			ModLessonComment comment
	) {
		if (y > contentBottom) {
			return y;
		}
		ModCommentAvatarCache.AvatarDraw avatar = ModCommentAvatarCache.get(comment.id(), comment.avatarUrl());
		if (y + COMMENT_AVATAR_SIZE >= listTop && y <= contentBottom) {
			graphics.blit(
					RenderPipelines.GUI_TEXTURED,
					avatar.textureId(),
					textX,
					y,
					0.0f,
					0.0f,
					COMMENT_AVATAR_SIZE,
					COMMENT_AVATAR_SIZE,
					avatar.width(),
					avatar.height(),
					avatar.width(),
					avatar.height(),
					0xFFFFFFFF
			);
		}
		int nameX = textX + COMMENT_AVATAR_SIZE + COMMENT_AVATAR_GAP;
		String header = comment.username();
		if (comment.replyToUsername() != null && !comment.replyToUsername().isBlank()) {
			header += " (" + ModContentLanguage.format(
					"gui.redstone-master.tutorial.comments.reply_prefix",
					comment.replyToUsername()
			) + ")";
		}
		if (y + this.screen.getFont().lineHeight >= listTop && y <= contentBottom) {
			graphics.drawString(
					this.screen.getFont(),
					Component.literal(header).withStyle(net.minecraft.ChatFormatting.GOLD),
					nameX,
					y,
					LESSON_COLOR,
					true
			);
		}
		int bodyY = y + this.screen.getFont().lineHeight + 2;
		for (var line : this.screen.getFont().split(Component.literal(comment.body()), textWidth)) {
			if (bodyY + this.screen.getFont().lineHeight >= listTop && bodyY <= contentBottom) {
				graphics.drawString(this.screen.getFont(), line, textX, bodyY, TEXT_COLOR, true);
			}
			bodyY += this.screen.getFont().lineHeight;
		}
		return bodyY;
	}

	private record StudyContent(String title, String body, String sources, List<String> images, List<String> videos) {
	}

	private record StudyBodyParts(String goalParagraph, String remainingBody) {
	}

	private static final class LayoutRow {
		private final String sectionId;
		private final String lessonId;
		private final String sectionTitle;
		private final String sectionSummary;
		private final String lessonTitle;
		private final int y;
		private final int rowHeight;
		private final boolean isEmpty;
		private final boolean isStudyBody;
		private final boolean isDisclaimer;
		private final boolean isSectionsHeader;
		private final boolean isSectionBlock;

		private LayoutRow(
				String sectionId,
				String lessonId,
				String sectionTitle,
				String sectionSummary,
				String lessonTitle,
				int y,
				int rowHeight,
				boolean isEmpty,
				boolean isStudyBody,
				boolean isDisclaimer,
				boolean isSectionsHeader,
				boolean isSectionBlock
		) {
			this.sectionId = sectionId;
			this.lessonId = lessonId;
			this.sectionTitle = sectionTitle;
			this.sectionSummary = sectionSummary;
			this.lessonTitle = lessonTitle;
			this.y = y;
			this.rowHeight = rowHeight;
			this.isEmpty = isEmpty;
			this.isStudyBody = isStudyBody;
			this.isDisclaimer = isDisclaimer;
			this.isSectionsHeader = isSectionsHeader;
			this.isSectionBlock = isSectionBlock;
		}

		static LayoutRow sectionHeader(String sectionId, String sectionTitle, int y, int rowHeight) {
			return new LayoutRow(sectionId, null, sectionTitle, null, null, y, rowHeight, false, false, false, false, false);
		}

		static LayoutRow sectionSummary(String sectionSummary, int y, int rowHeight) {
			return new LayoutRow(null, null, null, sectionSummary, null, y, rowHeight, false, false, false, false, false);
		}

		static LayoutRow lesson(String sectionId, String lessonId, String lessonTitle, int y, int rowHeight) {
			return new LayoutRow(sectionId, lessonId, null, null, lessonTitle, y, rowHeight, false, false, false, false, false);
		}

		static LayoutRow sectionBlock(int y, int rowHeight) {
			return new LayoutRow(null, null, null, null, null, y, rowHeight, false, false, false, false, true);
		}

		static LayoutRow disclaimer(int y, int rowHeight) {
			return new LayoutRow(null, null, null, null, null, y, rowHeight, false, false, true, false, false);
		}

		static LayoutRow sectionsHeader(int y, int rowHeight) {
			return new LayoutRow(null, null, null, null, null, y, rowHeight, false, false, false, true, false);
		}

		static LayoutRow empty(int y) {
			return new LayoutRow(null, null, null, null, null, y, ROW_HEIGHT, true, false, false, false, false);
		}

		static LayoutRow studyBody(int y) {
			return new LayoutRow(null, null, null, null, null, y, 0, false, true, false, false, false);
		}
	}
}
