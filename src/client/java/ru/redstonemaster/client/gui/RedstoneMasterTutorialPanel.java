package ru.redstonemaster.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import ru.redstonemaster.client.gui.tutorial.TutorialCatalog;
import ru.redstonemaster.client.gui.tutorial.TutorialCatalog.FilteredSection;
import ru.redstonemaster.client.gui.tutorial.TutorialLesson;
import ru.redstonemaster.client.gui.tutorial.TutorialSection;
import ru.redstonemaster.client.gui.tutorial.TutorialStudyTarget;
import ru.redstonemaster.client.gui.tutorial.TutorialTextures;
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
	private static final int IMAGE_GAP = 10;
	private static final int COLLAPSE_ALL_GAP = 6;

	private static final int SECTION_COLOR = 0xFFE8C070;
	private static final int LESSON_COLOR = 0xFFFFFFFF;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int HEADER_LABEL_COLOR = 0xFFE0E0E0;
	private static final int DISCLAIMER_COLOR = 0xFFBBBBBB;
	private static final int SOURCES_COLOR = 0xFFAAAAAA;
	private static final int EMPTY_COLOR = 0xFFBBBBBB;
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

	RedstoneMasterTutorialPanel(RedstoneMasterScreen screen) {
		this.screen = screen;
	}

	boolean isStudying() {
		return this.studyTarget != null;
	}

	void leaveTab() {
		this.studyTarget = null;
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
	}

	void dispose() {
		this.searchBox = null;
		this.backButton = null;
		this.collapseAllButton = null;
		this.layoutRows.clear();
		this.studyButtons.clear();
		this.sectionToggleButtons.clear();
	}

	void rebuildWidgets() {
		this.layoutRows.clear();
		this.studyButtons.clear();
		this.sectionToggleButtons.clear();
		this.backButton = null;

		int innerX = this.screen.getContentX() + RedstoneMasterScreen.CONTENT_INNER_PADDING;
		int innerWidth = this.screen.getContentWidth() - RedstoneMasterScreen.CONTENT_INNER_PADDING * 2;

		if (this.studyTarget != null) {
			this.rebuildStudyWidgets(innerX, innerWidth);
			return;
		}

		this.rebuildListWidgets(innerX, innerWidth);
	}

	private void rebuildStudyWidgets(int innerX, int innerWidth) {
		this.layoutRows.add(LayoutRow.studyBody(this.getListTop()));

		this.backButton = Button.builder(
						ModContentLanguage.translatable("gui.redstone-master.tutorial.back"),
						button -> this.closeStudy())
				.bounds(innerX, this.getSearchY(), 80, this.getSearchHeight())
				.build();
		this.screen.addContentWidget(this.backButton);

		this.clampScrollOffset();
	}

	private void rebuildListWidgets(int innerX, int innerWidth) {
		int studyButtonWidth = this.getStudyButtonWidth();
		int blockInnerX = innerX + SECTION_BLOCK_PADDING;
		int blockInnerWidth = innerWidth - SECTION_BLOCK_PADDING * 2;
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

			Button arrowButton = Button.builder(
							this.getExpandArrowLabel(expanded),
							button -> this.toggleSection(section.id()))
					.bounds(blockInnerX, sectionRowY, ARROW_BUTTON_SIZE, ROW_HEIGHT)
					.build();
			this.sectionToggleButtons.add(arrowButton);
			this.screen.addContentWidget(arrowButton);

			Button sectionStudy = Button.builder(
							ModContentLanguage.translatable("gui.redstone-master.tutorial.study"),
							button -> this.openStudy(TutorialStudyTarget.section(section.id())))
					.bounds(blockInnerX + blockInnerWidth - studyButtonWidth, sectionRowY, studyButtonWidth, ROW_HEIGHT)
					.build();
			this.studyButtons.add(sectionStudy);
			this.screen.addContentWidget(sectionStudy);

			this.layoutRows.add(LayoutRow.sectionHeader(section.title(), sectionRowY, ROW_HEIGHT));
			y += ROW_HEIGHT + ROW_GAP;

			if (expanded) {
				int lessonX = blockInnerX + ARROW_BUTTON_SIZE + ARROW_TO_TITLE_GAP + LESSON_EXTRA_INDENT;
				for (TutorialLesson lesson : entry.lessons()) {
					int lessonRowY = y;
					this.layoutRows.add(LayoutRow.lesson(lesson.title(), lessonRowY, ROW_HEIGHT));

					Button lessonStudy = Button.builder(
									ModContentLanguage.translatable("gui.redstone-master.tutorial.study"),
									button -> this.openStudy(TutorialStudyTarget.lesson(section.id(), lesson.id())))
							.bounds(blockInnerX + blockInnerWidth - studyButtonWidth, lessonRowY, studyButtonWidth, ROW_HEIGHT)
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

		this.searchBox = new EditBox(
				this.screen.getFont(),
				innerX,
				this.getSearchY(),
				innerWidth,
				this.getSearchHeight(),
				ModContentLanguage.translatable("gui.redstone-master.tutorial.search_hint")
		);
		this.searchBox.setMaxLength(64);
		this.searchBox.setHint(ModContentLanguage.translatable("gui.redstone-master.tutorial.search_hint"));
		this.searchBox.setValue(this.searchQuery);
		this.searchBox.setResponder(value -> {
			this.searchQuery = value;
			this.scrollOffset = 0;
			this.screen.rebuildTutorialWidgets();
		});
		this.screen.addContentWidget(this.searchBox);

		this.clampScrollOffset();
		this.applyScrollToControls();
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
		this.studyTarget = target;
		this.scrollOffset = 0;
		this.screen.rebuildTutorialWidgets();
		this.screen.onNavigationPointReached();
	}

	private void closeStudy() {
		this.screen.navigateBack();
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

	private int getLineColor() {
		return ModConfig.get().highContrastBorders ? LINE_COLOR_HIGH_CONTRAST : LINE_COLOR_NORMAL;
	}

	boolean mouseScrolled(double scrollX, double scrollY) {
		int maxScroll = this.getMaxScroll();
		if (maxScroll <= 0) {
			return false;
		}
		this.scrollOffset = (int) Math.clamp(this.scrollOffset - scrollY * 12, 0, maxScroll);
		if (!this.isStudying()) {
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
			this.renderStudyContent(graphics, textX, textWidth, listTop, contentBottom);
		} else {
			this.renderListContent(graphics, innerX, innerWidth, textX, textWidth, listTop, contentBottom);
		}

		graphics.disableScissor();
	}

	private void renderStudyContent(
			GuiGraphics graphics,
			int textX,
			int textWidth,
			int listTop,
			int contentBottom
	) {
		StudyContent content = this.resolveStudyContent();
		if (content == null) {
			return;
		}

		int y = this.getListTop() - this.scrollOffset;
		for (var line : this.screen.getFont()
				.split(Component.literal(content.title()).withStyle(net.minecraft.ChatFormatting.BOLD), textWidth)) {
			if (y + this.screen.getFont().lineHeight >= listTop && y <= contentBottom) {
				graphics.drawString(this.screen.getFont(), line, textX, y, SECTION_COLOR, true);
			}
			y += this.screen.getFont().lineHeight;
		}
		y += 4;

		for (var line : this.screen.getFont().split(Component.literal(content.body()), textWidth)) {
			if (y + this.screen.getFont().lineHeight >= listTop && y <= contentBottom) {
				graphics.drawString(this.screen.getFont(), line, textX, y, TEXT_COLOR, true);
			}
			y += this.screen.getFont().lineHeight;
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
	}

	private StudyContent resolveStudyContent() {
		if (this.studyTarget instanceof TutorialStudyTarget.SectionTarget sectionTarget) {
			TutorialSection section = TutorialCatalog.findSection(sectionTarget.sectionId());
			if (section == null) {
				return null;
			}
			return new StudyContent(section.title(), section.summary(), section.sources(), section.imagePaths());
		}
		if (this.studyTarget instanceof TutorialStudyTarget.LessonTarget lessonTarget) {
			TutorialLesson lesson = TutorialCatalog.findLesson(lessonTarget.sectionId(), lessonTarget.lessonId());
			TutorialSection section = TutorialCatalog.findSection(lessonTarget.sectionId());
			if (lesson == null) {
				return null;
			}
			String sources = section != null ? section.sources() : "";
			return new StudyContent(lesson.title(), lesson.body(), sources, lesson.imagePaths());
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
		int blockInnerX = innerX + SECTION_BLOCK_PADDING;
		int sectionTitleX = blockInnerX + ARROW_BUTTON_SIZE + ARROW_TO_TITLE_GAP;
		int sectionTitleMaxWidth = innerWidth - SECTION_BLOCK_PADDING * 2 - ARROW_BUTTON_SIZE
				- ARROW_TO_TITLE_GAP - studyButtonWidth - 4;
		int lessonX = sectionTitleX + LESSON_EXTRA_INDENT;

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
				int textY = drawY + (ROW_HEIGHT - this.screen.getFont().lineHeight) / 2;
				if (textY + this.screen.getFont().lineHeight >= listTop && textY <= contentBottom) {
					for (var line : this.screen.getFont()
							.split(Component.literal(row.sectionTitle), sectionTitleMaxWidth)) {
						graphics.drawString(this.screen.getFont(), line, sectionTitleX, textY, SECTION_COLOR, true);
						textY += this.screen.getFont().lineHeight;
					}
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
			}
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
			boolean visible = displayY >= listTop - 1 && displayY + ROW_HEIGHT <= contentBottom + 1;

			if (row.sectionTitle != null) {
				if (toggleIndex < this.sectionToggleButtons.size()) {
					Button arrow = this.sectionToggleButtons.get(toggleIndex++);
					arrow.setY(displayY);
					arrow.visible = visible;
				}
				if (studyIndex < this.studyButtons.size()) {
					Button study = this.studyButtons.get(studyIndex++);
					study.setY(displayY);
					study.visible = visible;
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
		int height = 0;
		height += this.screen.getFont().split(Component.literal(content.title()), innerWidth).size()
				* this.screen.getFont().lineHeight + 4;
		height += this.screen.getFont().split(Component.literal(content.body()), innerWidth).size()
				* this.screen.getFont().lineHeight + IMAGE_GAP;
		height += TutorialTextures.measureImagesHeight(content.images(), innerWidth, IMAGE_GAP);
		height += 4;
		if (content.sources() != null && !content.sources().isBlank()) {
			height += this.screen.getFont().split(Component.literal(content.sources()), innerWidth).size()
					* this.screen.getFont().lineHeight;
		}
		return height + 16;
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
	}

	private record StudyContent(String title, String body, String sources, List<String> images) {
	}

	private static final class LayoutRow {
		private final String sectionTitle;
		private final String lessonTitle;
		private final int y;
		private final int rowHeight;
		private final boolean isEmpty;
		private final boolean isStudyBody;
		private final boolean isDisclaimer;
		private final boolean isSectionsHeader;
		private final boolean isSectionBlock;

		private LayoutRow(
				String sectionTitle,
				String lessonTitle,
				int y,
				int rowHeight,
				boolean isEmpty,
				boolean isStudyBody,
				boolean isDisclaimer,
				boolean isSectionsHeader,
				boolean isSectionBlock
		) {
			this.sectionTitle = sectionTitle;
			this.lessonTitle = lessonTitle;
			this.y = y;
			this.rowHeight = rowHeight;
			this.isEmpty = isEmpty;
			this.isStudyBody = isStudyBody;
			this.isDisclaimer = isDisclaimer;
			this.isSectionsHeader = isSectionsHeader;
			this.isSectionBlock = isSectionBlock;
		}

		static LayoutRow sectionHeader(String sectionTitle, int y, int rowHeight) {
			return new LayoutRow(sectionTitle, null, y, rowHeight, false, false, false, false, false);
		}

		static LayoutRow lesson(String lessonTitle, int y, int rowHeight) {
			return new LayoutRow(null, lessonTitle, y, rowHeight, false, false, false, false, false);
		}

		static LayoutRow sectionBlock(int y, int rowHeight) {
			return new LayoutRow(null, null, y, rowHeight, false, false, false, false, true);
		}

		static LayoutRow disclaimer(int y, int rowHeight) {
			return new LayoutRow(null, null, y, rowHeight, false, false, true, false, false);
		}

		static LayoutRow sectionsHeader(int y, int rowHeight) {
			return new LayoutRow(null, null, y, rowHeight, false, false, false, true, false);
		}

		static LayoutRow empty(int y) {
			return new LayoutRow(null, null, y, ROW_HEIGHT, true, false, false, false, false);
		}

		static LayoutRow studyBody(int y) {
			return new LayoutRow(null, null, y, 0, false, true, false, false, false);
		}
	}
}
